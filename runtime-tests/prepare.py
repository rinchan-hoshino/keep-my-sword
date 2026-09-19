"""Materialize common runtime scenarios against the version/loader API spellings."""

from pathlib import Path
import argparse
import shutil
import re

p = argparse.ArgumentParser()
p.add_argument("minecraft")
p.add_argument("loader")
p.add_argument("output", type=Path)
a = p.parse_args()
root = Path(__file__).parent
out = a.output.resolve()
out.mkdir(parents=True, exist_ok=True)
for folder in [
    "common",
    "fabric",
    "neoforge",
    "players-modern",
    "players-26-neoforge",
    "client-modern",
    "client-26.2",
    "unload",
    "unload-entry",
]:
    shutil.copytree(root / folder, out / folder, dirs_exist_ok=True)
for name in ["IndependentStorage.java", "PlatformStorage.java"]:
    shutil.copy2(
        out / "common/java/dev/rinchan/rinlibprobe" / name,
        out / "unload/java/dev/rinchan/rinlibprobe" / name,
    )
if a.loader == "neoforge":
    p = out / "neoforge/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java"
    p.write_text(
        p.read_text().replace(
            "    public ProbeEntrypoint() {",
            """    public static ProbeEffectItem EFFECT_ITEM;
    public ProbeEntrypoint(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            event.register(net.minecraft.core.registries.Registries.ITEM, helper -> {
                EFFECT_ITEM = new ProbeEffectItem();
                helper.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kms_probe","effect_item"), EFFECT_ITEM);
            });
        });""",
        )
    )
    p = out / "unload/java/dev/rinchan/rinlibprobe/RuntimeProbe.java"
    p.write_text(
        p.read_text()
        .replace("import net.fabricmc.loader.api.FabricLoader;", "")
        .replace(
            "FabricLoader.getInstance().isModLoaded(",
            "net.neoforged.fml.ModList.get().isLoaded(",
        )
    )
    p = out / "unload-entry/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java"
    p.write_text(
        'package dev.rinchan.rinlibprobe; @net.neoforged.fml.common.Mod("kms_probe") public final class ProbeEntrypoint { public ProbeEntrypoint() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> new RuntimeProbe().run(event.getServer())); } }'
    )
    shutil.copytree(
        out / "neoforge/resources", out / "unload/resources", dirs_exist_ok=True
    )
    for p in out.rglob("PlatformStorage.java"):
        if a.minecraft.startswith("26."):
            p.write_text(
                p.read_text()
                .replace(
                    "net.fabricmc.fabric.api.transfer.v1.item.ItemVariant",
                    "net.neoforged.neoforge.transfer.item.ItemResource",
                )
                .replace("ItemVariant", "ItemResource")
            )
        else:
            p.write_text(
                "package dev.rinchan.rinlibprobe; public final class PlatformStorage { public static void save(net.minecraft.server.MinecraftServer s, net.minecraft.world.item.ItemStack stack) {} public static void verify(net.minecraft.server.MinecraftServer s) {} }"
            )
if a.minecraft.startswith("26."):
    for p in out.rglob("*.java"):
        s = (
            p.read_text()
            .replace("ResourceLocation", "Identifier")
            .replace("getItemHolder()", "typeHolder()")
            .replace("MobEffects.DAMAGE_BOOST", "MobEffects.STRENGTH")
            .replace("TagParser.parseTag", "TagParser.parseCompoundFully")
        )
        s = re.sub(r'\.getBoolean\(("[^\"]+")\)', r".getBooleanOr(\1,false)", s)
        s = re.sub(r'\.getString\(("[^\"]+")\)', r'.getStringOr(\1,"")', s)
        s = s.replace(
            'data.getCompound("keep_my_sword:sealed")',
            'data.getCompoundOrEmpty("keep_my_sword:sealed")',
        ).replace("chunks.getString(i)", "chunks.getString(i).orElseThrow()")
        s = re.sub(
            r"recipe\.assemble\(crafting,\s*s\.registryAccess\(\)\)",
            "recipe.assemble(crafting)",
            s,
        )
        s = re.sub(
            r"new net\.minecraft\.world\.item\.crafting\.RepairItemRecipe\(\s*net\.minecraft\.world\.item\.crafting\.CraftingBookCategory\.MISC\)",
            "new net.minecraft.world.item.crafting.RepairItemRecipe()",
            s,
        )
        s = s.replace(
            "!(armor.getItem() instanceof ArmorItem)",
            "!armor.has(DataComponents.EQUIPPABLE)",
        )
        s = s.replace(
            "inventoryTick(s.overworld(), player, 0, true)",
            "inventoryTick(s.overworld(), player, EquipmentSlot.MAINHAND)",
        )
        if p.name == "PreviewScreen.java":
            s = (
                s.replace("GuiGraphics", "GuiGraphicsExtractor")
                .replace("void render(", "void extractRenderState(")
                .replace("graphics.drawString(", "graphics.text(")
                .replace("graphics.renderItemDecorations(", "graphics.itemDecorations(")
                .replace("graphics.renderItem(", "graphics.item(")
            )
        if p.name == "ClientProbe.java" and a.minecraft == "26.2":
            s = s.replace("client.setScreen(", "client.gui.setScreen(").replace(
                "client.getMainRenderTarget()", "client.gameRenderer.mainRenderTarget()"
            )
        if p.name == "ProbeEffectItem.java":
            s = (
                s.replace("InteractionResultHolder", "InteractionResult")
                .replace("InteractionResult<ItemStack>", "InteractionResult")
                .replace(
                    "return InteractionResult.success(player.getItemInHand(hand));",
                    "return InteractionResult.SUCCESS;",
                )
            )
            s = s.replace(
                "new Item.Properties().durability(10)",
                'new Item.Properties().durability(10).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.fromNamespaceAndPath("kms_probe","effect_item")))',
            )
            s = s.replace(
                "ItemStack stack, Level level, Entity entity, int slot, boolean selected",
                "ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot",
            )
            s = s.replace(
                "public boolean hurtEnemy(", "public void hurtEnemy("
            ).replace("        return true;", "")
        p.write_text(s)
if a.minecraft == "26.2":
    shutil.copytree(out / "client-26.2", out / "client-modern", dirs_exist_ok=True)
