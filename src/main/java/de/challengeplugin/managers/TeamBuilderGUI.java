package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * GUI-System für manuelles Team-Building
 * Erlaubt dem Boss, Spieler manuell zu Teams zuzuweisen
 */
public class TeamBuilderGUI {

    private final ChallengePlugin plugin;
    private final Player boss;
    private final Challenge challenge;

    // Temporäre Team-Daten während des Builds
    private final Map<Integer, List<UUID>> teams;
    private final List<UUID> unassignedPlayers;
    private final int maxTeamSize;
    private final int maxTeams;

    // GUI-State
    private int currentPage = 0;

    public TeamBuilderGUI(ChallengePlugin plugin, Player boss, Challenge challenge) {
        this.plugin = plugin;
        this.boss = boss;
        this.challenge = challenge;
        this.maxTeamSize = challenge.getTeamMode().getTeamSize();

        // Berechne max Teams
        List<UUID> allParticipants = new ArrayList<>(challenge.getParticipants());
        if (!challenge.isBossParticipates()) {
            allParticipants.remove(challenge.getBossPlayerId());
        }

        this.maxTeams = (int) Math.ceil((double) allParticipants.size() / maxTeamSize);
        this.teams = new HashMap<>();
        this.unassignedPlayers = new ArrayList<>(allParticipants);

        // Initialisiere leere Teams
        for (int i = 0; i < maxTeams; i++) {
            teams.put(i, new ArrayList<>());
        }
    }

    /**
     * Öffnet die Haupt-Team-Builder-GUI
     */
    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lTeam-Builder");

        // Zeile 1-3: Teams anzeigen (max 3 Teams pro Seite)
        int startTeam = currentPage * 3;
        int endTeam = Math.min(startTeam + 3, maxTeams);

        for (int teamIndex = startTeam; teamIndex < endTeam; teamIndex++) {
            int displaySlot = 10 + ((teamIndex - startTeam) * 2);
            renderTeam(inv, teamIndex, displaySlot);
        }

        // Zeile 4: Nicht zugewiesene Spieler
        renderUnassignedPlayers(inv);

        // Zeile 5: Navigation & Bestätigung
        if (currentPage > 0) {
            ItemStack prev = createItem(Material.ARROW, "§e← Vorherige Seite", null);
            inv.setItem(45, prev);
        }

        if (endTeam < maxTeams) {
            ItemStack next = createItem(Material.ARROW, "§eNächste Seite →", null);
            inv.setItem(53, next);
        }

        // Auto-Fill Button
        ItemStack autoFill = createItem(Material.GOLDEN_APPLE, "§6§lAuto-Fill", Arrays.asList(
                "§7Verteilt Spieler automatisch",
                "§7gleichmäßig auf Teams"
        ));
        inv.setItem(48, autoFill);

        // Reset Button
        ItemStack reset = createItem(Material.BARRIER, "§c§lZurücksetzen", Arrays.asList(
                "§7Setzt alle Teams zurück"
        ));
        inv.setItem(49, reset);

        // Bestätigen Button (nur wenn alle zugewiesen)
        if (unassignedPlayers.isEmpty()) {
            ItemStack confirm = createItem(Material.LIME_WOOL, "§a§lBestätigen", Arrays.asList(
                    "§7Teams sind vollständig",
                    "§7Klicke um fortzufahren"
            ));
            inv.setItem(50, confirm);
        } else {
            ItemStack warn = createItem(Material.ORANGE_WOOL, "§e§lNoch nicht fertig", Arrays.asList(
                    "§c" + unassignedPlayers.size() + " Spieler noch nicht zugewiesen!"
            ));
            inv.setItem(50, warn);
        }

