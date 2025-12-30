package com.raven.client.skyblock.calculator;

/**
 * Comprehensive DPS Calculator for Hypixel Skyblock Catacombs
 * Supports Archer, Berserker, and Mage classes with accurate damage formulas
 * 
 * Damage Formulas:
 * - Archer/Bers: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))(2-(critchance/400))(1-(defense/(defense+100)))
 *                + (1.5(damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))((critchance/400)-1)(1-(defense/(defense+100)))
 * - Mage: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))+(.3(.09+(intelligence/1000)))(1-(defense/(defense+100)))
 * 
 * Stat Calculations:
 * - Damage: (5 + weapon_damage)
 * - Strength/CritDamage: (Base+76)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.38)
 * - Intelligence: (Base+48)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.24)
 * 
 * Dungeon Scaling (Catacombs 50, 5 star gear):
 * - Base stats: basestat * (6.25 + 5*masterstars + 0.01*secretdigit)
 * - Crit chance: critchance * (1.5 + 0.05*masterstars + 0.01*secretdigit)
 * 
 * Soft Cap: All damage after 1 million has 90% reduction
 */
public class SkyblockDPSCalculator {
    
    // ==================== ENUMS ====================
    
    public enum DungeonClass {
        ARCHER(0.5, 0.5, 0),       // 50% wither, 50% drag weight
        BERSERKER(0.2, 0.8, 1200), // 20% wither, 80% drag weight, 1200 base additive
        MAGE(0.8, 0.2, 0);         // 80% wither, 20% drag weight
        
        public final double witherWeight;
        public final double dragWeight;
        public final double baseAdditiveDamage;
        
        DungeonClass(double witherWeight, double dragWeight, double baseAdditiveDamage) {
            this.witherWeight = witherWeight;
            this.dragWeight = dragWeight;
            this.baseAdditiveDamage = baseAdditiveDamage;
        }
    }
    
    public enum EnemyType {
        WITHER,
        DRAGON,
        GLOBAL // Affects both equally
    }
    
    // ==================== BASE STATS ====================
    
    // Raw weapon stats
    private double weaponDamage = 0;
    
    // Base stats (before multipliers)
    private double baseStrength = 0;
    private double baseCritDamage = 50;
    private double baseCritChance = 30;
    private double baseIntelligence = 0;
    
    // Additive bonuses (percentage based)
    private double additiveStrength1 = 0;
    private double additiveStrength2 = 0;
    private double additiveCritDamage1 = 0;
    private double additiveCritDamage2 = 0;
    private double additiveIntelligence1 = 0;
    private double additiveIntelligence2 = 0;
    
    // Multiplicative bonuses
    private double multiplicativeStrength = 1.0;
    private double multiplicativeCritDamage = 1.0;
    private double multiplicativeIntelligence = 1.0;
    
    // Additive damage bonus (from armor, accessories, etc.)
    private double additiveDamageBonus = 0;
    
    // Enemy defense (for damage reduction calculation)
    private double enemyDefense = 0;
    
    // Attack speed for DPS calculation
    private double attackSpeed = 1.0;
    private double ferocity = 0;
    
    // ==================== DUNGEON STATS ====================
    
    private int catacombsLevel = 50;
    private int masterStars = 0; // 0-5
    private int secretDigit = 0; // For secrets bonus
    
    // Class selection
    private DungeonClass dungeonClass = DungeonClass.ARCHER;
    
    // Mage specific
    private boolean hasHyperion = false; // 1.5x against withers
    
    // ==================== STAT CONSTANTS ====================
    
    // Base stat additions from default gear/skills
    private static final double STRENGTH_BASE_ADDITION = 76;
    private static final double INTELLIGENCE_BASE_ADDITION = 48;
    
    // Final multiplier constants (1.1 * 1.1 * 1.2)
    private static final double STAT_MULTIPLIER_BASE = 1.1 * 1.1 * 1.2;
    private static final double STRENGTH_FINAL_MULTIPLIER = STAT_MULTIPLIER_BASE * 0.38;
    private static final double INTELLIGENCE_FINAL_MULTIPLIER = STAT_MULTIPLIER_BASE * 0.24;
    
