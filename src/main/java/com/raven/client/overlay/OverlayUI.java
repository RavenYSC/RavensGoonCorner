package com.raven.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Map;

public class OverlayUI {

    private Minecraft mc;
    private final Map<String, OverlayElement> elements = new HashMap<>();

    private boolean dragging = false;
    private OverlayElement draggingElement = null;
    private int dragOffsetX, dragOffsetY;
    
    private Minecraft getMc() {
        if (mc == null) {
            mc = Minecraft.getMinecraft();
        }
        return mc;
    }

    public OverlayUI() {	 
        // Add overlays
        elements.put("Ping", new OverlayElement("Ping", 20, 20));
        elements.put("FPS", new OverlayElement("FPS", 20, 30));
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        // Skip rendering when on main menu (no world loaded) or when a screen is open
        Minecraft mc = getMc();
        if (mc.theWorld == null || mc.currentScreen != null || event.phase != TickEvent.RenderTickEvent.Phase.END) return;

        try {
            ScaledResolution sr = new ScaledResolution(mc);
            int scaleFactor = sr.getScaleFactor();
            
            // Safe mouse coordinate calculation
            int rawMouseX = Math.max(0, Math.min(mc.displayWidth, Mouse.getX()));
            int rawMouseY = Math.max(0, Math.min(mc.displayHeight, mc.displayHeight - Mouse.getY()));

            int mouseX = rawMouseX / scaleFactor;
            int mouseY = rawMouseY / scaleFactor;

            if (dragging && draggingElement != null) {
                draggingElement.x = mouseX - dragOffsetX;
                draggingElement.y = mouseY - dragOffsetY;
            }

            // Render overlays - push/pop matrix around all rendering
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            try {
                for (OverlayElement element : elements.values()) {
                    element.render(mouseX, mouseY);
                }
            } finally {
                net.minecraft.client.renderer.GlStateManager.popMatrix();
            }
        } catch (Exception e) {
            System.err.println("[OverlayUI] Error in onTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        // Skip when on main menu or when a screen is open
        Minecraft mc = getMc();
        if (mc.theWorld == null || mc.currentScreen != null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();

        int rawMouseX = Mouse.getX();
        int rawMouseY = mc.displayHeight - Mouse.getY();

        int mouseX = rawMouseX / scaleFactor;
        int mouseY = rawMouseY / scaleFactor;

        for (OverlayElement element : elements.values()) {
            if (element.isHovered(mouseX, mouseY)) {
                int dWheel = Mouse.getDWheel();
                if (dWheel > 0) {
                    element.scale = Math.min(5.0f, element.scale + 0.1f);
                    event.setCanceled(true);
                } else if (dWheel < 0) {
                    element.scale = Math.max(0.5f, element.scale - 0.1f);
                    event.setCanceled(true);
                }
            }
        }

        if (Mouse.isButtonDown(0)) {
            if (!dragging) {
                for (OverlayElement element : elements.values()) {
                    if (element.isHovered(mouseX, mouseY)) {
                        dragging = true;
                        draggingElement = element;
                        dragOffsetX = mouseX - element.x;
                        dragOffsetY = mouseY - element.y;
                        break;
                    }
                }
            }
        } else {
            dragging = false;
            draggingElement = null;
        }
    }
}
