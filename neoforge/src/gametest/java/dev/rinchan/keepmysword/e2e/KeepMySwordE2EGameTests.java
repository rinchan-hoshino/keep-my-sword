package dev.rinchan.keepmysword.e2e;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Packaged-JAR black-box GameTests running in real Minecraft 1.21.1 NeoForge runtime.
 * Calls ZERO internal keep_my_sword classes: validates real Mixin & vanilla behavior end-to-end.
 */
@GameTestHolder(KeepMySwordE2EMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KeepMySwordE2EGameTests {

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swordBreaksIntoSealedStateNeutralizesDamageAndUnsealsInAnvil(GameTestHelper helper) {
        helper.assertTrue(
                ModList.get().isLoaded("keep_my_sword"),
                "Packaged keep_my_sword JAR must be loaded by NeoForge FML");

        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());

        // 1. Equip Diamond Sword with 1 durability remaining
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int maxDamage = sword.getMaxDamage();
        sword.setDamageValue(maxDamage - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);

        // Verify attribute modifiers exist BEFORE breaking
        AtomicInteger intactModifiers = new AtomicInteger();
        sword.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> intactModifiers.incrementAndGet());
        helper.assertTrue(intactModifiers.get() > 0, "Intact diamond sword must expose weapon attribute modifiers");

        // 2. Strike a real Pig in ServerLevel with full attack strength -> durability hits 0
        Pig target1 = helper.spawn(EntityType.PIG, 2, 2, 3);
        float hpBefore1 = target1.getHealth();
        setFullAttackStrength(player);
        player.getAttributes().addTransientAttributeModifiers(sword.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.slot().test(EquipmentSlot.MAINHAND))
                .collect(com.google.common.collect.Multimaps.toMultimap(
                        e -> e.attribute(), e -> e.modifier(), com.google.common.collect.ArrayListMultimap::create)));
        player.attack(target1);
        float damageDealtIntact = hpBefore1 - target1.getHealth();
        helper.assertTrue(
                damageDealtIntact >= 5.0F,
                "Intact diamond sword strike should deal weapon damage (>=5.0), dealt=" + damageDealtIntact);

        // 3. Verify sword is NOT destroyed and is clamped at maxDamage (Sealed/Broken)
        ItemStack heldAfterBreak = player.getMainHandItem();
        helper.assertTrue(!heldAfterBreak.isEmpty(), "Sword must NOT be destroyed when durability reaches 0");
        helper.assertTrue(heldAfterBreak.is(Items.DIAMOND_SWORD), "Held item must remain a diamond sword");
        helper.assertTrue(
                heldAfterBreak.getDamageValue() == maxDamage,
                "Broken sword damage must clamp at maxDamage=" + maxDamage + ", got=" + heldAfterBreak.getDamageValue());

        // 4. Verify attribute modifiers & mining speed are neutralized on broken sword
        AtomicInteger brokenModifiers = new AtomicInteger();
        heldAfterBreak.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> brokenModifiers.incrementAndGet());
        helper.assertTrue(brokenModifiers.get() == 0, "Broken sword must expose zero attribute modifiers");
        helper.assertTrue(
                heldAfterBreak.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) == 1.0F,
                "Broken sword destroy speed must be neutralized to 1.0F");

        // 5. Strike a fresh Pig with the broken sword -> must deal ONLY bare-hand damage (<= 1.5F)
        Pig target2 = helper.spawn(EntityType.PIG, 3, 2, 3);
        float hpBefore2 = target2.getHealth();
        // Reset player's attack damage attribute to base (as equipment refresh does on real players)
        player.getAttributes().removeAttributeModifiers(sword.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.slot().test(EquipmentSlot.MAINHAND))
                .collect(com.google.common.collect.Multimaps.toMultimap(
                        e -> e.attribute(), e -> e.modifier(), com.google.common.collect.ArrayListMultimap::create)));
        setFullAttackStrength(player);
        player.attack(target2);
        float damageDealtBroken = hpBefore2 - target2.getHealth();
        helper.assertTrue(
                damageDealtBroken <= 1.5F,
                "Broken sword strike must deal only bare-hand damage (<=1.5), dealt=" + damageDealtBroken);
        helper.assertTrue(
                !player.getMainHandItem().isEmpty() && player.getMainHandItem().getDamageValue() == maxDamage,
                "Broken sword must stay intact at maxDamage after additional swings");

        // 6. Place a real Anvil and repair the broken sword through real AnvilMenu
        BlockPos anvilRel = new BlockPos(1, 2, 1);
        helper.setBlock(anvilRel, Blocks.ANVIL);
        BlockPos anvilAbs = helper.absolutePos(anvilRel);
        player.giveExperienceLevels(10);

        AnvilMenu anvil = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(level, anvilAbs));
        anvil.getSlot(0).set(heldAfterBreak.copy());
        anvil.getSlot(1).set(new ItemStack(Items.DIAMOND, 2));
        anvil.createResult();

        ItemStack repaired = anvil.getSlot(2).getItem().copy();
        helper.assertTrue(!repaired.isEmpty(), "AnvilMenu must produce a repaired sword from broken sword + diamonds");
        helper.assertTrue(
                repaired.getDamageValue() < maxDamage,
                "Repaired sword damage must be < maxDamage, got=" + repaired.getDamageValue());

        // 7. Verify repaired sword unseals: modifiers restored & full weapon damage restored
        AtomicInteger repairedModifiers = new AtomicInteger();
        repaired.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> repairedModifiers.incrementAndGet());
        helper.assertTrue(repairedModifiers.get() > 0, "Repaired sword must restore weapon attribute modifiers");

        player.setItemInHand(InteractionHand.MAIN_HAND, repaired);
        player.getAttributes().addTransientAttributeModifiers(repaired.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.slot().test(EquipmentSlot.MAINHAND))
                .collect(com.google.common.collect.Multimaps.toMultimap(
                        e -> e.attribute(), e -> e.modifier(), com.google.common.collect.ArrayListMultimap::create)));
        Pig target3 = helper.spawn(EntityType.PIG, 4, 2, 3);
        float hpBefore3 = target3.getHealth();
        setFullAttackStrength(player);
        player.attack(target3);
        float damageDealtRepaired = hpBefore3 - target3.getHealth();
        helper.assertTrue(
                damageDealtRepaired >= 5.0F,
                "Repaired sword must restore full weapon damage (>=5.0), dealt=" + damageDealtRepaired);

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void continuousUseShieldAndBowAutoStopUsingOnZeroDurability(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Shield with 1 durability remaining, actively blocking
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.setDamageValue(shield.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, shield);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "Player must be actively using shield before break");

        // Trigger breaking hurtAndBreak through real vanilla ItemStack API
        shield.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        helper.assertTrue(!shield.isEmpty(), "Shield must not be destroyed on 0 durability");
        helper.assertTrue(
                shield.getDamageValue() == shield.getMaxDamage(),
                "Shield must clamp at maxDamage");
        helper.assertTrue(
                !player.isUsingItem(),
                "Player must automatically stopUsingItem when shield hits 0 durability");

        // Bow already at 0 durability: onUseTick must also force stopUsingItem
        ItemStack bow = new ItemStack(Items.BOW);
        bow.setDamageValue(bow.getMaxDamage());
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        bow.onUseTick(level, player, 20);
        helper.assertTrue(
                !player.isUsingItem(),
                "Broken bow onUseTick must automatically stopUsingItem");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dispenserNullOwnerHurtAndBreakDoesNotBypassProtection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack shears = new ItemStack(Items.SHEARS);
        int maxDamage = shears.getMaxDamage();
        shears.setDamageValue(maxDamage - 1);

        // Exact vanilla 1.21.1 Dispenser ShearsDispenseItemBehavior hurtAndBreak(1, level, null, onBreak) call
        AtomicInteger onBreakInvocations = new AtomicInteger();
        shears.hurtAndBreak(1, level, (LivingEntity) null, item -> onBreakInvocations.incrementAndGet());

        helper.assertTrue(!shears.isEmpty(), "Dispenser hurtAndBreak must NOT destroy shears");
        helper.assertTrue(shears.getCount() == 1, "Shears count must remain 1");
        helper.assertTrue(
                shears.getDamageValue() == maxDamage,
                "Shears damage must clamp at maxDamage=" + maxDamage + ", got=" + shears.getDamageValue());
        helper.assertTrue(
                onBreakInvocations.get() == 0,
                "Destruction callback must not run when protection prevents destruction");

        // Second hit while already broken must also keep shears intact
        shears.hurtAndBreak(5, level, (LivingEntity) null, item -> onBreakInvocations.incrementAndGet());
        helper.assertTrue(!shears.isEmpty() && shears.getDamageValue() == maxDamage,
                "Already-broken shears must remain intact on repeated dispenser hurt calls");

        helper.succeed();
    }

    private static void setFullAttackStrength(Player player) {
        try {
            var field = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            field.setAccessible(true);
            field.setInt(player, 100);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set attackStrengthTicker", e);
        }
    }
}
