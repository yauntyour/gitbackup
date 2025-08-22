package com.yauntyour.gitbackup;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // 保存默认配置（如果不存在）
        plugin.saveDefaultConfig();

        // 重载配置
        plugin.reloadConfig();

        // 获取配置对象
        config = plugin.getConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public String getRepositoryPath() {
        return config.getString("git.repository-path", "backups");
    }

    public String getRemoteUrl() {
        return config.getString("git.remote-url", "");
    }

    public String getBranch() {
        return config.getString("git.branch", "main");
    }

    public String getUserName() {
        return config.getString("git.user.name", "Minecraft Server");
    }

    public String getUserEmail() {
        return config.getString("git.user.email", "server@example.com");
    }

    public int getBackupInterval() {
        return config.getInt("backup.interval", 60);
    }

    public List<String> getWorlds() {
        return config.getStringList("backup.worlds");
    }

    public List<String> getExcludes() {
        return config.getStringList("backup.excludes");
    }

    public int getMaxBackups() {
        return config.getInt("backup.max-backups", 50);
    }

    public boolean shouldSaveWorld() {
        return config.getBoolean("backup.save-world", true);
    }
}