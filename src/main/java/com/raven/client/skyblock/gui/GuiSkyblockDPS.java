package com.raven.client.skyblock.gui;

import com.raven.client.skyblock.calculator.SkyblockDPSCalculator;
import com.raven.client.skyblock.calculator.SkyblockDPSCalculator.DungeonClass;
import com.raven.client.skyblock.calculator.SkyblockDPSCalculator.EnemyType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * GUI for Hypixel Skyblock Catacombs DPS Calculator
 * Supports Archer, Berserker, and Mage with accurate damage formulas
 */
public class GuiSkyblockDPS extends GuiScreen {
    
    private SkyblockDPSCalculator calculator;
    private DecimalFormat intFormat = new DecimalFormat("#,##0");
    private DecimalFormat decFormat = new DecimalFormat("#,##0.00");
    
    // Current class selection
    private DungeonClass selectedClass = DungeonClass.ARCHER;
    
    // Text fields - Base Stats
    private GuiTextField weaponDamageField;
    private GuiTextField baseStrengthField;
    private GuiTextField baseCritDamageField;
    private GuiTextField baseCritChanceField;
    private GuiTextField baseIntelligenceField;
    
    // Text fields - Additive Bonuses
    private GuiTextField additiveStrength1Field;
    private GuiTextField additiveStrength2Field;
    private GuiTextField additiveCritDmg1Field;
    private GuiTextField additiveCritDmg2Field;
    private GuiTextField additiveInt1Field;
    private GuiTextField additiveInt2Field;
    
    // Text fields - Multiplicative Bonuses
    private GuiTextField multStrengthField;
    private GuiTextField multCritDmgField;
    private GuiTextField multIntelligenceField;
    
    // Text fields - Combat Stats
    private GuiTextField additiveDamageBonusField;
    private GuiTextField enemyDefenseField;
    private GuiTextField attackSpeedField;
    private GuiTextField ferocityField;
    
    // Text fields - Dungeon Stats
    private GuiTextField catacombsLevelField;
    private GuiTextField masterStarsField;
    private GuiTextField secretDigitField;
    
    // Buttons
    private GuiButton archerButton;
    private GuiButton bersButton;
    private GuiButton mageButton;
    private GuiButton hyperionToggle;
    private GuiButton calculateButton;
    private GuiButton resetButton;
    
    // Results
    private double witherDPS = 0;
    private double dragDPS = 0;
    private double weightedDPS = 0;
    private double witherDamage = 0;
    private double dragDamage = 0;
    
    public GuiSkyblockDPS() {
        this.calculator = new SkyblockDPSCalculator();
    }
    
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        
        int col1 = 30;
        int col2 = width / 3 + 10;
        int col3 = 2 * width / 3 - 20;
        int startY = 50;
        int fieldWidth = 80;
        int fieldHeight = 16;
        int spacing = 20;
        
        // ===== Column 1: Base Stats =====
        weaponDamageField = createField(0, col1, startY, fieldWidth, fieldHeight, "0");
        baseStrengthField = createField(1, col1, startY + spacing, fieldWidth, fieldHeight, "0");
        baseCritDamageField = createField(2, col1, startY + spacing * 2, fieldWidth, fieldHeight, "50");
        baseCritChanceField = createField(3, col1, startY + spacing * 3, fieldWidth, fieldHeight, "30");
        baseIntelligenceField = createField(4, col1, startY + spacing * 4, fieldWidth, fieldHeight, "0");
        
        // Additive bonuses
        additiveStrength1Field = createField(5, col1, startY + spacing * 6, fieldWidth, fieldHeight, "0");
        additiveStrength2Field = createField(6, col1, startY + spacing * 7, fieldWidth, fieldHeight, "0");
        additiveCritDmg1Field = createField(7, col1, startY + spacing * 8, fieldWidth, fieldHeight, "0");
        additiveCritDmg2Field = createField(8, col1, startY + spacing * 9, fieldWidth, fieldHeight, "0");
        additiveInt1Field = createField(9, col1, startY + spacing * 10, fieldWidth, fieldHeight, "0");
        additiveInt2Field = createField(10, col1, startY + spacing * 11, fieldWidth, fieldHeight, "0");
        
