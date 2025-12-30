package com.raven.client.skyblock.calculator;

/**
 * DPS Calculator for Hypixel Skyblock Catacombs Archer
 * Calculates damage per second based on various damage modifiers
 */
public class ArcherDPSCalculator {
    
    // Base stats
    private double baseDamage = 0;
    private double strength = 0;
    private double critDamage = 50; // Base crit damage (50%)
    private double critChance = 30; // Base crit chance (30%)
    private double attackSpeed = 1.0; // Attacks per second
    
    // Class and level bonuses
    private int archeryLevel = 0;
    private int combatLevel = 0;
    private int catacombsLevel = 0;
    private int archerClassLevel = 0;
    
    // Multipliers
    private double arrowDamageMultiplier = 1.0;
    private double bowAbilityMultiplier = 1.0;
    private double enchantmentMultiplier = 1.0;
    private double reforgeMultiplier = 1.0;
    private double armorBonusMultiplier = 1.0;
    private double petBonusMultiplier = 1.0;
    private double potionMultiplier = 1.0;
    private double dungeonMultiplier = 1.0;
    
    // Damage type percentages
    private double ferocity = 0; // Additional hit chance
    private double bonusAttackSpeed = 0;
    
    public ArcherDPSCalculator() {
    }
    
    /**
     * Calculate the total DPS based on all modifiers
     */
    public double calculateDPS() {
        // Calculate effective attack speed
        double effectiveAttackSpeed = attackSpeed * (1 + bonusAttackSpeed / 100);
        
        // Calculate base damage with strength
        double damageWithStrength = baseDamage * (1 + strength / 100);
        
        // Calculate average damage (considering crit)
        double effectiveCritChance = Math.min(critChance, 100) / 100;
        double effectiveCritDamage = 1 + (critDamage / 100);
        double averageDamageMultiplier = (1 - effectiveCritChance) + (effectiveCritChance * effectiveCritDamage);
        
        // Apply all multipliers
        double totalDamage = damageWithStrength * averageDamageMultiplier;
        totalDamage *= arrowDamageMultiplier;
        totalDamage *= bowAbilityMultiplier;
        totalDamage *= enchantmentMultiplier;
        totalDamage *= reforgeMultiplier;
        totalDamage *= armorBonusMultiplier;
        totalDamage *= petBonusMultiplier;
        totalDamage *= potionMultiplier;
        totalDamage *= dungeonMultiplier;
        
        // Apply class bonuses
        totalDamage *= getArcherClassBonus();
        totalDamage *= getArcheryLevelBonus();
        totalDamage *= getCombatLevelBonus();
        totalDamage *= getCatacombsBonus();
        
        // Calculate DPS with attack speed
        double baseDPS = totalDamage * effectiveAttackSpeed;
        
        // Apply ferocity (extra hits)
        double ferocityMultiplier = 1 + (ferocity / 100);
        
        return baseDPS * ferocityMultiplier;
    }
    
    /**
     * Calculate damage per hit
     */
    public double calculateDamagePerHit() {
        return calculateDPS() / (attackSpeed * (1 + bonusAttackSpeed / 100));
    }
    
    /**
     * Get archer class bonus (1.25x at level 50)
     */
    private double getArcherClassBonus() {
        return 1 + (archerClassLevel * 0.005); // 0.5% per level
    }
    
    /**
     * Get archery skill bonus
     */
    private double getArcheryLevelBonus() {
        return 1 + (archeryLevel * 0.01); // 1% per level
    }
    
    /**
     * Get combat level bonus
     */
    private double getCombatLevelBonus() {
        return 1 + (combatLevel * 0.005); // 0.5% per level
    }
    
    /**
     * Get catacombs level bonus
     */
    private double getCatacombsBonus() {
        return 1 + (catacombsLevel * 0.02); // 2% per level
    }
    
    // Getters and setters
    public void setBaseDamage(double baseDamage) {
        this.baseDamage = Math.max(0, baseDamage);
    }
    
    public double getBaseDamage() {
        return baseDamage;
    }
    
    public void setStrength(double strength) {
        this.strength = Math.max(0, strength);
    }
    
    public double getStrength() {
        return strength;
    }
    
    public void setCritDamage(double critDamage) {
        this.critDamage = Math.max(0, critDamage);
    }
    
    public double getCritDamage() {
        return critDamage;
    }
    
