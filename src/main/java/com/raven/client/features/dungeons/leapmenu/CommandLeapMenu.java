package com.raven.client.features.dungeons.leapmenu;

import com.raven.client.features.dungeons.leapmenu.LeapMenu;
import com.raven.client.gui.GuiOpener;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class CommandLeapMenu extends CommandBase {
    @Override
    public String getCommandName() {
        return "leapmenu";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/leapmenu - opens the custom leap menu GUI";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
    	GuiOpener.openGuiNextTick(new LeapMenu());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Allow all players (even in SP or dev)
    }
}