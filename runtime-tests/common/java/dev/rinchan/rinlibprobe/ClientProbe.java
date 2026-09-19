package dev.rinchan.rinlibprobe;

import net.minecraft.client.Minecraft;

import java.nio.file.*;

public final class ClientProbe {
    private static int titleTicks, previewTicks;
    private static boolean preview, opening;

    public static void tick() {
        var client = Minecraft.getInstance();
        if (!preview) {
            if (!opening) {
                if (!ClientScreens.isTitle(client) || ++titleTicks < 20) return;
                opening = true;
                client.createWorldOpenFlows()
                        .openWorld(
                                "kms-preview",
                                () -> {
                                    throw new IllegalStateException("Failed to load preview world");
                                });
                return;
            }
            if (client.level == null || client.player == null || ++titleTicks < 60) return;
            client.setScreen(new PreviewScreen());
            preview = true;
            return;
        }
        previewTicks++;
        if (previewTicks == 40)
            net.minecraft.client.Screenshot.grab(
                    client.gameDirectory, client.getMainRenderTarget(), message -> {});
        if (previewTicks < 70) return;
        try (var files = Files.list(Path.of("screenshots"))) {
            var screenshot =
                    files.filter(p -> p.toString().endsWith(".png")).findFirst().orElseThrow();
            Files.writeString(
                    Path.of("kms-client.pass"),
                    "Sealed sword, shield, armor and tooltip rendered: " + screenshot + "\n");
            System.out.println("KMS_CLIENT_PASS");
        } catch (Exception error) {
            throw new IllegalStateException("Client visual verification failed", error);
        } finally {
            client.stop();
        }
    }

    private ClientProbe() {}
}
