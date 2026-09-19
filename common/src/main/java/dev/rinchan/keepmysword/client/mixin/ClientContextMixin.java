package dev.rinchan.keepmysword.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.RegistryContext;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class ClientContextMixin {
    @WrapMethod(method = "runTick")
    private void kms$registryContext(boolean render, Operation<Void> original) {
        var connection = ((Minecraft) (Object) this).getConnection();
        RegistryContext.call(
                connection == null ? null : connection.registryAccess(),
                () -> {
                    original.call(render);
                    return null;
                });
    }
}
