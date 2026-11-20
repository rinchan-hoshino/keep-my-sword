package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Automated architecture contract guardrails scanned directly against the real packaged JAR:
 * 1. Dedicated Server isolation: zero client class references in non-client classes.
 * 2. Mixin bidirectional closure: every declared mixin exists in JAR, every @Mixin class is declared.
 * 3. Clean artifact redline: zero rinlib, zero modmenu compile-stub leakage, within size budget.
 */
class ArchitectureContractTest {
    private static final long MAX_JAR_BYTES = 120 * 1024;
    private static final List<String> FORBIDDEN_CLIENT_PREFIXES = List.of(
            "net/minecraft/client/",
            "net/neoforged/neoforge/client/",
            "net/minecraftforge/client/",
            "com/mojang/blaze3d/",
            "net/fabricmc/api/ClientModInitializer",
            "com/terraformersmc/modmenu/");

    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("@Mixin\\s*\\(");
    private static final Pattern JSON_PACKAGE = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_ARRAY = Pattern.compile("\"(mixins|client|server)\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern JSON_STRING = Pattern.compile("\"([^\"]+)\"");

    private static List<Path> resolvePackagedJars() throws IOException {
        Map<Path, Path> unique = new LinkedHashMap<>();
        String configured = System.getProperty("kms.jar");
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured);
            assertTrue(Files.isRegularFile(p), "Configured kms.jar does not exist: " + p);
            unique.put(p.toRealPath(), p);
        }

        Path root = resolveProjectRoot();
        Path releasesDir = root.resolve("build/releases");
        boolean requireAll = Boolean.parseBoolean(System.getProperty("kms.requireAllReleases", "false"));
        List<Path> releaseJars = List.of();
        if (Files.isDirectory(releasesDir)) {
            try (Stream<Path> s = Files.list(releasesDir)) {
                releaseJars = s.filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return name.startsWith("keep-my-sword-") && name.endsWith(".jar");
                        })
                        .sorted()
                        .toList();
            }
            for (Path jar : releaseJars) {
                unique.put(jar.toRealPath(), jar);
            }
        }
        if (requireAll) {
            assertEquals(8, releaseJars.size(), "Expected all 8 release JARs in " + releasesDir + ", found: " + releaseJars);
        }

        if (unique.isEmpty()) {
            Path libsDir = root.resolve("build/profiles/1.21.1/neoforge/libs");
            if (Files.isDirectory(libsDir)) {
                try (Stream<Path> s = Files.list(libsDir)) {
                    Path fallback = s.filter(p -> p.getFileName().toString().endsWith(".jar")
                                    && !p.getFileName().toString().endsWith("-sources.jar"))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("No packaged JAR found in " + libsDir));
                    unique.put(fallback.toRealPath(), fallback);
                }
            }
        }
        assertFalse(unique.isEmpty(), "System property -Dkms.jar is not set and no built JARs found");
        return List.copyOf(unique.values());
    }

    private static String inferApiLayer(Path jarPath) {
        String name = jarPath.getFileName().toString();
        if (name.contains("+1.20.1-") || name.contains("+1.20.1.")) {
            return "v120";
        }
        if (name.contains("+1.21.1-") || name.contains("+1.21.1.")) {
            return "v121";
        }
        if (name.contains("+26.")) {
            return "v26";
        }
        return System.getProperty("kms.apiLayer", "v121");
    }

    private static Path resolveProjectRoot() {
        String configured = System.getProperty("kms.root");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path cur = Path.of("").toAbsolutePath();
        while (cur != null) {
            if (Files.isRegularFile(cur.resolve("profiles.json"))) {
                return cur;
            }
            cur = cur.getParent();
        }
        throw new AssertionError("Could not locate project root containing profiles.json");
    }

    @Test
    void dedicatedServerIsolationZeroClientReferencesOutsideClientPackages() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path jarPath : resolvePackagedJars()) {
            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                Set<String> clientOnlyMixins = collectDeclaredMixins(zf, true);

                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class")) continue;

                    String internalName = name.substring(0, name.length() - ".class".length());
                    if (isClientAllowedClass(internalName, clientOnlyMixins)) {
                        continue;
                    }

                    byte[] bytes = readEntry(zf, entry);
                    List<String> utf8Pool = readConstantPoolUtf8AndClasses(bytes);
                    for (String token : utf8Pool) {
                        for (String forbidden : FORBIDDEN_CLIENT_PREFIXES) {
                            if (token.contains(forbidden)) {
                                violations.add(jarPath.getFileName() + ": " + internalName + " references forbidden client symbol: " + token);
                                break;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(
                violations.isEmpty(),
                "Dedicated-server isolation violations:\n" + String.join("\n", violations));
    }

    @Test
    void mixinBidirectionalClosureBetweenConfigsJarAndSources() throws Exception {
        Path root = resolveProjectRoot();
        for (Path jarPath : resolvePackagedJars()) {
            String apiLayer = inferApiLayer(jarPath);
            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                Set<String> declaredMixins = collectDeclaredMixins(zf, false);
                assertFalse(declaredMixins.isEmpty(), "Expected at least one declared mixin in " + jarPath.getFileName());

                // Forward check: every declared mixin MUST exist as a .class inside the final JAR
                List<String> missingInJar = new ArrayList<>();
                for (String internalName : declaredMixins) {
                    if (zf.getEntry(internalName + ".class") == null) {
                        missingInJar.add(internalName);
                    }
                }
                assertTrue(
                        missingInJar.isEmpty(),
                        "Declared mixins missing from packaged JAR " + jarPath.getFileName() + ": " + missingInJar);

                // Reverse check 1 (JAR bytecode): every class with @Mixin annotation in JAR must be declared
                List<String> undeclaredInJar = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class") || name.contains("$")) continue;
                    String internalName = name.substring(0, name.length() - ".class".length());
                    byte[] bytes = readEntry(zf, entry);
                    List<String> pool = readConstantPoolUtf8AndClasses(bytes);
                    boolean hasMixinAnnotation = pool.contains("Lorg/spongepowered/asm/mixin/Mixin;");
                    if (hasMixinAnnotation && !declaredMixins.contains(internalName)) {
                        undeclaredInJar.add(internalName);
                    }
                }
                assertTrue(
                        undeclaredInJar.isEmpty(),
                        "Orphan @Mixin classes present in JAR " + jarPath.getFileName() + " but not registered in *.mixins.json: " + undeclaredInJar);

                // Reverse check 2 (Sources): every @Mixin in common/src/main and common/src/<apiLayer> must be declared
                List<Path> srcRoots = List.of(
                        root.resolve("common/src/main/java"),
                        root.resolve("common/src/" + apiLayer + "/java"));
                List<String> undeclaredInSources = new ArrayList<>();
                for (Path srcRoot : srcRoots) {
                    if (!Files.isDirectory(srcRoot)) continue;
                    try (Stream<Path> walk = Files.walk(srcRoot)) {
                        for (Path javaFile : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                            String content = Files.readString(javaFile, StandardCharsets.UTF_8);
                            if (MIXIN_ANNOTATION.matcher(content).find()) {
                                String rel = srcRoot.relativize(javaFile).toString().replace('\\', '/');
                                String internalName = rel.substring(0, rel.length() - ".java".length());
                                if (!declaredMixins.contains(internalName)) {
                                    undeclaredInSources.add(internalName);
                                }
                            }
                        }
                    }
                }
                assertTrue(
                        undeclaredInSources.isEmpty(),
                        "Source @Mixin classes not registered in *.mixins.json for " + jarPath.getFileName() + ": " + undeclaredInSources);
            }
        }
    }

    @Test
    void cleanArtifactRedlineZeroRinlibZeroModMenuStubAndWithinSizeBudget() throws Exception {
        for (Path jarPath : resolvePackagedJars()) {
            long size = Files.size(jarPath);
            assertTrue(
                    size > 0 && size < MAX_JAR_BYTES,
                    "Packaged JAR size out of budget (" + size + " bytes, max " + MAX_JAR_BYTES + "): " + jarPath);

            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                List<String> forbiddenEntries = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    String lower = name.toLowerCase();
                    if (lower.contains("rinlib")) {
                        forbiddenEntries.add("rinlib leak: " + name);
                    }
                    if (name.startsWith("com/terraformersmc/modmenu/")) {
                        forbiddenEntries.add("modmenu compile stub leak: " + name);
                    }
                }
                assertTrue(
                        forbiddenEntries.isEmpty(),
                        "Forbidden entries found in packaged JAR " + jarPath.getFileName() + ":\n"
                                + String.join("\n", forbiddenEntries));
            }
        }
    }

    private static boolean isClientAllowedClass(String internalName, Set<String> clientOnlyMixins) {
        if (internalName.contains("/client/")) return true;
        int dollar = internalName.indexOf('$');
        String outer = dollar >= 0 ? internalName.substring(0, dollar) : internalName;
        return clientOnlyMixins.contains(outer);
    }

    private static Set<String> collectDeclaredMixins(ZipFile zf, boolean clientOnly) throws IOException {
        Set<String> result = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".mixins.json")) continue;

            String json = new String(readEntry(zf, entry), StandardCharsets.UTF_8);
            Matcher pkgMatcher = JSON_PACKAGE.matcher(json);
            assertTrue(pkgMatcher.find(), "Missing package field in " + entry.getName());
            String pkgPath = pkgMatcher.group(1).replace('.', '/');

            Matcher arrMatcher = JSON_ARRAY.matcher(json);
            while (arrMatcher.find()) {
                String section = arrMatcher.group(1);
                if (clientOnly && !"client".equals(section)) continue;
                String body = arrMatcher.group(2);
                Matcher itemMatcher = JSON_STRING.matcher(body);
                while (itemMatcher.find()) {
                    String mixinRel = itemMatcher.group(1).replace('.', '/');
                    result.add(pkgPath + "/" + mixinRel);
                }
            }
        }
        return result;
    }

    private static byte[] readEntry(ZipFile zf, ZipEntry entry) throws IOException {
        try (InputStream in = zf.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    /** Minimal JVM ClassFile constant-pool parser extracting all CONSTANT_Utf8 strings. */
    private static List<String> readConstantPoolUtf8AndClasses(byte[] classBytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(classBytes));
        int magic = dis.readInt();
        assertEquals(0xCAFEBABE, magic, "Invalid class file magic");
        dis.readUnsignedShort(); // minor
        dis.readUnsignedShort(); // major
        int cpCount = dis.readUnsignedShort();

        Map<Integer, String> utf8s = new HashMap<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i < cpCount; i++) {
            int tag = dis.readUnsignedByte();
            switch (tag) {
                case 1 -> { // CONSTANT_Utf8
                    String s = dis.readUTF();
                    utf8s.put(i, s);
                    tokens.add(s);
                }
                case 3, 4 -> dis.skipBytes(4); // Integer, Float
                case 5, 6 -> { // Long, Double take 2 slots
                    dis.skipBytes(8);
                    i++;
                }
                case 7, 8, 16, 19, 20 -> dis.skipBytes(2); // Class, String, MethodType, Module, Package
                case 9, 10, 11, 12, 17, 18 -> dis.skipBytes(4); // Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
                case 15 -> dis.skipBytes(3); // MethodHandle
                default -> throw new IOException("Unknown constant pool tag: " + tag);
            }
        }
        return tokens;
    }
}
