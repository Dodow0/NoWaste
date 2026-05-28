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
    const val BatchPhotoCapture = "photos/batch"
    const val BatchSmartParse = "smart/batch"
    const val TextPicker = "text-picker"
    const val Settings = "settings"

    fun editFood(itemId: Long): String = "foods/$itemId"
}

@Composable
fun NoWasteApp(viewModel: FoodViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.foodListUiState.collectAsStateWithLifecycle()

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
                onBatchSmartAddClick = { navController.navigate(Routes.BatchSmartParse) },
                onBatchPhotoAddClick = { navController.navigate(Routes.BatchPhotoCapture) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onFoodClick = { item -> navController.navigate(Routes.editFood(item.id)) },
                onDeleteFood = { item -> viewModel.deleteFoodItem(item.id) {} },
            )
        }
        composable(Routes.AddFood) { entry ->
            val settings = (uiState as? FoodListUiState.Ready)?.settings
            FoodFormScreen(
                item = null,
                onNavigateBack = { navController.popBackStack() },
                onPickNameFromPhoto = { navController.navigate(Routes.TextPicker) },
                categoryTags = settings?.categoryTags.orEmpty(),
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
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            FoodEditRoute(
                itemId = itemId,
                uiState = uiState,
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
                onPickNameFromPhoto = { navController.navigate(Routes.TextPicker) },
                navBackStackEntry = backStackEntry,
            )
        }
        composable(Routes.BatchSmartParse) {
            val currentState = uiState
            if (currentState is FoodListUiState.Ready) {
                BatchSmartParseScreen(
                    smartParsingEnabled = currentState.settings.smartParsingEnabled,
                    categoryTags = currentState.settings.categoryTags,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onParseText = viewModel::parseSmartFoodBatchText,
                    onSaveFoods = { inputs ->
                        viewModel.saveFoodItems(
                            inputs = inputs,
                            onSaved = {
                                navController.popBackStack(
                                    route = Routes.FoodList,
                                    inclusive = false,
                                )
                            },
                        )
                    },
                )
            } else {
                LoadingScreen()
            }
        }
        composable(Routes.BatchPhotoCapture) {
            BatchPhotoCaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinished = { photoUris ->
                    viewModel.addFoodsFromPhotos(
                        photoUris = photoUris,
                        onAdded = {
                            navController.popBackStack(
                                route = Routes.FoodList,
                                inclusive = false,
                            )
                        },
                    )
                },
            )
        }
        composable(Routes.TextPicker) {
            TextPickerCameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onTextSelected = { text ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedFoodName", text)
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
                    onAddCategoryTag = viewModel::addCategoryTag,
                    onDeleteCategoryTag = viewModel::deleteCategoryTag,
                    onMoveCategoryTag = viewModel::moveCategoryTag,
                    onSmartParsingEnabledChange = viewModel::updateSmartParsingEnabled,
                    onSmartParsingApiUrlChange = viewModel::updateSmartParsingApiUrl,
                    onSmartParsingApiKeyChange = viewModel::updateSmartParsingApiKey,
                    onSmartParsingModelChange = viewModel::updateSmartParsingModel,
                    onTestSmartParsing = viewModel::testSmartParsing,
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
    onNavigateBack: () -> Unit,
    onSave: (FoodItemInput) -> Unit,
    onDelete: () -> Unit,
    onPickNameFromPhoto: () -> Unit,
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
                    onPickNameFromPhoto = onPickNameFromPhoto,
                    categoryTags = uiState.settings.categoryTags,
                    onSave = onSave,
                    onDelete = onDelete,
                    navBackStackEntry = navBackStackEntry,
                )
            }
        }
    }
}
