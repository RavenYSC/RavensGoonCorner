package com.raven.client.commands;

import com.raven.client.features.Stockholder.OpenStockGuiCommand;
import com.raven.client.features.Stockholder.StockholderCommand;
import com.raven.client.features.dungeons.AutoKick.AutoKickCommand;
import com.raven.client.features.dungeons.leapmenu.CommandLeapMenu;
import com.raven.client.music.MusicCommand;
import com.raven.client.voicechat.commands.VoiceChatCommand;

import net.minecraftforge.client.ClientCommandHandler;

public class CommandRegistry {

    public static void registerAll() {
        ClientCommandHandler.instance.registerCommand(new RavenCommand());
        ClientCommandHandler.instance.registerCommand(new AutoKickCommand());
        ClientCommandHandler.instance.registerCommand(new StockholderCommand());
        ClientCommandHandler.instance.registerCommand(new OpenStockGuiCommand());
        ClientCommandHandler.instance.registerCommand(new MusicCommand());
        ClientCommandHandler.instance.registerCommand(new CommandLeapMenu());
        ClientCommandHandler.instance.registerCommand(new AnnouncementCommand());
        ClientCommandHandler.instance.registerCommand(new VoiceChatCommand());
        // Add any future commands here
    }
}