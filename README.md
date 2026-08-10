# DG Lib — NeoForge 26.1.2 移植版

DG-LAB 设备控制库：通过 WebSocket 连接 DG-LAB 设备，提供配对、强度控制、波形调度与游戏 UI 支持。

原仓库 [NoobEric0746/dg-lib](https://github.com/NoobEric0746/dg-lib) 基于 Minecraft Forge 1.20.1。本仓库已迁移至 **NeoForge 26.1.2.74**（原作者未发布 26.1.2 版本，此为本地迁移版）。

## 版本信息

- Mod ID：`dglib`，版本 `1.3.3`
- Minecraft：26.1.2 / NeoForge：26.1.2.74 / Java：25
- 构建工具：ModDevGradle（`net.neoforged.moddev` 2.0.143）

## 依赖

- zxing core 3.5.3（通过 jarjar 内嵌进 mod jar，运行时无需单独安装）

## 构建

前置要求：JDK 25。

```bat
gradlew build
```

构建产物：`build/libs/dglib-1.3.3.jar`

## 运行

- 将 `dglib-1.3.3.jar` 放入 `mods` 目录（NeoForge 26.1.2.74 客户端）。
- `dg-toy` 依赖此库；dg-toy 仓库的 `libs/dglib-1.3.3.jar` 与本仓库产物一致，构建 dg-toy 时无需单独拉取。

## 许可

All Rights Reserved（沿用原仓库）。