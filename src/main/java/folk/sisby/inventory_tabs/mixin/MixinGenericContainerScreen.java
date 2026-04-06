package folk.sisby.inventory_tabs.mixin;

import folk.sisby.inventory_tabs.InventoryTabs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GenericContainerScreen.class)
public abstract class MixinGenericContainerScreen extends HandledScreen<GenericContainerScreenHandler> {
    @Shadow @Final private int rows;
    @Shadow @Final private static Identifier TEXTURE;

    public MixinGenericContainerScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

	@ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;<init>(Lnet/minecraft/screen/ScreenHandler;Lnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/text/Text;)V"))
	private static void containerHideTitle(Args args) {
		if (((GenericContainerScreenHandler) args.get(0)).getRows() == 6 && InventoryTabs.CONFIG.compactLargeContainers) {
			args.set(2, Text.empty());
		}
	}

    @Inject(method = "<init>", at = @At("TAIL"))
    public void containerTextHeight(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) {
            this.backgroundHeight -= 30;
        } else {
            this.backgroundHeight -= 2;
            this.playerInventoryTitleY = this.backgroundHeight - 94;
        }
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    public void containerHeader(DrawContext drawContext, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        drawContext.drawTexture(RenderLayer::getGuiTextured, TEXTURE, (this.width - this.backgroundWidth) / 2, (this.height - this.backgroundHeight) / 2, 0, 0, this.backgroundWidth, 7, 256, 256);
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 0), index = 3)
    public int containerY(int original) {
        return original + 7;
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 0), index = 5)
    public float containerV(float original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original + 17;
        return original + 8;
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 0), index = 7)
    public int containerHeight(int original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original - 17;
        return original - 8;
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 1), index = 3)
    public int inventoryY(int original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original - 10;
        return original - 1;
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 1), index = 5)
    public float inventoryV(float original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original + 9;
        return original;
    }

    @ModifyArg(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V", ordinal = 1), index = 7)
    public int inventoryHeight(int original) {
        if (rows == 6 && InventoryTabs.CONFIG.compactLargeContainers) return original - 9;
        return original;
    }
}
