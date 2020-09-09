# Textile Backup
>终于有了一个Fabric的备份mod！

[![下载](http://cf.way2muchnoise.eu/full_359893_downloads.svg)
![版本](http://cf.way2muchnoise.eu/versions/359893.svg)](https://www.curseforge.com/minecraft/mc-mods/textile-backup)

轻量，可配置，完全服务器端的备份Fabric备份mod

指令看起来应该像这样：`/backup <operation> [args]`

有效的指令包括: 

 * start - 创建备份。你可以为备份加入注释，只要将注释作为第二个参数传入即可。例如: `/backup start Fabric太棒了`
 * restore - 恢复备份。注意当前的时间会被备份，你可以添加该备份的注释。`/backup restore <版本> [注释]`
 * killR - 终止当前恢复进程。
 * list - 列出所有可用备份。
 * cleanup - 强制启动清理进程 - 根据配置文件删除无效备份
 * whitelist - 你可以在这里添加，移除和列出所有无需足够权限就可以操作这个mod的玩家（白名单）*
 * backlist - 你可以在这里添加，移除和列出所有即使有足够权限也不能操作这个mod的玩家（黑名单）*
 
上面这些只能被服务器管理员(权限等级4 - 可配置*)、白名单玩家、单人模式的玩家或局域网联机的所有玩家使用。

你可以随意在你的整合包或服务器中使用这个mod。

### 重要

* 这个mod使用的时间格式是`dd.MM.yyyy_HH-mm-ss`，当然，这是可以配置的*.
* 这个mod以jars in a jar的形式包含 **Cotton Config** 和它的依赖，这是**CottonMC**的作品_.

\* - 自1.1.0版本可用的特性

如果你有任何建议或发现了问题，请在[Github](https://github.com/Szum123321/textile_backup)报告。
