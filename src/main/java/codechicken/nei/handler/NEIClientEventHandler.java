package codechicken.nei.handler;

import codechicken.lib.gui.GuiDraw;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.state.GlStateTracker;
import codechicken.nei.NEIServerConfig;
import codechicken.nei.PlayerSave;
import codechicken.nei.config.KeyBindings;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerObjectHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.guihook.IInputHandler;
import codechicken.nei.network.NEIClientPacketHandler;
import codechicken.nei.util.helper.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.*;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

import static codechicken.nei.NEIClientConfig.canPerformAction;

/**
 * Created by covers1624 on 29/03/2017.
 */
@SideOnly (Side.CLIENT)
public class NEIClientEventHandler {

    public static NEIClientEventHandler INSTANCE = new NEIClientEventHandler();

    public static final LinkedList<IInputHandler> inputHandlers = new LinkedList<>();
    public static final LinkedList<IContainerObjectHandler> objectHandlers = new LinkedList<>();
    public static final LinkedList<IContainerDrawHandler> drawHandlers = new LinkedList<>();
    public static final LinkedList<IContainerTooltipHandler> tooltipHandlers = new LinkedList<>();
    private static List<IContainerTooltipHandler> instanceTooltipHandlers;
    private static GuiScreen lastGui;


    /**
     * Register a new Input handler;
     *
     * @param handler The handler to register
     */
    public static void addInputHandler(IInputHandler handler) {

        inputHandlers.add(handler);
    }

    /**
     * Register a new Tooltip render handler;
     *
     * @param handler The handler to register
     */
    public static void addTooltipHandler(IContainerTooltipHandler handler) {
        tooltipHandlers.add(handler);
    }

    /**
     * Register a new Drawing handler;
     *
     * @param handler The handler to register
     */
    public static void addDrawHandler(IContainerDrawHandler handler) {
        drawHandlers.add(handler);
    }

    /**
     * Register a new Object handler;
     *
     * @param handler The handler to register
     */
    public static void addObjectHandler(IContainerObjectHandler handler) {
        objectHandlers.add(handler);
    }

    private NEIClientEventHandler() {
    }

    public void init() {
        GuiDraw.addContainerForegroundHook(INSTANCE::foregroundRenderEvent);
    }

