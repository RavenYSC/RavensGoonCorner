package com.raven.client.api;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AnnouncementFetcher {

    public static String[] fetch() {
        try {
            URL url = new URL("http://100.42.184.35:8081/announcement");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();

            String title = json.get("title").getAsString();
            String message = json.get("message").getAsString();

            return new String[] { title, message };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
