package dev.imhamid.messages.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.imhamid.messages.app
import dev.imhamid.messages.sms.SmsSender

object Routes {
    const val LIST = "list"
    const val CHAT = "chat/{addr}"
    const val NEW = "new"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val BLOCKED = "blocked"
    const val SCHEDULED = "scheduled"
    const val SEARCH = "search"

    /** Addresses can contain any character, so they travel through the route hex-encoded. */
    fun chat(address: String): String =
        "chat/" + address.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

    fun decode(hex: String): String {
        if (hex.length % 2 != 0) return ""
        val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return String(bytes, Charsets.UTF_8)
    }
}

@Composable
fun AppRoot(launchAddress: String?, onLaunchConsumed: () -> Unit) {
    val ctx = LocalContext.current
    val vm: MainViewModel = viewModel()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val smsGranted = remember(tick) { vm.repo.canReadSms() && SmsSender.hasPermission(ctx) }
    val isDefault = remember(tick) { vm.repo.isDefaultSms() }
    val showOnboarding = !smsGranted || (!isDefault && !ctx.app.settings.defaultPromptDismissed)

    LaunchedEffect(smsGranted) {
        if (smsGranted) {
            vm.runImport()
            vm.repo.purgeExpiredTrash()
        }
    }

    AnimatedContent(
        targetState = showOnboarding,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "root"
    ) { onboarding ->
        if (onboarding) {
            Onboarding(
                smsGranted = smsGranted,
                onChanged = { tick++ },
                onSkipDefault = {
                    ctx.app.settings.defaultPromptDismissed = true
                    tick++
                }
            )
        } else {
            AppNavHost(vm, launchAddress, onLaunchConsumed)
        }
    }
}

@Composable
private fun AppNavHost(vm: MainViewModel, launchAddress: String?, onLaunchConsumed: () -> Unit) {
    val nav = rememberNavController()
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val spec = tween<androidx.compose.ui.unit.IntOffset>(320, easing = FastOutSlowInEasing)
    val fade = tween<Float>(220)

    LaunchedEffect(launchAddress) {
        if (!launchAddress.isNullOrBlank()) {
            nav.navigate(Routes.chat(launchAddress)) { launchSingleTop = true }
            onLaunchConsumed()
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.LIST,
        enterTransition = { slideInHorizontally(spec) { sign * it / 4 } + fadeIn(fade) },
        exitTransition = { slideOutHorizontally(spec) { -sign * it / 8 } + fadeOut(fade) },
        popEnterTransition = { slideInHorizontally(spec) { -sign * it / 8 } + fadeIn(fade) },
        popExitTransition = { slideOutHorizontally(spec) { sign * it / 4 } + fadeOut(fade) }
    ) {
        composable(Routes.LIST) {
            ChatListScreen(
                vm,
                onOpen = { nav.navigate(Routes.chat(it)) },
                onNew = { nav.navigate(Routes.NEW) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onTrash = { nav.navigate(Routes.TRASH) },
                onBlocked = { nav.navigate(Routes.BLOCKED) },
                onScheduled = { nav.navigate(Routes.SCHEDULED) },
                onSearch = { nav.navigate(Routes.SEARCH) }
            )
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("addr") { type = NavType.StringType })) { e ->
            val addr = Routes.decode(e.arguments?.getString("addr").orEmpty())
            ChatScreen(vm, addr, onBack = { nav.popBackStack() })
        }
        composable(Routes.NEW) {
            NewChatScreen(
                vm,
                onBack = { nav.popBackStack() },
                onOpen = { addr ->
                    nav.navigate(Routes.chat(addr)) { popUpTo(Routes.NEW) { inclusive = true } }
                }
            )
        }
        composable(Routes.SETTINGS) { SettingsScreen(vm) { nav.popBackStack() } }
        composable(Routes.TRASH) { TrashScreen(vm) { nav.popBackStack() } }
        composable(Routes.BLOCKED) { BlockedScreen(vm) { nav.popBackStack() } }
        composable(Routes.SCHEDULED) { ScheduledScreen(vm) { nav.popBackStack() } }
        composable(Routes.SEARCH) {
            SearchScreen(vm, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.chat(it)) })
        }
    }
}
