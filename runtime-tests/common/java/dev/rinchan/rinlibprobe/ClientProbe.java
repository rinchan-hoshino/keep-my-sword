package dev.rinchan.rinlibprobe;

import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

/** Wait for the actual title screen to render before recording a client startup pass. */
public final class ClientProbe {
    private static int titleTicks;

    private ClientProbe() {}

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (!(ClientScreens.isTitle(client)) || ++titleTicks != 20) {
            return;
        }
        try {
            Files.writeString(Path.of("kms-client.pass"), "Title screen rendered for 20 ticks\n");
            System.out.println("KMS_CLIENT_PASS");
        } catch (Exception error) {
            throw new IllegalStateException("Unable to record client startup result", error);
        } finally {
            client.stop();
        }
    }
}
