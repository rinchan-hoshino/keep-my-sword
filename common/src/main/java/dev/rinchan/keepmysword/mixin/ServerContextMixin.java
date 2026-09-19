package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.RegistryContext;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class ServerContextMixin {
    @WrapMethod(method = "runServer")
    private void kms$registryContext(Operation<Void> original) {
        RegistryContext.runServer(
                ((MinecraftServer) (Object) this).registryAccess(), () -> original.call());
    }
}
