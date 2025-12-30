package com.raven.client.features.dungeons.AutoKick;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Map;

public class AutoKickCommand extends CommandBase {

    private static final File configFile = new File(Minecraft.getMinecraft().mcDataDir, "config/autokick_thresholds.cfg");
    private static final Configuration config = new Configuration(configFile);

    static {
        if (configFile.exists()) {
            config.load();
            for (String key : config.getCategory("thresholds").keySet()) {
                int value = config.get("thresholds", key, 9999).getInt();
                AutoKick.floorThresholds.put(key.toUpperCase(), value);
            }
        }
    }

    private void saveConfig() {
        config.getCategory("thresholds").clear(); // Clear old entries
        for (Map.Entry<String, Integer> entry : AutoKick.floorThresholds.entrySet()) {
            config.get("thresholds", entry.getKey(), entry.getValue());
        }
        config.save();
    }

    @Override
    public String getCommandName() {
        return "ak";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "//ak <floor> <seconds>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.addChatMessage(new ChatComponentText("§cUsage: //ak <floor> <seconds>"));
            return;
        }

        String floor = args[0].toUpperCase();
        try {
            int seconds = Integer.parseInt(args[1]);
            AutoKick.floorThresholds.put(floor, seconds);
            AutoKick.currentFloor = floor;
            saveConfig(); // Save after update
            sender.addChatMessage(new ChatComponentText("§aSet §e" + floor + " §aPB limit to §e" + seconds + "s"));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText("§cInvalid time: " + args[1]));
        }
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