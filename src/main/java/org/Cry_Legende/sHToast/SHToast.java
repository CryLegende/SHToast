package org.Cry_Legende.sHToast;

import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.google.gson.JsonObject;
import org.Cry_Legende.sHToast.network.ShNetworkToasts;
import org.Cry_Legende.sHToast.network.ToastNetwork;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SHToast extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_USE = "shtoast.use";
    private static final String PERMISSION_RELOAD = "shtoast.admin.reload";
    private UltimateAdvancementAPI ultimateApi;
    private Messages messages;
    /** Joueurs des autres serveurs du réseau ; null sans SHNetwork. */
    private ToastNetwork network;
    /** Valeur de reseau.actif au démarrage : le mode réseau ne se branche ni ne se débranche à chaud. */
    private boolean networkConfigured;

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

        saveDefaultConfig();
        networkConfigured = getConfig().getBoolean("reseau.actif", true);
        messages = new Messages(this);
        enableNetwork();

        this.getCommand("shtoast").setExecutor(this);
        this.getCommand("shtoast").setTabCompleter(this);
        getLogger().info("SHToast enabled and hooked into UltimateAdvancementAPI!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SHToast disabled!");
    }

    /**
     * Passe en mode réseau si SHNetwork est installé et 'reseau.actif' est vrai. La classe qui touche à SHNetwork
     * n'est résolue qu'ici : sans lui, rien n'est chargé et les toasts restent locaux.
     */
    private void enableNetwork() {
        if (!getConfig().getBoolean("reseau.actif", true)) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("SHNetwork")) return;
        try {
            ToastNetwork net = ShNetworkToasts.open();
            net.onBroadcast(this::receiveGlobal);
            network = net;
            getLogger().info("SHNetwork détecté : les toasts atteignent les joueurs de tout le réseau.");
        } catch (RuntimeException | LinkageError e) {
            network = null;
            getLogger().log(Level.SEVERE, "SHNetwork présent mais inutilisable, toasts locaux à ce serveur : " + e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("shtoast")) {
            return false;
        }

        if (ultimateApi == null) {
            sender.sendMessage(messages.get("not-initialized"));
            // Log silencieux ici car l'erreur d'initialisation a déjà été loggée dans onEnable
            return true;
        }

        // /shtoast reload : seule forme à un argument ; les envois gardent leur syntaxe (item type cible message)
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(PERMISSION_RELOAD)) {
                sender.sendMessage(messages.get("no-permission"));
                return true;
            }
            reload(sender);
            return true;
        }

        if (!sender.hasPermission(PERMISSION_USE)) {
            // Seul le droit de recharger : la ligne d'usage ne montre que cette forme
            sender.sendMessage(sender.hasPermission(PERMISSION_RELOAD)
                ? messages.get("usage-line", "commands", usageCommands(sender))
                : messages.get("no-permission"));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(messages.get("usage-line", "commands", usageCommands(sender)));
            return true;
        }

        String itemName = args[0];
        String typeArg = args[1].toUpperCase();
        String targetArg = args[2];
        String messageContent = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null || !itemMaterial.isItem()) {
            sender.sendMessage(messages.get("invalid-item", "item", itemName));
            return true;
        }
        ItemStack icon = new ItemStack(itemMaterial);

        AdvancementFrameType frameType;
        try {
            frameType = AdvancementFrameType.valueOf(typeArg);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(messages.get("invalid-type", "type", typeArg));
            return true;
        }

        List<Player> playersToSend = new ArrayList<>();
        boolean everywhere = false;
        if (targetArg.equalsIgnoreCase("global")) {
            if (Bukkit.getOnlinePlayers().isEmpty() && (network == null || network.playersElsewhere() == 0)) {
                sender.sendMessage(messages.get("nobody-online"));
                return true;
            }
            playersToSend.addAll(Bukkit.getOnlinePlayers());
            everywhere = network != null;
        } else {
            Player player = Bukkit.getPlayerExact(targetArg);
            if (player == null && network != null) {
                // Connecté sur un autre serveur : son serveur affiche le toast à sa place
                if (network.relay(targetArg, name -> "shtoast " + itemName + " " + typeArg + " " + name + " " + messageContent)) {
                    sender.sendMessage(messages.get("sent"));
                    return true;
                }
            }
            if (player == null) {
                player = Bukkit.getPlayer(targetArg);
            }
            if (player == null) {
                sender.sendMessage(messages.get("player-not-found", "player", targetArg));
                return true;
            }
            playersToSend.add(player);
        }

        if (playersToSend.isEmpty() && !everywhere) {
            sender.sendMessage(messages.get("no-target"));
            return true;
        }

        try {
            sendToast(playersToSend, icon, frameType, messageContent);
            if (everywhere) {
                JsonObject toast = new JsonObject();
                toast.addProperty("item", itemMaterial.name());
                toast.addProperty("type", frameType.name());
                toast.addProperty("message", messageContent);
                network.broadcast(toast);
            }
            sender.sendMessage(messages.get("sent"));
        } catch (Throwable t) { // Attraper Throwable pour inclure les Errors critiques
            sender.sendMessage(messages.get("internal-error"));
            getLogger().log(Level.SEVERE, "Erreur lors de l'appel à sendToast (UltimateAdvancementAPI):", t);
        }
        return true;
    }

    /** Les formes de la commande que l'expéditeur a le droit d'utiliser, pour la ligne d'usage. */
    private String usageCommands(CommandSender sender) {
        List<String> forms = new ArrayList<>();
        if (sender.hasPermission(PERMISSION_USE)) {
            forms.add(messages.get("usage-send"));
        }
        if (sender.hasPermission(PERMISSION_RELOAD)) {
            forms.add(messages.get("usage-reload"));
        }
        return String.join(messages.get("usage-separator"), forms);
    }

    /**
     * Relit config.yml et messages.yml. Un fichier illisible laisse tout en place. reseau.actif ne se relit qu'au
     * démarrage : si sa valeur a changé, le message le dit.
     */
    private void reload(CommandSender sender) {
        for (String name : List.of("config.yml", "messages.yml")) {
            File file = new File(getDataFolder(), name);
            if (!file.isFile()) {
                continue;
            }
            try {
                new YamlConfiguration().load(file);
            } catch (IOException | InvalidConfigurationException e) {
                String error = String.valueOf(e.getMessage()).replaceAll("\\s+", " ").trim();
                getLogger().warning(name + " illisible, configuration inchangée : " + error);
                sender.sendMessage(messages.get("reload-error", "file", name, "error", error));
                return;
            }
        }
        reloadConfig();
        try {
            messages.reload();
        } catch (IOException | InvalidConfigurationException e) {
            sender.sendMessage(messages.get("reload-error", "file", "messages.yml", "error", String.valueOf(e.getMessage())));
            return;
        }
        sender.sendMessage(messages.get("reload-done"));
        if (getConfig().getBoolean("reseau.actif", true) != networkConfigured) {
            sender.sendMessage(messages.get("reload-restart"));
        }
    }

    /** Toast général envoyé depuis un autre serveur : affiché à tous les joueurs d'ici. */
    private void receiveGlobal(JsonObject toast) {
        try {
            Material material = Material.getMaterial(toast.get("item").getAsString().toUpperCase(Locale.ROOT));
            AdvancementFrameType frame = AdvancementFrameType.valueOf(toast.get("type").getAsString().toUpperCase(Locale.ROOT));
            if (material == null || !material.isItem() || Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            sendToast(Bukkit.getOnlinePlayers(), new ItemStack(material), frame, toast.get("message").getAsString());
        } catch (RuntimeException e) {
            getLogger().log(Level.WARNING, "Toast général reçu du réseau illisible : " + e);
        }
    }

    private void sendToast(Collection<? extends Player> players, ItemStack icon, AdvancementFrameType frame, String message) {
        for (Player player : players) {
            if (player.isOnline()) {
                try {
                    ultimateApi.displayCustomToast(player, icon, message, frame);
                } catch (Throwable t) {
                    // Si l'API échoue pour un joueur, on le note sans interrompre les autres
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
        boolean canSend = sender.hasPermission(PERMISSION_USE);
        boolean canReload = sender.hasPermission(PERMISSION_RELOAD);
        if (!canSend && !canReload) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();

        if (!canSend) { // reload seulement
            if (args.length == 1 && "reload".startsWith(currentArg)) {
                completions.add("reload");
            }
            return completions;
        }
        if (args.length == 1) { // <item>
            if (canReload && "reload".startsWith(currentArg)) {
                completions.add("reload");
            }
            for (Material material : Material.values()) {
                if (material.isItem() && material.name().toLowerCase().startsWith(currentArg)) {
                    completions.add(material.name().toLowerCase());
                }
            }
        } else if (args.length == 2) { // <type>
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
