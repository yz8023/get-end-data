package com.expiryguard.app.ui.product

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.util.DateUtils
import com.expiryguard.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * 保质期分组（保留枚举作为默认回退）
 *
 * 每个分组代表一个保质期区间，分组是清单的固定属性，不会随时间变化。
 * 每个分组有对应的提醒阈值（提前多少天提醒）。
 *
 * 退货规则：
 * - 1年以上（>365天）：提前45天提醒可退货
 * - 6个月~1年（183~365天）：提前20天提醒
 * - 3个月~6个月（90~183天）：提前15天提醒
 * - 3个月以下（<90天）：今日到期提醒
 * - 当天到期：今日到期提醒
 */
enum class ShelfLifeGroup(
    val label: String,
    val description: String,
    val minDays: Int,
    val maxDays: Int,
    val reminderThreshold: Int
) {
    TODAY("当天到期", "今日到期提醒", 0, 1, 0),
    LESS_THAN_3M("3个月以下", "今日到期提醒", 1, 90, 0),
    THREE_TO_SIX_M("3~6个月", "提前15天提醒", 90, 183, 15),
    SIX_TO_TWELVE_M("6个月~1年", "提前20天提醒", 183, 365, 20),
    MORE_THAN_1Y("1年以上", "提前45天提醒", 365, Int.MAX_VALUE, 45);

    companion object {
        /**
         * 根据保质期天数匹配对应的分组
         */
        fun matchByDays(shelfLifeDays: Int): ShelfLifeGroup {
            return entries.firstOrNull { shelfLifeDays in it.minDays until it.maxDays }
                ?: MORE_THAN_1Y
        }

        /**
         * 根据到期日期和今天计算保质期天数，再匹配分组
         */
        fun matchByExpiryDate(expiryDate: Long): ShelfLifeGroup {
            val today = DateUtils.todayTimestamp()
            val days = DateUtils.daysBetween(today, expiryDate).coerceAtLeast(0)
            return matchByDays(days)
        }
    }
}

/**
 * UI 层保质期分组数据类
 *
 * 由数据库分组实体转换而来，也可从枚举回退生成。
 */
data class ShelfLifeGroupUi(
    val id: Long = 0,
    val name: String,
    val description: String,
    val minDays: Int,
    val maxDays: Int,
    val reminderThreshold: Int,
    val colorHex: String = "#F97316",
    val isDefault: Boolean = false
) {
    companion object {
        /** 从数据库实体转换 */
        fun fromEntity(entity: ShelfLifeGroupEntity) = ShelfLifeGroupUi(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            minDays = entity.minDays,
            maxDays = entity.maxDays,
            reminderThreshold = entity.reminderThreshold,
            colorHex = entity.colorHex,
            isDefault = entity.isDefault
        )

        /** 从枚举回退生成 */
        fun fromEnum(group: ShelfLifeGroup, index: Int) = ShelfLifeGroupUi(
            id = -(index + 1).toLong(), // 负 ID 表示枚举回退
            name = group.label,
            description = group.description,
            minDays = group.minDays,
            maxDays = group.maxDays,
            reminderThreshold = group.reminderThreshold,
            colorHex = when (group) {
                ShelfLifeGroup.TODAY -> "#EF4444"
                ShelfLifeGroup.LESS_THAN_3M -> "#F97316"
                ShelfLifeGroup.THREE_TO_SIX_M -> "#F59E0B"
                ShelfLifeGroup.SIX_TO_TWELVE_M -> "#10B981"
                ShelfLifeGroup.MORE_THAN_1Y -> "#6B7280"
            },
            isDefault = true
        )
    }
}

/**
 * 添加清单页面状态
 */
