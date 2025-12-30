package com.raven.client.music;

import com.raven.client.commands.Command;
import com.raven.client.gui.notifications.NotificationManager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.gui.GuiScreen;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.*;

public class MusicCommand extends Command {

    private static final String API_URL = "http://100.42.184.35:8081";
    private Minecraft mc;
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    public MusicCommand() {
        super("music");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            send("Usage: /music <play|stop|next|list|download <name>|gui>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "play":
                MusicManager.loadTracks();
                MusicManager.playCurrent();
                send("Playing music.");
                break;

            case "stop":
                MusicManager.stop();
                send("Stopped music.");
                break;

            case "next":
                MusicManager.next();
                send("Next track.");
                break;

            case "list":
                List<RemoteSong> songs = fetchAvailableSongs();
                send("Available Songs:");
                for (RemoteSong song : songs) {
                    send(" - " + song.name);
                }
                break;

            case "download":
                if (args.length < 2) {
                    send("Usage: /music download <name>");
                    return;
                }
                String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                RemoteSong target = null;
                for (RemoteSong song : fetchAvailableSongs()) {
                    if (song.name.equalsIgnoreCase(targetName)) {
                        target = song;
                        break;
                    }
                }
                if (target != null && downloadSong(target)) {
                    send("Downloaded and added to library: " + target.name);
                } else {
                    send("Failed to download song: " + targetName);
                }
                break;

            case "gui":
                openGui(new MusicGUI());
                send("Opening Music GUI.");
                break;

            default:
                send("Unknown subcommand.");
        }
    }

    private void send(String message) {
        getMc().thePlayer.addChatMessage(new ChatComponentText("�b[Music]�r " + message));
    }

    private List<RemoteSong> fetchAvailableSongs() {
        List<RemoteSong> songs = new ArrayList<>();
        try {
            URL url = new URL(API_URL + "/music");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            JsonParser parser = new JsonParser();
            JsonArray array = parser.parse(json.toString()).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                songs.add(new RemoteSong(obj.get("name").getAsString(), obj.get("id").getAsString()));
            }
        } catch (Exception e) {
            send("Error fetching song list.");
            e.printStackTrace();
        }
        return songs;
    }

    private boolean downloadSong(RemoteSong song) {
        try {
            URL url = new URL("https://100.42.184.35:8081/music/download/" + URLEncoder.encode(song.id, "UTF-8"));
            send("Requesting: " + url.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            File target = new File("config/RavenYSC Configs/MusicLibrary/" + song.id);
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            MusicManager.addTrack(target);
            return true;
        } catch (Exception e) {
            send("Download failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Optional: if not already present in Command base class
    protected void openGui(GuiScreen gui) {
        com.raven.client.gui.GuiOpener.openGuiNextTick(gui);
    }
}
