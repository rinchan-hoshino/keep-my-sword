package dev.rinchan.rinlibprobe;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "kms_probe", value = Dist.CLIENT)
public final class ClientEntrypoint {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientProbe.tick();
    }
}
