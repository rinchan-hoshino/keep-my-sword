package dev.rinchan.keepmysword.item;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DamageStateTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void newToolIsIntact() {
        assertFalse(DamageState.isBroken(new ItemStack(Items.IRON_PICKAXE)));
    }

    @Test
    void exhaustedToolIsBroken() {
        var stack = new ItemStack(Items.IRON_PICKAXE);
        stack.setDamageValue(stack.getMaxDamage());
        assertTrue(DamageState.isBroken(stack));
    }

    @Test
    void emptyStackIsIntact() {
        assertFalse(DamageState.isBroken(ItemStack.EMPTY));
    }

    @Test
    void damageIsBounded() {
        var stack = new ItemStack(Items.IRON_PICKAXE);
        assertEquals(0, DamageState.clampDamage(stack, -1));
        assertEquals(stack.getMaxDamage(), DamageState.clampDamage(stack, Integer.MAX_VALUE));
    }

    @Test
    void nonDamageableValuesRemainUnchanged() {
        assertEquals(10, DamageState.clampDamage(new ItemStack(Items.STONE), 10));
    }
}
