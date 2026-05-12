package dev.rinchan.keepmysword.forge;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import dev.rinchan.keepmysword.KeepMySword;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;

@Mod(modid = KeepMySword.MOD_ID, name = "Keep My Sword", version = "0.1.0", dependencies = "required-after:rinlib")
public final class KeepMySwordForge {
    public KeepMySwordForge() {
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeepMySwordForgeEvents());
    }
}
