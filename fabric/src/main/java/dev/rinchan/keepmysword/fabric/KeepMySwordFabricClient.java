package dev.rinchan.keepmysword.fabric;

import dev.rinchan.keepmysword.client.KeepMySwordClient;
import net.fabricmc.api.ClientModInitializer;

public final class KeepMySwordFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() { KeepMySwordClient.initialize(); }
}
