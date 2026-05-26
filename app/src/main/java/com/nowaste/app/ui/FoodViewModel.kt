package com.nowaste.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nowaste.app.data.FoodItem
import com.nowaste.app.data.FoodRepository
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.network.ProductLookupResult
import com.nowaste.app.network.ProductLookupService
import com.nowaste.app.notifications.ReminderScheduler
import com.nowaste.app.settings.AppSettings
import com.nowaste.app.settings.SettingsState
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FoodListUiState {
    data object Loading : FoodListUiState
    data class Ready(
        val items: List<FoodItem>,
        val settings: SettingsState,
    ) : FoodListUiState
}

sealed interface ProductLookupUiState {
    data object Idle : ProductLookupUiState
    data object Loading : ProductLookupUiState
    data class Success(val result: ProductLookupResult?) : ProductLookupUiState
    data object Failed : ProductLookupUiState
}

class FoodViewModel(
    private val repository: FoodRepository,
    private val settings: AppSettings,
    private val productLookupService: ProductLookupService,
    private val appContext: Context,
) : ViewModel() {
    private val productLookupState = MutableStateFlow<ProductLookupUiState>(ProductLookupUiState.Idle)
    val productLookupUiState: StateFlow<ProductLookupUiState> = productLookupState.asStateFlow()

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

    fun lookupProduct(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            productLookupState.value = ProductLookupUiState.Loading
            productLookupState.value = try {
                ProductLookupUiState.Success(productLookupService.lookup(barcode))
            } catch (_: Exception) {
                ProductLookupUiState.Failed
            }
        }
    }

    fun clearProductLookupState() {
        productLookupState.value = ProductLookupUiState.Idle
    }

    class Factory(
        private val repository: FoodRepository,
        private val settings: AppSettings,
        private val productLookupService: ProductLookupService,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
                return FoodViewModel(
                    repository = repository,
                    settings = settings,
                    productLookupService = productLookupService,
                    appContext = appContext,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
