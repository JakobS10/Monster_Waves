package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.ArenaManager;
import de.challengeplugin.models.ArenaInstance;
import de.challengeplugin.models.Challenge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;

/**
 * Schützt Arenen vor Veränderungen
 * Verhindert Block-Break, Teleport-Escape, etc.
 */
public class ArenaProtectionListener implements Listener {

    private final ChallengePlugin plugin;

    public ArenaProtectionListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verhindert Block-Break in Arena
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        ArenaManager am = plugin.getChallengeManager().getArenaManager();
        if (am.isInArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Block-Place in Arena
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        ArenaManager am = plugin.getChallengeManager().getArenaManager();
        if (am.isInArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Teleport aus Arena
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        Player player = event.getPlayer();
        ArenaManager am = plugin.getChallengeManager().getArenaManager();

        // Wenn Spieler in Arena ist und versucht rauszuteleportieren
        if (am.isInArena(event.getFrom()) && !am.isInArena(event.getTo())) {
            // Erlaube nur für Spectators
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst die Arena nicht verlassen!");
            }
        }
    }

    /**
     * Verhindert Movement aus Arena (zusätzliche Sicherheit)
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        Player player = event.getPlayer();

        // Nur für kämpfende Spieler
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        ArenaManager am = plugin.getChallengeManager().getArenaManager();
        ArenaInstance arena = am.getArenaForPlayer(player.getUniqueId());

        if (arena == null) return;

        // Prüfe ob Spieler Arena-Grenzen verlassen würde
        if (!isInArenaBounds(event.getTo(), arena)) {
            event.setCancelled(true);
            player.sendMessage("§cDu kannst die Arena nicht verlassen!");
        }
    }

    /**
     * Prüft ob Location in Arena-Grenzen
     */
    private boolean isInArenaBounds(org.bukkit.Location loc, ArenaInstance arena) {
        org.bukkit.Location center = arena.getCenterLocation();
        double halfX = arena.getSizeX() / 2.0;
        double halfZ = arena.getSizeZ() / 2.0;

        return Math.abs(loc.getX() - center.getX()) <= halfX
                && Math.abs(loc.getZ() - center.getZ()) <= halfZ
                && loc.getY() >= center.getY()
                && loc.getY() <= center.getY() + arena.getSizeY();
    }
}