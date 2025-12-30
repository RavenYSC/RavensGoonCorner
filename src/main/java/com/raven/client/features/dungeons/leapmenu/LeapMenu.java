package com.raven.client.features.dungeons.leapmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class LeapMenu extends GuiScreen {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final int radius = 80;

    // 0 = Top-right, 1 = Top-left, 2 = Bottom-left, 3 = Bottom-right
    private final String[] quadrantTargets = new String[4];
    private final String[] quadrantRoles = new String[4];

    @Override
    public void initGui() {
        List<String> teammates = getPartyMembers();
        List<String> roles = getPartyRoles();

        for (int i = 0; i < 4; i++) {
            quadrantTargets[i] = i < teammates.size() ? teammates.get(i) : null;
            quadrantRoles[i] = i < roles.size() ? roles.get(i) : "Unknown";
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;
        int centerY = height / 2;

        drawWheel(centerX, centerY);

        // Hover detection
        int hoveredQuadrant = -1;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= radius) {
            double angle = Math.atan2(dy, dx);
            hoveredQuadrant = getQuadrantFromAngle(angle);
        }

        for (int i = 0; i < 4; i++) {
            String name = quadrantTargets[i];
            if (name == null) continue;

            int[] pos = getQuadrantPosition(centerX, centerY, radius / 2, i);
            int x = pos[0], y = pos[1];

            int roleColor = getRoleColor(quadrantRoles[i]);
            boolean isHovered = i == hoveredQuadrant;

            if (isHovered) {
                drawCircleFilled(x + 16, y + 16, 22, 0x88FFFFFF); // white glow
            }

            drawCircleFilled(x + 16, y + 16, 18, roleColor); // role background
            drawPlayerHead(x, y, name);
        }

        drawCenteredString(fontRendererObj, "Click To Leap", centerX, centerY + radius + 25, 0xFFFFFF);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        int cx = width / 2, cy = height / 2;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > radius) return;

        double angle = Math.atan2(dy, dx);
        int clickedQuadrant = getQuadrantFromAngle(angle);

        String target = quadrantTargets[clickedQuadrant];
        if (target != null) {
            mc.thePlayer.sendChatMessage("/leap " + target);
            com.raven.client.gui.GuiOpener.openGuiNextTick(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // === Drawing ===

    private void drawWheel(int cx, int cy) {
        for (int deg = 45; deg < 360; deg += 90) {
            double angle = Math.toRadians(deg);
            int x = (int) (cx + radius * Math.cos(angle));
            int y = (int) (cy + radius * Math.sin(angle));
            drawLine(cx, cy, x, y, 0xFFFFFFFF);
        }
        drawCircleOutline(cx, cy, radius, 0xFFFFFFFF);
    }

    private void drawLine(int x1, int y1, int x2, int y2, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2.0F);
        GL11.glColor3f(r, g, b);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawCircleOutline(int cx, int cy, int r, int color) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(red, green, blue);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 360; i++) {
            double angle = Math.toRadians(i);
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            GL11.glVertex2d(x, y);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawCircleFilled(int cx, int cy, int r, int color) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = ((color >> 24) & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            GL11.glVertex2d(x, y);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawPlayerHead(int x, int y, String playerName) {
        ResourceLocation skin = getPlayerSkin(playerName);
        mc.getTextureManager().bindTexture(skin);

        drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, 32, 32, 256, 256); // Face
        drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, 32, 32, 256, 256); // Hat
    }

    private ResourceLocation getPlayerSkin(String name) {
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(name);
        return info != null ? info.getLocationSkin() : DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    private int[] getQuadrantPosition(int cx, int cy, int dist, int quadrant) {
        double angle = Math.toRadians(45 + quadrant * 90);
        int x = (int) (cx + dist * Math.cos(angle)) - 16;
        int y = (int) (cy + dist * Math.sin(angle)) - 16;
        return new int[]{x, y};
    }

    private int getQuadrantFromAngle(double angle) {
        angle = Math.toDegrees(angle);
        if (angle < 0) angle += 360;

        if (angle >= 0 && angle < 90) return 0;      // Top-right
        if (angle >= 90 && angle < 180) return 1;    // Top-left
        if (angle >= 180 && angle < 270) return 2;   // Bottom-left
        return 3;                                    // Bottom-right
    }

    private int getRoleColor(String role) {
        switch (role.toLowerCase()) {
            case "mage": return 0xFF3B6EFF;      // Blue
            case "tank": return 0xFFAAAAAA;      // Grey
            case "healer": return 0xFF4AFF52;    // Green
            case "archer": return 0xFFFF4444;    // Red
            case "berserker": return 0xFFFF8800; // Orange
            default: return 0xFFFFFFFF;
        }
    }

    private List<String> getPartyMembers() {
        List<String> dummy = new ArrayList<>();
        dummy.add("TankMan");
        dummy.add("HealerGirl");
        dummy.add("MageDude");
        dummy.add("BerserkBoy");
        return dummy;
    }

    private List<String> getPartyRoles() {
        List<String> dummy = new ArrayList<>();
        dummy.add("Tank");
        dummy.add("Healer");
        dummy.add("Mage");
        dummy.add("Berserker");
        return dummy;
    }
}