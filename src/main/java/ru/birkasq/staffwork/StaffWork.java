package ru.birkasq.staffwork;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import ru.birkasq.staffwork.PlayerData;

public class StaffWork extends JavaPlugin implements CommandExecutor, Listener {

    private final HashMap<Player, Long> cooldowns = new HashMap<>();
    private FileConfiguration config;
    private File dataFile;
    private Map<String, PlayerData> staffWorkStates = new HashMap<String, PlayerData>();

    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();
        this.dataFile = new File(this.getDataFolder(), "data.yml");
        if (!this.dataFile.exists()) {
            this.saveResource("data.yml", false);
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        this.loadStaffWorkStates();
        this.getCommand("staffwork").setExecutor(this);
        this.getCommand("sw").setExecutor(this);
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, this::resetDailyStats, 20L, 1728000L);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }
        Player player = (Player)((Object)sender);
        String playerName = player.getName();

        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            if (!player.hasPermission("staffwork.use")) {
                String noPermission = config.getString("messages.noPermission");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
                return true;
            }

            String targetName = args.length > 1 ? args[1] : playerName;

            if (!staffWorkStates.containsKey(targetName)) {
                String msg = config.getString("messages.stats.player-not-found")
                        .replace("{player}", targetName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return true;
            }

            PlayerData targetData = staffWorkStates.get(targetName);
            sendCompactStats(player, targetName, targetData);
            return true;
        }

        if (!player.hasPermission("staffwork.use")) {
            String noPermission = config.getString("messages.noPermission");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
            return true;
        }
        if (!this.staffWorkStates.containsKey(playerName)) {
            this.staffWorkStates.put(playerName, new PlayerData());
        }
        if (command.getName().equalsIgnoreCase("staffwork") || command.getName().equalsIgnoreCase("sw")) {
            playerName = player.getName();
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());

            int cooldownSeconds = config.getInt("delay");

            long currentTime = System.currentTimeMillis();
            if (cooldowns.containsKey(player)) {
                long lastUseTime = cooldowns.get(player);
                long timeLeft = (lastUseTime + (cooldownSeconds * 1000L)) - currentTime;

                if (timeLeft > 0) {
                    int secondsLeft = (int) (timeLeft / 1000);
                    String cooldownMessage = config.getString("messages.cooldown");
                    cooldownMessage = cooldownMessage.replace("%delay%", String.valueOf(secondsLeft));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMessage));
                    return true;
                }
            }

            cooldowns.put(player, currentTime);

            if (!user.getPrimaryGroup().isEmpty()) {
                String group = user.getPrimaryGroup();
                if (this.config.contains("groupCommands." + group)) {
                    List<String> commandsToExecute;
                    String commandMessage;
                    boolean currentStatus = this.staffWorkStates.get(playerName).isInStaffWork();
                    this.staffWorkStates.get(playerName).setInStaffWork(!currentStatus);
                    this.saveStaffWorkStates();
                    if (!currentStatus) {
                        commandsToExecute = this.config.getStringList("groupCommands." + group + ".enable");
                        commandMessage = this.config.getString("messages.staffWorkEnabled");
                    } else {
                        commandsToExecute = this.config.getStringList("groupCommands." + group + ".disable");
                        commandMessage = this.config.getString("messages.staffWorkDisabled");
                    }
                    for (String commandToExecute : commandsToExecute) {
                        commandToExecute = commandToExecute.replace("%player%", playerName);
                        this.getServer().dispatchCommand(this.getServer().getConsoleSender(), commandToExecute);
                    }
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandMessage));
                } else {
                    String NoConfig = config.getString("messages.noconfig");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', NoConfig));
                }
            } else {
                String NoGroup = config.getString("messages.nogroup");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', NoGroup));
            }
        }
        return true;
    }

    private void sendCompactStats(Player sender, String playerName, PlayerData data) {
        List<String> format = config.getStringList("messages.stats.format");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{player}", playerName);

        String[] periods = {"day", "month", "allTime"};

        for (String period : periods) {
            int totalBan = data.getStatCountForPeriod(period, "/ban") +
                    data.getStatCountForPeriod(period, "/tempban");
            replacements.put("{total_ban_" + period + "}", String.valueOf(totalBan));

            int totalMute = data.getStatCountForPeriod(period, "/mute") +
                    data.getStatCountForPeriod(period, "/tempmute");
            replacements.put("{total_mute_" + period + "}", String.valueOf(totalMute));
        }

        for (String line : format) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                line = line.replace(entry.getKey(), entry.getValue());
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (staffWorkStates.containsKey(player.getName()) && staffWorkStates.get(player.getName()).isInStaffWork()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (staffWorkStates.containsKey(damager.getName()) && staffWorkStates.get(damager.getName()).isInStaffWork()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (staffWorkStates.containsKey(player.getName()) && staffWorkStates.get(player.getName()).isInStaffWork()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (staffWorkStates.containsKey(player.getName()) && staffWorkStates.get(player.getName()).isInStaffWork()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (staffWorkStates.containsKey(player.getName()) && staffWorkStates.get(player.getName()).isInStaffWork()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (staffWorkStates.containsKey(player.getName()) && staffWorkStates.get(player.getName()).isInStaffWork()) {
            ItemStack item = event.getItem();
            if (item != null) {
                List<String> blockedItems = config.getStringList("blocked-usage");
                if (blockedItems.contains(item.getType().toString())) {
                    event.setCancelled(true);
                    String NoUsage = config.getString("messages.nousage");
                    player.sendMessage(NoUsage);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        this.staffWorkStates.remove(playerName);
        this.saveStaffWorkStates();
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String commandName;
        int spaceIndex;
        Player player = event.getPlayer();
        String playerName = player.getName();
        String command = event.getMessage().toLowerCase();
        if (this.staffWorkStates.containsKey(playerName) && this.staffWorkStates.get(playerName).isInStaffWork() && (command.startsWith("/ban ") || command.startsWith("/mute ")) && (spaceIndex = command.indexOf(32)) > 0 && ("/ban".equals(commandName = command.substring(0, spaceIndex)) || "/tempban".equals(commandName) || "/mute".equals(commandName) || "/tempmute".equals(commandName))) {
            int currentCount = this.staffWorkStates.get(playerName).getStatCountForPeriod("day", commandName);
            this.staffWorkStates.get(playerName).setStatCountForPeriod("day", commandName, currentCount + 1);
            currentCount = this.staffWorkStates.get(playerName).getStatCountForPeriod("month", commandName);
            this.staffWorkStates.get(playerName).setStatCountForPeriod("month", commandName, currentCount + 1);
            currentCount = this.staffWorkStates.get(playerName).getStatCountForPeriod("allTime", commandName);
            this.staffWorkStates.get(playerName).setStatCountForPeriod("allTime", commandName, currentCount + 1);
            this.saveStaffWorkStates();
        }
    }

    private void loadStaffWorkStates() {
        this.staffWorkStates.clear();
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
        for (String key : dataConfig.getKeys(false)) {
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(key);
            if (playerSection == null) continue;
            PlayerData playerData = PlayerData.fromConfigSection(playerSection);
            this.staffWorkStates.put(key, playerData);
        }
    }

    private void saveStaffWorkStates() {
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
        for (Map.Entry<String, PlayerData> entry : this.staffWorkStates.entrySet()) {
            ConfigurationSection playerSection = entry.getValue().toConfigSection();
            dataConfig.set(entry.getKey(), playerSection);
        }
        try {
            dataConfig.save(this.dataFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetDailyStats() {
        for (PlayerData playerData : this.staffWorkStates.values()) {
            playerData.resetDailyStats();
        }
    }
}