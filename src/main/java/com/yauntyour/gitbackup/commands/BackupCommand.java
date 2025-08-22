package com.yauntyour.gitbackup.commands;

import com.yauntyour.gitbackup.GitBackupPlugin;
import com.yauntyour.gitbackup.GitManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BackupCommand {
    private final GitBackupPlugin plugin;
    private final GitManager gitManager;

    public BackupCommand(GitBackupPlugin plugin) {
        this.plugin = plugin;
        this.gitManager = plugin.getGitManager();
    }

    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "now":
                return handleNowCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "restore":
                return handleRestoreCommand(sender, args);
            case "status":
                return handleStatusCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "init":
                return handleInitCommand(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleNowCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gitbackup.now")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        String message = "手动备份";
        if (args.length > 1) {
            message = args[1];
            for (int i = 2; i < args.length; i++) {
                message += " " + args[i];
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "开始创建备份...");
        // 同步执行备份
        boolean success = gitManager.commitChanges(message);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "备份创建成功!");
        } else {
            sender.sendMessage(ChatColor.RED + "备份创建失败，请查看控制台获取详细信息");
        }

        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gitbackup.list")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "页码必须是数字!");
                return true;
            }
        }

        if (page < 1) {
            page = 1;
        }

        sender.sendMessage(ChatColor.YELLOW + "获取备份列表...");

        List<String> history = gitManager.getBackupHistory();

        if (history.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "没有找到备份记录");
        }

        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) history.size() / itemsPerPage);

        if (page > totalPages) {
            page = totalPages;
        }

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, history.size());

        sender.sendMessage(ChatColor.GOLD + "=== 备份列表 (第 " + page + " 页 / 共 " + totalPages + " 页) ===");

        for (int i = start; i < end; i++) {
            sender.sendMessage(ChatColor.GREEN + "[" + (i + 1) + "] " + ChatColor.WHITE + history.get(i));
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.YELLOW + "使用 '/gitbackup list " + (page + 1) + "' 查看下一页");
        }


        return true;
    }

    private boolean handleRestoreCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gitbackup.restore")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gitbackup restore <commit-hash>");
            return true;
        }

        // 确认操作
        sender.sendMessage(ChatColor.RED + "警告: 这将覆盖当前世界数据!");
        sender.sendMessage(ChatColor.RED + "输入 '/gitbackup restore confirm <commit-hash>' 确认恢复");

        if (args.length >= 3) {
            String commitHash = args[2];
            sender.sendMessage(ChatColor.YELLOW + "Backup to the commit-hash: " + commitHash);
            // 同步执行恢复
            try {
                boolean success = gitManager.restoreBackup(commitHash);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "备份恢复成功! 请重启服务器使更改生效");
                } else {
                    sender.sendMessage(ChatColor.RED + "备份恢复失败，请查看控制台获取详细信息");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("gitbackup.use")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        // 这里可以添加更多状态信息
        sender.sendMessage(ChatColor.GOLD + "=== Git备份状态 ===");
        sender.sendMessage(ChatColor.GREEN + "插件运行正常");
        sender.sendMessage(ChatColor.YELLOW + "使用 '/gitbackup list' 查看备份历史");

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("gitbackup.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        plugin.getConfigManager().loadConfig();
        plugin.getBackupScheduler().stopScheduledBackups();
        plugin.getBackupScheduler().startScheduledBackups();

        sender.sendMessage(ChatColor.GREEN + "配置已重新加载!");
        return true;
    }

    private boolean handleInitCommand(CommandSender sender) {
        if (!sender.hasPermission("gitbackup.init")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "初始化Git仓库...");
        boolean success = gitManager.initRepo();
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Git仓库初始化成功!");
        } else {
            sender.sendMessage(ChatColor.RED + "Git仓库初始化失败，请查看控制台获取详细信息");
        }


        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Git备份插件使用说明 ===");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup now [消息] - 立即创建备份");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup list [页码] - 列出备份历史");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup restore <commit-id> - 恢复到指定备份");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup status - 显示当前备份状态");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup reload - 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/gitbackup init - 初始化Git仓库");
    }
}