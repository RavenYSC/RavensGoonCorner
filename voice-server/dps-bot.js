/**
 * RavenClient - Skyblock DPS Calculator Discord Bot
 * 
 * Features:
 * - /dps command to calculate upgrade priorities
 * - Fetches player data from Hypixel API
 * - Gets prices from Auction House and Bazaar (NOT NPC)
 * - Calculates DPS increase per million coins for all upgrades
 * - Supports Archer, Berserker, and Mage classes
 * - Optional Withers/Drags focus mode
 * 
 * Dependencies:
 *   npm install discord.js axios
 * 
 * Environment Variables:
 *   DISCORD_BOT_TOKEN=your_bot_token
 *   HYPIXEL_API_KEY=your_hypixel_api_key
 *   DISCORD_CLIENT_ID=your_client_id
 */

const { Client, GatewayIntentBits, EmbedBuilder, SlashCommandBuilder, REST, Routes } = require('discord.js');
const axios = require('axios');

// ==================== CONFIGURATION ====================

const CONFIG = {
    BOT_TOKEN: process.env.DISCORD_BOT_TOKEN || 'YOUR_BOT_TOKEN_HERE',
    HYPIXEL_API_KEY: process.env.HYPIXEL_API_KEY || 'YOUR_HYPIXEL_API_KEY_HERE',
    DISCORD_CLIENT_ID: process.env.DISCORD_CLIENT_ID || 'YOUR_CLIENT_ID_HERE',
    GUILD_ID: process.env.DISCORD_GUILD_ID || null
};

// API Endpoints
const API = {
    MOJANG_UUID: (username) => `https://api.mojang.com/users/profiles/minecraft/${username}`,
    HYPIXEL_PLAYER: (uuid) => `https://api.hypixel.net/v2/player?uuid=${uuid}`,
    HYPIXEL_SKYBLOCK_PROFILES: (uuid) => `https://api.hypixel.net/v2/skyblock/profiles?uuid=${uuid}`,
    HYPIXEL_ITEMS: 'https://api.hypixel.net/v2/resources/skyblock/items',
    HYPIXEL_BAZAAR: 'https://api.hypixel.net/v2/skyblock/bazaar',
    HYPIXEL_AUCTIONS: 'https://api.hypixel.net/v2/skyblock/auctions'
};

// ==================== CLASS WEIGHTS ====================

const CLASS_WEIGHTS = {
    ARCHER: { wither: 0.5, drag: 0.5, global: 1.0, baseAdditiveDamage: 0 },
    BERSERKER: { wither: 0.2, drag: 0.8, global: 1.0, baseAdditiveDamage: 1200 },
    MAGE: { wither: 0.8, drag: 0.2, global: 1.0, baseAdditiveDamage: 0 }
};

// ==================== STAT CONSTANTS ====================

const STAT_CONSTANTS = {
    STRENGTH_BASE_ADDITION: 76,
    INTELLIGENCE_BASE_ADDITION: 48,
    STAT_MULTIPLIER_BASE: 1.1 * 1.1 * 1.2,
    STRENGTH_FINAL_MULT: 1.1 * 1.1 * 1.2 * 0.38,
    INTELLIGENCE_FINAL_MULT: 1.1 * 1.1 * 1.2 * 0.24,
    SOFT_CAP_THRESHOLD: 1000000,
    SOFT_CAP_REDUCTION: 0.10
};

// ==================== UPGRADE ITEMS DATABASE ====================
// ADD YOUR ITEM IDS AND STATS HERE
// Format: ITEM_ID: { name, stats: { strength, critDamage, intelligence, etc }, cost: (fetched from API), prerequisites: [], affectsWither: true/false, affectsDrag: true/false }

