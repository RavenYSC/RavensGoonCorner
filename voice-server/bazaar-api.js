/**
 * RavenClient Bazaar API Server
 * 
 * Dynamically pulls ALL items from Hypixel Bazaar API
 * Categories items using pattern matching
 * Stores price history for trend analysis
 * 
 * Run with:
 *   node bazaar-api.js
 * 
 * Port: 25581
 */

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

// Configuration
const PORT = 25581;
const HYPIXEL_API_KEY = 'd2938c4a-6d99-40b9-b1bd-3f17105eaea8';
const HYPIXEL_API_URL = 'https://api.hypixel.net/v2/skyblock/bazaar';
const UPDATE_INTERVAL = 60 * 1000; // Update every 60 seconds
const DATA_FILE = path.join(__dirname, 'bazaar_data.json');
const HISTORY_FILE = path.join(__dirname, 'bazaar_history.json');

// Category patterns - used to auto-categorize items from the API
// Order matters - items are placed in the first matching category/section
const CATEGORY_PATTERNS = {
    farming: {
        name: "Farming",
        icon: "golden_hoe",
        color: 0xFFAA00,
        sections: {
            "wheat_seeds": { name: "Wheat & Seeds", patterns: [/^WHEAT/, /^SEEDS/, /^BREAD/, /^HAY_BLOCK/, /ENCHANTED_HAY/] },
            "carrot": { name: "Carrot", patterns: [/CARROT(?!_KING)(?!_CANDY)/] },
            "potato": { name: "Potato", patterns: [/POTATO(?!_BOOK)/] },
            "pumpkin": { name: "Pumpkin", patterns: [/PUMPKIN/] },
            "melon": { name: "Melon", patterns: [/MELON/] },
            "mushroom": { name: "Mushrooms", patterns: [/MUSHROOM/, /^BROWN_MUSHROOM/, /^RED_MUSHROOM/] },
            "cocoa": { name: "Cocoa Beans", patterns: [/^INK_SACK:3/, /COCOA/, /COOKIE/] },
            "cactus": { name: "Cactus", patterns: [/CACTUS/] },
            "sugar_cane": { name: "Sugar Cane", patterns: [/^SUGAR_CANE/, /^PAPER/, /ENCHANTED_SUGAR/, /ENCHANTED_PAPER/] },
            "leather_beef": { name: "Leather & Beef", patterns: [/LEATHER/, /BEEF/] },
            "pork": { name: "Pork", patterns: [/PORK/] },
            "chicken": { name: "Chicken & Feather", patterns: [/CHICKEN/, /FEATHER/, /^EGG$/, /SUPER_EGG/, /CAKE/] },
            "mutton": { name: "Mutton & Wool", patterns: [/MUTTON/, /^WOOL/, /ENCHANTED_WOOL/] },
            "rabbit": { name: "Rabbit", patterns: [/RABBIT/] },
            "nether_wart": { name: "Nether Warts", patterns: [/NETHER_STALK/, /MUTANT_NETHER/] },
            "flowers": { name: "Flowers", patterns: [/^DOUBLE_PLANT/, /^RED_ROSE/, /^YELLOW_FLOWER/, /SUNFLOWER/, /MOONFLOWER/, /WILD_ROSE/, /HELIANTHUS/, /FLOWER/] },
            "garden": { name: "Garden Items", patterns: [/COMPOST/, /CROPIE/, /SQUASH/, /FERMENTO/, /POLISHED_PUMPKIN/, /DUNG/, /CHEESE/, /PLANT_MATTER/, /HONEY_JAR/, /BEE/, /LARVA/, /FLOUR/, /JACOBS/, /ENCHANTED_SEED/, /BOX_OF_SEEDS/, /COMPACTED_/, /CONDENSED_/] }
        }
    },
    mining: {
        name: "Mining",
        icon: "diamond_pickaxe",
        color: 0x55FFFF,
        sections: {
            "cobblestone": { name: "Cobblestone", patterns: [/^COBBLESTONE/] },
            "coal": { name: "Coal", patterns: [/^COAL/, /^CHARCOAL/, /COAL_BLOCK/, /ENCHANTED_COAL/, /ENCHANTED_CHARCOAL/] },
            "iron": { name: "Iron", patterns: [/^IRON_INGOT/, /^ENCHANTED_IRON/, /^IRON_BLOCK/] },
            "gold": { name: "Gold", patterns: [/^GOLD_INGOT/, /^ENCHANTED_GOLD(?!EN)/, /^GOLD_BLOCK/] },
            "diamond": { name: "Diamond", patterns: [/^DIAMOND(?!_SPREADING)/, /REFINED_DIAMOND/] },
            "lapis": { name: "Lapis Lazuli", patterns: [/^INK_SACK:4/, /LAPIS/] },
            "emerald": { name: "Emerald", patterns: [/^EMERALD/] },
            "redstone": { name: "Redstone", patterns: [/^REDSTONE/, /REDSTONE_LAMP/] },
            "quartz": { name: "Quartz", patterns: [/QUARTZ/] },
            "obsidian": { name: "Obsidian", patterns: [/OBSIDIAN/] },
            "glowstone": { name: "Glowstone", patterns: [/GLOWSTONE/] },
            "gravel": { name: "Gravel & Flint", patterns: [/^GRAVEL/, /^FLINT/] },
            "ice": { name: "Ice", patterns: [/^ICE$/, /PACKED_ICE/, /BLUE_ICE/, /ENCHANTED_ICE/, /ENCHANTED_PACKED_ICE/] },
            "netherrack": { name: "Netherrack", patterns: [/NETHERRACK/] },
            "sand": { name: "Sand", patterns: [/^SAND$/, /^RED_SAND/, /ENCHANTED_SAND/, /GLASS$/] },
            "end_stone": { name: "End Stone", patterns: [/ENDER_STONE/, /END_STONE/] },
            "snow": { name: "Snow", patterns: [/^SNOW/] },
            "mithril": { name: "Mithril", patterns: [/MITHRIL/] },
            "titanium": { name: "Titanium", patterns: [/TITANIUM/] },
            "starfall": { name: "Starfall", patterns: [/STARFALL/] },
            "umber": { name: "Umber", patterns: [/UMBER/] },
            "tungsten": { name: "Tungsten", patterns: [/TUNGSTEN/] },
            "glacite": { name: "Glacite", patterns: [/GLACITE/] },
            "gemstones": { name: "Gemstones", patterns: [/RUBY/, /AMBER(?!_MATERIAL)/, /SAPPHIRE/, /JADE/, /AMETHYST/, /TOPAZ/, /JASPER/, /OPAL/, /ONYX/, /AQUAMARINE/, /CITRINE/, /PERIDOT/, /_GEM$/, /PERFECT_/, /FLAWLESS_/, /FINE_/, /ROUGH_/, /GEMSTONE_MIXTURE/] },
            "hard_stone": { name: "Hard Stone", patterns: [/HARD_STONE/, /CONCENTRATED_STONE/] },
            "sulphur_mining": { name: "Sulphur", patterns: [/SULPHUR_ORE/, /SULPHUR_CUBE/] },
            "mycelium": { name: "Mycelium", patterns: [/MYCEL/] },
            "fuel": { name: "Fuel & Oil", patterns: [/MAGMA_BUCKET/, /MAGMA_CORE/, /INFERNO_VERTEX/, /PLASMA/, /^LAVA_BUCKET/, /LAVA_PEARL/, /OIL_BARREL/, /FUEL_TANK/, /HEAVY_GABAGOOL/] },
            "dwarven": { name: "Dwarven Mines", patterns: [/TREASURITE/, /VOLTA/, /WORM_MEMBRANE/, /SLUDGE_JUICE/, /DIVAN_/, /DWARVEN_/, /DRILL_ENGINE/, /CONTROL_SWITCH/, /ELECTRON_TRANSMITTER/, /ROBOTRON_REFLECTOR/, /FTX_3070/, /SYNTHETIC_HEART/] },
            "crystals": { name: "Crystal Hollows", patterns: [/CRYSTAL_FRAGMENT/, /WISHING_COMPASS/, /AUTOMATON/, /ROBOT_PART/, /SCRAP/, /DIRT_BOTTLE/, /SLUDGE/] }
        }
    },
    combat: {
        name: "Combat",
        icon: "iron_sword",
        color: 0xFF5555,
        sections: {
            "rotten_flesh": { name: "Rotten Flesh", patterns: [/ROTTEN/, /REVENANT/, /FOUL_FLESH/] },
            "bone": { name: "Bone", patterns: [/^BONE/, /ENCHANTED_BONE/] },
            "string": { name: "String", patterns: [/^STRING/, /TARANTULA/, /ARACHNE/] },
            "spider_eye": { name: "Spider Eye", patterns: [/SPIDER_EYE/, /FERMENTED/] },
            "gunpowder": { name: "Gunpowder", patterns: [/^SULPHUR$/, /GUNPOWDER/, /FIREWORK/, /ENCHANTED_GUNPOWDER/] },
            "ender_pearl": { name: "Ender Pearl", patterns: [/ENDER_PEARL/, /EYE_OF_ENDER/, /ABSOLUTE_ENDER/, /NULL_SPHERE/] },
            "ghast_tear": { name: "Ghast Tear", patterns: [/GHAST/] },
            "slime": { name: "Slimeball", patterns: [/^SLIME/, /ENCHANTED_SLIME/] },
            "blaze_rod": { name: "Blaze Rod", patterns: [/BLAZE/] },
            "magma_cream": { name: "Magma Cream", patterns: [/MAGMA_CREAM/, /WHIPPED_MAGMA/] },
            "wolf_tooth": { name: "Wolf Tooth", patterns: [/WOLF_TOOTH/, /GOLDEN_TOOTH/] },
            "soulflow": { name: "Soulflow", patterns: [/SOULFLOW(?!_ENGINE)/, /^NULL_(?!SPHERE)/, /ECTOPLASM/] },
            "nether_star": { name: "Nether Star", patterns: [/NETHER_STAR/] },
            "end_items": { name: "End Items", patterns: [/^DRAGON_/, /SUMMONING_EYE/, /END_PORTAL/, /CHORUS/, /SHULKER/] },
            "crimson_isle": { name: "Crimson Isle", patterns: [/CHILI/, /INFERNO(?!_VERTEX)/, /KUUDRA/, /GRIFF/, /VENOM/, /GABAGOOL/, /TENTACLE/, /MAGMA_FISH/, /BEZOS/, /MOOGMA/, /FLAMING_FIST/, /BURNING_EYE/, /CRIMSONITE/, /AMALGAMATED/, /CORRUPTED_/, /NETHER_/, /LUMP_OF_MAGMA/] },
            "slayer_drops": { name: "Slayer Drops", patterns: [/SVEN_/, /TARA_/, /REV_/, /EMAN_/, /BLAZE_/, /INFERNAL_/, /DERELICT/, /SCYTHE_BLADE/, /SNAKE_RUNE/, /OVERFLUX/, /PLASMA_NUCLEUS/, /JUDGEMENT_CORE/] }
        }
    },
    woods_fishes: {
        name: "Woods & Fishes",
        icon: "fishing_rod",
        color: 0x55FF55,
        sections: {
            "oak": { name: "Oak", patterns: [/^LOG$/, /^LOG:0/, /OAK/, /ENCHANTED_OAK/] },
            "spruce": { name: "Spruce", patterns: [/^LOG:1/, /SPRUCE/] },
            "birch": { name: "Birch", patterns: [/^LOG:2/, /BIRCH/] },
            "dark_oak": { name: "Dark Oak", patterns: [/^LOG_2:1/, /DARK_OAK/] },
            "acacia": { name: "Acacia", patterns: [/^LOG_2$/, /^LOG_2:0/, /ACACIA/] },
            "jungle": { name: "Jungle", patterns: [/^LOG:3/, /JUNGLE/] },
            "raw_fish": { name: "Raw Fish", patterns: [/^RAW_FISH$/, /^RAW_FISH:0/, /COOKED_FISH(?!:)/] },
            "salmon": { name: "Salmon", patterns: [/RAW_FISH:1/, /COOKED_FISH:1/, /SALMON/] },
            "clownfish": { name: "Clownfish", patterns: [/RAW_FISH:2/, /CLOWNFISH/] },
            "pufferfish": { name: "Pufferfish", patterns: [/RAW_FISH:3/, /PUFFERFISH/] },
            "prismarine": { name: "Prismarine", patterns: [/PRISMARINE/] },
            "clay": { name: "Clay", patterns: [/^CLAY/] },
            "lily_pad": { name: "Lily Pad", patterns: [/LILY/, /WATER_LILY/] },
            "ink_sac": { name: "Ink Sac", patterns: [/^INK_SACK$/] },
            "sponge": { name: "Sponge", patterns: [/SPONGE/] },
            "shark": { name: "Shark Fins", patterns: [/SHARK/, /NURSE_SHARK/] },
            "sea_creature": { name: "Sea Creatures", patterns: [/^SQUID/, /SEA_LANTERN/, /GUARDIAN/, /NAUTILUS/, /CARROT_KING/, /DEEP_SEA/, /ALLIGATOR/] },
            "fishing_misc": { name: "Fishing Items", patterns: [/FISHING/, /BAIT/, /CHUM/, /MAGMAFISH/, /LURE_/, /BAYOU/, /FISH_AFFINITY/, /SPIKED_HOOK/] }
        }
    },
    oddities: {
        name: "Oddities",
        icon: "enchantment_table",
        color: 0xAA55FF,
        sections: {
            "essence": { name: "Essence", patterns: [/ESSENCE/] },
            "fragments": { name: "Fragments", patterns: [/_FRAGMENT$/, /BONZO_FRAGMENT/, /SCARF_FRAGMENT/, /LIVID_FRAGMENT/, /SADAN_FRAGMENT/, /NECRON_FRAGMENT/] },
            "booster_cookie": { name: "Booster Cookie", patterns: [/BOOSTER_COOKIE/] },
            "experience": { name: "Experience Bottles", patterns: [/^EXP_BOTTLE/, /^GRAND_EXP/, /^TITANIC_EXP/, /COLOSSAL_EXP/] },
            "catalysts": { name: "Catalysts", patterns: [/CATALYST/] },
            "recombobulator": { name: "Recombobulator", patterns: [/RECOMBOBULATOR/] },
            "hot_potato": { name: "Upgrade Books", patterns: [/POTATO_BOOK/, /HOT_POTATO_BOOK/, /FUMING_POTATO_BOOK/, /BOOK_OF_STATS/, /ETHERWARP_CONDUIT/, /SILEX/] },
            "master_stars": { name: "Master Stars", patterns: [/FIRST_MASTER_STAR/, /SECOND_MASTER_STAR/, /THIRD_MASTER_STAR/, /FOURTH_MASTER_STAR/, /FIFTH_MASTER_STAR/] },
            "enchantments": { name: "Enchantments", patterns: [/^ENCHANTMENT_/] },
            "pet_items": { name: "Pet Items", patterns: [/^PET_ITEM/, /CARROT_CANDY/, /CLOVER/, /EXP_SHARE_CORE/, /TIER_BOOST_CORE/, /CHYME/, /TEXTBOOK/, /SPOOKY_CUPCAKE/, /ALL_SKILLS/, /LUCKY_CLOVER/, /KAT_/] },
            "power_stones": { name: "Power Stones", patterns: [/SEARING_STONE/, /LUXURIOUS/, /ENDER_MONOCLE/, /MANA_DISINTEGRATOR/, /^FINE_.*_GEM$/, /_STONE$/] },
            "dungeon_items": { name: "Dungeon Items", patterns: [/DUNGEON/, /SPIRIT_LEAP/, /DECOY/, /SUPERBOOM/, /TRAINING_WEIGHTS/, /INFLATABLE_JERRY/, /SPIRIT_STONE/, /WITHER_CATALYST/, /^JERRY/, /GIANT_FRAGMENT/, /BONZO/, /SCARF/, /PROFESSOR/, /LIVID/, /SADAN/, /NECRON/, /WARDEN/, /ENSNARED/] },
            "minion_upgrades": { name: "Minion Upgrades", patterns: [/COMPACTOR/, /HOPPER/, /MINION/, /FLYCATCHER/, /SOULFLOW_ENGINE/, /DIAMOND_SPREADING/, /FUEL_TANK/, /AUTO_RECOMBOBULATOR/, /AUTO_SMELTER/, /SPREADING_/, /BUDDING/, /GENERATOR_UPGRADE/] },
            "forge": { name: "Forge Items", patterns: [/PERFECT_PLATE/, /REFINED_MINERAL/, /HOT_STUFF/, /MAGMA_CORE/, /BEJEWELED/, /BULKY_STONE/] },
            "rift": { name: "Rift Items", patterns: [/LIVING_METAL/, /AGARIMOO/, /HEMOVIBE/, /TIMITE/, /BERBERIS/, /CADUCOUS/, /HALF_EATEN/, /RIFT_/, /MOTES/, /WYLD_/, /DREADFARM/, /LURKER/, /ETHEREAL/, /FLESHTRAP/, /FLEXBONE/, /BANSHEE/, /WOOLEN_YARN/] },
            "bits": { name: "Bits Shop", patterns: [/KAT_FLOWER/, /KAT_BOUQUET/, /GOD_POTION/, /KISMET_FEATHER/] },
            "candy": { name: "Candy & Sweets", patterns: [/CANDY/, /CHOCOLATE/, /CHOCOBERRY/, /CHOCO/, /SWEET/, /CANDY_CORN/, /BONBON/] },
            "boosters": { name: "Boosters", patterns: [/BOOSTER$/, /FARMING_FOR_DUMMIES/, /FORTUNE_BOOSTER/, /WISDOM_BOOSTER/, /FIGHTING_BOOSTER/] },
            "coins": { name: "Coins & Currency", patterns: [/COINS/, /COIN$/, /MINTED/] }
        }
    },
    events: {
        name: "Events",
        icon: "firework",
        color: 0xFF55FF,
        sections: {
            "spooky": { name: "Spooky Festival", patterns: [/SPOOKY/, /PUMPKIN_LAUNCHER/, /WEREWOLF/, /VAMPIRE/, /^DARK_CANDY/, /^DARK_ORB/, /SOUL_FRAGMENT/, /SOUL_STRING/, /DARK_QUEENS/] },
            "winter": { name: "Winter", patterns: [/JERRY/, /SNOW/, /WHITE_GIFT/, /GREEN_GIFT/, /RED_GIFT/, /GIFT/, /YETI/, /FROZEN/, /FROST/, /GLACIAL/] },
            "dante": { name: "Dante Event", patterns: [/DANTE/, /RESISTANCE/] },
            "diana": { name: "Diana/Mythological", patterns: [/GRIFFIN/, /ANCIENT_CLAW/, /DAEDALUS/, /CHIMERA/, /MINOS/, /MYTHOLOGICAL/, /MINOTAUR/] },
            "fishing_festival": { name: "Fishing Festival", patterns: [/^FISH_/, /FISHING_FESTIVAL/] },
            "mayor": { name: "Mayor Items", patterns: [/MAYOR/, /SCORPIUS/, /JERRY_BOX/, /^AATROX/, /^MARINA/, /^FOXY/, /^COLE/, /^DERPY/, /^DIAZ/, /COUPON/] },
            "misc_event": { name: "Event Items", patterns: [/ANNIVERSARY/, /PRESENT/, /FIREWORK/, /LUCKY/, /CLOVER/, /BRAIN_FOOD/, /CAPSAICIN/, /RUNE/, /BEACH_BALL/, /FIRE_IN_A_BOTTLE/, /BOB_OMB/] }
        }
    },
    misc: {
        name: "Miscellaneous",
        icon: "hopper",
        color: 0xAAAAAA,
        sections: {
            "arrows": { name: "Arrows", patterns: [/^ARROW/, /ARROW_BUNDLE/, /FLINT_ARROW/, /BOW_/] },
            "dyes": { name: "Dyes & Colors", patterns: [/^DYE/, /^INK_SACK(?!:3)(?!:4)/, /^INK_SACK:/, /PAINT/] },
            "brewing": { name: "Brewing", patterns: [/^BREW/, /POTION/, /ALOE/, /FRUIT/, /INGREDIENT/, /OPAL_APPLE/, /APPLE/, /FLASK/, /VIAL/, /EXTRACT/] },
            "materials": { name: "Materials", patterns: [/HANDLE/, /BLADE/, /RING/, /CHAIN/, /CLIPPED_WINGS/, /TOOTH/, /HORN/, /CLAW/, /SCALE/, /SKIN/, /HEART/, /ORB$/, /ASHE/, /BLOOM/, /ROOT/, /TWIG/, /HUSK/, /BARK/, /MEMBRANE/, /RESIN/, /SAP/, /FIBER/, /THREAD/, /SHELL/, /CHITIN/] },
            "enchanted_misc": { name: "Enchanted Items", patterns: [/^ENCHANTED_BREAD/, /^ENCHANTED_DIAMOND/, /^ENCHANTED_EMERALD/, /^ENCHANTED_REDSTONE/] },
            "goblin_eggs": { name: "Goblin Eggs", patterns: [/GOBLIN_EGG/] },
            "garden_chips": { name: "Garden Chips", patterns: [/GARDEN_CHIP/] },
            "ores_misc": { name: "Ores & Minerals", patterns: [/ONITE$/, /GEODE$/, /IDOL$/, /GODSEED/, /GEMSTONE$/, /GLOSSY/, /GLASSCORN/] },
            "reforges": { name: "Reforge Items", patterns: [/REFORGE/, /ENTROPY/, /ENDERMAN_CORTEX/, /CONVERTER/, /TRANSMITTER/, /REFLECTOR/] },
            "slayer_misc": { name: "Slayer Items", patterns: [/STINGER/, /FEL_PEARL/, /FANGING/, /DEVOURER/, /DISPLACED/, /SNAIL/, /FURBALL/, /GAZING_PEARL/, /LEECH/] },
            "garden_misc": { name: "Garden Misc", patterns: [/SPORES/, /SOIL/, /DEAD_PLANT/, /SHROOM/, /VINE/, /GARDEN/, /CHALICE/, /WREATH/, /CUP_OF/, /BRONZE_BOWL/] },
            "other": { name: "Other", patterns: [/COUPON/, /SCRIPTURES/, /PAINTING/, /RADAR/, /ROPE/, /SWITCH/, /NOTES/, /DUST/, /BUILDER/, /ARCHITECT/, /STUFF/, /BOOK(?!_OF)/, /FREE_WILL/, /SWATTER/, /SHURIKEN/, /BALL$/, /CAN_OF/, /FLAMES$/] }
        }
    }
};

