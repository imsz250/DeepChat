package com.deepseekchat.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepseekchat.app.ui.AppIcons
import com.deepseekchat.app.ui.BalanceScreen
import com.deepseekchat.app.ui.ChatScreen
import com.deepseekchat.app.ui.ChatViewModel
import com.deepseekchat.app.ui.L10n
import com.deepseekchat.app.ui.HistoryScreen
import com.deepseekchat.app.ui.SettingsScreen
import com.deepseekchat.app.ui.theme.DeepChatTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
            val seedKey by viewModel.themeSeed.collectAsStateWithLifecycle()
            DeepChatTheme(themeMode = themeMode, dynamicColor = dynamicColor, seedKey = seedKey) {
                MainScreen(viewModel)
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ChatViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val language by vm.language.collectAsStateWithLifecycle()
    val view = LocalView.current
    val darkBackground = MaterialTheme.colorScheme.surface.luminance() < 0.5
    val barColor = MaterialTheme.colorScheme.surface.toArgb()
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = barColor
            window.navigationBarColor = barColor
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !darkBackground
                isAppearanceLightNavigationBars = !darkBackground
            }
        }
    }
    val tabs = remember(language) {
        listOf(
            TabItem(L10n.chat(language), AppIcons.Chat),
            TabItem(L10n.history(language), Icons.Filled.List),
            TabItem(L10n.balance(language), AppIcons.Wallet),
            TabItem(L10n.settings(language), Icons.Filled.Settings)
        )
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val chatPage = 0

    // 键盘弹起时把底部导航收起来，别跟输入框抢地方
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val keyboardOpen by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    // 切页面就把当前对话存下来
    LaunchedEffect(pagerState.currentPage) {
        vm.saveNow()
        if (pagerState.currentPage != 0) keyboard?.hide()
    }
    // 退到后台、杀掉 App 也存
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.saveNow()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = tabs[pagerState.currentPage].label,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (keyboardOpen) {
                // 键盘开着：底部不放导航栏，全部空间给输入框
            } else NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> ChatScreen(vm)
                1 -> HistoryScreen(
                    vm = vm,
                    onOpen = { scope.launch { pagerState.animateScrollToPage(chatPage) } }
                )
                2 -> BalanceScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }

    Box(Modifier.fillMaxSize())
}
