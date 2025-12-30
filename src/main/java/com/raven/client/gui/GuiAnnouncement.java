package com.raven.client.gui;

import com.raven.client.gui.notifications.Message;
import com.raven.client.gui.notifications.MessageManager;
import com.raven.client.gui.components.CustomButton;
import com.raven.client.gui.components.RenderUtils;
import com.raven.client.gui.partyfinder.PartyFinderCategory;
import com.raven.client.gui.partyfinder.PartyFinderAPI;
import com.raven.client.utils.ConfigManager;
import com.raven.client.voicechat.VoiceChatManager;
import com.raven.client.voicechat.model.VoiceRoom;
import com.raven.client.voicechat.model.VoiceUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.input.Mouse;

public class GuiAnnouncement extends GuiScreen {

    private Message.MessageType currentTab = Message.MessageType.EVENT;
    private Message selectedMessage = null;
    private float animationProgress = 0f;
    
    // Party Finder category navigation
    private List<PartyFinderCategory> partyFinderRootCategories;
    private PartyFinderCategory currentPartyFinderCategory = null;
    private PartyFinderCategory selectedPartyFinderCategory = null; // The final selected category for party
    private List<PartyFinderCategory> currentCategoryList;
    private int partyFinderScroll = 0;
    
    // Party Finder mode: "browse" or "create"
    private String partyFinderMode = "browse";
    
    // Party creation fields
    private GuiTextField partyNoteField;
    private int partyMinLevel = 0;
    private int partyMaxPlayers = 5;
    private String partyCreationStatus = "";
    
    // Filter panel state
    private boolean showFilterPanel = false;
    private java.util.Map<String, Integer> activeFilters = new java.util.HashMap<>();
    private String editingFilterName = null; // Which filter is being edited (null = none)
    private String editingFilterValue = ""; // Current typed value while editing
    
    // Party list data from API
    private List<JsonObject> partyList = new ArrayList<>();
    private boolean partiesLoading = false;
    private String partiesError = null;
    private long lastPartyRefresh = 0;
    private static final long PARTY_REFRESH_INTERVAL = 10000; // 10 seconds
    private int partyListScroll = 0;
    
    // Current player's party
    private JsonObject myParty = null;
    private boolean myPartyLoading = false;
    
    private static final int PANEL_X = 50;
    private static final int PANEL_Y = 40;
    private static final int HEADER_HEIGHT = 50;
    private static final int MESSAGE_LIST_WIDTH = 280; // Wider message list
    private static final int CATEGORY_PANEL_WIDTH = 200; // Left panel for categories

    // ===================== BAZAAR STATE =====================
    private static final String BAZAAR_API_URL = "http://100.42.184.35:25581";
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.0");
    private static final DecimalFormat VOLUME_FORMAT = new DecimalFormat("#,##0");
    
    private ExecutorService bazaarExecutor = Executors.newSingleThreadExecutor();
    private GuiTextField bazaarSearchField;
    private int bazaarScrollOffset = 0;
    private int bazaarMaxScroll = 0;
    private int bazaarSectionScrollOffset = 0;
    private int bazaarMaxSectionScroll = 0;
    
    // Bazaar Data
    private List<BazaarCategory> bazaarCategories = new ArrayList<>();
    private List<BazaarSection> bazaarSections = new ArrayList<>();
    private List<BazaarItem> bazaarItems = new ArrayList<>();
    private List<BazaarItem> bazaarSearchResults = new ArrayList<>();
    
    // Bazaar State
    private String selectedBazaarCategory = null;
    private String selectedBazaarSection = null;
    private BazaarItem hoveredBazaarItem = null;
    private BazaarItemDetail hoveredBazaarItemDetail = null;
    private String bazaarStatusMessage = "Loading categories...";
    private boolean bazaarIsLoading = false;
    private boolean bazaarShowSearch = false;
    private boolean bazaarDataLoaded = false;
    
    // Voice Chat state
    private String voiceChatViewMode = "main"; // main, rooms, settings, create_room
    private String voiceStatusMessage = "";
    private long voiceStatusMessageTime = 0;
    private int voiceRoomListScroll = 0;
    private List<VoiceRoomEntry> voiceAvailableRooms = new ArrayList<>();
    private GuiTextField voiceApiKeyField;
    private GuiTextField voiceRoomNameField;
    
    // Bazaar layout
    private int bazaarCategoryWidth = 120;
    private int bazaarSectionWidth = 140;
    
    // Bazaar data classes
    private static class BazaarCategory {
        String id;
        String name;
        int color;
        int sectionCount;
    }
    
    private static class BazaarSection {
        String id;
        String name;
        int itemCount;
    }
    
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
    
    private static class BazaarItemDetail {
        String productId;
        String displayName;
        double buyPrice;
        double sellPrice;
        long buyVolume;
        long sellVolume;
        long buyOrders;
        long sellOrders;
        Double buyPrice1h;
        Double sellPrice1h;
        Double buyPrice1d;
        Double sellPrice1d;
        Double buyPrice7d;
        Double sellPrice7d;
    }
    // ===================== END BAZAAR STATE =====================

    public GuiAnnouncement() {
        partyFinderRootCategories = PartyFinderCategory.buildCategoryTree();
        currentCategoryList = partyFinderRootCategories;
        partyFinderMode = "browse";
    }
    
    public GuiAnnouncement(String title, String message) {
        partyFinderRootCategories = PartyFinderCategory.buildCategoryTree();
        currentCategoryList = partyFinderRootCategories;
        partyFinderMode = "browse";
    }