    // Soft cap
    private static final double SOFT_CAP_THRESHOLD = 1_000_000;
    private static final double SOFT_CAP_REDUCTION = 0.10; // 90% reduction = 10% effectiveness
    
    // ==================== CONSTRUCTORS ====================
    
    public SkyblockDPSCalculator() {
    }
    
    public SkyblockDPSCalculator(DungeonClass dungeonClass) {
        this.dungeonClass = dungeonClass;
    }
    
    // ==================== MAIN CALCULATIONS ====================
    
    /**
     * Calculate final damage value
     * Formula: (5 + weapon_damage)
     */
    public double calculateDamage() {
        return 5 + weaponDamage;
    }
    
    /**
     * Calculate final strength
     * Formula: (Strength+76)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.38)
     */
    public double calculateStrength() {
        double base = baseStrength + STRENGTH_BASE_ADDITION;
        double additiveMultiplier = 1 + (additiveStrength1 / 100) + (additiveStrength2 / 100);
        return base * additiveMultiplier * multiplicativeStrength * STRENGTH_FINAL_MULTIPLIER;
    }
    
    /**
     * Calculate final crit damage
     * Uses same formula as strength
     */
    public double calculateCritDamage() {
        double base = baseCritDamage + STRENGTH_BASE_ADDITION;
        double additiveMultiplier = 1 + (additiveCritDamage1 / 100) + (additiveCritDamage2 / 100);
        return base * additiveMultiplier * multiplicativeCritDamage * STRENGTH_FINAL_MULTIPLIER;
    }
    
    /**
     * Calculate final intelligence
     * Formula: (Intelligence+48)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.24)
     */
    public double calculateIntelligence() {
        double base = baseIntelligence + INTELLIGENCE_BASE_ADDITION;
        double additiveMultiplier = 1 + (additiveIntelligence1 / 100) + (additiveIntelligence2 / 100);
        return base * additiveMultiplier * multiplicativeIntelligence * INTELLIGENCE_FINAL_MULTIPLIER;
    }
    
    /**
     * Calculate dungeon-scaled stat
     * Formula: basestat * (6.25 + 5*masterstars + 0.01*secretdigit)
     */
    public double calculateDungeonStat(double baseStat) {
        return baseStat * (6.25 + 5 * masterStars + 0.01 * secretDigit);
    }
    
    /**
     * Calculate dungeon-scaled crit chance
     * Formula: critchance * (1.5 + 0.05*masterstars + 0.01*secretdigit)
     */
    public double calculateDungeonCritChance(double baseCritChance) {
        return baseCritChance * (1.5 + 0.05 * masterStars + 0.01 * secretDigit);
    }
    
    /**
     * Calculate defense reduction factor
     * Formula: (1 - (defense / (defense + 100)))
     */
    public double calculateDefenseReduction() {
        if (enemyDefense <= 0) return 1.0;
        return 1.0 - (enemyDefense / (enemyDefense + 100));
    }
    
    /**
     * Apply soft cap to damage
     * All damage after 1 million has 90% reduction
     */
    public double applySoftCap(double damage) {
        if (damage <= SOFT_CAP_THRESHOLD) {
            return damage;
        }
        double overCap = damage - SOFT_CAP_THRESHOLD;
        return SOFT_CAP_THRESHOLD + (overCap * SOFT_CAP_REDUCTION);
    }
    
    /**
     * Calculate Archer/Berserker damage per hit
     * Formula: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))(2-(critchance/400))(defenseReduction)
     *        + (1.5(damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))((critchance/400)-1)(defenseReduction)
     */
    public double calculatePhysicalDamage(EnemyType enemyType) {
        double damage = calculateDungeonStat(calculateDamage());
        double strength = calculateDungeonStat(calculateStrength());
        double critDamage = calculateDungeonStat(calculateCritDamage());
        double critChance = Math.min(100, calculateDungeonCritChance(baseCritChance));
        double defenseReduction = calculateDefenseReduction();
        
        // Add class base additive damage (Bers gets 1200)
        double totalAdditiveDamage = additiveDamageBonus + dungeonClass.baseAdditiveDamage;
        
        // First term (non-crit weighted)
        double term1 = damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + totalAdditiveDamage / 100) 
            * (2 - critChance / 400) 
            * defenseReduction;
        
