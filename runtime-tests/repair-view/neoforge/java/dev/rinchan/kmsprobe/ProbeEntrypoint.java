package dev.rinchan.kmsprobe;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
@Mod("kms_probe")
public final class ProbeEntrypoint {
    public ProbeEntrypoint() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> e.getServer().execute(() -> RepairViewProbe.run(e.getServer())));
    }
}
