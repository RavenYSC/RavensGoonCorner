package com.raven.client.gui;

import com.raven.client.RavenClient;
import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import com.raven.client.utils.AnimatedToggleButton;
import com.raven.client.utils.ChromaText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import com.raven.client.overlay.OverlayUI;


import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RavenGui extends GuiScreen {

    private Minecraft mc;
    private final List<FeatureCategory> categories = Arrays.asList(FeatureCategory.values());
    private FeatureCategory selectedCategory = categories.get(0);

    private final List<Feature> filteredFeatures = new ArrayList<>();
    private final List<AnimatedToggleButton> toggleButtons = new ArrayList<>();
    private final List<Rect> categoryBounds = new ArrayList<>();

    private int featureBoxX, featureBoxY, featureBoxWidth, featureBoxHeight;
    private int rawWidth;
    private int rawHeight;
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    @Override
    public void initGui() {
        Minecraft mc = getMc();
        rawWidth = mc.displayWidth;
        rawHeight = mc.displayHeight;
        updateFilteredFeatures();
        buildToggleButtons();
    }

    private void updateFilteredFeatures() {
        filteredFeatures.clear();
        for (Feature f : RavenClient.featureManager.getAllFeatures()) {
            if (f.getCategory() == selectedCategory) {
                filteredFeatures.add(f);
            }
        }
    }

    private void buildToggleButtons() {
        Minecraft mc = getMc();
        toggleButtons.clear();

        float featureScale = 3.0f; // Same scale used for drawing text
        int y = featureBoxY + 10;

        for (Feature feature : filteredFeatures) {
            // Scale dimensions of the toggle button
            int toggleWidth = (int)(24 * featureScale);
            int toggleHeight = (int)(12 * featureScale);

            // Align vertically with text line
            int rawToggleY = y + (int)((mc.fontRendererObj.FONT_HEIGHT * featureScale - toggleHeight) / 2);

            // Align to the right inside the box
            int rawToggleX = featureBoxX + featureBoxWidth - toggleWidth - 10;

            toggleButtons.add(new AnimatedToggleButton(
                rawToggleX,
                rawToggleY,
                toggleWidth,
                toggleHeight,
                feature
            ));

            // Increment Y for next row
            y += (int)(mc.fontRendererObj.FONT_HEIGHT * featureScale) + 6;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = getMc();
        
        int guiRawWidth = (int) (rawWidth * 0.8);
        int guiRawHeight = (int) (rawHeight * 0.8);
        int guiRawX = (rawWidth - guiRawWidth) / 2;
        int guiRawY = (rawHeight - guiRawHeight) / 2;

        float scale = 1.0f / new ScaledResolution(mc).getScaleFactor(); // inverse of GUI scale
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale); // makes drawing at raw resolution consistent

        // === Background Box ===
        drawRect(guiRawX, guiRawY, guiRawX + guiRawWidth, guiRawY + guiRawHeight, 0xCC1E1E1E);

        // === Title (Chroma) ===
        String title = "1:1 Client";
        float titleScale = 4.0f;
        int titleWidth = mc.fontRendererObj.getStringWidth(title);
        int titleX = guiRawX + (guiRawWidth - (int)(titleWidth * titleScale)) / 2;
        int titleY = guiRawY + 20;

        GL11.glPushMatrix();
        GL11.glScalef(titleScale, titleScale, titleScale);
        ChromaText.drawChromaString(title, (int)(titleX / titleScale), (int)(titleY / titleScale), true);
        GL11.glPopMatrix();

     // === CATEGORY Box + Scaled Text ===
        float categoryScale = 3.0f;

        int longestCategory = categories.stream()
                .mapToInt(c -> mc.fontRendererObj.getStringWidth(c.name()))
                .max().orElse(60);

        int categoryBoxWidth = (int)(longestCategory * categoryScale) + 20;
        int categoryBoxX = guiRawX + 20;
        int categoryBoxY = guiRawY + 80;

        categoryBounds.clear();
        int catY = categoryBoxY;

        for (FeatureCategory cat : categories) {
            String label = cat.name();
            int scaledTextHeight = (int)(mc.fontRendererObj.FONT_HEIGHT * categoryScale);
            int boxHeight = scaledTextHeight + 6;

            GL11.glPushMatrix();
            GL11.glScalef(categoryScale, categoryScale, 1.0f);

            if (cat == selectedCategory) {
                // Draw chroma text for selected
                ChromaText.drawChromaString(
                    label,
                    (int)((categoryBoxX + 5) / categoryScale),
                    (int)((catY + 3) / categoryScale),
                    true
                );
            } else {
                // Draw normal white text
                mc.fontRendererObj.drawString(
                    label,
                    (int)((categoryBoxX + 5) / categoryScale),
                    (int)((catY + 3) / categoryScale),
                    0xFFFFFFFF
                );
            }

            GL11.glPopMatrix();

            categoryBounds.add(new Rect(categoryBoxX, catY, categoryBoxWidth, boxHeight));
            catY += boxHeight + 4;
        }

     // === FEATURE Box + Scaled Text ===
        featureBoxWidth = 800;
        featureBoxHeight = guiRawHeight - 200;
        featureBoxX = guiRawX + guiRawWidth / 2 - featureBoxWidth / 2;
        featureBoxY = guiRawY + 80;

        drawRect(featureBoxX, featureBoxY, featureBoxX + featureBoxWidth, featureBoxY + featureBoxHeight, 0xFF111111);

        float featureScale = 3.0f;
        int y = featureBoxY + 10;

        for (int i = 0; i < filteredFeatures.size(); i++) {
            Feature feature = filteredFeatures.get(i);

            GL11.glPushMatrix();
            GL11.glScalef(featureScale, featureScale, 1.0f);
            mc.fontRendererObj.drawString(
                    feature.getName(),
                    (int)((featureBoxX + 10) / featureScale),
                    (int)(y / featureScale),
                    0xFFFFFF
            );
            GL11.glPopMatrix();

            y += (int)(mc.fontRendererObj.FONT_HEIGHT * featureScale) + 6; // spacing
        }

        for (AnimatedToggleButton toggle : toggleButtons) {
            toggle.draw(mc); // already using raw coordinates
        }

        GL11.glPopMatrix(); // restore original scale
        super.drawScreen(mouseX, mouseY, partialTicks);
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        // Convert scaled mouse coords to raw screen coords
    	int scaleFactor = new ScaledResolution(getMc()).getScaleFactor();
    	int rawMouseX = mouseX * scaleFactor;
    	int rawMouseY = mouseY * scaleFactor;

        // === Category Selection ===
        for (int i = 0; i < categoryBounds.size(); i++) {
            Rect bounds = categoryBounds.get(i);
            if (bounds.contains(rawMouseX, rawMouseY)) {
                selectedCategory = categories.get(i);
                updateFilteredFeatures();
                buildToggleButtons(); // Rebuild toggles for selected category
                return;
            }
        }

        // === Toggle Buttons ===
        for (AnimatedToggleButton toggle : toggleButtons) {
            toggle.handleClick(rawMouseX, rawMouseY); // Raw click position
        }

        super.mouseClicked(mouseX, mouseY, button); // Call base in case others need it
    }

    public static class Rect {
        public int x, y, width, height;
        public Rect(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }

        public boolean contains(int mx, int my) {
            return mx >= x && mx <= x + width &&
                   my >= y && my <= y + height;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
