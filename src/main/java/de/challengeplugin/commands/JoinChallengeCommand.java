package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * Command: /joinchallenge
 * Erlaubt Late-Joining während der Farm-Phase
 * Spieler können bestehendem Team beitreten, neues Team erstellen oder solo bleiben
 */
public class JoinChallengeCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    // Witzige Zu-Spät-Nachrichten
    private static final String[] LATE_MESSAGES = {
            "§e%player% §7ist zu spät aufgestanden und %action%",
            "§e%player% §7hat den Wecker verschlafen und %action%",
            "§e%player% §7ist aus dem Bus gefallen und %action%",
            "§e%player% §7musste noch schnell Brötchen holen und %action%",
            "§e%player% §7war noch beim Friseur und %action%",
            "§e%player% §7hat sich verlaufen und %action%",
            "§e%player% §7hatte wichtigere Dinge zu tun und %action%",
            "§e%player% §7kommt fashionably late und %action%",
            "§e%player% §7erscheint wie ein echter Star und %action%",
            "§e%player% §7taucht aus dem Nichts auf und %action%"
    };

    public JoinChallengeCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen!");
            return true;
        }

        Player player = (Player) sender;
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        // Prüfe ob Challenge läuft
        if (challenge == null) {
            player.sendMessage("§cEs läuft keine Challenge!");
            return true;
        }

        // Prüfe ob Farm-Phase
        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.FARMING) {
            player.sendMessage("§cDu kannst nur während der Farm-Phase beitreten!");
            player.sendMessage("§7Die Challenge ist bereits in Phase: §e" + challenge.getCurrentPhase().name());
            return true;
        }

        // Prüfe ob Spieler bereits teilnimmt
        if (challenge.getParticipants().contains(player.getUniqueId())) {
            player.sendMessage("§cDu nimmst bereits an der Challenge teil!");
            return true;
        }

        // Öffne Team-Auswahl GUI
        openTeamSelectionGUI(player, challenge);

        return true;
    }

    /**
     * Öffnet GUI zur Team-Auswahl
     */
    private void openTeamSelectionGUI(Player player, Challenge challenge) {
        Inventory inv = Bukkit.createInventory(null, 36, "§6§lChallenge Beitreten");

        // Info-Item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§lWillkommen!");
        infoMeta.setLore(Arrays.asList(
                "§7Du kannst der Challenge beitreten:",
                "",
                "§a1. Bestehendem Team beitreten",
                "§7   (wenn Platz frei ist)",
                "",
                "§b2. Neues Team erstellen",
                "§7   (Solo/Duo/Trio je nach Modus)",
                "",
                "§c3. Kein Team (Solo spielen)",
                "§7   (du kämpfst alleine)",
                "",
                "§7Wähle unten aus!"
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Finde Teams mit freiem Platz
        int teamSize = challenge.getTeamMode().getTeamSize();
        List<UUID> availableTeams = new ArrayList<>();

        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            if (entry.getValue().size() < teamSize) {
                availableTeams.add(entry.getKey());
            }
        }

        // Zeige verfügbare Teams
        int slot = 10;
        int teamNumber = 1;
        for (UUID teamId : availableTeams) {
            List<UUID> members = challenge.getTeamMembers(teamId);

            ItemStack teamItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta teamMeta = teamItem.getItemMeta();
            teamMeta.setDisplayName("§aTeam " + teamNumber);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Mitglieder: §e" + members.size() + "§7/§e" + teamSize);
            lore.add("");

            // Zeige Namen der Mitglieder
            for (UUID memberId : members) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    lore.add("§7- §e" + member.getName());
                }
            }

            lore.add("");
            lore.add("§aKlicke um beizutreten!");

            teamMeta.setLore(lore);
            teamItem.setItemMeta(teamMeta);

            inv.setItem(slot, teamItem);
            slot++;
            teamNumber++;

            if (slot >= 17) break; // Max 7 Teams anzeigen
        }

        // Neues Team erstellen Button
        ItemStack newTeam = new ItemStack(Material.EMERALD);
        ItemMeta newTeamMeta = newTeam.getItemMeta();
        newTeamMeta.setDisplayName("§b§lNeues Team erstellen");
        newTeamMeta.setLore(Arrays.asList(
                "",
                "§7Erstelle dein eigenes Team!",
                "§7Modus: §e" + challenge.getTeamMode().name(),
                "§7Team-Größe: §e" + teamSize + " Spieler",
                "",
                "§7Du startest alleine und andere",
                "§7können später beitreten.",
                "",
                "§bKlicke zum Erstellen!"
        ));
        newTeam.setItemMeta(newTeamMeta);
        inv.setItem(30, newTeam);

        // NEU: Kein Team Button (Solo)
        ItemStack noTeam = new ItemStack(Material.BARRIER);
        ItemMeta noTeamMeta = noTeam.getItemMeta();
        noTeamMeta.setDisplayName("§c§lKein Team (Solo)");
        noTeamMeta.setLore(Arrays.asList(
                "",
                "§7Du kämpfst alleine!",
                "§7Kein Team, kein Backpack,",
                "§7aber volle Freiheit!",
                "",
                "§7Du kannst später immer noch",
                "§7einem Team beitreten.",
                "",
                "§cKlicke für Solo-Modus!"
        ));
        noTeam.setItemMeta(noTeamMeta);
        inv.setItem(32, noTeam);

        player.openInventory(inv);
    }

    /**
     * Spieler tritt bestehendem Team bei
     */
    public void joinExistingTeam(Player player, UUID teamId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        // Füge Spieler zum Team hinzu
        List<UUID> members = challenge.getTeamMembers(teamId);
        members.add(player.getUniqueId());

        // Update Challenge-Daten
        challenge.getParticipants().add(player.getUniqueId());
        challenge.getPlayerToTeam().put(player.getUniqueId(), teamId);
        challenge.getPlayerData().put(player.getUniqueId(), new de.challengeplugin.models.PlayerChallengeData(player.getUniqueId()));

        // Team-Color aktualisieren
        plugin.getChallengeManager().getTeamColorManager().addPlayerToTeam(challenge, player);

        // Team-Namen
        List<String> memberNames = new ArrayList<>();
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                memberNames.add(member.getName());
            }
        }

        // Witzige Broadcast-Nachricht
        String action = "§7ist §aTeam " + getTeamNumber(challenge, teamId) + " §7beigetreten!";
        broadcastLateMessage(player, action);

        player.sendMessage("§a§lDu bist Team " + getTeamNumber(challenge, teamId) + " beigetreten!");
        player.sendMessage("§7Deine Teammates: §e" + String.join(", ", memberNames));
        player.sendMessage("");
        player.sendMessage("§6Nutze §e/backpack §6um auf euren Team-Backpack zuzugreifen!");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Spieler erstellt neues Team
     */
    public void createNewTeam(Player player) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        // Erstelle neues Team
        UUID newTeamId = UUID.randomUUID();
        List<UUID> members = new ArrayList<>();
        members.add(player.getUniqueId());

        // Füge Team zur Challenge hinzu
        challenge.getTeams().put(newTeamId, members);
        challenge.getParticipants().add(player.getUniqueId());
        challenge.getPlayerToTeam().put(player.getUniqueId(), newTeamId);
        challenge.getPlayerData().put(player.getUniqueId(), new de.challengeplugin.models.PlayerChallengeData(player.getUniqueId()));

        // WICHTIG: Erstelle Backpack für neues Team
        plugin.getChallengeManager().getBackpackManager().createBackpackForTeam(newTeamId, challenge.getTeams().size());

        // WICHTIG: Team-Color hinzufügen
        plugin.getChallengeManager().getTeamColorManager().addTeamColor(challenge, newTeamId);

        // Witzige Broadcast-Nachricht
        String action = "§7hat §bein neues Team erstellt §7(§eTeam " + challenge.getTeams().size() + "§7)!";
        broadcastLateMessage(player, action);

        player.sendMessage("§a§lDu hast ein neues Team erstellt!");
        player.sendMessage("§7Du bist jetzt Team " + challenge.getTeams().size());
        player.sendMessage("§7Andere Spieler können deinem Team beitreten!");
        player.sendMessage("");
        player.sendMessage("§6Nutze §e/backpack §6um auf deinen Team-Backpack zuzugreifen!");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * NEU: Spieler bleibt ohne Team (Solo)
     */
    public void joinNoTeam(Player player) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        // Füge Spieler nur zu Participants hinzu (kein Team!)
        challenge.getParticipants().add(player.getUniqueId());
        challenge.getPlayerData().put(player.getUniqueId(), new de.challengeplugin.models.PlayerChallengeData(player.getUniqueId()));

        // Witzige Broadcast-Nachricht
        String action = "§7bleibt §csolo §7(kein Team)!";
        broadcastLateMessage(player, action);

        player.sendMessage("§c§lDu spielst solo (ohne Team)!");
        player.sendMessage("§7Du hast keinen Team-Backpack");
        player.sendMessage("§7Du kämpfst alleine in der Arena");
        player.sendMessage("");
        player.sendMessage("§7Nutze §e/joinchallenge §7nochmal falls du doch einem Team beitreten willst!");

        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.5f, 1.5f);
    }

    /**
     * Sendet witzige Zu-Spät-Nachricht
     */
    private void broadcastLateMessage(Player player, String action) {
        Random random = new Random();
        String message = LATE_MESSAGES[random.nextInt(LATE_MESSAGES.length)];

        message = message.replace("%player%", player.getName());
        message = message.replace("%action%", action);

        Bukkit.broadcastMessage("§6§l⏰ §7" + message);
    }

    /**
     * Hilfsmethode: Gibt Team-Nummer zurück
     */
    private int getTeamNumber(Challenge challenge, UUID teamId) {
        int number = 1;
        for (UUID id : challenge.getTeams().keySet()) {
            if (id.equals(teamId)) {
                return number;
            }
            number++;
        }
        return -1;
    }
}