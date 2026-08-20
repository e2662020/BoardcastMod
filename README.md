# BoardcastMod

一个面向 **Minecraft 1.21** 的 Fabric 客户端模组。它把 Scoreboard Helper、
Scoreboard Overhaul、OBS Overlay 以及聊天正则记录器中最实用的部分，合并成一个
可配置的模组。

## 功能

1. **实时比分板 CSV 导出（Scoreboard Helper 格式）**
   - 默认每个刻轮询一次，一旦计分项 / 持有者 / 分数发生变化，立即以原子方式重写文件。
   - 将一份 Scoreboard Helper 风格的 CSV（`Player,Score`）写入
     `.minecraft/config/boardcastmod/scoreboard_helper.csv`。
   - 行始终来自**侧边栏计分项**（`Score` 列）。
   - **从其他计分项追加列**：在设置里勾选当前世界中实时采集到的计分项
     （需先进入世界，选项会自动刷新，还有「刷新」按钮强制立即更新）。每个勾选的
     计分项会在 `Score` 之后追加一列（第 3 列、第 4 列……），按**玩家名匹配**，
     填入该玩家在对应计分项中的分数。
     - 单人模式读取服务端完整计分板（含全部计分项）；多人服务器只能列出已同步到
       客户端的计分项。
   - 开启「只保留侧边栏显示的计分项」后，只导出侧边栏当前可见的行（受最大行数
     限制，并排除隐藏行）。
   - 用 `/boardcastmod export` 强制立即导出。

2. **Scoreboard Overhaul 风格侧边栏 + OBS Overlay 兼容**
   - 用可配置的渲染器替换原版侧边栏。
   - 标题、持有者、分数和背景颜色均可在配置界面中编辑。
   - 位置、边距、最小/最大宽度、行高、最大行数和文字阴影均可配置。
     可选 Scoreboard Overhaul 风格的 1K/1M 数字缩写。
   - 若安装了 `obs_overlay` / `obs-overlay`，会自动隐藏原生侧边栏，让 OBS 自行
     绘制比分板以避免重复渲染（可在设置中关闭）。
   - `/boardcastmod sidebar hide|show|toggle` 也可控制侧边栏可见性。

3. **血量以心形显示**
   - 血量计分项可以心形显示（`NUMBER` / `HEARTS` / `AUTO`）。
   - 一颗心等于 2 HP，支持半颗心：
     - `ICONS`：整颗心用整心颜色绘制，最后半颗心用半心颜色。
     - `NUMBER_WITH_HEART`：显示类似 `3.5 ♥` 的值。
   - `AUTO` 会对原版心形渲染类型，或名称 / 判据中包含 `health`、`hp`、
     `heart`、`生命`、`血量`、`血` 的计分项启用心形。

4. **计时器比分板**
   - 通过关键词（`timer`、`time`、`clock`、`计时`、`倒计时`、`cd`）和判据名称
     识别计时器计分项。
   - 原始分数仍会每个刻写入 CSV，因此计时器更新是实时传输的。
   - 一键切换到秒：按 **N**（可在原版按键设置中修改），或使用
     `/boardcastmod timer seconds`。
   - 支持原始刻、小数秒（`12.5s`）和 `mm:ss` 时钟格式。
   - 如果服务器计时器本身就是秒，请关闭「计时器源为刻」。

5. **客户端聊天正则抓取**
   - 抓取 GAME/overlay 消息、玩家聊天、系统消息和发出的聊天输入。
   - 只要任一配置的 Java 正则匹配（`Matcher.find`），整条消息就会被追加写入 txt 文件。
   - 默认规则为 `.*`（抓取全部）。默认输出：
     `.minecraft/config/boardcastmod/chat_capture.txt`。
   - 时间戳、忽略大小写和自动文件轮换均可配置。

## 设置界面

- 使用 **Cloth Config API**（必需依赖）。
- 与 **ModMenu** 完全集成：打开「模组」→ BoardcastMod → 设置。
- 配置文件：`.minecraft/config/boardcastmod.json`。

## 命令

| 命令 | 作用 |
| --- | --- |
| `/boardcastmod export` / `csv` | 下一刻强制导出比分板 CSV |
| `/boardcastmod sidebar hide` | 隐藏原生侧边栏 |
| `/boardcastmod sidebar show` | 显示原生侧边栏 |
| `/boardcastmod sidebar toggle` | 切换侧边栏可见性 |
| `/boardcastmod timer seconds` | 切换计时器秒显示模式 |
| `/boardcastmod timer clock` | 切换 `mm:ss` 时钟格式 |

## 按键绑定

- `N` — 切换计时器秒显示模式（原版按键设置中的「BoardcastMod」分类）。

## 构建

需要 JDK 21。构建脚本使用 Gradle 9.4。

Windows：

```bat
gradlew.bat build
```

macOS / Linux：

```sh
chmod +x gradlew
./gradlew build
```

如果 wrapper jar 缺失，脚本会尝试下载一次。也可以直接在 IntelliJ IDEA 中打开本
目录，用 IDEA 的 Gradle 导入。

重映射后的 jar 生成于 `build/libs/BoardcastMod-1.0.0.jar`。

## 参考仓库

本模组合并了 Scoreboard Helper、Scoreboard Overhaul、zziger/obs-overlay 以及
聊天正则记录器的思路。参考仓库的克隆脚本位于 `scripts/clone-reference-repos.bat`
（Windows）和 `scripts/clone-reference-repos.sh`（macOS/Linux）。
