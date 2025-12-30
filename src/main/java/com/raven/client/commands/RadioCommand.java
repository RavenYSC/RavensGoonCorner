package com.raven.client.commands;

import com.raven.client.music.RadioManager;
import com.raven.client.music.RadioManager.RadioChannel;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * Command to control the radio system.
 * Usage: /radio [play|stop|next|prev|channel|volume|list|shuffle]
 */
public class RadioCommand extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "radio";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/radio [play|stop|next|prev|channel <#>|volume <0-100>|list|shuffle]";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    
    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("rad");
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        RadioManager radio = RadioManager.getInstance();
        
        if (args.length == 0) {
            // Show status
            showStatus(sender, radio);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "play":
            case "start":
                radio.play();
                sendMessage(sender, EnumChatFormatting.GREEN + "Radio started: " + 
                    EnumChatFormatting.AQUA + radio.getCurrentChannelName());
                break;
                
            case "stop":
            case "pause":
                radio.stop();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Radio stopped");
                break;
                
            case "toggle":
                radio.toggle();
                sendMessage(sender, radio.isPlaying() ? 
                    EnumChatFormatting.GREEN + "Radio playing" : 
                    EnumChatFormatting.YELLOW + "Radio stopped");
                break;
                
            case "next":
            case "skip":
                if (args.length > 1 && args[1].equalsIgnoreCase("channel")) {
                    radio.nextChannel();
                    sendMessage(sender, EnumChatFormatting.GREEN + "Switched to: " + 
                        EnumChatFormatting.AQUA + radio.getCurrentChannelName());
                } else {
                    radio.nextTrack();
                    sendMessage(sender, EnumChatFormatting.GREEN + "Skipped to next track");
                }
                break;
                
            case "prev":
            case "previous":
            case "back":
                if (args.length > 1 && args[1].equalsIgnoreCase("channel")) {
                    radio.previousChannel();
                    sendMessage(sender, EnumChatFormatting.GREEN + "Switched to: " + 
                        EnumChatFormatting.AQUA + radio.getCurrentChannelName());
                } else {
                    radio.previousTrack();
                    sendMessage(sender, EnumChatFormatting.GREEN + "Skipped to previous track");
                }
                break;
                
            case "channel":
            case "ch":
                if (args.length < 2) {
                    sendMessage(sender, EnumChatFormatting.RED + "Usage: /radio channel <number>");
                    listChannels(sender, radio);
                } else {
                    try {
                        int channelNum = Integer.parseInt(args[1]) - 1; // User-friendly 1-based
                        if (channelNum >= 0 && channelNum < radio.getChannels().size()) {
                            radio.setChannel(channelNum);
                            sendMessage(sender, EnumChatFormatting.GREEN + "Switched to channel: " + 
                                EnumChatFormatting.AQUA + radio.getCurrentChannelName());
                        } else {
                            sendMessage(sender, EnumChatFormatting.RED + "Invalid channel number");
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(sender, EnumChatFormatting.RED + "Invalid channel number");
                    }
                }
                break;
                
            case "volume":
            case "vol":
                if (args.length < 2) {
                    sendMessage(sender, EnumChatFormatting.AQUA + "Volume: " + 
                        (int)(radio.getVolume() * 100) + "%");
                } else {
                    try {
                        int vol = Integer.parseInt(args[1]);
                        radio.setVolume(vol / 100f);
                        sendMessage(sender, EnumChatFormatting.GREEN + "Volume set to " + vol + "%");
                    } catch (NumberFormatException e) {
                        sendMessage(sender, EnumChatFormatting.RED + "Usage: /radio volume <0-100>");
                    }
                }
                break;
                
            case "list":
            case "channels":
                listChannels(sender, radio);
                break;
                
            case "shuffle":
                radio.toggleShuffle();
                sendMessage(sender, EnumChatFormatting.GREEN + "Shuffle: " + 
                    (radio.isShuffle() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;
                
            case "enable":
            case "on":
                radio.setEnabled(true);
                sendMessage(sender, EnumChatFormatting.GREEN + "Radio enabled");
                break;
                
            case "disable":
            case "off":
                radio.setEnabled(false);
                sendMessage(sender, EnumChatFormatting.YELLOW + "Radio disabled");
                break;
                
            default:
                sendMessage(sender, EnumChatFormatting.RED + "Unknown subcommand. Use /radio for help.");
                showHelp(sender);
        }
    }
    
    private void showStatus(ICommandSender sender, RadioManager radio) {
        sendMessage(sender, EnumChatFormatting.GOLD + "=== Radio Status ===");
        sendMessage(sender, EnumChatFormatting.WHITE + "Status: " + 
            (radio.isPlaying() ? EnumChatFormatting.GREEN + "Playing" : EnumChatFormatting.RED + "Stopped"));
        sendMessage(sender, EnumChatFormatting.WHITE + "Channel: " + 
            EnumChatFormatting.AQUA + radio.getCurrentChannelName() + 
            EnumChatFormatting.GRAY + " (" + (radio.getCurrentChannelIndex() + 1) + "/" + radio.getChannels().size() + ")");
        sendMessage(sender, EnumChatFormatting.WHITE + "Track: " + 
            EnumChatFormatting.YELLOW + radio.getCurrentTrackName());
        sendMessage(sender, EnumChatFormatting.WHITE + "Volume: " + 
            EnumChatFormatting.GREEN + (int)(radio.getVolume() * 100) + "%");
        sendMessage(sender, EnumChatFormatting.WHITE + "Shuffle: " + 
            (radio.isShuffle() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
        sendMessage(sender, "");
        showHelp(sender);
    }
    
    private void showHelp(ICommandSender sender) {
        sendMessage(sender, EnumChatFormatting.GRAY + "Commands:");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio play" + EnumChatFormatting.GRAY + " - Start playing");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio stop" + EnumChatFormatting.GRAY + " - Stop playing");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio next/prev" + EnumChatFormatting.GRAY + " - Skip track");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio next channel" + EnumChatFormatting.GRAY + " - Next channel");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio channel <#>" + EnumChatFormatting.GRAY + " - Select channel");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio volume <0-100>" + EnumChatFormatting.GRAY + " - Set volume");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio list" + EnumChatFormatting.GRAY + " - List channels");
        sendMessage(sender, EnumChatFormatting.YELLOW + "/radio shuffle" + EnumChatFormatting.GRAY + " - Toggle shuffle");
    }
    
    private void listChannels(ICommandSender sender, RadioManager radio) {
        sendMessage(sender, EnumChatFormatting.GOLD + "=== Radio Channels ===");
        List<RadioChannel> channels = radio.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            RadioChannel ch = channels.get(i);
            boolean current = i == radio.getCurrentChannelIndex();
            String prefix = current ? EnumChatFormatting.GREEN + "> " : EnumChatFormatting.GRAY + "  ";
            String typeIcon = ch.type.equals("stream") ? EnumChatFormatting.AQUA + "[STREAM]" : EnumChatFormatting.YELLOW + "[LOCAL]";
            sendMessage(sender, prefix + EnumChatFormatting.WHITE + (i + 1) + ". " + ch.name + " " + typeIcon);
        }
    }
    
    private void sendMessage(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "[Radio] " + EnumChatFormatting.RESET + message));
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
