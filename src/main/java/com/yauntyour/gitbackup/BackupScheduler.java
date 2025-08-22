package com.yauntyour.gitbackup;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BackupScheduler {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final GitManager gitManager;
    private BukkitTask backupTask;

    public BackupScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = ((GitBackupPlugin) plugin).getConfigManager();
        this.gitManager = ((GitBackupPlugin) plugin).getGitManager();
    }

    public void startScheduledBackups() {
        int interval = configManager.getBackupInterval();

        if (interval <= 0) {
            plugin.getLogger().info("自动备份已禁用");
            return;
        }

        // 将分钟转换为ticks (20 ticks = 1秒)
        long intervalTicks = interval * 60 * 20;

        backupTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("开始自动备份...");
                gitManager.commitChanges("自动备份 - " + System.currentTimeMillis());
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);

        plugin.getLogger().info("已启动自动备份，间隔: " + interval + " 分钟");
    }

    public void stopScheduledBackups() {
        if (backupTask != null) {
            backupTask.cancel();
            plugin.getLogger().info("已停止自动备份");
        }
    }
}