// State
let bazaarData = {};
let priceHistory = {};
let categorizedProducts = null; // Cache for categorized products

// Load saved data
function loadData() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            bazaarData = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
            console.log('[Bazaar] Loaded cached bazaar data');
        }
    } catch (e) {
        console.error('[Bazaar] Error loading data:', e.message);
    }
    
    try {
        if (fs.existsSync(HISTORY_FILE)) {
            priceHistory = JSON.parse(fs.readFileSync(HISTORY_FILE, 'utf8'));
            console.log('[Bazaar] Loaded price history');
        }
    } catch (e) {
        console.error('[Bazaar] Error loading history:', e.message);
    }
}

// Save data
function saveData() {
    try {
        fs.writeFileSync(DATA_FILE, JSON.stringify(bazaarData, null, 2));
        fs.writeFileSync(HISTORY_FILE, JSON.stringify(priceHistory));
    } catch (e) {
        console.error('[Bazaar] Error saving data:', e.message);
    }
}

// Categorize a product ID - returns { categoryId, sectionId } or null
function categorizeProduct(productId) {
    for (const [categoryId, category] of Object.entries(CATEGORY_PATTERNS)) {
        for (const [sectionId, section] of Object.entries(category.sections)) {
            for (const pattern of section.patterns) {
                if (pattern.test(productId)) {
                    return { categoryId, sectionId };
                }
            }
        }
    }
    return null;
}

