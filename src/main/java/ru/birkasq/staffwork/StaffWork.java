package ru.birkasq.staffwork;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class StaffWork extends JavaPlugin {
    private PlayerDataManager dataManager;
    private FileConfiguration config;
    private File dataFile;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();
        this.dataFile = new File(getDataFolder(), "data.yml");

        this.dataManager = new PlayerDataManager(this);

        getCommand("staffwork").setExecutor(new StaffWorkCommand(this));
        getCommand("sw").setExecutor(new StaffWorkCommand(this));

        getServer().getPluginManager().registerEvents(new StaffWorkListener(this), this);

        getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                dataManager::resetDailyStats,
                20L,
                1728000L
        );
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getDataFile() {
        return dataFile;
    }
}