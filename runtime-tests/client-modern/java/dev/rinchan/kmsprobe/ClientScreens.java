package dev.rinchan.kmsprobe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

final class ClientScreens {
    private ClientScreens() {}

    static boolean isTitle(Minecraft client) {
        return client.screen instanceof TitleScreen;
    }
}
