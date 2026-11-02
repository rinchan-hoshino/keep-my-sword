package dev.rinchan.keepmysword.neoforge;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.rinlib.v2.RinNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(KeepMySword.MOD_ID)
public final class KeepMySwordNeoForge {
    public KeepMySwordNeoForge(IEventBus modBus) {
        new RinNeoForge("keep_my_sword", modBus).start(new KeepMySword());
    }
}
