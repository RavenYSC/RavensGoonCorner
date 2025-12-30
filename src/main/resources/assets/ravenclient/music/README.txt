BUNDLED MUSIC FOR 1:1 CLIENT
=============================

Place MP3 files in the 'menu' folder to bundle them with the mod.
These will be automatically extracted to the user's menu music folder on first run.

After adding files, update the BUNDLED_TRACKS array in MainMenuMusicHandler.java:

    private static final String[] BUNDLED_TRACKS = {
        "your_track_1.mp3",
        "your_track_2.mp3",
        // etc...
    };

Note: Keep file sizes reasonable (under 5MB each recommended) to avoid bloating the mod jar.
Consider using lower bitrate MP3s (128-192kbps) for menu music.

Folder structure:
  /assets/ravenclient/music/
    /menu/           <- Main menu music (extracted to user's menu folder)
      menu_chill_1.mp3
      menu_ambient_1.mp3
      etc...
