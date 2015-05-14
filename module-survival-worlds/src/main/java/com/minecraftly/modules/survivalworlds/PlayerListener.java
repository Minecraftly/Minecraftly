package com.minecraftly.modules.survivalworlds;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.survivalworlds.PacketPlayerWorld;
import com.minecraftly.modules.survivalworlds.data.DataStore;
import com.minecraftly.modules.survivalworlds.data.PlayerWorldData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener {

    private SurvivalWorldsModule module;
    private DataStore dataStore;
    private Cache<UUID, UUID> joinQueue = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public PlayerListener(SurvivalWorldsModule module) {
        this.module = module;
        this.dataStore = module.getDataStore();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();
        UUID worldUUID = joinQueue.getIfPresent(playerUUID);
        if (worldUUID == null) {
            worldUUID = playerUUID;
        }

        final UUID finalWorldUUID = worldUUID;
        Bukkit.getScheduler().runTask(module.getBukkitPlugin(), new Runnable() {
            @Override
            public void run() {
                joinWorld(player, module.getWorld(finalWorldUUID));
            }
        });
    }

    @PacketHandler
    public void onPacketJoinWorld(PacketPlayerWorld packet) {
        UUID playerUUID = packet.getPlayer();
        UUID worldUUID = packet.getWorld();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            joinWorld(player, module.getWorld(worldUUID));
        } else if (!playerUUID.equals(worldUUID)) {
            joinQueue.put(playerUUID, worldUUID);
        }
    }

    public void joinWorld(Player player, World world) {
        Preconditions.checkNotNull(player);
        UUID playerUUID = player.getUniqueId();

        if (world == null) {
            player.kickPlayer(ChatColor.RED + "Unable to load world.");
            module.getLogger().log(Level.SEVERE, "Unable to load world for player " + playerUUID);
            return;
        }

        joinQueue.invalidate(playerUUID);
        Location spawnLocation;
        PlayerWorldData playerWorldData = dataStore.getPlayerWorldData(world, player);

        // todo util method for player data
        if (playerWorldData != null && playerWorldData.getLastLocation() != null) {
            spawnLocation = playerWorldData.getLastLocation();
        } else {
            spawnLocation = BukkitUtilities.getSafeLocation(world.getSpawnLocation());
        }

        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();

        // todo remove???
        if (module.isSurvivalWorld(world)) {

        }

        checkWorldForUnloadDelayed(e.getPlayer().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        World from = e.getFrom().getWorld();
        World to = e.getTo().getWorld();

        if (!from.equals(to)) {
            checkWorldForUnloadDelayed(from);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        // e.setRespawnLocation(e.getPlayer().getWorld()); todo
    }

    public void checkWorldForUnloadDelayed(final World world) {
        Bukkit.getScheduler().runTaskLater(module.getBukkitPlugin(), new Runnable() {
            @Override
            public void run() {
                checkWorldForUnload(world);
            }
        }, 5L);
    }

    public void checkWorldForUnload(World world) {
        if (module.isSurvivalWorld(world) && world.getPlayers().size() == 0) {
            Bukkit.unloadWorld(world, true);
        }
    }

}
