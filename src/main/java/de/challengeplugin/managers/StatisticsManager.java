package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verwaltet Statistiken und Auswertung
 * FIX: Spieler die aufgegeben haben erscheinen NICHT in der Wertung!
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
        for (UUID playerId : challenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§6§l=================================");
                player.sendMessage("§e§l      CHALLENGE BEENDET!");
                player.sendMessage("§6§l=================================");
                player.sendMessage("");

                openEvaluationGUI(player, challenge, SortCriteria.FASTEST);
            }
        }
    }

    /**
     * Öffnet Auswertungs-GUI mit gewähltem Filter
     */
    public void openEvaluationGUI(Player player, Challenge challenge, SortCriteria criteria) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lChallenge-Auswertung");

        // Filter-Buttons
        ItemStack fastestItem = createFilterItem(Material.GOLDEN_BOOTS,
                "§e§lSchnellster",
                "§7Sortiert nach kürzester Zeit",
                criteria == SortCriteria.FASTEST);

        ItemStack leastDeathsItem = createFilterItem(Material.TOTEM_OF_UNDYING,
                "§a§lWenigste Tode",
                "§7Sortiert nach geringster Todesanzahl",
                criteria == SortCriteria.LEAST_DEATHS);

        ItemStack leastDamageItem = createFilterItem(Material.SHIELD,
                "§c§lWenigster Schaden",
                "§7Sortiert nach geringstem Damage",
                criteria == SortCriteria.LEAST_DAMAGE);

        inv.setItem(10, fastestItem);
        inv.setItem(12, leastDeathsItem);
        inv.setItem(14, leastDamageItem);

        fillRankingData(inv, challenge, criteria);

        player.openInventory(inv);
    }

    /**
     * Füllt Ranking-Daten in GUI
     * FIX: Nur Spieler die COMPLETED haben (nicht FORFEITED)!
     */
    private void fillRankingData(Inventory inv, Challenge challenge, SortCriteria criteria) {
        // FIX: Filter NUR Spieler die abgeschlossen haben (nicht aufgegeben!)
        List<PlayerChallengeData> winners = challenge.getPlayerData().values().stream()
                .filter(PlayerChallengeData::isHasCompleted)
                .filter(d -> !d.isHasForfeited()) // WICHTIG: Aufgegebene rausfiltern!
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

        int slot = 27;
        int rank = 1;
        for (PlayerChallengeData data : winners) {
            if (slot >= 54 || rank > 10) break;

            Player p = Bukkit.getPlayer(data.getPlayerId());
            String playerName = p != null ? p.getName() : "Unbekannt";

            // Echter Spieler-Kopf mit SkullMeta
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

            // Setze Owner für echten Skin
            if (p != null) {
                skullMeta.setOwningPlayer(p);
            }

            // Färbe Platzierung
            String rankColor = rank == 1 ? "§6" : rank == 2 ? "§7" : rank == 3 ? "§c" : "§e";
            skullMeta.setDisplayName(rankColor + "#" + rank + " §7- §f" + playerName);

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

            skullMeta.setLore(lore);
            playerHead.setItemMeta(skullMeta);

            inv.setItem(slot, playerHead);
            slot++;
            rank++;
        }

        // Zeige Info wenn niemand abgeschlossen hat
        if (winners.isEmpty()) {
            ItemStack noWinner = new ItemStack(Material.BARRIER);
            ItemMeta meta = noWinner.getItemMeta();
            meta.setDisplayName("§c§lKeine Gewinner");
            meta.setLore(Arrays.asList(
                    "§7Niemand hat die Challenge",
                    "§7erfolgreich abgeschlossen",
                    "",
                    "§c§oAufgegebene Spieler werden nicht gewertet"
            ));
            noWinner.setItemMeta(meta);
            inv.setItem(31, noWinner);
        }
    }

    /**
     * Erstellt Filter-Button mit Highlight für aktiven Filter
     */
    private ItemStack createFilterItem(Material material, String name, String description, boolean isActive) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(description);
        lore.add("");

        if (isActive) {
            lore.add("§a§l✓ Aktiv");
            meta.setEnchantmentGlintOverride(true);
        } else {
            lore.add("§7Klicke zum Anzeigen");
        }

        meta.setLore(lore);
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
        FASTEST("§e§lSchnellster"),
        LEAST_DEATHS("§a§lWenigste Tode"),
        LEAST_DAMAGE("§c§lWenigster Schaden");

        private final String displayName;

        SortCriteria(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}