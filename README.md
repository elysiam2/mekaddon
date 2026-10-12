# MekAddon — Mekanism 融合工厂副输入升级

一个 Mekanism 附属模组（Minecraft **1.21.1** / NeoForge **21.1** / Mekanism **10.7.19+**）。

它给**融合工厂（Combining Factory）**加了两种真正的升级物品，**两者互斥，同一台机器只能装其中一个**：

| 物品 | 作用 |
|---|---|
| **「融合格子升级」（Combining Slot Upgrade）** | 把全机共享的那 **1 个黄色副输入格**，替换成**每个工序各 1 个**的副输入格，让每个工序都能使用**不同的副材料并行加工** |
| **「额外堆叠升级」（Extra Stack Upgrade）** | 把副输入格的**堆叠上限翻倍**：装 1 个 ×2，2 个 ×4 …… 最多 8 个 ×256。机器原本那个共享副输入格（融合工厂的黄格 / 压模机的模具格）也在生效范围内 |

> 两者是同一个问题的两种解法（「怎么让这台机器的副输入更能装」），所以不能共存。

---

## 特性

| 特性 | 说明 |
|---|---|
| 真·升级类型 | 向 Mekanism 的 `Upgrade` 枚举注入两个新常量 `COMBINING_SLOTS`（黄色，上限 1 个/台）与 `EXTRA_STACK`（紫色，上限 8 个/台），在升级窗口里像原版升级一样安装、显示、卸载 |
| 互斥机制 | 装了一个，另一个就**装不进升级槽**（升级窗口里也不再列出它），并给出明确提示；卸下后另一个立刻恢复可用 |
| 安装方式 | 升级界面安装；或手持物品 **潜行 + 右键** 机器（物品进入升级槽，由机器自身逻辑完成安装，两条路殊途同归） |
| 每工序副输入格 | 基础 3 / 进阶 5 / 精英 7 / 终极 9 个工序，各得 1 个独立副输入格 |
| 真实参与加工 | 额外副输入格不是储物格：每个工序的配方查找、缓存校验、均分排序都按列使用各自的副材料 |
| 堆叠上限翻倍 | 按已装「额外堆叠升级」数量放大上限：`×2^N`，N=1..8 即 ×2 / ×4 / ×8 / ×16 / ×32 / ×64 / ×128 / ×256 |
| 即时生效 | 放大倍数由槽位**实时读取**升级数量算出，装卸升级立刻生效，不需要重开界面、不需要重建库存 |
| 可逆 | 随时在升级界面卸下，物品原样返还，机器恢复原样 |
| 持久化 | 状态随方块实体存档，重启/跨维度不丢失 |
| 自动兼容 | Mekanism **Extras**（Absolute 及以上融合工厂）与 **Mek Energistics** 替换界面：装了就启用对应兼容层，没装则完全跳过 |

### 支持的机器

「融合格子升级」（每工序一排副输入格）只对融合工厂生效；「额外堆叠升级」的生效范围更广，凡是本模组接管的**副输入格**都算：

| 机器 | 融合格子升级 | 额外堆叠升级 | 说明 |
|---|---|---|---|
| Mekanism 融合工厂 | ✔ 基础 / 进阶 / 精英 / 终极 | ✔ 共享黄格，或（装了融合格子升级时）每工序那排 | 两种升级互斥 |
| Mekanism Extras 融合工厂 | ✔ Absolute 及以上 | ✔ 同上 | `ExtrasStackedInputSlot` 在其档位倍率（`8 << tier`）之上再乘升级倍率 |
| Mek Energistics 转换出的机器 | ✔ 其替换 GUI 由兼容 mixin 适配 | ✔ 同上 | `MeGuiFactory` 系列界面 |
| Mek MM 数控压模机（`mekmm:cnc_stamper`） | — | ✔ 模具格 | 与主输入格配对的那个第二输入 |
| Mek MM 压模工厂（`mekmm:basic_stamping_factory` 等，含各等级） | — | ✔ 共享模具格（`extraSlot`） | 结构与融合工厂的共享副输入格相同 |
| Mek MM 压制机（`mekmm:presser`） | — | ✔ 第二 / 第三输入格（y=35、y=54） | 被压制的物品是主输入，升级只放大与它配对的两个辅料格 |
| Mek MM 压制工厂（`mekmm:basic_pressing_factory` 等，含各等级） | — | ✔ 共享的第二 / 第三输入格（`secondarySlot`、`tertiarySlot`） | 同上 |
| Mekanism Extras 压制工厂（`mekanism_extras:absolute_pressing_factory` 等，含各等级） | — | ✔ 共享的第二 / 第三输入格 | 走 Extras 自己的槽类（`ExtrasStackedInputSlot`），在其档位倍率之上再乘升级倍率 |
| Mekanism Extras 压模工厂（`mekanism_extras:absolute_stamping_factory` 等，含各等级） | — | ✔ 共享模具格 | 同上 |

