package dev.rinchan.keepmysword.item;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import net.minecraft.world.item.ItemStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UnscopedSealingTest {
    @BeforeAll
    static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void networkDecodeDoesNotQueryItemsBeforeRegistryAndEnchantmentSync() {
        ItemStack decoded = mock(ItemStack.class);
        assertSame(decoded, SealedItems.seal(decoded));
        verifyNoInteractions(decoded);
    }
}
