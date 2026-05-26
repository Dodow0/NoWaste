package com.nowaste.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nowaste.app.domain.FoodItemInput

private object Routes {
    const val FoodList = "foods"
    const val AddFood = "foods/new"
    const val EditFood = "foods/{itemId}"
    const val BarcodeScanner = "scanner/barcode"
    const val Settings = "settings"

    fun editFood(itemId: Long): String = "foods/$itemId"
}

@Composable
fun NoWasteApp(viewModel: FoodViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.foodListUiState.collectAsStateWithLifecycle()
    val productLookupUiState by viewModel.productLookupUiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.FoodList,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 220),
            ) + fadeIn(animationSpec = tween(durationMillis = 120))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 220),
            ) + fadeOut(animationSpec = tween(durationMillis = 90))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 220),
            ) + fadeIn(animationSpec = tween(durationMillis = 120))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 220),
            ) + fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) {
        composable(Routes.FoodList) {
            FoodListScreen(
                uiState = uiState,
                onAddClick = { navController.navigate(Routes.AddFood) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onFoodClick = { item -> navController.navigate(Routes.editFood(item.id)) },
                onDeleteFood = { item -> viewModel.deleteFoodItem(item.id) {} },
                onConsumeFood = { item -> viewModel.deleteFoodItem(item.id) {} },
                onAddCategoryTag = viewModel::addCategoryTag,
                onDeleteCategoryTag = viewModel::deleteCategoryTag,
            )
        }
        composable(Routes.AddFood) { entry ->
            val categoryTags = (uiState as? FoodListUiState.Ready)?.settings?.categoryTags.orEmpty()
            FoodFormScreen(
                item = null,
                onNavigateBack = { navController.popBackStack() },
                onScanBarcode = { navController.navigate(Routes.BarcodeScanner) },
                categoryTags = categoryTags,
                productLookupUiState = productLookupUiState,
                onLookupProduct = viewModel::lookupProduct,
                onClearProductLookupState = viewModel::clearProductLookupState,
                onSave = { input ->
                    viewModel.saveFoodItem(
                        id = null,
                        input = input,
                        onSaved = { navController.popBackStack() },
                    )
                },
                onDelete = null,
                navBackStackEntry = entry,
            )
        }
        composable(
            route = Routes.EditFood,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { entry ->
            val itemId = entry.arguments?.getLong("itemId") ?: return@composable
            FoodEditRoute(
                itemId = itemId,
                uiState = uiState,
                productLookupUiState = productLookupUiState,
                onNavigateBack = { navController.popBackStack() },
                onSave = { input ->
                    viewModel.saveFoodItem(
                        id = itemId,
                        input = input,
                        onSaved = { navController.popBackStack() },
                    )
                },
                onDelete = {
                    viewModel.deleteFoodItem(
                        id = itemId,
                        onDeleted = { navController.popBackStack() },
                    )
                },
                onScanBarcode = { navController.navigate(Routes.BarcodeScanner) },
                onLookupProduct = viewModel::lookupProduct,
                onClearProductLookupState = viewModel::clearProductLookupState,
                navBackStackEntry = entry,
            )
        }
        composable(Routes.BarcodeScanner) {
            BarcodeScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onBarcodeDetected = { barcode ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scannedBarcode", barcode)
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.Settings) {
            val currentState = uiState
            if (currentState is FoodListUiState.Ready) {
                SettingsScreen(
                    settings = currentState.settings,
                    onNavigateBack = { navController.popBackStack() },
                    onReminderTimeChange = viewModel::updateReminderTime,
                    onNearExpiryDaysChange = viewModel::updateNearExpiryDays,
                )
            } else {
                LoadingScreen()
            }
        }
    }
}

@Composable
private fun FoodEditRoute(
    itemId: Long,
    uiState: FoodListUiState,
    productLookupUiState: ProductLookupUiState,
    onNavigateBack: () -> Unit,
    onSave: (FoodItemInput) -> Unit,
    onDelete: () -> Unit,
    onScanBarcode: () -> Unit,
    onLookupProduct: (String) -> Unit,
    onClearProductLookupState: () -> Unit,
    navBackStackEntry: NavBackStackEntry,
) {
    when (uiState) {
        FoodListUiState.Loading -> LoadingScreen()
        is FoodListUiState.Ready -> {
            val item = uiState.items.firstOrNull { it.id == itemId }
            if (item == null) {
                MissingFoodScreen(onNavigateBack = onNavigateBack)
            } else {
                FoodFormScreen(
                    item = item,
                    onNavigateBack = onNavigateBack,
                    onScanBarcode = onScanBarcode,
                    categoryTags = uiState.settings.categoryTags,
                    productLookupUiState = productLookupUiState,
                    onLookupProduct = onLookupProduct,
                    onClearProductLookupState = onClearProductLookupState,
                    onSave = onSave,
                    onDelete = onDelete,
                    navBackStackEntry = navBackStackEntry,
                )
            }
        }
    }
}
