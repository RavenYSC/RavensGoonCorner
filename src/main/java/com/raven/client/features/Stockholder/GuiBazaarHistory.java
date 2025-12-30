package com.raven.client.features.Stockholder;

import com.raven.client.utils.ChromaText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GuiBazaarHistory extends GuiScreen {

    private Minecraft mc;
    private final Map<String, List<BazaarSnapshot>> history = new HashMap<>();
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    private String selectedItem = null;
    private int scrollOffset = 0;

    @Override
    public void initGui() {
        loadHistory();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(getMc());
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        int listX = 20;
        int listY = 40;
        int listWidth = 120;
        int listHeight = height - 60;

        int detailX = listX + listWidth + 20;
        int detailY = listY;

        ChromaText.drawChromaString("�lTracked Items:", listX, listY - 12, allowUserInput);

        int i = 0;
        for (String item : history.keySet()) {
            int y = listY + i * 12 - scrollOffset;
            if (y >= listY && y < listY + listHeight) {
                drawString(fontRendererObj, (item.equals(selectedItem) ? "> " : "  ") + item, listX, y, 0xAAAAAA);
            }
            i++;
        }

        if (selectedItem != null && history.containsKey(selectedItem)) {
            drawString(fontRendererObj, "�lHistory for: " + selectedItem, detailX, detailY - 12, 0xFFFFFF);

            List<BazaarSnapshot> snapshots = history.get(selectedItem);
            int j = 0;
            for (BazaarSnapshot snap : snapshots) {
                int y = detailY + j * 10 - scrollOffset;
                if (y >= detailY && y < height - 20) {
                    String line = snap.timestamp + " | Buy: " + snap.buyPrice + " | Sell: " + snap.sellPrice +
                                  " | Margin: " + String.format("%.2f", snap.getMarginPercent()) + "%";
                    drawString(fontRendererObj, line, detailX, y, 0xCCCCCC);
                }
                j++;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int listX = 20;
        int listY = 40;

        int i = 0;
        for (String item : history.keySet()) {
            int y = listY + i * 12 - scrollOffset;
            if (mouseX >= listX && mouseX <= listX + 120 && mouseY >= y && mouseY <= y + 10) {
                selectedItem = item;
                break;
            }
            i++;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void loadHistory() {
        history.clear();
        File file = new File(getMc().mcDataDir, "bazaar_snapshots.txt");
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length != 6) continue;

                String id = parts[0].trim();
                String timestamp = parts[1].trim();
                double buy = Double.parseDouble(parts[2].replace("Buy:", "").trim());
                double sell = Double.parseDouble(parts[3].replace("Sell:", "").trim());
                double buyVol = Double.parseDouble(parts[4].replace("BuyVol:", "").trim());
                double sellVol = Double.parseDouble(parts[5].replace("SellVol:", "").trim());

                BazaarSnapshot snap = new BazaarSnapshot(timestamp, buy, sell, buyVol, sellVol);
                history.computeIfAbsent(id, k -> new ArrayList<>()).add(snap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}