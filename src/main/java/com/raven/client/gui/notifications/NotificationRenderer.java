package com.raven.client.gui.notifications;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class NotificationRenderer {

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        
        // Skip rendering on main menu
        if (mc.theWorld == null) return;
        
        ScaledResolution res = new ScaledResolution(mc);

        List<Notification> notifs = NotificationManager.getActive();
        int y = 5;

        for (Notification n : notifs) {
            float timeAlive = (System.currentTimeMillis() - n.startTime) / (float) n.duration;
            float slide = Math.min(timeAlive * 2, 1f);  // slide in over 0.5s

            int width = mc.fontRendererObj.getStringWidth(n.message) + 20;
            int height = 20;
            int x = (int) (-width + slide * (width + 5));

            drawRect(x, y, x + width, y + height, 0xAA000000);
            mc.fontRendererObj.drawString(n.message, x + 10, y + 6, 0xFFFFFF);

            y += height + 5;
        }
    }

    private void drawRect(int x1, int y1, int x2, int y2, int color) {
        net.minecraft.client.gui.Gui.drawRect(x1, y1, x2, y2, color);
    }
}
