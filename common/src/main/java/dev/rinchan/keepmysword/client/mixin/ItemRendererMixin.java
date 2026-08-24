package dev.rinchan.keepmysword.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    private static final ThreadLocal<ItemDisplayContext> keepMySword$renderContext = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> keepMySword$renderStack = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void keepMySword$rememberRenderContext(
        ItemStack stack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        BakedModel model,
        CallbackInfo ci
    ) {
        keepMySword$renderContext.set(displayContext);
        keepMySword$renderStack.set(stack);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void keepMySword$clearRenderContext(
        ItemStack stack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        BakedModel model,
        CallbackInfo ci
    ) {
        keepMySword$renderContext.remove();
        keepMySword$renderStack.remove();
    }

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"
        ),
        require = 0
    )
    private void keepMySword$tintBrokenNonGuiModelFabric(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        BakedQuad quad,
        float red,
        float green,
        float blue,
        float alpha,
        int combinedLight,
        int combinedOverlay
    ) {
        keepMySword$putTintedBulkData(consumer, pose, quad, red, green, blue, alpha, combinedLight, combinedOverlay);
    }

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
        ),
        require = 0
    )
    private void keepMySword$tintBrokenNonGuiModelNeoForge(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        BakedQuad quad,
        float red,
        float green,
        float blue,
        float alpha,
        int combinedLight,
        int combinedOverlay,
        boolean readExistingColor
    ) {
        keepMySword$putTintedBulkData(consumer, pose, quad, red, green, blue, alpha, combinedLight, combinedOverlay);
    }

    private void keepMySword$putTintedBulkData(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        BakedQuad quad,
        float red,
        float green,
        float blue,
        float alpha,
        int combinedLight,
        int combinedOverlay
    ) {
        ItemStack stack = keepMySword$renderStack.get();
        ItemDisplayContext context = keepMySword$renderContext.get();
        if (stack != null && KeepMySword.isManagedBroken(stack) && context != ItemDisplayContext.GUI) {
            red = DamageState.BROKEN_SURFACE_RED;
            green *= DamageState.BROKEN_SURFACE_GREEN;
            blue *= DamageState.BROKEN_SURFACE_BLUE;
        }
        consumer.putBulkData(pose, quad, red, green, blue, alpha, combinedLight, combinedOverlay);
    }
}
