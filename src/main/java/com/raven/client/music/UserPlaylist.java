package com.raven.client.music;

import java.util.List;

public class UserPlaylist {
    public String id;
    public String name;
    public String image; // just filename like "chill.png"
    public List<String> songIds;

    public UserPlaylist(String id, String name, String image, List<String> songIds) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.songIds = songIds;
    }
}