其它机器（普通工厂等）不在支持范围内：升级既不会出现在它们的可用升级列表里，也读不到倍率。

### 合成配方

「融合格子升级」：

```
钢锭      富集碳    钢锭
富集碳    速度升级  富集碳
钢锭      富集碳    钢锭
```

即 4× 钢锭（`mekanism:ingot_steel`）+ 4× 富集碳（`mekanism:enriched_carbon`）+ 1× 速度升级（`mekanism:upgrade_speed`）。

「额外堆叠升级」：

```
钢锭      富集碳    钢锭
富集碳    钢制外壳  富集碳
钢锭      富集碳    钢锭
```

即 4× 钢锭 + 4× 富集碳 + 1× 钢制外壳（`mekanism:steel_casing`）。

---

## 使用

1. **选一条路线。** 两种升级互斥：
   - 装「融合格子升级」→ 主输入行下方出现一排与工序数相同的副输入格，每个工序可以用不同的副材料；
   - 或装「额外堆叠升级」（若干张）→ 机器原本那**一个**共享副输入格的堆叠上限翻倍（×2 … ×256）。
2. 装错了？在升级界面把它卸下，物品原样返还，另一个升级立刻又能装了。
3. 已放进格子的物品不会因为卸下升级而消失，只是上限回落。

---

## 实现原理

### 1. 向封闭枚举注入升级类型

Mekanism 的 `mekanism.api.Upgrade` 是封闭枚举，且 `UpgradeUtils#getItem` 按穷举 `tableswitch`
处理序数，第三方无法用常规手段扩展。本模组的 `UpgradeMixin` 在 `Upgrade.<clinit>` 内部、
`$VALUES` / `BY_ID` / `CODEC` / `EnumUtils.UPGRADES` 派生**之前**替换 `$values()` 的返回值，
追加 `COMBINING_SLOTS`（ordinal 7）与 `EXTRA_STACK`（ordinal 8）——所有派生结构自动把新常量收进来。

枚举常量本身不能用反射构造（JDK 明确禁止），因此先尝试 `MethodHandles.privateLookupIn`
调用私有构造器，失败则退回 `sun.misc.Unsafe.allocateInstance` 手工分配并逐字段填充（全部经反射调用，
保证在 `--release 21` 下可编译）。`UpgradeUtilsMixin` 则把两个常量分别映射回对应物品，卸载时才能返还。

### 2. 共享槽 → 每工序一列

原版把副输入建模为**全机共享**的 `extraSlot` + 单个 `extraInputHandler`，每个工序的缓存配方都读它们。
本模组不去复制整套配方机器，而是：

- `ExtraRowLayout` 直接在**活体槽列表**上追加/移除一排副输入格（不替换任何既有槽，避免破坏槽位配色与输出空间统计）；
- 在每条已知工序号的代码路径上，先把 `extraSlot` / `extraInputHandler` 重指向当前列，再放行原版逻辑
  （`CombiningFactoryExtraSlotsMixin#mekaddon$selectProcess`）；
- `FactoryCachedRecipeMixin` 钩住 `getCachedRecipe(process)`——配方查找与「均分/排序」两条路径的
  唯一公共入口，保证按列校验副输入，而不是拿「上次选中的那一格」糊弄所有列。

### 3. 堆叠上限翻倍

`ExtraStackState` 只做一件事：从 `TileComponentUpgrade` 里读出 `EXTRA_STACK` 的已装数量（0..8），
得出倍率 `2^N`。放大动作落在槽位自己身上 ——