        // ===== Column 2: Multipliers & Combat =====
        multStrengthField = createField(11, col2, startY, fieldWidth, fieldHeight, "1.0");
        multCritDmgField = createField(12, col2, startY + spacing, fieldWidth, fieldHeight, "1.0");
        multIntelligenceField = createField(13, col2, startY + spacing * 2, fieldWidth, fieldHeight, "1.0");
        
        additiveDamageBonusField = createField(14, col2, startY + spacing * 4, fieldWidth, fieldHeight, "0");
        enemyDefenseField = createField(15, col2, startY + spacing * 5, fieldWidth, fieldHeight, "0");
        attackSpeedField = createField(16, col2, startY + spacing * 6, fieldWidth, fieldHeight, "1.0");
        ferocityField = createField(17, col2, startY + spacing * 7, fieldWidth, fieldHeight, "0");
        
        // Dungeon stats
        catacombsLevelField = createField(18, col2, startY + spacing * 9, fieldWidth, fieldHeight, "50");
        masterStarsField = createField(19, col2, startY + spacing * 10, fieldWidth, fieldHeight, "0");
        secretDigitField = createField(20, col2, startY + spacing * 11, fieldWidth, fieldHeight, "0");
        
        // ===== Column 3: Class Selection & Results =====
        int btnY = startY;
        archerButton = new GuiButton(100, col3, btnY, 100, 20, "Archer");
        bersButton = new GuiButton(101, col3, btnY + 25, 100, 20, "Berserker");
        mageButton = new GuiButton(102, col3, btnY + 50, 100, 20, "Mage");
        hyperionToggle = new GuiButton(103, col3, btnY + 80, 100, 20, "Hyperion: OFF");
        
        calculateButton = new GuiButton(200, col3, btnY + 120, 100, 20, "Calculate");
        resetButton = new GuiButton(201, col3, btnY + 145, 100, 20, "Reset");
        
        buttonList.add(archerButton);
        buttonList.add(bersButton);
        buttonList.add(mageButton);
        buttonList.add(hyperionToggle);
        buttonList.add(calculateButton);
        buttonList.add(resetButton);
        