// Build categorized products cache
function buildCategorizedProducts() {
    const result = {
        categories: {},
        uncategorized: []
    };
    
    // Initialize categories structure
    for (const [categoryId, category] of Object.entries(CATEGORY_PATTERNS)) {
        result.categories[categoryId] = {
            name: category.name,
            icon: category.icon,
            color: category.color,
            sections: {}
        };
        for (const [sectionId, section] of Object.entries(category.sections)) {
            result.categories[categoryId].sections[sectionId] = {
                name: section.name,
                items: []
            };
        }
    }
    
    // Categorize all products
    const products = bazaarData.products || {};
    for (const productId of Object.keys(products)) {
        const cat = categorizeProduct(productId);
        if (cat) {
            result.categories[cat.categoryId].sections[cat.sectionId].items.push(productId);
        } else {
            result.uncategorized.push(productId);
        }
    }
    
    // Sort items alphabetically within each section
    for (const category of Object.values(result.categories)) {
        for (const section of Object.values(category.sections)) {
            section.items.sort();
        }
    }
    result.uncategorized.sort();
    
    // Log uncategorized items for debugging
    if (result.uncategorized.length > 0) {
        console.log(`[Bazaar] ${result.uncategorized.length} uncategorized items:`, result.uncategorized.slice(0, 10).join(', '), '...');
    }
    
    return result;
}

