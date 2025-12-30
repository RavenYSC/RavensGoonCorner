package com.raven.client.skyblock.gui;

import com.raven.client.skyblock.calculator.ArcherDPSCalculator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * GUI for Hypixel Skyblock Catacombs Archer DPS Calculator
 */
public class GuiArcherDPSCalculator extends GuiScreen {
    
    private ArcherDPSCalculator calculator;
    private DecimalFormat numberFormat = new DecimalFormat("#,##0.00");
    private DecimalFormat intFormat = new DecimalFormat("#,##0");
    
    // Text fields for input
    private GuiTextField baseDamageField;
    private GuiTextField strengthField;
    private GuiTextField critDamageField;
    private GuiTextField critChanceField;
    private GuiTextField attackSpeedField;
    private GuiTextField ferocityField;
    private GuiTextField bonusAttackSpeedField;
    
    // Level fields
    private GuiTextField archeryLevelField;
    private GuiTextField combatLevelField;
    private GuiTextField catacombsLevelField;
    private GuiTextField archerClassLevelField;
    
    // Multiplier fields
    private GuiTextField arrowMultiplierField;
    private GuiTextField bowAbilityField;
    private GuiTextField enchantmentField;
    private GuiTextField reforgeField;
    private GuiTextField armorField;
    private GuiTextField petField;
    private GuiTextField potionField;
    private GuiTextField dungeonField;
    
    // Results
    private double calculatedDPS = 0;
    private double calculatedDamagePerHit = 0;
    
    // Buttons
    private GuiButton calculateButton;
    private GuiButton resetButton;
    
    // Scroll offset for smaller screens
    private int scrollOffset = 0;
    
    public GuiArcherDPSCalculator() {
        this.calculator = new ArcherDPSCalculator();
    }
    
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        
        int leftX = width / 4;
        int rightX = width / 2 + 20;
        int startY = 40;
        int fieldWidth = 100;
        int fieldHeight = 20;
        int spacing = 25;
        
        // Base stats (left column)
        baseDamageField = new GuiTextField(0, fontRendererObj, leftX, startY, fieldWidth, fieldHeight);
        baseDamageField.setText("0");
        baseDamageField.setMaxStringLength(10);
        
        strengthField = new GuiTextField(1, fontRendererObj, leftX, startY + spacing, fieldWidth, fieldHeight);
        strengthField.setText("0");
        strengthField.setMaxStringLength(10);
        
        critDamageField = new GuiTextField(2, fontRendererObj, leftX, startY + spacing * 2, fieldWidth, fieldHeight);
        critDamageField.setText("50");
        critDamageField.setMaxStringLength(10);
        
        critChanceField = new GuiTextField(3, fontRendererObj, leftX, startY + spacing * 3, fieldWidth, fieldHeight);
        critChanceField.setText("30");
        critChanceField.setMaxStringLength(10);
        
        attackSpeedField = new GuiTextField(4, fontRendererObj, leftX, startY + spacing * 4, fieldWidth, fieldHeight);
        attackSpeedField.setText("1.0");
        attackSpeedField.setMaxStringLength(10);
        
        ferocityField = new GuiTextField(5, fontRendererObj, leftX, startY + spacing * 5, fieldWidth, fieldHeight);
        ferocityField.setText("0");
        ferocityField.setMaxStringLength(10);
        
        bonusAttackSpeedField = new GuiTextField(6, fontRendererObj, leftX, startY + spacing * 6, fieldWidth, fieldHeight);
        bonusAttackSpeedField.setText("0");
        bonusAttackSpeedField.setMaxStringLength(10);
        
        // Levels (left column continued)
        archeryLevelField = new GuiTextField(7, fontRendererObj, leftX, startY + spacing * 7, fieldWidth, fieldHeight);
        archeryLevelField.setText("0");
        archeryLevelField.setMaxStringLength(2);
        
        combatLevelField = new GuiTextField(8, fontRendererObj, leftX, startY + spacing * 8, fieldWidth, fieldHeight);
        combatLevelField.setText("0");
        combatLevelField.setMaxStringLength(2);
        
