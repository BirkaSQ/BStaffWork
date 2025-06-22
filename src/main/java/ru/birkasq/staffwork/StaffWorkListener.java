package ru.birkasq.staffwork;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StaffWorkListener implements Listener {
    private final PlayerDataManager dataManager;
    private final FileConfiguration config;

    public StaffWorkListener(StaffWork plugin) {
        this.dataManager = plugin.getDataManager();
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (dataManager.isInStaffWork(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        if (dataManager.isInStaffWork(damager.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (dataManager.isInStaffWork(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (dataManager.isInStaffWork(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (dataManager.isInStaffWork(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (dataManager.isInStaffWork(player.getName())) {
            ItemStack item = event.getItem();
            if (item != null) {
                List<String> blockedItems = config.getStringList("blocked-usage");
                if (blockedItems.contains(item.getType().toString())) {
                    event.setCancelled(true);
                    String noUsage = config.getString("messages.nousage");
                    player.sendMessage(noUsage);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataManager.removePlayerData(player.getName());
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!dataManager.isInStaffWork(playerName)) return;

        String command = event.getMessage().toLowerCase();
        if (command.startsWith("/ban ") || command.startsWith("/mute ")) {
            int spaceIndex = command.indexOf(32);
            if (spaceIndex > 0) {
                String commandName = command.substring(0, spaceIndex);
                if (commandName.equals("/ban") || commandName.equals("/tempban") ||
                        commandName.equals("/mute") || commandName.equals("/tempmute")) {

                    dataManager.incrementCommandStats(playerName, commandName);
                }
            }
        }
    }
}