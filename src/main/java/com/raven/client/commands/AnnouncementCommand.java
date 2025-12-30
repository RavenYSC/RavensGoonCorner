package com.raven.client.commands;

import com.raven.client.gui.GuiAnnouncement;
import com.raven.client.gui.GuiOpener;
import com.raven.client.gui.notifications.Message;
import com.raven.client.gui.notifications.MessageManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class AnnouncementCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "111";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/111 - Open the announcement GUI";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // Add sample messages on first open
        if (MessageManager.getMessages().isEmpty()) {
            MessageManager.addMessage(new Message("evt1", "Server Maintenance", 
                "Server will go down for maintenance from 2-4 AM EST tomorrow.", Message.MessageType.EVENT));
            MessageManager.addMessage(new Message("news1", "New Feature: Dungeons v2", 
                "Dungeons have been completely revamped with new mechanics and rewards.", Message.MessageType.NEWS));
            MessageManager.addMessage(new Message("inbox1", "Welcome!", 
                "Welcome to the RavenClient messaging system!\n\nYou can now receive events, news, and messages all in one place.", Message.MessageType.INBOX));
        }
        
        GuiOpener.openGuiNextTick(new GuiAnnouncement());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
