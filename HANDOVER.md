# ExpiryGuard（过期管家）交接文档

> 当前版本：v1.1.7（versionCode 15）
>
> 当前工程：根目录 `app/`；历史嵌套工程已移除。
>
> 远程仓库：`https://github.com/yz8023/get-end-data`

## 1. 技术与数据

- Kotlin、Jetpack Compose Material3、Hilt、Room、DataStore、WorkManager、CameraX、Coil、Vico。
- `minSdk=26`，`compileSdk=36`，`targetSdk=35`，JDK 21。
- 应用完全离线，无后端和第三方 API Key。
- Room 数据库：`expiry_guard_db`，当前版本 **6**；所有迁移集中在 `AppDatabase.ALL_MIGRATIONS`，禁止使用生产 destructive fallback。
- 产品有稳定 UUID；本地数字 ID 只用于数据库主键，不能用于跨设备恢复身份。

## 2. 本地构建

```bash
git clone https://github.com/yz8023/get-end-data.git
cd get-end-data
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
chmod +x gradlew
./gradlew :app:assembleDebug --no-daemon
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。

## 3. 验证命令

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
# 有 Android 设备或模拟器时：
./gradlew :app:connectedDebugAndroidTest --no-daemon
```

测试覆盖日期/规则、ZIP 条目校验、加密容器（密码/恢复密钥/错误凭据/篡改）、SAF 文件 URI 往返，以及真实 Room 打开器驱动的 v1–v5→v6 数据保留迁移。模拟器测试位于 `app/src/androidTest/`。

## 4. 关键目录

- `data/db/`：Entity、DAO、迁移和 v6 schema。
- `data/repository/ExpiryPolicyRepository.kt`：取件、提醒窗口和自定义分组的统一策略源。
- `domain/engine/`：纯日期规则引擎；调用方传入不可变 `ExpiryPolicy` 和明确的今天。
- `data/backup/BackupManager.kt`：SAF 备份、ZIP 白名单/canonical/大小限制、事务恢复、v1 兼容和 UUID 去重。
- `data/backup/BackupCrypto.kt`：随机数据密钥 + AES-GCM；PBKDF2 密码包裹和独立恢复密钥包裹，支持跨设备恢复，不持久化凭据。
- `data/export/DataExporter.kt`：流式 CSV/JSON SAF 导出。
- `util/LocalDateTicker.kt`：跨午夜、改时间和时区变化刷新。
- `util/ImageUtils.kt`：采样解码、EXIF 方向、受控路径和临时相机原图清理；FileProvider 仅开放 `cache/camera/` 与私有图片目录。
- `.github/workflows/build-apk.yml`：验证、模拟器测试、签名发布三阶段门禁。

## 5. 发布流程

1. 在 `app/build.gradle.kts` 更新 `versionCode`/`versionName`，更新 `RELEASE_NOTES.md`。
2. 确认单测、Lint、Debug 构建和模拟器迁移测试通过。
3. 在 GitHub 仓库 Secrets 配置以下**值**，不要把密钥写入仓库：
   - `EXPIRYGUARD_KEYSTORE_BASE64`
   - `EXPIRYGUARD_STORE_PASSWORD`
   - `EXPIRYGUARD_KEY_PASSWORD`
   - `EXPIRYGUARD_KEY_ALIAS`
4. 推送 `v<versionName>` 标签。CI 先执行测试，再使用 Secrets 签名 release APK、校验签名、生成源码包和 SHA-256，并创建 GitHub Release。
5. 正式发布只接受 `app-release.apk`，不得把 debug APK 改名为正式包。

首次启用独立 release 签名后，旧 CI debug 包可能无法覆盖安装；应先备份数据，再按系统提示处理签名冲突。

## 6. 数据安全注意事项

- 新备份包含名称、日期、备注和图片，并使用 AES-GCM 加密；密码不会保存，恢复密钥只在成功界面显示一次，必须离线备份。忘记密码且丢失恢复密钥将无法恢复。
- 恢复只接受 `data.json` 和受限图片条目，不信任备份中的绝对图片路径；旧版明文 ZIP 仅为兼容导入，不再由新界面生成。
- 加密备份使用随机数据密钥，分别由密码派生密钥和高熵恢复密钥包裹；跨设备恢复不依赖原设备的 Android Keystore。
- 升级前建议先导出 `.expiryguard` 备份；任何迁移验证都不能替代真实旧 APK 的人工升级 smoke test。
- 发布密钥丢失会影响后续覆盖升级，应由仓库所有者离线备份并定期轮换访问凭据。

## 7. 仍需人工验收的 ROM 行为

相机回调、SAF 文档授权、通知运行时权限、WorkManager 调度、时区/DST 和不同厂商 ROM 的通知策略。上述项目不能仅靠 JVM 测试证明。
