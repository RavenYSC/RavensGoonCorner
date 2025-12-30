package com.raven.client.features.Stockholder;

import com.raven.client.gui.GuiOpener;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class OpenStockGuiCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "stockgui";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/stockgui";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
    	GuiOpener.openGuiNextTick(new GuiBazaarHistory());
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
