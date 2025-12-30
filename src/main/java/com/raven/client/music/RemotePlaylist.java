package com.raven.client.music;

public class RemotePlaylist {
    public final String name;
    public final String id;  // used for image path or internal reference

    public RemotePlaylist(String id, String name) {
        this.id = id;
        this.name = name;
    }
}