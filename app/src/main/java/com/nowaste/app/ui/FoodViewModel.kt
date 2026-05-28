package com.nowaste.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nowaste.app.data.FoodItem
import com.nowaste.app.data.FoodRepository
import com.nowaste.app.domain.BatchPhotoPendingNamePrefix
import com.nowaste.app.domain.BatchPhotoPendingNote
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.network.SmartFoodParseConfig
import com.nowaste.app.network.SmartFoodParseResult
import com.nowaste.app.network.SmartFoodTextParser
import com.nowaste.app.notifications.ReminderScheduler
import com.nowaste.app.settings.AppSettings
import com.nowaste.app.settings.SettingsState
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface FoodListUiState {
    data object Loading : FoodListUiState
    data class Ready(
        val items: List<FoodItem>,
        val settings: SettingsState,
    ) : FoodListUiState
}

class FoodViewModel(
    private val repository: FoodRepository,
    private val settings: AppSettings,
    private val appContext: Context,
    private val smartFoodTextParser: SmartFoodTextParser = SmartFoodTextParser(),
) : ViewModel() {
    val foodListUiState: StateFlow<FoodListUiState> =
        combine(
            repository.observeFoodItemsSortedByExpiry(),
            settings.state,
        ) { items, settingsState ->
            FoodListUiState.Ready(items, settingsState)
        }
            .map<FoodListUiState.Ready, FoodListUiState> { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FoodListUiState.Loading,
            )

    fun saveFoodItem(
        id: Long?,
        input: FoodItemInput,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            input.categoryTag.trim().takeIf { it.isNotBlank() }?.let(settings::addCategoryTag)
            if (id == null) {
                repository.addFoodItem(input)
            } else {
                repository.updateFoodItem(id, input)
            }
            onSaved()
        }
    }

    fun deleteFoodItem(
        id: Long,
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.deleteFoodItem(id)
            onDeleted()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        settings.updateReminderTime(hour, minute)
        ReminderScheduler.scheduleDaily(appContext, settings)
    }

    fun updateNearExpiryDays(days: Int) {
        settings.nearExpiryDays = days
        ReminderScheduler.scheduleDaily(appContext, settings)
    }

    fun addCategoryTag(tag: String) {
        settings.addCategoryTag(tag)
    }

    fun deleteCategoryTag(tag: String) {
        settings.deleteCategoryTag(tag)
    }

    fun moveCategoryTag(tag: String, direction: Int) {
        settings.moveCategoryTag(tag, direction)
    }

    fun updateSmartParsingEnabled(enabled: Boolean) {
        settings.smartParsingEnabled = enabled
    }

    fun updateSmartParsingApiUrl(apiUrl: String) {
        settings.smartParsingApiUrl = apiUrl
    }

    fun updateSmartParsingApiKey(apiKey: String) {
        settings.smartParsingApiKey = apiKey
    }

    fun updateSmartParsingModel(model: String) {
        settings.smartParsingModel = model
    }

    fun parseSmartFoodBatchText(
        text: String,
        onParsed: (List<SmartFoodParseResult>) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (!settings.smartParsingEnabled) {
                    onError("请先在设置中开启智能解析。")
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    smartFoodTextParser.parseBatch(
                        text = text,
                        config = SmartFoodParseConfig(
                            apiUrl = settings.smartParsingApiUrl,
                            apiKey = settings.smartParsingApiKey,
                            model = settings.smartParsingModel,
                        ),
                    )
                }
                onParsed(result)
            } catch (error: Throwable) {
                onError(error.message ?: "智能解析失败，请检查配置或稍后重试。")
            }
        }
    }

    fun testSmartParsing(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (!settings.smartParsingEnabled) {
                    onError("请先在设置中开启智能解析。")
                    return@launch
                }
                val message = withContext(Dispatchers.IO) {
                    smartFoodTextParser.testConnection(
                        config = SmartFoodParseConfig(
                            apiUrl = settings.smartParsingApiUrl,
                            apiKey = settings.smartParsingApiKey,
                            model = settings.smartParsingModel,
                        ),
                    )
                }
                onSuccess(message)
            } catch (error: Throwable) {
                onError(error.message ?: "智能解析连接测试失败，请检查配置。")
            }
        }
    }

    fun addFoodsFromPhotos(
        photoUris: List<String>,
        onAdded: () -> Unit,
    ) {
        val distinctPhotoUris = photoUris.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctPhotoUris.isEmpty()) {
            onAdded()
            return
        }

        viewModelScope.launch {
            distinctPhotoUris.forEachIndexed { index, photoUri ->
                repository.addFoodItem(
                    FoodItemInput(
                        name = "$BatchPhotoPendingNamePrefix ${index + 1}",
                        expiryDate = LocalDate.now().plusDays(3),
                        categoryTag = "",
                        note = BatchPhotoPendingNote,
                        photoUri = photoUri,
                    ),
                )
            }
            onAdded()
        }
    }

    fun saveFoodItems(
        inputs: List<FoodItemInput>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            inputs.forEach { input ->
                input.categoryTag.trim().takeIf { it.isNotBlank() }?.let(settings::addCategoryTag)
                repository.addFoodItem(input)
            }
            onSaved()
        }
    }

    class Factory(
        private val repository: FoodRepository,
        private val settings: AppSettings,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
                return FoodViewModel(
                    repository = repository,
                    settings = settings,
                    appContext = appContext,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

}
