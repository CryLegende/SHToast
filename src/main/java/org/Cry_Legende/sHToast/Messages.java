package org.Cry_Legende.sHToast;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Textes de messages.yml, codes couleur en &. Une clé absente du fichier retombe sur celui du jar. */
final class Messages {

    private final JavaPlugin plugin;
    private volatile FileConfiguration file;
    private volatile FileConfiguration defaults;

    Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        file = YamlConfiguration.loadConfiguration(target());
        defaults = jarDefaults();
    }

    private File target() {
        File target = new File(plugin.getDataFolder(), "messages.yml");
        if (!target.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return target;
    }

    private FileConfiguration jarDefaults() {
        return YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8));
    }

    /** Relit messages.yml. Un fichier illisible lève l'erreur et laisse les textes en place. */
    void reload() throws IOException, InvalidConfigurationException {
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.load(target());
        file = loaded;
        defaults = jarDefaults();
    }

    /** Message de la clé ; chaque paire de {@code values} est un {nom} à remplacer par sa valeur. */
    String get(String key, String... values) {
        String text = file.getString(key);
        if (text == null) {
            text = defaults.getString(key, key);
        }
        for (int i = 0; i + 1 < values.length; i += 2) {
            text = text.replace("{" + values[i] + "}", values[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
