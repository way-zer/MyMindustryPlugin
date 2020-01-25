# Mindustry 服务器基础管理插件
Essitial plugin for Mindustry (now only Chinese,need English version open an issue).  
当前使用在我自己服务器 m.tiny:smile:lake.tk

## 功能介绍
1. 重写默认换图机制(可以根据文件名首字母确定模式 S生存P对抗A攻击)
2. 完整投票功能
3. 每10分钟自动保存(整10, xx:00保存为100,xx:50保存为105)
4. 额外的管理员系统(可以执行管理员指令,vote kick立即执行)
5. 进服欢迎信息(目前为硬编码,如需修改,请修改源码并手动编译)
6. 开源,可直接修改配置(配置在[Config.kt](https://github.com/way-zer/MyMindustryPlugin/blob/master/MindustryPlugin/src/main/kotlin/cf/wayzer/mindustry/Config.kt)文件中)

### 指令
#### 玩家指令:
- /status 查看服务器当前状态
- /info 查看当前个人信息
- /votekick <player...>(原版指令,导向/vote kick <player...>)
- /maps 查看服务器地图
- /slots 查看自动备份
- /vote 各种投票功能  
  skipwave 投票跳波(10波)  
  gameover 投降  
  map <id(见/maps)> 换图  
  rollback <id(见/slots)> 回滚地图  
  kick <player...> 投票踢人(ban)(直接点Tab中的会重定向到该指令)  
#### 管理员指令:
- /list 查看服务器玩家Id列表
- /ban [三位id] 查看已ban玩家,ban玩家(或解ban) | 三位id见/list
- /reloadMaps 重载地图(地图上传功能请使用其他软件实现)  
#### 后台指令:
- /maps (覆盖原版)内容与玩家基本相同
- /load (覆盖原版)加载某个slot存档(不限于自动保存)
- /host (覆盖原版)用法和原版基本相同
- /madmin [uuid] 查看当前管理员,增减管理员
- /addExp <playerId> <num> 增加玩家经验(暂无多用,用户等级系统正在计划中)

### 未来计划
- 增加反捣乱措施
- 完善用户等级机制
- 直接地图评价机制

## 安装方式
在Relase中下载最新jar,放置在config/mods下即可
> 注意: 第一次启动会下载依赖库,网络不好可能会耗费较长时间,且需要多次尝试(下载成功后会自动缓存)

## 关于授权(License)
目前保留权限,只允许个人修改及使用,不允许他人分发
