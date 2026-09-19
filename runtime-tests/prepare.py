"""Materialize the same safety scenarios against version-specific API spellings."""

from pathlib import Path
import argparse, shutil, re

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
if a.loader == "neoforge" and not a.minecraft.startswith("26."):
    p = out / "common/java/dev/rinchan/rinlibprobe/MoreSafetyTests.java"
    s = p.read_text().replace(
        "import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;", ""
    )
    start = s.index("        // Same public storage API")
    end = s.index("        // Unfamiliar future data", start)
    s = s[:start] + s[end:]
    p.write_text(s)
    p = out / "common/java/dev/rinchan/rinlibprobe/RuntimeProbe.java"
    s = p.read_text()
    start = s.index("                            var variantOps")
    end = s.index("                            s.saveEverything", start)
    s = s[:start] + s[end:]
    p.write_text(s)
if a.loader == "neoforge":
    p = out / "neoforge/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java"
    s = p.read_text().replace(
        "    public ProbeEntrypoint() {",
        """    public static ProbeEffectItem EFFECT_ITEM;
    public ProbeEntrypoint(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            event.register(net.minecraft.core.registries.Registries.ITEM, helper -> {
                EFFECT_ITEM = new ProbeEffectItem();
                helper.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kms_probe","effect_item"),EFFECT_ITEM);
            });
        });""",
    )
    p.write_text(s)
if a.loader == "neoforge":
    p = out / "unload/java/dev/rinchan/rinlibprobe/RuntimeProbe.java"
    s = (
        p.read_text()
        .replace("import net.fabricmc.loader.api.FabricLoader;", "")
        .replace(
            'FabricLoader.getInstance().isModLoaded("keep_my_sword")',
            'net.neoforged.fml.ModList.get().isLoaded("keep_my_sword")',
        )
    )
    if not a.minecraft.startswith("26."):
        start = s.index("                        var variantOps")
        end = s.index(
            "                        Fixtures.check(\n                                Fixtures.chest(s).getItem(0)",
            start,
        )
        s = s[:start] + s[end:]
        start = s.index("                        var nbtOps")
        end = s.index("                        Fixtures.chest(s).setChanged()", start)
        s = s[:start] + s[end:]
    p.write_text(s)
    p = out / "unload-entry/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java"
    p.write_text(
        'package dev.rinchan.rinlibprobe; @net.neoforged.fml.common.Mod("kms_probe") public final class ProbeEntrypoint { public ProbeEntrypoint() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> new RuntimeProbe().run(event.getServer())); } }'
    )
    shutil.copytree(
        out / "neoforge/resources", out / "unload/resources", dirs_exist_ok=True
    )
if a.loader == "neoforge" and a.minecraft.startswith("26."):
    for p in out.rglob("*.java"):
        s = (
            p.read_text()
            .replace(
                "net.fabricmc.fabric.api.transfer.v1.item.ItemVariant",
                "net.neoforged.neoforge.transfer.item.ItemResource",
            )
            .replace("ItemVariant", "ItemResource")
            .replace("Fabric Transfer", "NeoForge ItemResource")
        )
        if p.name == "MoreSafetyTests.java":
            s = s.replace('.get("item")', '.get("id")')
        p.write_text(s)
if a.minecraft.startswith("26."):
    for p in out.rglob("*.java"):
        s = (
            p.read_text()
            .replace("ResourceLocation", "Identifier")
            .replace("getItemHolder()", "typeHolder()")
            .replace("MobEffects.DAMAGE_BOOST", "MobEffects.STRENGTH")
            .replace("TagParser.parseTag", "TagParser.parseCompoundFully")
        )
        s = re.sub(r'\.getBoolean\(("[^"]+")\)', r".getBooleanOr(\1,false)", s)
        s = re.sub(r'\.getString\(("[^"]+")\)', r'.getStringOr(\1,"")', s)
        s = s.replace(
            ".getCompound(ShellPersistence.KEY)",
            ".getCompoundOrEmpty(ShellPersistence.KEY)",
        ).replace(
            '.getLongArray("shell_longs")', '.getLongArray("shell_longs").orElseThrow()'
        )
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
        if p.name == "MoreSafetyTests.java":
            s = s.replace(
                "        var direct =",
                '\n        var template=net.minecraft.world.item.ItemStackTemplate.CODEC.parse(nbtOps,net.minecraft.world.item.ItemStackTemplate.CODEC.encodeStart(nbtOps,net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(mutated)).getOrThrow()).getOrThrow();\n        var applied=template.withCount(1).apply(net.minecraft.core.component.DataComponentPatch.EMPTY);\n        check(SealedItems.isSealed(applied) && applied.get(DataComponents.CUSTOM_DATA).copyTag().getBooleanOr("third_party_marker",false),"template apply preserves sealed shell metadata");\n        var direct =',
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