        // Second term (crit weighted)
        double term2 = 1.5 * damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + totalAdditiveDamage / 100) 
            * (critChance / 400 - 1) 
            * defenseReduction;
        
        double totalDamage = term1 + term2;
        return applySoftCap(totalDamage);
    }
    
    /**
     * Calculate Mage damage per hit
     * Formula: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))+(.3(.09+(intelligence/1000)))(defenseReduction)
     */
    public double calculateMageDamage(EnemyType enemyType) {
        double damage = calculateDungeonStat(calculateDamage());
        double strength = calculateDungeonStat(calculateStrength());
        double critDamage = calculateDungeonStat(calculateCritDamage());
        double intelligence = calculateDungeonStat(calculateIntelligence());
        double defenseReduction = calculateDefenseReduction();
        
        // Base magic damage
        double baseMagicDamage = damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + additiveDamageBonus / 100);
        
        // Intelligence scaling
        double intelligenceBonus = 0.3 * (0.09 + intelligence / 1000);
        
        double totalDamage = (baseMagicDamage + intelligenceBonus) * defenseReduction;
        
        // Apply Hyperion bonus for withers
        if (hasHyperion && enemyType == EnemyType.WITHER) {
            totalDamage *= 1.5;
        }
        
        return applySoftCap(totalDamage);
    }
    
    /**
     * Calculate damage based on current class
     */
    public double calculateDamageForClass(EnemyType enemyType) {
        if (dungeonClass == DungeonClass.MAGE) {
            return calculateMageDamage(enemyType);
        } else {
            return calculatePhysicalDamage(enemyType);
        }
    }
    
    /**
     * Calculate weighted DPS based on class weights
     */
    public double calculateWeightedDPS() {
        double witherDamage = calculateDamageForClass(EnemyType.WITHER);
        double dragDamage = calculateDamageForClass(EnemyType.DRAGON);
        
        // Apply class weights
        double weightedDamage = (witherDamage * dungeonClass.witherWeight) 
                              + (dragDamage * dungeonClass.dragWeight);
        
        // Apply ferocity and attack speed for DPS
        double ferocityMultiplier = 1 + (ferocity / 100);
        return weightedDamage * attackSpeed * ferocityMultiplier;
    }
    
    /**
     * Calculate DPS against specific enemy type
     */
    public double calculateDPS(EnemyType enemyType) {
        double damage = calculateDamageForClass(enemyType);
        double ferocityMultiplier = 1 + (ferocity / 100);
        return damage * attackSpeed * ferocityMultiplier;
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public void setWeaponDamage(double weaponDamage) {
        this.weaponDamage = Math.max(0, weaponDamage);
    }
    
    public double getWeaponDamage() {
        return weaponDamage;
    }
    
    public void setBaseStrength(double baseStrength) {
        this.baseStrength = Math.max(0, baseStrength);
    }
    
    public double getBaseStrength() {
        return baseStrength;
    }
    
    public void setBaseCritDamage(double baseCritDamage) {
        this.baseCritDamage = Math.max(0, baseCritDamage);
    }
    
    public double getBaseCritDamage() {
        return baseCritDamage;
    }
    
    public void setBaseCritChance(double baseCritChance) {
        this.baseCritChance = Math.max(0, Math.min(100, baseCritChance));
    }
    
    public double getBaseCritChance() {
        return baseCritChance;
    }
    
    public void setBaseIntelligence(double baseIntelligence) {
        this.baseIntelligence = Math.max(0, baseIntelligence);
    }
    
    public double getBaseIntelligence() {
        return baseIntelligence;
    }
    
    public void setAdditiveStrength1(double additiveStrength1) {
        this.additiveStrength1 = additiveStrength1;
    }
    
    public void setAdditiveStrength2(double additiveStrength2) {
        this.additiveStrength2 = additiveStrength2;
    }
    
    public void setAdditiveCritDamage1(double additiveCritDamage1) {
        this.additiveCritDamage1 = additiveCritDamage1;
    }
    
    public void setAdditiveCritDamage2(double additiveCritDamage2) {
        this.additiveCritDamage2 = additiveCritDamage2;
    }
    
    public void setAdditiveIntelligence1(double additiveIntelligence1) {
        this.additiveIntelligence1 = additiveIntelligence1;
    }
    
    public void setAdditiveIntelligence2(double additiveIntelligence2) {
        this.additiveIntelligence2 = additiveIntelligence2;
    }
    
    public void setMultiplicativeStrength(double multiplicativeStrength) {
        this.multiplicativeStrength = Math.max(0, multiplicativeStrength);
    }
    
    public void setMultiplicativeCritDamage(double multiplicativeCritDamage) {
        this.multiplicativeCritDamage = Math.max(0, multiplicativeCritDamage);
    }
    
    public void setMultiplicativeIntelligence(double multiplicativeIntelligence) {
        this.multiplicativeIntelligence = Math.max(0, multiplicativeIntelligence);
    }
    
    public void setAdditiveDamageBonus(double additiveDamageBonus) {
        this.additiveDamageBonus = additiveDamageBonus;
    }
    
    public double getAdditiveDamageBonus() {
        return additiveDamageBonus;
    }
    
    public void setEnemyDefense(double enemyDefense) {
        this.enemyDefense = Math.max(0, enemyDefense);
    }
    
    public double getEnemyDefense() {
        return enemyDefense;
    }
    
    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = Math.max(0.1, attackSpeed);
    }
    
    public double getAttackSpeed() {
        return attackSpeed;
    }
    
    public void setFerocity(double ferocity) {
        this.ferocity = Math.max(0, ferocity);
    }
    
    public double getFerocity() {
        return ferocity;
    }
    
    public void setCatacombsLevel(int catacombsLevel) {
        this.catacombsLevel = Math.max(0, Math.min(50, catacombsLevel));
    }
    
    public int getCatacombsLevel() {
        return catacombsLevel;
    }
    
    public void setMasterStars(int masterStars) {
        this.masterStars = Math.max(0, Math.min(5, masterStars));
    }
    
    public int getMasterStars() {
        return masterStars;
    }
    
    public void setSecretDigit(int secretDigit) {
        this.secretDigit = Math.max(0, secretDigit);
    }
    
    public int getSecretDigit() {
        return secretDigit;
    }
    
    public void setDungeonClass(DungeonClass dungeonClass) {
        this.dungeonClass = dungeonClass;
    }
    
    public DungeonClass getDungeonClass() {
        return dungeonClass;
    }
    
    public void setHasHyperion(boolean hasHyperion) {
        this.hasHyperion = hasHyperion;
    }
    
    public boolean hasHyperion() {
        return hasHyperion;
    }
    
    /**
     * Reset all values to defaults
     */
    public void reset() {
        weaponDamage = 0;
        baseStrength = 0;
        baseCritDamage = 50;
        baseCritChance = 30;
        baseIntelligence = 0;
        additiveStrength1 = 0;
        additiveStrength2 = 0;
        additiveCritDamage1 = 0;
        additiveCritDamage2 = 0;
        additiveIntelligence1 = 0;
        additiveIntelligence2 = 0;
        multiplicativeStrength = 1.0;
        multiplicativeCritDamage = 1.0;
        multiplicativeIntelligence = 1.0;
        additiveDamageBonus = 0;
        enemyDefense = 0;
        attackSpeed = 1.0;
        ferocity = 0;
        catacombsLevel = 50;
        masterStars = 0;
        secretDigit = 0;
        dungeonClass = DungeonClass.ARCHER;
        hasHyperion = false;
    }
}