    public void setCritChance(double critChance) {
        this.critChance = Math.max(0, Math.min(100, critChance));
    }
    
    public double getCritChance() {
        return critChance;
    }
    
    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = Math.max(0.1, attackSpeed);
    }
    
    public double getAttackSpeed() {
        return attackSpeed;
    }
    
    public void setArcheryLevel(int archeryLevel) {
        this.archeryLevel = Math.max(0, Math.min(50, archeryLevel));
    }
    
    public int getArcheryLevel() {
        return archeryLevel;
    }
    
    public void setCombatLevel(int combatLevel) {
        this.combatLevel = Math.max(0, Math.min(50, combatLevel));
    }
    
    public int getCombatLevel() {
        return combatLevel;
    }
    
    public void setCatacombsLevel(int catacombsLevel) {
        this.catacombsLevel = Math.max(0, Math.min(50, catacombsLevel));
    }
    
    public int getCatacombsLevel() {
        return catacombsLevel;
    }
    
    public void setArcherClassLevel(int archerClassLevel) {
        this.archerClassLevel = Math.max(0, Math.min(50, archerClassLevel));
    }
    
    public int getArcherClassLevel() {
        return archerClassLevel;
    }
    
    public void setArrowDamageMultiplier(double arrowDamageMultiplier) {
        this.arrowDamageMultiplier = Math.max(1, arrowDamageMultiplier);
    }
    
    public double getArrowDamageMultiplier() {
        return arrowDamageMultiplier;
    }
    
    public void setBowAbilityMultiplier(double bowAbilityMultiplier) {
        this.bowAbilityMultiplier = Math.max(1, bowAbilityMultiplier);
    }
    
    public double getBowAbilityMultiplier() {
        return bowAbilityMultiplier;
    }
    
    public void setEnchantmentMultiplier(double enchantmentMultiplier) {
        this.enchantmentMultiplier = Math.max(1, enchantmentMultiplier);
    }
    
    public double getEnchantmentMultiplier() {
        return enchantmentMultiplier;
    }
    
    public void setReforgeMultiplier(double reforgeMultiplier) {
        this.reforgeMultiplier = Math.max(1, reforgeMultiplier);
    }
    
    public double getReforgeMultiplier() {
        return reforgeMultiplier;
    }
    
    public void setArmorBonusMultiplier(double armorBonusMultiplier) {
        this.armorBonusMultiplier = Math.max(1, armorBonusMultiplier);
    }
    
    public double getArmorBonusMultiplier() {
        return armorBonusMultiplier;
    }
    
    public void setPetBonusMultiplier(double petBonusMultiplier) {
        this.petBonusMultiplier = Math.max(1, petBonusMultiplier);
    }
    
    public double getPetBonusMultiplier() {
        return petBonusMultiplier;
    }
    
    public void setPotionMultiplier(double potionMultiplier) {
        this.potionMultiplier = Math.max(1, potionMultiplier);
    }
    
    public double getPotionMultiplier() {
        return potionMultiplier;
    }
    
    public void setDungeonMultiplier(double dungeonMultiplier) {
        this.dungeonMultiplier = Math.max(1, dungeonMultiplier);
    }
    
    public double getDungeonMultiplier() {
        return dungeonMultiplier;
    }
    
    public void setFerocity(double ferocity) {
        this.ferocity = Math.max(0, ferocity);
    }
    
    public double getFerocity() {
        return ferocity;
    }
    
    public void setBonusAttackSpeed(double bonusAttackSpeed) {
        this.bonusAttackSpeed = Math.max(0, bonusAttackSpeed);
    }
    
    public double getBonusAttackSpeed() {
        return bonusAttackSpeed;
    }
    
    /**
     * Reset all values to defaults
     */
    public void reset() {
        baseDamage = 0;
        strength = 0;
        critDamage = 50;
        critChance = 30;
        attackSpeed = 1.0;
        archeryLevel = 0;
        combatLevel = 0;
        catacombsLevel = 0;
        archerClassLevel = 0;
        arrowDamageMultiplier = 1.0;
        bowAbilityMultiplier = 1.0;
        enchantmentMultiplier = 1.0;
        reforgeMultiplier = 1.0;
        armorBonusMultiplier = 1.0;
        petBonusMultiplier = 1.0;
        potionMultiplier = 1.0;
        dungeonMultiplier = 1.0;
        ferocity = 0;
        bonusAttackSpeed = 0;
    }
}