const UPGRADE_ITEMS = {
    // ===== WEAPONS =====
    'TERMINATOR': {
        name: 'Terminator',
        type: 'weapon',
        stats: { damage: 310, strength: 50, critDamage: 250 },
        classes: ['ARCHER'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'JUJU_SHORTBOW': {
        name: 'Juju Shortbow',
        type: 'weapon',
        stats: { damage: 310, strength: 40, critDamage: 186 },
        classes: ['ARCHER'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'HYPERION': {
        name: 'Hyperion',
        type: 'weapon',
        stats: { damage: 260, strength: 150, intelligence: 350 },
        classes: ['MAGE'],
        affectsWither: true,  // 1.5x multiplier against withers
        affectsDrag: true,
        isHyperion: true,
        prerequisites: []
    },
    'VALKYRIE': {
        name: 'Valkyrie',
        type: 'weapon',
        stats: { damage: 270, strength: 145, critDamage: 60 },
        classes: ['BERSERKER'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'AXE_OF_THE_SHREDDED': {
        name: 'Axe of the Shredded',
        type: 'weapon',
        stats: { damage: 220, strength: 115, critDamage: 35 },
        classes: ['BERSERKER'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'GIANTS_SWORD': {
        name: 'Giants Sword',
        type: 'weapon',
        stats: { damage: 500, strength: 100 },
        classes: ['BERSERKER'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    
    // ===== ARMOR PIECES =====
    'WITHER_HELMET': {
        name: 'Storm/Necron Helmet',
        type: 'armor',
        stats: { strength: 20, critDamage: 30, intelligence: 100 },
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'WITHER_CHESTPLATE': {
        name: 'Storm/Necron Chestplate',
        type: 'armor',
        stats: { strength: 40, critDamage: 50, intelligence: 150 },
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'WITHER_LEGGINGS': {
        name: 'Storm/Necron Leggings',
        type: 'armor',
        stats: { strength: 35, critDamage: 45, intelligence: 130 },
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'WITHER_BOOTS': {
        name: 'Storm/Necron Boots',
        type: 'armor',
        stats: { strength: 15, critDamage: 25, intelligence: 80 },
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    
    // ===== MASTER STARS =====
    'FIRST_MASTER_STAR': {
        name: 'First Master Star',
        type: 'upgrade',
        statMultiplier: 1,  // +1 to master stars
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: []
    },
    'SECOND_MASTER_STAR': {
        name: 'Second Master Star',
        type: 'upgrade',
        statMultiplier: 1,
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: ['FIRST_MASTER_STAR']
    },
    'THIRD_MASTER_STAR': {
        name: 'Third Master Star',
        type: 'upgrade',
        statMultiplier: 1,
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: ['SECOND_MASTER_STAR']
    },
    'FOURTH_MASTER_STAR': {
        name: 'Fourth Master Star',
        type: 'upgrade',
        statMultiplier: 1,
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: ['THIRD_MASTER_STAR']
    },
    'FIFTH_MASTER_STAR': {
        name: 'Fifth Master Star',
        type: 'upgrade',
        statMultiplier: 1,
        classes: ['ARCHER', 'BERSERKER', 'MAGE'],
        affectsWither: true,
        affectsDrag: true,
        prerequisites: ['FOURTH_MASTER_STAR']
    },
    
    // ===== ENCHANTMENTS =====
    // ADD ENCHANTMENT ITEMS HERE
    // Example:
    // 'ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1': {
    //     name: 'One For All',
    //     type: 'enchantment',
    //     stats: { damageMultiplier: 3.1 },  // 310% damage
    //     classes: ['ARCHER', 'BERSERKER'],
    //     affectsWither: true,
    //     affectsDrag: true,
    //     prerequisites: []
    // },
    
    // ===== ACCESSORIES =====
    // ADD ACCESSORY ITEMS HERE
    
    // ===== PETS =====
    // ADD PET ITEMS HERE
    
    // ===== REFORGES =====
    // ADD REFORGE ITEMS HERE
};

// ==================== DAMAGE CALCULATOR ====================

class DamageCalculator {
    constructor(playerClass, playerStats) {
        this.class = playerClass;
        this.stats = playerStats;
        this.weights = CLASS_WEIGHTS[playerClass];
    }
    
    /**
     * Calculate final damage value
     * Formula: (5 + weapon_damage)
     */
    calculateDamage() {
        return 5 + (this.stats.weaponDamage || 0);
    }
    
    /**
     * Calculate final strength
     * Formula: (Strength+76)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.38)
     */
    calculateStrength() {
        const base = (this.stats.strength || 0) + STAT_CONSTANTS.STRENGTH_BASE_ADDITION;
        const additiveMult = 1 + ((this.stats.additiveStrength1 || 0) / 100) + ((this.stats.additiveStrength2 || 0) / 100);
        return base * additiveMult * (this.stats.multiplicativeStrength || 1) * STAT_CONSTANTS.STRENGTH_FINAL_MULT;
    }
    
    /**
     * Calculate final crit damage
     */
    calculateCritDamage() {
        const base = (this.stats.critDamage || 50) + STAT_CONSTANTS.STRENGTH_BASE_ADDITION;
        const additiveMult = 1 + ((this.stats.additiveCritDamage1 || 0) / 100) + ((this.stats.additiveCritDamage2 || 0) / 100);
        return base * additiveMult * (this.stats.multiplicativeCritDamage || 1) * STAT_CONSTANTS.STRENGTH_FINAL_MULT;
    }
    
    /**
     * Calculate final intelligence
     * Formula: (Intelligence+48)(1+(additive1)+(additive2))(multiplicative)(1.1*1.1*1.2*0.24)
     */
    calculateIntelligence() {
        const base = (this.stats.intelligence || 0) + STAT_CONSTANTS.INTELLIGENCE_BASE_ADDITION;
        const additiveMult = 1 + ((this.stats.additiveIntelligence1 || 0) / 100) + ((this.stats.additiveIntelligence2 || 0) / 100);
        return base * additiveMult * (this.stats.multiplicativeIntelligence || 1) * STAT_CONSTANTS.INTELLIGENCE_FINAL_MULT;
    }
    
    /**
     * Calculate dungeon-scaled stat
     * Formula: basestat * (6.25 + 5*masterstars + 0.01*secretdigit)
     */
    calculateDungeonStat(baseStat) {
        const masterStars = this.stats.masterStars || 0;
        const secretDigit = this.stats.secretDigit || 0;
        return baseStat * (6.25 + 5 * masterStars + 0.01 * secretDigit);
    }
    
    /**
     * Calculate dungeon-scaled crit chance
     * Formula: critchance * (1.5 + 0.05*masterstars + 0.01*secretdigit)
     */
    calculateDungeonCritChance(baseCritChance) {
        const masterStars = this.stats.masterStars || 0;
        const secretDigit = this.stats.secretDigit || 0;
        return baseCritChance * (1.5 + 0.05 * masterStars + 0.01 * secretDigit);
    }
    
    /**
     * Calculate defense reduction factor
     */
    calculateDefenseReduction(enemyDefense = 0) {
        if (enemyDefense <= 0) return 1.0;
        return 1.0 - (enemyDefense / (enemyDefense + 100));
    }
    
    /**
     * Apply soft cap to damage
     */
    applySoftCap(damage) {
        if (damage <= STAT_CONSTANTS.SOFT_CAP_THRESHOLD) {
            return damage;
        }
        const overCap = damage - STAT_CONSTANTS.SOFT_CAP_THRESHOLD;
        return STAT_CONSTANTS.SOFT_CAP_THRESHOLD + (overCap * STAT_CONSTANTS.SOFT_CAP_REDUCTION);
    }
    
    /**
     * Calculate Archer/Berserker damage per hit
     * Formula: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))(2-(critchance/400))(defenseReduction)
     *        + (1.5(damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))((critchance/400)-1)(defenseReduction)
     */
    calculatePhysicalDamage(enemyType, enemyDefense = 0) {
        const damage = this.calculateDungeonStat(this.calculateDamage());
        const strength = this.calculateDungeonStat(this.calculateStrength());
        const critDamage = this.calculateDungeonStat(this.calculateCritDamage());
        const critChance = Math.min(100, this.calculateDungeonCritChance(this.stats.critChance || 30));
        const defenseReduction = this.calculateDefenseReduction(enemyDefense);
        
        // Add class base additive damage
        const totalAdditiveDamage = (this.stats.additiveDamageBonus || 0) + this.weights.baseAdditiveDamage;
        
        // First term (non-crit weighted)
        const term1 = damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + totalAdditiveDamage / 100) 
            * (2 - critChance / 400) 
            * defenseReduction;
        
        // Second term (crit weighted)
        const term2 = 1.5 * damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + totalAdditiveDamage / 100) 
            * (critChance / 400 - 1) 
            * defenseReduction;
        
        return this.applySoftCap(term1 + term2);
    }
    
    /**
     * Calculate Mage damage per hit
     * Formula: ((damage)(1+(critdamage/100))(1+(strength/100))(1+(additivedamage/100))+(.3(.09+(intelligence/1000)))(defenseReduction)
     */
    calculateMageDamage(enemyType, enemyDefense = 0) {
        const damage = this.calculateDungeonStat(this.calculateDamage());
        const strength = this.calculateDungeonStat(this.calculateStrength());
        const critDamage = this.calculateDungeonStat(this.calculateCritDamage());
        const intelligence = this.calculateDungeonStat(this.calculateIntelligence());
        const defenseReduction = this.calculateDefenseReduction(enemyDefense);
        
        // Base magic damage
        const baseMagicDamage = damage 
            * (1 + critDamage / 100) 
            * (1 + strength / 100) 
            * (1 + (this.stats.additiveDamageBonus || 0) / 100);
        
        // Intelligence scaling
        const intelligenceBonus = 0.3 * (0.09 + intelligence / 1000);
        
        let totalDamage = (baseMagicDamage + intelligenceBonus) * defenseReduction;
        
        // Apply Hyperion bonus for withers (1.5x)
        if (this.stats.hasHyperion && enemyType === 'WITHER') {
            totalDamage *= 1.5;
        }
        
        return this.applySoftCap(totalDamage);
    }
    
    /**
     * Calculate damage based on class and enemy type
     */
    calculateDamageForEnemy(enemyType, enemyDefense = 0) {
        if (this.class === 'MAGE') {
            return this.calculateMageDamage(enemyType, enemyDefense);
        } else {
            return this.calculatePhysicalDamage(enemyType, enemyDefense);
        }
    }
    
    /**
     * Calculate weighted DPS based on class weights
     */
    calculateWeightedDPS(enemyDefense = 0) {
        const witherDamage = this.calculateDamageForEnemy('WITHER', enemyDefense);
        const dragDamage = this.calculateDamageForEnemy('DRAGON', enemyDefense);
        
        const attackSpeed = this.stats.attackSpeed || 1.0;
        const ferocity = this.stats.ferocity || 0;
        const ferocityMult = 1 + (ferocity / 100);
        
        const witherDPS = witherDamage * attackSpeed * ferocityMult;
        const dragDPS = dragDamage * attackSpeed * ferocityMult;
        
        return (witherDPS * this.weights.wither) + (dragDPS * this.weights.drag);
    }
    
    /**
     * Calculate DPS for specific enemy type
     */
    calculateDPS(enemyType, enemyDefense = 0) {
        const damage = this.calculateDamageForEnemy(enemyType, enemyDefense);
        const attackSpeed = this.stats.attackSpeed || 1.0;
        const ferocity = this.stats.ferocity || 0;
        return damage * attackSpeed * (1 + ferocity / 100);
    }
}

// ==================== API HELPERS ====================

/**
 * Get UUID from Minecraft username
 */
async function getUUID(username) {
    try {
        const response = await axios.get(API.MOJANG_UUID(username));
        return response.data.id;
    } catch (error) {
        console.error(`Error fetching UUID for ${username}:`, error.message);
        return null;
    }
}

/**
 * Get player's Skyblock profile
 */
async function getPlayerProfile(uuid) {
    try {
        const response = await axios.get(API.HYPIXEL_SKYBLOCK_PROFILES(uuid), {
            headers: { 'API-Key': CONFIG.HYPIXEL_API_KEY }
        });
        
        if (!response.data.success || !response.data.profiles) {
            return null;
        }
        
        // Find the selected/most recent profile
        const profiles = response.data.profiles;
        const selected = profiles.find(p => p.selected) || profiles[0];
        
        return selected;
    } catch (error) {
        console.error(`Error fetching profile for ${uuid}:`, error.message);
        return null;
    }
}

/**
 * Get current Bazaar prices
 */
async function getBazaarPrices() {
    try {
        const response = await axios.get(API.HYPIXEL_BAZAAR);
        if (!response.data.success) return {};
        
        const prices = {};
        for (const [itemId, data] of Object.entries(response.data.products)) {
            if (data.quick_status) {
                prices[itemId] = {
                    buyPrice: data.quick_status.buyPrice,
                    sellPrice: data.quick_status.sellPrice
                };
            }
        }
        return prices;
    } catch (error) {
        console.error('Error fetching Bazaar prices:', error.message);
        return {};
    }
}

/**
 * Get lowest BIN prices from Auction House
 */
async function getAuctionPrices() {
    try {
        // Get first page to determine total pages
        const response = await axios.get(API.HYPIXEL_AUCTIONS);
        if (!response.data.success) return {};
        
        const lowestBIN = {};
        const processAuctions = (auctions) => {
            for (const auction of auctions) {
                if (auction.bin) {
                    const itemId = extractItemId(auction.item_bytes);
                    if (itemId && (!lowestBIN[itemId] || auction.starting_bid < lowestBIN[itemId])) {
                        lowestBIN[itemId] = auction.starting_bid;
                    }
                }
            }
        };
        
        processAuctions(response.data.auctions);
        
        // Note: In production, you'd want to fetch all pages
        // For brevity, we're just getting the first page here
        
        return lowestBIN;
    } catch (error) {
        console.error('Error fetching Auction prices:', error.message);
        return {};
    }
}

/**
 * Extract item ID from auction item bytes (simplified)
 * In production, you'd need to decode the NBT data
 */
function extractItemId(itemBytes) {
    // This is a placeholder - actual implementation would decode NBT
    return null;
}

/**
 * Get all Skyblock items from API
 */
async function getSkyblockItems() {
    try {
        const response = await axios.get(API.HYPIXEL_ITEMS);
        if (!response.data.success) return {};
        
        const items = {};
        for (const item of response.data.items) {
            items[item.id] = item;
        }
        return items;
    } catch (error) {
        console.error('Error fetching Skyblock items:', error.message);
        return {};
    }
}

// ==================== UPGRADE CALCULATOR ====================

class UpgradeCalculator {
    constructor(playerClass, currentStats, currentItems) {
        this.playerClass = playerClass;
        this.currentStats = currentStats;
        this.currentItems = currentItems; // Array of item IDs player owns
        this.bazaarPrices = {};
        this.auctionPrices = {};
    }
    
    async loadPrices() {
        this.bazaarPrices = await getBazaarPrices();
        this.auctionPrices = await getAuctionPrices();
    }
    
    /**
     * Get price for an item (from Bazaar or AH, NOT NPC)
     */
    getItemPrice(itemId) {
        // Check Bazaar first
        if (this.bazaarPrices[itemId]) {
            return this.bazaarPrices[itemId].buyPrice;
        }
        // Check Auction House
        if (this.auctionPrices[itemId]) {
            return this.auctionPrices[itemId];
        }
        // Return a high default if price unknown
        return 999999999;
    }
    
    /**
     * Check if player can apply upgrade (has prerequisites)
     */
    canApplyUpgrade(upgradeId) {
        const upgrade = UPGRADE_ITEMS[upgradeId];
        if (!upgrade) return false;
        
        // Check class compatibility
        if (!upgrade.classes.includes(this.playerClass)) return false;
        
        // Check if already owned
        if (this.currentItems.includes(upgradeId)) return false;
        
        // Check prerequisites
        for (const prereq of upgrade.prerequisites || []) {
            if (!this.currentItems.includes(prereq)) return false;
        }
        
        return true;
    }
    
    /**
     * Calculate DPS increase from an upgrade
     */
    calculateUpgradeDPSIncrease(upgradeId, focusType = null) {
        const upgrade = UPGRADE_ITEMS[upgradeId];
        if (!upgrade) return { dpsIncrease: 0, price: 0, perMillion: 0 };
        
        // Calculate current DPS
        const currentCalc = new DamageCalculator(this.playerClass, this.currentStats);
        let currentDPS;
        
        if (focusType === 'WITHER') {
            currentDPS = currentCalc.calculateDPS('WITHER');
        } else if (focusType === 'DRAGON') {
            currentDPS = currentCalc.calculateDPS('DRAGON');
        } else {
            currentDPS = currentCalc.calculateWeightedDPS();
        }
        
        // Create modified stats with upgrade applied
        const newStats = { ...this.currentStats };
        
        if (upgrade.type === 'upgrade' && upgrade.statMultiplier) {
            // Master stars increase
            newStats.masterStars = (newStats.masterStars || 0) + upgrade.statMultiplier;
        } else if (upgrade.stats) {
            // Regular stat upgrade
            for (const [stat, value] of Object.entries(upgrade.stats)) {
                newStats[stat] = (newStats[stat] || 0) + value;
            }
        }
        
        // Handle Hyperion special case
        if (upgrade.isHyperion) {
            newStats.hasHyperion = true;
        }
        
        // Calculate new DPS
        const newCalc = new DamageCalculator(this.playerClass, newStats);
        let newDPS;
        
        if (focusType === 'WITHER') {
            newDPS = newCalc.calculateDPS('WITHER');
        } else if (focusType === 'DRAGON') {
            newDPS = newCalc.calculateDPS('DRAGON');
        } else {
            newDPS = newCalc.calculateWeightedDPS();
        }
        
        // Apply weight for enemy-specific upgrades
        let dpsIncrease = newDPS - currentDPS;
        
        if (focusType === null) {
            const weights = CLASS_WEIGHTS[this.playerClass];
            if (!upgrade.affectsWither) {
                dpsIncrease *= weights.drag;
            } else if (!upgrade.affectsDrag) {
                dpsIncrease *= weights.wither;
            }
        }
        
        const price = this.getItemPrice(upgradeId);
        const perMillion = dpsIncrease / (price / 1000000);
        
        return {
            dpsIncrease,
            price,
            perMillion: isFinite(perMillion) ? perMillion : 0
        };
    }
    
    /**
     * Get all possible upgrades sorted by DPS/million
     */
    getAllUpgradesSorted(focusType = null) {
        const upgrades = [];
        
        for (const [itemId, item] of Object.entries(UPGRADE_ITEMS)) {
            if (this.canApplyUpgrade(itemId)) {
                const result = this.calculateUpgradeDPSIncrease(itemId, focusType);
                upgrades.push({
                    id: itemId,
                    name: item.name,
                    type: item.type,
                    ...result
                });
            }
        }
        
        // Sort by DPS per million (highest first)
        upgrades.sort((a, b) => b.perMillion - a.perMillion);
        
        return upgrades;
    }
}

// ==================== DISCORD BOT ====================

const client = new Client({
    intents: [GatewayIntentBits.Guilds]
});

async function registerCommands() {
    const commands = [
        new SlashCommandBuilder()
            .setName('dps')
            .setDescription('Calculate DPS upgrade priorities for Skyblock Catacombs')
            .addStringOption(option =>
                option.setName('ign')
                    .setDescription('Your Hypixel IGN (Minecraft username)')
                    .setRequired(true)
            )
            .addStringOption(option =>
                option.setName('class')
                    .setDescription('Your dungeon class')
                    .setRequired(true)
                    .addChoices(
                        { name: 'Archer', value: 'ARCHER' },
                        { name: 'Berserker', value: 'BERSERKER' },
                        { name: 'Mage', value: 'MAGE' }
                    )
            )
            .addStringOption(option =>
                option.setName('focus')
                    .setDescription('Focus on specific enemy type (optional)')
                    .setRequired(false)
                    .addChoices(
                        { name: 'Withers', value: 'WITHER' },
                        { name: 'Dragons', value: 'DRAGON' }
                    )
            )
    ].map(cmd => cmd.toJSON());
    
    const rest = new REST({ version: '10' }).setToken(CONFIG.BOT_TOKEN);
    
    try {
        console.log('[Discord] Registering /dps command...');
        
        if (CONFIG.GUILD_ID) {
            await rest.put(
                Routes.applicationGuildCommands(CONFIG.DISCORD_CLIENT_ID, CONFIG.GUILD_ID),
                { body: commands }
            );
        } else {
            await rest.put(
                Routes.applicationCommands(CONFIG.DISCORD_CLIENT_ID),
                { body: commands }
            );
        }
        
        console.log('[Discord] /dps command registered!');
    } catch (err) {
        console.error('[Discord] Error registering commands:', err);
    }
}

client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    
    if (interaction.commandName === 'dps') {
        await interaction.deferReply();
        
        try {
            const ign = interaction.options.getString('ign');
            const playerClass = interaction.options.getString('class');
            const focusType = interaction.options.getString('focus');
            
            // Get player UUID
            const uuid = await getUUID(ign);
            if (!uuid) {
                await interaction.editReply({
                    content: `❌ Could not find player **${ign}**. Check the spelling!`
                });
                return;
            }
            
            // Get player profile
            const profile = await getPlayerProfile(uuid);
            if (!profile) {
                await interaction.editReply({
                    content: `❌ Could not fetch Skyblock profile for **${ign}**.`
                });
                return;
            }
            
            // Extract player stats from profile
            const memberData = profile.members[uuid];
            if (!memberData) {
                await interaction.editReply({
                    content: `❌ No profile data found for **${ign}**.`
                });
                return;
            }
            
            // Build current stats from profile data
            // NOTE: This is simplified - actual implementation would parse inventory/armor/skills
            const currentStats = {
                weaponDamage: 0,
                strength: memberData?.player_data?.experience?.SKILL_COMBAT || 0,
                critDamage: 50,
                critChance: 30,
                intelligence: 0,
                attackSpeed: 1.0,
                ferocity: 0,
                masterStars: 0,
                secretDigit: 0,
                hasHyperion: false
            };
            
            // Get items player owns (simplified)
            const currentItems = [];
            
            // Calculate upgrades
            const calculator = new UpgradeCalculator(playerClass, currentStats, currentItems);
            await calculator.loadPrices();
            
            const upgrades = calculator.getAllUpgradesSorted(focusType);
            
            // Build response embed
            const classWeights = CLASS_WEIGHTS[playerClass];
            const focusText = focusType ? ` (${focusType} focus)` : '';
            
            const embed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle(`🏹 DPS Upgrades for ${ign}`)
                .setDescription(`**Class:** ${playerClass}${focusText}\n**Weights:** ${classWeights.wither * 100}% Wither / ${classWeights.drag * 100}% Dragon`)
                .setTimestamp();
            
            // Add top 10 upgrades
            const top10 = upgrades.slice(0, 10);
            let upgradeList = '';
            
            for (let i = 0; i < top10.length; i++) {
                const upg = top10[i];
                const priceStr = formatPrice(upg.price);
                const dpsStr = formatNumber(upg.dpsIncrease);
                const perMillionStr = formatNumber(upg.perMillion);
                
                upgradeList += `**${i + 1}. ${upg.name}**\n`;
                upgradeList += `   💰 ${priceStr} | 📈 +${dpsStr} DPS | ⚡ ${perMillionStr}/M\n\n`;
            }
            
            if (upgradeList) {
                embed.addFields({ name: 'Top Upgrades (by DPS/million coins)', value: upgradeList });
            } else {
                embed.addFields({ name: 'Upgrades', value: 'No available upgrades found!' });
            }
            
            embed.setFooter({ text: 'Prices from Bazaar/AH • DPS formula by RavenClient' });
            
            await interaction.editReply({ embeds: [embed] });
            
        } catch (error) {
            console.error('Error in /dps command:', error);
            await interaction.editReply({
                content: `❌ An error occurred while calculating DPS upgrades.`
            });
        }
    }
});

// ==================== HELPER FUNCTIONS ====================

function formatPrice(price) {
    if (price >= 1000000000) {
        return (price / 1000000000).toFixed(2) + 'B';
    } else if (price >= 1000000) {
        return (price / 1000000).toFixed(2) + 'M';
    } else if (price >= 1000) {
        return (price / 1000).toFixed(2) + 'K';
    }
    return price.toFixed(0);
}

function formatNumber(num) {
    if (Math.abs(num) >= 1000000) {
        return (num / 1000000).toFixed(2) + 'M';
    } else if (Math.abs(num) >= 1000) {
        return (num / 1000).toFixed(2) + 'K';
    }
    return num.toFixed(2);
}

// ==================== START BOT ====================

client.once('ready', async () => {
    console.log('=========================================');
    console.log('  RavenClient Skyblock DPS Calculator Bot');
    console.log('=========================================');
    console.log(`Logged in as ${client.user.tag}`);
    
    await registerCommands();
    
    console.log('Bot is ready!');
});

client.login(CONFIG.BOT_TOKEN);

module.exports = { DamageCalculator, UpgradeCalculator, UPGRADE_ITEMS };
