package com.raven.client.voicechat.commands;

import com.raven.client.gui.GuiOpener;
import com.raven.client.voicechat.gui.GuiVoiceChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

/**
 * Command to open the voice chat GUI
 * Usage: /vc or /voicechat
 */
public class VoiceChatCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "vc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vc - Opens the voice chat GUI";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        GuiOpener.openGuiNextTick(new GuiVoiceChat());
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
