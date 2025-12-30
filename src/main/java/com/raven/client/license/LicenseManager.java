package com.raven.client.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

public class LicenseManager {
    
    private static final String LICENSE_API_URL = "http://100.42.184.35:25582";
    private static final String LICENSE_FILE = "ravenclient_license.key";
    
    private static LicenseManager instance;
    private String licenseKey = null;
    private String username = null;
    private String discordId = null;
    private boolean isValidated = false;
    private String validationError = null;
    
    public static LicenseManager getInstance() {
        if (instance == null) {
            instance = new LicenseManager();
        }
        return instance;
    }
    
    private LicenseManager() {
        loadStoredKey();
    }
    
    /**
     * Load the stored license key from file
     */
    private void loadStoredKey() {
        try {
            File licenseFile = new File(Minecraft.getMinecraft().mcDataDir, LICENSE_FILE);
            if (licenseFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(licenseFile));
                String key = reader.readLine();
                reader.close();
                
                if (key != null && !key.trim().isEmpty()) {
                    this.licenseKey = key.trim();
                    System.out.println("[RavenClient] Found stored license key");
                }
            }
        } catch (Exception e) {
            System.err.println("[RavenClient] Error loading license key: " + e.getMessage());
        }
    }
    
    /**
     * Save the license key to file
     */
    private void saveKey(String key) {
        try {
            File licenseFile = new File(Minecraft.getMinecraft().mcDataDir, LICENSE_FILE);
            FileWriter writer = new FileWriter(licenseFile);
            writer.write(key);
            writer.close();
            System.out.println("[RavenClient] License key saved");
        } catch (Exception e) {
            System.err.println("[RavenClient] Error saving license key: " + e.getMessage());
        }
    }
    
    /**
     * Check if we have a stored key that needs validation
     */
    public boolean hasStoredKey() {
        return licenseKey != null && !licenseKey.isEmpty();
    }
    
    /**
     * Get the stored key
     */
    public String getStoredKey() {
        return licenseKey;
    }
    
    /**
     * Check if the license is validated
     */
    public boolean isValidated() {
        return isValidated;
    }
    
    /**
     * Get the last validation error
     */
    public String getValidationError() {
        return validationError;
    }
    
    /**
     * Get the licensed username
     */
    public String getLicensedUsername() {
        return username;
    }
    
    /**
     * Get the licensed Discord ID
     */
    public String getDiscordId() {
        return discordId;
    }
    
    /**
     * Validate a license key against the server
     * @param key The license key to validate
     * @return true if valid, false otherwise
     */
    public boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            validationError = "Please enter a license key";
            return false;
        }
        
        key = key.trim();
        validationError = null;
        
        try {
            // Get the Minecraft username for HWID binding
            String mcUsername = Minecraft.getMinecraft().getSession().getUsername();
            String hwid = generateHWID();
            
            String urlStr = LICENSE_API_URL + "/license/validate?key=" + URLEncoder.encode(key, "UTF-8")
                          + "&username=" + URLEncoder.encode(mcUsername, "UTF-8")
                          + "&hwid=" + URLEncoder.encode(hwid, "UTF-8");
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            BufferedReader reader;
            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
            
            if (json.has("valid") && json.get("valid").getAsBoolean()) {
                // License is valid
                this.licenseKey = key;
                this.isValidated = true;
                
                if (json.has("username")) {
                    this.username = json.get("username").getAsString();
                }
                if (json.has("discordId")) {
                    this.discordId = json.get("discordId").getAsString();
                }
                
                // Save the key for future use
                saveKey(key);
                
                System.out.println("[RavenClient] License validated for: " + this.username);
                return true;
            } else {
                // License is invalid
                this.isValidated = false;
                if (json.has("error")) {
                    this.validationError = json.get("error").getAsString();
                } else {
                    this.validationError = "Invalid license key";
                }
                return false;
            }
            
        } catch (Exception e) {
            this.isValidated = false;
            this.validationError = "Connection error: " + e.getMessage();
            System.err.println("[RavenClient] License validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate a hardware ID for HWID binding
     */
    private String generateHWID() {
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String userName = System.getProperty("user.name");
            String userHome = System.getProperty("user.home");
            
            String combined = osName + osVersion + userName + userHome;
            
            // Simple hash
            int hash = combined.hashCode();
            return Integer.toHexString(hash).toUpperCase();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Invalidate the current license (for logout/reset)
     */
    public void invalidateLicense() {
        this.isValidated = false;
        this.licenseKey = null;
        this.username = null;
        this.discordId = null;
        
        // Delete stored key
        try {
            File licenseFile = new File(Minecraft.getMinecraft().mcDataDir, LICENSE_FILE);
            if (licenseFile.exists()) {
                licenseFile.delete();
            }
        } catch (Exception e) {
            System.err.println("[RavenClient] Error deleting license file: " + e.getMessage());
        }
    }
}
