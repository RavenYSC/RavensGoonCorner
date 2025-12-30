package com.raven.client.features.Stockholder;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class StockholderCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "stock";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/stock <track|untrack|list> [ITEM_ID]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("§cUsage: /stock <track|untrack|list> [ITEM_ID]"));
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "track":
                if (args.length != 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /stock track <ITEM_ID>"));
                    return;
                }
                String toTrack = args[1].toUpperCase();
                BazaarDataManager.watchItem(toTrack);
                BazaarDataManager.saveTrackedItems();
                sender.addChatMessage(new ChatComponentText("§aNow tracking: §e" + toTrack));
                break;

            case "untrack":
                if (args.length != 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /stock untrack <ITEM_ID>"));
                    return;
                }
                String toUntrack = args[1].toUpperCase();
                BazaarDataManager.unwatchItem(toUntrack);
                BazaarDataManager.saveTrackedItems();
                sender.addChatMessage(new ChatComponentText("§cNo longer tracking: §e" + toUntrack));
                break;

            case "list":
                Set<String> tracked = BazaarDataManager.getTrackedItemIds();
                if (tracked.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("§7No items are currently being tracked."));
                    return;
                }
                sender.addChatMessage(new ChatComponentText("§aTracked Items (" + tracked.size() + "):"));
                for (String id : tracked) {
                    sender.addChatMessage(new ChatComponentText("§8- §e" + id));
                }
                break;

            default:
                sender.addChatMessage(new ChatComponentText("§cUnknown subcommand. Use: track, untrack, list"));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("stockholder");
    }
}
