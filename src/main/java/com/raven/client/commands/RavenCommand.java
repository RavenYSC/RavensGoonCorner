package com.raven.client.commands;

import com.raven.client.gui.GuiOpener;
import com.raven.client.gui.RavenGui;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class RavenCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "11";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/11";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
    	GuiOpener.openGuiNextTick(new RavenGui());
        sender.addChatMessage(new ChatComponentText("[1:1 Client] GUI opened."));
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

}
