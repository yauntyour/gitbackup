# 【spigot】服务端git自动备份插件GitBackup

### 命令设计

```
/gitbackup - 显示插件帮助
/gitbackup now [消息] - 立即创建备份
/gitbackup list [页码] - 列出备份历史
/gitbackup restore <commit-id> - 恢复到指定备份
/gitbackup status - 显示当前备份状态
/gitbackup reload - 重新加载配置
```

# 特点

1. **错误处理** - 妥善处理Git操作中可能出现的异常
2. **世界保存** - 备份前确保世界数据已完全写入磁盘
3. **资源清理** - 确保文件句柄和Git资源正确释放
4. **内存管理** - 处理大文件时的内存使用优化

## 安装与设置

1. 将插件放入服务器的plugins文件夹
2. 启动服务器生成默认配置文件
3. 编辑config.yml配置Git仓库路径和其他设置
4. 如果是新仓库，需要初始化：`/gitbackup init`
5. 如果是现有仓库，确保正确配置远程URL

## 使用示例

```
# 创建即时备份
/gitbackup now "日常备份"

# 查看最近备份
/gitbackup list

# 恢复到特定备份
/gitbackup restore a1b2c3d

# 查看状态
/gitbackup status
```