package com.raven.client;

import com.raven.client.Renderer.ClientHandler;
import com.raven.client.commands.CommandRegistry;
import com.raven.client.features.FeatureManager;
import com.raven.client.features.Stockholder.BazaarDataManager;
import com.raven.client.features.dungeons.AutoKick.AutoKick;
import com.raven.client.features.dungeons.chatmessages.PositionalMessages;
import com.raven.client.features.mining.EfficientMinerHeatmap;
import com.raven.client.features.mining.NoBlockBreakReset;
import com.raven.client.gui.GuiOpener;
import com.raven.client.gui.notifications.NotificationRenderer;
import com.raven.client.gui.partyfinder.HypixelPartyTracker;
import com.raven.client.license.LicenseChecker;
import com.raven.client.music.MainMenuMusicHandler;
import com.raven.client.music.MusicManager;
import com.raven.client.music.PlaylistSyncManager;
import com.raven.client.music.RadioManager;
import com.raven.client.music.UserPlaylistManager;
import com.raven.client.overlay.OverlayUI;
import com.raven.client.skyblock.SkyblockKeyBinds;
import com.raven.client.updater.UpdateChecker;
import com.raven.client.updater.UpdateHandler;
import com.raven.client.utils.ConfigManager;
import com.raven.client.utils.DirectoryManager;
import com.raven.client.voicechat.VoiceChatEventHandler;
import com.raven.client.voicechat.VoiceChatManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = RavenClient.MODID, name = RavenClient.NAME, version = RavenClient.VERSION)
public class RavenClient {

    public static final String MODID = "ravenclient";
    public static final String NAME = "1:1 Client";
    public static final String VERSION = "1.0";

    public static final FeatureManager featureManager = new FeatureManager();
    public static final CommandRegistry commandManager = new CommandRegistry();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Apply any pending updates before mod loads
        UpdateChecker.applyPendingUpdates();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("[" + NAME + "] Initializing v" + VERSION + "...");
        
        ConfigManager.load();

        // Register License Checker FIRST - before anything else
        MinecraftForge.EVENT_BUS.register(new LicenseChecker());

        // Register Update Handler for update notifications
        MinecraftForge.EVENT_BUS.register(new UpdateHandler());

        // Check for updates asynchronously
        UpdateChecker.getInstance().checkForUpdatesAsync();

        // Event Registration
        MinecraftForge.EVENT_BUS.register(new GuiInterceptor());
        MinecraftForge.EVENT_BUS.register(new GuiOpener());
        MinecraftForge.EVENT_BUS.register(new OverlayUI());
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new NotificationRenderer());
        MinecraftForge.EVENT_BUS.register(new SkyblockKeyBinds());

        // Register Keybinds
        SkyblockKeyBinds.register();

        // Register Features & Commands
        CommandRegistry.registerAll();
        registerFeatures();

        // Initialize Voice Chat system
        VoiceChatManager.getInstance().initialize();
        MinecraftForge.EVENT_BUS.register(new VoiceChatEventHandler());

        // Ensure folders
        DirectoryManager.ensureDirectoriesExist();

        // Load user-created playlists (fast, local)
        UserPlaylistManager.load();

        // Load music library (fast, local)
        MusicManager.init();

        // Initialize Radio system
        RadioManager.getInstance();
        System.out.println("[" + NAME + "] Radio system initialized");

        // Initialize Main Menu music handler and register events
        MinecraftForge.EVENT_BUS.register(MainMenuMusicHandler.getInstance());
        System.out.println("[" + NAME + "] Main menu music handler initialized");

        // Initialize Hypixel Party Tracker for party finder verification
        HypixelPartyTracker.getInstance();
        System.out.println("[" + NAME + "] Party tracker initialized");

        // Async: Sync playlists from API on background thread (non-blocking)
        new Thread(() -> {
            try {
                PlaylistSyncManager.syncPlaylistsFromAPI();
            } catch (Exception e) {
                System.err.println("[RavenClient] Error syncing playlists: " + e.getMessage());
            }
        }).start();
    }

    private void registerFeatures() {
        featureManager.register(new AutoKick());
        featureManager.register(new PositionalMessages());
        featureManager.register(new EfficientMinerHeatmap());
        featureManager.register(new NoBlockBreakReset());
        featureManager.register(new BazaarDataManager());
    }
}
