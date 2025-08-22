package com.yauntyour.gitbackup;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GitManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private Git git;
    private Repository repository;

    public GitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = ((GitBackupPlugin) plugin).getConfigManager();
    }

    public boolean initRepo() {
        try {
            File repoDir = new File(configManager.getRepositoryPath());

            // 如果目录不存在，创建它
            if (!repoDir.exists()) {
                repoDir.mkdirs();
            }

            // 检查是否已经是Git仓库
            if (isGitRepository(repoDir)) {
                plugin.getLogger().info("发现现有的Git仓库");
                openRepo();
                return true;
            }

            // 初始化新的Git仓库
            plugin.getLogger().info("初始化新的Git仓库...");
            git = Git.init()
                    .setDirectory(repoDir)
                    .setBare(false)
                    .call();

            repository = git.getRepository();

            // 创建.gitignore文件
            createGitIgnore();

            // 初始提交
            commitChanges("初始提交");

            plugin.getLogger().info("Git仓库初始化成功!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("初始化Git仓库时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean isGitRepository(File dir) {
        File gitDir = new File(dir, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    private void openRepo() {
        try {
            File repoDir = new File(configManager.getRepositoryPath());
            git = Git.open(repoDir);
            repository = git.getRepository();
        } catch (IOException e) {
            plugin.getLogger().severe("打开Git仓库时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createGitIgnore() throws IOException {
        File gitIgnore = new File(configManager.getRepositoryPath(), ".gitignore");
        List<String> excludes = configManager.getExcludes();

        if (!gitIgnore.exists()) {
            Files.write(gitIgnore.toPath(), excludes);
            try {
                git.add().addFilepattern(".gitignore").call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean commitChanges(String message) {
        if (git == null) {
            plugin.getLogger().warning("Git仓库未初始化!");
            return false;
        }

        try {
            // 保存世界（如果需要）
            if (configManager.shouldSaveWorld()) {
                for (World world : Bukkit.getWorlds()) {
                    world.save();
                }
            }

            // 复制世界到备份目录
            copyWorldsToBackup();

            // 添加所有文件到Git
            git.add().addFilepattern(".").call();

            // 检查是否有更改
            if (git.status().call().getChanged().isEmpty() &&
                    git.status().call().getAdded().isEmpty() &&
                    git.status().call().getModified().isEmpty() &&
                    git.status().call().getRemoved().isEmpty()) {
                plugin.getLogger().info("没有检测到更改，跳过提交");
                return false;
            }

            // 提交更改
            git.commit()
                    .setMessage(message)
                    .setAuthor(configManager.getUserName(), configManager.getUserEmail())
                    .call();

            plugin.getLogger().info("已创建备份提交: " + message);

            // 推送到远程（如果配置了）
            pushToRemote();

            // 清理旧备份（如果需要）
            cleanupOldBackups();

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("提交更改时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void copyWorldsToBackup() throws IOException {
        File backupDir = new File(configManager.getRepositoryPath());

        for (String worldName : configManager.getWorlds()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("世界 '" + worldName + "' 不存在，跳过备份");
                continue;
            }

            File worldDir = world.getWorldFolder();
            File destDir = new File(backupDir, worldName);

            // 如果目标目录存在，删除它
            if (destDir.exists()) {
                deleteDirectory(destDir);
            }

            // 复制世界目录
            copyDirectory(worldDir.toPath(), destDir.toPath());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
                .forEach(sourcePath -> {
                    try {
                        Path targetPath = target.resolve(source.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            if (!Files.exists(targetPath)) {
                                Files.createDirectory(targetPath);
                            }
                        } else {
                            Files.copy(sourcePath, targetPath);
                        }
                    } catch (IOException e) {
                        plugin.getLogger().severe("复制文件时出错: " + e.getMessage());
                    }
                });
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        Files.delete(directory.toPath());
    }

    private void pushToRemote() {
        String remoteUrl = configManager.getRemoteUrl();
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return;
        }

        try {
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", ""))
                    .call();
            plugin.getLogger().info("已推送到远程仓库");
        } catch (InvalidRemoteException e) {
            plugin.getLogger().warning("无效的远程仓库URL: " + e.getMessage());
        } catch (TransportException e) {
            plugin.getLogger().warning("推送时传输错误: " + e.getMessage());
        } catch (GitAPIException e) {
            plugin.getLogger().severe("推送时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanupOldBackups() {
        int maxBackups = configManager.getMaxBackups();
        if (maxBackups <= 0) {
            return; // 无限制
        }

        try {
            Iterable<RevCommit> commits = git.log().call();
            List<RevCommit> commitList = new ArrayList<>();
            commits.forEach(commitList::add);

            if (commitList.size() > maxBackups) {
                // 找到需要删除的旧提交
                List<RevCommit> toRemove = commitList.subList(maxBackups, commitList.size());
                for (RevCommit commit : toRemove) {
                    // 注意：这不会从历史中完全删除提交，只是删除标签/引用
                    git.tagDelete().setTags(commit.getName()).call();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("清理旧备份时出错: " + e.getMessage());
        }
    }

    public List<String> getBackupHistory() {
        List<String> history = new ArrayList<>();

        if (git == null) {
            plugin.getLogger().warning("Git仓库未初始化!");
            return history;
        }

        try {
            Iterable<RevCommit> commits = git.log().call();
            for (RevCommit commit : commits) {
                String entry = commit.getName() + " - " +
                        commit.getAuthorIdent().getWhen() + " - " +
                        commit.getShortMessage();
                history.add(entry);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("获取备份历史时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return history;
    }

    public boolean restoreBackup(String commitHash) {
        if (git == null) {
            plugin.getLogger().warning("Git仓库未初始化!");
            return false;
        }

        try {
            // 检出特定提交
            git.checkout()
                    .setName(commitHash)
                    .call();

            plugin.getLogger().info("已检出提交: " + commitHash);

            // 复制世界回服务器目录
            copyBackupToWorlds();

            plugin.getLogger().info("备份恢复完成，请重启服务器使更改生效");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("恢复备份时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void copyBackupToWorlds() throws IOException {
        File backupDir = new File(configManager.getRepositoryPath());

        for (String worldName : configManager.getWorlds()) {
            File sourceDir = new File(backupDir, worldName);
            if (!sourceDir.exists()) {
                plugin.getLogger().warning("备份中找不到世界 '" + worldName + "'，跳过恢复");
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("服务器中不存在世界 '" + worldName + "'，跳过恢复");
                continue;
            }

            File worldDir = world.getWorldFolder();

            // 备份当前世界
            File backupWorldDir = new File(worldDir.getParent(), worldName + ".backup");
            if (backupWorldDir.exists()) {
                deleteDirectory(backupWorldDir);
            }
            copyDirectory(worldDir.toPath(), backupWorldDir.toPath());

            // 删除当前世界
            deleteDirectory(worldDir);

            // 复制备份的世界
            copyDirectory(sourceDir.toPath(), worldDir.toPath());
        }
    }

    public void close() {
        if (repository != null) {
            repository.close();
        }
    }
}