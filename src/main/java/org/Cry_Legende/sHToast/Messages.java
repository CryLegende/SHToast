package org.Cry_Legende.sHToast;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Textes de messages.yml, codes couleur en &. Une clé absente du fichier retombe sur celui du jar. */
final class Messages {

    private final JavaPlugin plugin;
    private FileConfiguration file;
    private FileConfiguration defaults;

    Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        File target = new File(plugin.getDataFolder(), "messages.yml");
        if (!target.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        file = YamlConfiguration.loadConfiguration(target);
        defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8));
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