    @SubscribeEvent
    public void onKeyTypedPre(KeyboardInputEvent.Pre event) {

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiContainer) {
            char c = Keyboard.getEventCharacter();
            int eventKey = Keyboard.getEventKey();

            if (eventKey == 0 && c >= 32 || Keyboard.getEventKeyState()) {
                for (IInputHandler handler : inputHandlers) {
                    handler.onKeyTyped(gui, c, eventKey);//TODO, do we want canceled events to appear here?
                }
                for (IInputHandler handler : inputHandlers) {
                    if (handler.keyTyped(gui, c, eventKey)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyTypedPost(KeyboardInputEvent.Post event) {

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiContainer) {
            char c = Keyboard.getEventCharacter();
            int eventKey = Keyboard.getEventKey();

            if (eventKey == 0 && c >= 32 || Keyboard.getEventKeyState()) {

                if (eventKey != 1) {
                    for (IInputHandler inputhander : inputHandlers) {
                        if (inputhander.lastKeyTyped(gui, c, eventKey)) {
                            event.setCanceled(true);
                            return;
                        }
                    }
                }

                if (KeyBindings.get("nei.options.keys.gui.enchant").isActiveAndMatches(eventKey) && canPerformAction("enchant")) {
                    NEIClientPacketHandler.sendOpenEnchantmentWindow();
                    event.setCanceled(true);
                }
                if (KeyBindings.get("nei.options.keys.gui.potion").isActiveAndMatches(eventKey) && canPerformAction("potion")) {
                    NEIClientPacketHandler.sendOpenPotionWindow();
                    event.setCanceled(true);
                }
            }
        }

    }

    @SubscribeEvent
    public void onMouseEventPre(MouseInputEvent.Pre event) {

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiContainer) {
            Point mousePos = GuiDraw.getMousePosition();
            int mouseButton = Mouse.getEventButton();

            if (Mouse.getEventButtonState()) {
                gui.lastMouseEvent = Minecraft.getSystemTime();
                for (IInputHandler handler : inputHandlers) {
                    handler.onMouseClicked(gui, mousePos.x, mousePos.y, mouseButton);
                }
                for (IInputHandler handler : inputHandlers) {
                    if (handler.mouseClicked(gui, mousePos.x, mousePos.y, mouseButton)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            } else if (mouseButton != -1) {
                for (IInputHandler inputhander : inputHandlers) {
                    inputhander.onMouseUp(gui, mousePos.x, mousePos.y, mouseButton);
                }
            } else if (mouseButton != -1 && gui.lastMouseEvent > 0) {
                long heldTime = Minecraft.getSystemTime() - gui.lastMouseEvent;
                for (IInputHandler inputHandler : inputHandlers) {
                    inputHandler.onMouseDragged(gui, mousePos.x, mousePos.y, mouseButton, heldTime);
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseEventPost(MouseInputEvent.Post event) {

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiContainer) {
            int i = Mouse.getDWheel();

            if (i != 0) {
                int scrolled = i > 0 ? 1 : -1;
                Point mousePos = GuiDraw.getMousePosition();
                for (IInputHandler handler : inputHandlers) {
                    if (handler.mouseScrolled(gui, mousePos.x, mousePos.y, scrolled)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }

    }

    @SubscribeEvent
    public void potionShiftEvent(PotionShiftEvent event) {

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void containerInitEvent(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (event.getGui() instanceof GuiContainer) {
            GuiContainer container = ((GuiContainer) event.getGui());
            for (IContainerObjectHandler objectHandler : objectHandlers) {
                objectHandler.load(container);
            }
        }
    }

    @SubscribeEvent
    public void guiOpenEvent(GuiOpenEvent event) {
        if (lastGui != event.getGui()) {
            if (event.getGui() == null) {
                instanceTooltipHandlers = null;
            } else {
                instanceTooltipHandlers = new LinkedList<>();
                if (event.getGui() instanceof IContainerTooltipHandler) {
                    instanceTooltipHandlers.add(((IContainerTooltipHandler) event.getGui()));
                }
                instanceTooltipHandlers.addAll(tooltipHandlers);
            }
            lastGui = event.getGui();
        }
    }

    @SubscribeEvent
    public void clientTickEvent(TickEvent.ClientTickEvent event) {

        if (event.phase == Phase.START) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen != null && minecraft.currentScreen instanceof GuiContainer) {
                for (IContainerObjectHandler objectHandler : objectHandlers) {
                    objectHandler.guiTick(((GuiContainer) minecraft.currentScreen));
                }
            }
        }
    }

    @SubscribeEvent
    public void drawScreenPost(DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();

        Point mousePos = GuiDraw.getMousePosition();
        List<String> tooltip = new LinkedList<>();
        ItemStack stack = null;
        if (instanceTooltipHandlers != null) {
            for (IContainerTooltipHandler handler : instanceTooltipHandlers) {
                handler.handleTooltip(screen, mousePos.x, mousePos.y, tooltip);
            }
        }


        if (screen instanceof GuiContainer) {
            if (tooltip.isEmpty() && GuiHelper.shouldShowTooltip(screen)) {
                GuiContainer container = ((GuiContainer) screen);
                stack = GuiHelper.getStackMouseOver(container, false);

                if (!stack.isEmpty()) {
                    tooltip = GuiHelper.itemDisplayNameMultiline(stack, container, false);
                }

                //for (IContainerTooltipHandler handler : instanceTooltipHandlers) {
                //    handler.handleItemDisplayName(screen, stack, tooltip);
                //}
            }
        }

        GuiDraw.drawMultiLineTip(stack, mousePos.x + 10, mousePos.y - 12, tooltip);
    }

    public void foregroundRenderEvent(GuiContainer container) {
        GlStateTracker.pushState();
        Point mousePos = GuiDraw.getMousePosition();

        GlStateManager.translate(-container.getGuiLeft(), -container.getGuiTop(), 200F);
        for (IContainerDrawHandler drawHandler : drawHandlers) {
            drawHandler.renderObjects(container, mousePos.x, mousePos.y);
        }

        for (IContainerDrawHandler drawHandler : drawHandlers) {
            drawHandler.postRenderObjects(container, mousePos.x, mousePos.y);
        }

        GlStateManager.translate(container.getGuiLeft(), container.getGuiTop(), -200F);
        GuiHelper.enable3DRender();

        GlStateManager.pushMatrix();
        for (Slot slot : container.inventorySlots.inventorySlots) {
            GlStateTracker.pushState();
            for (IContainerDrawHandler drawHandler : drawHandlers) {
                drawHandler.renderSlotOverlay(container, slot);
            }
            GlStateTracker.popState();
        }
        GlStateManager.popMatrix();

        GlStateTracker.popState();
    }

    @SubscribeEvent
    public void tooltipPreEvent(RenderTooltipEvent.Pre event) {
        event.setY(MathHelper.clip(event.getY(), 8, event.getScreenHeight() - 8));
    }

    @SubscribeEvent
    public void itemTooltipEvent(ItemTooltipEvent event) {

        if (instanceTooltipHandlers != null && Minecraft.getMinecraft().currentScreen != null) {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;

            for (IContainerObjectHandler handler : objectHandlers) {
                if (!handler.shouldShowTooltip(screen)) {
                    event.setCanceled(true);
                    return;
                }
            }

            Point mousePos = GuiDraw.getMousePosition();

            for (IContainerTooltipHandler handler : instanceTooltipHandlers) {
                handler.handleTooltip(screen, mousePos.x, mousePos.y, event.getToolTip());
            }
            if (screen instanceof GuiContainer) {
                GuiContainer container = ((GuiContainer) screen);
                for (IContainerTooltipHandler handler : instanceTooltipHandlers) {
                    handler.handleItemDisplayName(screen, GuiHelper.getStackMouseOver(container, true), event.getToolTip());
                }
            }
        }
    }

}
