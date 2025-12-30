package com.raven.client.gui;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;

public class GuiBazaar extends GuiScreen {
    
    private static final String BAZAAR_API_URL = "http://100.42.184.35:25581";
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.0");
    private static final DecimalFormat VOLUME_FORMAT = new DecimalFormat("#,##0");
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // UI Components
    private GuiTextField searchField;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int sectionScrollOffset = 0;
    private int maxSectionScroll = 0;
    
    // Data
    private List<BazaarCategory> categories = new ArrayList<>();
    private List<BazaarSection> sections = new ArrayList<>();
    private List<BazaarItem> items = new ArrayList<>();
    private List<BazaarItem> searchResults = new ArrayList<>();
    
    // State
    private String selectedCategory = null;
    private String selectedSection = null;
    private BazaarItem hoveredItem = null;
    private BazaarItemDetail hoveredItemDetail = null;
    private String statusMessage = "Loading categories...";
    private boolean isLoading = false;
    private boolean showSearch = false;
    
    // Layout constants
    private int panelX, panelY, panelWidth, panelHeight;
    private int categoryWidth = 120;
    private int sectionWidth = 140;
    
    // Display name overrides for cleaner item names
    private static final java.util.Map<String, String> DISPLAY_NAMES = new java.util.HashMap<>();
    static {
        // Farming
        DISPLAY_NAMES.put("WHEAT", "Wheat");
        DISPLAY_NAMES.put("ENCHANTED_BREAD", "Enchanted Bread");
        DISPLAY_NAMES.put("ENCHANTED_HAY_BLOCK", "Enchanted Hay Bale");
        DISPLAY_NAMES.put("TIGHTLY_TIED_HAY_BALE", "Tightly-Tied Hay Bale");
        DISPLAY_NAMES.put("SEEDS", "Seeds");
        DISPLAY_NAMES.put("ENCHANTED_SEEDS", "Enchanted Seeds");
        DISPLAY_NAMES.put("BOX_OF_SEEDS", "Box of Seeds");
        DISPLAY_NAMES.put("CARROT_ITEM", "Carrot");
        DISPLAY_NAMES.put("ENCHANTED_CARROT", "Enchanted Carrot");
        DISPLAY_NAMES.put("ENCHANTED_CARROT_STICK", "Enchanted Carrot on a Stick");
        DISPLAY_NAMES.put("ENCHANTED_GOLDEN_CARROT", "Enchanted Golden Carrot");
        DISPLAY_NAMES.put("POTATO_ITEM", "Potato");
        DISPLAY_NAMES.put("POISONOUS_POTATO", "Poisonous Potato");
        DISPLAY_NAMES.put("ENCHANTED_POTATO", "Enchanted Potato");
        DISPLAY_NAMES.put("ENCHANTED_POISONOUS_POTATO", "Enchanted Poisonous Potato");
        DISPLAY_NAMES.put("ENCHANTED_BAKED_POTATO", "Enchanted Baked Potato");
        DISPLAY_NAMES.put("PUMPKIN", "Pumpkin");
        DISPLAY_NAMES.put("ENCHANTED_PUMPKIN", "Enchanted Pumpkin");
        DISPLAY_NAMES.put("POLISHED_PUMPKIN", "Polished Pumpkin");
        DISPLAY_NAMES.put("MELON", "Melon Slice");
        DISPLAY_NAMES.put("ENCHANTED_MELON", "Enchanted Melon");
        DISPLAY_NAMES.put("ENCHANTED_GLISTERING_MELON", "Enchanted Glistering Melon");
        DISPLAY_NAMES.put("MELON_BLOCK", "Melon Block");
        DISPLAY_NAMES.put("ENCHANTED_MELON_BLOCK", "Enchanted Melon Block");
        DISPLAY_NAMES.put("RED_MUSHROOM", "Red Mushroom");
        DISPLAY_NAMES.put("ENCHANTED_RED_MUSHROOM", "Enchanted Red Mushroom");
        DISPLAY_NAMES.put("RED_MUSHROOM_BLOCK", "Red Mushroom Block");
        DISPLAY_NAMES.put("ENCHANTED_RED_MUSHROOM_BLOCK", "Enchanted Red Mushroom Block");
        DISPLAY_NAMES.put("BROWN_MUSHROOM", "Brown Mushroom");
        DISPLAY_NAMES.put("ENCHANTED_BROWN_MUSHROOM", "Enchanted Brown Mushroom");
        DISPLAY_NAMES.put("BROWN_MUSHROOM_BLOCK", "Brown Mushroom Block");
        DISPLAY_NAMES.put("ENCHANTED_BROWN_MUSHROOM_BLOCK", "Enchanted Brown Mushroom Block");
        DISPLAY_NAMES.put("INK_SACK:3", "Cocoa Beans");
        DISPLAY_NAMES.put("ENCHANTED_COCOA", "Enchanted Cocoa Bean");
        DISPLAY_NAMES.put("ENCHANTED_COOKIE", "Enchanted Cookie");
        DISPLAY_NAMES.put("CACTUS", "Cactus");
        DISPLAY_NAMES.put("ENCHANTED_CACTUS_GREEN", "Enchanted Cactus Green");
        DISPLAY_NAMES.put("ENCHANTED_CACTUS", "Enchanted Cactus");
        DISPLAY_NAMES.put("SUGAR_CANE", "Sugar Cane");
        DISPLAY_NAMES.put("ENCHANTED_SUGAR", "Enchanted Sugar");
        DISPLAY_NAMES.put("ENCHANTED_PAPER", "Enchanted Paper");
        DISPLAY_NAMES.put("ENCHANTED_SUGAR_CANE", "Enchanted Sugar Cane");
        DISPLAY_NAMES.put("LEATHER", "Leather");
        DISPLAY_NAMES.put("ENCHANTED_LEATHER", "Enchanted Leather");
        DISPLAY_NAMES.put("RAW_BEEF", "Raw Beef");
        DISPLAY_NAMES.put("ENCHANTED_RAW_BEEF", "Enchanted Raw Beef");
        DISPLAY_NAMES.put("PORK", "Raw Porkchop");
        DISPLAY_NAMES.put("ENCHANTED_PORK", "Enchanted Pork");
        DISPLAY_NAMES.put("ENCHANTED_GRILLED_PORK", "Enchanted Grilled Pork");
        DISPLAY_NAMES.put("RAW_CHICKEN", "Raw Chicken");
        DISPLAY_NAMES.put("ENCHANTED_RAW_CHICKEN", "Enchanted Raw Chicken");
        DISPLAY_NAMES.put("FEATHER", "Feather");
        DISPLAY_NAMES.put("ENCHANTED_FEATHER", "Enchanted Feather");
        DISPLAY_NAMES.put("EGG", "Egg");
        DISPLAY_NAMES.put("ENCHANTED_EGG", "Enchanted Egg");
        DISPLAY_NAMES.put("SUPER_EGG", "Super Enchanted Egg");
        DISPLAY_NAMES.put("OMEGA_EGG", "Omega Enchanted Egg");
        DISPLAY_NAMES.put("MUTTON", "Raw Mutton");
        DISPLAY_NAMES.put("ENCHANTED_MUTTON", "Enchanted Mutton");
        DISPLAY_NAMES.put("ENCHANTED_COOKED_MUTTON", "Enchanted Cooked Mutton");
        DISPLAY_NAMES.put("WOOL", "Wool");
        DISPLAY_NAMES.put("ENCHANTED_WOOL", "Enchanted Wool");
        DISPLAY_NAMES.put("RABBIT", "Raw Rabbit");
        DISPLAY_NAMES.put("RABBIT_FOOT", "Rabbit's Foot");
        DISPLAY_NAMES.put("RABBIT_HIDE", "Rabbit Hide");
        DISPLAY_NAMES.put("ENCHANTED_RABBIT", "Enchanted Raw Rabbit");
        DISPLAY_NAMES.put("ENCHANTED_RABBIT_FOOT", "Enchanted Rabbit Foot");
        DISPLAY_NAMES.put("ENCHANTED_RABBIT_HIDE", "Enchanted Rabbit Hide");
        DISPLAY_NAMES.put("NETHER_STALK", "Nether Wart");
        DISPLAY_NAMES.put("ENCHANTED_NETHER_STALK", "Enchanted Nether Wart");
        DISPLAY_NAMES.put("MUTANT_NETHER_STALK", "Mutant Nether Wart");
        
        // Mining
        DISPLAY_NAMES.put("COBBLESTONE", "Cobblestone");
        DISPLAY_NAMES.put("ENCHANTED_COBBLESTONE", "Enchanted Cobblestone");
        DISPLAY_NAMES.put("COAL", "Coal");
        DISPLAY_NAMES.put("ENCHANTED_COAL", "Enchanted Coal");
        DISPLAY_NAMES.put("ENCHANTED_CHARCOAL", "Enchanted Charcoal");
        DISPLAY_NAMES.put("ENCHANTED_COAL_BLOCK", "Enchanted Coal Block");
        DISPLAY_NAMES.put("IRON_INGOT", "Iron Ingot");
        DISPLAY_NAMES.put("ENCHANTED_IRON", "Enchanted Iron");
        DISPLAY_NAMES.put("ENCHANTED_IRON_BLOCK", "Enchanted Iron Block");
        DISPLAY_NAMES.put("GOLD_INGOT", "Gold Ingot");
        DISPLAY_NAMES.put("ENCHANTED_GOLD", "Enchanted Gold");
        DISPLAY_NAMES.put("ENCHANTED_GOLD_BLOCK", "Enchanted Gold Block");
        DISPLAY_NAMES.put("DIAMOND", "Diamond");
        DISPLAY_NAMES.put("ENCHANTED_DIAMOND", "Enchanted Diamond");
        DISPLAY_NAMES.put("ENCHANTED_DIAMOND_BLOCK", "Enchanted Diamond Block");
        DISPLAY_NAMES.put("REFINED_DIAMOND", "Refined Diamond");
        DISPLAY_NAMES.put("INK_SACK:4", "Lapis Lazuli");
        DISPLAY_NAMES.put("ENCHANTED_LAPIS_LAZULI", "Enchanted Lapis Lazuli");
        DISPLAY_NAMES.put("ENCHANTED_LAPIS_LAZULI_BLOCK", "Enchanted Lapis Block");
        DISPLAY_NAMES.put("EMERALD", "Emerald");
        DISPLAY_NAMES.put("ENCHANTED_EMERALD", "Enchanted Emerald");
        DISPLAY_NAMES.put("ENCHANTED_EMERALD_BLOCK", "Enchanted Emerald Block");
        DISPLAY_NAMES.put("REDSTONE", "Redstone");
        DISPLAY_NAMES.put("ENCHANTED_REDSTONE", "Enchanted Redstone");
        DISPLAY_NAMES.put("ENCHANTED_REDSTONE_BLOCK", "Enchanted Redstone Block");
        DISPLAY_NAMES.put("ENCHANTED_REDSTONE_LAMP", "Enchanted Redstone Lamp");
        DISPLAY_NAMES.put("QUARTZ", "Nether Quartz");
        DISPLAY_NAMES.put("ENCHANTED_QUARTZ", "Enchanted Quartz");
        DISPLAY_NAMES.put("ENCHANTED_QUARTZ_BLOCK", "Enchanted Quartz Block");
        DISPLAY_NAMES.put("OBSIDIAN", "Obsidian");
        DISPLAY_NAMES.put("ENCHANTED_OBSIDIAN", "Enchanted Obsidian");
        DISPLAY_NAMES.put("GLOWSTONE_DUST", "Glowstone Dust");
        DISPLAY_NAMES.put("ENCHANTED_GLOWSTONE_DUST", "Enchanted Glowstone Dust");
        DISPLAY_NAMES.put("ENCHANTED_GLOWSTONE", "Enchanted Glowstone");
        DISPLAY_NAMES.put("FLINT", "Flint");
        DISPLAY_NAMES.put("ENCHANTED_FLINT", "Enchanted Flint");
        DISPLAY_NAMES.put("GRAVEL", "Gravel");
        DISPLAY_NAMES.put("ICE", "Ice");
        DISPLAY_NAMES.put("PACKED_ICE", "Packed Ice");
        DISPLAY_NAMES.put("ENCHANTED_ICE", "Enchanted Ice");
        DISPLAY_NAMES.put("ENCHANTED_PACKED_ICE", "Enchanted Packed Ice");
        DISPLAY_NAMES.put("NETHERRACK", "Netherrack");
        DISPLAY_NAMES.put("ENCHANTED_NETHERRACK", "Enchanted Netherrack");
        DISPLAY_NAMES.put("SAND", "Sand");
        DISPLAY_NAMES.put("ENCHANTED_SAND", "Enchanted Sand");
        DISPLAY_NAMES.put("RED_SAND", "Red Sand");
        DISPLAY_NAMES.put("ENCHANTED_RED_SAND", "Enchanted Red Sand");
        DISPLAY_NAMES.put("ENDER_STONE", "End Stone");
        DISPLAY_NAMES.put("ENCHANTED_ENDSTONE", "Enchanted End Stone");
        DISPLAY_NAMES.put("MITHRIL_ORE", "Mithril");
        DISPLAY_NAMES.put("ENCHANTED_MITHRIL", "Enchanted Mithril");
        DISPLAY_NAMES.put("REFINED_MITHRIL", "Refined Mithril");
        DISPLAY_NAMES.put("TITANIUM_ORE", "Titanium");
        DISPLAY_NAMES.put("ENCHANTED_TITANIUM", "Enchanted Titanium");
        DISPLAY_NAMES.put("REFINED_TITANIUM", "Refined Titanium");
        DISPLAY_NAMES.put("HARD_STONE", "Hard Stone");
        DISPLAY_NAMES.put("ENCHANTED_HARD_STONE", "Enchanted Hard Stone");
        DISPLAY_NAMES.put("CONCENTRATED_STONE", "Concentrated Stone");
        DISPLAY_NAMES.put("SULPHUR_ORE", "Sulphur");
        DISPLAY_NAMES.put("ENCHANTED_SULPHUR", "Enchanted Sulphur");
        DISPLAY_NAMES.put("MYCEL", "Mycelium");
        DISPLAY_NAMES.put("ENCHANTED_MYCELIUM", "Enchanted Mycelium");
        DISPLAY_NAMES.put("ENCHANTED_MYCELIUM_CUBE", "Enchanted Mycelium Cube");
        
        // Gemstones
        DISPLAY_NAMES.put("ROUGH_RUBY_GEM", "Rough Ruby");
        DISPLAY_NAMES.put("FLAWED_RUBY_GEM", "Flawed Ruby");
        DISPLAY_NAMES.put("FINE_RUBY_GEM", "Fine Ruby");
        DISPLAY_NAMES.put("FLAWLESS_RUBY_GEM", "Flawless Ruby");
        DISPLAY_NAMES.put("PERFECT_RUBY_GEM", "Perfect Ruby");
        DISPLAY_NAMES.put("ROUGH_AMBER_GEM", "Rough Amber");
        DISPLAY_NAMES.put("FLAWED_AMBER_GEM", "Flawed Amber");
        DISPLAY_NAMES.put("FINE_AMBER_GEM", "Fine Amber");
        DISPLAY_NAMES.put("FLAWLESS_AMBER_GEM", "Flawless Amber");
        DISPLAY_NAMES.put("PERFECT_AMBER_GEM", "Perfect Amber");
        DISPLAY_NAMES.put("ROUGH_SAPPHIRE_GEM", "Rough Sapphire");
        DISPLAY_NAMES.put("FLAWED_SAPPHIRE_GEM", "Flawed Sapphire");
        DISPLAY_NAMES.put("FINE_SAPPHIRE_GEM", "Fine Sapphire");
        DISPLAY_NAMES.put("FLAWLESS_SAPPHIRE_GEM", "Flawless Sapphire");
        DISPLAY_NAMES.put("PERFECT_SAPPHIRE_GEM", "Perfect Sapphire");
        DISPLAY_NAMES.put("ROUGH_JADE_GEM", "Rough Jade");
        DISPLAY_NAMES.put("FLAWED_JADE_GEM", "Flawed Jade");
        DISPLAY_NAMES.put("FINE_JADE_GEM", "Fine Jade");
        DISPLAY_NAMES.put("FLAWLESS_JADE_GEM", "Flawless Jade");
        DISPLAY_NAMES.put("PERFECT_JADE_GEM", "Perfect Jade");
        DISPLAY_NAMES.put("ROUGH_AMETHYST_GEM", "Rough Amethyst");
        DISPLAY_NAMES.put("FLAWED_AMETHYST_GEM", "Flawed Amethyst");
        DISPLAY_NAMES.put("FINE_AMETHYST_GEM", "Fine Amethyst");
        DISPLAY_NAMES.put("FLAWLESS_AMETHYST_GEM", "Flawless Amethyst");
        DISPLAY_NAMES.put("PERFECT_AMETHYST_GEM", "Perfect Amethyst");
        DISPLAY_NAMES.put("ROUGH_TOPAZ_GEM", "Rough Topaz");
        DISPLAY_NAMES.put("FLAWED_TOPAZ_GEM", "Flawed Topaz");
        DISPLAY_NAMES.put("FINE_TOPAZ_GEM", "Fine Topaz");
        DISPLAY_NAMES.put("FLAWLESS_TOPAZ_GEM", "Flawless Topaz");
        DISPLAY_NAMES.put("PERFECT_TOPAZ_GEM", "Perfect Topaz");
        DISPLAY_NAMES.put("ROUGH_JASPER_GEM", "Rough Jasper");
        DISPLAY_NAMES.put("FLAWED_JASPER_GEM", "Flawed Jasper");
        DISPLAY_NAMES.put("FINE_JASPER_GEM", "Fine Jasper");
        DISPLAY_NAMES.put("FLAWLESS_JASPER_GEM", "Flawless Jasper");
        DISPLAY_NAMES.put("PERFECT_JASPER_GEM", "Perfect Jasper");
        DISPLAY_NAMES.put("ROUGH_OPAL_GEM", "Rough Opal");
        DISPLAY_NAMES.put("FLAWED_OPAL_GEM", "Flawed Opal");
        DISPLAY_NAMES.put("FINE_OPAL_GEM", "Fine Opal");
        DISPLAY_NAMES.put("FLAWLESS_OPAL_GEM", "Flawless Opal");
        DISPLAY_NAMES.put("PERFECT_OPAL_GEM", "Perfect Opal");
        DISPLAY_NAMES.put("ROUGH_ONYX_GEM", "Rough Onyx");
        DISPLAY_NAMES.put("FLAWED_ONYX_GEM", "Flawed Onyx");
        DISPLAY_NAMES.put("FINE_ONYX_GEM", "Fine Onyx");
        DISPLAY_NAMES.put("FLAWLESS_ONYX_GEM", "Flawless Onyx");
        DISPLAY_NAMES.put("PERFECT_ONYX_GEM", "Perfect Onyx");
        DISPLAY_NAMES.put("ROUGH_AQUAMARINE_GEM", "Rough Aquamarine");
        DISPLAY_NAMES.put("FLAWED_AQUAMARINE_GEM", "Flawed Aquamarine");
        DISPLAY_NAMES.put("FINE_AQUAMARINE_GEM", "Fine Aquamarine");
        DISPLAY_NAMES.put("FLAWLESS_AQUAMARINE_GEM", "Flawless Aquamarine");
        DISPLAY_NAMES.put("PERFECT_AQUAMARINE_GEM", "Perfect Aquamarine");
        DISPLAY_NAMES.put("ROUGH_CITRINE_GEM", "Rough Citrine");
        DISPLAY_NAMES.put("FLAWED_CITRINE_GEM", "Flawed Citrine");
        DISPLAY_NAMES.put("FINE_CITRINE_GEM", "Fine Citrine");
        DISPLAY_NAMES.put("FLAWLESS_CITRINE_GEM", "Flawless Citrine");
        DISPLAY_NAMES.put("PERFECT_CITRINE_GEM", "Perfect Citrine");
        DISPLAY_NAMES.put("ROUGH_PERIDOT_GEM", "Rough Peridot");
        DISPLAY_NAMES.put("FLAWED_PERIDOT_GEM", "Flawed Peridot");
        DISPLAY_NAMES.put("FINE_PERIDOT_GEM", "Fine Peridot");
        DISPLAY_NAMES.put("FLAWLESS_PERIDOT_GEM", "Flawless Peridot");
        DISPLAY_NAMES.put("PERFECT_PERIDOT_GEM", "Perfect Peridot");
        
        // Combat
        DISPLAY_NAMES.put("ROTTEN_FLESH", "Rotten Flesh");
        DISPLAY_NAMES.put("ENCHANTED_ROTTEN_FLESH", "Enchanted Rotten Flesh");
        DISPLAY_NAMES.put("REVENANT_FLESH", "Revenant Flesh");
        DISPLAY_NAMES.put("REVENANT_VISCERA", "Revenant Viscera");
        DISPLAY_NAMES.put("FOUL_FLESH", "Foul Flesh");
        DISPLAY_NAMES.put("BONE", "Bone");
        DISPLAY_NAMES.put("ENCHANTED_BONE", "Enchanted Bone");
        DISPLAY_NAMES.put("ENCHANTED_BONE_BLOCK", "Enchanted Bone Block");
        DISPLAY_NAMES.put("ENCHANTED_BONE_MEAL", "Enchanted Bone Meal");
        DISPLAY_NAMES.put("STRING", "String");
        DISPLAY_NAMES.put("ENCHANTED_STRING", "Enchanted String");
        DISPLAY_NAMES.put("TARANTULA_WEB", "Tarantula Web");
        DISPLAY_NAMES.put("TARANTULA_SILK", "Tarantula Silk");
        DISPLAY_NAMES.put("SPIDER_EYE", "Spider Eye");
        DISPLAY_NAMES.put("ENCHANTED_SPIDER_EYE", "Enchanted Spider Eye");
        DISPLAY_NAMES.put("ENCHANTED_FERMENTED_SPIDER_EYE", "Enchanted Fermented Spider Eye");
        DISPLAY_NAMES.put("SULPHUR", "Gunpowder");
        DISPLAY_NAMES.put("ENCHANTED_GUNPOWDER", "Enchanted Gunpowder");
        DISPLAY_NAMES.put("ENCHANTED_FIREWORK_ROCKET", "Enchanted Firework Rocket");
        DISPLAY_NAMES.put("ENDER_PEARL", "Ender Pearl");
        DISPLAY_NAMES.put("ENCHANTED_ENDER_PEARL", "Enchanted Ender Pearl");
        DISPLAY_NAMES.put("ENCHANTED_EYE_OF_ENDER", "Enchanted Eye of Ender");
        DISPLAY_NAMES.put("ABSOLUTE_ENDER_PEARL", "Absolute Ender Pearl");
        DISPLAY_NAMES.put("GHAST_TEAR", "Ghast Tear");
        DISPLAY_NAMES.put("ENCHANTED_GHAST_TEAR", "Enchanted Ghast Tear");
        DISPLAY_NAMES.put("SLIME_BALL", "Slimeball");
        DISPLAY_NAMES.put("ENCHANTED_SLIME_BALL", "Enchanted Slimeball");
        DISPLAY_NAMES.put("ENCHANTED_SLIME_BLOCK", "Enchanted Slime Block");
        DISPLAY_NAMES.put("SLUDGE_JUICE", "Sludge Juice");
        DISPLAY_NAMES.put("YOGGIE", "Yoggie");
        DISPLAY_NAMES.put("BLAZE_ROD", "Blaze Rod");
        DISPLAY_NAMES.put("ENCHANTED_BLAZE_POWDER", "Enchanted Blaze Powder");
        DISPLAY_NAMES.put("ENCHANTED_BLAZE_ROD", "Enchanted Blaze Rod");
        DISPLAY_NAMES.put("MAGMA_CREAM", "Magma Cream");
        DISPLAY_NAMES.put("ENCHANTED_MAGMA_CREAM", "Enchanted Magma Cream");
        DISPLAY_NAMES.put("WHIPPED_MAGMA_CREAM", "Whipped Magma Cream");
        DISPLAY_NAMES.put("WOLF_TOOTH", "Wolf Tooth");
        DISPLAY_NAMES.put("ENCHANTED_WOLF_TOOTH", "Enchanted Wolf Tooth");
        DISPLAY_NAMES.put("GOLDEN_TOOTH", "Golden Tooth");
        DISPLAY_NAMES.put("RAW_SOULFLOW", "Raw Soulflow");
        DISPLAY_NAMES.put("SOULFLOW", "Soulflow");
        DISPLAY_NAMES.put("NULL_SPHERE", "Null Sphere");
        DISPLAY_NAMES.put("NULL_OVOID", "Null Ovoid");
        DISPLAY_NAMES.put("NULL_ATOM", "Null Atom");
        
        // Woods & Fishes
        DISPLAY_NAMES.put("LOG", "Oak Log");
        DISPLAY_NAMES.put("ENCHANTED_OAK_LOG", "Enchanted Oak Log");
        DISPLAY_NAMES.put("LOG:1", "Spruce Log");
        DISPLAY_NAMES.put("ENCHANTED_SPRUCE_LOG", "Enchanted Spruce Log");
        DISPLAY_NAMES.put("LOG:2", "Birch Log");
        DISPLAY_NAMES.put("ENCHANTED_BIRCH_LOG", "Enchanted Birch Log");
        DISPLAY_NAMES.put("LOG_2:1", "Dark Oak Log");
        DISPLAY_NAMES.put("ENCHANTED_DARK_OAK_LOG", "Enchanted Dark Oak Log");
        DISPLAY_NAMES.put("LOG_2", "Acacia Log");
        DISPLAY_NAMES.put("ENCHANTED_ACACIA_LOG", "Enchanted Acacia Log");
        DISPLAY_NAMES.put("LOG:3", "Jungle Log");
        DISPLAY_NAMES.put("ENCHANTED_JUNGLE_LOG", "Enchanted Jungle Log");
        DISPLAY_NAMES.put("RAW_FISH", "Raw Fish");
        DISPLAY_NAMES.put("ENCHANTED_RAW_FISH", "Enchanted Raw Fish");
        DISPLAY_NAMES.put("ENCHANTED_COOKED_FISH", "Enchanted Cooked Fish");
        DISPLAY_NAMES.put("RAW_FISH:1", "Raw Salmon");
        DISPLAY_NAMES.put("ENCHANTED_RAW_SALMON", "Enchanted Raw Salmon");
        DISPLAY_NAMES.put("ENCHANTED_COOKED_SALMON", "Enchanted Cooked Salmon");
        DISPLAY_NAMES.put("RAW_FISH:2", "Clownfish");
        DISPLAY_NAMES.put("ENCHANTED_CLOWNFISH", "Enchanted Clownfish");
        DISPLAY_NAMES.put("RAW_FISH:3", "Pufferfish");
        DISPLAY_NAMES.put("ENCHANTED_PUFFERFISH", "Enchanted Pufferfish");
        DISPLAY_NAMES.put("PRISMARINE_SHARD", "Prismarine Shard");
        DISPLAY_NAMES.put("ENCHANTED_PRISMARINE_SHARD", "Enchanted Prismarine Shard");
        DISPLAY_NAMES.put("PRISMARINE_CRYSTALS", "Prismarine Crystals");
        DISPLAY_NAMES.put("ENCHANTED_PRISMARINE_CRYSTALS", "Enchanted Prismarine Crystals");
        DISPLAY_NAMES.put("CLAY_BALL", "Clay");
        DISPLAY_NAMES.put("ENCHANTED_CLAY_BALL", "Enchanted Clay");
        DISPLAY_NAMES.put("WATER_LILY", "Lily Pad");
        DISPLAY_NAMES.put("ENCHANTED_WATER_LILY", "Enchanted Lily Pad");
        DISPLAY_NAMES.put("INK_SACK", "Ink Sac");
        DISPLAY_NAMES.put("ENCHANTED_INK_SACK", "Enchanted Ink Sac");
        DISPLAY_NAMES.put("SPONGE", "Sponge");
        DISPLAY_NAMES.put("ENCHANTED_SPONGE", "Enchanted Sponge");
        DISPLAY_NAMES.put("ENCHANTED_WET_SPONGE", "Enchanted Wet Sponge");
        DISPLAY_NAMES.put("SHARK_FIN", "Shark Fin");
        DISPLAY_NAMES.put("ENCHANTED_SHARK_FIN", "Enchanted Shark Fin");
        DISPLAY_NAMES.put("NURSE_SHARK_TOOTH", "Nurse Shark Tooth");
        DISPLAY_NAMES.put("BLUE_SHARK_TOOTH", "Blue Shark Tooth");
        DISPLAY_NAMES.put("TIGER_SHARK_TOOTH", "Tiger Shark Tooth");
        DISPLAY_NAMES.put("GREAT_WHITE_SHARK_TOOTH", "Great White Shark Tooth");
        
        // Oddities - Essence
        DISPLAY_NAMES.put("WITHER_ESSENCE", "Wither Essence");
        DISPLAY_NAMES.put("SPIDER_ESSENCE", "Spider Essence");
        DISPLAY_NAMES.put("UNDEAD_ESSENCE", "Undead Essence");
        DISPLAY_NAMES.put("DRAGON_ESSENCE", "Dragon Essence");
        DISPLAY_NAMES.put("GOLD_ESSENCE", "Gold Essence");
        DISPLAY_NAMES.put("DIAMOND_ESSENCE", "Diamond Essence");
        DISPLAY_NAMES.put("ICE_ESSENCE", "Ice Essence");
        DISPLAY_NAMES.put("CRIMSON_ESSENCE", "Crimson Essence");
        
        // Oddities - Dragon Fragments
        DISPLAY_NAMES.put("SUPERIOR_FRAGMENT", "Superior Dragon Fragment");
        DISPLAY_NAMES.put("STRONG_FRAGMENT", "Strong Dragon Fragment");
        DISPLAY_NAMES.put("UNSTABLE_FRAGMENT", "Unstable Dragon Fragment");
        DISPLAY_NAMES.put("YOUNG_FRAGMENT", "Young Dragon Fragment");
        DISPLAY_NAMES.put("OLD_FRAGMENT", "Old Dragon Fragment");
        DISPLAY_NAMES.put("WISE_FRAGMENT", "Wise Dragon Fragment");
        DISPLAY_NAMES.put("PROTECTOR_FRAGMENT", "Protector Dragon Fragment");
        DISPLAY_NAMES.put("HOLY_FRAGMENT", "Holy Dragon Fragment");
        
        // Oddities - General
        DISPLAY_NAMES.put("BOOSTER_COOKIE", "Booster Cookie");
        DISPLAY_NAMES.put("EXP_BOTTLE", "Experience Bottle");
        DISPLAY_NAMES.put("GRAND_EXP_BOTTLE", "Grand Experience Bottle");
        DISPLAY_NAMES.put("TITANIC_EXP_BOTTLE", "Titanic Experience Bottle");
        DISPLAY_NAMES.put("COLOSSAL_EXP_BOTTLE", "Colossal Experience Bottle");
        DISPLAY_NAMES.put("CATALYST", "Catalyst");
        DISPLAY_NAMES.put("HYPER_CATALYST", "Hyper Catalyst");
        DISPLAY_NAMES.put("RECOMBOBULATOR_3000", "Recombobulator 3000");
        DISPLAY_NAMES.put("FUMING_POTATO_BOOK", "Fuming Potato Book");
        DISPLAY_NAMES.put("HOT_POTATO_BOOK", "Hot Potato Book");
        DISPLAY_NAMES.put("JACOBS_TICKET", "Jacob's Ticket");
        DISPLAY_NAMES.put("FIRST_MASTER_STAR", "First Master Star");
        DISPLAY_NAMES.put("SECOND_MASTER_STAR", "Second Master Star");
        DISPLAY_NAMES.put("THIRD_MASTER_STAR", "Third Master Star");
        DISPLAY_NAMES.put("FOURTH_MASTER_STAR", "Fourth Master Star");
        DISPLAY_NAMES.put("FIFTH_MASTER_STAR", "Fifth Master Star");
        DISPLAY_NAMES.put("LUCKY_CLOVER", "Lucky Clover");
        DISPLAY_NAMES.put("EXP_SHARE_CORE", "Exp Share Core");
        DISPLAY_NAMES.put("TIER_BOOST_CORE", "Tier Boost Core");
        DISPLAY_NAMES.put("SIMPLE_CARROT_CANDY", "Simple Carrot Candy");
        DISPLAY_NAMES.put("GREAT_CARROT_CANDY", "Great Carrot Candy");
        DISPLAY_NAMES.put("SUPERB_CARROT_CANDY", "Superb Carrot Candy");
        DISPLAY_NAMES.put("ULTIMATE_CARROT_CANDY", "Ultimate Carrot Candy");
        
        // Garden
        DISPLAY_NAMES.put("COMPOST", "Compost");
        DISPLAY_NAMES.put("COMPOST_BUNDLE", "Compost Bundle");
        DISPLAY_NAMES.put("CROPIE", "Cropie");
        DISPLAY_NAMES.put("SQUASH", "Squash");
        DISPLAY_NAMES.put("FERMENTO", "Fermento");
        DISPLAY_NAMES.put("CONDENSED_FERMENTO", "Condensed Fermento");
        DISPLAY_NAMES.put("DUNG", "Dung");
        DISPLAY_NAMES.put("TASTY_CHEESE", "Tasty Cheese");
        DISPLAY_NAMES.put("PLANT_MATTER", "Plant Matter");
        DISPLAY_NAMES.put("HONEY_JAR", "Honey Jar");
        DISPLAY_NAMES.put("WRIGGLING_LARVA", "Wriggling Larva");
        DISPLAY_NAMES.put("FINE_FLOUR", "Fine Flour");
    }
    