        updateClassButtons();
    }
    
    private GuiTextField createField(int id, int x, int y, int w, int h, String defaultValue) {
        GuiTextField field = new GuiTextField(id, fontRendererObj, x, y, w, h);
        field.setText(defaultValue);
        field.setMaxStringLength(10);
        return field;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Title
        drawCenteredString(fontRendererObj, "Skyblock DPS Calculator - Catacombs", width / 2, 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Class: " + selectedClass.name(), width / 2, 25, 0x55FF55);
        
        int col1 = 30;
        int col2 = width / 3 + 10;
        int col3 = 2 * width / 3 - 20;
        int startY = 50;
        int spacing = 20;
        int labelOffset = 85;
        
        // ===== Column 1 Labels =====
        drawString(fontRendererObj, "--- Base Stats ---", col1, startY - 12, 0xFFFF55);
        drawString(fontRendererObj, "Weapon Dmg:", col1 + labelOffset, startY + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Strength:", col1 + labelOffset, startY + spacing + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Crit Dmg %:", col1 + labelOffset, startY + spacing * 2 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Crit Chance %:", col1 + labelOffset, startY + spacing * 3 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Intelligence:", col1 + labelOffset, startY + spacing * 4 + 4, 0xAAAAAA);
        
        drawString(fontRendererObj, "--- Additive % ---", col1, startY + spacing * 5 + 8, 0xFFFF55);
        drawString(fontRendererObj, "Str Add 1:", col1 + labelOffset, startY + spacing * 6 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Str Add 2:", col1 + labelOffset, startY + spacing * 7 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "CD Add 1:", col1 + labelOffset, startY + spacing * 8 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "CD Add 2:", col1 + labelOffset, startY + spacing * 9 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Int Add 1:", col1 + labelOffset, startY + spacing * 10 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Int Add 2:", col1 + labelOffset, startY + spacing * 11 + 4, 0xAAAAAA);
        
        // ===== Column 2 Labels =====
        drawString(fontRendererObj, "--- Multipliers ---", col2, startY - 12, 0xFFFF55);
        drawString(fontRendererObj, "Str Mult:", col2 + labelOffset, startY + 4, 0xAAAAAA);
        drawString(fontRendererObj, "CD Mult:", col2 + labelOffset, startY + spacing + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Int Mult:", col2 + labelOffset, startY + spacing * 2 + 4, 0xAAAAAA);
        
        drawString(fontRendererObj, "--- Combat ---", col2, startY + spacing * 3 + 8, 0xFFFF55);
        drawString(fontRendererObj, "Add Dmg %:", col2 + labelOffset, startY + spacing * 4 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Enemy Def:", col2 + labelOffset, startY + spacing * 5 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Atk Speed:", col2 + labelOffset, startY + spacing * 6 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Ferocity:", col2 + labelOffset, startY + spacing * 7 + 4, 0xAAAAAA);
        
        drawString(fontRendererObj, "--- Dungeon ---", col2, startY + spacing * 8 + 8, 0xFFFF55);
        drawString(fontRendererObj, "Cata Lvl:", col2 + labelOffset, startY + spacing * 9 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "M. Stars:", col2 + labelOffset, startY + spacing * 10 + 4, 0xAAAAAA);
        drawString(fontRendererObj, "Secrets:", col2 + labelOffset, startY + spacing * 11 + 4, 0xAAAAAA);
        
        // Draw all text fields
        drawAllFields();
        
        // ===== Column 3: Results =====
        if (weightedDPS > 0) {
            int resultY = startY + 180;
            drawString(fontRendererObj, "--- Results ---", col3, resultY, 0x55FF55);
            drawString(fontRendererObj, "Wither DMG: " + intFormat.format(witherDamage), col3, resultY + 15, 0xFF5555);
            drawString(fontRendererObj, "Dragon DMG: " + intFormat.format(dragDamage), col3, resultY + 30, 0xFF55FF);
            drawString(fontRendererObj, "Wither DPS: " + intFormat.format(witherDPS), col3, resultY + 50, 0xFF5555);
            drawString(fontRendererObj, "Dragon DPS: " + intFormat.format(dragDPS), col3, resultY + 65, 0xFF55FF);
            drawString(fontRendererObj, "Weighted DPS: " + intFormat.format(weightedDPS), col3, resultY + 85, 0xFFFF55);
            
            // Show class weights
            String weights = String.format("(W:%.0f%% D:%.0f%%)", 
                selectedClass.witherWeight * 100, selectedClass.dragWeight * 100);
            drawString(fontRendererObj, weights, col3, resultY + 100, 0x888888);
        }
        
        // Soft cap warning
        if (witherDamage > 1000000 || dragDamage > 1000000) {
            drawCenteredString(fontRendererObj, "! Soft cap applied (90% reduction after 1M)", width / 2, height - 30, 0xFF5555);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawAllFields() {
        weaponDamageField.drawTextBox();
        baseStrengthField.drawTextBox();
        baseCritDamageField.drawTextBox();
        baseCritChanceField.drawTextBox();
        baseIntelligenceField.drawTextBox();
        additiveStrength1Field.drawTextBox();
        additiveStrength2Field.drawTextBox();
        additiveCritDmg1Field.drawTextBox();
        additiveCritDmg2Field.drawTextBox();
        additiveInt1Field.drawTextBox();
        additiveInt2Field.drawTextBox();
        multStrengthField.drawTextBox();
        multCritDmgField.drawTextBox();
        multIntelligenceField.drawTextBox();
        additiveDamageBonusField.drawTextBox();
        enemyDefenseField.drawTextBox();
        attackSpeedField.drawTextBox();
        ferocityField.drawTextBox();
        catacombsLevelField.drawTextBox();
        masterStarsField.drawTextBox();
        secretDigitField.drawTextBox();
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 100: // Archer
                selectedClass = DungeonClass.ARCHER;
                calculator.setDungeonClass(DungeonClass.ARCHER);
                updateClassButtons();
                break;
            case 101: // Berserker
                selectedClass = DungeonClass.BERSERKER;
                calculator.setDungeonClass(DungeonClass.BERSERKER);
                updateClassButtons();
                break;
            case 102: // Mage
                selectedClass = DungeonClass.MAGE;
                calculator.setDungeonClass(DungeonClass.MAGE);
                updateClassButtons();
                break;
            case 103: // Hyperion toggle
                calculator.setHasHyperion(!calculator.hasHyperion());
                hyperionToggle.displayString = "Hyperion: " + (calculator.hasHyperion() ? "ON" : "OFF");
                break;
            case 200: // Calculate
                updateCalculatorValues();
                calculateResults();
                break;
            case 201: // Reset
                resetAll();
                break;
        }
    }
    
    private void updateClassButtons() {
        archerButton.displayString = (selectedClass == DungeonClass.ARCHER ? "> " : "") + "Archer";
        bersButton.displayString = (selectedClass == DungeonClass.BERSERKER ? "> " : "") + "Berserker";
        mageButton.displayString = (selectedClass == DungeonClass.MAGE ? "> " : "") + "Mage";
        
        // Hyperion only relevant for Mage
        hyperionToggle.enabled = (selectedClass == DungeonClass.MAGE);
    }
    
    private void updateCalculatorValues() {
        try {
            calculator.setWeaponDamage(parseDouble(weaponDamageField.getText()));
            calculator.setBaseStrength(parseDouble(baseStrengthField.getText()));
            calculator.setBaseCritDamage(parseDouble(baseCritDamageField.getText()));
            calculator.setBaseCritChance(parseDouble(baseCritChanceField.getText()));
            calculator.setBaseIntelligence(parseDouble(baseIntelligenceField.getText()));
            
            calculator.setAdditiveStrength1(parseDouble(additiveStrength1Field.getText()));
            calculator.setAdditiveStrength2(parseDouble(additiveStrength2Field.getText()));
            calculator.setAdditiveCritDamage1(parseDouble(additiveCritDmg1Field.getText()));
            calculator.setAdditiveCritDamage2(parseDouble(additiveCritDmg2Field.getText()));
            calculator.setAdditiveIntelligence1(parseDouble(additiveInt1Field.getText()));
            calculator.setAdditiveIntelligence2(parseDouble(additiveInt2Field.getText()));
            
            calculator.setMultiplicativeStrength(parseDouble(multStrengthField.getText()));
            calculator.setMultiplicativeCritDamage(parseDouble(multCritDmgField.getText()));
            calculator.setMultiplicativeIntelligence(parseDouble(multIntelligenceField.getText()));
            
            calculator.setAdditiveDamageBonus(parseDouble(additiveDamageBonusField.getText()));
            calculator.setEnemyDefense(parseDouble(enemyDefenseField.getText()));
            calculator.setAttackSpeed(parseDouble(attackSpeedField.getText()));
            calculator.setFerocity(parseDouble(ferocityField.getText()));
            
            calculator.setCatacombsLevel(parseInt(catacombsLevelField.getText()));
            calculator.setMasterStars(parseInt(masterStarsField.getText()));
            calculator.setSecretDigit(parseInt(secretDigitField.getText()));
        } catch (NumberFormatException e) {
            // Keep previous values
        }
    }
    
    private void calculateResults() {
        witherDamage = calculator.calculateDamageForClass(EnemyType.WITHER);
        dragDamage = calculator.calculateDamageForClass(EnemyType.DRAGON);
        witherDPS = calculator.calculateDPS(EnemyType.WITHER);
        dragDPS = calculator.calculateDPS(EnemyType.DRAGON);
        weightedDPS = calculator.calculateWeightedDPS();
    }
    
    private void resetAll() {
        calculator.reset();
        selectedClass = DungeonClass.ARCHER;
        calculator.setDungeonClass(DungeonClass.ARCHER);
        
        weaponDamageField.setText("0");
        baseStrengthField.setText("0");
        baseCritDamageField.setText("50");
        baseCritChanceField.setText("30");
        baseIntelligenceField.setText("0");
        additiveStrength1Field.setText("0");
        additiveStrength2Field.setText("0");
        additiveCritDmg1Field.setText("0");
        additiveCritDmg2Field.setText("0");
        additiveInt1Field.setText("0");
        additiveInt2Field.setText("0");
        multStrengthField.setText("1.0");
        multCritDmgField.setText("1.0");
        multIntelligenceField.setText("1.0");
        additiveDamageBonusField.setText("0");
        enemyDefenseField.setText("0");
        attackSpeedField.setText("1.0");
        ferocityField.setText("0");
        catacombsLevelField.setText("50");
        masterStarsField.setText("0");
        secretDigitField.setText("0");
        
        hyperionToggle.displayString = "Hyperion: OFF";
        updateClassButtons();
        
        witherDPS = 0;
        dragDPS = 0;
        weightedDPS = 0;
        witherDamage = 0;
        dragDamage = 0;
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        
        weaponDamageField.textboxKeyTyped(typedChar, keyCode);
        baseStrengthField.textboxKeyTyped(typedChar, keyCode);
        baseCritDamageField.textboxKeyTyped(typedChar, keyCode);
        baseCritChanceField.textboxKeyTyped(typedChar, keyCode);
        baseIntelligenceField.textboxKeyTyped(typedChar, keyCode);
        additiveStrength1Field.textboxKeyTyped(typedChar, keyCode);
        additiveStrength2Field.textboxKeyTyped(typedChar, keyCode);
        additiveCritDmg1Field.textboxKeyTyped(typedChar, keyCode);
        additiveCritDmg2Field.textboxKeyTyped(typedChar, keyCode);
        additiveInt1Field.textboxKeyTyped(typedChar, keyCode);
        additiveInt2Field.textboxKeyTyped(typedChar, keyCode);
        multStrengthField.textboxKeyTyped(typedChar, keyCode);
        multCritDmgField.textboxKeyTyped(typedChar, keyCode);
        multIntelligenceField.textboxKeyTyped(typedChar, keyCode);
        additiveDamageBonusField.textboxKeyTyped(typedChar, keyCode);
        enemyDefenseField.textboxKeyTyped(typedChar, keyCode);
        attackSpeedField.textboxKeyTyped(typedChar, keyCode);
        ferocityField.textboxKeyTyped(typedChar, keyCode);
        catacombsLevelField.textboxKeyTyped(typedChar, keyCode);
        masterStarsField.textboxKeyTyped(typedChar, keyCode);
        secretDigitField.textboxKeyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        weaponDamageField.mouseClicked(mouseX, mouseY, mouseButton);
        baseStrengthField.mouseClicked(mouseX, mouseY, mouseButton);
        baseCritDamageField.mouseClicked(mouseX, mouseY, mouseButton);
        baseCritChanceField.mouseClicked(mouseX, mouseY, mouseButton);
        baseIntelligenceField.mouseClicked(mouseX, mouseY, mouseButton);
        additiveStrength1Field.mouseClicked(mouseX, mouseY, mouseButton);
        additiveStrength2Field.mouseClicked(mouseX, mouseY, mouseButton);
        additiveCritDmg1Field.mouseClicked(mouseX, mouseY, mouseButton);
        additiveCritDmg2Field.mouseClicked(mouseX, mouseY, mouseButton);
        additiveInt1Field.mouseClicked(mouseX, mouseY, mouseButton);
        additiveInt2Field.mouseClicked(mouseX, mouseY, mouseButton);
        multStrengthField.mouseClicked(mouseX, mouseY, mouseButton);
        multCritDmgField.mouseClicked(mouseX, mouseY, mouseButton);
        multIntelligenceField.mouseClicked(mouseX, mouseY, mouseButton);
        additiveDamageBonusField.mouseClicked(mouseX, mouseY, mouseButton);
        enemyDefenseField.mouseClicked(mouseX, mouseY, mouseButton);
        attackSpeedField.mouseClicked(mouseX, mouseY, mouseButton);
        ferocityField.mouseClicked(mouseX, mouseY, mouseButton);
        catacombsLevelField.mouseClicked(mouseX, mouseY, mouseButton);
        masterStarsField.mouseClicked(mouseX, mouseY, mouseButton);
        secretDigitField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        weaponDamageField.updateCursorCounter();
        baseStrengthField.updateCursorCounter();
        baseCritDamageField.updateCursorCounter();
        baseCritChanceField.updateCursorCounter();
        baseIntelligenceField.updateCursorCounter();
        additiveStrength1Field.updateCursorCounter();
        additiveStrength2Field.updateCursorCounter();
        additiveCritDmg1Field.updateCursorCounter();
        additiveCritDmg2Field.updateCursorCounter();
        additiveInt1Field.updateCursorCounter();
        additiveInt2Field.updateCursorCounter();
        multStrengthField.updateCursorCounter();
        multCritDmgField.updateCursorCounter();
        multIntelligenceField.updateCursorCounter();
        additiveDamageBonusField.updateCursorCounter();
        enemyDefenseField.updateCursorCounter();
        attackSpeedField.updateCursorCounter();
        ferocityField.updateCursorCounter();
        catacombsLevelField.updateCursorCounter();
        masterStarsField.updateCursorCounter();
        secretDigitField.updateCursorCounter();
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
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
