package folk.sisby.inventory_tabs.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class DrawUtil {
    public static void drawCrunched(DrawContext context, Identifier texture, int x, int y, int width, int height, int regionWidth, int regionHeight, int u, int v, int texWidth, int texHeight) {
        int leftWidth = width / 2 + (width % 2);
        int topHeight = height / 2 + (height % 2);
        int rightWidth = width - leftWidth;
        int bottomHeight = height - topHeight;

        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, leftWidth, topHeight, texWidth, texHeight); // Top Left
        context.drawTexture(RenderLayer::getGuiTextured, texture, x + leftWidth, y, u + regionWidth - rightWidth, v, rightWidth, topHeight, texWidth, texHeight); // Top Right
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y + topHeight, u, v + regionHeight - bottomHeight, leftWidth, bottomHeight, texWidth, texHeight); // Bottom Left
        context.drawTexture(RenderLayer::getGuiTextured, texture, x + leftWidth, y + topHeight, u + regionWidth - rightWidth, v + regionHeight - bottomHeight, rightWidth, bottomHeight, texWidth, texHeight); // Bottom Right
    }
}
