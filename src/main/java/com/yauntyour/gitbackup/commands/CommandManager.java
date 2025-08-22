package com.yauntyour.gitbackup.commands;

import com.yauntyour.gitbackup.GitBackupPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final GitBackupPlugin plugin;
    private final BackupCommand backupCommand;

    public CommandManager(GitBackupPlugin plugin) {
        this.plugin = plugin;
        this.backupCommand = new BackupCommand(plugin);
    }

    public void registerCommands() {
        plugin.getCommand("gitbackup").setExecutor(this);
        plugin.getCommand("gitbackup").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return backupCommand.execute(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            if ("now".startsWith(partial) && sender.hasPermission("gitbackup.now")) {
                completions.add("now");
            }
            if ("list".startsWith(partial) && sender.hasPermission("gitbackup.list")) {
                completions.add("list");
            }
            if ("restore".startsWith(partial) && sender.hasPermission("gitbackup.restore")) {
                completions.add("restore");
            }
            if ("status".startsWith(partial) && sender.hasPermission("gitbackup.use")) {
                completions.add("status");
            }
            if ("reload".startsWith(partial) && sender.hasPermission("gitbackup.reload")) {
                completions.add("reload");
            }
            if ("init".startsWith(partial) && sender.hasPermission("gitbackup.init")) {
                completions.add("init");
            }
        }

        return completions;
    }
}