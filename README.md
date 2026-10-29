# ExpiryGuard（过期管家）

离线 Android 清单与保质期提醒应用，使用 Compose、Room、Hilt、DataStore 和 WorkManager。

- 当前版本：v1.1.7（versionCode 15）
- 最低系统：Android 8.0（API 26）
- 构建与测试：见 [HANDOVER.md](HANDOVER.md)
- 发布说明：见 [RELEASE_NOTES.md](RELEASE_NOTES.md)

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

新备份使用 AES-GCM 加密：设置至少 8 位密码，并在成功后离线保存一次性恢复密钥；密码或恢复密钥均可用于跨设备恢复。旧版明文备份仍可导入，请仅保存到可信位置。
