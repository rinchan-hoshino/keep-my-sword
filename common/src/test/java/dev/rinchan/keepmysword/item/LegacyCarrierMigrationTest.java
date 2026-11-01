package dev.rinchan.keepmysword.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mojang.serialization.DataResult;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.junit.jupiter.api.Test;

class LegacyCarrierMigrationTest {
    @Test
    void subsequentNativeReadsDoNotCallMigration() {
        var registries = mock(RegistryAccess.Frozen.class);
        var level = mock(ServerLevel.class);
        when(level.registryAccess()).thenReturn(registries);
        var payload = new CompoundTag();
        payload.putInt("DataVersion", VersionSupport.dataVersion());
        payload.putString("id", "minecraft:diamond_sword");
        var data = new CompoundTag();
        data.put(CarrierSnapshot.KEY, payload);
        var carrier = mock(ItemStack.class);
        when(carrier.get(DataComponents.CUSTOM_DATA)).thenReturn(CustomData.of(data));
        var recovered = mock(ItemStack.class);

        // World-backed codec behavior is covered by the server/client runtime probes.
        try (var nativeCodec = mockStatic(CarrierSnapshot.class);
                var migration = mockStatic(LegacyCarrierMigration.class)) {
            nativeCodec
                    .when(() -> CarrierSnapshot.decode(payload, registries))
                    .thenReturn(DataResult.success(recovered));
            assertSame(recovered, SealedItems.decodeSnapshot(carrier, level).getOrThrow());
            assertSame(recovered, SealedItems.decodeSnapshot(carrier, level).getOrThrow());
            nativeCodec.verify(() -> CarrierSnapshot.decode(payload, registries), times(2));
            migration.verifyNoInteractions();
        }
        verify(carrier, never()).set(eq(DataComponents.CUSTOM_DATA), any());
    }
}
