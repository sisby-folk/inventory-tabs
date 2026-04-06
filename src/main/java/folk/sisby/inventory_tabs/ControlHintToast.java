package folk.sisby.inventory_tabs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ControlHintToast implements Toast {
    private static final Identifier TEXTURE = Identifier.of("toast/advancement");
    protected Text title;
    protected Text keyHint;
    protected KeyBinding keyBinding;
    protected int titleWidth;
    protected int hintWidth;
	private Toast.Visibility visibility = Toast.Visibility.HIDE;

    public ControlHintToast(Text title, KeyBinding keybinding) {
        this.title = title;
        this.keyBinding = keybinding;
        keyHint = Text.translatable("toast.inventory_tabs.disabled.key_hint", keyBinding.getBoundKeyLocalizedText().copy().formatted(Formatting.YELLOW)).formatted(Formatting.BLUE);
        titleWidth = MinecraftClient.getInstance().textRenderer.getWidth(title);
        hintWidth = MinecraftClient.getInstance().textRenderer.getWidth(keyHint);
    }

	@Override
	public Visibility getVisibility() {
		return this.visibility;
	}

	@Override
	public void update(ToastManager manager, long elapsedTime) {
		double time = 2000 * manager.getNotificationDisplayTimeMultiplier();

		this.visibility = elapsedTime >= time ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, getWidth(), getHeight());
		context.drawText(textRenderer, title, (getWidth() - titleWidth) / 2, 7, 0xFFFFFF, false);
		context.drawText(textRenderer, keyHint, (getWidth() - hintWidth) / 2, 18, 0xFFFFFF, false);
	}

	@Override
    public int getWidth() {
        return Math.max(titleWidth, hintWidth) + 24;
    }
}
