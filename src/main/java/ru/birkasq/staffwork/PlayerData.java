package ru.birkasq.staffwork;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private boolean inStaffWork = false;
    private Map<String, Integer> dayStats = new HashMap<>();
    private Map<String, Integer> monthStats = new HashMap<>();
    private Map<String, Integer> allTimeStats = new HashMap<>();

    public boolean isInStaffWork() {
        return inStaffWork;
    }

    public void setInStaffWork(boolean inStaffWork) {
        this.inStaffWork = inStaffWork;
    }

    public int getStatCountForPeriod(String period, String action) {
        if ("day".equals(period)) {
            return dayStats.getOrDefault(action, 0);
        }
        if ("month".equals(period)) {
            return monthStats.getOrDefault(action, 0);
        }
        if ("allTime".equals(period)) {
            return allTimeStats.getOrDefault(action, 0);
        }
        return 0;
    }

    public void setStatCountForPeriod(String period, String action, int count) {
        if ("day".equals(period)) {
            dayStats.put(action, count);
        } else if ("month".equals(period)) {
            monthStats.put(action, count);
        } else if ("allTime".equals(period)) {
            allTimeStats.put(action, count);
        }
    }

    public void resetDailyStats() {
        dayStats.clear();
    }

    public ConfigurationSection toConfigSection() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("inStaffWork", inStaffWork);

        ConfigurationSection daySection = section.createSection("day");
        for (Map.Entry<String, Integer> entry : dayStats.entrySet()) {
            daySection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection monthSection = section.createSection("month");
        for (Map.Entry<String, Integer> entry : monthStats.entrySet()) {
            monthSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection allTimeSection = section.createSection("allTime");
        for (Map.Entry<String, Integer> entry : allTimeStats.entrySet()) {
            allTimeSection.set(entry.getKey(), entry.getValue());
        }

        return section;
    }

    public static PlayerData fromConfigSection(ConfigurationSection section) {
        PlayerData playerData = new PlayerData();
        playerData.setInStaffWork(section.getBoolean("inStaffWork"));

        ConfigurationSection daySection = section.getConfigurationSection("day");
        if (daySection != null) {
            for (String action : daySection.getKeys(false)) {
                playerData.setStatCountForPeriod("day", action, daySection.getInt(action));
            }
        }

        ConfigurationSection monthSection = section.getConfigurationSection("month");
        if (monthSection != null) {
            for (String action : monthSection.getKeys(false)) {
                playerData.setStatCountForPeriod("month", action, monthSection.getInt(action));
            }
        }

        ConfigurationSection allTimeSection = section.getConfigurationSection("allTime");
        if (allTimeSection != null) {
            for (String action : allTimeSection.getKeys(false)) {
                playerData.setStatCountForPeriod("allTime", action, allTimeSection.getInt(action));
            }
        }

        return playerData;
    }
}