        catacombsLevelField = new GuiTextField(9, fontRendererObj, leftX, startY + spacing * 9, fieldWidth, fieldHeight);
        catacombsLevelField.setText("0");
        catacombsLevelField.setMaxStringLength(2);
        
        archerClassLevelField = new GuiTextField(10, fontRendererObj, leftX, startY + spacing * 10, fieldWidth, fieldHeight);
        archerClassLevelField.setText("0");
        archerClassLevelField.setMaxStringLength(2);
        
        // Multipliers (right column)
        arrowMultiplierField = new GuiTextField(11, fontRendererObj, rightX, startY, fieldWidth, fieldHeight);
        arrowMultiplierField.setText("1.0");
        arrowMultiplierField.setMaxStringLength(10);
        
        bowAbilityField = new GuiTextField(12, fontRendererObj, rightX, startY + spacing, fieldWidth, fieldHeight);
        bowAbilityField.setText("1.0");
        bowAbilityField.setMaxStringLength(10);
        
        enchantmentField = new GuiTextField(13, fontRendererObj, rightX, startY + spacing * 2, fieldWidth, fieldHeight);
        enchantmentField.setText("1.0");
        enchantmentField.setMaxStringLength(10);
        
        reforgeField = new GuiTextField(14, fontRendererObj, rightX, startY + spacing * 3, fieldWidth, fieldHeight);
        reforgeField.setText("1.0");
        reforgeField.setMaxStringLength(10);
        
        armorField = new GuiTextField(15, fontRendererObj, rightX, startY + spacing * 4, fieldWidth, fieldHeight);
        armorField.setText("1.0");
        armorField.setMaxStringLength(10);
        
        petField = new GuiTextField(16, fontRendererObj, rightX, startY + spacing * 5, fieldWidth, fieldHeight);
        petField.setText("1.0");
        petField.setMaxStringLength(10);
        
        potionField = new GuiTextField(17, fontRendererObj, rightX, startY + spacing * 6, fieldWidth, fieldHeight);
        potionField.setText("1.0");
        potionField.setMaxStringLength(10);
        
        dungeonField = new GuiTextField(18, fontRendererObj, rightX, startY + spacing * 7, fieldWidth, fieldHeight);
        dungeonField.setText("1.0");
        dungeonField.setMaxStringLength(10);
        
        // Buttons
        calculateButton = new GuiButton(0, width / 2 - 155, height - 50, 150, 20, "Calculate DPS");
        resetButton = new GuiButton(1, width / 2 + 5, height - 50, 150, 20, "Reset");
        
