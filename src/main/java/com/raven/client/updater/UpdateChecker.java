package com.raven.client.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raven.client.RavenClient;

import net.minecraft.launchwrapper.Launch;

public class UpdateChecker {
    
    // GitHub repository info
    private static final String GITHUB_OWNER = "RavenYSC";
    private static final String GITHUB_REPO = "RavensGoonCorner";
    private static final String RELEASES_URL = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
    
    private static UpdateChecker instance;
    private String latestVersion = null;
    private String downloadUrl = null;
    private String releaseNotes = null;
    private boolean updateAvailable = false;
    private boolean checkComplete = false;
    private boolean isDownloading = false;
    private int downloadProgress = 0;
    
    public static UpdateChecker getInstance() {
        if (instance == null) {
            instance = new UpdateChecker();
        }
        return instance;
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdatesAsync() {
        new Thread(() -> {
            checkForUpdates();
        }, "RavenClient-UpdateChecker").start();
    }
    
    /**
     * Check for updates (blocking)
     */
    public void checkForUpdates() {
        try {
            System.out.println("[RavenClient] Checking for updates...");
            
            URL url = new URL(RELEASES_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "RavenClient-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JsonObject release = new JsonParser().parse(response.toString()).getAsJsonObject();
                
                // Get version tag (e.g., "v1.1" or "1.1")
                String tagName = release.get("tag_name").getAsString();
                latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                
                // Get release notes
                if (release.has("body") && !release.get("body").isJsonNull()) {
                    releaseNotes = release.get("body").getAsString();
                }
                
                // Find the JAR download URL
                JsonArray assets = release.getAsJsonArray("assets");
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }
                
                // Compare versions
                String currentVersion = RavenClient.VERSION;
                updateAvailable = isNewerVersion(latestVersion, currentVersion);
                
                if (updateAvailable) {
                    System.out.println("[RavenClient] Update available! Current: " + currentVersion + ", Latest: " + latestVersion);
                } else {
                    System.out.println("[RavenClient] You are running the latest version (" + currentVersion + ")");
                }
                
            } else if (responseCode == 404) {
                System.out.println("[RavenClient] No releases found on GitHub");
            } else {
                System.out.println("[RavenClient] Failed to check for updates: HTTP " + responseCode);
            }
            
        } catch (Exception e) {
            System.err.println("[RavenClient] Error checking for updates: " + e.getMessage());
        } finally {
            checkComplete = true;
        }
    }
    
    /**
     * Compare version strings (e.g., "1.2" vs "1.1")
     */
    private boolean isNewerVersion(String latest, String current) {
        try {
            // Remove any non-numeric prefixes
            latest = latest.replaceAll("[^0-9.]", "");
            current = current.replaceAll("[^0-9.]", "");
            
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestNum > currentNum) {
                    return true;
                } else if (latestNum < currentNum) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Download the update
     */
    public boolean downloadUpdate() {
        if (downloadUrl == null) {
            System.err.println("[RavenClient] No download URL available");
            return false;
        }
        
        isDownloading = true;
        downloadProgress = 0;
        
        try {
            System.out.println("[RavenClient] Downloading update from: " + downloadUrl);
            
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "RavenClient-Updater");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            int fileSize = conn.getContentLength();
            
            // Get mods folder using Launch.minecraftHome (works in production)
            File modsFolder = new File(Launch.minecraftHome, "mods");
            File updateFile = new File(modsFolder, "RavenClient-" + latestVersion + ".jar.update");
            
            // Download to temp file
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(updateFile);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                
                if (fileSize > 0) {
                    downloadProgress = (int) ((totalRead * 100L) / fileSize);
                }
            }
            
            out.close();
            in.close();
            
            // Find and mark old jar for deletion
            File[] modFiles = modsFolder.listFiles();
            if (modFiles != null) {
                for (File file : modFiles) {
                    if (file.getName().toLowerCase().contains("ravenclient") && file.getName().endsWith(".jar")) {
                        // Create marker file to delete old version on next launch
                        File deleteMarker = new File(modsFolder, file.getName() + ".delete");
                        deleteMarker.createNewFile();
                    }
                }
            }
            
            // Rename update file to final name
            File finalFile = new File(modsFolder, "RavenClient-" + latestVersion + ".jar");
            
            // Create a marker to rename on next launch
            File renameMarker = new File(modsFolder, "ravenclient_update.marker");
            java.io.PrintWriter writer = new java.io.PrintWriter(renameMarker);
            writer.println(updateFile.getAbsolutePath());
            writer.println(finalFile.getAbsolutePath());
            writer.close();
            
            System.out.println("[RavenClient] Update downloaded successfully! Restart Minecraft to apply.");
            
            isDownloading = false;
            return true;
            
        } catch (Exception e) {
            System.err.println("[RavenClient] Error downloading update: " + e.getMessage());
            e.printStackTrace();
            isDownloading = false;
            return false;
        }
    }
    
    /**
     * Apply pending updates (call early in mod loading)
     */
    public static void applyPendingUpdates() {
        try {
            // Use Launch.minecraftHome since Minecraft instance may not be ready
            File modsFolder = new File(Launch.minecraftHome, "mods");
            
            // Process delete markers
            File[] files = modsFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".delete")) {
                        String jarName = file.getName().replace(".delete", "");
                        File jarFile = new File(modsFolder, jarName);
                        if (jarFile.exists()) {
                            if (jarFile.delete()) {
                                System.out.println("[RavenClient] Deleted old version: " + jarName);
                            }
                        }
                        file.delete();
                    }
                }
            }
            
            // Process rename marker
            File renameMarker = new File(modsFolder, "ravenclient_update.marker");
            if (renameMarker.exists()) {
                BufferedReader reader = new BufferedReader(new java.io.FileReader(renameMarker));
                String updatePath = reader.readLine();
                String finalPath = reader.readLine();
                reader.close();
                
                if (updatePath != null && finalPath != null) {
                    File updateFile = new File(updatePath);
                    File finalFile = new File(finalPath);
                    
                    if (updateFile.exists()) {
                        if (updateFile.renameTo(finalFile)) {
                            System.out.println("[RavenClient] Applied update: " + finalFile.getName());
                        }
                    }
                }
                
                renameMarker.delete();
            }
            
        } catch (Exception e) {
            System.err.println("[RavenClient] Error applying updates: " + e.getMessage());
        }
    }
    
    // Getters
    public boolean isUpdateAvailable() { return updateAvailable; }
    public boolean isCheckComplete() { return checkComplete; }
    public boolean isDownloading() { return isDownloading; }
    public int getDownloadProgress() { return downloadProgress; }
    public String getLatestVersion() { return latestVersion; }
    public String getReleaseNotes() { return releaseNotes; }
    public String getDownloadUrl() { return downloadUrl; }
}