- Mekanism 侧：`StackScaledInputSlot#getLimit`（共享黄格）与其子类 `AddonExtraInputSlot#getLimit`（每工序那排）
  都返回 `super.getLimit(stack) * 2^N`；
- Extras 侧：`ExtrasStackedInputSlot#getLimit` 在 `StackableInputInventorySlot` 的档位倍率之上再乘 `2^N`。

共享黄格是在 `TileEntityCombiningFactory#addSlots`（以及 Extras 的同名方法）里用 `@Redirect`
**在创建那一刻**换成自己的子类的：此时它还没被注册到任何 holder / 侧配置 / 配方处理器，所以安全；
换成「机器建好之后再替换槽对象」就会让这些引用失效——那正是本项目早期踩过的坑。

`getLimit` 是 Mekanism 询问槽位容量的**唯一入口**（`insertItem`、`setStackSize`、容器的
`getMaxStackSize` 都走它），所以不需要维护任何镜像状态，也不存在「改了倍率但机器还在用旧上限」的问题。
不可堆叠物品（原版上限 1）不参与倍数放大，避免把不能叠的东西叠起来。

### 4. 互斥的落地方式

规则不会存成任何标志位，而是每次现算，所以装卸升级后立刻生效。几个落脚点，各管一件事：

| 落脚点 | 作用 |
|---|---|
| `BasicInventorySlotGuardMixin` | 由 `TileComponentUpgradeMixin` 在组件构造时把一个「守卫谓词」装到机器的升级槽上。守卫在槽位原有校验**通过之后**再咨询一次，因此只能把「可以」变「不可以」。这条覆盖：手动拖拽 / Shift 点击（`InventoryContainerSlot#mayPlace` 用一次模拟插入来问槽位）、自动化、以及本模组自己的潜行 + 右键 |
| `TileComponentUpgradeMixin#supports` | 阻止 `tickServer` 安装冲突的那一个 —— 这是升级槽守卫堵不住的唯一缝隙：玩家可以在另一个升级「正在安装」的过程中先把物品塞进升级槽 |
| `TileComponentUpgradeMixin#getSupportedTypes` | 升级窗口的「可用升级」一栏就是从这里取的，于是冲突项直接不显示 |
| `TileComponentUpgradeMixin#addUpgrades` | 原版物品走的是「右键直接安装」而不是填槽，这里再兜一道；装不上就返回 0，Mekanism 会原样保留物品 |

判定刻意做成**不对称**的：只有当「自己没装、对方装了」时才隐藏 / 拒绝。这样一台在规则生效前就同时装着两个升级的旧存档不会因为 `supports` 变 false 而丢计数，安装过的升级也永远不会从窗口里消失。

### 5. 持久化与同步

激活状态由 `ExtraRowState` 管理，经 `UpdateTagWriteMixin` / `UpdateTagReadMixin` 随方块实体的
update tag 写入与恢复；容器侧由 `MekanismContainerMixin` / `ContainerContentGuardMixin` 保证
客户端与服务端槽内容一致。倍率与互斥判断都不需要额外同步：它们完全由 Mekanism 自己同步的升级数量推导。

### 6. 三份 mixin 配置，按需启用

| 配置 | 启用条件 |
|---|---|
| `mekaddon.mixins.json` | 总是启用（Mekanism 主线） |
| `mekaddon.extras.mixins.json` | 目标全部字符串化，未装 Mekanism Extras 时自动跳过 |
| `mekaddon.mekenergistics.mixins.json` | 由 `MekAddonMixinPlugin` 逐目标探测字节码，装了 Mek Energistics 才逐个应用（适配其替换工厂 GUI：窗口加高、进度箭头重定位、补绘缺失槽位） |

---

## 构建

需要 **JDK 21**。

```bash
./gradlew build        # Windows: gradlew.bat build
```

产物：`build/libs/mekaddon-1.1.0+1.21.1.jar`。

> 若克隆后缺少 `gradle/wrapper/gradle-wrapper.jar`，用本机 Gradle 执行 `gradle wrapper` 重新生成即可。
> Mekanism 依赖来自 `modmaven.dev` 仓库；`build.gradle` 配置了腾讯云镜像优先，加速国内环境。

