# MekAddon — Mekanism 融合工厂副输入升级

一个 Mekanism 附属模组（Minecraft **1.21.1** / NeoForge **21.1** / Mekanism **10.7.19+**）。

它为**融合工厂（Combining Factory）**增加一种真正的升级物品——**「融合格子升级」（Combining Slot Upgrade）**：
把全机共享的那 **1 个黄色副输入格**，替换成**每个工序各 1 个**的副输入格，
让每个工序都能使用**不同的副材料并行加工**。

---

## 特性

| 特性 | 说明 |
|---|---|
| 真·升级类型 | 向 Mekanism 的 `Upgrade` 枚举注入新常量 `COMBINING_SLOTS`（黄色，上限 1 个/台），在升级窗口里像原版升级一样安装、显示、卸载 |
| 安装方式 | 升级界面安装；或手持物品 **潜行 + 右键** 机器（物品进入升级槽，由机器自身逻辑完成安装，两条路殊途同归） |
| 每工序副输入格 | 基础 3 / 进阶 5 / 精英 7 / 终极 9 个工序，各得 1 个独立副输入格 |
| 真实参与加工 | 额外副输入格不是储物格：每个工序的配方查找、缓存校验、均分排序都按列使用各自的副材料 |
| 可逆 | 随时在升级界面卸下，物品返还，机器恢复原样 |
| 持久化 | 状态随方块实体存档，重启/跨维度不丢失 |
| 自动兼容 | Mekanism **Extras**（Absolute 及以上融合工厂）与 **Mek Energistics** 替换界面：装了就启用对应兼容层，没装则完全跳过 |

### 支持的机器

| 机器 | 覆盖范围 |
|---|---|
| Mekanism 融合工厂 | 基础 / 进阶 / 精英 / 终极 全档位 |
| Mekanism Extras 融合工厂 | Absolute 及以上档位（其 `StackableInputInventorySlot` 独立实现，走专属兼容层） |
| Mek Energistics 转换出的机器 | 其替换 GUI（`MeGuiFactory` 系列）由逐目标 gating 的兼容 mixin 适配 |

### 合成配方

```
钢锭      富集碳    钢锭
富集碳    速度升级  富集碳
钢锭      富集碳    钢锭
```

即 4× 钢锭（`mekanism:ingot_steel`）+ 4× 富集碳（`mekanism:enriched_carbon`）+ 1× 速度升级（`mekanism:upgrade_speed`）。

---

## 使用

1. 合成「融合格子升级」；
2. 打开融合工厂界面 → **升级** → 在可用升级中安装；或手持物品潜行 + 右键机器；
3. 重新打开界面，主输入行下方会出现一排与工序数相同的副输入格；
4. 想撤销？在升级界面把它卸下即可。

---

## 实现原理

### 1. 向封闭枚举注入升级类型

Mekanism 的 `mekanism.api.Upgrade` 是封闭枚举，且 `UpgradeUtils#getItem` 按穷举 `tableswitch`
处理序数，第三方无法用常规手段扩展。本模组的 `UpgradeMixin` 在 `Upgrade.<clinit>` 内部、
`$VALUES` / `BY_ID` / `CODEC` / `EnumUtils.UPGRADES` 派生**之前**替换 `$values()` 的返回值，
追加 `COMBINING_SLOTS`（ordinal 7）——所有派生结构自动把新常量收进来。

枚举常量本身不能用反射构造（JDK 明确禁止），因此先尝试 `MethodHandles.privateLookupIn`
调用私有构造器，失败则退回 `sun.misc.Unsafe.allocateInstance` 手工分配并逐字段填充（全部经反射调用，
保证在 `--release 21` 下可编译）。`UpgradeUtilsMixin` 则把该常量映射回物品，卸载时才能返还。

### 2. 共享槽 → 每工序一列

原版把副输入建模为**全机共享**的 `extraSlot` + 单个 `extraInputHandler`，每个工序的缓存配方都读它们。
本模组不去复制整套配方机器，而是：

- `ExtraRowLayout` 直接在**活体槽列表**上追加/移除一排副输入格（不替换任何既有槽，避免破坏槽位配色与输出空间统计）；
- 在每条已知工序号的代码路径上，先把 `extraSlot` / `extraInputHandler` 重指向当前列，再放行原版逻辑
  （`CombiningFactoryExtraSlotsMixin#mekaddon$selectProcess`）；
- `FactoryCachedRecipeMixin` 钩住 `getCachedRecipe(process)`——配方查找与「均分/排序」两条路径的
  唯一公共入口，保证按列校验副输入，而不是拿「上次选中的那一格」糊弄所有列。

### 3. 持久化与同步

激活状态由 `ExtraRowState` 管理，经 `UpdateTagWriteMixin` / `UpdateTagReadMixin` 随方块实体的
update tag 写入与恢复；容器侧由 `MekanismContainerMixin` / `ContainerContentGuardMixin` 保证
客户端与服务端槽内容一致。

### 4. 三份 mixin 配置，按需启用

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

产物：`build/libs/mekaddon-1.0.0+1.21.1.jar`。

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
2. **升级枚举注入**：占用 `Upgrade` 枚举 ordinal 7。若其它附属也向同一枚举注入常量，可能产生序数冲突
   （本模组按「直接追加在原版 7 个常量之后」处理，冲突概率低但非零）。
3. **GUI 适配**：第三方替换界面（如 Mek Energistics）的布局由其自身决定，兼容层按其当前实现定位；
   其更新可能需要重新校准坐标。

---

## 目前已知问题

1. **Mekanism Extras 兼容：GUI 错位**。
   Extras（Absolute 及以上档位）融合工厂上升级本身可正常安装、槽位逻辑正常工作，
   但界面中新增的副输入格一排显示位置有偏移。

2. **「新时代科技」（New Age Science and Technology，NAST）整合包：新增槽位不显示**。
   在该包中安装升级后，界面里不出现额外一排副输入格；纯净环境与 Mekanism Extras 环境均无此问题，
   目前尚未确定是包内哪个模组导致的。已针对包内 Mek Energistics 替换工厂界面的模组加了兼容层，
   但问题仍未完全解决，仍在排查中。

---

## License

[MIT](LICENSE)
