package dev.rinchan.keepmysword.neoforge;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.client.KeepMySwordClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = KeepMySword.MOD_ID, dist = Dist.CLIENT)
public final class KeepMySwordNeoForgeClient {
    public KeepMySwordNeoForgeClient() { KeepMySwordClient.initialize(); }
}
