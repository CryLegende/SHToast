package org.Cry_Legende.sHToast;

// Imports de UltimateAdvancementAPI (les noms exacts peuvent varier)

import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType; // Ou un nom similaire
// Possiblement besoin d'importer BaseComponent si l'API l'utilise pour le titre du toast
// import net.md_5.bungee.api.chat.BaseComponent;
// import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class SHToast extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_USE = "shtoast.use";
    private UltimateAdvancementAPI ultimateApi;

    @Override
    public void onEnable() {
        try {
            ultimateApi = UltimateAdvancementAPI.getInstance(this);
            if (ultimateApi == null) {
                getLogger().severe("Impossible d'obtenir une instance de UltimateAdvancementAPI !");
                getLogger().severe("Veuillez vérifier que UltimateAdvancementAPI est correctement installé et chargé AVANT SHToast.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erreur lors de l'obtention de l'instance de UltimateAdvancementAPI:", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getCommand("shtoast").setExecutor(this);
        this.getCommand("shtoast").setTabCompleter(this);
        getLogger().info("SHToast enabled and hooked into UltimateAdvancementAPI!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SHToast disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("shtoast")) {
            return false;
        }

        if (ultimateApi == null) {
            sender.sendMessage("§cErreur : Le module de toasts n'est pas correctement initialisé. Contactez un administrateur.");
            // Log silencieux ici car l'erreur d'initialisation a déjà été loggée dans onEnable
            return true;
        }

        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /shtoast <item> <type> <player|global> <message>");
            sender.sendMessage("§cTypes: TASK, GOAL, CHALLENGE (Vérifier les noms exacts dans l'API)");
            return true;
        }

        String itemName = args[0];
        String typeArg = args[1].toUpperCase();
        String targetArg = args[2];
        String messageContent = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null || !itemMaterial.isItem()) {
            sender.sendMessage("§cNom d'item invalide: " + itemName);
            return true;
        }
        ItemStack icon = new ItemStack(itemMaterial);

        AdvancementFrameType frameType;
        try {
            frameType = AdvancementFrameType.valueOf(typeArg); // Adapter aux noms exacts de l'enum de l'API
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType invalide: " + typeArg + ". Valeurs possibles: TASK, GOAL, CHALLENGE (vérifier doc API).");
            return true;
        }

        List<Player> playersToSend = new ArrayList<>();
        if (targetArg.equalsIgnoreCase("global")) {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                sender.sendMessage("§eAucun joueur en ligne.");
                return true;
            }
            playersToSend.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player player = Bukkit.getPlayer(targetArg);
            if (player == null) {
                sender.sendMessage("§cJoueur non trouvé: " + targetArg);
                return true;
            }
            playersToSend.add(player);
        }

        if (playersToSend.isEmpty()) {
            sender.sendMessage("§eAucun joueur cible.");
            return true;
        }

        try {
            sendToast(playersToSend, icon, frameType, messageContent);
            sender.sendMessage("§aToast envoyé !");
        } catch (Throwable t) { // Attraper Throwable pour inclure les Errors critiques
            sender.sendMessage("§cUne erreur interne est survenue lors de l'envoi du toast. Vérifiez la console du serveur.");
            getLogger().log(Level.SEVERE, "Erreur lors de l'appel à sendToast (UltimateAdvancementAPI):", t);
        }
        return true;
    }

    private void sendToast(List<Player> players, ItemStack icon, AdvancementFrameType frame, String message) {
        // Cette partie dépendra ENTIÈREMENT de la signature exacte de UltimateAdvancementAPI#displayCustomToast
        // et des types d'objets qu'elle attend.
        // L'exemple ci-dessous est une HYPOTHÈSE.

        for (Player player : players) {
            if (player.isOnline()) {
                try {
                    // HYPOTHÈSE 1: Le message est une String
                    ultimateApi.displayCustomToast(player, icon, message, frame);

                    // HYPOTHÈSE 2: Le message est un BaseComponent[] (comme avec BungeeCord Chat API)
                    // import net.md_5.bungee.api.chat.TextComponent;
                    // import net.md_5.bungee.api.chat.BaseComponent;
                    // ultimateApi.displayCustomToast(player, icon, new BaseComponent[]{new TextComponent(message)}, frame);

                } catch (Throwable t) {
                    // Logguer l'erreur si l'API elle-même échoue pour un joueur spécifique.
                    // Éviter de spammer le joueur à moins que ce ne soit une erreur qu'il peut résoudre.
                    getLogger().log(Level.WARNING, "Erreur lors de l'envoi du toast au joueur " + player.getName() + " via UltimateAdvancementAPI:", t);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("shtoast")) {
            return Collections.emptyList();
        }
        if (!sender.hasPermission(PERMISSION_USE)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();

        if (args.length == 1) { // <item>
            for (Material material : Material.values()) {
                if (material.isItem() && material.name().toLowerCase().startsWith(currentArg)) {
                    completions.add(material.name().toLowerCase());
                }
            }
        } else if (args.length == 2) { // <type>
            // Adapter aux noms exacts de l'enum AdvancementFrameType de l'API
            for (AdvancementFrameType frameApiType : AdvancementFrameType.values()) {
                if (frameApiType.name().toLowerCase().startsWith(currentArg)) {
                    completions.add(frameApiType.name().toLowerCase());
                }
            }
        } else if (args.length == 3) { // <player|global>
            if ("global".startsWith(currentArg)) {
                completions.add("global");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(currentArg)) {
                    completions.add(player.getName());
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }
}
