package folk.sisby.inventory_tabs;

import folk.sisby.inventory_tabs.duck.InventoryTabsScreen;
import folk.sisby.inventory_tabs.tabs.BlockTab;
import folk.sisby.inventory_tabs.tabs.EntityTab;
import folk.sisby.inventory_tabs.tabs.ItemTab;
import folk.sisby.inventory_tabs.tabs.PlayerInventoryTab;
import folk.sisby.inventory_tabs.tabs.Tab;
import folk.sisby.inventory_tabs.tabs.VehicleInventoryTab;
import folk.sisby.inventory_tabs.util.RaycastCache;
import folk.sisby.inventory_tabs.util.HandlerSlotUtil;
import folk.sisby.inventory_tabs.util.WidgetPosition;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class TabManager {
    public static final Identifier BUTTONS_TEXTURE = InventoryTabs.id("textures/gui/buttons.png");
    public static final int TAB_WIDTH = 24;
    public static final int TAB_HEIGHT = 21; // Without Inset
    public static final int BUTTON_WIDTH = 10;
    public static final int BUTTON_HEIGHT = 18;

    public static final Map<Identifier, BiFunction<HandledScreen<?>, List<Tab>, Tab>> tabGuessers = new HashMap<>();

    public static Tab nextTab;
    public static HandledScreen<?> currentScreen;
    public static final List<Tab> tabs = new ArrayList<>();
    public static int currentPage = 0;
    public static Tab currentTab;
    public static List<WidgetPosition> tabPositions = new ArrayList<>();
    public static int holdTabCooldown = 0;
    public static boolean enabled = true;
    public static Map<BlockPos, RaycastCache> blockRaycastCache = new HashMap<>();

    public static void initScreen(MinecraftClient client, HandledScreen<?> screen) {
        currentScreen = screen;
        tabPositions = ((InventoryTabsScreen) currentScreen).getTabPositions(TAB_WIDTH);
        if (nextTab == null) {
            nextTab = guessOpenedTab(client, screen);
            finishOpeningScreen(screen.getScreenHandler());
        }
    }

    public static void finishOpeningScreen(ScreenHandler handler) {
        if (nextTab != null) {
            if (currentTab != null && currentTab != nextTab) currentTab.close(MinecraftClient.getInstance().player, MinecraftClient.getInstance().world, handler, MinecraftClient.getInstance().interactionManager);
            HandlerSlotUtil.tryPop(MinecraftClient.getInstance().player, MinecraftClient.getInstance().interactionManager, handler);
            currentTab = nextTab;
            setCurrentPage(tabPositions.isEmpty() ? 0 : tabs.indexOf(nextTab) / tabPositions.size());
            nextTab = null;
        }
    }

    public static void screenDiscarded() {
        if (currentTab != null) {
            currentTab.close(MinecraftClient.getInstance().player, MinecraftClient.getInstance().world, MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.currentScreenHandler : null, MinecraftClient.getInstance().interactionManager);
            currentTab = null;
        }
        nextTab = null;
        currentPage = 0;
    }

    public static void tick(ClientWorld world) {
        blockRaycastCache.values().removeIf(timer -> !timer.validThisTick && timer.ticksInvalid >= InventoryTabs.CONFIG.blockRaycastTimeout);
        blockRaycastCache.values().forEach(RaycastCache::tick);
        if (holdTabCooldown > 0) {
            if (InventoryTabs.NEXT_TAB.isPressed()) {
                holdTabCooldown--;
            } else {
                holdTabCooldown = 0;
            }
        }
        if (tabs.removeIf(t -> t.shouldBeRemoved(world, t == currentTab))) {
            sortTabs();
        }
        TabProviders.REGISTRY.values().forEach(tabProvider -> tabProvider.addAvailableTabs(MinecraftClient.getInstance().player, TabManager::tryAddTab));
        if (currentTab != null && !tabs.contains(currentTab)) currentTab = null;
    }

    public static void openTabImmediate(Tab tab, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, ClientWorld world) {
        nextTab = tab;
        HandlerSlotUtil.push(player, MinecraftClient.getInstance().interactionManager, currentScreen.getScreenHandler(), tab.isInstant());
        player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(currentScreen.getScreenHandler().syncId));
        tab.open(player, world, currentScreen.getScreenHandler(), interactionManager);
        if (tab.isInstant()) { // Instant screens don't have slot updates to wait for, so finish now.
            finishOpeningScreen(currentScreen.getScreenHandler());
        }
    }


    public static void openTab(Tab tab) {
        if (tab != currentTab) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
            ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
            if (player != null && interactionManager != null && networkHandler != null && player.getWorld() instanceof ClientWorld world) {
                if (!tab.shouldBeRemoved(world, false)) {
                    if (tab.isBuffered()) openTabImmediate(new PlayerInventoryTab(), player, interactionManager, world);
                    openTabImmediate(tab, player, interactionManager, world);
                }
            }
        }
    }

    public static Tab guessOpenedTab(MinecraftClient client, HandledScreen<?> screen) {
        World world = client.player.getWorld();
        // "Open Inventory" Guesses
        if (currentScreen instanceof InventoryScreen) return tabs.get(0);
        if (client.player.hasVehicle()) {
            for (Tab tab : tabs) {
                if (tab instanceof VehicleInventoryTab vit) {
                    if (client.player.getVehicle().equals(vit.entity)) {
                        return tab;
                    }
                }
            }
        }
        for (BiFunction<HandledScreen<?>, List<Tab>, Tab> guesser : tabGuessers.values()) {
            Tab guessedTab = guesser.apply(screen, tabs);
            if (guessedTab != null) return guessedTab;
        }
        // Crosshair Guesses
        if (client.crosshairTarget instanceof BlockHitResult result) {
            BlockPos pos = result.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            for (Tab tab : tabs) {
                if (tab instanceof BlockTab bt) {
                    if (pos.equals(bt.pos) || blockEntity == world.getBlockEntity(bt.pos) || bt.multiblockPositions.contains(pos))
                        return tab;
                }
            }
        } else if (client.crosshairTarget instanceof EntityHitResult result) {
            Entity entity = result.getEntity();
            for (Tab tab : tabs) {
                if (tab instanceof EntityTab et) {
                    if (entity.equals(et.entity)) {
                        return tab;
                    }
                }
            }
        }
        // Hand Guesses
        for (int slot : List.of(client.player.getInventory().selectedSlot, PlayerInventory.OFF_HAND_SLOT)) {
            for (Tab tab : tabs) {
                if (tab instanceof ItemTab it) {
                    if (slot == it.slot) {
                        return tab;
                    }
                }
            }
        }
        return null;
    }

    public static void tryAddTab(Tab tab) {
        if (!tabs.contains(tab)) {
            tabs.add(tab);
            sortTabs();
        }
    }

    public static void sortTabs() {
        tabs.sort(Comparator.comparingInt(Tab::getPriority).reversed().thenComparing(t -> t.getHoverText().getString()));
    }

    public static void clearTabs() {
        tabs.clear();
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isLocked()) return true;
        if (isHidden()) return false;
        if (button == 0) {
            if (getPageButton(true).contains((int) mouseX, (int) mouseY)) {
                if (currentPage > 0) {
                    setCurrentPage(currentPage - 1);
                    playClick();
                }
                return true;
            }

            if (getPageButton(false).contains((int) mouseX, (int) mouseY)) {
                if (currentPage < getMaximumPage()) {
                    setCurrentPage(currentPage + 1);
                    playClick();
                }
                return true;
            }

            for (int i = 0; i < Math.min(tabPositions.size(), tabs.size() - currentPage * tabPositions.size()); i++) {
                WidgetPosition pos = tabPositions.get(i);
                Tab tab = tabs.get(currentPage * tabPositions.size() + i);
                if (pos != null && tab != null && tab != currentTab) {
                    if (getTabArea(pos).contains((int) mouseX, (int) mouseY)) {
                        openTab(tab);
                        playClick();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean mouseReleased(double mouseX, double mouseY, int button) {
        return isLocked();
    }

    public static boolean isClickOutsideBounds(double mouseX, double mouseY) {
        return tabPositions.isEmpty() || !getPageButton(true).contains((int) mouseX, (int) mouseY) && !getPageButton(false).contains((int) mouseX, (int) mouseY) && tabPositions.stream().noneMatch(pos -> getTabArea(pos).contains((int) mouseX, (int) mouseY));
    }

    public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (InventoryTabs.TOGGLE_TABS.matchesKey(keyCode, scanCode)) {
            enabled = !enabled;
            if (!enabled) MinecraftClient.getInstance().getToastManager().add(new ControlHintToast(Text.translatable("toast.inventory_tabs.disabled.title").formatted(Formatting.BOLD), InventoryTabs.TOGGLE_TABS));
        }
        if (isHidden() || isLocked()) return false;
        if (holdTabCooldown <= 0 && InventoryTabs.NEXT_TAB.matchesKey(keyCode, scanCode)) {
            holdTabCooldown = InventoryTabs.CONFIG.holdTabCooldown;
            if (Screen.hasShiftDown()) {
                if (tabs.indexOf(currentTab) == 0) {
                    openTab(tabs.get(tabs.size() - 1));
                } else {
                    openTab(tabs.get(tabs.indexOf(currentTab) - 1));
                }
            } else {
                if (tabs.indexOf(currentTab) == tabs.size() - 1) {
                    openTab(tabs.get(0));
                } else {
                    openTab(tabs.get(tabs.indexOf(currentTab) + 1));
                }
            }
            return true;
        }

        return false;
    }

    public static void setCurrentPage(int page) {
        if (page == 0 || tabs.size() >= tabPositions.size()) currentPage = page;
    }

    public static int getMaximumPage() {
        return tabs.size() / (tabPositions.size() + 1);
    }

    public static void render(DrawContext drawContext, double mouseX, double mouseY) {
        if (isHidden()) return;
        for (int i = 0; i < Math.min(tabPositions.size(), tabs.size() - currentPage * tabPositions.size()); i++) {
            WidgetPosition pos = tabPositions.get(i);
            Tab tab = tabs.get(currentPage * tabPositions.size() + i);
            if (pos != null && tab != null) tab.render(drawContext, pos, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY, tab == currentTab);
        }
        if (getMaximumPage() > 0) {
            drawButton(drawContext, mouseX, mouseY, true);
            drawButton(drawContext, mouseX, mouseY, false);
        }
    }

    public static Rect2i getPageButton(boolean left) {
        WidgetPosition pos = tabPositions.get(left ? 0 : tabPositions.size() - 1);
        return new Rect2i(pos.x + (left ? -BUTTON_WIDTH : TAB_WIDTH), pos.y - (pos.up ? BUTTON_HEIGHT : 0), BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    public static Rect2i getTabArea(WidgetPosition pos) {
        return new Rect2i(pos.x, pos.y + (pos.up ? -TAB_HEIGHT : 0), TAB_WIDTH, TAB_HEIGHT);
    }

    public static void drawButton(DrawContext drawContext, double mouseX, double mouseY, boolean left) {
        Rect2i rect = getPageButton(left);
        boolean hovered = rect.contains((int) mouseX, (int) mouseY);
        boolean active = left ? currentPage > 0 : currentPage < getMaximumPage();
        int u = BUTTON_WIDTH * (left ? 0 : 1);
        int v = BUTTON_HEIGHT * (active ? hovered ? 2 : 1 : 0);
        drawContext.drawTexture(RenderLayer::getGuiTextured, BUTTONS_TEXTURE, rect.getX(), rect.getY(), u, v, rect.getWidth(), rect.getHeight(), 256, 256);
        if (hovered) drawContext.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.literal((currentPage + 1) + "/" + (getMaximumPage() + 1)), (int) mouseX, (int) mouseY);
    }

    public static void playClick() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    public static boolean isHidden() {
        return !enabled || currentScreen == null;
    }

    public static boolean isLocked() {
        return nextTab != null;
    }
}


