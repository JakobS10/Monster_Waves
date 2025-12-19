package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import de.challengeplugin.models.ArenaInstance;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * ERWEITERT: Gibt Backpack-Item bei Respawn zurück
 */
public class ChallengeListener implements Listener {

    private final ChallengePlugin plugin;

    public ChallengeListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler-Tod während Challenge
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        data.setTotalDeaths(data.getTotalDeaths() + 1);

        int currentWave = data.getCurrentWaveIndex();
        if (data.getWaveStats().containsKey(currentWave)) {
            data.getWaveStats().get(currentWave).deaths++;
        }

        player.sendMessage("§c§lDu bist gestorben!");
        player.sendMessage("§7Tode: §c" + data.getTotalDeaths());
    }

    /**
     * Spieler-Respawn während Challenge
     * NEU: Gibt Backpack-Item zurück!
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        // Teleportiere zurück in Arena
        ArenaInstance arena = plugin.getChallengeManager().getArenaManager()
                .getArenaForPlayer(player.getUniqueId());

        if (arena != null) {
            event.setRespawnLocation(arena.getSpawnPoint());
        }


        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§aDas würde mir so massiv stinken! Ganz viele liebe Grüße vom völlig unterbezahten Plugin-Ersteller!");
        }, 1L);
    }

    /**
     * Spieler nimmt Schaden
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        double damage = event.getFinalDamage();
        data.setTotalDamageTaken(data.getTotalDamageTaken() + damage);

        int currentWave = data.getCurrentWaveIndex();
        if (data.getWaveStats().containsKey(currentWave)) {
            data.getWaveStats().get(currentWave).damageTaken += damage;
        }
    }

    /**
     * Mob stirbt
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player) return;

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        Player killer = entity.getKiller();
        UUID killerId = killer != null ? killer.getUniqueId() : null;

        plugin.getChallengeManager().getWaveManager()
                .onMobDeath(killerId, entity.getUniqueId());
    }

    /**
     * Spieler disconnected während Combat
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        if (challenge.getCurrentPhase() == Challenge.ChallengePhase.COMBAT) {
            if (!data.isHasCompleted() && !data.isHasForfeited()) {
                plugin.getLogger().info("[ChallengeListener] Spieler " + player.getName() + " hat während Combat disconnected -> Forfeit");
                plugin.getChallengeManager().onPlayerForfeited(player.getUniqueId());
            }
        }
    }
}