        buttonList.add(calculateButton);
        buttonList.add(resetButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Title
        String title = "Archer DPS Calculator - Hypixel Skyblock Catacombs";
        drawCenteredString(fontRendererObj, title, width / 2, 15, 0xFFFFFF);
        
        int leftX = width / 4;
        int rightX = width / 2 + 20;
        int startY = 40;
        int labelOffset = -110;
        int spacing = 25;
        
        // Draw labels for left column (Base Stats & Levels)
        drawString(fontRendererObj, "Base Damage:", leftX + labelOffset, startY + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Strength:", leftX + labelOffset, startY + spacing + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Crit Damage (%):", leftX + labelOffset, startY + spacing * 2 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Crit Chance (%):", leftX + labelOffset, startY + spacing * 3 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Attack Speed:", leftX + labelOffset, startY + spacing * 4 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Ferocity:", leftX + labelOffset, startY + spacing * 5 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Bonus AS (%):", leftX + labelOffset, startY + spacing * 6 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Archery Level:", leftX + labelOffset, startY + spacing * 7 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Combat Level:", leftX + labelOffset, startY + spacing * 8 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Catacombs Lvl:", leftX + labelOffset, startY + spacing * 9 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Archer Class Lvl:", leftX + labelOffset, startY + spacing * 10 + 6, 0xFFFFFF);
        
        // Draw labels for right column (Multipliers)
        drawString(fontRendererObj, "Arrow Multiplier:", rightX + labelOffset, startY + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Bow Ability:", rightX + labelOffset, startY + spacing + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Enchantments:", rightX + labelOffset, startY + spacing * 2 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Reforge:", rightX + labelOffset, startY + spacing * 3 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Armor Bonus:", rightX + labelOffset, startY + spacing * 4 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Pet Bonus:", rightX + labelOffset, startY + spacing * 5 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Potion Bonus:", rightX + labelOffset, startY + spacing * 6 + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Dungeon Bonus:", rightX + labelOffset, startY + spacing * 7 + 6, 0xFFFFFF);
        
        // Draw text fields
        baseDamageField.drawTextBox();
        strengthField.drawTextBox();
        critDamageField.drawTextBox();
        critChanceField.drawTextBox();
        attackSpeedField.drawTextBox();
        ferocityField.drawTextBox();
        bonusAttackSpeedField.drawTextBox();
        archeryLevelField.drawTextBox();
        combatLevelField.drawTextBox();
        catacombsLevelField.drawTextBox();
        archerClassLevelField.drawTextBox();
        arrowMultiplierField.drawTextBox();
        bowAbilityField.drawTextBox();
        enchantmentField.drawTextBox();
        reforgeField.drawTextBox();
        armorField.drawTextBox();
        petField.drawTextBox();
        potionField.drawTextBox();
        dungeonField.drawTextBox();
        
        // Draw results
        if (calculatedDPS > 0) {
            int resultY = height - 80;
            drawCenteredString(fontRendererObj, "Results:", width / 2, resultY, 0x55FF55);
            drawCenteredString(fontRendererObj, "DPS: " + intFormat.format(calculatedDPS), width / 2, resultY + 15, 0xFFFF55);
            drawCenteredString(fontRendererObj, "Damage per Hit: " + intFormat.format(calculatedDamagePerHit), width / 2, resultY + 30, 0xFFFF55);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // Calculate
            updateCalculatorValues();
            calculatedDPS = calculator.calculateDPS();
            calculatedDamagePerHit = calculator.calculateDamagePerHit();
        } else if (button.id == 1) { // Reset
            calculator.reset();
            resetFields();
            calculatedDPS = 0;
            calculatedDamagePerHit = 0;
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        
        // Allow typing in text fields
        baseDamageField.textboxKeyTyped(typedChar, keyCode);
        strengthField.textboxKeyTyped(typedChar, keyCode);
        critDamageField.textboxKeyTyped(typedChar, keyCode);
        critChanceField.textboxKeyTyped(typedChar, keyCode);
        attackSpeedField.textboxKeyTyped(typedChar, keyCode);
        ferocityField.textboxKeyTyped(typedChar, keyCode);
        bonusAttackSpeedField.textboxKeyTyped(typedChar, keyCode);
        archeryLevelField.textboxKeyTyped(typedChar, keyCode);
        combatLevelField.textboxKeyTyped(typedChar, keyCode);
        catacombsLevelField.textboxKeyTyped(typedChar, keyCode);
        archerClassLevelField.textboxKeyTyped(typedChar, keyCode);
        arrowMultiplierField.textboxKeyTyped(typedChar, keyCode);
        bowAbilityField.textboxKeyTyped(typedChar, keyCode);
        enchantmentField.textboxKeyTyped(typedChar, keyCode);
        reforgeField.textboxKeyTyped(typedChar, keyCode);
        armorField.textboxKeyTyped(typedChar, keyCode);
        petField.textboxKeyTyped(typedChar, keyCode);
        potionField.textboxKeyTyped(typedChar, keyCode);
        dungeonField.textboxKeyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        baseDamageField.mouseClicked(mouseX, mouseY, mouseButton);
        strengthField.mouseClicked(mouseX, mouseY, mouseButton);
        critDamageField.mouseClicked(mouseX, mouseY, mouseButton);
        critChanceField.mouseClicked(mouseX, mouseY, mouseButton);
        attackSpeedField.mouseClicked(mouseX, mouseY, mouseButton);
        ferocityField.mouseClicked(mouseX, mouseY, mouseButton);
        bonusAttackSpeedField.mouseClicked(mouseX, mouseY, mouseButton);
        archeryLevelField.mouseClicked(mouseX, mouseY, mouseButton);
        combatLevelField.mouseClicked(mouseX, mouseY, mouseButton);
        catacombsLevelField.mouseClicked(mouseX, mouseY, mouseButton);
        archerClassLevelField.mouseClicked(mouseX, mouseY, mouseButton);
        arrowMultiplierField.mouseClicked(mouseX, mouseY, mouseButton);
        bowAbilityField.mouseClicked(mouseX, mouseY, mouseButton);
        enchantmentField.mouseClicked(mouseX, mouseY, mouseButton);
        reforgeField.mouseClicked(mouseX, mouseY, mouseButton);
        armorField.mouseClicked(mouseX, mouseY, mouseButton);
        petField.mouseClicked(mouseX, mouseY, mouseButton);
        potionField.mouseClicked(mouseX, mouseY, mouseButton);
        dungeonField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        baseDamageField.updateCursorCounter();
        strengthField.updateCursorCounter();
        critDamageField.updateCursorCounter();
        critChanceField.updateCursorCounter();
        attackSpeedField.updateCursorCounter();
        ferocityField.updateCursorCounter();
        bonusAttackSpeedField.updateCursorCounter();
        archeryLevelField.updateCursorCounter();
        combatLevelField.updateCursorCounter();
        catacombsLevelField.updateCursorCounter();
        archerClassLevelField.updateCursorCounter();
        arrowMultiplierField.updateCursorCounter();
        bowAbilityField.updateCursorCounter();
        enchantmentField.updateCursorCounter();
        reforgeField.updateCursorCounter();
        armorField.updateCursorCounter();
        petField.updateCursorCounter();
        potionField.updateCursorCounter();
        dungeonField.updateCursorCounter();
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
    
    private void updateCalculatorValues() {
        try {
            calculator.setBaseDamage(parseDouble(baseDamageField.getText()));
            calculator.setStrength(parseDouble(strengthField.getText()));
            calculator.setCritDamage(parseDouble(critDamageField.getText()));
            calculator.setCritChance(parseDouble(critChanceField.getText()));
            calculator.setAttackSpeed(parseDouble(attackSpeedField.getText()));
            calculator.setFerocity(parseDouble(ferocityField.getText()));
            calculator.setBonusAttackSpeed(parseDouble(bonusAttackSpeedField.getText()));
            
            calculator.setArcheryLevel(parseInt(archeryLevelField.getText()));
            calculator.setCombatLevel(parseInt(combatLevelField.getText()));
            calculator.setCatacombsLevel(parseInt(catacombsLevelField.getText()));
            calculator.setArcherClassLevel(parseInt(archerClassLevelField.getText()));
            
            calculator.setArrowDamageMultiplier(parseDouble(arrowMultiplierField.getText()));
            calculator.setBowAbilityMultiplier(parseDouble(bowAbilityField.getText()));
            calculator.setEnchantmentMultiplier(parseDouble(enchantmentField.getText()));
            calculator.setReforgeMultiplier(parseDouble(reforgeField.getText()));
            calculator.setArmorBonusMultiplier(parseDouble(armorField.getText()));
            calculator.setPetBonusMultiplier(parseDouble(petField.getText()));
            calculator.setPotionMultiplier(parseDouble(potionField.getText()));
            calculator.setDungeonMultiplier(parseDouble(dungeonField.getText()));
        } catch (NumberFormatException e) {
            // Keep previous values if parsing fails
        }
    }
    
    private void resetFields() {
        baseDamageField.setText("0");
        strengthField.setText("0");
        critDamageField.setText("50");
        critChanceField.setText("30");
        attackSpeedField.setText("1.0");
        ferocityField.setText("0");
        bonusAttackSpeedField.setText("0");
        archeryLevelField.setText("0");
        combatLevelField.setText("0");
        catacombsLevelField.setText("0");
        archerClassLevelField.setText("0");
        arrowMultiplierField.setText("1.0");
        bowAbilityField.setText("1.0");
        enchantmentField.setText("1.0");
        reforgeField.setText("1.0");
        armorField.setText("1.0");
        petField.setText("1.0");
        potionField.setText("1.0");
        dungeonField.setText("1.0");
    }
    
    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
