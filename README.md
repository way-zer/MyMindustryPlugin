# Mindustry 服务器基础管理插件
Essitial plugin for Mindustry (now only Chinese,need English version open an issue).  
当前使用在我自己服务器 m.tiny:smile:lake.tk

## 功能介绍
1. 重写默认换图机制(可以根据文件名首字母确定模式 S生存P对抗A攻击C沙盒创造E编辑器模式)
2. 完整投票功能(跳波,换图,踢人,投降,回滚)
3. 每10分钟自动保存(整10, xx:00保存为100,xx:50保存为105)
4. 额外的管理员系统(可以执行管理员指令,vote kick立即执行)
5. 进服欢迎信息(目前为硬编码,如需修改,请修改源码并手动编译)
6. 开源,~~可直接修改配置(配置在[Config.kt](https://github.com/way-zer/MyMindustryPlugin/blob/master/MindustryPlugin/src/main/kotlin/cf/wayzer/mindustry/Config.kt)文件中)~~(已有配置文件config/pluginConf.conf)
7. PVP辅助(PVP保护时间;防止重进换队等)
8. 贡献榜(一局结算一次,目前根据时间贡献,有更好的请联系我)
9. 限制每队单位数量(兵和无人机,防卡服)
10. 部分功能见下方指令说明

### 指令
#### 玩家指令:
- /status 查看服务器当前状态
- /info 查看当前个人信息
- /votekick <player...>(原版指令,导向/vote kick <player...>)
- /maps 查看服务器地图
- /slots 查看自动备份
- /spectate 进入观察者模式
- /vote 各种投票功能  
    skipwave 投票跳波(快速进行10波)  
    gameover 投降  
    map <id(见/maps)> 换图  
    rollback <id(见/slots)> 回滚地图  
    kick <player...> 投票踢人(3次15分钟警告,然后ban)(直接点Tab中的会重定向到该指令)
#### 管理员指令:
- /list 查看服务器玩家Id列表
- /ban [三位id] 查看已ban玩家,ban玩家(或解ban) | 三位id见/list | 点tab列表中的投票也有相同效果
- /robot (实验性功能) 召唤专用鬼怪建筑机 管理员可用,1分钟复活,限2个
- ~~/reloadMaps 重载地图(地图上传功能请使用其他软件实现)~~(在需要时自动reload)
#### 后台指令:
- /maps (覆盖原版)内容与玩家基本相同
- /load (覆盖原版)加载某个slot存档(不限于自动保存)
- /host (覆盖原版)用法和原版基本相同
- /madmin [uuid] 查看当前管理员,增减管理员
- /mBans 类似原版/bans,按时间排序,可显示日期
- /mInfo <UUID> 显示玩家统计信息
- /addExp <playerId> <num> 增加玩家经验(暂无多用,用户等级系统正在计划中)
- /reloadConfig 重载配置文件

### 未来计划
- 增加反捣乱措施
- 完善用户等级机制
- 直接地图评价机制
- 增加对沙盒和编辑器模式的支持
- 国际化支持(每个玩家单独语言)

### 更新日记及下载
请见[发布页](https://github.com/way-zer/MyMindustryPlugin/releases)

## 安装方式
在Relase中下载最新jar,放置在config/mods下即可
> 注意: 第一次启动会下载依赖库,网络不好可能会耗费较长时间,且需要多次尝试(下载成功后会自动缓存)
### 自行编译
```shell
cd MindustryPlugin
.\gradlew shadowJar #注意不是build
cd build\libs
ls #可以看到生成的jar文件
```

## 关于授权(License)
目前保留权限,只允许个人修改及使用,不允许他人分发
