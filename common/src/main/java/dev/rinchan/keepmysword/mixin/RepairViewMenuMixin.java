package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rinchan.keepmysword.item.RepairView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;

// AbstractContainerMenu.clicked is the single entry for container clicks, so wrapping it brackets repair-view work with before/after calls.
@Mixin(AbstractContainerMenu.class)
public abstract class RepairViewMenuMixin {

    @WrapMethod(method = "clicked")
    private void kms$workTransaction(int index, int button, @Coerce Enum<?> type, Player player, Operation<Void> original) {
        var menu = (AbstractContainerMenu) (Object) this;
        // ClickType became ContainerInput in 26.x; both keep the same enum operation names.
        RepairView.beforeClick(menu, index, button, type.name(), player);
        try { original.call(index, button, type, player); }
        finally { RepairView.afterClick(menu, player); }
    }
}
