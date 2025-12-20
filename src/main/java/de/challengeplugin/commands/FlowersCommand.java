package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Command: /flowers
 * Gibt jedem Online-Spieler eine zufällige Blume und rosa Lederrüstung
 * Die Rüstung verschwindet nach 1 Minute automatisch!
 */
public class FlowersCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    // Alle verfügbaren Blumen in Minecraft
    private static final Material[] FLOWERS = {
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY
    };

    // Set um zu tracken welche Items verschwinden sollen
    private final Set<UUID> trackedArmor = new HashSet<>();

    public FlowersCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Jeder kann diesen Command nutzen!

        int flowerCount = 0;
        int armorCount = 0;
        Random random = new Random();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 1. Zufällige Blume droppen
            Material randomFlower = FLOWERS[random.nextInt(FLOWERS.length)];
            ItemStack flower = new ItemStack(randomFlower, 1);
            player.getWorld().dropItemNaturally(player.getLocation(), flower);
            flowerCount++;

            // 2. Rosa Lederrüstung erstellen und droppen
            giveDisappearingPinkArmor(player);
            armorCount++;
        }

        // Broadcast
        Bukkit.broadcastMessage("§d§l BLUMEN-REGEN!");
        Bukkit.broadcastMessage("§7" + flowerCount + " Blumen und " + armorCount + " rosa Rüstungssets wurden verteilt!");
        Bukkit.broadcastMessage("§7Die Rüstung verschwindet in 1 Minute!");

        // Sound für alle
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        return true;
    }

    /**
     * Gibt Spieler rosa Lederrüstung die nach 1 Minute verschwindet
     */
    private void giveDisappearingPinkArmor(Player player) {
        // Rosa Farbe (RGB)
        Color pink = Color.fromRGB(255, 192, 203);

        // Erstelle rosa Rüstungsteile
        ItemStack helmet = createPinkLeatherArmor(Material.LEATHER_HELMET, pink, "§d§lSteht dir!");
        ItemStack chestplate = createPinkLeatherArmor(Material.LEATHER_CHESTPLATE, pink, "§d§lSehr hübsch!");
        ItemStack leggings = createPinkLeatherArmor(Material.LEATHER_LEGGINGS, pink, "§d§lDekorativer Beinschmuck!");
        ItemStack boots = createPinkLeatherArmor(Material.LEATHER_BOOTS, pink, "§d§lDu solltest das öfter tragen!");

        // Droppe Rüstung am Spieler
        player.getWorld().dropItemNaturally(player.getLocation(), helmet);
        player.getWorld().dropItemNaturally(player.getLocation(), chestplate);
        player.getWorld().dropItemNaturally(player.getLocation(), leggings);
        player.getWorld().dropItemNaturally(player.getLocation(), boots);

        // Tracke diese Rüstungsteile für Auto-Delete
        UUID armorSetId = UUID.randomUUID();
        trackedArmor.add(armorSetId);

        // Timer: Nach 1 Minute (1200 Ticks) verschwinden lassen
        new BukkitRunnable() {
            @Override
            public void run() {
                removeDisappearingArmor(player, pink);
                trackedArmor.remove(armorSetId);
            }
        }.runTaskLater(plugin, 1200L); // 60 Sekunden = 1200 Ticks
    }

    /**
     * Erstellt ein gefärbtes Lederrüstungsteil
     */
    private ItemStack createPinkLeatherArmor(Material material, Color color, String name) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        meta.setColor(color);
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§7Verschwindet nach 1 Minute!",
                "§dMagisch"
        ));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Entfernt rosa Rüstung von Spieler (Inventar, Boden, angezogen)
     */
    private void removeDisappearingArmor(Player player, Color pink) {
        if (!player.isOnline()) return;

        // 1. Aus Inventar entfernen
        for (ItemStack item : player.getInventory().getContents()) {
            if (isPinkLeatherArmor(item, pink)) {
                player.getInventory().remove(item);
            }
        }

        // 2. Von Rüstungsslots entfernen
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isPinkLeatherArmor(armor[i], pink)) {
                armor[i] = null;
            }
        }
        player.getInventory().setArmorContents(armor);

        // 3. Von Boden entfernen (in Nähe des Spielers)
        player.getWorld().getEntities().stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Item)
                .map(entity -> (org.bukkit.entity.Item) entity)
                .filter(itemEntity -> player.getLocation().distance(itemEntity.getLocation()) < 50)
                .filter(itemEntity -> isPinkLeatherArmor(itemEntity.getItemStack(), pink))
                .forEach(org.bukkit.entity.Entity::remove);

        player.sendMessage("§dDeine rosa Rüstung ist verschwunden!<");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    /**
     * Prüft ob Item rosa Lederrüstung ist
     */
    private boolean isPinkLeatherArmor(ItemStack item, Color pink) {
        if (item == null) return false;
        if (!(item.getItemMeta() instanceof LeatherArmorMeta)) return false;

        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        // Prüfe ob Farbe pink ist
        Color itemColor = meta.getColor();
        return itemColor.getRed() == pink.getRed()
                && itemColor.getGreen() == pink.getGreen()
                && itemColor.getBlue() == pink.getBlue();
    }
}