// Fetch data from Hypixel API with API key
function fetchBazaarData() {
    return new Promise((resolve, reject) => {
        const url = new URL(HYPIXEL_API_URL);
        
        const options = {
            hostname: url.hostname,
            path: url.pathname,
            method: 'GET',
            headers: {
                'API-Key': HYPIXEL_API_KEY
            }
        };
        
        https.get(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    if (json.success) {
                        resolve(json.products);
                    } else {
                        reject(new Error('API returned success: false - ' + (json.cause || 'unknown')));
                    }
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

// Update bazaar data
async function updateBazaarData() {
    try {
        const products = await fetchBazaarData();
        const timestamp = Date.now();
        
        bazaarData = {
            timestamp,
            lastUpdate: new Date(timestamp).toISOString(),
            products: {}
        };
        
        for (const [productId, productData] of Object.entries(products)) {
            const quickStatus = productData.quick_status;
            
            bazaarData.products[productId] = {
                productId,
                buyPrice: quickStatus.buyPrice || 0,
                sellPrice: quickStatus.sellPrice || 0,
                buyVolume: quickStatus.buyVolume || 0,
                sellVolume: quickStatus.sellVolume || 0,
                buyOrders: quickStatus.buyOrders || 0,
                sellOrders: quickStatus.sellOrders || 0,
                buyMovingWeek: quickStatus.buyMovingWeek || 0,
                sellMovingWeek: quickStatus.sellMovingWeek || 0
            };
            
            // Store history
            if (!priceHistory[productId]) {
                priceHistory[productId] = [];
            }
            
            priceHistory[productId].push({
                timestamp,
                buyPrice: quickStatus.buyPrice || 0,
                sellPrice: quickStatus.sellPrice || 0
            });
            
            // Keep only last 7 days of history (10080 minutes at 1-min intervals)
            const sevenDaysAgo = timestamp - (7 * 24 * 60 * 60 * 1000);
            priceHistory[productId] = priceHistory[productId].filter(h => h.timestamp > sevenDaysAgo);
        }
        
        // Rebuild categorization cache
        categorizedProducts = buildCategorizedProducts();
        
        saveData();
        console.log(`[Bazaar] Updated ${Object.keys(bazaarData.products).length} products`);
        
    } catch (e) {
        console.error('[Bazaar] Error updating data:', e.message);
    }
}

// Get price at specific time period
function getPriceAtPeriod(productId, hoursAgo) {
    if (!priceHistory[productId] || priceHistory[productId].length === 0) {
        return null;
    }
    
    const targetTime = Date.now() - (hoursAgo * 60 * 60 * 1000);
    const history = priceHistory[productId];
    
    // Find closest entry to target time
    let closest = history[0];
    let closestDiff = Math.abs(history[0].timestamp - targetTime);
    
    for (const entry of history) {
        const diff = Math.abs(entry.timestamp - targetTime);
        if (diff < closestDiff) {
            closest = entry;
            closestDiff = diff;
        }
    }
    
    // Only return if within 10% of target time
    const tolerance = hoursAgo * 60 * 60 * 1000 * 0.1;
    if (closestDiff < tolerance || hoursAgo === 0) {
        return closest;
    }
    
    return null;
}

// Get item display name from product ID
function getDisplayName(productId) {
    return productId
        .replace(/:/g, ' ')
        .replace(/_/g, ' ')
        .toLowerCase()
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

// Send JSON response
function sendJson(res, statusCode, data) {
    res.writeHead(statusCode, {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    });
    res.end(JSON.stringify(data));
}

// Parse query parameters
function parseQuery(url) {
    const params = {};
    const queryIndex = url.indexOf('?');
    if (queryIndex !== -1) {
        const queryString = url.substring(queryIndex + 1);
        for (const param of queryString.split('&')) {
            const [key, value] = param.split('=');
            const decodedKey = decodeURIComponent((key || '').replace(/\+/g, ' '));
            const decodedValue = decodeURIComponent((value || '').replace(/\+/g, ' '));
            params[decodedKey] = decodedValue;
        }
    }
    return params;
}

// HTTP Server
const server = http.createServer((req, res) => {
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type'
        });
        res.end();
        return;
    }
    
    const urlPath = req.url.split('?')[0];
    const query = parseQuery(req.url);
    
    try {
        // Get categories
        if (urlPath === '/bazaar/categories') {
            if (!categorizedProducts) {
                categorizedProducts = buildCategorizedProducts();
            }
            
            const categoryList = [];
            for (const [id, cat] of Object.entries(categorizedProducts.categories)) {
                // Count total items across all sections
                let totalItems = 0;
                for (const section of Object.values(cat.sections)) {
                    totalItems += section.items.length;
                }
                
                categoryList.push({
                    id,
                    name: cat.name,
                    icon: cat.icon,
                    color: cat.color,
                    sectionCount: Object.keys(cat.sections).length,
                    itemCount: totalItems
                });
            }
            
            // Add uncategorized if there are items
            if (categorizedProducts.uncategorized.length > 0) {
                categoryList.push({
                    id: 'uncategorized',
                    name: 'Uncategorized',
                    icon: 'barrier',
                    color: 0x888888,
                    sectionCount: 1,
                    itemCount: categorizedProducts.uncategorized.length
                });
            }
            
            sendJson(res, 200, { 
                success: true, 
                categories: categoryList,
                totalProducts: Object.keys(bazaarData.products || {}).length
            });
        }
        
        // Get sections for a category
        else if (urlPath === '/bazaar/sections') {
            if (!categorizedProducts) {
                categorizedProducts = buildCategorizedProducts();
            }
            
            const categoryId = query.category;
            
            // Handle uncategorized
            if (categoryId === 'uncategorized') {
                sendJson(res, 200, {
                    success: true,
                    category: 'Uncategorized',
                    sections: [{
                        id: 'all',
                        name: 'All Items',
                        itemCount: categorizedProducts.uncategorized.length
                    }]
                });
                return;
            }
            
            const category = categorizedProducts.categories[categoryId];
            
            if (!category) {
                sendJson(res, 404, { success: false, error: 'Category not found' });
                return;
            }
            
            const sections = [];
            for (const [id, section] of Object.entries(category.sections)) {
                // Only include sections that have items
                if (section.items.length > 0) {
                    sections.push({
                        id,
                        name: section.name,
                        itemCount: section.items.length
                    });
                }
            }
            
            sendJson(res, 200, { 
                success: true, 
                category: category.name,
                sections 
            });
        }
        
        // Get items for a section
        else if (urlPath === '/bazaar/items') {
            if (!categorizedProducts) {
                categorizedProducts = buildCategorizedProducts();
            }
            
            const categoryId = query.category;
            const sectionId = query.section;
            
            let productIds = [];
            let sectionName = '';
            
            // Handle uncategorized
            if (categoryId === 'uncategorized') {
                productIds = categorizedProducts.uncategorized;
                sectionName = 'All Items';
            } else {
                const category = categorizedProducts.categories[categoryId];
                if (!category) {
                    sendJson(res, 404, { success: false, error: 'Category not found' });
                    return;
                }
                
                const section = category.sections[sectionId];
                if (!section) {
                    sendJson(res, 404, { success: false, error: 'Section not found' });
                    return;
                }
                
                productIds = section.items;
                sectionName = section.name;
            }
            
            const items = [];
            for (const productId of productIds) {
                const product = bazaarData.products?.[productId];
                if (product) {
                    items.push({
                        productId,
                        displayName: getDisplayName(productId),
                        buyPrice: product.buyPrice,
                        sellPrice: product.sellPrice,
                        buyVolume: product.buyVolume,
                        sellVolume: product.sellVolume
                    });
                }
            }
            
            sendJson(res, 200, {
                success: true,
                section: sectionName,
                items,
                lastUpdate: bazaarData.lastUpdate
            });
        }
        
        // Get single item details with price history
        else if (urlPath === '/bazaar/item') {
            const productId = query.id;
            
            if (!productId) {
                sendJson(res, 400, { success: false, error: 'Missing item id' });
                return;
            }
            
            const product = bazaarData.products?.[productId];
            if (!product) {
                sendJson(res, 404, { success: false, error: 'Item not found' });
                return;
            }
            
            // Get historical prices
            const price1h = getPriceAtPeriod(productId, 1);
            const price1d = getPriceAtPeriod(productId, 24);
            const price7d = getPriceAtPeriod(productId, 168);
            
            const response = {
                success: true,
                item: {
                    productId,
                    displayName: getDisplayName(productId),
                    current: {
                        buyPrice: product.buyPrice,
                        sellPrice: product.sellPrice,
                        buyVolume: product.buyVolume,
                        sellVolume: product.sellVolume,
                        buyOrders: product.buyOrders,
                        sellOrders: product.sellOrders,
                        buyMovingWeek: product.buyMovingWeek,
                        sellMovingWeek: product.sellMovingWeek
                    },
                    history: {
                        oneHour: price1h ? { buyPrice: price1h.buyPrice, sellPrice: price1h.sellPrice } : null,
                        oneDay: price1d ? { buyPrice: price1d.buyPrice, sellPrice: price1d.sellPrice } : null,
                        sevenDays: price7d ? { buyPrice: price7d.buyPrice, sellPrice: price7d.sellPrice } : null
                    }
                },
                lastUpdate: bazaarData.lastUpdate
            };
            
            sendJson(res, 200, response);
        }
        
        // Search items - searches ALL products dynamically
        else if (urlPath === '/bazaar/search') {
            const searchQuery = (query.q || '').toLowerCase();
            
            if (searchQuery.length < 2) {
                sendJson(res, 400, { success: false, error: 'Search query too short' });
                return;
            }
            
            if (!categorizedProducts) {
                categorizedProducts = buildCategorizedProducts();
            }
            
            const results = [];
            
            // Search all products
            for (const productId of Object.keys(bazaarData.products || {})) {
                const displayName = getDisplayName(productId);
                
                if (displayName.toLowerCase().includes(searchQuery) || 
                    productId.toLowerCase().includes(searchQuery)) {
                    
                    const product = bazaarData.products[productId];
                    const cat = categorizeProduct(productId);
                    
                    let categoryName = 'Uncategorized';
                    let categoryId = 'uncategorized';
                    let sectionName = 'All Items';
                    let sectionId = 'all';
                    
                    if (cat) {
                        const category = categorizedProducts.categories[cat.categoryId];
                        categoryName = category.name;
                        categoryId = cat.categoryId;
                        sectionName = category.sections[cat.sectionId].name;
                        sectionId = cat.sectionId;
                    }
                    
                    results.push({
                        productId,
                        displayName,
                        category: categoryName,
                        categoryId,
                        section: sectionName,
                        sectionId,
                        buyPrice: product?.buyPrice || 0,
                        sellPrice: product?.sellPrice || 0
                    });
                }
            }
            
            // Sort by relevance (exact matches first)
            results.sort((a, b) => {
                const aExact = a.displayName.toLowerCase() === searchQuery;
                const bExact = b.displayName.toLowerCase() === searchQuery;
                if (aExact && !bExact) return -1;
                if (!aExact && bExact) return 1;
                return a.displayName.localeCompare(b.displayName);
            });
            
            sendJson(res, 200, {
                success: true,
                query: searchQuery,
                results: results.slice(0, 50), // Limit to 50 results
                total: results.length
            });
        }
        
        // Get all products (raw data)
        else if (urlPath === '/bazaar/all') {
            sendJson(res, 200, {
                success: true,
                products: bazaarData.products || {},
                lastUpdate: bazaarData.lastUpdate
            });
        }
        
        // Health check
        else if (urlPath === '/health') {
            sendJson(res, 200, {
                status: 'ok',
                productCount: Object.keys(bazaarData.products || {}).length,
                categorizedCount: categorizedProducts ? 
                    Object.keys(bazaarData.products || {}).length - categorizedProducts.uncategorized.length : 0,
                uncategorizedCount: categorizedProducts ? categorizedProducts.uncategorized.length : 0,
                lastUpdate: bazaarData.lastUpdate,
                uptime: process.uptime()
            });
        }
        
        // 404
        else {
            sendJson(res, 404, { error: 'Not found' });
        }
        
    } catch (error) {
        console.error('[Bazaar] Error:', error);
        sendJson(res, 500, { error: 'Internal server error' });
    }
});

// Start server
loadData();
updateBazaarData();

// Update every minute
setInterval(updateBazaarData, UPDATE_INTERVAL);

server.listen(PORT, () => {
    console.log('=================================');
    console.log('  RavenClient Bazaar API');
    console.log('  Dynamic Item Loading Enabled');
    console.log('=================================');
    console.log(`Listening on port ${PORT}`);
    console.log('');
    console.log('Endpoints:');
    console.log('  GET /bazaar/categories');
    console.log('  GET /bazaar/sections?category=<id>');
    console.log('  GET /bazaar/items?category=<id>&section=<id>');
    console.log('  GET /bazaar/item?id=<productId>');
    console.log('  GET /bazaar/search?q=<query>');
    console.log('  GET /bazaar/all');
    console.log('  GET /health');
    console.log('');
    console.log('Ready for connections...');
});
