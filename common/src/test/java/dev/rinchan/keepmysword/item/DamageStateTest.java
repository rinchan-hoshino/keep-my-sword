package dev.rinchan.keepmysword.item;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DamageStateTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void newToolIsIntact() {
        assertFalse(DamageState.isBroken(tool(0)));
    }

    @Test
    void exhaustedToolIsBroken() {
        var stack = tool(250);
        assertTrue(DamageState.isBroken(stack));
    }

    @Test
    void emptyStackIsIntact() {
        assertFalse(DamageState.isBroken(ItemStack.EMPTY));
    }

    @Test
    void damageIsBounded() {
        var stack = tool(0);
        assertEquals(0, DamageState.clampDamage(stack, -1));
        assertEquals(250, DamageState.clampDamage(stack, Integer.MAX_VALUE));
    }

    @Test
    void nonDamageableValuesRemainUnchanged() {
        assertEquals(10, DamageState.clampDamage(mock(ItemStack.class), 10));
    }

    private static ItemStack tool(int damage) {
        // Only durability inputs matter here; 26.x registry components bind on world load.
        var stack = mock(ItemStack.class);
        when(stack.isDamageableItem()).thenReturn(true);
        when(stack.getMaxDamage()).thenReturn(250);
        when(stack.getDamageValue()).thenReturn(damage);
        return stack;
    }
}
