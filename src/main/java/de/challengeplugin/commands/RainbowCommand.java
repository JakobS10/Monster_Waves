package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Command: /rainbow [Spieler]
 * Gibt animierte Regenbogen-Rüstung die kontinuierlich die Farben wechselt!
 * Rüstung und Wolle können nicht gedroppt oder verschoben werden!
 *
 * VERBESSERT: Toggle-Funktion explizit dokumentiert!
 * Nur für Gammelbrot73!
 */
public class RainbowCommand implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;
    private static final String ALLOWED_USER = "Gammelbrot73";

    // Aktive Rainbow-Effekte (Spieler-UUID -> Task-ID)
    private final Map<UUID, Integer> activeRainbows = new HashMap<>();

    // Regenbogen-Farben (RGB)
    private static final Color[] RAINBOW_COLORS = {
            Color.fromRGB(255, 0, 0),      // Rot
            Color.fromRGB(255, 127, 0),    // Orange
            Color.fromRGB(255, 255, 0),    // Gelb
            Color.fromRGB(0, 255, 0),      // Grün
            Color.fromRGB(0, 0, 255),      // Blau
            Color.fromRGB(139, 0, 255)     // Pink/Violett
    };

    // Wolle-Farben (entsprechend zu RGB)
    private static final Material[] WOOL_COLORS = {
            Material.RED_WOOL,
            Material.ORANGE_WOOL,
            Material.YELLOW_WOOL,
            Material.LIME_WOOL,
            Material.BLUE_WOOL,
            Material.MAGENTA_WOOL
    };

    public RainbowCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Gammelbrot73 darf nutzen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player executor = (Player) sender;
        if (!executor.getName().equals(ALLOWED_USER)) {
            executor.sendMessage("§c§lDieser Command ist nur für " + ALLOWED_USER + "!");
            return true;
        }

        Player target;

        // Ziel-Spieler bestimmen
        if (args.length == 0) {
            // Kein Argument = sich selbst
            target = executor;
        } else {
            // Mit Argument = anderen Spieler
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                executor.sendMessage("§cSpieler nicht gefunden: " + args[0]);
                return true;
            }
        }

        // TOGGLE: Prüfe ob bereits Rainbow aktiv
        if (activeRainbows.containsKey(target.getUniqueId())) {
            stopRainbow(target);

            if (executor.equals(target)) {
                executor.sendMessage("§7Rainbow-Effekt wurde §cdeaktiviert§7!");
            } else {
                executor.sendMessage("§7Rainbow-Effekt für §e" + target.getName() + " §7wurde §cdeaktiviert§7!");
                target.sendMessage("§7Dein Rainbow-Effekt wurde von §e" + executor.getName() + " §cdeaktiviert§7!");
            }

            return true;
        }

        // Starte Rainbow
        startRainbow(target);

        if (executor.equals(target)) {
            executor.sendMessage("§d§l✨ RAINBOW AKTIVIERT! ✨");
            executor.sendMessage("§7Nutze §e/rainbow §7nochmal um zu deaktivieren");
        } else {
            executor.sendMessage("§d§l✨ Rainbow aktiviert für " + target.getName() + "! ✨");
            executor.sendMessage("§7Nutze §e/rainbow " + target.getName() + " §7nochmal um zu deaktivieren");
            target.sendMessage("§d§l✨ " + executor.getName() + " hat dir Rainbow gegeben! ✨");
        }

        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        return true;
    }

    /**
     * Startet Rainbow-Effekt für Spieler
     */
    private void startRainbow(Player player) {
        final int[] colorIndex = {0}; // Aktueller Farb-Index

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Prüfe ob Spieler noch online
                if (!player.isOnline()) {
                    stopRainbow(player);
                    return;
                }

                // Berechne Farben für jedes Rüstungsteil (von unten nach oben verschoben)
                Color bootsColor = RAINBOW_COLORS[colorIndex[0] % RAINBOW_COLORS.length];
                Color leggingsColor = RAINBOW_COLORS[(colorIndex[0] + 1) % RAINBOW_COLORS.length];
                Color chestplateColor = RAINBOW_COLORS[(colorIndex[0] + 2) % RAINBOW_COLORS.length];
                Color helmetColor = RAINBOW_COLORS[(colorIndex[0] + 3) % RAINBOW_COLORS.length];

                // Erstelle und setze Rüstung
                ItemStack boots = createRainbowArmor(Material.LEATHER_BOOTS, bootsColor, "§d§lRainbow Boots");
                ItemStack leggings = createRainbowArmor(Material.LEATHER_LEGGINGS, leggingsColor, "§d§lRainbow Leggings");
                ItemStack chestplate = createRainbowArmor(Material.LEATHER_CHESTPLATE, chestplateColor, "§d§lRainbow Chestplate");
                ItemStack helmet = createRainbowArmor(Material.LEATHER_HELMET, helmetColor, "§d§lRainbow Helmet");

                player.getInventory().setBoots(boots);
                player.getInventory().setLeggings(leggings);
                player.getInventory().setChestplate(chestplate);
                player.getInventory().setHelmet(helmet);

                // Wolle in Offhand und Slot 0 (Farbe der Brustplatte)
                Material woolColor = WOOL_COLORS[(colorIndex[0] + 2) % WOOL_COLORS.length];
                ItemStack wool = createRainbowWool(woolColor);

                player.getInventory().setItemInOffHand(wool);
                player.getInventory().setItem(0, wool);

                // Nächste Farbe
                colorIndex[0]++;
            }
        };

        // Starte Animation (alle 10 Ticks = 0.5 Sekunden)
        int taskId = task.runTaskTimer(plugin, 0L, 10L).getTaskId();
        activeRainbows.put(player.getUniqueId(), taskId);
    }

    /**
     * Stoppt Rainbow-Effekt für Spieler
     * VERBESSERT: Sauberes Cleanup!
     */
    private void stopRainbow(Player player) {
        Integer taskId = activeRainbows.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // Entferne Rainbow-Items
        if (player.isOnline()) {
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.getInventory().setItemInOffHand(null);

            ItemStack slot0 = player.getInventory().getItem(0);
            if (slot0 != null && slot0.hasItemMeta() &&
                    slot0.getItemMeta().getDisplayName().equals("§d§lRainbow Wool")) {
                player.getInventory().setItem(0, null);
            }

            player.sendMessage("§7Rainbow-Effekt wurde entfernt!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        }
    }

    /**
     * Erstellt Rainbow-Rüstungsteil
     */
    private ItemStack createRainbowArmor(Material material, Color color, String name) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        meta.setColor(color);
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§d✨ RAINBOW ✨",
                "§7Kann nicht gedroppt werden!"
        ));
        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Erstellt Rainbow-Wolle
     */
    private ItemStack createRainbowWool(Material woolType) {
        ItemStack wool = new ItemStack(woolType, 1);
        org.bukkit.inventory.meta.ItemMeta meta = wool.getItemMeta();

        meta.setDisplayName("§d§lRainbow Wool");
        meta.setLore(Arrays.asList(
                "§dRAINBOW",
                "§7Kann nicht gedroppt werden!"
        ));

        wool.setItemMeta(meta);
        return wool;
    }

    /**
     * Prüft ob Item ein Rainbow-Item ist
     */
    public boolean isRainbowItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String displayName = item.getItemMeta().getDisplayName();
        return displayName.startsWith("§d§lRainbow");
    }

    /**
     * Prüft ob Spieler Rainbow aktiv hat
     */
    public boolean hasRainbow(UUID playerId) {
        return activeRainbows.containsKey(playerId);
    }

    /**
     * Cleanup beim Plugin-Disable
     */
    public void cleanup() {
        for (UUID playerId : new HashSet<>(activeRainbows.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                stopRainbow(player);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Nur für Gammelbrot73
        if (!(sender instanceof Player) || !((Player) sender).getName().equals(ALLOWED_USER)) {
            return completions;
        }

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        return completions;
    }
}