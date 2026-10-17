package com.lemiepiante.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lemiepiante.ui.detail.PlantDetailScreen
import com.lemiepiante.ui.edit.PlantEditScreen
import com.lemiepiante.ui.list.PlantListScreen

private object Routes {
    const val LIST = "list"
    const val DETAIL = "plant/{plantId}"
    const val EDIT = "edit?plantId={plantId}"

    fun detail(id: Long) = "plant/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?plantId=$id"
}

/**
 * @param openPlantId pianta da aprire (es. dopo il tocco su una notifica).
 * @param onPlantOpened chiamata dopo la navigazione, per non riaprirla di nuovo.
 */
@Composable
fun AppNavigation(openPlantId: Long?, onPlantOpened: () -> Unit) {
    val navController = rememberNavController()

    LaunchedEffect(openPlantId) {
        if (openPlantId != null) {
            navController.navigate(Routes.detail(openPlantId)) {
                popUpTo(Routes.LIST)
                launchSingleTop = true
            }
            onPlantOpened()
        }
    }

    NavHost(navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            PlantListScreen(
                onPlantClick = { navController.navigate(Routes.detail(it)) },
                onAddPlant = { navController.navigate(Routes.edit()) },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("plantId") { type = NavType.LongType })) {
            PlantDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(it)) },
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("plantId") { type = NavType.LongType; defaultValue = -1L }),
        ) {
            PlantEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
