package folk.sisby.inventory_tabs.mixin;

import folk.sisby.inventory_tabs.InventoryTabs;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GenericContainerScreenHandler.class)
public abstract class MixinGenericContainerScreenHandler {
    @Shadow @Final private int rows;

    @ModifyArg(method = "addInventorySlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;<init>(Lnet/minecraft/inventory/Inventory;III)V", ordinal = 0), index = 3)
    public int raiseContainerSlotY(int original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original - 10;
        return original - 1;
    }

    @ModifyArg(method = "<init>(Lnet/minecraft/screen/ScreenHandlerType;ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/inventory/Inventory;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/GenericContainerScreenHandler;addPlayerSlots(Lnet/minecraft/inventory/Inventory;II)V", ordinal = 0), index = 2)
    public int raiseHotbarAndInventorySlotY(int original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original - 19;
        return original - 1;
    }
}
