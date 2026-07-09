# DG Lib 发布指南

这份指南用于以后每次更新 DG Lib 之后，按固定步骤导出、验证并发布到 `page` 分支。

## 0. 发布前约定

- 发布仓库当前的 `build.gradle` 已经改成只发布 `jar` 和 `sourcesJar`，不会再把 Forge 开发态依赖写进发布 POM。
- 发布产物的 Maven 根目录来自 `build/repo`。
- `page` 分支用于承载对外访问的 Maven 仓库内容。

## 1. 更新代码

先完成正常的功能修改、资源修改、文案修改或 bug 修复。

如果这次更新需要对外发布新版本，优先检查 `gradle.properties`：

- `mod_version`
- `mod_name`
- `mod_description`

如果是正式发布，建议把版本号从 `1.0-SNAPSHOT` 改成稳定版本，例如 `1.0.1`、`1.1.0`。

如果只是临时测试，可以继续使用 `SNAPSHOT`，但对外依赖时最好避免长期使用相同快照版本。

## 2. 本地构建检查

在项目根目录执行：

```powershell
./gradlew.bat clean build
```

如果你只想快速看发布是否可行，也可以执行：

```powershell
./gradlew.bat publishMavenJavaPublicationToLocalRepoRepository
```

这一步的目标是确认：

- 代码能编译
- `jar` 能正常生成
- `sourcesJar` 能正常生成
- 发布任务能生成本地 Maven 仓库内容

## 3. 检查发布产物

发布完成后，重点检查下面两个文件：

- `build/publications/mavenJava/pom-default.xml`
- `build/repo/org/nooberic/dglib/`

你要确认：

- POM 中没有 `net.minecraftforge:forge`
- POM 中没有 `*_mapped_official_*`
- 发布目录下确实生成了当前版本的 `jar`、`pom`、`sourcesJar`

如果以后又看到 Forge 开发态依赖出现在发布元数据里，说明发布配置被改回了不安全的形式，需要先修 `build.gradle` 再继续发布。

## 4. 同步到 page 分支

如果 `page` 分支是你的对外 Maven 仓库根目录，那么发布时要把 `build/repo` 的内容同步过去，并保持目录结构不变。

推荐流程：

1. 切到 `page` 分支。
2. 清理旧版本里不再需要的文件。
3. 把 `build/repo` 里的内容复制到 `page` 分支根目录。
4. 检查目标分支里是否存在类似下面的结构：
   - `org/nooberic/dglib/`
   - `org/nooberic/dglib/maven-metadata.xml`
   - `org/nooberic/dglib/<version>/`
5. 提交并推送 `page` 分支。

如果你在 Windows 上手工复制，可以用资源管理器，也可以用 `robocopy` 保持目录结构同步。

示例思路：把 `build/repo` 作为源，把 `page` 分支工作区根目录作为目标。

## 5. 更新依赖方引用

当你发布了新版本之后，其他模组引用时要更新版本号。

在依赖方里通常写成：

```groovy
repositories {
    maven {
        url = uri("https://nooberic0746.github.io/dg-lib/")
    }
}

dependencies {
    compileOnly fg.deobf("org.nooberic:dglib:1.0.1")
    runtimeOnly fg.deobf("org.nooberic:dglib:1.0.1")
}
```

如果你发布的是快照版本，就把版本号换成对应的 `SNAPSHOT`。

## 6. 最后验证

发布到 `page` 分支之后，建议再确认一次：

- 仓库地址可以访问
- 依赖方能正常拉取新版本
- 不会再解析到 `net.minecraftforge:forge:..._mapped_official_1.20.1`
- 游戏里或者依赖工程里能正常加载 `org.nooberic.dglib.api.DgLibApi`

## 7. 每次更新时的固定顺序

以后你可以直接按这个顺序执行：

1. 改代码或资源
2. 改版本号
3. `./gradlew.bat clean build`
4. `./gradlew.bat publishMavenJavaPublicationToLocalRepoRepository`
5. 检查 `pom-default.xml`
6. 同步 `build/repo` 到 `page` 分支
7. 提交并推送 `page` 分支
8. 在依赖方验证新版本

## 8. 常见问题

### 问题：发布出来的 POM 里又出现 Forge 依赖

通常是发布配置又回到了把 Java 组件直接导出的方式。先检查 `build.gradle` 的 `publishing` 块，确认仍然是显式发布 `jar` 和 `sourcesJar`，而不是 `from components.java`。

### 问题：发布后下游还是解析失败

优先检查版本号和仓库缓存：

- 下游是否还在使用旧版本号
- `page` 分支是否真的推送成功
- 依赖方是否命中了旧缓存

### 问题：只想快速验证一次新内容

最短流程是：

1. 改代码
2. `./gradlew.bat clean build`
3. `./gradlew.bat publishMavenJavaPublicationToLocalRepoRepository`
4. 检查 POM
5. 同步到 `page` 分支
