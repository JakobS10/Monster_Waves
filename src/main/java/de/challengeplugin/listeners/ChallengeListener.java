package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import de.challengeplugin.models.ArenaInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import java.util.UUID;

/**
 * Listener für Challenge-Events
 * FIX: Disconnect während Combat triggert jetzt korrekt Forfeit
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

        // Zähle Tod
        data.setTotalDeaths(data.getTotalDeaths() + 1);

        // Wave-Stat aktualisieren
        int currentWave = data.getCurrentWaveIndex();
        if (data.getWaveStats().containsKey(currentWave)) {
            data.getWaveStats().get(currentWave).deaths++;
        }

        player.sendMessage("§c§lDu bist gestorben!");
        player.sendMessage("§7Tode: §c" + data.getTotalDeaths());
    }

    /**
     * Spieler-Respawn während Challenge
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

        // Zähle Damage
        double damage = event.getFinalDamage();
        data.setTotalDamageTaken(data.getTotalDamageTaken() + damage);

        // Wave-Stat aktualisieren
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

        // Nur Mobs, keine Spieler
        if (entity instanceof Player) return;

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        // FIX: Auch ohne Killer muss der Mob gezählt werden!
        Player killer = entity.getKiller();
        UUID killerId = killer != null ? killer.getUniqueId() : null;

        // Informiere WaveManager (der findet das Team über Mob-Mapping)
        plugin.getChallengeManager().getWaveManager()
                .onMobDeath(killerId, entity.getUniqueId());
    }

    /**
     * FIX: Spieler disconnected während Combat
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        // Nur während Combat-Phase als Aufgeben werten
        if (challenge.getCurrentPhase() == Challenge.ChallengePhase.COMBAT) {
            // Disconnect = Aufgeben (nur wenn noch aktiv)
            if (!data.isHasCompleted() && !data.isHasForfeited()) {
                plugin.getLogger().info("[ChallengeListener] Spieler " + player.getName() + " hat während Combat disconnected -> Forfeit");
                plugin.getChallengeManager().onPlayerForfeited(player.getUniqueId());
            }
        }
    }
}