---

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.194+ |
| Mekanism | 1.21.1-10.7.19+（编译期 `compileOnly`，不打包） |
| Mekanism Extras / Mek Energistics | 可选，存在即启用对应兼容层 |

---

## 已知限制与风险

1. **版本敏感**：大量 Mixin 依赖 Mekanism 内部字段与方法签名（已在 10.7.x 验证）。Mekanism 大版本更新
   （如 10.7 → 10.8）若改动这些内部结构，需要同步适配。
2. **升级枚举注入**：占用 `Upgrade` 枚举 ordinal 7、8。若其它附属也向同一枚举注入常量，可能产生序数冲突
   （本模组按「直接追加在原版 7 个常量之后」处理，冲突概率低但非零）。
3. **GUI 适配**：第三方替换界面（如 Mek Energistics）的布局由其自身决定，兼容层按其当前实现定位；
   其更新可能需要重新校准坐标。装上一个升级后，冲突项要等升级窗口刷新（重开界面）才会从列表里消失。
4. **倍率基数**：`×2^N` 乘的是槽位**原本的上限**（普通可堆叠物品为 64，即 64 → 128 → … → 16384；
   Extras 档位则在其 `8 << tier` 倍率之上再乘）。若希望改成「封顶 N 个」而不是「倍数」，只需改
   `ExtraStackState#scaledLimit` 一处。
5. **旧存档**：互斥只约束**新的**安装，不会自动卸下任何东西。规则生效前就同时装着两个升级的机器保持原样
   （仍能看到每工序副输入格，倍率也照常生效）；只要卸下其中一个，就再也装不回去了。

---

## 已修复的历史问题（1.1.0）

两个长期存在的界面问题在 1.1.0 里一起解决，根因都不是「包里有冲突模组」，而是本模组自己的设计缺陷：

0. **早期构建的修法仍然不够 —— 槽位还是画不出来**。
   早期实现试图把那一排槽写进「机器的库存容器」，结果日志直接报出
   `could not add secondary input slot … to the machine's inventory holder`：本模组能拿到的那份列表，并不是菜单读的那一份。
   现在改成**在菜单真正读取的那一层追加**（`ConfigHolder#getSlots(null, …)`，菜单就是从这里取整机槽列表），
   于是不再依赖容器的具体实现类、列表可不可变、以及拿到的是不是同一个容器对象。

1. **界面里根本不出现新增的副输入格（所有机器）**。
   旧实现是**在运行时把那一排槽加进 / 移出机器的库存容器**，前提是「`holder.getInventorySlots(null)` 返回
   的是容器自己那份内部列表」。这个前提不成立时（机器由另一份包装/过滤过的列表供槽），加进去的槽只落在
   一份临时副本里：一排副输入格装好了却谁都看不到，而本该让位的共享黄格还留在界面上——正是截图里的样子。
   现在改成：**槽只创建一次、永远留在容器里**，界面上到底显示哪一个由槽自己的
   `createContainerSlot()` 依据 `ExtraRowState.isUnlocked` 决定 —— 和窗口加高、箭头下移用的是**同一个答案**，
   两边不可能再对不上。

2. **Extras / Mek Energistics 界面里背包压在产物行上（错位）**。
   Extras 的 `ExtraFactoryContainer` 自己覆写了 `getInventoryYOffset` 且不调 super，所以挂在基类
   `MekanismContainer` 上的补丁对它不生效，机器那几行下移了、玩家背包没跟着下移
   → 背包直接盖住产物行。1.1.0 增加 `ExtraFactoryContainerMixin` 补上这一处（Mek Energistics 的工厂菜单继承
   自它，一并覆盖）。

另外顺手修掉了两个相关的隐患：`ConfigInfo#addSlotInfo` 会把 `DataType` **追加**到槽自己的列表上，而
Mekanism 在「该列表长度 ≥ 配置支持的类型数」时就不再去解析槽的类型（界面会退化成灰底无框）——所以侧配置注册
改成了**先比较再写**；同时 `GuiMekanism#findDataType` 对本模组的副输入格直接返回 `DataType.EXTRA`，槽框颜色
不再依赖侧配置的状态。

---

## License

[MIT](LICENSE)
