package com.cairn.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.cairn.app.ui.screens.contactdetail.ContactDetailScreen
import com.cairn.app.ui.screens.contacts.ContactsScreen
import com.cairn.app.ui.screens.dashboard.DashboardScreen
import com.cairn.app.ui.screens.home.HomeScreen
import com.cairn.app.ui.screens.search.SearchScreen
import com.cairn.app.ui.screens.timeline.TimelineScreen
import com.cairn.app.ui.screens.calldetail.CallDetailScreen
import com.cairn.app.ui.screens.favorites.FavoritesScreen
import com.cairn.app.ui.screens.backup.BackupScreen
import com.cairn.app.ui.screens.security.SecurityScreen
import com.cairn.app.ui.screens.appearance.AppearanceScreen
import com.cairn.app.ui.screens.dbhealth.DatabaseHealthScreen
import com.cairn.app.ui.screens.tags.TagsScreen
import com.cairn.app.ui.screens.archive.ArchiveExplorerScreen
import com.cairn.app.ui.screens.onboarding.OnboardingScreen
import com.cairn.app.ui.screens.permissions.PermissionsScreen
import com.cairn.app.ui.screens.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val SEARCH = "search"
    const val CONTACTS = "contacts"
    const val CONTACT_DETAIL = "contact/{contactId}"
    const val TIMELINE = "timeline"
    const val CALL_DETAIL = "call/{callId}"
    const val DASHBOARD = "dashboard"
    const val FAVORITES = "favorites"
    const val TAGS = "tags"
    const val ARCHIVE = "archive"
    const val BACKUP = "backup"
    const val SECURITY = "security"
    const val APPEARANCE = "appearance"
    const val DB_HEALTH = "db_health"

    fun contactDetail(id: Long) = "contact/$id"
    fun callDetail(id: Long) = "call/$id"
}

@Composable
fun CairnNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) { SplashScreen(onFinished = {
            navController.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } }
        }) }

        composable(Routes.ONBOARDING) { OnboardingScreen(onDone = { navController.navigate(Routes.PERMISSIONS) }) }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onGranted = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenTimeline = { navController.navigate(Routes.TIMELINE) },
                onOpenDashboard = { navController.navigate(Routes.DASHBOARD) },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onOpenCall = { id -> navController.navigate(Routes.callDetail(id)) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenCall = { id -> navController.navigate(Routes.callDetail(id)) },
                onOpenContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONTACTS) {
            ContactsScreen(
                onOpenContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.CONTACT_DETAIL,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
            ContactDetailScreen(
                contactId = contactId,
                onOpenCall = { id -> navController.navigate(Routes.callDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TIMELINE) {
            TimelineScreen(
                onOpenCall = { id -> navController.navigate(Routes.callDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.CALL_DETAIL,
            arguments = listOf(navArgument("callId") { type = NavType.LongType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getLong("callId") ?: 0L
            CallDetailScreen(callId = callId, onBack = { navController.popBackStack() })
        }

        composable(Routes.DASHBOARD) { DashboardScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onOpenContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.TAGS) { TagsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ARCHIVE) { ArchiveExplorerScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.BACKUP) { BackupScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SECURITY) { SecurityScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.APPEARANCE) { AppearanceScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DB_HEALTH) { DatabaseHealthScreen(onBack = { navController.popBackStack() }) }
    }
}
