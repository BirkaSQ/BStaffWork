package ru.birkasq.staffwork;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerDataManager {
    private final StaffWork plugin;
    private final Map<String, PlayerData> staffWorkStates = new HashMap<>();

    public PlayerDataManager(StaffWork plugin) {
        this.plugin = plugin;
        loadStaffWorkStates();
    }

    public boolean isInStaffWork(String playerName) {
        return staffWorkStates.containsKey(playerName) &&
                staffWorkStates.get(playerName).isInStaffWork();
    }

    public void setStaffWorkStatus(String playerName, boolean status) {
        if (!staffWorkStates.containsKey(playerName)) {
            staffWorkStates.put(playerName, new PlayerData());
        }
        staffWorkStates.get(playerName).setInStaffWork(status);
        saveStaffWorkStates();
    }

    public boolean hasPlayerData(String playerName) {
        return staffWorkStates.containsKey(playerName);
    }

    public void sendCompactStats(Player player, String targetName) {
        PlayerData data = staffWorkStates.get(targetName);
        List<String> format = plugin.getConfig().getStringList("messages.stats.format");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{player}", targetName);

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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    public void incrementCommandStats(String playerName, String commandName) {
        PlayerData data = staffWorkStates.get(playerName);
        int dayCount = data.getStatCountForPeriod("day", commandName);
        data.setStatCountForPeriod("day", commandName, dayCount + 1);

        int monthCount = data.getStatCountForPeriod("month", commandName);
        data.setStatCountForPeriod("month", commandName, monthCount + 1);

        int allTimeCount = data.getStatCountForPeriod("allTime", commandName);
        data.setStatCountForPeriod("allTime", commandName, allTimeCount + 1);

        saveStaffWorkStates();
    }

    public void removePlayerData(String playerName) {
        staffWorkStates.remove(playerName);
        saveStaffWorkStates();
    }

    public void resetDailyStats() {
        staffWorkStates.values().forEach(PlayerData::resetDailyStats);
        saveStaffWorkStates();
    }

    private void loadStaffWorkStates() {
        staffWorkStates.clear();
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(plugin.getDataFile());

        for (String key : dataConfig.getKeys(false)) {
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(key);
            if (playerSection == null) continue;
            PlayerData playerData = PlayerData.fromConfigSection(playerSection);
            staffWorkStates.put(key, playerData);
        }
    }

    private void saveStaffWorkStates() {
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(plugin.getDataFile());

        for (Map.Entry<String, PlayerData> entry : staffWorkStates.entrySet()) {
            ConfigurationSection playerSection = entry.getValue().toConfigSection();
            dataConfig.set(entry.getKey(), playerSection);
        }

        try {
            dataConfig.save(plugin.getDataFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}