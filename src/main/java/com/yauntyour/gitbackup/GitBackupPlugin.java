package com.yauntyour.gitbackup;

import com.yauntyour.gitbackup.commands.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GitBackupPlugin extends JavaPlugin {
    private static GitBackupPlugin instance;
    private ConfigManager configManager;
    private GitManager gitManager;
    private BackupScheduler backupScheduler;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 初始化Git管理器
        gitManager = new GitManager(this);

        // 初始化命令管理器
        commandManager = new CommandManager(this);
        commandManager.registerCommands();

        // 初始化备份调度器
        backupScheduler = new BackupScheduler(this);
        backupScheduler.startScheduledBackups();

        getLogger().info("GitBackup插件已启用!");
    }

    @Override
    public void onDisable() {
        if (backupScheduler != null) {
            backupScheduler.stopScheduledBackups();
        }

        if (gitManager != null) {
            gitManager.close();
        }

        getLogger().info("GitBackup插件已禁用!");
    }

    public static GitBackupPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GitManager getGitManager() {
        return gitManager;
    }

    public BackupScheduler getBackupScheduler() {
        return backupScheduler;
    }
}