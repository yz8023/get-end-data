package com.expiryguard.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.backup.BackupManager
import com.expiryguard.app.data.backup.RestoreMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 仅负责备份/恢复界面状态；文件与数据库细节由 [BackupManager] 处理。 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: BackupManager,
    @ApplicationContext appContext: Context
) : ViewModel() {
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress: StateFlow<Float> = _backupProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow(0f)
    val restoreProgress: StateFlow<Float> = _restoreProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<String?>(null)
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    private val _restoreMode = MutableStateFlow(RestoreMode.INCREMENTAL)
    val restoreMode: StateFlow<RestoreMode> = _restoreMode.asStateFlow()

    private val _backupFilePath = MutableStateFlow<String?>(null)
    val backupFilePath: StateFlow<String?> = _backupFilePath.asStateFlow()

    private val _pendingRecoveryCode = MutableStateFlow<String?>(null)
    val pendingRecoveryCode: StateFlow<String?> = _pendingRecoveryCode.asStateFlow()

    private val prefs = appContext.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    init {
        _lastBackupTime.value = prefs.getString(PREF_LAST_BACKUP_TIME, null)
    }

    fun setRestoreMode(mode: RestoreMode) {
        if (!_isRestoring.value) _restoreMode.value = mode
    }

    fun backupData(destinationUri: Uri, password: CharArray? = null) {
        if (_isBackingUp.value || _isRestoring.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0f
            _errorMessage.value = null
            _successMessage.value = null
            _backupFilePath.value = null
            _pendingRecoveryCode.value = null
            try {
                val result = backupManager.createBackup(destinationUri, password) { progress ->
                    _backupProgress.value = progress
                }
                _backupFilePath.value = result.uri.toString()
                _pendingRecoveryCode.value = result.recoveryCode
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                prefs.edit().putString(PREF_LAST_BACKUP_TIME, now).apply()
                _lastBackupTime.value = now
                _successMessage.value =
                    "备份完成：${result.productCount} 条记录、${result.imageCount} 张图片，已保存到${result.destinationLabel}" +
                        if (result.encrypted) "；请立即保存恢复密钥" else ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "备份失败：${e.message ?: "未知错误"}"
            } finally {
                password?.fill('\u0000')
                _isBackingUp.value = false
            }
        }
    }

    fun acknowledgeRecoveryCode() {
        _pendingRecoveryCode.value = null
    }

    fun restoreData(uri: Uri, password: CharArray? = null, recoveryCode: String? = null) {
        if (_isBackingUp.value || _isRestoring.value) return
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreProgress.value = 0f
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val result = backupManager.restoreBackup(
                    uri = uri,
                    mode = _restoreMode.value,
                    password = password,
                    recoveryCode = recoveryCode,
                    onProgress = { progress -> _restoreProgress.value = progress }
                )
                val base = if (_restoreMode.value == RestoreMode.OVERWRITE) {
                    "覆盖恢复完成：${result.insertedProducts} 条记录"
                } else {
                    "增量恢复完成：新增 ${result.insertedProducts} 条，跳过 ${result.skippedProducts} 条"
                }
                _successMessage.value = base + if (result.missingImages > 0) {
                    "；${result.missingImages} 张图片未包含在备份中"
                } else {
                    ""
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "恢复失败，现有数据未变更：${e.message ?: "未知错误"}"
            } finally {
                password?.fill('\u0000')
                _isRestoring.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private companion object {
        const val PREF_LAST_BACKUP_TIME = "last_backup_time"
    }
}
