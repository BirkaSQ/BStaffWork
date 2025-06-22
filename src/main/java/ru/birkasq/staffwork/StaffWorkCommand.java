package ru.birkasq.staffwork;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class StaffWorkCommand implements CommandExecutor {
    private final StaffWork plugin;
    private final PlayerDataManager dataManager;
    private final HashMap<Player, Long> cooldowns = new HashMap<>();

    public StaffWorkCommand(StaffWork plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            return handleStatsCommand(player, args);
        }

        return handleStaffWorkCommand(player);
    }

    private boolean handleStatsCommand(Player player, String[] args) {
        if (!player.hasPermission("staffwork.use")) {
            String noPermission = plugin.getConfig().getString("messages.noPermission");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
            return true;
        }

        String targetName = args.length > 1 ? args[1] : player.getName();

        if (!dataManager.hasPlayerData(targetName)) {
            String msg = plugin.getConfig().getString("messages.stats.player-not-found")
                    .replace("{player}", targetName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return true;
        }

        dataManager.sendCompactStats(player, targetName);
        return true;
    }

    private boolean handleStaffWorkCommand(Player player) {
        if (!player.hasPermission("staffwork.use")) {
            String noPermission = plugin.getConfig().getString("messages.noPermission");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
            return true;
        }

        String playerName = player.getName();
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());

        int cooldownSeconds = plugin.getConfig().getInt("delay");
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(player)) {
            long lastUseTime = cooldowns.get(player);
            long timeLeft = (lastUseTime + (cooldownSeconds * 1000L)) - currentTime;

            if (timeLeft > 0) {
                int secondsLeft = (int) (timeLeft / 1000);
                String cooldownMessage = plugin.getConfig().getString("messages.cooldown");
                cooldownMessage = cooldownMessage.replace("%delay%", String.valueOf(secondsLeft));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMessage));
                return true;
            }
        }

        cooldowns.put(player, currentTime);

        if (!user.getPrimaryGroup().isEmpty()) {
            String group = user.getPrimaryGroup();
            if (plugin.getConfig().contains("groupCommands." + group)) {
                List<String> commandsToExecute;
                String commandMessage;
                boolean currentStatus = dataManager.isInStaffWork(playerName);
                dataManager.setStaffWorkStatus(playerName, !currentStatus);

                if (!currentStatus) {
                    commandsToExecute = plugin.getConfig().getStringList("groupCommands." + group + ".enable");
                    commandMessage = plugin.getConfig().getString("messages.staffWorkEnabled");
                } else {
                    commandsToExecute = plugin.getConfig().getStringList("groupCommands." + group + ".disable");
                    commandMessage = plugin.getConfig().getString("messages.staffWorkDisabled");
                }

                for (String commandToExecute : commandsToExecute) {
                    commandToExecute = commandToExecute.replace("%player%", playerName);
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandToExecute);
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandMessage));
            } else {
                String noConfig = plugin.getConfig().getString("messages.noconfig");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noConfig));
            }
        } else {
            String noGroup = plugin.getConfig().getString("messages.nogroup");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noGroup));
        }
        return true;
    }
}