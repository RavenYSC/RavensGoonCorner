/**
 * RavenClient Bazaar API Server
 * 
 * Pulls data from Hypixel API and stores price history
 * Dynamically reads all available bazaar items
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
const CATEGORY_PATTERNS = {
    farming: {
        name: "Farming",
        icon: "golden_hoe",
        color: 0xFFAA00,
        patterns: [
            /^WHEAT/, /^SEEDS/, /^BREAD/, /^HAY/,
            /^CARROT/, /^POTATO/, /^PUMPKIN/, /^MELON/,
            /^MUSHROOM/, /^RED_MUSHROOM/, /^BROWN_MUSHROOM/,
            /^COCOA/, /^COOKIE/, /^INK_SACK:3/,
            /^CACTUS/, /^SUGAR/, /^PAPER/,
            /^LEATHER/, /^BEEF/, /^PORK/, /^CHICKEN/, /^MUTTON/, /^RABBIT/, /^FEATHER/, /^EGG/, /^WOOL/,
            /^NETHER_STALK/, /^MUTANT_NETHER/,
            /^COMPOST/, /^CROPIE/, /^SQUASH/, /^FERMENTO/, /^DUNG/, /^CHEESE/, /^PLANT_MATTER/, /^HONEY/, /^LARVA/, /^FLOUR/
        ],
        sections: {
            "wheat_seeds": { name: "Wheat & Seeds", patterns: [/^WHEAT/, /^SEEDS/, /^BREAD/, /^HAY/] },
            "carrot": { name: "Carrot", patterns: [/CARROT/] },
            "potato": { name: "Potato", patterns: [/POTATO/] },
            "pumpkin": { name: "Pumpkin", patterns: [/PUMPKIN/] },
            "melon": { name: "Melon", patterns: [/MELON/] },
            "mushroom": { name: "Mushrooms", patterns: [/MUSHROOM/] },
            "cocoa": { name: "Cocoa Beans", patterns: [/^INK_SACK:3/, /COCOA/, /COOKIE/] },
            "cactus": { name: "Cactus", patterns: [/CACTUS/] },
            "sugar_cane": { name: "Sugar Cane", patterns: [/SUGAR/, /PAPER/] },
            "leather_beef": { name: "Leather & Beef", patterns: [/LEATHER/, /BEEF/] },
            "pork": { name: "Pork", patterns: [/PORK/] },
            "chicken": { name: "Chicken & Feather", patterns: [/CHICKEN/, /FEATHER/, /^EGG/, /CAKE/] },
            "mutton": { name: "Mutton & Wool", patterns: [/MUTTON/, /WOOL/] },
            "rabbit": { name: "Rabbit", patterns: [/RABBIT/] },
            "nether_wart": { name: "Nether Warts", patterns: [/NETHER_STALK/, /MUTANT_NETHER/] },
            "garden": { name: "Garden", patterns: [/COMPOST/, /CROPIE/, /SQUASH/, /FERMENTO/, /DUNG/, /CHEESE/, /PLANT_MATTER/, /HONEY/, /LARVA/, /FLOUR/] }
        }
    },
    mining: {
        name: "Mining",
        icon: "diamond_pickaxe",
        color: 0x55FFFF,
        patterns: [
            /^COBBLESTONE/, /^COAL/, /^CHARCOAL/,
            /^IRON/, /^GOLD/, /^DIAMOND/, /^LAPIS/, /^INK_SACK:4/, /^EMERALD/,
            /^REDSTONE/, /^QUARTZ/, /^OBSIDIAN/, /^GLOWSTONE/,
            /^GRAVEL/, /^FLINT/, /^ICE/, /^PACKED_ICE/, /^BLUE_ICE/,
            /^NETHERRACK/, /^SAND/, /^RED_SAND/, /^END/, /^ENDER_STONE/,
            /^SNOW/, /^MITHRIL/, /^TITANIUM/, /^STARFALL/, /^UMBER/, /^TUNGSTEN/, /^GLACITE/,
            /GEM$/, /_GEM/, /RUBY/, /AMBER/, /SAPPHIRE/, /JADE/, /AMETHYST/, /TOPAZ/, /JASPER/, /OPAL/, /ONYX/, /AQUAMARINE/, /CITRINE/, /PERIDOT/,
            /^HARD_STONE/, /^CONCENTRATED/, /^SULPHUR_ORE/, /^MYCEL/,
            /MAGMA_BUCKET/, /PLASMA/, /LAVA_BUCKET/, /LAVA_PEARL/, /FUEL/,
            /^OIL_BARREL/, /^VOLTA/, /^TREASURITE/, /^BLOBFISH/, /^WORM_MEMBRANE/
        ],
        sections: {
            "cobblestone": { name: "Cobblestone", patterns: [/^COBBLESTONE/] },
            "coal": { name: "Coal", patterns: [/^COAL/, /^CHARCOAL/, /COAL_BLOCK/] },
            "iron": { name: "Iron", patterns: [/^IRON/] },
            "gold": { name: "Gold", patterns: [/^GOLD(?!EN)/] },
            "diamond": { name: "Diamond", patterns: [/^DIAMOND/, /REFINED_DIAMOND/] },
            "lapis": { name: "Lapis Lazuli", patterns: [/^INK_SACK:4/, /LAPIS/] },
            "emerald": { name: "Emerald", patterns: [/^EMERALD/] },
            "redstone": { name: "Redstone", patterns: [/^REDSTONE/, /REDSTONE_LAMP/] },
            "quartz": { name: "Quartz", patterns: [/QUARTZ/] },
            "obsidian": { name: "Obsidian", patterns: [/OBSIDIAN/] },
            "glowstone": { name: "Glowstone", patterns: [/GLOWSTONE/] },
            "gravel": { name: "Gravel & Flint", patterns: [/^GRAVEL/, /^FLINT/] },
            "ice": { name: "Ice", patterns: [/^ICE/, /PACKED_ICE/, /BLUE_ICE/] },
            "netherrack": { name: "Netherrack", patterns: [/NETHERRACK/] },
            "sand": { name: "Sand", patterns: [/^SAND/, /RED_SAND/] },
            "end_stone": { name: "End Stone", patterns: [/ENDER_STONE/, /ENDSTONE/] },
            "snow": { name: "Snow", patterns: [/^SNOW/] },
            "mithril": { name: "Mithril", patterns: [/MITHRIL/] },
            "titanium": { name: "Titanium", patterns: [/TITANIUM/] },
            "starfall": { name: "Starfall", patterns: [/STARFALL/] },
            "umber": { name: "Umber", patterns: [/UMBER/] },
            "tungsten": { name: "Tungsten", patterns: [/TUNGSTEN/] },
            "glacite": { name: "Glacite", patterns: [/GLACITE/] },
            "ruby": { name: "Ruby", patterns: [/RUBY/] },
            "amber": { name: "Amber", patterns: [/AMBER/] },
            "sapphire": { name: "Sapphire", patterns: [/SAPPHIRE/] },
            "jade": { name: "Jade", patterns: [/JADE/] },
            "amethyst": { name: "Amethyst", patterns: [/AMETHYST/] },
            "topaz": { name: "Topaz", patterns: [/TOPAZ/] },
            "jasper": { name: "Jasper", patterns: [/JASPER/] },
            "opal": { name: "Opal", patterns: [/OPAL/] },
            "onyx": { name: "Onyx", patterns: [/ONYX/] },
            "aquamarine": { name: "Aquamarine", patterns: [/AQUAMARINE/] },
            "citrine": { name: "Citrine", patterns: [/CITRINE/] },
            "peridot": { name: "Peridot", patterns: [/PERIDOT/] },
            "hard_stone": { name: "Hard Stone", patterns: [/HARD_STONE/, /CONCENTRATED_STONE/] },
            "sulphur_mining": { name: "Sulphur Ore", patterns: [/^SULPHUR_ORE/, /SULPHUR_CUBE/] },
            "mycelium": { name: "Mycelium", patterns: [/MYCEL/] },
            "fuel": { name: "Fuel & Oil", patterns: [/MAGMA_BUCKET/, /MAGMA_CORE/, /INFERNO_VERTEX/, /PLASMA/, /LAVA_BUCKET/, /LAVA_PEARL/, /OIL_BARREL/, /FUEL/] },
            "dwarven": { name: "Dwarven Mines", patterns: [/TREASURITE/, /VOLTA/, /WORM_MEMBRANE/] }
        }
    },
    combat: {
        name: "Combat",
        icon: "iron_sword",
        color: 0xFF5555,
        patterns: [
            /^ROTTEN/, /REVENANT/, /FOUL_FLESH/,
            /^BONE/, /^STRING/, /TARANTULA/, /^SPIDER/,
            /^SULPHUR$/, /GUNPOWDER/, /FIREWORK/,
            /ENDER_PEARL/, /EYE_OF_ENDER/, /^GHAST/, /^SLIME/, /SLUDGE/, /YOGGIE/,
            /^BLAZE/, /^MAGMA_CREAM/, /WHIPPED_MAGMA/,
            /WOLF_TOOTH/, /GOLDEN_TOOTH/, /^SOULFLOW/, /^RAW_SOULFLOW/, /^NULL_/,
            /CHILI_PEPPER/, /INFERNO/, /KUUDRA/, /NETHER_STAR/,
            /^GRIFF/, /^VENOM/, /^SNAKE_RUNE/, /^ZEALOT/
        ],
        sections: {
            "rotten_flesh": { name: "Rotten Flesh", patterns: [/ROTTEN/, /REVENANT/, /FOUL_FLESH/] },
            "bone": { name: "Bone", patterns: [/^BONE/] },
            "string": { name: "String", patterns: [/^STRING/, /TARANTULA/] },
            "spider_eye": { name: "Spider Eye", patterns: [/SPIDER_EYE/, /FERMENTED/] },
            "gunpowder": { name: "Gunpowder", patterns: [/^SULPHUR$/, /GUNPOWDER/, /FIREWORK/] },
            "ender_pearl": { name: "Ender Pearl", patterns: [/ENDER_PEARL/, /EYE_OF_ENDER/, /ABSOLUTE_ENDER/] },
            "ghast_tear": { name: "Ghast Tear", patterns: [/GHAST/] },
            "slime": { name: "Slimeball", patterns: [/SLIME/, /SLUDGE/, /YOGGIE/] },
            "blaze_rod": { name: "Blaze Rod", patterns: [/BLAZE/] },
            "magma_cream": { name: "Magma Cream", patterns: [/MAGMA_CREAM/, /WHIPPED_MAGMA/] },
            "wolf_tooth": { name: "Wolf Tooth", patterns: [/WOLF_TOOTH/, /GOLDEN_TOOTH/] },
            "soulflow": { name: "Soulflow", patterns: [/SOULFLOW/, /^NULL_/] },
            "nether_star": { name: "Nether Star", patterns: [/NETHER_STAR/] },
            "crimson_isle": { name: "Crimson Isle", patterns: [/CHILI/, /INFERNO/, /KUUDRA/, /GRIFF/, /VENOM/] }
        }
    },
    woods_fishes: {
        name: "Woods & Fishes",
        icon: "fishing_rod",
        color: 0x55FF55,
        patterns: [
            /^LOG/, /OAK_LOG/, /SPRUCE_LOG/, /BIRCH_LOG/, /DARK_OAK_LOG/, /ACACIA_LOG/, /JUNGLE_LOG/,
            /^RAW_FISH/, /SALMON/, /CLOWNFISH/, /PUFFERFISH/,
            /PRISMARINE/, /^CLAY/, /LILY/, /WATER_LILY/, /^INK_SACK$/, /^INK_SACK(?!:)/, /SPONGE/,
            /SHARK/, /^SQUID/, /SEA_LANTERN/, /GUARDIAN/, /NAUTILUS/, /MAGMAFISH/, /LURE_INGREDIENT/
        ],
        sections: {
            "oak": { name: "Oak", patterns: [/^LOG$/, /OAK_LOG/] },
            "spruce": { name: "Spruce", patterns: [/^LOG:1/, /SPRUCE/] },
            "birch": { name: "Birch", patterns: [/^LOG:2/, /BIRCH/] },
            "dark_oak": { name: "Dark Oak", patterns: [/^LOG_2:1/, /DARK_OAK/] },
            "acacia": { name: "Acacia", patterns: [/^LOG_2$/, /ACACIA/] },
            "jungle": { name: "Jungle", patterns: [/^LOG:3/, /JUNGLE/] },
            "raw_fish": { name: "Raw Fish", patterns: [/^RAW_FISH$/, /RAW_FISH(?!:)/, /COOKED_FISH/] },
            "salmon": { name: "Salmon", patterns: [/RAW_FISH:1/, /SALMON/] },
            "clownfish": { name: "Clownfish", patterns: [/RAW_FISH:2/, /CLOWNFISH/] },
            "pufferfish": { name: "Pufferfish", patterns: [/RAW_FISH:3/, /PUFFERFISH/] },
            "prismarine": { name: "Prismarine", patterns: [/PRISMARINE/] },
            "clay": { name: "Clay", patterns: [/^CLAY/] },
            "lily_pad": { name: "Lily Pad", patterns: [/LILY/, /WATER_LILY/] },
            "ink_sac": { name: "Ink Sac", patterns: [/^INK_SACK$/] },
            "sponge": { name: "Sponge", patterns: [/SPONGE/] },
            "shark": { name: "Shark Fins", patterns: [/SHARK/] },
            "sea_creature": { name: "Sea Creatures", patterns: [/SQUID/, /SEA_LANTERN/, /GUARDIAN/, /NAUTILUS/] },
            "magmafish": { name: "Magmafish", patterns: [/MAGMAFISH/] },
            "chumming": { name: "Chumming", patterns: [/LURE_INGREDIENT/] }
        }
    },
    oddities: {
        name: "Oddities",
        icon: "enchantment_table",
        color: 0xAA55FF,
        patterns: [
            /ESSENCE/, /FRAGMENT$/, /BOOSTER_COOKIE/, /EXP_BOTTLE/, /CATALYST/,
            /RECOMBOBULATOR/, /POTATO_BOOK/, /JACOBS_TICKET/, /MASTER_STAR/,
            /ENCHANTMENT_/, /PET_ITEM/, /CARROT_CANDY/, /CLOVER/, /EXP_SHARE/, /TIER_BOOST/,
            /SEARING_STONE/, /LUXURIOUS/, /ENDER_MONOCLE/, /MANA_DISINTEGRATOR/,
            /DUNGEON/, /SPIRIT_LEAP/, /JERRY/, /DECOY/, /SUPERBOOM/, /TRAINING_WEIGHTS/,
            /PERFECT_PLATE/, /REFINED_MINERAL/, /HOT_STUFF/,
            /LIVING_METAL/, /AGARIMOO/, /HEMOVIBE/, /MOOGMA/,
            /KAT_FLOWER/, /GOD_POTION/, /COMPACTOR/, /HOPPER/, /MINION/, /FLYCATCHER/, /SOULFLOW_ENGINE/, /DIAMOND_SPREADING/
        ],
        sections: {
            "essence": { name: "Essence", patterns: [/ESSENCE/] },
            "dragon_fragments": { name: "Dragon Fragments", patterns: [/SUPERIOR_FRAGMENT/, /STRONG_FRAGMENT/, /UNSTABLE_FRAGMENT/, /YOUNG_FRAGMENT/, /OLD_FRAGMENT/, /WISE_FRAGMENT/, /PROTECTOR_FRAGMENT/, /HOLY_FRAGMENT/] },
            "booster_cookie": { name: "Booster Cookie", patterns: [/BOOSTER_COOKIE/] },
            "experience": { name: "Experience Bottles", patterns: [/EXP_BOTTLE/] },
            "catalysts": { name: "Catalysts", patterns: [/CATALYST/] },
            "recombobulator": { name: "Recombobulator", patterns: [/RECOMBOBULATOR/] },
            "hot_potato": { name: "Potato Books", patterns: [/POTATO_BOOK/] },
            "jacobs_ticket": { name: "Jacob's Ticket", patterns: [/JACOBS_TICKET/] },
            "master_stars": { name: "Master Stars", patterns: [/MASTER_STAR/] },
            "enchantments": { name: "Enchantments", patterns: [/ENCHANTMENT_/] },
            "pet_items": { name: "Pet Items", patterns: [/PET_ITEM/, /CARROT_CANDY/, /CLOVER/, /EXP_SHARE_CORE/, /TIER_BOOST/, /CHYME/, /TEXTBOOK/, /SPOOKY_CUPCAKE/, /ALL_SKILLS/] },
            "power_stones": { name: "Power Stones", patterns: [/SEARING_STONE/, /LUXURIOUS/, /ENDER_MONOCLE/, /MANA_DISINTEGRATOR/] },
            "dungeon_items": { name: "Dungeon Items", patterns: [/DUNGEON/, /SPIRIT_LEAP/, /JERRY/, /DECOY/, /SUPERBOOM/, /TRAINING_WEIGHTS/] },
            "minion_upgrades": { name: "Minion Upgrades", patterns: [/COMPACTOR/, /HOPPER/, /MINION/, /FLYCATCHER/, /SOULFLOW_ENGINE/, /DIAMOND_SPREADING/, /LAVA_BUCKET/, /FUEL_TANK/] },
            "forge": { name: "Forge Items", patterns: [/PERFECT_PLATE/, /REFINED_MINERAL/, /HOT_STUFF/, /TREASURITE/] },
            "rift": { name: "Rift Items", patterns: [/LIVING_METAL/, /AGARIMOO/, /HEMOVIBE/, /MOOGMA/] },
            "bits": { name: "Bits Items", patterns: [/KAT_FLOWER/, /GOD_POTION/, /BUILDER/] }
        }
    }
};

// State
let bazaarData = {};
let priceHistory = {};

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

// Fetch data from Hypixel API
function fetchBazaarData() {
    return new Promise((resolve, reject) => {
        https.get(HYPIXEL_API_URL, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    if (json.success) {
                        resolve(json.products);
                    } else {
                        reject(new Error('API returned success: false'));
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
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, c => c.toUpperCase());
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
            const categoryList = [];
            for (const [id, cat] of Object.entries(CATEGORIES)) {
                categoryList.push({
                    id,
                    name: cat.name,
                    icon: cat.icon,
                    color: cat.color,
                    sectionCount: Object.keys(cat.sections).length
                });
            }
            sendJson(res, 200, { success: true, categories: categoryList });
        }
        
        // Get sections for a category
        else if (urlPath === '/bazaar/sections') {
            const categoryId = query.category;
            const category = CATEGORIES[categoryId];
            
            if (!category) {
                sendJson(res, 404, { success: false, error: 'Category not found' });
                return;
            }
            
            const sections = [];
            for (const [id, section] of Object.entries(category.sections)) {
                sections.push({
                    id,
                    name: section.name,
                    itemCount: section.items.length
                });
            }
            
            sendJson(res, 200, { 
                success: true, 
                category: category.name,
                sections 
            });
        }
        
        // Get items for a section
        else if (urlPath === '/bazaar/items') {
            const categoryId = query.category;
            const sectionId = query.section;
            
            const category = CATEGORIES[categoryId];
            if (!category) {
                sendJson(res, 404, { success: false, error: 'Category not found' });
                return;
            }
            
            const section = category.sections[sectionId];
            if (!section) {
                sendJson(res, 404, { success: false, error: 'Section not found' });
                return;
            }
            
            const items = [];
            for (const productId of section.items) {
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
                } else {
                    // Item exists in config but not in API
                    items.push({
                        productId,
                        displayName: getDisplayName(productId),
                        buyPrice: 0,
                        sellPrice: 0,
                        buyVolume: 0,
                        sellVolume: 0,
                        unavailable: true
                    });
                }
            }
            
            sendJson(res, 200, {
                success: true,
                section: section.name,
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
        
        // Search items
        else if (urlPath === '/bazaar/search') {
            const searchQuery = (query.q || '').toLowerCase();
            
            if (searchQuery.length < 2) {
                sendJson(res, 400, { success: false, error: 'Search query too short' });
                return;
            }
            
            const results = [];
            
            for (const [categoryId, category] of Object.entries(CATEGORIES)) {
                for (const [sectionId, section] of Object.entries(category.sections)) {
                    for (const productId of section.items) {
                        const displayName = getDisplayName(productId);
                        
                        if (displayName.toLowerCase().includes(searchQuery) || 
                            productId.toLowerCase().includes(searchQuery)) {
                            
                            const product = bazaarData.products?.[productId];
                            results.push({
                                productId,
                                displayName,
                                category: category.name,
                                categoryId,
                                section: section.name,
                                sectionId,
                                buyPrice: product?.buyPrice || 0,
                                sellPrice: product?.sellPrice || 0
                            });
                        }
                    }
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
