package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verwaltet Statistiken und Auswertung
 * Zeigt Ranglisten nach verschiedenen Kriterien
 */
public class StatisticsManager {

    private final ChallengePlugin plugin;

    public StatisticsManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Zeigt Auswertungs-GUI für alle Spieler
     */
    public void showEvaluation(Challenge challenge) {
        // Broadcast an alle Teilnehmer
        for (UUID playerId : challenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§6§l=================================");
                player.sendMessage("§e§l      CHALLENGE BEENDET!");
                player.sendMessage("§6§l=================================");
                player.sendMessage("");

                // Öffne Auswertungs-GUI
                openEvaluationGUI(player, challenge);
            }
        }
    }

    /**
     * Öffnet Auswertungs-GUI
     */
    public void openEvaluationGUI(Player player, Challenge challenge) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lChallenge-Auswertung");

        // Filter-Buttons
        ItemStack fastestItem = createFilterItem(Material.GOLDEN_BOOTS,
                "§e§lSchnellster",
                "§7Sortiert nach kürzester Zeit");
        ItemStack leastDeathsItem = createFilterItem(Material.TOTEM_OF_UNDYING,
                "§a§lWenigste Tode",
                "§7Sortiert nach geringster Todesanzahl");
        ItemStack leastDamageItem = createFilterItem(Material.SHIELD,
                "§c§lWenigster Schaden",
                "§7Sortiert nach geringstem Damage");

        inv.setItem(10, fastestItem);
        inv.setItem(12, leastDeathsItem);
        inv.setItem(14, leastDamageItem);

        // Standard: Zeige "Schnellste"
        fillRankingData(inv, challenge, SortCriteria.FASTEST);

        player.openInventory(inv);
    }

    /**
     * Füllt Ranking-Daten in GUI
     */
    private void fillRankingData(Inventory inv, Challenge challenge, SortCriteria criteria) {
        // Filtere nur Gewinner (completed)
        List<PlayerChallengeData> winners = challenge.getPlayerData().values().stream()
                .filter(PlayerChallengeData::isHasCompleted)
                .collect(Collectors.toList());

        // Sortiere nach Kriterium
        switch (criteria) {
            case FASTEST:
                winners.sort(Comparator.comparingLong(d ->
                        d.getCombatEndTick() - d.getCombatStartTick()));
                break;
            case LEAST_DEATHS:
                winners.sort(Comparator.comparingInt(PlayerChallengeData::getTotalDeaths));
                break;
            case LEAST_DAMAGE:
                winners.sort(Comparator.comparingDouble(PlayerChallengeData::getTotalDamageTaken));
                break;
        }

        // Zeige Top 10 (oder weniger)
        int slot = 27;
        int rank = 1;
        for (PlayerChallengeData data : winners) {
            if (slot >= 54 || rank > 10) break;

            Player p = Bukkit.getPlayer(data.getPlayerId());
            String playerName = p != null ? p.getName() : "Unbekannt";

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            meta.setDisplayName("§e#" + rank + " §7- §6" + playerName);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Zeit: §e" + formatTicks(data.getCombatEndTick() - data.getCombatStartTick()));
            lore.add("§7Tode: §c" + data.getTotalDeaths());
            lore.add("§7Schaden: §c" + String.format("%.1f", data.getTotalDamageTaken()) + " HP");
            lore.add("");

            // Wave-Details
            lore.add("§7§lWave-Details:");
            for (int i = 0; i < 3; i++) {
                if (data.getWaveStats().containsKey(i)) {
                    PlayerChallengeData.WaveStats waveStats = data.getWaveStats().get(i);
                    long waveTime = waveStats.endTick - waveStats.startTick;
                    lore.add("  §7Wave " + (i+1) + ": §e" + formatTicks(waveTime) +
                            " §7(§c" + waveStats.deaths + " Tode§7)");
                }
            }

            meta.setLore(lore);
            playerHead.setItemMeta(meta);

            inv.setItem(slot, playerHead);
            slot++;
            rank++;
        }
    }

    /**
     * Erstellt Filter-Button
     */
    private ItemStack createFilterItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("", description, "", "§7Klicke zum Anzeigen"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Formatiert Ticks zu Zeit-String
     */
    private String formatTicks(long ticks) {
        return DataManager.formatTime(ticks);
    }

    public enum SortCriteria {
        FASTEST,
        LEAST_DEATHS,
        LEAST_DAMAGE
    }
}