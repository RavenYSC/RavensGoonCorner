package com.raven.client.commands;


import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.client.gui.GuiScreen;
import com.raven.client.gui.GuiOpener;

import java.util.ArrayList;
import java.util.List;

public abstract class Command implements ICommand {
    private final String name;

    public Command(String name) {
        this.name = name;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + name;
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        onCommand(args);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        return null;
    }


    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    public abstract void onCommand(String[] args);
    
    protected void openGui(GuiScreen gui) {
        GuiOpener.openGuiNextTick(gui);
    }
    
    @Override
    public int compareTo(ICommand other) {
        return this.getCommandName().compareTo(other.getCommandName());
    }
}