data class AddProductUiState(
    val name: String = "",
    val productionDate: Long? = null,
    val expiryDate: Long? = null,
    val shelfLifeGroup: ShelfLifeGroupUi? = null,
    val shelfLifeDays: Int = 0,
    val categoryId: Long? = null,
    val photoPath: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val shelfLifeGroups: List<ShelfLifeGroupUi> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 保质期预设选项
 */
data class ShelfLifePreset(
    val label: String,
    val days: Int
)

/** 默认保质期预设列表 */
val defaultShelfLifePresets = listOf(
    ShelfLifePreset("3天", 3),
    ShelfLifePreset("5天", 5),
    ShelfLifePreset("7天", 7),
    ShelfLifePreset("15天", 15),
    ShelfLifePreset("21天", 21),
    ShelfLifePreset("1个月", 30),
    ShelfLifePreset("3个月", 90),
    ShelfLifePreset("6个月", 180),
    ShelfLifePreset("9个月", 270),
    ShelfLifePreset("12个月", 365),
    ShelfLifePreset("120天", 120),
    ShelfLifePreset("270天", 270),
    ShelfLifePreset("3年", 1095),
    ShelfLifePreset("5年", 1825)
)

/**
 * 添加清单 ViewModel
 */
@HiltViewModel
class AddProductViewModel @Inject constructor(
    application: Application,
    private val repository: ProductRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadShelfLifeGroups()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    /**
     * 从数据库加载保质期分组列表
     *
     * 如果数据库为空，则使用默认枚举作为回退
     */
    private fun loadShelfLifeGroups() {
        viewModelScope.launch {
            repository.getAllGroups().collect { entities ->
                val groups = if (entities.isNotEmpty()) {
                    entities.map { ShelfLifeGroupUi.fromEntity(it) }
                } else {
                    // 数据库为空时使用枚举回退
                    ShelfLifeGroup.entries.mapIndexed { index, group ->
                        ShelfLifeGroupUi.fromEnum(group, index)
                    }
                }
                _uiState.update { it.copy(shelfLifeGroups = groups) }
            }
        }
    }

    /**
     * 根据分组范围估算保质期天数
     * 使用分组中点值，对于无上限分组（如1年以上）使用 minDays + 1
     */
    private fun estimateShelfLifeDays(minDays: Int, maxDays: Int): Int {
        return if (maxDays == Int.MAX_VALUE || maxDays > 10000) {
            minDays + 1
        } else {
            (minDays + maxDays) / 2
        }
    }

    /**
     * 选择保质期分组
     *
     * 分组仅用于分类和确定提醒阈值，不会修改已选择的到期日期。
     * shelfLifeDays 使用分组中点值作为实际保质期天数。
     */
    fun selectShelfLifeGroup(group: ShelfLifeGroupUi) {
        _uiState.update { state ->
            state.copy(
                shelfLifeGroup = group,
                shelfLifeDays = estimateShelfLifeDays(group.minDays, group.maxDays)
            )
        }
    }

    /**
     * 更新清单名称
     */
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * 手动选择到期日期，自动匹配对应的保质期分组
     *
     * 如果已设置生产日期，使用"生产日期到到期日"的实际保质期天数匹配分组；
     * 否则用剩余天数作为回退估算。
     */
    fun updateExpiryDate(date: LocalDate) {
        val expiryTs = DateUtils.toTimestamp(date)
        val state = _uiState.value

        // 优先使用实际保质期天数（生产日期 → 到期日）
        val daysForMatching = if (state.productionDate != null) {
            DateUtils.daysBetween(state.productionDate!!, expiryTs).coerceAtLeast(1)
        } else {
            val today = DateUtils.todayTimestamp()
            DateUtils.daysBetween(today, expiryTs).coerceAtLeast(0)
        }

        val matchedGroup = matchGroupByDays(daysForMatching)

        _uiState.update {
            it.copy(
                expiryDate = expiryTs,
                shelfLifeGroup = matchedGroup,
                shelfLifeDays = matchedGroup?.let { estimateShelfLifeDays(it.minDays, it.maxDays) } ?: daysForMatching
            )
        }
    }

    /**
     * 更新生产日期
     */
    fun updateProductionDate(date: LocalDate) {
        _uiState.update { it.copy(productionDate = DateUtils.toTimestamp(date)) }
    }

    /**
     * 根据生产日期 + 保质期预设天数自动计算到期日
     *
     * 到期日 = 生产日期 + (保质期天数 - 1)
     * 保质期 N 天意为生产日期当天为第 1 天，第 N 天为最后有效日。
     * 同时自动匹配保质期分组（使用保质期天数匹配，而非剩余天数）
     */
    fun calculateExpiryFromPreset(preset: ShelfLifePreset) {
        val state = _uiState.value
        val productionDate = state.productionDate ?: return

        // 到期日 = 生产日期 + (保质期天数 - 1)，生产当天算第1天
        val expiryDate = DateUtils.addDays(productionDate, (preset.days - 1).coerceAtLeast(0))

        // 使用保质期天数（preset.days）匹配分组，而非剩余天数
        val matchedGroup = matchGroupByDays(preset.days)

        _uiState.update {
            it.copy(
                expiryDate = expiryDate,
                shelfLifeDays = preset.days,
                shelfLifeGroup = matchedGroup
            )
        }
    }

    /**
     * 根据自定义天数计算到期日
     *
     * 到期日 = 生产日期 + (自定义天数 - 1)
     * 与 calculateExpiryFromPreset 逻辑相同，但接受任意整数天数。
     */
    fun calculateExpiryFromCustomDays(days: Int) {
        val state = _uiState.value
        val productionDate = state.productionDate ?: return

        // 到期日 = 生产日期 + (天数 - 1)，生产当天算第1天
        val expiryDate = DateUtils.addDays(productionDate, (days - 1).coerceAtLeast(0))
        val matchedGroup = matchGroupByDays(days)

        _uiState.update {
            it.copy(
                expiryDate = expiryDate,
                shelfLifeDays = days,
                shelfLifeGroup = matchedGroup
            )
        }
    }

    /**
     * 根据保质期天数匹配对应的分组
     *
     * 优先从数据库分组列表匹配，回退到枚举匹配
     */
    private fun matchGroupByDays(days: Int): ShelfLifeGroupUi? {
        val groups = _uiState.value.shelfLifeGroups
        // 先从数据库分组匹配
        val dbMatch = groups.firstOrNull { days in it.minDays until it.maxDays }
        if (dbMatch != null) return dbMatch

        // 回退到枚举匹配
        val enumMatch = ShelfLifeGroup.matchByDays(days)
        val enumIndex = ShelfLifeGroup.entries.indexOf(enumMatch)
        return ShelfLifeGroupUi.fromEnum(enumMatch, enumIndex)
    }

    fun updateCategory(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun pickImage(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                ImageUtils.saveImage(context, uri)
            }
            if (savedPath != null) {
                _uiState.update { it.copy(photoPath = savedPath) }
            } else {
                _uiState.update { it.copy(errorMessage = "图片保存失败") }
            }
        }
    }

    fun setPhotoPath(path: String?) {
        _uiState.update { it.copy(photoPath = path) }
    }

    fun saveProduct() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入清单名称") }
            return
        }

        if (state.expiryDate == null) {
            _uiState.update { it.copy(errorMessage = "请选择到期日期") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val product = ProductEntity(
                name = state.name.trim(),
                categoryId = state.categoryId,
                photoPath = state.photoPath,
                productionDate = state.productionDate,
                expiryDate = state.expiryDate,
                shelfLifeDays = state.shelfLifeDays,
                notes = null
            )

            val result = repository.insert(product)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "保存失败: ${e.localizedMessage ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}