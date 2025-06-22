package ru.birkasq.staffwork;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

public class PlayerData {
    private boolean inStaffWork = false;
    private Map<String, Integer> dayStats = new HashMap<String, Integer>();
    private Map<String, Integer> monthStats = new HashMap<String, Integer>();
    private Map<String, Integer> allTimeStats = new HashMap<String, Integer>();

    public boolean isInStaffWork() {
        return this.inStaffWork;
    }

    public void setInStaffWork(boolean inStaffWork) {
        this.inStaffWork = inStaffWork;
    }

    public int getStatCountForPeriod(String period, String action) {
        if ("day".equals(period)) {
            return this.dayStats.getOrDefault(action, 0);
        }
        if ("month".equals(period)) {
            return this.monthStats.getOrDefault(action, 0);
        }
        if ("allTime".equals(period)) {
            return this.allTimeStats.getOrDefault(action, 0);
        }
        return 0;
    }

    public void setStatCountForPeriod(String period, String action, int count) {
        if ("day".equals(period)) {
            this.dayStats.put(action, count);
        } else if ("month".equals(period)) {
            this.monthStats.put(action, count);
        } else if ("allTime".equals(period)) {
            this.allTimeStats.put(action, count);
        }
    }

    public void resetDailyStats() {
        this.dayStats.clear();
    }

    public void resetMonthlyStats() {
        this.monthStats.clear();
    }

    public void saveStatsForPeriod(String period, ConfigurationSection section) {
        Map<String, Integer> stats;
        if ("day".equals(period)) {
            stats = this.dayStats;
        } else if ("month".equals(period)) {
            stats = this.monthStats;
        } else if ("allTime".equals(period)) {
            stats = this.allTimeStats;
        } else {
            return;
        }
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
    }

    public static PlayerData fromConfigSection(ConfigurationSection section) {
        ConfigurationSection allTimeSection;
        ConfigurationSection monthSection;
        PlayerData playerData = new PlayerData();
        playerData.setInStaffWork(section.getBoolean("inStaffWork"));
        ConfigurationSection daySection = section.getConfigurationSection("day");
        if (daySection != null) {
            for (Object action : daySection.getKeys(false)) {
                playerData.setStatCountForPeriod("day", (String)action, daySection.getInt((String)action));
            }
        }
        if ((monthSection = section.getConfigurationSection("month")) != null) {
            for (String action : monthSection.getKeys(false)) {
                playerData.setStatCountForPeriod("month", action, monthSection.getInt(action));
            }
        }
        if ((allTimeSection = section.getConfigurationSection("allTime")) != null) {
            for (String action : allTimeSection.getKeys(false)) {
                playerData.setStatCountForPeriod("allTime", action, allTimeSection.getInt(action));
            }
        }
        return playerData;
    }

    public ConfigurationSection toConfigSection() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("inStaffWork", this.inStaffWork);
        ConfigurationSection daySection = section.createSection("day");
        for (Map.Entry<String, Integer> entry : this.dayStats.entrySet()) {
            daySection.set(entry.getKey(), entry.getValue());
        }
        ConfigurationSection monthSection = section.createSection("month");
        for (Map.Entry<String, Integer> entry : this.monthStats.entrySet()) {
            monthSection.set(entry.getKey(), entry.getValue());
        }
        ConfigurationSection configurationSection = section.createSection("allTime");
        for (Map.Entry<String, Integer> entry : this.allTimeStats.entrySet()) {
            configurationSection.set(entry.getKey(), entry.getValue());
        }
        return section;
    }
}
