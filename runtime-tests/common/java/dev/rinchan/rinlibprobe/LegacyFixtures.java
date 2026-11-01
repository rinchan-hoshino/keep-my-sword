package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.CarrierSnapshot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

final class LegacyFixtures {
    static CompoundTag original() throws Exception {
        return TagParser.parseTag(
                "{id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:1561,other_mod_owner:\"legacy\"}}");
    }

    static ItemStack carrier() throws Exception {
        var bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(original(), bytes);
        var payload = new CompoundTag();
        payload.putInt("schema", 1);
        payload.putInt("data_version", 3700);
        var chunks = new ListTag();
        String encoded = Base64.getEncoder().encodeToString(bytes.toByteArray());
        chunks.add(StringTag.valueOf(encoded.substring(0, 16)));
        chunks.add(StringTag.valueOf(encoded.substring(16)));
        payload.put("chunks", chunks);
        var data = new CompoundTag();
        data.put(CarrierSnapshot.KEY, payload);
        data.putString("other_mod_carrier", "retained");
        var carrier = new ItemStack(Items.RECOVERY_COMPASS);
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return carrier;
    }

    private LegacyFixtures() {}
}
