#!/usr/bin/env python3
"""Build all supported loader/version targets from profiles.json and verify zero-dependency budgets."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MAX_JAR_BYTES = 120 * 1024


def discover_jdk(major: int) -> str:
    env_key = f"JAVA_{major}_HOME"
    if env_key in os.environ and Path(os.environ[env_key]).is_dir():
        return os.environ[env_key]
    candidates = [
        Path(f"/opt/java-{major}"),
        Path(f"/opt/homebrew/opt/openjdk@{major}/libexec/openjdk.jdk/Contents/Home"),
        Path(f"/Users/rin/Library/Java/JavaVirtualMachines/temurin-{major}.jdk/Contents/Home"),
    ]
    for pattern in (
        f"/usr/lib/jvm/temurin-{major}-jdk-*",
        f"/opt/homebrew/Cellar/openjdk@{major}/*/libexec/openjdk.jdk/Contents/Home",
        f"{Path.home()}/.gradle/jdks/*-{major}-*/Contents/Home",
    ):
        for p in sorted(Path("/").glob(pattern.lstrip("/")), reverse=True):
            candidates.append(p)
    for c in candidates:
        if (c / "bin/java").is_file():
            return str(c)
    raise RuntimeError(f"Could not find JDK {major}; set {env_key}")


def verify_jar(jar_path: Path, minecraft: str, loader: str):
    size = jar_path.stat().st_size
    if size >= MAX_JAR_BYTES:
        raise RuntimeError(f"JAR exceeds 120KB budget: {jar_path.name} ({size} bytes)")
    with zipfile.ZipFile(jar_path) as zf:
        names = set(zf.namelist())
        for name in names:
            if "rinlib" in name.lower():
                raise RuntimeError(f"Forbidden rinlib entry found in {jar_path.name}: {name}")
            if name.startswith("com/terraformersmc/modmenu/"):
                raise RuntimeError(f"Forbidden modmenu compile stub leaked in {jar_path.name}: {name}")
        if minecraft == "1.20.1" and loader == "forge":
            if "pack.mcmeta" not in names:
                raise RuntimeError(f"Missing pack.mcmeta in 1.20.1 forge JAR: {jar_path.name}")
            pack_meta = json.loads(zf.read("pack.mcmeta").decode("utf-8"))
            if pack_meta.get("pack", {}).get("pack_format") != 15:
                raise RuntimeError(f"Expected pack.pack_format == 15 in {jar_path.name}, got: {pack_meta}")
        elif "pack.mcmeta" in names:
            pack_meta = json.loads(zf.read("pack.mcmeta").decode("utf-8"))
            if pack_meta.get("pack", {}).get("pack_format") == 15:
                raise RuntimeError(f"Unexpected pack_format == 15 pack.mcmeta in non-1.20.1-forge JAR: {jar_path.name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", help="Optional single MC version to build")
    args = parser.parse_args()

    profiles = json.loads((ROOT / "profiles.json").read_text())
    version = next(
        line.split("=", 1)[1].strip()
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    )
    output = ROOT / "build/releases"
    if not args.profile and output.exists():
        for stale_jar in output.glob("*.jar"):
            stale_jar.unlink()
    output.mkdir(parents=True, exist_ok=True)
    lock = (ROOT / "build/matrix.lock").open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)

    all_jdks = {major: discover_jdk(major) for major in sorted({p["java"] for p in profiles.values()})}
    installations_paths = ",".join(all_jdks.values())

    manifest = []
    for minecraft, profile in profiles.items():
        if args.profile and minecraft != args.profile:
            continue
        loaders = profile["loaders"]
        env = os.environ.copy()
        # Gradle 9.2 runs on JDK 21/25 cleanly; Java 17 targets use Gradle toolchain.
        daemon_java = all_jdks.get(21) if profile["java"] < 21 else all_jdks[profile["java"]]
        env["JAVA_HOME"] = daemon_java

        tasks = []
        for loader in loaders:
            tasks.append(f":{loader}:clean")
        for loader in loaders:
            tasks.append(f":{loader}:build")

        in_container = Path("/.dockerenv").exists()
        command = [
            str(ROOT / "gradlew"),
            *tasks,
            "-Pminecraft_version=" + minecraft,
            "-Ploader=all",
            "-Porg.gradle.java.installations.paths=" + installations_paths,
            "--no-configuration-cache",
            *(["--no-parallel", "--max-workers=1", "-Dorg.gradle.jvmargs=-Xmx1G"] if in_container else ["--max-workers=2"]),
        ]
        subprocess.run(command, cwd=ROOT, env=env, check=True)

        for loader in loaders:
            source = (
                ROOT
                / f"build/profiles/{minecraft}/{loader}/libs/keep_my_sword-{loader}-{version}+{minecraft}.jar"
            )
            target = output / f"keep-my-sword-{version}+{minecraft}-{loader}.jar"
            shutil.copy2(source, target)
            verify_jar(target, minecraft, loader)
            binary = target.read_bytes()
            manifest.append(
                dict(
                    minecraft=minecraft,
                    loader=loader,
                    file=str(target),
                    bytes=len(binary),
                    sha256=hashlib.sha256(binary).hexdigest(),
                    sha512=hashlib.sha512(binary).hexdigest(),
                )
            )
            print(f"BUILT {minecraft} {loader}: {target.name} ({len(binary):,} bytes)", flush=True)

    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    if not args.profile:
        audit_env = os.environ.copy()
        audit_env["JAVA_HOME"] = all_jdks[profiles["1.21.1"]["java"]]
        in_container = Path("/.dockerenv").exists()
        subprocess.run(
            [
                str(ROOT / "gradlew"),
                ":neoforge:test",
                "--tests",
                "dev.rinchan.keepmysword.ArchitectureContractTest",
                "--rerun-tasks",
                "-Pminecraft_version=1.21.1",
                "-Ploader=neoforge",
                "-PrequireAllReleases=true",
                "-Porg.gradle.java.installations.paths=" + installations_paths,
                "--no-configuration-cache",
                *(["--no-parallel", "--max-workers=1", "-Dorg.gradle.jvmargs=-Xmx1G"] if in_container else ["--max-workers=2"]),
            ],
            cwd=ROOT,
            env=audit_env,
            check=True,
        )
    print(f"ALL DONE ({len(manifest)} targets).", flush=True)


if __name__ == "__main__":
    main()