    // Get display name with override support
    private static String getDisplayName(String productId) {
        if (DISPLAY_NAMES.containsKey(productId)) {
            return DISPLAY_NAMES.get(productId);
        }
        // Fallback: Convert product ID to readable name (title case)
        String[] words = productId.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1));
            }
        }
        return result.toString();
    }
    
    // Category data class
    private static class BazaarCategory {
        String id;
        String name;
        String icon;
        int color;
        int sectionCount;
    }
    
    // Section data class
    private static class BazaarSection {
        String id;
        String name;
        int itemCount;
    }
    
    // Item data class
    private static class BazaarItem {
        String productId;
        String displayName;
        double buyPrice;
        double sellPrice;
        long buyVolume;
        long sellVolume;
        String category;
        String section;
        boolean unavailable;
    }
    
    // Item detail with price history
    private static class BazaarItemDetail {
        String productId;
        String displayName;
        double buyPrice;
        double sellPrice;
        long buyVolume;
        long sellVolume;
        long buyOrders;
        long sellOrders;
        // Price history
        Double buyPrice1h;
        Double sellPrice1h;
        Double buyPrice1d;
        Double sellPrice1d;
        Double buyPrice7d;
        Double sellPrice7d;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        
        // Calculate panel dimensions
        panelWidth = Math.min(700, width - 40);
        panelHeight = Math.min(400, height - 60);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        
        // Search field
        searchField = new GuiTextField(0, fontRendererObj, panelX + categoryWidth + 10, panelY - 25, 200, 18);
        searchField.setMaxStringLength(50);
        searchField.setFocused(false);
        searchField.setText("");
        
        // Buttons
        buttonList.clear();
        buttonList.add(new GuiButton(1, panelX + panelWidth - 60, panelY - 25, 50, 18, "Search"));
        buttonList.add(new GuiButton(2, panelX + panelWidth - 20, panelY + 5, 15, 15, "X"));
        
        // Load categories
        loadCategories();
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Background
        drawDefaultBackground();
        
        // Main panel background
        drawRect(panelX - 5, panelY - 30, panelX + panelWidth + 5, panelY + panelHeight + 5, 0xCC000000);
        drawRect(panelX - 3, panelY - 28, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF1A1A2E);
        
        // Title
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "BAZAAR", width / 2, panelY - 45, 0xFFFFFF);
        
        // Draw search field
        searchField.drawTextBox();
        
        // Category panel
        drawCategoryPanel(mouseX, mouseY);
        
        // Section panel (if category selected)
        if (selectedCategory != null && !showSearch) {
            drawSectionPanel(mouseX, mouseY);
        }
        
        // Items panel
        if (showSearch) {
            drawSearchResults(mouseX, mouseY);
        } else if (selectedSection != null) {
            drawItemsPanel(mouseX, mouseY);
        }
        
        // Status message
        if (isLoading) {
            drawCenteredString(fontRendererObj, EnumChatFormatting.YELLOW + statusMessage, width / 2, panelY + panelHeight + 10, 0xFFFFFF);
        }
        
        // Draw tooltip for hovered item
        if (hoveredItem != null) {
            drawItemTooltip(mouseX, mouseY);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawCategoryPanel(int mouseX, int mouseY) {
        int x = panelX;
        int y = panelY;
        int itemHeight = 24;
        
        // Panel background
        drawRect(x, y, x + categoryWidth, y + panelHeight, 0xFF2D2D44);
        
        // Header
        drawRect(x, y, x + categoryWidth, y + 20, 0xFF3D3D5C);
        drawCenteredString(fontRendererObj, "Categories", x + categoryWidth / 2, y + 6, 0xFFFFFF);
        
        y += 25;
        
        for (BazaarCategory cat : categories) {
            boolean hovered = mouseX >= x && mouseX < x + categoryWidth && mouseY >= y && mouseY < y + itemHeight;
            boolean selected = cat.id.equals(selectedCategory);
            
            // Background
            if (selected) {
                drawRect(x + 2, y, x + categoryWidth - 2, y + itemHeight - 2, 0xFF4A4A6A);
            } else if (hovered) {
                drawRect(x + 2, y, x + categoryWidth - 2, y + itemHeight - 2, 0xFF3A3A5A);
            }
            
            // Category name with color
            int textColor = cat.color != 0 ? cat.color : 0xFFFFFF;
            fontRendererObj.drawString(cat.name, x + 8, y + 7, textColor);
            
            // Section count
            String count = "(" + cat.sectionCount + ")";
            fontRendererObj.drawString(count, x + categoryWidth - fontRendererObj.getStringWidth(count) - 8, y + 7, 0x888888);
            
            y += itemHeight;
        }
    }
    
    private void drawSectionPanel(int mouseX, int mouseY) {
        int x = panelX + categoryWidth + 5;
        int y = panelY;
        int itemHeight = 22;
        
        // Panel background
        drawRect(x, y, x + sectionWidth, y + panelHeight, 0xFF2D2D44);
        
        // Header
        drawRect(x, y, x + sectionWidth, y + 20, 0xFF3D3D5C);
        drawCenteredString(fontRendererObj, "Sections", x + sectionWidth / 2, y + 6, 0xFFFFFF);
        
        y += 25;
        
        int visibleSections = (panelHeight - 30) / itemHeight;
        maxSectionScroll = Math.max(0, sections.size() - visibleSections);
        
        for (int i = sectionScrollOffset; i < sections.size() && y < panelY + panelHeight - itemHeight; i++) {
            BazaarSection section = sections.get(i);
            boolean hovered = mouseX >= x && mouseX < x + sectionWidth && mouseY >= y && mouseY < y + itemHeight;
            boolean selected = section.id.equals(selectedSection);
            
            // Background
            if (selected) {
                drawRect(x + 2, y, x + sectionWidth - 2, y + itemHeight - 2, 0xFF4A4A6A);
            } else if (hovered) {
                drawRect(x + 2, y, x + sectionWidth - 2, y + itemHeight - 2, 0xFF3A3A5A);
            }
            
            // Section name
            String name = section.name;
            if (fontRendererObj.getStringWidth(name) > sectionWidth - 30) {
                name = fontRendererObj.trimStringToWidth(name, sectionWidth - 35) + "...";
            }
            fontRendererObj.drawString(name, x + 5, y + 6, 0xFFFFFF);
            
            y += itemHeight;
        }
        
        // Scroll indicator for sections
        if (maxSectionScroll > 0) {
            int scrollBarHeight = panelHeight - 30;
            int scrollThumbHeight = Math.max(20, scrollBarHeight * visibleSections / sections.size());
            int scrollThumbY = panelY + 25 + (int)((scrollBarHeight - scrollThumbHeight) * sectionScrollOffset / (float)maxSectionScroll);
            
            drawRect(x + sectionWidth - 5, panelY + 25, x + sectionWidth - 2, panelY + panelHeight, 0xFF1A1A2E);
            drawRect(x + sectionWidth - 5, scrollThumbY, x + sectionWidth - 2, scrollThumbY + scrollThumbHeight, 0xFF5A5A7A);
        }
    }
    
    private void drawItemsPanel(int mouseX, int mouseY) {
        int x = panelX + categoryWidth + sectionWidth + 10;
        int y = panelY;
        int itemsWidth = panelWidth - categoryWidth - sectionWidth - 15;
        int itemHeight = 40;
        
        // Panel background
        drawRect(x, y, x + itemsWidth, y + panelHeight, 0xFF2D2D44);
        
        // Header
        drawRect(x, y, x + itemsWidth, y + 20, 0xFF3D3D5C);
        drawString(fontRendererObj, "Item", x + 5, y + 6, 0xFFFFFF);
        drawString(fontRendererObj, "Buy", x + itemsWidth - 140, y + 6, 0x55FF55);
        drawString(fontRendererObj, "Sell", x + itemsWidth - 70, y + 6, 0xFF5555);
        
        y += 25;
        hoveredItem = null;
        
        int visibleItems = (panelHeight - 30) / itemHeight;
        maxScroll = Math.max(0, items.size() - visibleItems);
        
        for (int i = scrollOffset; i < items.size() && y < panelY + panelHeight - itemHeight; i++) {
            BazaarItem item = items.get(i);
            boolean hovered = mouseX >= x && mouseX < x + itemsWidth && mouseY >= y && mouseY < y + itemHeight;
            
            if (hovered) {
                drawRect(x + 2, y, x + itemsWidth - 2, y + itemHeight - 2, 0xFF3A3A5A);
                hoveredItem = item;
                
                // Load detailed info for tooltip
                if (hoveredItemDetail == null || !hoveredItemDetail.productId.equals(item.productId)) {
                    loadItemDetail(item.productId);
                }
            }
            
            // Item name
            String name = item.displayName;
            if (fontRendererObj.getStringWidth(name) > itemsWidth - 160) {
                name = fontRendererObj.trimStringToWidth(name, itemsWidth - 165) + "...";
            }
            
            int nameColor = item.unavailable ? 0x888888 : 0xFFFFFF;
            fontRendererObj.drawString(name, x + 5, y + 5, nameColor);
            
            // Buy price
            String buyStr = item.unavailable ? "N/A" : formatPrice(item.buyPrice);
            fontRendererObj.drawString(buyStr, x + itemsWidth - 140, y + 5, 0x55FF55);
            
            // Sell price
            String sellStr = item.unavailable ? "N/A" : formatPrice(item.sellPrice);
            fontRendererObj.drawString(sellStr, x + itemsWidth - 70, y + 5, 0xFF5555);
            
            // Volume info
            if (!item.unavailable) {
                String volumeInfo = EnumChatFormatting.GRAY + "Vol: " + VOLUME_FORMAT.format(item.buyVolume) + " / " + VOLUME_FORMAT.format(item.sellVolume);
                fontRendererObj.drawString(volumeInfo, x + 5, y + 17, 0x888888);
            }
            
            // Separator line
            drawRect(x + 5, y + itemHeight - 3, x + itemsWidth - 5, y + itemHeight - 2, 0xFF3D3D5C);
            
            y += itemHeight;
        }
        
        // Scroll indicator
        if (maxScroll > 0) {
            int scrollBarHeight = panelHeight - 30;
            int scrollThumbHeight = Math.max(20, scrollBarHeight * visibleItems / items.size());
            int scrollThumbY = panelY + 25 + (int)((scrollBarHeight - scrollThumbHeight) * scrollOffset / (float)maxScroll);
            
            drawRect(x + itemsWidth - 5, panelY + 25, x + itemsWidth - 2, panelY + panelHeight, 0xFF1A1A2E);
            drawRect(x + itemsWidth - 5, scrollThumbY, x + itemsWidth - 2, scrollThumbY + scrollThumbHeight, 0xFF5A5A7A);
        }
    }
    
    private void drawSearchResults(int mouseX, int mouseY) {
        int x = panelX + categoryWidth + 10;
        int y = panelY;
        int resultsWidth = panelWidth - categoryWidth - 15;
        int itemHeight = 35;
        
        // Panel background
        drawRect(x, y, x + resultsWidth, y + panelHeight, 0xFF2D2D44);
        
        // Header
        drawRect(x, y, x + resultsWidth, y + 20, 0xFF3D3D5C);
        drawString(fontRendererObj, "Search Results (" + searchResults.size() + ")", x + 5, y + 6, 0xFFFFFF);
        
        y += 25;
        hoveredItem = null;
        
        if (searchResults.isEmpty()) {
            drawCenteredString(fontRendererObj, EnumChatFormatting.GRAY + "No results found", x + resultsWidth / 2, y + 50, 0xFFFFFF);
            return;
        }
        
        int visibleItems = (panelHeight - 30) / itemHeight;
        maxScroll = Math.max(0, searchResults.size() - visibleItems);
        
        for (int i = scrollOffset; i < searchResults.size() && y < panelY + panelHeight - itemHeight; i++) {
            BazaarItem item = searchResults.get(i);
            boolean hovered = mouseX >= x && mouseX < x + resultsWidth && mouseY >= y && mouseY < y + itemHeight;
            
            if (hovered) {
                drawRect(x + 2, y, x + resultsWidth - 2, y + itemHeight - 2, 0xFF3A3A5A);
                hoveredItem = item;
                
                if (hoveredItemDetail == null || !hoveredItemDetail.productId.equals(item.productId)) {
                    loadItemDetail(item.productId);
                }
            }
            
            // Item name
            String name = item.displayName;
            if (fontRendererObj.getStringWidth(name) > resultsWidth - 180) {
                name = fontRendererObj.trimStringToWidth(name, resultsWidth - 185) + "...";
            }
            fontRendererObj.drawString(name, x + 5, y + 3, 0xFFFFFF);
            
            // Category/Section path
            String path = EnumChatFormatting.DARK_GRAY + item.category + " > " + item.section;
            fontRendererObj.drawString(path, x + 5, y + 14, 0x666666);
            
            // Prices
            fontRendererObj.drawString(formatPrice(item.buyPrice), x + resultsWidth - 140, y + 8, 0x55FF55);
            fontRendererObj.drawString(formatPrice(item.sellPrice), x + resultsWidth - 70, y + 8, 0xFF5555);
            
            y += itemHeight;
        }
    }
    
    private void drawItemTooltip(int mouseX, int mouseY) {
        if (hoveredItem == null) return;
        
        List<String> lines = new ArrayList<>();
        
        // Title
        lines.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + hoveredItem.displayName);
        lines.add("");
        
        // Current prices
        lines.add(EnumChatFormatting.GREEN + "Buy Price: " + EnumChatFormatting.WHITE + formatPrice(hoveredItem.buyPrice));
        lines.add(EnumChatFormatting.RED + "Sell Price: " + EnumChatFormatting.WHITE + formatPrice(hoveredItem.sellPrice));
        lines.add("");
        
        // Volume
        lines.add(EnumChatFormatting.GRAY + "Buy Volume: " + EnumChatFormatting.WHITE + VOLUME_FORMAT.format(hoveredItem.buyVolume));
        lines.add(EnumChatFormatting.GRAY + "Sell Volume: " + EnumChatFormatting.WHITE + VOLUME_FORMAT.format(hoveredItem.sellVolume));
        
        // Price history if available
        if (hoveredItemDetail != null && hoveredItemDetail.productId.equals(hoveredItem.productId)) {
            lines.add("");
            lines.add(EnumChatFormatting.YELLOW + "Price History:");
            
            // 1 Hour
            if (hoveredItemDetail.buyPrice1h != null) {
                double buyChange = calculatePercentChange(hoveredItemDetail.buyPrice1h, hoveredItem.buyPrice);
                double sellChange = calculatePercentChange(hoveredItemDetail.sellPrice1h, hoveredItem.sellPrice);
                lines.add(EnumChatFormatting.GRAY + "  1h: " + formatChangeString(buyChange) + EnumChatFormatting.GRAY + " / " + formatChangeString(sellChange));
            }
            
            // 1 Day
            if (hoveredItemDetail.buyPrice1d != null) {
                double buyChange = calculatePercentChange(hoveredItemDetail.buyPrice1d, hoveredItem.buyPrice);
                double sellChange = calculatePercentChange(hoveredItemDetail.sellPrice1d, hoveredItem.sellPrice);
                lines.add(EnumChatFormatting.GRAY + "  1d: " + formatChangeString(buyChange) + EnumChatFormatting.GRAY + " / " + formatChangeString(sellChange));
            }
            
            // 7 Days
            if (hoveredItemDetail.buyPrice7d != null) {
                double buyChange = calculatePercentChange(hoveredItemDetail.buyPrice7d, hoveredItem.buyPrice);
                double sellChange = calculatePercentChange(hoveredItemDetail.sellPrice7d, hoveredItem.sellPrice);
                lines.add(EnumChatFormatting.GRAY + "  7d: " + formatChangeString(buyChange) + EnumChatFormatting.GRAY + " / " + formatChangeString(sellChange));
            }
            
            // Orders
            lines.add("");
            lines.add(EnumChatFormatting.GRAY + "Buy Orders: " + EnumChatFormatting.WHITE + VOLUME_FORMAT.format(hoveredItemDetail.buyOrders));
            lines.add(EnumChatFormatting.GRAY + "Sell Orders: " + EnumChatFormatting.WHITE + VOLUME_FORMAT.format(hoveredItemDetail.sellOrders));
        }
        
        drawHoveringText(lines, mouseX, mouseY);
    }
    
    private double calculatePercentChange(double oldPrice, double newPrice) {
        if (oldPrice == 0) return 0;
        return ((newPrice - oldPrice) / oldPrice) * 100;
    }
    
    private String formatChangeString(double percentChange) {
        DecimalFormat df = new DecimalFormat("+0.0;-0.0");
        String formatted = df.format(percentChange) + "%";
        
        if (percentChange > 0) {
            return EnumChatFormatting.GREEN + formatted;
        } else if (percentChange < 0) {
            return EnumChatFormatting.RED + formatted;
        }
        return EnumChatFormatting.GRAY + formatted;
    }
    
    private String formatPrice(double price) {
        if (price >= 1000000000) {
            return PRICE_FORMAT.format(price / 1000000000) + "B";
        } else if (price >= 1000000) {
            return PRICE_FORMAT.format(price / 1000000) + "M";
        } else if (price >= 1000) {
            return PRICE_FORMAT.format(price / 1000) + "K";
        }
        return PRICE_FORMAT.format(price);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (mouseButton == 0) {
            // Check category clicks
            int x = panelX;
            int y = panelY + 25;
            int itemHeight = 24;
            
            for (BazaarCategory cat : categories) {
                if (mouseX >= x && mouseX < x + categoryWidth && mouseY >= y && mouseY < y + itemHeight) {
                    if (!cat.id.equals(selectedCategory)) {
                        selectedCategory = cat.id;
                        selectedSection = null;
                        sections.clear();
                        items.clear();
                        scrollOffset = 0;
                        sectionScrollOffset = 0;
                        showSearch = false;
                        loadSections(cat.id);
                    }
                    return;
                }
                y += itemHeight;
            }
            
            // Check section clicks (if not in search mode)
            if (selectedCategory != null && !showSearch) {
                x = panelX + categoryWidth + 5;
                y = panelY + 25;
                itemHeight = 22;
                
                for (int i = sectionScrollOffset; i < sections.size() && y < panelY + panelHeight - itemHeight; i++) {
                    BazaarSection section = sections.get(i);
                    if (mouseX >= x && mouseX < x + sectionWidth && mouseY >= y && mouseY < y + itemHeight) {
                        if (!section.id.equals(selectedSection)) {
                            selectedSection = section.id;
                            items.clear();
                            scrollOffset = 0;
                            loadItems(selectedCategory, section.id);
                        }
                        return;
                    }
                    y += itemHeight;
                }
            }
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
            
            if (keyCode == Keyboard.KEY_RETURN) {
                performSearch();
            }
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        }
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            // Check if mouse is over sections panel
            int sectionPanelX = panelX + categoryWidth + 5;
            boolean overSections = selectedCategory != null && !showSearch && 
                mouseX >= sectionPanelX && mouseX < sectionPanelX + sectionWidth &&
                mouseY >= panelY && mouseY < panelY + panelHeight;
            
            if (overSections) {
                // Scroll sections
                if (scroll > 0) {
                    sectionScrollOffset = Math.max(0, sectionScrollOffset - 1);
                } else {
                    sectionScrollOffset = Math.min(maxSectionScroll, sectionScrollOffset + 1);
                }
            } else {
                // Scroll items/search results
                if (scroll > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                }
            }
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) { // Search button
            performSearch();
        } else if (button.id == 2) { // Close button
            mc.displayGuiScreen(null);
        }
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.length() >= 2) {
            showSearch = true;
            searchResults.clear();
            scrollOffset = 0;
            searchItems(query);
        }
    }
    
    // API Methods
    
    private void loadCategories() {
        isLoading = true;
        statusMessage = "Loading categories...";
        
        executor.submit(() -> {
            try {
                String response = httpGet(BAZAAR_API_URL + "/bazaar/categories");
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    categories.clear();
                    JsonArray catArray = json.getAsJsonArray("categories");
                    
                    for (JsonElement elem : catArray) {
                        JsonObject catObj = elem.getAsJsonObject();
                        BazaarCategory cat = new BazaarCategory();
                        cat.id = catObj.get("id").getAsString();
                        cat.name = catObj.get("name").getAsString();
                        cat.icon = catObj.get("icon").getAsString();
                        cat.color = catObj.get("color").getAsInt();
                        cat.sectionCount = catObj.get("sectionCount").getAsInt();
                        categories.add(cat);
                    }
                    
                    isLoading = false;
                }
            } catch (Exception e) {
                statusMessage = "Error loading categories: " + e.getMessage();
                System.err.println("[Bazaar] Error loading categories: " + e.getMessage());
            }
        });
    }
    
    private void loadSections(String categoryId) {
        isLoading = true;
        statusMessage = "Loading sections...";
        
        executor.submit(() -> {
            try {
                String response = httpGet(BAZAAR_API_URL + "/bazaar/sections?category=" + URLEncoder.encode(categoryId, "UTF-8"));
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    sections.clear();
                    JsonArray sectionArray = json.getAsJsonArray("sections");
                    
                    for (JsonElement elem : sectionArray) {
                        JsonObject secObj = elem.getAsJsonObject();
                        BazaarSection section = new BazaarSection();
                        section.id = secObj.get("id").getAsString();
                        section.name = secObj.get("name").getAsString();
                        section.itemCount = secObj.get("itemCount").getAsInt();
                        sections.add(section);
                    }
                    
                    isLoading = false;
                }
            } catch (Exception e) {
                statusMessage = "Error loading sections: " + e.getMessage();
                System.err.println("[Bazaar] Error loading sections: " + e.getMessage());
            }
        });
    }
    
    private void loadItems(String categoryId, String sectionId) {
        isLoading = true;
        statusMessage = "Loading items...";
        
        executor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/items?category=" + URLEncoder.encode(categoryId, "UTF-8") 
                           + "&section=" + URLEncoder.encode(sectionId, "UTF-8");
                String response = httpGet(url);
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    items.clear();
                    JsonArray itemArray = json.getAsJsonArray("items");
                    
                    for (JsonElement elem : itemArray) {
                        JsonObject itemObj = elem.getAsJsonObject();
                        BazaarItem item = new BazaarItem();
                        item.productId = itemObj.get("productId").getAsString();
                        item.displayName = getDisplayName(item.productId);
                        item.buyPrice = itemObj.get("buyPrice").getAsDouble();
                        item.sellPrice = itemObj.get("sellPrice").getAsDouble();
                        item.buyVolume = itemObj.get("buyVolume").getAsLong();
                        item.sellVolume = itemObj.get("sellVolume").getAsLong();
                        item.unavailable = itemObj.has("unavailable") && itemObj.get("unavailable").getAsBoolean();
                        items.add(item);
                    }
                    
                    isLoading = false;
                }
            } catch (Exception e) {
                statusMessage = "Error loading items: " + e.getMessage();
                System.err.println("[Bazaar] Error loading items: " + e.getMessage());
            }
        });
    }
    
    private void loadItemDetail(String productId) {
        executor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/item?id=" + URLEncoder.encode(productId, "UTF-8");
                String response = httpGet(url);
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    JsonObject itemObj = json.getAsJsonObject("item");
                    JsonObject current = itemObj.getAsJsonObject("current");
                    JsonObject history = itemObj.getAsJsonObject("history");
                    
                    BazaarItemDetail detail = new BazaarItemDetail();
                    detail.productId = itemObj.get("productId").getAsString();
                    detail.displayName = itemObj.get("displayName").getAsString();
                    detail.buyPrice = current.get("buyPrice").getAsDouble();
                    detail.sellPrice = current.get("sellPrice").getAsDouble();
                    detail.buyVolume = current.get("buyVolume").getAsLong();
                    detail.sellVolume = current.get("sellVolume").getAsLong();
                    detail.buyOrders = current.get("buyOrders").getAsLong();
                    detail.sellOrders = current.get("sellOrders").getAsLong();
                    
                    // Parse history
                    if (!history.get("oneHour").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("oneHour");
                        detail.buyPrice1h = h.get("buyPrice").getAsDouble();
                        detail.sellPrice1h = h.get("sellPrice").getAsDouble();
                    }
                    if (!history.get("oneDay").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("oneDay");
                        detail.buyPrice1d = h.get("buyPrice").getAsDouble();
                        detail.sellPrice1d = h.get("sellPrice").getAsDouble();
                    }
                    if (!history.get("sevenDays").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("sevenDays");
                        detail.buyPrice7d = h.get("buyPrice").getAsDouble();
                        detail.sellPrice7d = h.get("sellPrice").getAsDouble();
                    }
                    
                    hoveredItemDetail = detail;
                }
            } catch (Exception e) {
                System.err.println("[Bazaar] Error loading item detail: " + e.getMessage());
            }
        });
    }
    
    private void searchItems(String query) {
        isLoading = true;
        statusMessage = "Searching...";
        
        executor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/search?q=" + URLEncoder.encode(query, "UTF-8");
                String response = httpGet(url);
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    searchResults.clear();
                    JsonArray resultArray = json.getAsJsonArray("results");
                    
                    for (JsonElement elem : resultArray) {
                        JsonObject itemObj = elem.getAsJsonObject();
                        BazaarItem item = new BazaarItem();
                        item.productId = itemObj.get("productId").getAsString();
                        item.displayName = getDisplayName(item.productId);
                        item.buyPrice = itemObj.get("buyPrice").getAsDouble();
                        item.sellPrice = itemObj.get("sellPrice").getAsDouble();
                        item.category = itemObj.get("category").getAsString();
                        item.section = itemObj.get("section").getAsString();
                        searchResults.add(item);
                    }
                    
                    isLoading = false;
                }
            } catch (Exception e) {
                statusMessage = "Error searching: " + e.getMessage();
                System.err.println("[Bazaar] Error searching: " + e.getMessage());
            }
        });
    }
    
    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
}