    @Override
    public void initGui() {
        if (!this.buttonList.isEmpty()) {
            return;
        }
        
        this.buttonList.clear();
        
        // Initialize text field for party notes
        int panelWidth = this.width - 100;
        partyNoteField = new GuiTextField(100, this.fontRendererObj, 
            PANEL_X + CATEGORY_PANEL_WIDTH + 30, PANEL_Y + HEADER_HEIGHT + 100, 
            panelWidth - CATEGORY_PANEL_WIDTH - 50, 20);
        partyNoteField.setMaxStringLength(100);
        partyNoteField.setText("");
        
        // Close button
        this.buttonList.add(new CustomButton(0, this.width - 35, 15, 25, 25, "X")
            .setTextColor(0xFFAAAA));
        
        // Tab buttons - TOP LEFT corner
        int tabX = PANEL_X + 15;
        int tabY = PANEL_Y + 15;
        
        this.buttonList.add(new CustomButton(1, tabX, tabY, 65, 25, "News")
            .setBorderColor(0xFF4ECDC4));
        this.buttonList.add(new CustomButton(2, tabX + 70, tabY, 65, 25, "Events")
            .setBorderColor(0xFFFF6B6B));
        this.buttonList.add(new CustomButton(3, tabX + 140, tabY, 80, 25, "Update Log")
            .setBorderColor(0xFF95E1D3));
        this.buttonList.add(new CustomButton(4, tabX + 225, tabY, 85, 25, "Party Finder")
            .setBorderColor(0xFFFFD700));
        this.buttonList.add(new CustomButton(5, tabX + 315, tabY, 60, 25, "Bazaar")
            .setBorderColor(0xFF55FF55));
        this.buttonList.add(new CustomButton(6, tabX + 380, tabY, 60, 25, "Auction")
            .setBorderColor(0xFFAA55FF));
        this.buttonList.add(new CustomButton(7, tabX + 445, tabY, 55, 25, "Stocks")
            .setBorderColor(0xFF55FFFF));
        this.buttonList.add(new CustomButton(8, tabX + 505, tabY, 50, 25, "Voice")
            .setBorderColor(0xFF5865F2));  // Discord blue
        
        // Initialize Bazaar search field
        bazaarSearchField = new GuiTextField(101, this.fontRendererObj,
            PANEL_X + bazaarCategoryWidth + 15, PANEL_Y + HEADER_HEIGHT + 5,
            180, 16);
        bazaarSearchField.setMaxStringLength(50);
        bazaarSearchField.setFocused(false);
        bazaarSearchField.setText("");
        
        // Initialize Voice API key field
        voiceApiKeyField = new GuiTextField(102, this.fontRendererObj,
            PANEL_X + 130, PANEL_Y + HEADER_HEIGHT + 150,
            200, 18);
        voiceApiKeyField.setMaxStringLength(100);
        voiceApiKeyField.setFocused(false);
        // Load saved API key from config
        String savedVoiceKey = ConfigManager.get("voiceApiKey", "");
        voiceApiKeyField.setText(savedVoiceKey);
        // Apply to VoiceChatManager if exists
        if (!savedVoiceKey.isEmpty()) {
            VoiceChatManager.getInstance().setAuthToken(savedVoiceKey);
        }
        
        // Initialize Voice Room Name field
        voiceRoomNameField = new GuiTextField(103, this.fontRendererObj,
            PANEL_X + 130, PANEL_Y + HEADER_HEIGHT + 150,
            200, 18);
        voiceRoomNameField.setMaxStringLength(32);
        voiceRoomNameField.setFocused(false);
        voiceRoomNameField.setText("");
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) throws java.io.IOException {
        if (button.id == 0) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if (button.id == 1) {
            currentTab = Message.MessageType.NEWS;
            selectedMessage = null;
        } else if (button.id == 2) {
            currentTab = Message.MessageType.EVENT;
            selectedMessage = null;
        } else if (button.id == 3) {
            currentTab = Message.MessageType.INBOX;
            selectedMessage = null;
        } else if (button.id == 4) {
            currentTab = Message.MessageType.PARTY_FINDER;
            selectedMessage = null;
            // Reset Party Finder to root
            currentPartyFinderCategory = null;
            selectedPartyFinderCategory = null;
            currentCategoryList = partyFinderRootCategories;
            partyFinderScroll = 0;
            partyFinderMode = "browse";
            partyCreationStatus = "";
        } else if (button.id == 5) {
            currentTab = Message.MessageType.BAZAAR;
            selectedMessage = null;
            // Load bazaar data if not already loaded
            if (!bazaarDataLoaded) {
                loadBazaarCategories();
            }
        } else if (button.id == 6) {
            currentTab = Message.MessageType.AUCTION_HOUSE;
            selectedMessage = null;
        } else if (button.id == 7) {
            currentTab = Message.MessageType.STOCK_MARKET;
            selectedMessage = null;
        } else if (button.id == 8) {
            currentTab = Message.MessageType.VOICE_CHAT;
            selectedMessage = null;
            voiceChatViewMode = "main";
            
            // Auto-connect to voice server when opening Voice Chat tab
            VoiceChatManager voiceManager = VoiceChatManager.getInstance();
            if (!voiceManager.isConnected() && !voiceManager.isConnecting()) {
                // Check if we have an auth token
                String token = voiceManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    // Initialize if not already done
                    if (!voiceManager.isInitialized()) {
                        voiceManager.initialize();
                    }
                    // Auto-connect
                    voiceManager.connect();
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (animationProgress < 1f) {
            animationProgress += 0.05f;
        }
        
        int panelWidth = this.width - 100;
        int panelHeight = this.height - 80;
        
        // Draw darker background with vignette effect
        RenderUtils.drawGradient(0, 0, this.width, this.height, 0xE6050505, 0xE6000000);
        
        // Draw panel shadow (stronger)
        RenderUtils.drawShadow(PANEL_X - 5, PANEL_Y - 5, PANEL_X + panelWidth + 5, PANEL_Y + panelHeight + 5, 100);
        
        // Draw main panel
        RenderUtils.drawGradient(PANEL_X, PANEL_Y, PANEL_X + panelWidth, PANEL_Y + panelHeight,
                0xFF15151F, 0xFF0D0D15);
        RenderUtils.drawBorder(PANEL_X, PANEL_Y, PANEL_X + panelWidth, PANEL_Y + panelHeight, 0xFF333344, 2);
        
        // Draw header
        RenderUtils.drawGradient(PANEL_X, PANEL_Y, PANEL_X + panelWidth, PANEL_Y + HEADER_HEIGHT,
                0xFF1A1A2E, 0xFF16213E);
        
        // Draw unread counts on tab buttons AFTER super.drawScreen to render on top
        // (moved to after super.drawScreen call below)
        
        // Calculate layout
        int listHeight = panelHeight - HEADER_HEIGHT - 20;
        int listX = PANEL_X + 10; // Left side
        int listY = PANEL_Y + HEADER_HEIGHT + 10;
        
        // Check if we're on a full-panel tab (Auction, Stocks - not Bazaar now)
        boolean isFullPanelTab = currentTab == Message.MessageType.AUCTION_HOUSE || 
                                  currentTab == Message.MessageType.STOCK_MARKET;
        
        // Check if we're on Party Finder tab
        boolean isPartyFinder = currentTab == Message.MessageType.PARTY_FINDER;
        
        // Check if we're on Bazaar tab
        boolean isBazaar = currentTab == Message.MessageType.BAZAAR;
        
        // Check if we're on Voice Chat tab
        boolean isVoiceChat = currentTab == Message.MessageType.VOICE_CHAT;
        
        if (isBazaar) {
            // Bazaar with full browsing functionality
            int fullWidth = panelWidth - 20;
            drawBazaarContent(listX, listY, fullWidth, listHeight, mouseX, mouseY);
        } else if (isVoiceChat) {
            // Voice Chat with room management
            int fullWidth = panelWidth - 20;
            drawVoiceChatContent(listX, listY, fullWidth, listHeight, mouseX, mouseY);
        } else if (isFullPanelTab) {
            // Full-width content panel for Bazaar/Auction/Stocks
            int fullWidth = panelWidth - 20;
            drawFullPanelContent(listX, listY, fullWidth, listHeight, currentTab);
        } else if (isPartyFinder) {
            // Party Finder with category navigation
            int fullWidth = panelWidth - 20;
            drawPartyFinderContent(listX, listY, fullWidth, listHeight, mouseX, mouseY);
        } else {
            // Standard message layout - Message list on LEFT, Content on RIGHT
            int contentX = PANEL_X + MESSAGE_LIST_WIDTH + 20;
            int contentWidth = panelWidth - MESSAGE_LIST_WIDTH - 30;
            
            // Message list (left side - wider)
            List<Message> messages = MessageManager.getMessagesByType(currentTab);
            drawMessageList(listX, listY, MESSAGE_LIST_WIDTH, listHeight, messages, mouseX, mouseY);
            
            // Message content (right side)
            if (selectedMessage != null) {
                drawMessageContent(contentX, listY, contentWidth, listHeight, selectedMessage);
            } else {
                // Show placeholder when no message selected
                RenderUtils.drawBox(contentX, listY, contentX + contentWidth, listY + listHeight, 0xFF16213E, 0xFF333344, 1);
                String hint = "Select a message to read";
                int hintWidth = this.fontRendererObj.getStringWidth(hint);
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString(hint, contentX + (contentWidth - hintWidth) / 2, listY + listHeight / 2, 0x666666);
            }
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Draw unread counts on tab buttons (rendered AFTER buttons so they appear on top)
        int tabX = PANEL_X + 15;
        int tabY = PANEL_Y + 15;
        
        // News unread count (button at tabX, width 65)
        int newsUnread = MessageManager.getUnreadCountByType(Message.MessageType.NEWS);
        if (newsUnread > 0) {
            drawUnreadCount(tabX + 65 - 14, tabY + 4, newsUnread);
        }
        
        // Events unread count (button at tabX + 70, width 65)
        int eventsUnread = MessageManager.getUnreadCountByType(Message.MessageType.EVENT);
        if (eventsUnread > 0) {
            drawUnreadCount(tabX + 70 + 65 - 14, tabY + 4, eventsUnread);
        }
        
        // Update Log unread count (button at tabX + 140, width 80)
        int inboxUnread = MessageManager.getUnreadCountByType(Message.MessageType.INBOX);
        if (inboxUnread > 0) {
            drawUnreadCount(tabX + 140 + 80 - 14, tabY + 4, inboxUnread);
        }
    }
    
    private void drawFullPanelContent(int x, int y, int width, int height, Message.MessageType type) {
        // Draw the full panel box with colored header
        RenderUtils.drawBox(x, y, x + width, y + height, 0xFF16213E, 0xFF333344, 1);
        
        // Colored header bar
        RenderUtils.drawRect(x, y, x + width, y + 45, type.color);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString(type.label, x + 10, y + 8, 0xFF000000);
        
        // Content area - placeholder for now
        int contentY = y + 55;
        String placeholder = "";
        switch (type) {
            case AUCTION_HOUSE:
                placeholder = "Auction House listings and alerts will appear here.";
                break;
            case STOCK_MARKET:
                placeholder = "Stock Market portfolio and updates will appear here.";
                break;
            default:
                break;
        }
        
        // Center the placeholder text
        int textWidth = this.fontRendererObj.getStringWidth(placeholder);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString(placeholder, x + (width - textWidth) / 2, contentY + 20, 0x888888);
    }
    
    private void drawPartyFinderContent(int x, int y, int width, int height, int mouseX, int mouseY) {
        // LEFT PANEL - Category Navigation
        int leftPanelWidth = CATEGORY_PANEL_WIDTH;
        RenderUtils.drawBox(x, y, x + leftPanelWidth, y + height, 0xFF1A1A2E, 0xFF333344, 1);
        
        // Category header
        RenderUtils.drawRect(x, y, x + leftPanelWidth, y + 35, 0xFF2A2A3E);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Categories", x + 10, y + 12, 0xFFFFFF);
        
        // Back button if not at root
        if (currentPartyFinderCategory != null) {
            String backText = "< Back";
            int backY = y + 40;
            boolean backHover = mouseX >= x + 5 && mouseX < x + leftPanelWidth - 5 &&
                               mouseY >= backY && mouseY < backY + 20;
            RenderUtils.drawRect(x + 5, backY, x + leftPanelWidth - 5, backY + 20, 
                backHover ? 0xFF3a3a4a : 0xFF2a2a3a);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(backText, x + 10, backY + 6, 0xFFAAAA);
        }
        
        // Category list
        int catY = y + (currentPartyFinderCategory != null ? 65 : 45);
        int itemHeight = 30;
        
        for (PartyFinderCategory category : currentCategoryList) {
            if (catY + itemHeight > y + height - 5) break;
            
            boolean hover = mouseX >= x + 5 && mouseX < x + leftPanelWidth - 5 &&
                           mouseY >= catY && mouseY < catY + itemHeight;
            boolean selected = (selectedPartyFinderCategory == category);
            
            int bgColor = selected ? 0xFF4a4a5a : (hover ? 0xFF3a3a4a : 0xFF2a2a3a);
            RenderUtils.drawRect(x + 5, catY, x + leftPanelWidth - 5, catY + itemHeight, bgColor);
            RenderUtils.drawRect(x + 5, catY, x + 8, catY + itemHeight, category.color | 0xFF000000);
            
            String name = category.name != null ? category.name : "Unknown";
            int maxWidth = leftPanelWidth - 35;
            if (this.fontRendererObj.getStringWidth(name) > maxWidth) {
                while (this.fontRendererObj.getStringWidth(name + "..") > maxWidth && name.length() > 3) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "..";
            }
            // Reset GL state and draw with shadow for visibility
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawStringWithShadow(name, x + 14, catY + 10, 0xFFFFFFFF);
            
            if (category.hasChildren()) {
                this.fontRendererObj.drawStringWithShadow(">", x + leftPanelWidth - 15, catY + 10, 0xFF888888);
            }
            
            catY += itemHeight + 3;
        }
        
        // RIGHT PANEL - Main Content
        int rightX = x + leftPanelWidth + 10;
        int rightWidth = width - leftPanelWidth - 10;
        RenderUtils.drawBox(rightX, y, rightX + rightWidth, y + height, 0xFF16213E, 0xFF333344, 1);
        
        // Header bar with selected category or mode
        int headerColor = selectedPartyFinderCategory != null ? (selectedPartyFinderCategory.color | 0xFF000000) : 0xFFFFD700;
        RenderUtils.drawRect(rightX, y, rightX + rightWidth, y + 45, headerColor);
        
        String headerText = selectedPartyFinderCategory != null ? 
            selectedPartyFinderCategory.getFullPath() : "Party Finder";
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString(headerText, rightX + 10, y + 8, 0xFF000000);
        
        // Mode toggle buttons (Browse / Create / Filters)
        int filtersX = rightX + rightWidth - 225;
        int browseX = rightX + rightWidth - 150;
        int createX = rightX + rightWidth - 75;
        int btnY = y + 20;
        
        boolean filtersHover = mouseX >= filtersX && mouseX < filtersX + 70 && mouseY >= btnY && mouseY < btnY + 20;
        boolean browseHover = mouseX >= browseX && mouseX < browseX + 70 && mouseY >= btnY && mouseY < btnY + 20;
        boolean createHover = mouseX >= createX && mouseX < createX + 70 && mouseY >= btnY && mouseY < btnY + 20;
        
        int filtersBg = showFilterPanel ? 0xFF5555AA : (filtersHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        int browseBg = partyFinderMode.equals("browse") && !showFilterPanel ? 0xFF55AA55 : (browseHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        int createBg = partyFinderMode.equals("create") && !showFilterPanel ? 0xFF55AA55 : (createHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        
        // Draw filter count badge if filters are active
        int activeFilterCount = activeFilters.size();
        
        RenderUtils.drawRect(filtersX, btnY, filtersX + 70, btnY + 20, filtersBg);
        RenderUtils.drawRect(browseX, btnY, browseX + 70, btnY + 20, browseBg);
        RenderUtils.drawRect(createX, btnY, createX + 70, btnY + 20, createBg);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Filters", filtersX + 17, btnY + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("Browse", browseX + 15, btnY + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("Create", createX + 17, btnY + 6, 0xFFFFFF);
        
        // Draw active filter count badge
        if (activeFilterCount > 0) {
            String countStr = String.valueOf(activeFilterCount);
            int badgeX = filtersX + 60;
            int badgeY = btnY - 2;
            RenderUtils.drawRect(badgeX, badgeY, badgeX + 12, badgeY + 12, 0xFFFF5555);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(countStr, badgeX + 3, badgeY + 2, 0xFFFFFFFF);
        }
        
        // Content area
        int contentY = y + 55;
        int contentHeight = height - 55;
        
        if (showFilterPanel) {
            drawFilterPanel(rightX, contentY, rightWidth, contentHeight, mouseX, mouseY);
        } else if (partyFinderMode.equals("create")) {
            drawPartyCreationForm(rightX, contentY, rightWidth, contentHeight, mouseX, mouseY);
        } else {
            drawPartyBrowseContent(rightX, contentY, rightWidth, contentHeight, mouseX, mouseY);
        }
    }
    
    private void drawPartyCreationForm(int x, int y, int width, int height, int mouseX, int mouseY) {
        // Selected category display
        if (selectedPartyFinderCategory != null) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Creating party for:", x + 10, y + 10, 0xAAAAAA);
            this.fontRendererObj.drawString(selectedPartyFinderCategory.getFullPath(), x + 10, y + 25, 0xFFFFFF);
        } else {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Select a category from the left panel first", x + 10, y + 10, 0xFF6666);
            return;
        }
        
        // Party Note
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Party Note:", x + 10, y + 55, 0xAAAAAA);
        partyNoteField.xPosition = x + 10;
        partyNoteField.yPosition = y + 70;
        partyNoteField.width = width - 30;
        partyNoteField.drawTextBox();
        
        // Min Level
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Min Level: " + partyMinLevel, x + 10, y + 105, 0xAAAAAA);
        
        // Level buttons
        int lvlBtnY = y + 100;
        boolean minusHover = mouseX >= x + 100 && mouseX < x + 120 && mouseY >= lvlBtnY && mouseY < lvlBtnY + 20;
        boolean plusHover = mouseX >= x + 125 && mouseX < x + 145 && mouseY >= lvlBtnY && mouseY < lvlBtnY + 20;
        
        RenderUtils.drawRect(x + 100, lvlBtnY, x + 120, lvlBtnY + 20, minusHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        RenderUtils.drawRect(x + 125, lvlBtnY, x + 145, lvlBtnY + 20, plusHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("-", x + 107, lvlBtnY + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("+", x + 132, lvlBtnY + 6, 0xFFFFFF);
        
        // Max Players
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Max Players: " + partyMaxPlayers, x + 10, y + 135, 0xAAAAAA);
        
        int plrBtnY = y + 130;
        boolean plrMinusHover = mouseX >= x + 110 && mouseX < x + 130 && mouseY >= plrBtnY && mouseY < plrBtnY + 20;
        boolean plrPlusHover = mouseX >= x + 135 && mouseX < x + 155 && mouseY >= plrBtnY && mouseY < plrBtnY + 20;
        
        RenderUtils.drawRect(x + 110, plrBtnY, x + 130, plrBtnY + 20, plrMinusHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        RenderUtils.drawRect(x + 135, plrBtnY, x + 155, plrBtnY + 20, plrPlusHover ? 0xFF4a4a4a : 0xFF3a3a3a);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("-", x + 117, plrBtnY + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("+", x + 142, plrBtnY + 6, 0xFFFFFF);
        
        // Create Party Button
        int createBtnY = y + 175;
        boolean createBtnHover = mouseX >= x + 10 && mouseX < x + 150 && mouseY >= createBtnY && mouseY < createBtnY + 30;
        RenderUtils.drawRect(x + 10, createBtnY, x + 150, createBtnY + 30, createBtnHover ? 0xFF55CC55 : 0xFF44AA44);
        RenderUtils.drawBorder(x + 10, createBtnY, x + 150, createBtnY + 30, 0xFF66DD66, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Create Party", x + 35, createBtnY + 10, 0xFFFFFF);
        
        // Status message
        if (!partyCreationStatus.isEmpty()) {
            int statusColor = partyCreationStatus.startsWith("Error") ? 0xFF6666 : 0x66FF66;
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(partyCreationStatus, x + 10, createBtnY + 45, statusColor);
        }
    }
    
    private void drawPartyBrowseContent(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (selectedPartyFinderCategory == null) {
            String hint = "Select a category to browse parties";
            int hintWidth = this.fontRendererObj.getStringWidth(hint);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(hint, x + (width - hintWidth) / 2, y + height / 2 - 20, 0x666666);
            return;
        }
        
        // Auto-refresh parties
        long now = System.currentTimeMillis();
        if (now - lastPartyRefresh > PARTY_REFRESH_INTERVAL && !partiesLoading) {
            refreshParties();
        }
        
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Available Parties for: " + selectedPartyFinderCategory.name, x + 10, y + 10, 0xAAAAAA);
        
        // Refresh button
        int refreshX = x + width - 70;
        boolean refreshHover = mouseX >= refreshX && mouseX < refreshX + 60 && mouseY >= y + 5 && mouseY < y + 25;
        RenderUtils.drawRect(refreshX, y + 5, refreshX + 60, y + 25, refreshHover ? 0xFF4a4a5a : 0xFF3a3a4a);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString(partiesLoading ? "..." : "Refresh", refreshX + 10, y + 10, 0xFFFFFF);
        
        // Show active filters if any
        int listStartY = y + 30;
        if (!activeFilters.isEmpty()) {
            int filterY = y + 25;
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Filters:", x + 10, filterY, 0xFF8888FF);
            StringBuilder filterStr = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> filter : activeFilters.entrySet()) {
                if (filterStr.length() > 0) filterStr.append(", ");
                filterStr.append(filter.getKey()).append(": ").append(filter.getValue()).append("+");
            }
            this.fontRendererObj.drawString(filterStr.toString(), x + 55, filterY, 0xFF7777DD);
            listStartY = y + 40;
        }
        
        // Error message
        if (partiesError != null) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Error: " + partiesError, x + 10, listStartY, 0xFF6666);
            return;
        }
        
        // Loading message
        if (partiesLoading && partyList.isEmpty()) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Loading parties...", x + 10, listStartY, 0x888888);
            return;
        }
        
        // No parties message
        if (partyList.isEmpty()) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("No parties available", x + 10, listStartY, 0x888888);
            this.fontRendererObj.drawString("Be the first to create one!", x + 10, listStartY + 15, 0x666666);
            return;
        }
        
        // Draw party list
        int itemHeight = 50;
        int maxItems = (height - (listStartY - y) - 10) / (itemHeight + 5);
        int listY = listStartY;
        
        for (int i = partyListScroll; i < partyList.size() && i < partyListScroll + maxItems; i++) {
            JsonObject party = partyList.get(i);
            
            boolean hover = mouseX >= x + 10 && mouseX < x + width - 20 &&
                           mouseY >= listY && mouseY < listY + itemHeight;
            
            // Party item background
            int bgColor = hover ? 0xFF3a3a4a : 0xFF2a2a3a;
            RenderUtils.drawRect(x + 10, listY, x + width - 20, listY + itemHeight, bgColor);
            
            // Category color bar
            int catColor = party.has("categoryColor") ? party.get("categoryColor").getAsInt() : 0xFFFFFF;
            RenderUtils.drawRect(x + 10, listY, x + 14, listY + itemHeight, catColor | 0xFF000000);
            
            // Leader name and note
            String leader = party.has("leader") ? party.get("leader").getAsString() : "Unknown";
            String note = party.has("note") && !party.get("note").getAsString().isEmpty() 
                         ? party.get("note").getAsString() : party.has("category") 
                         ? party.get("category").getAsString() : "Party";
            
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawStringWithShadow(leader + "'s Party", x + 20, listY + 5, 0xFFFFFF);
            
            // Truncate note if needed
            int maxNoteWidth = width - 150;
            if (this.fontRendererObj.getStringWidth(note) > maxNoteWidth) {
                while (this.fontRendererObj.getStringWidth(note + "...") > maxNoteWidth && note.length() > 3) {
                    note = note.substring(0, note.length() - 1);
                }
                note = note + "...";
            }
            this.fontRendererObj.drawString(note, x + 20, listY + 18, 0xAAAAAA);
            
            // Member count
            int members = party.has("memberCount") ? party.get("memberCount").getAsInt() : 1;
            int maxMembers = party.has("maxPlayers") ? party.get("maxPlayers").getAsInt() : 5;
            String memberStr = members + "/" + maxMembers;
            this.fontRendererObj.drawString(memberStr, x + 20, listY + 31, 0x888888);
            
            // Min level if set
            int minLevel = party.has("minLevel") ? party.get("minLevel").getAsInt() : 0;
            if (minLevel > 0) {
                this.fontRendererObj.drawString("Lvl " + minLevel + "+", x + 70, listY + 31, 0x888888);
            }
            
            // Join button
            int joinX = x + width - 80;
            boolean joinHover = mouseX >= joinX && mouseX < joinX + 50 && 
                               mouseY >= listY + 10 && mouseY < listY + 35;
            RenderUtils.drawRect(joinX, listY + 10, joinX + 50, listY + 35, 
                               joinHover ? 0xFF55AA55 : 0xFF449944);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Join", joinX + 13, listY + 17, 0xFFFFFF);
            
            listY += itemHeight + 5;
        }
        
        // Scroll indicators
        if (partyListScroll > 0) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("^ More above", x + width / 2 - 30, listStartY - 10, 0x888888);
        }
        if (partyListScroll + maxItems < partyList.size()) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("v More below", x + width / 2 - 30, y + height - 15, 0x888888);
        }
    }
    
    private void refreshParties() {
        if (selectedPartyFinderCategory == null) return;
        
        partiesLoading = true;
        partiesError = null;
        lastPartyRefresh = System.currentTimeMillis();
        
        System.out.println("[PartyFinder] Refreshing parties for category: " + selectedPartyFinderCategory.getFullPath());
        
        PartyFinderAPI.getParties(selectedPartyFinderCategory, new PartyFinderAPI.PartiesCallback() {
            @Override
            public void onSuccess(JsonObject response) {
                partiesLoading = false;
                partyList.clear();
                
                System.out.println("[PartyFinder] Response: " + response.toString());
                
                if (response.has("parties") && response.get("parties").isJsonArray()) {
                    JsonArray parties = response.getAsJsonArray("parties");
                    for (JsonElement elem : parties) {
                        if (elem.isJsonObject()) {
                            partyList.add(elem.getAsJsonObject());
                        }
                    }
                }
                System.out.println("[PartyFinder] Loaded " + partyList.size() + " parties");
            }
            
            @Override
            public void onError(String error) {
                partiesLoading = false;
                partiesError = error;
                System.err.println("[PartyFinder] Error: " + error);
            }
        });
    }
    
    private void drawFilterPanel(int x, int y, int width, int height, int mouseX, int mouseY) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Search Filters", x + 10, y + 10, 0xFFFFFF);
        
        // Get filters based on selected category - only category-specific + global 3
        List<String> availableFilters = new ArrayList<>();
        
        if (selectedPartyFinderCategory != null) {
            // Add primary filter first (category-specific only, no parent inheritance)
            if (selectedPartyFinderCategory.primaryFilter != null) {
                availableFilters.add(selectedPartyFinderCategory.primaryFilter);
            }
            // Add secondary filters (category-specific only)
            availableFilters.addAll(selectedPartyFinderCategory.secondaryFilters);
        }
        
        // Add the 3 global filters that are always available
        String[] commonFilters = {"Combat Level", "SkyBlock Level", "Magical Power"};
        for (String common : commonFilters) {
            if (!availableFilters.contains(common)) {
                availableFilters.add(common);
            }
        }
        
        if (availableFilters.isEmpty()) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Select a category to see available filters", x + 10, y + 40, 0x888888);
            return;
        }
        
        int filterY = y + 35;
        int filterRowHeight = 28;
        
        for (String filterName : availableFilters) {
            if (filterY + filterRowHeight > y + height - 50) break;
            
            // Filter row background
            boolean isActive = activeFilters.containsKey(filterName);
            int currentValue = activeFilters.getOrDefault(filterName, 0);
            boolean isEditing = filterName.equals(editingFilterName);
            
            int rowBg = isEditing ? 0xFF3a4a5a : (isActive ? 0xFF2a3a4a : 0xFF1a2a3a);
            RenderUtils.drawRect(x + 10, filterY, x + width - 20, filterY + filterRowHeight - 3, rowBg);
            
            // Reset GL state before drawing text to prevent color issues
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Filter name
            this.fontRendererObj.drawStringWithShadow(filterName, x + 15, filterY + 5, isActive ? 0xFF88AAFF : 0xFFAAAAAA);
            
            // Value controls area
            int controlsX = x + width - 130;
            
            // Reset GL state for minus button
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Minus text button
            boolean minusHover = mouseX >= controlsX && mouseX < controlsX + 15 && 
                                mouseY >= filterY + 2 && mouseY < filterY + 22;
            this.fontRendererObj.drawStringWithShadow("-", controlsX + 2, filterY + 6, 
                                minusHover ? 0xFFFFAAAA : 0xFFAAAAAA);
            
            // Editable value field
            int valueFieldX = controlsX + 20;
            int valueFieldWidth = 45;
            boolean valueHover = mouseX >= valueFieldX && mouseX < valueFieldX + valueFieldWidth && 
                                mouseY >= filterY + 2 && mouseY < filterY + 22;
            
            // Draw value input box
            int valueBg = isEditing ? 0xFF4a5a6a : (valueHover ? 0xFF3a4a5a : 0xFF2a3a4a);
            RenderUtils.drawRect(valueFieldX, filterY + 2, valueFieldX + valueFieldWidth, filterY + 22, valueBg);
            RenderUtils.drawBorder(valueFieldX, filterY + 2, valueFieldX + valueFieldWidth, filterY + 22, 
                                  isEditing ? 0xFF88AAFF : 0xFF555566, 1);
            
            // Reset GL state for value text
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Draw value text (show editing value or current value)
            String displayValue = isEditing ? editingFilterValue + (System.currentTimeMillis() % 1000 < 500 ? "_" : "") 
                                           : String.valueOf(currentValue);
            int textWidth = this.fontRendererObj.getStringWidth(displayValue.replace("_", ""));
            int textX = valueFieldX + (valueFieldWidth - textWidth) / 2;
            this.fontRendererObj.drawStringWithShadow(displayValue, textX, filterY + 7, 0xFFFFFFFF);
            
            // Reset GL state for plus button
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Plus text button
            int plusX = valueFieldX + valueFieldWidth + 5;
            boolean plusHover = mouseX >= plusX && mouseX < plusX + 15 && 
                               mouseY >= filterY + 2 && mouseY < filterY + 22;
            this.fontRendererObj.drawStringWithShadow("+", plusX + 2, filterY + 6, 
                               plusHover ? 0xFFAAFFAA : 0xFFAAAAAA);
            
            // Clear button (X) if filter is active
            if (isActive) {
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int clearBtnX = x + width - 45;
                boolean clearHover = mouseX >= clearBtnX && mouseX < clearBtnX + 20 && 
                                    mouseY >= filterY + 2 && mouseY < filterY + 22;
                this.fontRendererObj.drawStringWithShadow("X", clearBtnX + 6, filterY + 6, 
                                    clearHover ? 0xFFFF6666 : 0xFFAA6666);
            }
            
            filterY += filterRowHeight;
        }
        
        // Clear All Filters button
        int clearAllY = y + height - 40;
        boolean clearAllHover = mouseX >= x + 10 && mouseX < x + 130 && 
                               mouseY >= clearAllY && mouseY < clearAllY + 25;
        boolean clearAllEnabled = !activeFilters.isEmpty();
        int clearAllBg = !clearAllEnabled ? 0xFF333344 : (clearAllHover ? 0xFFBB5555 : 0xFF994444);
        RenderUtils.drawRect(x + 10, clearAllY, x + 130, clearAllY + 25, clearAllBg);
        RenderUtils.drawBorder(x + 10, clearAllY, x + 130, clearAllY + 25, 
                              clearAllEnabled ? 0xFFCC6666 : 0xFF444455, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawStringWithShadow("Clear All", x + 35, clearAllY + 8, 
                                       clearAllEnabled ? 0xFFFFFFFF : 0xFF666666);
        
        // Apply button
        int applyY = y + height - 40;
        boolean applyHover = mouseX >= x + width - 100 && mouseX < x + width - 20 && 
                            mouseY >= applyY && mouseY < applyY + 25;
        int applyBg = applyHover ? 0xFF66BB66 : 0xFF55AA55;
        RenderUtils.drawRect(x + width - 100, applyY, x + width - 20, applyY + 25, applyBg);
        RenderUtils.drawBorder(x + width - 100, applyY, x + width - 20, applyY + 25, 0xFF77CC77, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawStringWithShadow("Apply", x + width - 75, applyY + 8, 0xFFFFFFFF);
    }
    
    // ===================== VOICE CHAT DRAWING METHODS =====================
    
    private void drawVoiceChatContent(int x, int y, int width, int height, int mouseX, int mouseY) {
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        
        // Main panel background
        RenderUtils.drawBox(x, y, x + width, y + height, 0xFF16213E, 0xFF333344, 1);
        
        // Title header with Discord blue
        RenderUtils.drawRect(x, y, x + width, y + 40, 0xFF5865F2);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Voice Chat", x + 15, y + 15, 0xFFFFFFFF);
        
        // Connection status
        int statusColor = voiceManager.isConnected() ? 0xFF55FF55 : 0xFFFF5555;
        String statusText = voiceManager.isConnected() ? "Connected" : "Disconnected";
        int statusWidth = this.fontRendererObj.getStringWidth(statusText);
        RenderUtils.drawRect(x + width - statusWidth - 30, y + 10, x + width - 10, y + 30, 0xFF2a2a3a);
        this.fontRendererObj.drawString(statusText, x + width - statusWidth - 20, y + 16, statusColor);
        
        int contentY = y + 55;
        int buttonWidth = 90;
        int buttonHeight = 22;
        int buttonSpacing = 10;
        
        // First row: Connect, Browse Rooms, Settings
        // Connect/Disconnect button
        boolean connectHover = mouseX >= x + 15 && mouseX < x + 15 + buttonWidth && 
                              mouseY >= contentY && mouseY < contentY + buttonHeight;
        int connectBg = connectHover ? 0xFF4a5a6a : 0xFF3a4a5a;
        RenderUtils.drawRect(x + 15, contentY, x + 15 + buttonWidth, contentY + buttonHeight, connectBg);
        RenderUtils.drawBorder(x + 15, contentY, x + 15 + buttonWidth, contentY + buttonHeight, 0xFF5865F2, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        String connectText = voiceManager.isConnected() ? "Disconnect" : "Connect";
        int connectTextW = this.fontRendererObj.getStringWidth(connectText);
        this.fontRendererObj.drawString(connectText, x + 15 + (buttonWidth - connectTextW) / 2, contentY + 7, 0xFFFFFF);
        
        // Browse Rooms button
        int browseX = x + 15 + buttonWidth + buttonSpacing;
        boolean browseHover = mouseX >= browseX && mouseX < browseX + 95 && 
                             mouseY >= contentY && mouseY < contentY + buttonHeight;
        int browseBg = voiceChatViewMode.equals("rooms") ? 0xFF5865F2 : (browseHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawRect(browseX, contentY, browseX + 95, contentY + buttonHeight, browseBg);
        RenderUtils.drawBorder(browseX, contentY, browseX + 95, contentY + buttonHeight, 0xFF5865F2, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Browse Rooms", browseX + 8, contentY + 7, 0xFFFFFF);
        
        // Settings button
        int settingsX = browseX + 95 + buttonSpacing;
        boolean settingsHover = mouseX >= settingsX && mouseX < settingsX + 70 && 
                               mouseY >= contentY && mouseY < contentY + buttonHeight;
        int settingsBg = voiceChatViewMode.equals("settings") ? 0xFF5865F2 : (settingsHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawRect(settingsX, contentY, settingsX + 70, contentY + buttonHeight, settingsBg);
        RenderUtils.drawBorder(settingsX, contentY, settingsX + 70, contentY + buttonHeight, 0xFF5865F2, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Settings", settingsX + 12, contentY + 7, 0xFFFFFF);
        
        contentY += buttonHeight + 10;
        
        // Second row: Mute, Deafen, Leave Room
        // Mute button
        int muteX = x + 15;
        boolean muteHover = mouseX >= muteX && mouseX < muteX + 70 && 
                           mouseY >= contentY && mouseY < contentY + buttonHeight;
        int muteBg = voiceManager.isMuted() ? 0xFF8B0000 : (muteHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawRect(muteX, contentY, muteX + 70, contentY + buttonHeight, muteBg);
        RenderUtils.drawBorder(muteX, contentY, muteX + 70, contentY + buttonHeight, voiceManager.isMuted() ? 0xFFFF5555 : 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        String muteText = voiceManager.isMuted() ? "Unmute" : "Mute";
        int muteTextW = this.fontRendererObj.getStringWidth(muteText);
        this.fontRendererObj.drawString(muteText, muteX + (70 - muteTextW) / 2, contentY + 7, 0xFFFFFF);
        
        // Deafen button
        int deafenX = muteX + 70 + buttonSpacing;
        boolean deafenHover = mouseX >= deafenX && mouseX < deafenX + 70 && 
                             mouseY >= contentY && mouseY < contentY + buttonHeight;
        int deafenBg = voiceManager.isDeafened() ? 0xFF8B0000 : (deafenHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawRect(deafenX, contentY, deafenX + 70, contentY + buttonHeight, deafenBg);
        RenderUtils.drawBorder(deafenX, contentY, deafenX + 70, contentY + buttonHeight, voiceManager.isDeafened() ? 0xFFFF5555 : 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        String deafenText = voiceManager.isDeafened() ? "Undeafen" : "Deafen";
        int deafenTextW = this.fontRendererObj.getStringWidth(deafenText);
        this.fontRendererObj.drawString(deafenText, deafenX + (70 - deafenTextW) / 2, contentY + 7, 0xFFFFFF);
        
        // Leave Room button
        int leaveX = deafenX + 70 + buttonSpacing;
        boolean leaveHover = mouseX >= leaveX && mouseX < leaveX + 85 && 
                            mouseY >= contentY && mouseY < contentY + buttonHeight;
        int leaveBg = leaveHover ? 0xFF6a3a3a : 0xFF5a3a3a;
        RenderUtils.drawRect(leaveX, contentY, leaveX + 85, contentY + buttonHeight, leaveBg);
        RenderUtils.drawBorder(leaveX, contentY, leaveX + 85, contentY + buttonHeight, 0xFFAA5555, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Leave Room", leaveX + 8, contentY + 7, 0xFFFFFF);
        
        contentY += buttonHeight + 20;
        
        // Draw content based on view mode
        if (voiceChatViewMode.equals("rooms")) {
            drawVoiceRoomsView(x, contentY, width, y + height - contentY - 20, mouseX, mouseY);
        } else if (voiceChatViewMode.equals("settings")) {
            drawVoiceSettingsView(x, contentY, width, y + height - contentY - 20, mouseX, mouseY);
        } else if (voiceChatViewMode.equals("create_room")) {
            drawVoiceCreateRoomView(x, contentY, width, y + height - contentY - 20, mouseX, mouseY);
        } else {
            drawVoiceMainView(x, contentY, width, y + height - contentY - 20, mouseX, mouseY);
        }
        
        // Status message at bottom
        if (!voiceStatusMessage.isEmpty() && System.currentTimeMillis() - voiceStatusMessageTime < 3000) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int msgWidth = this.fontRendererObj.getStringWidth(voiceStatusMessage);
            this.fontRendererObj.drawString(voiceStatusMessage, x + (width - msgWidth) / 2, y + height - 20, 0xFFFF55);
        }
    }
    
    private void drawVoiceMainView(int x, int y, int width, int height, int mouseX, int mouseY) {
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        int contentY = y;
        
        // Current room info
        VoiceRoom currentRoom = voiceManager.getCurrentRoom();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        if (currentRoom != null) {
            this.fontRendererObj.drawString("Current Room: " + currentRoom.getName(), x + 15, contentY, 0xFFFFFF);
            contentY += 15;
            
            if (currentRoom.isLinkedToParty()) {
                this.fontRendererObj.drawString("(Linked to Party)", x + 15, contentY, 0x88FF88);
                contentY += 15;
            }
            
            // Users in room
            contentY += 10;
            this.fontRendererObj.drawString("Users in Room:", x + 15, contentY, 0xAAAAAA);
            contentY += 18;
            
            Map<String, VoiceUser> usersInRoom = voiceManager.getUsersInRoom();
            if (usersInRoom != null) {
                for (VoiceUser user : usersInRoom.values()) {
                    // User entry background
                    int userBgColor = user.isTalking() ? 0xFF3a5a3a : 0xFF2a2a3a;
                    RenderUtils.drawRect(x + 15, contentY, x + width - 30, contentY + 25, userBgColor);
                    
                    // Talking indicator
                    if (user.isTalking()) {
                        RenderUtils.drawRect(x + 17, contentY + 2, x + 22, contentY + 23, 0xFF55FF55);
                    }
                    
                    // User name
                    net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    String displayName = user.getDisplayName();
                    if (user.getDiscordName() != null) {
                        displayName += " (" + user.getDiscordName() + ")";
                    }
                    this.fontRendererObj.drawString(displayName, x + 28, contentY + 8, 
                        user.isMuted() ? 0x888888 : 0xFFFFFF);
                    
                    // Mute/deafen icons
                    if (user.isMuted()) {
                        this.fontRendererObj.drawString("[M]", x + width - 60, contentY + 8, 0xFF5555);
                    }
                    if (user.isDeafened()) {
                        this.fontRendererObj.drawString("[D]", x + width - 45, contentY + 8, 0xFF5555);
                    }
                    
                    contentY += 28;
                    
                    // Limit displayed users
                    if (contentY > y + height - 60) break;
                }
            }
        } else {
            this.fontRendererObj.drawString("Not in a room", x + 15, contentY, 0x888888);
            contentY += 20;
            this.fontRendererObj.drawString("Click 'Browse Rooms' to join or create a room", x + 15, contentY, 0x666666);
        }
        
        // Mic level meter at bottom
        if (voiceManager.isConnected()) {
            int meterY = y + height - 30;
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Mic Level:", x + 15, meterY, 0xAAAAAA);
            
            // Meter background
            RenderUtils.drawRect(x + 80, meterY - 2, x + 280, meterY + 12, 0xFF1a1a2a);
            
            // Meter fill
            float level = voiceManager.getMicrophoneLevel();
            int fillWidth = (int) (200 * Math.min(1.0f, level * 5));
            
            int meterColor;
            if (voiceManager.isMuted()) {
                meterColor = 0xFF555555;
            } else if (voiceManager.isPushToTalkActive() || (!voiceManager.isUsePushToTalk() && level > 0.02f)) {
                meterColor = 0xFF55FF55;
            } else {
                meterColor = 0xFF55AA55;
            }
            RenderUtils.drawRect(x + 80, meterY - 2, x + 80 + fillWidth, meterY + 12, meterColor);
            
            // PTT indicator
            if (voiceManager.isUsePushToTalk()) {
                String pttText = voiceManager.isPushToTalkActive() ? "PTT Active" : "Press V to talk";
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString(pttText, x + 290, meterY, 
                    voiceManager.isPushToTalkActive() ? 0x55FF55 : 0x888888);
            }
        }
    }
    
    private void drawVoiceRoomsView(int x, int y, int width, int height, int mouseX, int mouseY) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Available Rooms", x + 15, y, 0xFFFFFF);
        
        int contentY = y + 20;
        
        // Create Room button
        int createBtnX = x + 15;
        boolean createHover = mouseX >= createBtnX && mouseX < createBtnX + 100 && 
                             mouseY >= contentY && mouseY < contentY + 22;
        RenderUtils.drawRect(createBtnX, contentY, createBtnX + 100, contentY + 22, createHover ? 0xFF4a6a4a : 0xFF3a5a3a);
        RenderUtils.drawBorder(createBtnX, contentY, createBtnX + 100, contentY + 22, 0xFF55AA55, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Create Room", createBtnX + 12, contentY + 7, 0xFFFFFF);
        
        // Refresh button
        int refreshBtnX = createBtnX + 110;
        boolean refreshHover = mouseX >= refreshBtnX && mouseX < refreshBtnX + 70 && 
                              mouseY >= contentY && mouseY < contentY + 22;
        RenderUtils.drawRect(refreshBtnX, contentY, refreshBtnX + 70, contentY + 22, refreshHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawBorder(refreshBtnX, contentY, refreshBtnX + 70, contentY + 22, 0xFF5865F2, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Refresh", refreshBtnX + 12, contentY + 7, 0xFFFFFF);
        
        // Back button
        int backBtnX = refreshBtnX + 80;
        boolean backHover = mouseX >= backBtnX && mouseX < backBtnX + 50 && 
                           mouseY >= contentY && mouseY < contentY + 22;
        RenderUtils.drawRect(backBtnX, contentY, backBtnX + 50, contentY + 22, backHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawBorder(backBtnX, contentY, backBtnX + 50, contentY + 22, 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Back", backBtnX + 12, contentY + 7, 0xFFFFFF);
        
        contentY += 35;
        
        // Room list - read directly from VoiceChatManager
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        java.util.List<VoiceRoom> availableRooms = voiceManager.getAvailableRooms();
        
        if (availableRooms.isEmpty()) {
            if (!voiceManager.isConnected()) {
                this.fontRendererObj.drawString("Connect to voice chat to browse rooms", x + 15, contentY + 20, 0x888888);
            } else {
                this.fontRendererObj.drawString("No rooms available", x + 15, contentY + 20, 0x888888);
                this.fontRendererObj.drawString("Create a new room or click Refresh", x + 15, contentY + 35, 0x666666);
            }
        } else {
            for (int i = voiceRoomListScroll; i < Math.min(voiceRoomListScroll + 6, availableRooms.size()); i++) {
                VoiceRoom room = availableRooms.get(i);
                
                boolean hover = mouseX >= x + 15 && mouseX < x + width - 30 &&
                               mouseY >= contentY && mouseY < contentY + 30;
                
                int bgColor = hover ? 0xFF3a3a4a : 0xFF2a2a3a;
                RenderUtils.drawRect(x + 15, contentY, x + width - 30, contentY + 30, bgColor);
                
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString(room.getName(), x + 25, contentY + 5, 0xFFFFFF);
                this.fontRendererObj.drawString(room.getUserCount() + " users", x + 25, contentY + 17, 0x888888);
                
                // Join button
                int joinBtnX = x + width - 80;
                boolean joinHover = hover && mouseX >= joinBtnX && mouseX < joinBtnX + 45;
                RenderUtils.drawRect(joinBtnX, contentY + 5, joinBtnX + 45, contentY + 25, joinHover ? 0xFF5865F2 : 0xFF4855E2);
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString("Join", joinBtnX + 12, contentY + 10, 0xFFFFFF);
                
                contentY += 35;
            }
        }
    }
    
    private void drawVoiceCreateRoomView(int x, int y, int width, int height, int mouseX, int mouseY) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Create New Room", x + 15, y, 0xFFFFFF);
        
        int contentY = y + 30;
        
        // Room name field
        this.fontRendererObj.drawString("Room Name:", x + 15, contentY + 5, 0xAAAAAA);
        voiceRoomNameField.xPosition = x + 100;
        voiceRoomNameField.yPosition = contentY;
        voiceRoomNameField.width = 200;
        voiceRoomNameField.drawTextBox();
        
        contentY += 35;
        
        // Create button
        int createBtnX = x + 100;
        boolean createHover = mouseX >= createBtnX && mouseX < createBtnX + 80 && 
                             mouseY >= contentY && mouseY < contentY + 22;
        RenderUtils.drawRect(createBtnX, contentY, createBtnX + 80, contentY + 22, createHover ? 0xFF55AA55 : 0xFF3a6a3a);
        RenderUtils.drawBorder(createBtnX, contentY, createBtnX + 80, contentY + 22, 0xFF77CC77, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Create", createBtnX + 20, contentY + 7, 0xFFFFFF);
        
        // Cancel button
        int cancelBtnX = createBtnX + 90;
        boolean cancelHover = mouseX >= cancelBtnX && mouseX < cancelBtnX + 70 && 
                             mouseY >= contentY && mouseY < contentY + 22;
        RenderUtils.drawRect(cancelBtnX, contentY, cancelBtnX + 70, contentY + 22, cancelHover ? 0xFF5a4a4a : 0xFF4a3a3a);
        RenderUtils.drawBorder(cancelBtnX, contentY, cancelBtnX + 70, contentY + 22, 0xFFAA5555, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Cancel", cancelBtnX + 15, contentY + 7, 0xFFFFFF);
    }
    
    private void drawVoiceSettingsView(int x, int y, int width, int height, int mouseX, int mouseY) {
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        int contentY = y;
        
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Voice Chat Settings", x + 15, contentY, 0xFFFFFF);
        contentY += 25;
        
        // API Key field
        this.fontRendererObj.drawString("API Key:", x + 15, contentY + 5, 0xAAAAAA);
        voiceApiKeyField.xPosition = x + 80;
        voiceApiKeyField.yPosition = contentY;
        voiceApiKeyField.drawTextBox();
        
        // Save button for API key
        int saveBtnX = x + 290;
        boolean saveHover = mouseX >= saveBtnX && mouseX < saveBtnX + 50 && 
                           mouseY >= contentY && mouseY < contentY + 18;
        RenderUtils.drawRect(saveBtnX, contentY, saveBtnX + 50, contentY + 18, saveHover ? 0xFF55AA55 : 0xFF3a6a3a);
        RenderUtils.drawBorder(saveBtnX, contentY, saveBtnX + 50, contentY + 18, 0xFF77CC77, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Save", saveBtnX + 13, contentY + 5, 0xFFFFFF);
        contentY += 25;
        
        // API Key status & Discord verified name
        String keyStatus = voiceManager.getAuthToken() != null && !voiceManager.getAuthToken().isEmpty() 
            ? "\\u00a7aKey set" : "\\u00a7cNo key set";
        this.fontRendererObj.drawString(keyStatus, x + 15, contentY, 0xFFFFFF);
        
        // Verified Discord name (if available)
        if (voiceManager.getVerifiedDiscordName() != null && !voiceManager.getVerifiedDiscordName().isEmpty()) {
            this.fontRendererObj.drawString("Verified: " + voiceManager.getVerifiedDiscordName(), x + 100, contentY, 0x55FF55);
        }
        contentY += 20;
        
        // Push to Talk toggle
        this.fontRendererObj.drawString("Push to Talk:", x + 15, contentY + 6, 0xAAAAAA);
        int pttBtnX = x + 120;
        boolean pttHover = mouseX >= pttBtnX && mouseX < pttBtnX + 60 && 
                          mouseY >= contentY && mouseY < contentY + 22;
        int pttBg = voiceManager.isUsePushToTalk() ? 0xFF55AA55 : (pttHover ? 0xFF4a5a6a : 0xFF3a4a5a);
        RenderUtils.drawRect(pttBtnX, contentY, pttBtnX + 60, contentY + 22, pttBg);
        RenderUtils.drawBorder(pttBtnX, contentY, pttBtnX + 60, contentY + 22, voiceManager.isUsePushToTalk() ? 0xFF77CC77 : 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        String pttText = voiceManager.isUsePushToTalk() ? "ON" : "OFF";
        int pttTextW = this.fontRendererObj.getStringWidth(pttText);
        this.fontRendererObj.drawString(pttText, pttBtnX + (60 - pttTextW) / 2, contentY + 7, 0xFFFFFF);
        contentY += 30;
        
        // PTT Key info
        this.fontRendererObj.drawString("PTT Key: V (hold to talk)", x + 15, contentY, 0x888888);
        contentY += 20;
        
        // Microphone Volume slider
        this.fontRendererObj.drawString("Mic Volume:", x + 15, contentY + 4, 0xAAAAAA);
        int micSliderX = x + 100;
        int micSliderWidth = 150;
        RenderUtils.drawRect(micSliderX, contentY, micSliderX + micSliderWidth, contentY + 15, 0xFF2a2a3a);
        int micFillWidth = (int)(micSliderWidth * voiceManager.getMicrophoneVolume());
        RenderUtils.drawRect(micSliderX, contentY, micSliderX + micFillWidth, contentY + 15, 0xFF5865F2);
        RenderUtils.drawBorder(micSliderX, contentY, micSliderX + micSliderWidth, contentY + 15, 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString((int)(voiceManager.getMicrophoneVolume() * 100) + "%", micSliderX + micSliderWidth + 10, contentY + 4, 0xFFFFFF);
        contentY += 22;
        
        // Output Volume slider  
        this.fontRendererObj.drawString("Output Vol:", x + 15, contentY + 4, 0xAAAAAA);
        int outSliderX = x + 100;
        RenderUtils.drawRect(outSliderX, contentY, outSliderX + micSliderWidth, contentY + 15, 0xFF2a2a3a);
        int outFillWidth = (int)(micSliderWidth * voiceManager.getOutputVolume());
        RenderUtils.drawRect(outSliderX, contentY, outSliderX + outFillWidth, contentY + 15, 0xFF55AA55);
        RenderUtils.drawBorder(outSliderX, contentY, outSliderX + micSliderWidth, contentY + 15, 0xFF666666, 1);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString((int)(voiceManager.getOutputVolume() * 100) + "%", outSliderX + micSliderWidth + 10, contentY + 4, 0xFFFFFF);
    }
    
    private void refreshVoiceRooms() {
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        if (!voiceManager.isConnected()) {
            voiceStatusMessage = "Connect first to browse rooms";
            voiceStatusMessageTime = System.currentTimeMillis();
            return;
        }
        
        voiceManager.requestRoomList();
    }
    
    private void handleVoiceChatClick(int mouseX, int mouseY) {
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        
        int x = PANEL_X + 10;
        int y = PANEL_Y + HEADER_HEIGHT + 10;
        int contentY = y + 55;
        int buttonHeight = 22;
        int buttonSpacing = 10;
        int buttonWidth = 90;
        
        // First row buttons
        // Connect/Disconnect button
        if (mouseX >= x + 15 && mouseX < x + 15 + buttonWidth && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            if (voiceManager.isConnected()) {
                voiceManager.disconnect();
                voiceStatusMessage = "Disconnected";
            } else {
                voiceManager.connect();
                voiceStatusMessage = "Connecting...";
            }
            voiceStatusMessageTime = System.currentTimeMillis();
            return;
        }
        
        // Browse Rooms button
        int browseX = x + 15 + buttonWidth + buttonSpacing;
        if (mouseX >= browseX && mouseX < browseX + 95 && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            if (!voiceChatViewMode.equals("rooms")) {
                voiceChatViewMode = "rooms";
                refreshVoiceRooms(); // Auto-refresh when entering rooms view
            } else {
                voiceChatViewMode = "main";
            }
            return;
        }
        
        // Settings button
        int settingsX = browseX + 95 + buttonSpacing;
        if (mouseX >= settingsX && mouseX < settingsX + 70 && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            voiceChatViewMode = voiceChatViewMode.equals("settings") ? "main" : "settings";
            return;
        }
        
        contentY += buttonHeight + 10;
        
        // Second row buttons
        // Mute button
        int muteX = x + 15;
        if (mouseX >= muteX && mouseX < muteX + 70 && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            voiceManager.setMuted(!voiceManager.isMuted());
            return;
        }
        
        // Deafen button
        int deafenX = muteX + 70 + buttonSpacing;
        if (mouseX >= deafenX && mouseX < deafenX + 70 && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            voiceManager.setDeafened(!voiceManager.isDeafened());
            return;
        }
        
        // Leave Room button
        int leaveX = deafenX + 70 + buttonSpacing;
        if (mouseX >= leaveX && mouseX < leaveX + 85 && 
            mouseY >= contentY && mouseY < contentY + buttonHeight) {
            voiceManager.leaveRoom();
            voiceStatusMessage = "Left room";
            voiceStatusMessageTime = System.currentTimeMillis();
            return;
        }
        
        contentY += buttonHeight + 20;
        
        // Handle view-specific clicks
        if (voiceChatViewMode.equals("settings")) {
            // API key field click
            if (voiceApiKeyField != null) {
                voiceApiKeyField.mouseClicked(mouseX, mouseY, 0);
            }
            
            // Settings view starts at contentY, matches drawVoiceSettingsView layout
            int settingsY = contentY + 25; // After title
            
            // Save API key button (at settingsY, next to API key field)
            int saveBtnX = x + 290;
            if (mouseX >= saveBtnX && mouseX < saveBtnX + 50 && 
                mouseY >= settingsY && mouseY < settingsY + 18) {
                String apiKey = voiceApiKeyField.getText().trim();
                voiceManager.setAuthToken(apiKey);
                ConfigManager.set("voiceApiKey", apiKey);
                ConfigManager.save();
                if (!apiKey.isEmpty()) {
                    voiceStatusMessage = "API key saved! Connecting...";
                    voiceStatusMessageTime = System.currentTimeMillis();
                    // Auto-connect with the new key
                    if (!voiceManager.isConnected()) {
                        voiceManager.connect();
                    }
                } else {
                    voiceStatusMessage = "API key cleared";
                    voiceStatusMessageTime = System.currentTimeMillis();
                }
                return;
            }
            
            // PTT toggle (settingsY + 25 for status line + 20 spacing = settingsY + 45)
            int pttBtnX = x + 120;
            int pttY = settingsY + 45;
            if (mouseX >= pttBtnX && mouseX < pttBtnX + 60 && 
                mouseY >= pttY && mouseY < pttY + 22) {
                voiceManager.setUsePushToTalk(!voiceManager.isUsePushToTalk());
                return;
            }
            
            // Mic volume slider (pttY + 30 for PTT button + 20 for PTT key info = pttY + 50)
            int micSliderX = x + 100;
            int micSliderWidth = 150;
            int micSliderY = pttY + 50;
            if (mouseX >= micSliderX && mouseX < micSliderX + micSliderWidth && 
                mouseY >= micSliderY && mouseY < micSliderY + 15) {
                float newVol = (float)(mouseX - micSliderX) / micSliderWidth;
                voiceManager.setMicrophoneVolume(Math.max(0, Math.min(1, newVol)));
                return;
            }
            
            // Output volume slider (micSliderY + 22)
            int outSliderY = micSliderY + 22;
            if (mouseX >= micSliderX && mouseX < micSliderX + micSliderWidth && 
                mouseY >= outSliderY && mouseY < outSliderY + 15) {
                float newVol = (float)(mouseX - micSliderX) / micSliderWidth;
                voiceManager.setOutputVolume(Math.max(0, Math.min(1, newVol)));
                return;
            }
        } else if (voiceChatViewMode.equals("rooms")) {
            // Create Room button
            int createY = contentY + 20;
            if (mouseX >= x + 15 && mouseX < x + 115 && 
                mouseY >= createY && mouseY < createY + 22) {
                voiceChatViewMode = "create_room";
                voiceRoomNameField.setText("");
                return;
            }
            
            // Refresh button
            if (mouseX >= x + 125 && mouseX < x + 195 && 
                mouseY >= createY && mouseY < createY + 22) {
                voiceStatusMessage = "Refreshing rooms...";
                voiceStatusMessageTime = System.currentTimeMillis();
                refreshVoiceRooms();
                return;
            }
            
            // Back button
            if (mouseX >= x + 205 && mouseX < x + 255 && 
                mouseY >= createY && mouseY < createY + 22) {
                voiceChatViewMode = "main";
                return;
            }
            
            // Room list items - check for Join clicks
            java.util.List<VoiceRoom> availableRooms = voiceManager.getAvailableRooms();
            int roomListY = createY + 35;
            int panelWidth = this.width - 100 - 20; // Match fullWidth calculation from drawScreen
            
            for (int i = voiceRoomListScroll; i < Math.min(voiceRoomListScroll + 6, availableRooms.size()); i++) {
                VoiceRoom room = availableRooms.get(i);
                
                // Check if click is on this room row
                if (mouseY >= roomListY && mouseY < roomListY + 30) {
                    // Join button area
                    int joinBtnX = x + panelWidth - 80;
                    if (mouseX >= joinBtnX && mouseX < joinBtnX + 45) {
                        voiceManager.joinRoom(room.getId());
                        voiceStatusMessage = "Joining room: " + room.getName();
                        voiceStatusMessageTime = System.currentTimeMillis();
                        voiceChatViewMode = "main";
                        return;
                    }
                }
                roomListY += 35;
            }
        } else if (voiceChatViewMode.equals("create_room")) {
            // Room name field click
            if (voiceRoomNameField != null) {
                voiceRoomNameField.mouseClicked(mouseX, mouseY, 0);
            }
            
            int createRoomY = contentY + 65;
            
            // Create button
            int createBtnX = x + 100;
            if (mouseX >= createBtnX && mouseX < createBtnX + 80 && 
                mouseY >= createRoomY && mouseY < createRoomY + 22) {
                String roomName = voiceRoomNameField.getText().trim();
                if (!roomName.isEmpty()) {
                    voiceManager.createRoom(roomName, false);
                    voiceStatusMessage = "Creating room: " + roomName;
                    voiceStatusMessageTime = System.currentTimeMillis();
                    voiceChatViewMode = "main";
                } else {
                    voiceStatusMessage = "Please enter a room name";
                    voiceStatusMessageTime = System.currentTimeMillis();
                }
                return;
            }
            
            // Cancel button
            int cancelBtnX = createBtnX + 90;
            if (mouseX >= cancelBtnX && mouseX < cancelBtnX + 70 && 
                mouseY >= createRoomY && mouseY < createRoomY + 22) {
                voiceChatViewMode = "rooms";
                return;
            }
        }
    }
    
    // ===================== BAZAAR DRAWING METHODS =====================
    
    private void drawBazaarContent(int x, int y, int width, int height, int mouseX, int mouseY) {
        // Main panel background
        RenderUtils.drawBox(x, y, x + width, y + height, 0xFF16213E, 0xFF333344, 1);
        
        // Title header
        RenderUtils.drawRect(x, y, x + width, y + 30, 0xFF55FF55);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Bazaar", x + 10, y + 10, 0xFF000000);
        
        // Search button
        int searchBtnX = x + width - 70;
        boolean searchBtnHover = mouseX >= searchBtnX && mouseX < searchBtnX + 60 && mouseY >= y + 5 && mouseY < y + 25;
        RenderUtils.drawRect(searchBtnX, y + 5, searchBtnX + 60, y + 25, searchBtnHover ? 0xFF4a4a5a : 0xFF3a3a4a);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Search", searchBtnX + 12, y + 10, 0xFFFFFF);
        
        int contentY = y + 35;
        int contentHeight = height - 35;
        
        // Left panel - Categories
        int catPanelWidth = bazaarCategoryWidth;
        RenderUtils.drawRect(x, contentY, x + catPanelWidth, y + height, 0xFF2D2D44);
        
        // Categories header
        RenderUtils.drawRect(x, contentY, x + catPanelWidth, contentY + 20, 0xFF3D3D5C);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Categories", x + 5, contentY + 6, 0xFFFFFF);
        
        // Draw categories
        int catY = contentY + 25;
        int catItemHeight = 22;
        for (BazaarCategory cat : bazaarCategories) {
            if (catY + catItemHeight > y + height - 5) break;
            
            boolean hovered = mouseX >= x && mouseX < x + catPanelWidth && mouseY >= catY && mouseY < catY + catItemHeight;
            boolean selected = cat.id.equals(selectedBazaarCategory);
            
            if (selected) {
                RenderUtils.drawRect(x + 2, catY, x + catPanelWidth - 2, catY + catItemHeight - 2, 0xFF4A4A6A);
            } else if (hovered) {
                RenderUtils.drawRect(x + 2, catY, x + catPanelWidth - 2, catY + catItemHeight - 2, 0xFF3A3A5A);
            }
            
            int textColor = cat.color != 0 ? cat.color : 0xFFFFFF;
            String catName = cat.name;
            if (this.fontRendererObj.getStringWidth(catName) > catPanelWidth - 35) {
                catName = this.fontRendererObj.trimStringToWidth(catName, catPanelWidth - 40) + "..";
            }
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(catName, x + 5, catY + 6, textColor);
            
            String countStr = "(" + cat.sectionCount + ")";
            this.fontRendererObj.drawString(countStr, x + catPanelWidth - this.fontRendererObj.getStringWidth(countStr) - 5, catY + 6, 0x888888);
            
            catY += catItemHeight;
        }
        
        // Middle panel - Sections (if category selected)
        if (selectedBazaarCategory != null && !bazaarShowSearch) {
            int secPanelX = x + catPanelWidth + 3;
            int secPanelWidth = bazaarSectionWidth;
            RenderUtils.drawRect(secPanelX, contentY, secPanelX + secPanelWidth, y + height, 0xFF2D2D44);
            
            // Sections header
            RenderUtils.drawRect(secPanelX, contentY, secPanelX + secPanelWidth, contentY + 20, 0xFF3D3D5C);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString("Sections", secPanelX + 5, contentY + 6, 0xFFFFFF);
            
            // Draw sections
            int secY = contentY + 25;
            int secItemHeight = 20;
            int visibleSections = (contentHeight - 30) / secItemHeight;
            bazaarMaxSectionScroll = Math.max(0, bazaarSections.size() - visibleSections);
            
            for (int i = bazaarSectionScrollOffset; i < bazaarSections.size() && secY < y + height - secItemHeight; i++) {
                BazaarSection section = bazaarSections.get(i);
                boolean hovered = mouseX >= secPanelX && mouseX < secPanelX + secPanelWidth && mouseY >= secY && mouseY < secY + secItemHeight;
                boolean selected = section.id.equals(selectedBazaarSection);
                
                if (selected) {
                    RenderUtils.drawRect(secPanelX + 2, secY, secPanelX + secPanelWidth - 2, secY + secItemHeight - 2, 0xFF4A4A6A);
                } else if (hovered) {
                    RenderUtils.drawRect(secPanelX + 2, secY, secPanelX + secPanelWidth - 2, secY + secItemHeight - 2, 0xFF3A3A5A);
                }
                
                String secName = section.name;
                if (this.fontRendererObj.getStringWidth(secName) > secPanelWidth - 10) {
                    secName = this.fontRendererObj.trimStringToWidth(secName, secPanelWidth - 15) + "..";
                }
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString(secName, secPanelX + 5, secY + 5, 0xFFFFFF);
                
                secY += secItemHeight;
            }
            
            // Scroll indicator for sections
            if (bazaarMaxSectionScroll > 0) {
                int scrollBarHeight = contentHeight - 30;
                int scrollThumbHeight = Math.max(20, scrollBarHeight * visibleSections / bazaarSections.size());
                int scrollThumbY = contentY + 25 + (int)((scrollBarHeight - scrollThumbHeight) * bazaarSectionScrollOffset / (float)bazaarMaxSectionScroll);
                RenderUtils.drawRect(secPanelX + secPanelWidth - 4, contentY + 25, secPanelX + secPanelWidth - 1, y + height, 0xFF1A1A2E);
                RenderUtils.drawRect(secPanelX + secPanelWidth - 4, scrollThumbY, secPanelX + secPanelWidth - 1, scrollThumbY + scrollThumbHeight, 0xFF5A5A7A);
            }
        }
        
        // Right panel - Items or Search Results
        int itemsPanelX = x + catPanelWidth + 3 + (selectedBazaarCategory != null && !bazaarShowSearch ? bazaarSectionWidth + 3 : 0);
        int itemsPanelWidth = width - catPanelWidth - 6 - (selectedBazaarCategory != null && !bazaarShowSearch ? bazaarSectionWidth + 3 : 0);
        
        if (bazaarShowSearch) {
            drawBazaarSearchResults(itemsPanelX, contentY, itemsPanelWidth, contentHeight, mouseX, mouseY);
        } else if (selectedBazaarSection != null) {
            drawBazaarItems(itemsPanelX, contentY, itemsPanelWidth, contentHeight, mouseX, mouseY);
        } else {
            // Show placeholder
            RenderUtils.drawRect(itemsPanelX, contentY, itemsPanelX + itemsPanelWidth, y + height, 0xFF2D2D44);
            String hint = selectedBazaarCategory == null ? "Select a category" : "Select a section";
            int hintWidth = this.fontRendererObj.getStringWidth(hint);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(hint, itemsPanelX + (itemsPanelWidth - hintWidth) / 2, contentY + contentHeight / 2, 0x666666);
        }
        
        // Status message
        if (bazaarIsLoading) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(bazaarStatusMessage, x + 10, y + height - 15, 0xFFFF55);
        }
        
        // Draw tooltip for hovered item
        if (hoveredBazaarItem != null) {
            drawBazaarItemTooltip(mouseX, mouseY);
        }
    }
    
    private void drawBazaarItems(int x, int y, int width, int height, int mouseX, int mouseY) {
        RenderUtils.drawRect(x, y, x + width, y + height, 0xFF2D2D44);
        
        // Header
        RenderUtils.drawRect(x, y, x + width, y + 20, 0xFF3D3D5C);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Item", x + 5, y + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("Buy", x + width - 130, y + 6, 0x55FF55);
        this.fontRendererObj.drawString("Sell", x + width - 65, y + 6, 0xFF5555);
        
        int itemY = y + 25;
        int itemHeight = 35;
        hoveredBazaarItem = null;
        
        int visibleItems = (height - 30) / itemHeight;
        bazaarMaxScroll = Math.max(0, bazaarItems.size() - visibleItems);
        
        for (int i = bazaarScrollOffset; i < bazaarItems.size() && itemY < y + height - itemHeight; i++) {
            BazaarItem item = bazaarItems.get(i);
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (hovered) {
                RenderUtils.drawRect(x + 2, itemY, x + width - 2, itemY + itemHeight - 2, 0xFF3A3A5A);
                hoveredBazaarItem = item;
                
                if (hoveredBazaarItemDetail == null || !hoveredBazaarItemDetail.productId.equals(item.productId)) {
                    loadBazaarItemDetail(item.productId);
                }
            }
            
            // Item name
            String name = item.displayName;
            if (this.fontRendererObj.getStringWidth(name) > width - 150) {
                name = this.fontRendererObj.trimStringToWidth(name, width - 155) + "...";
            }
            int nameColor = item.unavailable ? 0x888888 : 0xFFFFFF;
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(name, x + 5, itemY + 5, nameColor);
            
            // Prices
            String buyStr = item.unavailable ? "N/A" : formatBazaarPrice(item.buyPrice);
            String sellStr = item.unavailable ? "N/A" : formatBazaarPrice(item.sellPrice);
            this.fontRendererObj.drawString(buyStr, x + width - 130, itemY + 5, 0x55FF55);
            this.fontRendererObj.drawString(sellStr, x + width - 65, itemY + 5, 0xFF5555);
            
            // Volume info
            if (!item.unavailable) {
                String volInfo = "Vol: " + VOLUME_FORMAT.format(item.buyVolume) + " / " + VOLUME_FORMAT.format(item.sellVolume);
                this.fontRendererObj.drawString(volInfo, x + 5, itemY + 17, 0x888888);
            }
            
            // Separator
            RenderUtils.drawRect(x + 5, itemY + itemHeight - 3, x + width - 5, itemY + itemHeight - 2, 0xFF3D3D5C);
            
            itemY += itemHeight;
        }
        
        // Scroll indicator
        if (bazaarMaxScroll > 0) {
            int scrollBarHeight = height - 30;
            int scrollThumbHeight = Math.max(20, scrollBarHeight * visibleItems / bazaarItems.size());
            int scrollThumbY = y + 25 + (int)((scrollBarHeight - scrollThumbHeight) * bazaarScrollOffset / (float)bazaarMaxScroll);
            RenderUtils.drawRect(x + width - 4, y + 25, x + width - 1, y + height, 0xFF1A1A2E);
            RenderUtils.drawRect(x + width - 4, scrollThumbY, x + width - 1, scrollThumbY + scrollThumbHeight, 0xFF5A5A7A);
        }
    }
    
    private void drawBazaarSearchResults(int x, int y, int width, int height, int mouseX, int mouseY) {
        RenderUtils.drawRect(x, y, x + width, y + height, 0xFF2D2D44);
        
        // Draw search field
        bazaarSearchField.xPosition = x + 5;
        bazaarSearchField.yPosition = y + 5;
        bazaarSearchField.width = width - 80;
        bazaarSearchField.drawTextBox();
        
        // Search button
        int searchBtnX = x + width - 70;
        boolean searchHover = mouseX >= searchBtnX && mouseX < searchBtnX + 60 && mouseY >= y + 3 && mouseY < y + 21;
        RenderUtils.drawRect(searchBtnX, y + 3, searchBtnX + 60, y + 21, searchHover ? 0xFF55AA55 : 0xFF449944);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Search", searchBtnX + 12, y + 7, 0xFFFFFF);
        
        // Back button
        int backX = x + width - 70;
        boolean backHover = mouseX >= backX - 45 && mouseX < backX - 5 && mouseY >= y + 3 && mouseY < y + 21;
        RenderUtils.drawRect(backX - 45, y + 3, backX - 5, y + 21, backHover ? 0xFF5a5a6a : 0xFF4a4a5a);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Back", backX - 35, y + 7, 0xFFFFFF);
        
        // Results header
        int resultsY = y + 28;
        RenderUtils.drawRect(x, resultsY, x + width, resultsY + 20, 0xFF3D3D5C);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Results (" + bazaarSearchResults.size() + ")", x + 5, resultsY + 6, 0xFFFFFF);
        
        int itemY = resultsY + 25;
        int itemHeight = 35;
        hoveredBazaarItem = null;
        
        if (bazaarSearchResults.isEmpty()) {
            this.fontRendererObj.drawString("No results found", x + 10, itemY + 20, 0x888888);
            return;
        }
        
        int visibleItems = (height - 55) / itemHeight;
        bazaarMaxScroll = Math.max(0, bazaarSearchResults.size() - visibleItems);
        
        for (int i = bazaarScrollOffset; i < bazaarSearchResults.size() && itemY < y + height - itemHeight; i++) {
            BazaarItem item = bazaarSearchResults.get(i);
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (hovered) {
                RenderUtils.drawRect(x + 2, itemY, x + width - 2, itemY + itemHeight - 2, 0xFF3A3A5A);
                hoveredBazaarItem = item;
                
                if (hoveredBazaarItemDetail == null || !hoveredBazaarItemDetail.productId.equals(item.productId)) {
                    loadBazaarItemDetail(item.productId);
                }
            }
            
            // Item name
            String name = item.displayName;
            if (this.fontRendererObj.getStringWidth(name) > width - 170) {
                name = this.fontRendererObj.trimStringToWidth(name, width - 175) + "...";
            }
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(name, x + 5, itemY + 3, 0xFFFFFF);
            
            // Category path
            String path = item.category + " > " + item.section;
            if (this.fontRendererObj.getStringWidth(path) > width - 20) {
                path = this.fontRendererObj.trimStringToWidth(path, width - 25) + "...";
            }
            this.fontRendererObj.drawString(path, x + 5, itemY + 14, 0x666666);
            
            // Prices
            this.fontRendererObj.drawString(formatBazaarPrice(item.buyPrice), x + width - 130, itemY + 8, 0x55FF55);
            this.fontRendererObj.drawString(formatBazaarPrice(item.sellPrice), x + width - 65, itemY + 8, 0xFF5555);
            
            itemY += itemHeight;
        }
    }
    
    private void drawBazaarItemTooltip(int mouseX, int mouseY) {
        if (hoveredBazaarItem == null) return;
        
        List<String> lines = new ArrayList<>();
        
        lines.add("\u00A76\u00A7l" + hoveredBazaarItem.displayName);
        lines.add("");
        lines.add("\u00A7aBuy Price: \u00A7f" + formatBazaarPrice(hoveredBazaarItem.buyPrice));
        lines.add("\u00A7cSell Price: \u00A7f" + formatBazaarPrice(hoveredBazaarItem.sellPrice));
        lines.add("");
        lines.add("\u00A77Buy Volume: \u00A7f" + VOLUME_FORMAT.format(hoveredBazaarItem.buyVolume));
        lines.add("\u00A77Sell Volume: \u00A7f" + VOLUME_FORMAT.format(hoveredBazaarItem.sellVolume));
        
        if (hoveredBazaarItemDetail != null && hoveredBazaarItemDetail.productId.equals(hoveredBazaarItem.productId)) {
            lines.add("");
            lines.add("\u00A7ePrice History:");
            
            if (hoveredBazaarItemDetail.buyPrice1h != null) {
                double buyChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.buyPrice1h, hoveredBazaarItem.buyPrice);
                double sellChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.sellPrice1h, hoveredBazaarItem.sellPrice);
                lines.add("\u00A77  1h: " + formatBazaarChangeString(buyChange) + "\u00A77 / " + formatBazaarChangeString(sellChange));
            }
            
            if (hoveredBazaarItemDetail.buyPrice1d != null) {
                double buyChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.buyPrice1d, hoveredBazaarItem.buyPrice);
                double sellChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.sellPrice1d, hoveredBazaarItem.sellPrice);
                lines.add("\u00A77  1d: " + formatBazaarChangeString(buyChange) + "\u00A77 / " + formatBazaarChangeString(sellChange));
            }
            
            if (hoveredBazaarItemDetail.buyPrice7d != null) {
                double buyChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.buyPrice7d, hoveredBazaarItem.buyPrice);
                double sellChange = calculateBazaarPercentChange(hoveredBazaarItemDetail.sellPrice7d, hoveredBazaarItem.sellPrice);
                lines.add("\u00A77  7d: " + formatBazaarChangeString(buyChange) + "\u00A77 / " + formatBazaarChangeString(sellChange));
            }
            
            lines.add("");
            lines.add("\u00A77Buy Orders: \u00A7f" + VOLUME_FORMAT.format(hoveredBazaarItemDetail.buyOrders));
            lines.add("\u00A77Sell Orders: \u00A7f" + VOLUME_FORMAT.format(hoveredBazaarItemDetail.sellOrders));
        }
        
        // Draw tooltip
        drawHoveringText(lines, mouseX, mouseY);
    }
    
    private String formatBazaarPrice(double price) {
        if (price >= 1000000000) {
            return PRICE_FORMAT.format(price / 1000000000) + "B";
        } else if (price >= 1000000) {
            return PRICE_FORMAT.format(price / 1000000) + "M";
        } else if (price >= 1000) {
            return PRICE_FORMAT.format(price / 1000) + "K";
        }
        return PRICE_FORMAT.format(price);
    }
    
    private double calculateBazaarPercentChange(double oldPrice, double newPrice) {
        if (oldPrice == 0) return 0;
        return ((newPrice - oldPrice) / oldPrice) * 100;
    }
    
    private String formatBazaarChangeString(double percentChange) {
        DecimalFormat df = new DecimalFormat("+0.0;-0.0");
        String formatted = df.format(percentChange) + "%";
        if (percentChange > 0) {
            return "\u00A7a" + formatted;
        } else if (percentChange < 0) {
            return "\u00A7c" + formatted;
        }
        return "\u00A77" + formatted;
    }
    
    // ===================== BAZAAR API METHODS =====================
    
    private void loadBazaarCategories() {
        bazaarIsLoading = true;
        bazaarStatusMessage = "Loading categories...";
        
        bazaarExecutor.submit(() -> {
            try {
                String response = bazaarHttpGet(BAZAAR_API_URL + "/bazaar/categories");
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    bazaarCategories.clear();
                    JsonArray catArray = json.getAsJsonArray("categories");
                    
                    for (JsonElement elem : catArray) {
                        JsonObject catObj = elem.getAsJsonObject();
                        BazaarCategory cat = new BazaarCategory();
                        cat.id = catObj.get("id").getAsString();
                        cat.name = catObj.get("name").getAsString();
                        cat.color = catObj.get("color").getAsInt();
                        cat.sectionCount = catObj.get("sectionCount").getAsInt();
                        bazaarCategories.add(cat);
                    }
                    
                    bazaarIsLoading = false;
                    bazaarDataLoaded = true;
                }
            } catch (Exception e) {
                bazaarStatusMessage = "Error: " + e.getMessage();
                System.err.println("[Bazaar] Error loading categories: " + e.getMessage());
            }
        });
    }
    
    private void loadBazaarSections(String categoryId) {
        bazaarIsLoading = true;
        bazaarStatusMessage = "Loading sections...";
        
        bazaarExecutor.submit(() -> {
            try {
                String response = bazaarHttpGet(BAZAAR_API_URL + "/bazaar/sections?category=" + URLEncoder.encode(categoryId, "UTF-8"));
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    bazaarSections.clear();
                    JsonArray sectionArray = json.getAsJsonArray("sections");
                    
                    for (JsonElement elem : sectionArray) {
                        JsonObject secObj = elem.getAsJsonObject();
                        BazaarSection section = new BazaarSection();
                        section.id = secObj.get("id").getAsString();
                        section.name = secObj.get("name").getAsString();
                        section.itemCount = secObj.get("itemCount").getAsInt();
                        bazaarSections.add(section);
                    }
                    
                    bazaarIsLoading = false;
                }
            } catch (Exception e) {
                bazaarStatusMessage = "Error: " + e.getMessage();
                System.err.println("[Bazaar] Error loading sections: " + e.getMessage());
            }
        });
    }
    
    private void loadBazaarItems(String categoryId, String sectionId) {
        bazaarIsLoading = true;
        bazaarStatusMessage = "Loading items...";
        
        bazaarExecutor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/items?category=" + URLEncoder.encode(categoryId, "UTF-8") 
                           + "&section=" + URLEncoder.encode(sectionId, "UTF-8");
                String response = bazaarHttpGet(url);
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    bazaarItems.clear();
                    JsonArray itemArray = json.getAsJsonArray("items");
                    
                    for (JsonElement elem : itemArray) {
                        JsonObject itemObj = elem.getAsJsonObject();
                        BazaarItem item = new BazaarItem();
                        item.productId = itemObj.get("productId").getAsString();
                        item.displayName = itemObj.has("displayName") ? itemObj.get("displayName").getAsString() : item.productId;
                        item.buyPrice = itemObj.get("buyPrice").getAsDouble();
                        item.sellPrice = itemObj.get("sellPrice").getAsDouble();
                        item.buyVolume = itemObj.get("buyVolume").getAsLong();
                        item.sellVolume = itemObj.get("sellVolume").getAsLong();
                        item.unavailable = itemObj.has("unavailable") && itemObj.get("unavailable").getAsBoolean();
                        bazaarItems.add(item);
                    }
                    
                    bazaarIsLoading = false;
                }
            } catch (Exception e) {
                bazaarStatusMessage = "Error: " + e.getMessage();
                System.err.println("[Bazaar] Error loading items: " + e.getMessage());
            }
        });
    }
    
    private void loadBazaarItemDetail(String productId) {
        bazaarExecutor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/item?id=" + URLEncoder.encode(productId, "UTF-8");
                String response = bazaarHttpGet(url);
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
                    
                    if (history.has("oneHour") && !history.get("oneHour").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("oneHour");
                        detail.buyPrice1h = h.get("buyPrice").getAsDouble();
                        detail.sellPrice1h = h.get("sellPrice").getAsDouble();
                    }
                    if (history.has("oneDay") && !history.get("oneDay").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("oneDay");
                        detail.buyPrice1d = h.get("buyPrice").getAsDouble();
                        detail.sellPrice1d = h.get("sellPrice").getAsDouble();
                    }
                    if (history.has("sevenDays") && !history.get("sevenDays").isJsonNull()) {
                        JsonObject h = history.getAsJsonObject("sevenDays");
                        detail.buyPrice7d = h.get("buyPrice").getAsDouble();
                        detail.sellPrice7d = h.get("sellPrice").getAsDouble();
                    }
                    
                    hoveredBazaarItemDetail = detail;
                }
            } catch (Exception e) {
                System.err.println("[Bazaar] Error loading item detail: " + e.getMessage());
            }
        });
    }
    
    private void searchBazaarItems(String query) {
        bazaarIsLoading = true;
        bazaarStatusMessage = "Searching...";
        
        bazaarExecutor.submit(() -> {
            try {
                String url = BAZAAR_API_URL + "/bazaar/search?q=" + URLEncoder.encode(query, "UTF-8");
                String response = bazaarHttpGet(url);
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                
                if (json.get("success").getAsBoolean()) {
                    bazaarSearchResults.clear();
                    JsonArray resultArray = json.getAsJsonArray("results");
                    
                    for (JsonElement elem : resultArray) {
                        JsonObject itemObj = elem.getAsJsonObject();
                        BazaarItem item = new BazaarItem();
                        item.productId = itemObj.get("productId").getAsString();
                        item.displayName = itemObj.has("displayName") ? itemObj.get("displayName").getAsString() : item.productId;
                        item.buyPrice = itemObj.get("buyPrice").getAsDouble();
                        item.sellPrice = itemObj.get("sellPrice").getAsDouble();
                        item.category = itemObj.get("category").getAsString();
                        item.section = itemObj.get("section").getAsString();
                        bazaarSearchResults.add(item);
                    }
                    
                    bazaarIsLoading = false;
                }
            } catch (Exception e) {
                bazaarStatusMessage = "Error: " + e.getMessage();
                System.err.println("[Bazaar] Error searching: " + e.getMessage());
            }
        });
    }
    
    private String bazaarHttpGet(String urlStr) throws java.io.IOException {
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
    
    // ===================== END BAZAAR METHODS =====================
    
    private void drawMessageList(int x, int y, int width, int height, 
                                 List<Message> messages, int mouseX, int mouseY) {
        RenderUtils.drawBox(x, y, x + width, y + height, 0xFF16213E, 0xFF333344, 1);
        
        int itemY = y + 5;
        int maxTextWidth = width - 25; // Leave space for padding and unread indicator
        
        for (Message msg : messages) {
            if (itemY >= y + height - 5) break;
            
            int itemHeight = 55;
            boolean hover = mouseX >= x + 3 && mouseX < x + width - 3 &&
                           mouseY >= itemY && mouseY < itemY + itemHeight;
            
            int bgColor = hover ? 0xFF2a2a3a : 0xFF1f1f2a;
            if (selectedMessage == msg) {
                bgColor = 0xFF3a3a4a;
            }
            
            RenderUtils.drawRect(x + 3, itemY, x + width - 3, itemY + itemHeight, bgColor);
            RenderUtils.drawRect(x + 4, itemY + 2, x + 7, itemY + itemHeight - 2, msg.type.color);
            
            // Draw title with truncation if needed
            String title = msg.title;
            if (this.fontRendererObj.getStringWidth(title) > maxTextWidth) {
                while (this.fontRendererObj.getStringWidth(title + "...") > maxTextWidth && title.length() > 0) {
                    title = title.substring(0, title.length() - 1);
                }
                title = title + "...";
            }
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.fontRendererObj.drawString(title, x + 12, itemY + 8, msg.read ? 0xAAAAAA : 0xFFFFFF);
            
            // Draw preview with text wrapping (2 lines max)
            String preview = msg.getPreview();
            int previewY = itemY + 22;
            int linesDrawn = 0;
            int maxLines = 2;
            
            String[] words = preview.split(" ");
            String currentLine = "";
            for (String word : words) {
                if (linesDrawn >= maxLines) break;
                
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (this.fontRendererObj.getStringWidth(testLine) > maxTextWidth) {
                    // Draw current line
                    if (!currentLine.isEmpty()) {
                        this.fontRendererObj.drawString(currentLine, x + 12, previewY, 0x888888);
                        previewY += 10;
                        linesDrawn++;
                    }
                    currentLine = word;
                } else {
                    currentLine = testLine;
                }
            }
            // Draw remaining text
            if (linesDrawn < maxLines && !currentLine.isEmpty()) {
                if (this.fontRendererObj.getStringWidth(currentLine) > maxTextWidth) {
                    while (this.fontRendererObj.getStringWidth(currentLine + "...") > maxTextWidth && currentLine.length() > 0) {
                        currentLine = currentLine.substring(0, currentLine.length() - 1);
                    }
                    currentLine = currentLine + "...";
                }
                this.fontRendererObj.drawString(currentLine, x + 12, previewY, 0x888888);
            }
            
            if (!msg.read) {
                RenderUtils.drawRect(x + width - 12, itemY + (itemHeight - 8) / 2,
                        x + width - 6, itemY + (itemHeight - 8) / 2 + 8, 0xFFFF6B6B);
            }
            
            itemY += itemHeight + 5;
        }
    }
    
    private void drawMessageContent(int x, int y, int width, int height, Message msg) {
        RenderUtils.drawBox(x, y, x + width, y + height, 0xFF16213E, 0xFF333344, 1);
        MessageManager.markAsRead(msg.id);
        
        // Colored header bar with type label and title
        RenderUtils.drawRect(x, y, x + width, y + 45, msg.type.color);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString(msg.type.label, x + 10, y + 8, 0xFF000000); // Dark text for label
        
        // Draw title in the header bar (top left, below the type label)
        String displayTitle = msg.title;
        int maxTitleWidth = width - 20;
        if (this.fontRendererObj.getStringWidth(displayTitle) > maxTitleWidth) {
            while (this.fontRendererObj.getStringWidth(displayTitle + "...") > maxTitleWidth && displayTitle.length() > 0) {
                displayTitle = displayTitle.substring(0, displayTitle.length() - 1);
            }
            displayTitle = displayTitle + "...";
        }
        this.fontRendererObj.drawString(displayTitle, x + 10, y + 25, 0xFFFFFFFF); // White title in header
        
        // Content starts right after the header bar
        int contentY = y + 55;
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (String line : msg.content.split("\n")) {
            if (contentY > y + height - 30) break;
            
            if (line.length() > 60) {
                String[] words = line.split(" ");
                String currentLine = "";
                for (String word : words) {
                    if (this.fontRendererObj.getStringWidth(currentLine + word) > width - 20) {
                        this.fontRendererObj.drawString(currentLine, x + 10, contentY, 0xCCCCCC);
                        currentLine = word + " ";
                        contentY += 12;
                    } else {
                        currentLine += word + " ";
                    }
                }
                if (!currentLine.isEmpty()) {
                    this.fontRendererObj.drawString(currentLine, x + 10, contentY, 0xCCCCCC);
                    contentY += 12;
                }
            } else {
                this.fontRendererObj.drawString(line, x + 10, contentY, 0xCCCCCC);
                contentY += 12;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle text field click
        if (partyNoteField != null && currentTab == Message.MessageType.PARTY_FINDER) {
            partyNoteField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        
        int panelWidth = this.width - 100;
        int panelHeight = this.height - 80;
        int listX = PANEL_X + 10;
        int listY = PANEL_Y + HEADER_HEIGHT + 10;
        int listHeight = panelHeight - HEADER_HEIGHT - 20;
        
        // Handle Party Finder clicks
        if (currentTab == Message.MessageType.PARTY_FINDER) {
            int leftPanelWidth = CATEGORY_PANEL_WIDTH;
            int rightX = listX + leftPanelWidth + 10;
            int rightWidth = panelWidth - 20 - leftPanelWidth - 10;
            
            // LEFT PANEL - Category clicks
            
            // Back button
            if (currentPartyFinderCategory != null) {
                int backY = listY + 40;
                if (mouseX >= listX + 5 && mouseX < listX + leftPanelWidth - 5 &&
                    mouseY >= backY && mouseY < backY + 20) {
                    if (currentPartyFinderCategory.parent != null) {
                        currentPartyFinderCategory = currentPartyFinderCategory.parent;
                        currentCategoryList = currentPartyFinderCategory.children;
                    } else {
                        currentPartyFinderCategory = null;
                        currentCategoryList = partyFinderRootCategories;
                    }
                    return;
                }
            }
            
            // Category list clicks
            int catY = listY + (currentPartyFinderCategory != null ? 65 : 45);
            int itemHeight = 30;
            
            for (PartyFinderCategory category : currentCategoryList) {
                if (catY + itemHeight > listY + listHeight - 5) break;
                
                if (mouseX >= listX + 5 && mouseX < listX + leftPanelWidth - 5 &&
                    mouseY >= catY && mouseY < catY + itemHeight) {
                    
                    if (category.hasChildren()) {
                        // Navigate into subcategory
                        currentPartyFinderCategory = category;
                        currentCategoryList = category.children;
                    } else {
                        // Leaf category - select it for party creation/browsing
                        selectedPartyFinderCategory = category;
                    }
                    return;
                }
                
                catY += itemHeight + 3;
            }
            
            // RIGHT PANEL - Mode toggle and form clicks
            
            // Filters/Browse/Create toggle buttons
            int filtersX = rightX + rightWidth - 225;
            int browseX = rightX + rightWidth - 150;
            int createX = rightX + rightWidth - 75;
            int btnY = listY + 20;
            
            if (mouseX >= filtersX && mouseX < filtersX + 70 && mouseY >= btnY && mouseY < btnY + 20) {
                showFilterPanel = !showFilterPanel;
                return;
            }
            if (mouseX >= browseX && mouseX < browseX + 70 && mouseY >= btnY && mouseY < btnY + 20) {
                partyFinderMode = "browse";
                showFilterPanel = false;
                return;
            }
            if (mouseX >= createX && mouseX < createX + 70 && mouseY >= btnY && mouseY < btnY + 20) {
                partyFinderMode = "create";
                showFilterPanel = false;
                return;
            }
            
            // Create mode - form interactions
            if (partyFinderMode.equals("create") && selectedPartyFinderCategory != null) {
                int formY = listY + 55;
                
                // Min level buttons
                int lvlBtnY = formY + 100;
                if (mouseX >= rightX + 100 && mouseX < rightX + 120 && mouseY >= lvlBtnY && mouseY < lvlBtnY + 20) {
                    partyMinLevel = Math.max(0, partyMinLevel - 1);
                    return;
                }
                if (mouseX >= rightX + 125 && mouseX < rightX + 145 && mouseY >= lvlBtnY && mouseY < lvlBtnY + 20) {
                    partyMinLevel = Math.min(50, partyMinLevel + 1);
                    return;
                }
                
                // Max players buttons
                int plrBtnY = formY + 130;
                if (mouseX >= rightX + 110 && mouseX < rightX + 130 && mouseY >= plrBtnY && mouseY < plrBtnY + 20) {
                    partyMaxPlayers = Math.max(2, partyMaxPlayers - 1);
                    return;
                }
                if (mouseX >= rightX + 135 && mouseX < rightX + 155 && mouseY >= plrBtnY && mouseY < plrBtnY + 20) {
                    partyMaxPlayers = Math.min(10, partyMaxPlayers + 1);
                    return;
                }
                
                // Create Party button
                int createBtnY = formY + 175;
                if (mouseX >= rightX + 10 && mouseX < rightX + 150 && mouseY >= createBtnY && mouseY < createBtnY + 30) {
                    createParty();
                    return;
                }
            }
            
            // Browse mode - party list interactions
            if (partyFinderMode.equals("browse") && selectedPartyFinderCategory != null && !showFilterPanel) {
                int contentY = listY + 55;
                int contentHeight = listHeight - 55;
                
                // Refresh button
                int refreshX = rightX + rightWidth - 80;
                int refreshY = contentY + 5;
                if (mouseX >= refreshX && mouseX < refreshX + 70 && mouseY >= refreshY && mouseY < refreshY + 18) {
                    // Force refresh
                    lastPartyRefresh = 0;
                    partyList.clear();
                    partiesLoading = true;
                    partiesError = null;
                    
                    PartyFinderAPI.getParties(selectedPartyFinderCategory, new PartyFinderAPI.PartiesCallback() {
                        @Override
                        public void onSuccess(JsonObject response) {
                            partyList.clear();
                            if (response.has("parties")) {
                                JsonArray parties = response.getAsJsonArray("parties");
                                for (int i = 0; i < parties.size(); i++) {
                                    partyList.add(parties.get(i).getAsJsonObject());
                                }
                            }
                            partiesLoading = false;
                            lastPartyRefresh = System.currentTimeMillis();
                        }
                        
                        @Override
                        public void onError(String error) {
                            partiesError = error;
                            partiesLoading = false;
                        }
                    });
                    return;
                }
                
                // Party card clicks (Join buttons)
                if (!partyList.isEmpty()) {
                    int cardY = contentY + 30;
                    int cardHeight = 70;
                    int cardSpacing = 5;
                    
                    for (int i = 0; i < partyList.size(); i++) {
                        if (cardY + cardHeight > contentY + contentHeight - 10) break;
                        
                        JsonObject party = partyList.get(i);
                        String partyId = party.has("id") ? party.get("id").getAsString() : "";
                        
                        // Join button position (same as in drawPartyBrowseContent)
                        int joinBtnX = rightX + rightWidth - 70;
                        int joinBtnY = cardY + 25;
                        
                        if (mouseX >= joinBtnX && mouseX < joinBtnX + 55 && 
                            mouseY >= joinBtnY && mouseY < joinBtnY + 20) {
                            // Join this party
                            joinParty(partyId, party);
                            return;
                        }
                        
                        cardY += cardHeight + cardSpacing;
                    }
                }
            }
            
            // Filter panel interactions
            if (showFilterPanel) {
                int contentY = listY + 55;
                int contentHeight = listHeight - 55;
                
                // Build available filters list (same logic as drawFilterPanel - category-specific + global 3 only)
                List<String> availableFilters = new ArrayList<>();
                if (selectedPartyFinderCategory != null) {
                    if (selectedPartyFinderCategory.primaryFilter != null) {
                        availableFilters.add(selectedPartyFinderCategory.primaryFilter);
                    }
                    availableFilters.addAll(selectedPartyFinderCategory.secondaryFilters);
                }
                String[] commonFilters = {"Combat Level", "SkyBlock Level", "Magical Power"};
                for (String common : commonFilters) {
                    if (!availableFilters.contains(common)) {
                        availableFilters.add(common);
                    }
                }
                
                // Handle filter button clicks
                int filterY = contentY + 35;
                int filterRowHeight = 28;
                
                for (String filterName : availableFilters) {
                    if (filterY + filterRowHeight > contentY + contentHeight - 50) break;
                    
                    int currentValue = activeFilters.getOrDefault(filterName, 0);
                    int controlsX = rightX + rightWidth - 130;
                    
                    // Minus button (text)
                    if (mouseX >= controlsX && mouseX < controlsX + 15 && 
                        mouseY >= filterY + 2 && mouseY < filterY + 22) {
                        // Commit any current edit first
                        commitFilterEdit();
                        int newVal = Math.max(0, currentValue - 1);
                        if (newVal == 0) {
                            activeFilters.remove(filterName);
                        } else {
                            activeFilters.put(filterName, newVal);
                        }
                        return;
                    }
                    
                    // Value field click - start editing
                    int valueFieldX = controlsX + 20;
                    int valueFieldWidth = 45;
                    if (mouseX >= valueFieldX && mouseX < valueFieldX + valueFieldWidth && 
                        mouseY >= filterY + 2 && mouseY < filterY + 22) {
                        // Commit previous edit if any
                        commitFilterEdit();
                        // Start editing this filter
                        editingFilterName = filterName;
                        editingFilterValue = String.valueOf(currentValue);
                        return;
                    }
                    
                    // Plus button (text)
                    int plusX = valueFieldX + valueFieldWidth + 5;
                    if (mouseX >= plusX && mouseX < plusX + 15 && 
                        mouseY >= filterY + 2 && mouseY < filterY + 22) {
                        // Commit any current edit first
                        commitFilterEdit();
                        activeFilters.put(filterName, currentValue + 1);
                        return;
                    }
                    
                    // Clear button (X)
                    if (activeFilters.containsKey(filterName)) {
                        int clearBtnX = rightX + rightWidth - 45;
                        if (mouseX >= clearBtnX && mouseX < clearBtnX + 20 && 
                            mouseY >= filterY + 2 && mouseY < filterY + 22) {
                            commitFilterEdit();
                            activeFilters.remove(filterName);
                            if (filterName.equals(editingFilterName)) {
                                editingFilterName = null;
                                editingFilterValue = "";
                            }
                            return;
                        }
                    }
                    
                    filterY += filterRowHeight;
                }
                
                // Clicking elsewhere commits current edit
                commitFilterEdit();
                
                // Clear All Filters button
                int clearAllY = contentY + contentHeight - 40;
                if (mouseX >= rightX + 10 && mouseX < rightX + 130 && 
                    mouseY >= clearAllY && mouseY < clearAllY + 25) {
                    activeFilters.clear();
                    editingFilterName = null;
                    editingFilterValue = "";
                    return;
                }
                
                // Apply button - closes filter panel and goes to browse
                int applyY = contentY + contentHeight - 40;
                if (mouseX >= rightX + rightWidth - 100 && mouseX < rightX + rightWidth - 20 && 
                    mouseY >= applyY && mouseY < applyY + 25) {
                    commitFilterEdit();
                    showFilterPanel = false;
                    partyFinderMode = "browse";
                    return;
                }
            }
            
            return;
        }
        
        // Handle Voice Chat clicks
        if (currentTab == Message.MessageType.VOICE_CHAT) {
            handleVoiceChatClick(mouseX, mouseY);
            return;
        }
        
        // Handle Bazaar clicks
        if (currentTab == Message.MessageType.BAZAAR) {
            // Handle bazaar search field click
            if (bazaarSearchField != null && bazaarShowSearch) {
                bazaarSearchField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            
            int contentY = listY + 35;
            int contentHeight = listHeight - 35;
            
            // Search button in header
            int searchBtnX = listX + (this.width - 100 - 20) - 70;
            if (mouseX >= searchBtnX && mouseX < searchBtnX + 60 && mouseY >= listY + 5 && mouseY < listY + 25) {
                bazaarShowSearch = true;
                bazaarScrollOffset = 0;
                if (bazaarSearchField != null) {
                    bazaarSearchField.setFocused(true);
                }
                return;
            }
            
            // Category clicks
            int catPanelWidth = bazaarCategoryWidth;
            int catY = contentY + 25;
            int catItemHeight = 22;
            
            for (BazaarCategory cat : bazaarCategories) {
                if (catY + catItemHeight > listY + listHeight - 5) break;
                
                if (mouseX >= listX && mouseX < listX + catPanelWidth && mouseY >= catY && mouseY < catY + catItemHeight) {
                    if (!cat.id.equals(selectedBazaarCategory)) {
                        selectedBazaarCategory = cat.id;
                        selectedBazaarSection = null;
                        bazaarSections.clear();
                        bazaarItems.clear();
                        bazaarScrollOffset = 0;
                        bazaarSectionScrollOffset = 0;
                        bazaarShowSearch = false;
                        loadBazaarSections(cat.id);
                    }
                    return;
                }
                catY += catItemHeight;
            }
            
            // Section clicks (if category selected and not in search mode)
            if (selectedBazaarCategory != null && !bazaarShowSearch) {
                int secPanelX = listX + catPanelWidth + 3;
                int secPanelWidth = bazaarSectionWidth;
                int secY = contentY + 25;
                int secItemHeight = 20;
                
                for (int i = bazaarSectionScrollOffset; i < bazaarSections.size() && secY < listY + listHeight - secItemHeight; i++) {
                    BazaarSection section = bazaarSections.get(i);
                    if (mouseX >= secPanelX && mouseX < secPanelX + secPanelWidth && mouseY >= secY && mouseY < secY + secItemHeight) {
                        if (!section.id.equals(selectedBazaarSection)) {
                            selectedBazaarSection = section.id;
                            bazaarItems.clear();
                            bazaarScrollOffset = 0;
                            loadBazaarItems(selectedBazaarCategory, section.id);
                        }
                        return;
                    }
                    secY += secItemHeight;
                }
            }
            
            // Search results panel interactions
            if (bazaarShowSearch) {
                int itemsPanelX = listX + catPanelWidth + 3;
                int itemsPanelWidth = (this.width - 100 - 20) - catPanelWidth - 6;
                
                // Back button
                int backX = itemsPanelX + itemsPanelWidth - 70;
                if (mouseX >= backX - 45 && mouseX < backX - 5 && mouseY >= contentY + 3 && mouseY < contentY + 21) {
                    bazaarShowSearch = false;
                    bazaarScrollOffset = 0;
                    return;
                }
                
                // Search button in search panel
                int searchX = itemsPanelX + itemsPanelWidth - 70;
                if (mouseX >= searchX && mouseX < searchX + 60 && mouseY >= contentY + 3 && mouseY < contentY + 21) {
                    String query = bazaarSearchField.getText().trim();
                    if (query.length() >= 2) {
                        bazaarSearchResults.clear();
                        bazaarScrollOffset = 0;
                        searchBazaarItems(query);
                    }
                    return;
                }
            }
            
            return;
        }
        
        // Standard message list clicks
        List<Message> messages = MessageManager.getMessagesByType(currentTab);
        int itemHeight = 55;
        
        int itemY = listY + 5;
        for (Message msg : messages) {
            if (mouseX >= listX + 3 && mouseX < listX + MESSAGE_LIST_WIDTH - 3 &&
                mouseY >= itemY && mouseY < itemY + itemHeight) {
                selectedMessage = msg;
                return;
            }
            itemY += itemHeight + 5;
        }
    }
    
    private void createParty() {
        if (selectedPartyFinderCategory == null) {
            partyCreationStatus = "Error: No category selected";
            return;
        }
        
        partyCreationStatus = "Creating party...";
        
        String note = partyNoteField != null ? partyNoteField.getText() : "";
        
        PartyFinderAPI.createParty(selectedPartyFinderCategory, note, partyMinLevel, partyMaxPlayers, 
            new PartyFinderAPI.ResultCallback() {
                @Override
                public void onSuccess(String partyId) {
                    partyCreationStatus = "Party created! ID: " + partyId;
                    
                    // Send chat message
                    Minecraft mc = Minecraft.getMinecraft();
                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                            "\u00A7a[Party Finder] \u00A7fParty created for \u00A7b" + 
                            selectedPartyFinderCategory.getFullPath() + 
                            "\u00A7f! Party ID: \u00A7e" + partyId
                        ));
                    }
                }
                
                @Override
                public void onError(String error) {
                    partyCreationStatus = "Error: " + error;
                }
            }
        );
    }
    
    private void joinParty(String partyId, JsonObject partyData) {
        if (partyId == null || partyId.isEmpty()) {
            return;
        }
        
        // Get party info from partyData
        String owner = partyData.has("owner") ? partyData.get("owner").getAsString() : "Unknown";
        String category = partyData.has("category") ? partyData.get("category").getAsString() : "Unknown";
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                "\u00A7e[Party Finder] \u00A7fJoining party..."
            ));
        }
        
        PartyFinderAPI.joinParty(partyId, new PartyFinderAPI.ResultCallback() {
            @Override
            public void onSuccess(String result) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                        "\u00A7a[Party Finder] \u00A7fJoined \u00A7b" + owner + "\u00A7f's party for \u00A7e" + category
                    ));
                    
                    // Send party join command
                    mc.thePlayer.sendChatMessage("/p join " + owner);
                }
                
                // Refresh the party list
                lastPartyRefresh = 0;
            }
            
            @Override
            public void onError(String error) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                        "\u00A7c[Party Finder] \u00A7fFailed to join party: " + error
                    ));
                }
            }
        });
    }
    
    /**
     * Draw a simple unread count number in the top right of a tab
     */
    private void drawUnreadCount(int x, int y, int count) {
        String countStr = count > 99 ? "99+" : String.valueOf(count);
        // Draw red number with shadow for visibility
        this.fontRendererObj.drawStringWithShadow(countStr, x, y, 0xFFFF5555);
    }
    
    /**
     * Commit the current filter edit to activeFilters
     */
    private void commitFilterEdit() {
        if (editingFilterName != null && !editingFilterValue.isEmpty()) {
            try {
                int value = Integer.parseInt(editingFilterValue);
                if (value > 0) {
                    activeFilters.put(editingFilterName, value);
                } else {
                    activeFilters.remove(editingFilterName);
                }
            } catch (NumberFormatException e) {
                // Invalid number, ignore
            }
        }
        // Don't remove filter if user just clicked away without typing - keep existing value
        editingFilterName = null;
        editingFilterValue = "";
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        // Handle filter value editing - numbers only
        if (editingFilterName != null && showFilterPanel) {
            // Escape key cancels editing
            if (keyCode == 1) { // ESC
                editingFilterName = null;
                editingFilterValue = "";
                return;
            }
            // Enter commits the edit
            if (keyCode == 28) { // ENTER
                commitFilterEdit();
                return;
            }
            // Backspace removes last character
            if (keyCode == 14) { // BACKSPACE
                if (!editingFilterValue.isEmpty()) {
                    editingFilterValue = editingFilterValue.substring(0, editingFilterValue.length() - 1);
                }
                return;
            }
            // Only accept number characters (0-9)
            if (Character.isDigit(typedChar) && editingFilterValue.length() < 5) {
                editingFilterValue += typedChar;
                return;
            }
            // Don't pass through to other handlers while editing
            return;
        }
        
        super.keyTyped(typedChar, keyCode);
        
        // Handle text field input
        if (partyNoteField != null && currentTab == Message.MessageType.PARTY_FINDER) {
            partyNoteField.textboxKeyTyped(typedChar, keyCode);
        }
        
        // Handle Bazaar search field input
        if (bazaarSearchField != null && currentTab == Message.MessageType.BAZAAR && bazaarShowSearch) {
            bazaarSearchField.textboxKeyTyped(typedChar, keyCode);
            
            // Enter key to search
            if (keyCode == 28) { // ENTER
                String query = bazaarSearchField.getText().trim();
                if (query.length() >= 2) {
                    bazaarSearchResults.clear();
                    bazaarScrollOffset = 0;
                    searchBazaarItems(query);
                }
            }
        }
        
        // Handle Voice API key field input
        if (voiceApiKeyField != null && currentTab == Message.MessageType.VOICE_CHAT && voiceChatViewMode.equals("settings")) {
            voiceApiKeyField.textboxKeyTyped(typedChar, keyCode);
        }
        
        // Handle Voice Room Name field input
        if (voiceRoomNameField != null && currentTab == Message.MessageType.VOICE_CHAT && voiceChatViewMode.equals("create_room")) {
            voiceRoomNameField.textboxKeyTyped(typedChar, keyCode);
            // Enter key to create room
            if (keyCode == 28) { // ENTER
                String roomName = voiceRoomNameField.getText().trim();
                if (!roomName.isEmpty()) {
                    VoiceChatManager.getInstance().createRoom(roomName, false);
                    voiceStatusMessage = "Creating room: " + roomName;
                    voiceStatusMessageTime = System.currentTimeMillis();
                    voiceChatViewMode = "main";
                }
            }
        }
    }
    
    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            // Handle Bazaar scrolling
            if (currentTab == Message.MessageType.BAZAAR) {
                int panelWidth = this.width - 100;
                int listX = PANEL_X + 10;
                int listY = PANEL_Y + HEADER_HEIGHT + 10;
                int contentY = listY + 35;
                
                int catPanelWidth = bazaarCategoryWidth;
                int secPanelX = listX + catPanelWidth + 3;
                int secPanelWidth = bazaarSectionWidth;
                
                // Check if mouse is over sections panel
                boolean overSections = selectedBazaarCategory != null && !bazaarShowSearch &&
                    mouseX >= secPanelX && mouseX < secPanelX + secPanelWidth &&
                    mouseY >= contentY && mouseY < listY + (this.height - 80 - HEADER_HEIGHT - 20);
                
                if (overSections) {
                    // Scroll sections
                    if (scroll > 0) {
                        bazaarSectionScrollOffset = Math.max(0, bazaarSectionScrollOffset - 1);
                    } else {
                        bazaarSectionScrollOffset = Math.min(bazaarMaxSectionScroll, bazaarSectionScrollOffset + 1);
                    }
                } else {
                    // Scroll items/search results
                    if (scroll > 0) {
                        bazaarScrollOffset = Math.max(0, bazaarScrollOffset - 1);
                    } else {
                        bazaarScrollOffset = Math.min(bazaarMaxScroll, bazaarScrollOffset + 1);
                    }
                }
            }
        }
    }
    
    // ===================== INNER CLASSES =====================
    
    /**
     * Simple class to hold voice room info for the room browser
     */
    private static class VoiceRoomEntry {
        public String roomId;
        public String name;
        public int userCount;
        public boolean isPrivate;
        
        public VoiceRoomEntry(String roomId, String name, int userCount, boolean isPrivate) {
            this.roomId = roomId;
            this.name = name;
            this.userCount = userCount;
            this.isPrivate = isPrivate;
        }
    }
}
