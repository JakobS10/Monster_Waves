package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import de.challengeplugin.models.ArenaInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;
import java.util.*;

/**
 * ERWEITERT:
 * - Keep-Inventory in Arena aktiviert
 * - 15 Sekunden Respawn-Cooldown im SPECTATOR-MODE
 * - Countdown-Title während Respawn
 * - Inventar bleibt erhalten!
 */
public class ChallengeListener implements Listener {

    private final ChallengePlugin plugin;

    // Tracking für Respawn-Cooldowns
    private final Map<UUID, Long> respawnCooldowns = new HashMap<>();
    private static final long RESPAWN_COOLDOWN_MS = 15000; // 15 Sekunden

    // Tracking für originale GameModes vor Cooldown
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();

    public ChallengeListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler-Tod während Challenge
     * NEU: Keep-Inventory aktiviert!
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) return;

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        // NEU: Keep-Inventory in Arena!
        ArenaInstance arena = plugin.getChallengeManager().getArenaManager()
                .getArenaForPlayer(player.getUniqueId());

        if (arena != null && plugin.getChallengeManager().getArenaManager()
                .isInArena(player.getLocation())) {
            // Verhindere Item-Drop - Items bleiben im Inventar!
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        }

        // Update Statistiken
        data.setTotalDeaths(data.getTotalDeaths() + 1);

        int currentWave = data.getCurrentWaveIndex();
        if (data.getWaveStats().containsKey(currentWave)) {
            data.getWaveStats().get(currentWave).deaths++;
        }

        player.sendMessage("§c§lDu bist gestorben!");
        player.sendMessage("§7Tode: §c" + data.getTotalDeaths());
        player.sendMessage("§e§lRespawn in 15 Sekunden...");

        // Setze Respawn-Cooldown
        respawnCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + RESPAWN_COOLDOWN_MS);
    }

    /**
     * Spieler-Respawn während Challenge
     * NEU: 15 Sekunden Cooldown im SPECTATOR-MODE + Countdown!
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

        // Starte Cooldown-Countdown
        startRespawnCooldown(player);
    }

    /**
     * NEU: Startet 15 Sekunden Respawn-Cooldown mit SPECTATOR-MODE
     * Inventar bleibt durch setKeepInventory(true) erhalten!
     */
    private void startRespawnCooldown(Player player) {
        Long cooldownEnd = respawnCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return;

        // Speichere originalen GameMode (sollte SURVIVAL sein)
        originalGameModes.put(player.getUniqueId(), player.getGameMode());

        // Setze Spectator-Mode während Cooldown
        // WICHTIG: Inventar bleibt trotzdem erhalten durch setKeepInventory!
        player.setGameMode(GameMode.SPECTATOR);

        player.sendMessage("§7§oDu bist im Spectator-Modus für 15 Sekunden...");
        player.sendMessage("§7§oFliege herum und beobachte den Kampf!");

        // Countdown-Task
        new BukkitRunnable() {
            int secondsLeft = 15;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup(player);
                    cancel();
                    return;
                }

                // Prüfe ob Cooldown vorbei
                if (System.currentTimeMillis() >= cooldownEnd) {
                    // Cooldown beendet!
                    GameMode original = originalGameModes.getOrDefault(player.getUniqueId(), GameMode.SURVIVAL);
                    player.setGameMode(original);

                    // Title: Ready!
                    player.showTitle(Title.title(
                            Component.text("§a§lBEREIT!"),
                            Component.text("§7Zurück im Kampf!"),
                            Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofSeconds(2),
                                    Duration.ofMillis(500)
                            )
                    ));

                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendMessage("§a§lDu kannst wieder kämpfen!");

                    cleanup(player);
                    cancel();
                    return;
                }

                // Zeige Countdown
                if (secondsLeft <= 15) {
                    // Title
                    player.showTitle(Title.title(
                            Component.text("§e§l" + secondsLeft),
                            Component.text("§7Spectator-Modus..."),
                            Title.Times.times(
                                    Duration.ofMillis(0),
                                    Duration.ofMillis(1100),
                                    Duration.ofMillis(0)
                            )
                    ));

                    // Sound (nur bei bestimmten Sekunden)
                    if (secondsLeft <= 5 || secondsLeft == 10 || secondsLeft == 15) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f,
                                secondsLeft <= 3 ? 1.5f : 1.0f);
                    }
                }

                secondsLeft--;
            }

            private void cleanup(Player player) {
                respawnCooldowns.remove(player.getUniqueId());
                originalGameModes.remove(player.getUniqueId());
            }
        }.runTaskTimer(plugin, 0L, 20L); // Jede Sekunde
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
                .onMobDeath(entity.getUniqueId());
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

        // Cleanup Cooldown
        respawnCooldowns.remove(player.getUniqueId());
        originalGameModes.remove(player.getUniqueId());
    }
}