package com.raven.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.Color;

public class ChromaText {

    public static void drawChromaString(String text, int x, int y, boolean shadow) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRendererObj;
        long time = System.currentTimeMillis();
        int offsetX = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Offset the hue slightly per character
            float hue = ((time + i * 75) % 1000L) / 1000f;
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            String letter = String.valueOf(c);

            if (shadow) {
                font.drawStringWithShadow(letter, x + offsetX, y, rgb);
            } else {
                font.drawString(letter, x + offsetX, y, rgb);
            }

            offsetX += font.getCharWidth(c);
        }
    }
}
