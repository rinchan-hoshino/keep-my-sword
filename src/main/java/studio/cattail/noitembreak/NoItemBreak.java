package studio.cattail.noitembreak;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(NoItemBreak.MOD_ID)
public final class NoItemBreak {
    public static final String MOD_ID = "no_item_break";

    public NoItemBreak() {
        NeoForge.EVENT_BUS.register(NoItemBreakRules.class);
    }
}