        boss.openInventory(inv);
    }

    /**
     * Rendert ein Team in der GUI
     */
    private void renderTeam(Inventory inv, int teamIndex, int startSlot) {
        List<UUID> teamMembers = teams.get(teamIndex);

        // Team-Header
        ItemStack header = new ItemStack(Material.WHITE_BANNER);
        ItemMeta headerMeta = header.getItemMeta();
        headerMeta.setDisplayName("§e§lTeam " + (teamIndex + 1));
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Größe: §e" + teamMembers.size() + "§7/§e" + maxTeamSize);
        headerLore.add("");
        if (teamMembers.isEmpty()) {
            headerLore.add("§7Keine Mitglieder");
        } else {
            for (UUID memberId : teamMembers) {
                Player p = Bukkit.getPlayer(memberId);
                headerLore.add("§7- §e" + (p != null ? p.getName() : "???"));
            }
        }
        headerLore.add("");
        headerLore.add("§7Klicke auf einen Spieler unten");
        headerLore.add("§7um ihn zu diesem Team hinzuzufügen");
        headerMeta.setLore(headerLore);
        header.setItemMeta(headerMeta);
        inv.setItem(startSlot, header);

        // Team-Mitglieder (darunter)
        for (int i = 0; i < teamMembers.size() && i < 3; i++) {
            UUID memberId = teamMembers.get(i);
            Player p = Bukkit.getPlayer(memberId);
            String name = p != null ? p.getName() : "???";

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            meta.setDisplayName("§e" + name);
            meta.setLore(Arrays.asList(
                    "§7Rechtsklick: §cAus Team entfernen"
            ));
            playerHead.setItemMeta(meta);
            inv.setItem(startSlot + 9 + i, playerHead);
        }
    }

    /**
     * Rendert nicht zugewiesene Spieler
     */
    private void renderUnassignedPlayers(Inventory inv) {
        // Label
        ItemStack label = createItem(Material.CHEST, "§6§lNicht zugewiesen (" + unassignedPlayers.size() + ")", null);
        inv.setItem(36, label);

        // Spieler (max 8 anzeigen)
        for (int i = 0; i < Math.min(unassignedPlayers.size(), 8); i++) {
            UUID playerId = unassignedPlayers.get(i);
            Player p = Bukkit.getPlayer(playerId);
            String name = p != null ? p.getName() : "???";

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            meta.setDisplayName("§e" + name);
            meta.setLore(Arrays.asList(
                    "§7Linksklick: §aZu Team hinzufügen",
                    "§7(wähle erst ein Team oben)"
            ));
            playerHead.setItemMeta(meta);
            inv.setItem(37 + i, playerHead);
        }
    }

    /**
     * Hilfsmethode zum Erstellen von Items
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handhabt Klicks in der GUI
     */
    public boolean handleClick(int slot, boolean isRightClick, boolean isShiftClick) {
        // Navigation
        if (slot == 45) { // Zurück
            if (currentPage > 0) {
                currentPage--;
                open();
            }
            return true;
        }
        if (slot == 53) { // Weiter
            currentPage++;
            open();
            return true;
        }

        // Auto-Fill
        if (slot == 48) {
            autoFillTeams();
            open();
            return true;
        }

        // Reset
        if (slot == 49) {
            resetTeams();
            open();
            return true;
        }

        // Bestätigen
        if (slot == 50 && unassignedPlayers.isEmpty()) {
            applyTeamsToChallenge();
            boss.closeInventory();
            return true;
        }

        // Team-Header klicken (Slots 10, 12, 14 für Teams)
        if (slot == 10 || slot == 12 || slot == 14) {
            int teamIndex = currentPage * 3 + ((slot - 10) / 2);
            boss.sendMessage("§eTeam " + (teamIndex + 1) + " ausgewählt. Klicke nun auf einen Spieler unten.");
            // Speichere ausgewähltes Team in Metadata
            boss.setMetadata("selectedTeam", new org.bukkit.metadata.FixedMetadataValue(plugin, teamIndex));
            return true;
        }

        // Team-Mitglied klicken (Rechtsklick zum Entfernen)
        if ((slot >= 19 && slot <= 21) || (slot >= 21 && slot <= 23) || (slot >= 23 && slot <= 25)) {
            if (isRightClick) {
                removePlayerFromTeams(slot);
                open();
            }
            return true;
        }

        // Nicht zugewiesener Spieler klicken
        if (slot >= 37 && slot <= 44) {
            int playerIndex = slot - 37;
            if (playerIndex < unassignedPlayers.size()) {
                assignPlayer(playerIndex);
                open();
            }
            return true;
        }
        return false;
    }

    /**
     * Weist einen Spieler einem Team zu
     */
    private void assignPlayer(int playerIndex) {
        if (!boss.hasMetadata("selectedTeam")) {
            boss.sendMessage("§cWähle erst ein Team aus (klicke auf ein Team-Banner)!");
            return;
        }

        int teamIndex = boss.getMetadata("selectedTeam").get(0).asInt();
        UUID playerId = unassignedPlayers.get(playerIndex);

        // Prüfe ob Team voll ist
        if (teams.get(teamIndex).size() >= maxTeamSize) {
            boss.sendMessage("§cTeam " + (teamIndex + 1) + " ist bereits voll!");
            return;
        }

        // Füge hinzu
        teams.get(teamIndex).add(playerId);
        unassignedPlayers.remove(playerId);

        Player p = Bukkit.getPlayer(playerId);
        boss.sendMessage("§a" + (p != null ? p.getName() : "Spieler") + " zu Team " + (teamIndex + 1) + " hinzugefügt!");
        boss.removeMetadata("selectedTeam", plugin);
    }

    /**
     * Entfernt Spieler aus Team
     */
    private void removePlayerFromTeams(int slot) {
        // Finde Spieler basierend auf Slot
        // (Komplex, vereinfacht: durchsuche alle Teams)
        for (Map.Entry<Integer, List<UUID>> entry : teams.entrySet()) {
            for (UUID playerId : new ArrayList<>(entry.getValue())) {
                // Wenn gefunden, entferne
                entry.getValue().remove(playerId);
                unassignedPlayers.add(playerId);

                Player p = Bukkit.getPlayer(playerId);
                boss.sendMessage("§e" + (p != null ? p.getName() : "Spieler") + " aus Team entfernt");
                return;
            }
        }
    }

    /**
     * Auto-Fill: Verteilt Spieler gleichmäßig
     */
    private void autoFillTeams() {
        // Resette erst
        resetTeams();

        // Verteile gleichmäßig
        int teamIndex = 0;
        for (UUID playerId : new ArrayList<>(unassignedPlayers)) {
            teams.get(teamIndex).add(playerId);
            unassignedPlayers.remove(playerId);

            teamIndex = (teamIndex + 1) % maxTeams;
        }

        boss.sendMessage("§aSpieler wurden automatisch verteilt!");
    }

    /**
     * Reset: Alle Spieler zurück zu unassigned
     */
    private void resetTeams() {
        for (List<UUID> team : teams.values()) {
            unassignedPlayers.addAll(team);
            team.clear();
        }
        boss.sendMessage("§eTeams wurden zurückgesetzt!");
    }

    /**
     * Übernimmt die Teams in die Challenge
     */
    private void applyTeamsToChallenge() {
        challenge.getTeams().clear();
        challenge.getPlayerToTeam().clear();

        for (Map.Entry<Integer, List<UUID>> entry : teams.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                UUID teamId = UUID.randomUUID();
                challenge.getTeams().put(teamId, entry.getValue());

                for (UUID playerId : entry.getValue()) {
                    challenge.getPlayerToTeam().put(playerId, teamId);
                }
            }
        }

        boss.sendMessage("§a§lTeams erstellt!");
        Bukkit.getLogger().info("[TeamBuilder] " + challenge.getTeams().size() + " Teams manuell erstellt");
    }

// === GETTER ===

    public Map<Integer, List<UUID>> getTeams() {
        return teams;
    }

    public List<UUID> getUnassignedPlayers() {
        return unassignedPlayers;
    }
}