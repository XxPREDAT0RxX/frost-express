package com.chetna.frostexpress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import kotlin.math.pow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Palette: snowy exterior (matches the scene) + warm wooden interior (matches the train floor).
// Cozy palette: cream panels, soft pastels and a warm brown outline around everything.
internal val Snow = Color(0xFFEEF5FB)
internal val Ink = Color(0xFF6B4A3A)       // warm brown: text and outlines
internal val Panel = Color(0xFFFFF6E9)     // cream
internal val PanelHi = Color(0xFFF3E3CC)
internal val Track = Color(0xFFEFE3D3)     // empty part of progress bars
internal val Wood = Color(0xFF6B4A3A)      // outline colour for cards and sheets
internal val Ice = Color(0xFF5E9CCF)
internal val Ember = Color(0xFFF08A5D)
internal val Gold = Color(0xFFFFC85C)
internal val GoldText = Color(0xFFC98A1E)
internal val Mint = Color(0xFF3F9E74)
internal val Muted = Color(0xFF9A7A64)
internal val Glass = Color(0xFFFFF6E9).copy(alpha = 0.97f)
internal val OutlineW = 2.5.dp
/** The game's display font (Poppins): titles, tutorial, pop-up messages. */
internal val CozyFont = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.poppins_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.poppins_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.poppins_bold, FontWeight.Bold),
    androidx.compose.ui.text.font.Font(R.font.poppins_bold, FontWeight.ExtraBold),
)

private fun upgradeColor(t: UpgradeType): Color = when (t) {
    UpgradeType.ENGINE -> Color(0xFFE8890C)
    UpgradeType.WAGON -> Color(0xFF2F8FE0)
    UpgradeType.HEAT -> Color(0xFFE5484D)
    UpgradeType.FOOD -> Color(0xFF3FAE5A)
    UpgradeType.PLOW -> Color(0xFF5BA7D9)
    UpgradeType.COAL -> Color(0xFF6B7580)
    UpgradeType.BOOSTER -> Color(0xFFB24DD6)
    UpgradeType.CONDUCTOR -> Color(0xFF5C7CFA)
    else -> gadgetColor(UPGRADES.firstOrNull { it.type == t }?.gadget ?: Gadget.GEAR)
}

@Composable
fun FrostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ice,
            onPrimary = Color.White,
            background = Snow,
            surface = Panel,
            onSurface = Ink,
            onBackground = Ink,
            secondary = Muted,
        ),
        content = { CompositionLocalProvider(LocalContentColor provides Ink, androidx.compose.foundation.LocalIndication provides CozyIndication) { content() } },
    )
}

/** Dialog that matches the game: cream card with a wooden border. */
@Composable
private fun FrostAlert(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        modifier = Modifier.border(OutlineW, Wood, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        containerColor = Panel,
        titleContentColor = Ink,
        textContentColor = Ink,
        tonalElevation = 0.dp,
    )
}

@Composable
fun GameScreen(vm: GameViewModel, initialSheet: Int = 0, initialDetail: UpgradeType? = null) {
    // 0 = no sheet, 1 = upgrades, 2 = shop. Both slide up over the bottom of the running scene.
    var sheet by remember { mutableIntStateOf(initialSheet) }
    val shownSheet = remember { intArrayOf(1) }
    if (sheet != 0) shownSheet[0] = sheet
    val showUpgrades = sheet == 1
    var showSettings by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf(initialDetail) }
    var sheetTab by remember { mutableIntStateOf(0) }
    val link = remember { SceneLink() }
    // the tutorial's last card appears right after the last step is done
    var lastStage by remember { mutableIntStateOf(vm.tutorialStage) }
    var showTutorialDone by remember { mutableStateOf(false) }
    if (vm.tutorialStage != lastStage) {
        if (lastStage == 3 && vm.tutorialStage == 4) showTutorialDone = true
        lastStage = vm.tutorialStage
    }
    var showGift by remember { mutableStateOf(false) }
    var showWheel by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    // a clean view: header, rails and footer slide away so only the train is left
    var uiHidden by remember { mutableStateOf(false) }
    // looking out the window: a side-on parallax view of the land rolling past
    var windowOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = sheet != 0) { sheet = 0 }
    // back on the main screen: ask before leaving the game
    BackHandler(enabled = sheet == 0) { showQuit = true }
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.52f).dp
    val sheetPx = with(LocalDensity.current) { sheetHeight.roundToPx() }
    // One progress value drives the sheet height, so opening and closing never jump.
    val sheetP by animateFloatAsState(if (sheet != 0) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "sheet")
    val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(Snow)
    ) {
        // The scene stays full-height; the sheet slides up over it so the train never resizes or jumps.
        TrainScene(vm, Modifier.fillMaxSize(), onDeviceTap = { type ->
                vm.tutorialAdvance(1)
                detail = type
            }, link = link)
            // "look out the window": a full-screen side view that rolls past with the train's speed
            if (windowOpen) WindowView(vm, Modifier.align(Alignment.Center))
            if (!uiHidden) TopHud(
                vm,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                onOpenSettings = { showSettings = true },
                onOpenGems = { sheet = 3 },
                onOpenLevel = { sheet = if (sheet == 9) 0 else 9 },
                onOpenFinance = { sheet = if (sheet == 10) 0 else 10 },
                onGoal = { sheet = it },
            )
            // side rails: they use the snowy space left and right of the train
            if (!uiHidden) LeftRail(
                vm,
                onQuests = { sheet = if (sheet == 5) 0 else 5 },
                onAwards = { sheet = if (sheet == 7) 0 else 7 },
                onWheel = { showWheel = true },
                onGift = { showGift = true },
                controls = {
                    SceneControls(
                        horizontal = LocalConfiguration.current.screenHeightDp < 700,
                        hidden = false,
                        zoom = link.zoom,
                        onToggleHide = { uiHidden = true; sheet = 0 },
                        onZoom = { link.zoom = (link.zoom * it).coerceIn(ZOOM_MIN, ZOOM_MAX) },
                        windowOpen = windowOpen,
                        onWindow = { windowOpen = !windowOpen },
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 142.dp, start = 8.dp),
            )
            AchievementBanner(
                vm.achievementPopup,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 188.dp),
            )
            ToastBanner(
                vm.toast,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 140.dp),
            )
            if (!uiHidden) Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 142.dp, end = 8.dp, bottom = FOOTER_H + 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RightRail(vm, link, onEvolve = { sheet = if (sheet == 6) 0 else 6 }, onJourney = { sheet = if (sheet == 8) 0 else 8 })
            }
            if (uiHidden) SceneControls(
                hidden = true,
                zoom = link.zoom,
                onToggleHide = { uiHidden = false },
                onZoom = { link.zoom = (link.zoom * it).coerceIn(ZOOM_MIN, ZOOM_MAX) },
                windowOpen = windowOpen,
                onWindow = { windowOpen = !windowOpen },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 16.dp + bottomInset),
            )
            if (vm.tutorialStage in 1..3) {
                TutorialGuide(
                    vm, sheetOpen = showUpgrades,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 84.dp + bottomInset * (1f - sheetP)),
                )
            }
            LevelUpPopup(vm, Modifier.align(Alignment.Center))
            FloatingPops(vm, Modifier.align(Alignment.Center))
            MinigamePrompt(
                vm,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp + bottomInset * (1f - sheetP)),
            )
            NewTrainPopup(vm)
            RouteChoiceCard(vm)
            BoosterPopup(vm, Modifier.align(Alignment.Center))
            if (!uiHidden) BottomBar(
                vm,
                sheetOpen = showUpgrades,
                onToggleUpgrades = {
                    if (!showUpgrades) vm.tutorialAdvance(1)
                    sheet = if (sheet == 1) 0 else 1
                },
                openSheet = sheet,
                onToggleShop = { sheet = if (sheet == 2) 0 else 2 },
                onToggleGems = { sheet = if (sheet == 3) 0 else 3 },
                onToggleBag = { sheet = if (sheet == 4) 0 else 4 },
                onToggleFinance = { sheet = if (sheet == 10) 0 else 10 },
                pulseGear = vm.tutorialStage == 1 || (vm.tutorialStage == 2 && !showUpgrades),
                pulseBoost = vm.tutorialStage == 3,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(top = 6.dp, bottom = 6.dp + bottomInset * (1f - sheetP) + sheetHeight * sheetP),
            )
        if (sheetP > 0.001f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .graphicsLayer { translationY = sheetPx * (1f - sheetP) }
                    .clipToBounds(),
            ) {
                if (shownSheet[0] == 2) {
                    ShopSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onGetGems = { sheet = 3 })
                } else if (shownSheet[0] == 3) {
                    GemShopSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onBack = { sheet = 2 })
                } else if (shownSheet[0] == 4) {
                    InventorySheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onShop = { sheet = 2 })
                } else if (shownSheet[0] == 5) {
                    QuestSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else if (shownSheet[0] == 6) {
                    EvolveSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else if (shownSheet[0] == 7) {
                    EventsSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else if (shownSheet[0] == 8) {
                    JourneySheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else if (shownSheet[0] == 9) {
                    ProfileSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else if (shownSheet[0] == 10) {
                    FinanceSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else {
                    UpgradesSheet(
                        vm, Modifier.height(sheetHeight),
                        focus = if (vm.tutorialStage == 2) UpgradeType.ENGINE else null, onFocusDone = {},
                        startTab = sheetTab, onTabShown = { sheetTab = 0 },
                        onOpenDetail = { detail = it },
                        onOpenEvolve = { sheet = 6 },
                        onClose = { sheet = 0 },
                    )
                }
            }
        }
    }

    detail?.let { UpgradeDialog(vm, it, onClose = { detail = null }) }

    // Pop-ups in order: tutorial welcome, daily reward, welcome back. The rest of the tutorial is shown as hints.
    if (vm.tutorialStage == 0) {
        TutorialCard(
            title = "Welcome aboard!",
            step = 0,
            text = "The world outside has frozen, but your little train keeps rolling. It earns money from the passengers it carries, even while the app is closed. Keep them happy, watch your running costs and grow your railway.\n\nLet me show you the ropes. It only takes a minute!",
            button = "Let's go!",
            onButton = { vm.tutorialAdvance(0) },
            onSkip = vm::finishTutorial,
        )
    } else if (vm.tutorialDone && vm.dailyReady) {
        DailyGiftCard(vm, onClose = if (showGift) ({ showGift = false }) else null)
    } else if (showGift) {
        DailyGiftCard(vm, onClose = { showGift = false })
    } else if (vm.tutorialDone && vm.offlineCoins > 0.0) {
        WelcomeBackCard(vm)
    }

    vm.cloudConflict?.let { cloudJson ->
        FrostAlert(
            onDismissRequest = {},
            title = { Text("Cloud save found") },
            text = {
                Text("Cloud: ${vm.describeCloud(cloudJson)}\nThis device: ${vm.describeLocal()}\n\nWhich progress do you want to keep?")
            },
            confirmButton = { TextButton(onClick = { UiSound.click(); vm.useCloudSave() }) { Text("Use cloud save") } },
            dismissButton = { TextButton(onClick = { UiSound.click(); vm.keepLocalSave() }) { Text("Keep this device") } },
        )
    }

    if (showSettings) SettingsDialog(vm, onClose = { showSettings = false })
    if (showQuit) QuitCard(vm, onStay = { showQuit = false })
    if (showWheel) WheelDialog(vm, onClose = { showWheel = false })
    val feat = vm.pendingFeature
    if (feat != null && vm.tutorialDone && !vm.dailyReady && vm.offlineCoins <= 0.0 && !vm.newTrainPending && vm.routeChoices.isEmpty() && !showGift) {
        FeatureIntroCard(vm, feat) { f ->
            when (f) {
                Feature.WHEEL -> showWheel = true
                Feature.EVENTS -> sheet = 7
                Feature.JOURNEY -> sheet = 8
            }
        }
    }
    MetaPopups(vm)
    MinigameDialog(vm)
    if (vm.tutorialDone && vm.simTour < SIM_TOUR_PAGES && !showTutorialDone && !vm.dailyReady && vm.offlineCoins <= 0.0 && !showGift && vm.cloudConflict == null) {
        val page = vm.simTour
        val (title, text) = when (page) {
            0 -> "Passengers pay the fare" to "At every station people get on and off. Each rider on board pays a fare every second, so full seats mean more money. Longer trips pay a better fare."
            1 -> "Happy riders pay more" to "Heating, food, lounge and blankets keep riders comfy. Happy riders and a high star rating bring bigger crowds and better fares."
            2 -> "Running costs" to "Wages, fuel and upkeep cost coins every second. The green chip at the top is your net income: what is left after the costs. Tap it, or the green chart button in the footer, to open Finances."
            3 -> "Manage your railway" to "In Finances you set the ticket price, service the train before it breaks down, hire attendants and mechanics, run ad campaigns and take a bank loan."
            else -> "Aim for a profit" to "Keep the net income green. Evolve to grow: bigger trains carry more riders and have a brand new interior."
        }
        TutorialCard(
            title = title,
            step = page,
            text = text,
            button = if (page == SIM_TOUR_PAGES - 1) "Open Finances" else "Next",
            onButton = { vm.simTourNext(); if (page == SIM_TOUR_PAGES - 1) sheet = 10 },
            onSkip = vm::simTourSkip,
        )
    }
    if (showTutorialDone) {
        TutorialCard(
            title = "You're all set!",
            step = 4,
            text = "Tap any device on the train to see it up close and upgrade it.\n\nBuy enough upgrades to evolve into a bigger train with more cars.\n\nDaily quests, awards and your gift are on the left. Shop, gems and your bag are in the footer.",
            button = "Start my journey",
            onButton = { showTutorialDone = false },
            onSkip = null,
        )
    }
}

@Composable
internal fun CoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE066), Color(0xFFF59F00))), radius = r)
        drawCircle(Color(0xFFB87400), radius = r * 0.66f, style = Stroke(width = r * 0.14f))
        drawLine(Color(0xFFB87400), Offset(size.width * 0.5f, size.height * 0.33f), Offset(size.width * 0.5f, size.height * 0.67f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
    }
}

@Composable
private fun GearIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(color, radius = r * 0.62f, center = c, style = Stroke(width = r * 0.3f))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color,
                Offset(c.x + dx * r * 0.7f, c.y + dy * r * 0.7f),
                Offset(c.x + dx * r * 0.98f, c.y + dy * r * 0.98f),
                strokeWidth = r * 0.3f,
            )
        }
    }
}

@Composable
private fun BoltIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawBolt(color) }
}

private fun DrawScope.drawBolt(color: Color) {
    val s = size.minDimension
    val p = Path().apply {
        moveTo(.58f * s, .05f * s); lineTo(.2f * s, .55f * s); lineTo(.47f * s, .55f * s)
        lineTo(.38f * s, .95f * s); lineTo(.8f * s, .4f * s); lineTo(.52f * s, .4f * s); close()
    }
    drawPath(p, color)
}

@Composable
private fun UpgradeIcon(type: UpgradeType, color: Color, modifier: Modifier = Modifier) {
    val sprites = rememberGameSprites()
    Canvas(modifier) {
        val s = size.minDimension
        sprites.device[type]?.let { img ->
            drawSprite(img, -s * 0.2f, -s * 0.2f, s * 1.4f, s * 1.4f)
            return@Canvas
        }
        when (type) {
            UpgradeType.ENGINE -> drawBolt(color)
            UpgradeType.WAGON -> {
                drawRoundRect(color, Offset(.08f * s, .22f * s), Size(.84f * s, .46f * s), CornerRadius(.08f * s))
                drawCircle(color, .1f * s, Offset(.3f * s, .82f * s))
                drawCircle(color, .1f * s, Offset(.7f * s, .82f * s))
            }
            UpgradeType.HEAT -> {
                val p = Path().apply {
                    moveTo(.5f * s, .04f * s)
                    cubicTo(.95f * s, .42f * s, .88f * s, .96f * s, .5f * s, .96f * s)
                    cubicTo(.12f * s, .96f * s, .05f * s, .5f * s, .5f * s, .04f * s)
                }
                drawPath(p, color)
                drawCircle(Color.White.copy(alpha = .75f), .13f * s, Offset(.5f * s, .72f * s))
            }
            UpgradeType.FOOD -> {
                val p = Path().apply {
                    moveTo(.08f * s, .48f * s); lineTo(.92f * s, .48f * s)
                    cubicTo(.92f * s, .9f * s, .08f * s, .9f * s, .08f * s, .48f * s); close()
                }
                drawPath(p, color)
                for (x in listOf(.35f, .65f)) {
                    drawLine(color, Offset(x * s, .12f * s), Offset(x * s, .36f * s), strokeWidth = .08f * s, cap = StrokeCap.Round)
                }
            }
            UpgradeType.PLOW -> {
                val p = Path().apply {
                    moveTo(.08f * s, .88f * s); lineTo(.92f * s, .88f * s); lineTo(.5f * s, .12f * s); close()
                }
                drawPath(p, color)
                drawLine(Color.White.copy(alpha = .6f), Offset(.5f * s, .35f * s), Offset(.5f * s, .72f * s), strokeWidth = .07f * s, cap = StrokeCap.Round)
            }
            UpgradeType.COAL -> {
                drawCircle(color, .22f * s, Offset(.32f * s, .66f * s))
                drawCircle(color, .22f * s, Offset(.68f * s, .66f * s))
                drawCircle(color, .2f * s, Offset(.5f * s, .36f * s))
            }
            UpgradeType.BOOSTER -> {
                for (dy in listOf(0f, .4f)) {
                    val p = Path().apply {
                        moveTo(.2f * s, (.45f + dy) * s); lineTo(.5f * s, (.15f + dy) * s); lineTo(.8f * s, (.45f + dy) * s)
                    }
                    drawPath(p, color, style = Stroke(width = .13f * s, cap = StrokeCap.Round))
                }
            }
            UpgradeType.CONDUCTOR -> {
                drawCircle(color, .38f * s, Offset(.5f * s, .5f * s))
                drawCircle(Panel, .32f * s, Offset(.66f * s, .4f * s))
                drawCircle(color, .05f * s, Offset(.78f * s, .22f * s))
            }
            else -> drawDevice(type, 0f, 0f, s, 0f, 3)
        }
    }
}

// ---------- HUD ----------

@Composable
private fun TopHud(vm: GameViewModel, modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}, onOpenGems: () -> Unit = {}, onOpenLevel: () -> Unit = {}, onOpenFinance: () -> Unit = {}, onGoal: (Int) -> Unit = {}) {
    val goal = vm.goal
    val progress = goal.progress
    val shape = RoundedCornerShape(20.dp)
    // a frosted wooden sign: snow on top, icicles below
    Column(modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF4).copy(alpha = 0.8f), Color(0xFFF6E8D3).copy(alpha = 0.8f))))
                .border(OutlineW, Wood, shape),
        ) {
            SnowCap(Modifier.align(Alignment.TopCenter), seed = 3)
            Column(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        LevelBadge(vm, onOpenLevel)
                        Text(
                            "${vm.playerLevel}", fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp).clip(RoundedCornerShape(6.dp)).background(XpPurple).border(1.2.dp, Ink, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF0D2))
                            .border(1.8.dp, Ink, RoundedCornerShape(14.dp))
                            .padding(start = 5.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CoinIcon(Modifier.size(22.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(fmt(vm.coins), fontSize = if (LocalConfiguration.current.screenWidthDp < 360) 16.sp else 19.sp, fontWeight = FontWeight.ExtraBold, color = GoldText, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    }
                    Spacer(Modifier.width(6.dp))
                    CurrencyPill({ GemIcon(Modifier.size(15.dp)) }, "${vm.gems}", GemPink, onClick = onOpenGems, plus = true)
                    Spacer(Modifier.width(6.dp))
                    SpeakerButton(vm.soundOn, vm::toggleSound)
                    Spacer(Modifier.width(5.dp))
                    MenuButton(onOpenSettings)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
                    StatChip("${fmt(vm.speed, 1)} m/s", Ice)
                    val net = vm.netPerSec
                    StatChip((if (net >= 0) "+" else "-") + fmt(kotlin.math.abs(net), 1) + "/s", if (net >= 0) Mint else Color(0xFFD0524F), onClick = onOpenFinance)
                    PassengerChip(vm, onOpenFinance)
                    val temp = (vm.regionTemp - 7f * (vm.darkness / 0.42f) - 10f * vm.stormLevel).toInt()
                    StatChip("$temp°C", if (temp <= 0) Color(0xFF4C8DB8) else if (temp >= 30) Color(0xFFE08A4C) else Color(0xFF5BA06B))
                }
                // time of day and the evolve progress (label inside the bar)
                Row(verticalAlignment = Alignment.CenterVertically) {
                TimeChip(vm)
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Track)
                        .border(1.5.dp, Ink, RoundedCornerShape(9.dp))
                        .clickable { UiSound.click(); onGoal(goal.sheet) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(Brush.horizontalGradient(if (progress >= 1f) listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)) else listOf(Color(0xFFB8D8F2), Color(0xFFB9A2EE))))
                    )
                    Text(
                        "🎯 " + goal.text,
                        fontSize = 9.5.sp, lineHeight = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, softWrap = false,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                }
            }
        }
        Icicles(Modifier.padding(horizontal = 18.dp))
    }
}

/** Passengers on board and the coin bonus they give (rolled again at every station). */
@Composable
private fun PassengerChip(vm: GameViewModel, onClick: () -> Unit = {}) {
    val c = Color(0xFFB0578A)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable { UiSound.click(); onClick() }.background(c.copy(alpha = 0.13f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(10.dp)) {
            val s = size.minDimension
            drawCircle(c, s * 0.22f, Offset(s * 0.5f, s * 0.24f))
            drawRoundRect(c, Offset(s * 0.18f, s * 0.52f), Size(s * 0.64f, s * 0.48f), CornerRadius(s * 0.3f))
        }
        Spacer(Modifier.width(3.dp))
        Text("${vm.passengers}/${vm.seats}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c, maxLines = 1, softWrap = false)
    }
}

/** Shows the time of day (and a storm warning when a snow storm is active). */
@Composable
private fun TimeChip(vm: GameViewModel) {
    val (phase, secs) = vm.dayPhase()
    val night = phase == "Night" || phase == "Dusk"
    val storm = vm.stormLeft > 0.0
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (storm) Ice.copy(alpha = 0.16f) else if (night) Color(0xFF5C7CFA).copy(alpha = 0.12f) else Gold.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(11.dp)) {
            val s = size.minDimension
            if (night) {
                drawCircle(Color(0xFF5C7CFA), .38f * s, Offset(.5f * s, .5f * s))
                drawCircle(Color.White, .32f * s, Offset(.66f * s, .4f * s))
            } else {
                drawCircle(Gold, .24f * s, Offset(.5f * s, .5f * s))
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    drawLine(Gold, Offset(.5f * s + cos(a).toFloat() * .34f * s, .5f * s + sin(a).toFloat() * .34f * s),
                        Offset(.5f * s + cos(a).toFloat() * .48f * s, .5f * s + sin(a).toFloat() * .48f * s), strokeWidth = .07f * s)
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            if (storm) "Storm ${vm.stormLeft.toInt() + 1}s" else "$phase ${secs}s",
            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (storm) Ice else Muted, maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.8.dp, Ink, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val s = size.minDimension
            for (i in 0 until 3) {
                val y = (.2f + i * .3f) * s
                drawLine(Muted, Offset(.1f * s, y), Offset(.9f * s, y), strokeWidth = .11f * s, cap = StrokeCap.Round)
                drawCircle(Ice, .13f * s, Offset((.3f + i * .2f) * s, y))
            }
        }
    }
}

@Composable
private fun AchievementBanner(title: String?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf("") }
    if (title != null) shown = title
    AnimatedVisibility(visible = title != null, modifier = modifier, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFE49A).copy(alpha = 0.82f))
                .border(2.dp, Ink.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarIcon(GoldText, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Award: $shown", fontSize = 13.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun TutorialHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ember)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
internal fun LockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawArc(color, 180f, 180f, false, Offset(.25f * s, .08f * s), Size(.5f * s, .5f * s), style = Stroke(width = .1f * s))
        drawRoundRect(color, Offset(.15f * s, .4f * s), Size(.7f * s, .52f * s), CornerRadius(.08f * s))
    }
}

@Composable
private fun ToastBanner(text: String?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf("") }
    if (text != null) shown = text
    AnimatedVisibility(visible = text != null, modifier = modifier, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut()) {
        Text(
            shown,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = CozyFont,
            fontWeight = FontWeight.Medium,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Panel.copy(alpha = 0.78f))
                .border(2.dp, Ink.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SupplyCrate(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "crate")
    val scale by pulse.animateFloat(
        1f, 1.12f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "crateScale",
    )
    Box(
        modifier
            .size((54 * scale).dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Wood)
            .border(2.dp, Color(0xFF8A5F3A), RoundedCornerShape(12.dp))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val s = size.minDimension
            drawRoundRect(Color(0xFFD9B27C), Offset(.05f * s, .15f * s), Size(.9f * s, .7f * s), CornerRadius(.08f * s))
            drawLine(Color(0xFF8A5F3A), Offset(.05f * s, .5f * s), Offset(.95f * s, .5f * s), strokeWidth = .09f * s)
            drawLine(Color(0xFF8A5F3A), Offset(.5f * s, .15f * s), Offset(.5f * s, .85f * s), strokeWidth = .09f * s)
            drawCircle(Gold, .09f * s, Offset(.5f * s, .5f * s))
        }
    }
}

@Composable
internal fun StarIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val p = Path()
        for (i in 0 until 10) {
            val ang = -PI / 2 + i * PI / 5
            val rr = if (i % 2 == 0) .5f else .22f
            val x = (.5f + kotlin.math.cos(ang).toFloat() * rr) * s
            val y = (.52f + kotlin.math.sin(ang).toFloat() * rr) * s
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, color)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelHi.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title.uppercase(), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = Wood, textAlign = TextAlign.Center)
        content()
    }
}

@Composable
internal fun PillButton(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(2.dp, Ink, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
internal fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(label, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        androidx.compose.material3.Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Mint, checkedBorderColor = Mint,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Track, uncheckedBorderColor = Track,
            ),
        )
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
        androidx.compose.material3.Slider(
            value, onChange, modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = Ember, activeTrackColor = Ember, inactiveTrackColor = Track),
        )
    }
}

@Composable
private fun SettingsDialog(vm: GameViewModel, onClose: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    FrostAlert(
        onDismissRequest = onClose,
        title = { Text("Settings", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SettingsCard("Sound") {
                    SettingToggle("Sound on", vm.soundOn) { vm.toggleSound() }
                    LabeledSlider("Music", vm.musicVolume, vm::changeMusicVolume)
                    LabeledSlider("Effects", vm.sfxVolume, vm::changeSfxVolume)
                    SettingToggle("Vibration", vm.vibrationOn) { vm.toggleVibration() }
                    SettingToggle("Reminders", vm.remindersOn) { vm.toggleReminders() }
                    Text("A note when your daily gift is ready or idle earnings are full.", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                }
                SettingsCard("Graphics") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Low", "Medium", "High").forEachIndexed { i, name ->
                            val on = vm.gfx == i
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) Wood else Track)
                                    .clickable { vm.changeGfx(i) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Muted)
                            }
                        }
                    }
                    Text(
                        when (vm.gfx) {
                            0 -> "30 fps, light snowfall, no light effects. Best for battery and older phones."
                            1 -> "60 fps with light effects and full snowfall."
                            else -> "Everything on: light bloom, colour grading and vignette."
                        },
                        fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center,
                    )
                    if (vm.gfx == 2) {
                        Text("Frame rate", fontSize = 12.sp, color = Muted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(60, 90, 120).forEach { fps ->
                                val on = vm.fpsCap == fps
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (on) Wood else Track)
                                        .clickable { vm.changeHighFps(fps) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("$fps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Muted)
                                }
                            }
                        }
                        Text("Your phone may cap this at its screen refresh rate.", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                    }
                }
                SettingsCard("Train paint") {
                    LIVERIES.indices.chunked(5).forEach { rowIdx -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowIdx.forEach { i -> val l = LIVERIES[i]
                            val open = vm.liveryUnlocked(i)
                            val c = if (i == 0) Ice else Color(l.body)
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (open) c else Track)
                                    .border(if (vm.liveryIndex == i) 3.dp else 0.dp, Ink, CircleShape)
                                    .clickable(enabled = open) { vm.chooseLivery(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!open) LockIcon(Muted, Modifier.size(14.dp))
                            }
                        }
                    } }
                    Text("A new paint unlocks with each evolve; more come from events and the Frost Pass (Journey > Style).", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                }
                SettingsCard("Google Play") {
                    if (vm.cloudStatus.isNotEmpty()) Text(vm.cloudStatus, fontSize = 11.sp, color = Ice, textAlign = TextAlign.Center)
                    PillButton(
                        if (vm.cloudSignedIn) "Save to cloud now" else "Sign in with Google Play",
                        Ice, Color.White,
                    ) { if (vm.cloudSignedIn) vm.cloudSaveNow() else vm.cloudSignIn() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    PillButton("Privacy policy", Track, Ink) {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_URL))
                        )
                    }
                    PillButton("Reset progress", Color(0xFFF3D3CF), Color(0xFFB03A2E)) { confirmReset = true }
                }
                Text("Little Express v${BuildConfigVersion}", fontSize = 10.sp, color = Muted)
            }
        },
        confirmButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PillButton("Done", Ice, Color.White, onClick = onClose) }
        },
    )
    if (confirmReset) {
        FrostAlert(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset everything?") },
            text = { Text("All coins, upgrades, cores and achievements will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { UiSound.click();  vm.resetAll(); confirmReset = false; onClose() }) { Text("Reset", color = Color(0xFFB03A2E)) }
            },
            dismissButton = { TextButton(onClick = { UiSound.click();  confirmReset = false }) { Text("Cancel") } },
        )
    }
}

private const val PRIVACY_URL = "https://claude.ai/artifact/L5YCpKP417J28S5icPpDnT"
private const val BuildConfigVersion = "1.0"

@Composable
private fun SpeakerButton(on: Boolean, onClick: () -> Unit) {
    val c = if (on) Ice else Muted
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.8.dp, Ink, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val s = size.minDimension
            val p = Path().apply {
                moveTo(.05f * s, .38f * s); lineTo(.28f * s, .38f * s); lineTo(.55f * s, .15f * s)
                lineTo(.55f * s, .85f * s); lineTo(.28f * s, .62f * s); lineTo(.05f * s, .62f * s); close()
            }
            drawPath(p, c)
            if (on) {
                drawArc(c, -40f, 80f, false, Offset(.35f * s, .25f * s), Size(.5f * s, .5f * s), style = Stroke(width = .09f * s, cap = StrokeCap.Round))
                drawArc(c, -50f, 100f, false, Offset(.25f * s, .1f * s), Size(.7f * s, .8f * s), style = Stroke(width = .09f * s, cap = StrokeCap.Round))
            } else {
                drawLine(c, Offset(.68f * s, .35f * s), Offset(.95f * s, .65f * s), strokeWidth = .1f * s, cap = StrokeCap.Round)
                drawLine(c, Offset(.95f * s, .35f * s), Offset(.68f * s, .65f * s), strokeWidth = .1f * s, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color, onClick: (() -> Unit)? = null) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { UiSound.click(); onClick() } else Modifier)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun BottomBar(
    vm: GameViewModel,
    sheetOpen: Boolean,
    onToggleUpgrades: () -> Unit,
    openSheet: Int = 0,
    onToggleShop: () -> Unit = {},
    onToggleGems: () -> Unit = {},
    onToggleBag: () -> Unit = {},
    onToggleFinance: () -> Unit = {},
    modifier: Modifier = Modifier,
    pulseGear: Boolean = false,
    pulseBoost: Boolean = false,
) {
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "tutorialPulse").animateFloat(
        0.25f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "tutorialPulseValue",
    )
    val fill = vm.boostMeter.toFloat().coerceIn(0f, 1f)
    val status = when {
        vm.isBoosting && vm.combo > 1 -> "Combo x${vm.combo} · x${fmt(vm.boostMultiplier, 2)} income"
        vm.endlessFuel -> "Endless fuel · ${fmtClock(vm.shopFuelLeft)}"
        vm.boostLocked -> "Recharging ${(fill * 100).toInt()}%"
        vm.isBoosting -> "x${fmt(vm.boostMultiplier, 1)} active · ${vm.boostTimeLeft.toInt() + 1}s"
        else -> "Tap BOOST · x${fmt(vm.boostMultiplier, 1)} income"
    }
    val affordable = UPGRADES.count { spec ->
        vm.isUnlocked(spec) && vm.level(spec.type) < spec.maxLevel && vm.coins >= vm.upgradeCost(spec)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel.copy(alpha = 0.8f))
            .border(OutlineW, Wood, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // boost meter on its own line, so the buttons below always fit, even on small phones
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Track)
                    .border(1.2.dp, Ink.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(
                            if (vm.boostLocked) Brush.horizontalGradient(listOf(Muted, Muted))
                            else Brush.horizontalGradient(listOf(Gold, Ember))
                        )
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(status, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(if (pulseGear) 3.dp else 0.dp, Ember.copy(alpha = pulse), CircleShape)
                    .clip(CircleShape)
                    .background(if (sheetOpen) Color(0xFF9FC5EA) else Color(0xFFCDE7F8))
                    .border(2.dp, Ink, CircleShape)
                    .clickable(onClick = onToggleUpgrades),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Ink, Modifier.size(22.dp))
            }
            if (affordable > 0 && !sheetOpen) NotifyBadge(affordable, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
        }
        val sprites = rememberGameSprites()
        Spacer(Modifier.width(6.dp))
        FooterIcon(sprites.iconShop, openSheet == 2, Color(0xFFFFE1C8), onToggleShop)
        Spacer(Modifier.width(6.dp))
        FooterIcon(sprites.iconGems, openSheet == 3, Color(0xFFFFE1EA), onToggleGems)
        Spacer(Modifier.width(6.dp))
        FooterIcon(sprites.iconBag, openSheet == 4, Color(0xFFDDEEFB), onToggleBag)
        Spacer(Modifier.width(6.dp))
        FinanceFooterButton(openSheet == 10, vm.netPerSec < 0.0 || vm.breakdownLeft > 0.0, onToggleFinance)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier
                .height(40.dp)
                .border(if (pulseBoost) 3.5.dp else 2.dp, if (pulseBoost) Ice.copy(alpha = pulse) else Ink, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (vm.boostLocked) Brush.horizontalGradient(listOf(PanelHi, PanelHi))
                    else Brush.horizontalGradient(listOf(Gold, Ember))
                )
                .clickable(enabled = !vm.boostLocked, onClick = vm::boost)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoltIcon(if (vm.boostLocked) Muted else Ink, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("BOOST!", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (vm.boostLocked) Muted else Ink, maxLines = 1, softWrap = false)
        }
    }
    }
}

@Composable
private fun Pill(label: String, bg: Color, fg: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) bg else PanelHi)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) fg else Muted)
    }
}

// ---------- upgrades sheet ----------

@Composable
private fun UpgradesSheet(
    vm: GameViewModel,
    modifier: Modifier = Modifier,
    focus: UpgradeType? = null,
    onFocusDone: () -> Unit = {},
    onOpenDetail: (UpgradeType) -> Unit = {},
    startTab: Int = 0,
    onTabShown: () -> Unit = {},
    onOpenEvolve: () -> Unit = {},
    onClose: () -> Unit,
) {
    var confirmEvolve by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(startTab) }
    LaunchedEffect(startTab) { if (startTab != 0) { tab = startTab; onTabShown() } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // A long list is kept short: cars fold (maxed-out cars start folded) and "Can buy" hides the rest.
    var canBuyOnly by remember { mutableStateOf(false) }
    val foldOverride = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }
    val chain = UPGRADES.filter { it.type != UpgradeType.SURVIVORS }
    val firstLocked = chain.indexOfFirst { !vm.isUnlocked(it) }
    val visibleUpgrades = if (firstLocked < 0) chain else chain.take(firstLocked + 1)
    val byCar = visibleUpgrades.groupBy { carOf(it.type) }.toSortedMap()
    fun maxed(spec: UpgradeSpec) = vm.level(spec.type) >= spec.maxLevel
    fun buyable(spec: UpgradeSpec) = vm.isUnlocked(spec) && !maxed(spec) && vm.coins >= vm.bulkCost(spec, vm.buyCount(spec))
    fun folded(car: Int): Boolean = foldOverride[car] ?: (byCar[car]?.all { maxed(it) } == true)
    fun rowsOf(car: Int): List<UpgradeSpec> {
        if (folded(car)) return emptyList()
        val specs = byCar[car] ?: return emptyList()
        return if (canBuyOnly) specs.filter { buyable(it) } else specs
    }

    // Tapping a device on the train jumps to its upgrade and highlights it for a moment.
    androidx.compose.runtime.LaunchedEffect(focus) {
        if (focus != null) {
            tab = 0
            canBuyOnly = false
            foldOverride[carOf(focus)] = false
            kotlinx.coroutines.delay(90)
            // survivors card, then a header per car and its rows
            var index = 1
            for (car in byCar.keys) {
                index++
                val rows = rowsOf(car)
                val at = rows.indexOfFirst { it.type == focus }
                if (at >= 0) { index += at; break }
                index += rows.size
            }
            listState.animateScrollToItem(index.coerceIn(0, maxOf(0, listState.layoutInfo.totalItemsCount - 1)))
            kotlinx.coroutines.delay(2500)
            onFocusDone()
        }
    }

    val sprites = rememberGameSprites()
    SheetFrame(
        "Upgrades", sprites.iconEvolve, Color(0xFFCDE7F8), onClose, modifier,
        right = {
            if (tab != 1) CurrencyPill({ CoinIcon(Modifier.size(16.dp)) }, fmt(vm.coins), GoldText)
            else CurrencyPill({ CoreIcon(Ice, Modifier.size(15.dp)) }, "${vm.coresAvailable}", Ice)
        },
    ) {
        TabRow(tab, { tab = it }, Modifier.padding(horizontal = 12.dp))
        if (tab == 0) BuyAmountRow(vm, Modifier.padding(horizontal = 12.dp))
        if (tab == 0) {
            val canCount = visibleUpgrades.count { buyable(it) }
            val allFolded = byCar.keys.all { folded(it) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Show", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                SmallChip("All", !canBuyOnly, Modifier.weight(1f)) { canBuyOnly = false }
                SmallChip("Can buy ($canCount)", canBuyOnly, Modifier.weight(1.6f)) { canBuyOnly = true }
                SmallChip(if (allFolded) "Open all" else "Fold all", false, Modifier.weight(1.3f)) {
                    val fold = !allFolded
                    byCar.keys.forEach { foldOverride[it] = fold }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (tab == 2) {
                item {
                    Text(
                        "Level ${vm.playerLevel} · ${vm.xp}/${vm.xpNeeded} XP · ${vm.unlocked.size}/${ACHIEVEMENTS.size} awards. Every award gives XP; every level up gives gems.",
                        fontSize = 11.sp, color = Muted,
                    )
                }
                items(ACHIEVEMENTS) { a -> AchievementRow(a, a.id in vm.unlocked) }
            } else if (tab == 1) {
                item {
                    Text(
                        "Kept forever. Bought with Frost Cores, which you earn by evolving.",
                        fontSize = 11.sp, color = Muted,
                    )
                }
                items(PERMANENT) { spec ->
                    val lvl = vm.permLevel(spec.type)
                    val maxed = lvl >= spec.maxLevel
                    val cost = vm.permCost(spec)
                    UpgradeRow(
                        type = permIcon(spec.type),
                        title = spec.title,
                        level = "Lv $lvl/${spec.maxLevel}",
                        effect = spec.effect,
                        note = null,
                        buttonTop = if (maxed) "MAX" else "Buy",
                        buttonBottom = if (maxed) null else "$cost",
                        enabled = !maxed && vm.coresAvailable >= cost,
                        onBuy = { vm.buyPerm(spec) },
                        core = true,
                        sprite = sprites.device[permIcon(spec.type)],
                        progress = lvl.toFloat() / spec.maxLevel,
                    )
                }
            } else {
            item { SurvivorsCard(vm, onInfo = { onOpenDetail(UpgradeType.SURVIVORS) }) }
            // Show every unlocked upgrade plus the next locked one; the rest stay a surprise.
            byCar.forEach { (car, specs) ->
            val rows = rowsOf(car)
            item(key = "car$car") {
                val isFolded = folded(car)
                val done = specs.count { maxed(it) }
                val can = specs.count { buyable(it) }
                CarHeader(
                    when (car) { 0 -> "Car 1 · Engine"; 1 -> "Car 2 · Workshop"; else -> "Car ${car + 1}" },
                    listOf(Ember, Ice, Mint, XpPurple, GoldText, GemPink)[car % 6],
                    buildString {
                        append("${specs.size} upgrades")
                        if (can > 0) append(" · $can to buy")
                        if (done > 0) append(" · $done maxed")
                        if (!isFolded && canBuyOnly && rows.isEmpty()) append(" · nothing to buy")
                    },
                    folded = isFolded,
                    onToggle = { foldOverride[car] = !isFolded },
                )
            }
            items(rows, key = { it.type.name }) { spec ->
                val lvl = vm.level(spec.type)
                val maxed = lvl >= spec.maxLevel
                val n = vm.buyCount(spec)
                val cost = vm.bulkCost(spec, n)
                val next = vm.nextMilestone(spec.type)
                val mult = vm.milestoneMult(spec.type)
                val locked = !vm.isUnlocked(spec)
                val req = UPGRADES.firstOrNull { it.type == spec.unlockType }
                UpgradeRow(
                    type = spec.type,
                    title = spec.title,
                    level = if (locked) "Locked" else if (lvl == 0) "Not installed" else "Lv $lvl${if (spec.maxLevel != Int.MAX_VALUE) "/${spec.maxLevel}" else ""}",
                    effect = if (locked && vm.blockedByTier(spec)) "Evolve your train to add the next car" else if (locked) "Reach ${req?.title} Lv ${spec.unlockLevel} (now Lv ${if (req != null) vm.level(req.type) else 0})" else spec.effect,
                    note = if (locked) null else when {
                        mult > 1.0 && next != null -> "Bonus x${fmt(mult)} · next x2 at Lv $next"
                        mult > 1.0 -> "Bonus x${fmt(mult)}"
                        next != null -> "Next x2 bonus at Lv $next"
                        else -> null
                    },
                    buttonTop = if (locked) "Locked" else if (maxed) "MAX" else if (lvl == 0) "Buy" else "+$n",
                    buttonBottom = if (maxed || locked) null else fmt(cost),
                    enabled = !locked && !maxed && vm.coins >= cost,
                    onBuy = { vm.buy(spec) },
                    locked = locked,
                    highlight = focus == spec.type,
                    onInfo = if (locked) null else ({ onOpenDetail(spec.type) }),
                    sprite = sprites.device[spec.type],
                    progress = when {
                        spec.maxLevel != Int.MAX_VALUE -> lvl.toFloat() / spec.maxLevel
                        next != null -> (lvl - (Balance.MILESTONES.lastOrNull { it <= lvl } ?: 0)).toFloat() / (next - (Balance.MILESTONES.lastOrNull { it <= lvl } ?: 0))
                        else -> null
                    },
                )
            }
            }
            if (firstLocked >= 0 && firstLocked + 1 < chain.size) item {
                Text("${chain.size - firstLocked - 1} more upgrades to discover...", fontSize = 11.sp, color = Muted)
            }
        }
            if (tab == 0) item { EvolveCard(vm, onEvolve = onOpenEvolve) }
        }
    }

    if (confirmEvolve) {
        FrostAlert(
            onDismissRequest = { confirmEvolve = false },
            title = { Text("Evolve your train?") },
            text = {
                Text("Coins and run upgrades will reset (permanent upgrades stay). You will get ${vm.coreGain} Frost Core(s) (permanent bonus) and unlock the next train.")
            },
            confirmButton = {
                TextButton(onClick = { UiSound.click();  vm.evolve(); confirmEvolve = false }) { Text("Evolve") }
            },
            dismissButton = {
                TextButton(onClick = { UiSound.click();  confirmEvolve = false }) { Text("Cancel") }
            },
        )
    }
}

private fun permIcon(t: PermType): UpgradeType = when (t) {
    PermType.INCOME -> UpgradeType.HEAT
    PermType.SPEED -> UpgradeType.ENGINE
    PermType.START -> UpgradeType.COAL
    PermType.BOOST -> UpgradeType.BOOSTER
    PermType.OFFLINE -> UpgradeType.CONDUCTOR
    PermType.CREW -> UpgradeType.FOOD
}

@Composable
internal fun CoreIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val p = Path().apply {
            moveTo(.5f * s, .02f * s); lineTo(.95f * s, .5f * s); lineTo(.5f * s, .98f * s); lineTo(.05f * s, .5f * s); close()
        }
        drawPath(p, color)
        drawLine(Color.White.copy(alpha = .55f), Offset(.5f * s, .2f * s), Offset(.5f * s, .8f * s), strokeWidth = .07f * s)
    }
}

@Composable
private fun TabRow(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // a segmented switch in a recessed wooden track
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PanelHi)
            .border(2.dp, Ink, RoundedCornerShape(16.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf("Upgrades", "Permanent").forEachIndexed { i, label ->
            val on = selected == i
            val c = listOf(Color(0xFFFFC98A), Color(0xFFB8D8F2), Color(0xFFFFE49A))[i]
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (on) Brush.verticalGradient(listOf(c.copy(alpha = 0.6f).compositeOver(Color.White), c)) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(if (on) 2.dp else 0.dp, if (on) Ink else Color.Transparent, RoundedCornerShape(13.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted)
            }
        }
    }
}

@Composable
internal fun AchievementRow(a: Achievement, done: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (done) Panel else PanelHi.copy(alpha = 0.6f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (done) Gold else Track),
            contentAlignment = Alignment.Center,
        ) {
            StarIcon(if (done) Color.White else Muted.copy(alpha = 0.6f), Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(a.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (done) Ink else Muted)
            Text(a.desc, fontSize = 11.sp, color = Muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("+${a.xp} XP", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Color(0xFF8E6FD8) else Muted)
            Text(if (a.cores > 0) "+${a.cores} core" else "coins", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (done) Mint else Muted)
        }
    }
}

@Composable
private fun BuyAmountRow(vm: GameViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Buy", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
        listOf(1 to "x1", 10 to "x10", 100 to "x100", 0 to "MAX").forEach { (amount, label) ->
            val on = vm.buyAmount == amount
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Brush.verticalGradient(listOf(Color(0xFFD3E9FA), Color(0xFF9FC5EA))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f))))
                    .border(2.dp, if (on) Ink else Ink.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                    .clickable { vm.buyAmount = amount }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted)
            }
        }
        if (vm.autoBuyUnlocked) {
            val on = vm.autoBuy
            Box(
                Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Brush.verticalGradient(listOf(Color(0xFFCDEBDC), Color(0xFF8FD1AE))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f))))
                    .border(2.dp, if (on) Ink else Ink.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                    .clickable { UiSound.click(); vm.toggleAutoBuy() }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (on) "AUTO ✓" else "AUTO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted)
            }
        }
    }
}

@Composable
private fun UpgradeRow(
    type: UpgradeType,
    title: String,
    level: String,
    effect: String,
    note: String?,
    buttonTop: String,
    buttonBottom: String?,
    enabled: Boolean,
    onBuy: () -> Unit,
    core: Boolean = false,
    locked: Boolean = false,
    highlight: Boolean = false,
    onInfo: (() -> Unit)? = null,
    priceIcon: Int = 0, // 0 coin, 1 frost core, 2 gem, 3 none (real-money price)
    icon: (@Composable () -> Unit)? = null,
    tint: Color? = null,
    progress: Float? = null,
    sprite: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    val accent = if (locked) Muted else tint ?: upgradeColor(type)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (locked) PanelHi.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f))
            .border(if (highlight) 3.dp else 2.dp, if (highlight) Ember else if (locked) Ink.copy(alpha = 0.35f) else Ink, shape)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // the device on a round coloured plate
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, accent.copy(alpha = 0.35f).compositeOver(Panel))))
                .border(2.dp, if (locked) Ink.copy(alpha = 0.4f) else Ink, CircleShape)
                .then(if (onInfo != null) Modifier.clickable { onInfo() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                locked -> LockIcon(Muted, Modifier.size(22.dp))
                sprite != null -> Image(sprite, null, Modifier.size(42.dp))
                icon != null -> icon()
                else -> UpgradeIcon(type, accent, Modifier.size(26.dp))
            }
        }
        Column(Modifier.weight(1f).then(if (onInfo != null) Modifier.clickable { onInfo() } else Modifier)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (locked) Muted else Ink, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (level.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        level, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (locked) Muted else Ink, maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(accent.copy(alpha = 0.25f)).padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(effect, fontSize = 11.sp, color = Muted, lineHeight = 13.sp)
            if (progress != null && !locked) {
                Box(
                    Modifier.padding(top = 3.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Track).border(1.dp, Ink.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                ) { Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(accent)) }
            }
            if (note != null) Text(note, fontSize = 10.sp, color = GoldText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
        }
        ChunkyButton(
            enabled,
            listOf(accent.copy(alpha = 0.55f).compositeOver(Color.White), accent),
            onBuy,
            Modifier.width(78.dp),
            repeat = true,
        ) {
            Text(buttonTop, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) Ink else Muted, maxLines = 1, softWrap = false)
            if (buttonBottom != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        priceIcon == 2 -> GemIcon(Modifier.size(11.dp))
                        priceIcon == 3 -> {}
                        core || priceIcon == 1 -> CoreIcon(if (enabled) Ice else Muted, Modifier.size(10.dp))
                        else -> CoinIcon(Modifier.size(10.dp))
                    }
                    Spacer(Modifier.width(3.dp))
                    Text(buttonBottom, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) Ink else Muted, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun EvolveCard(vm: GameViewModel, onEvolve: () -> Unit) {
    val next = TIERS[min(vm.evolutions + 1, TIERS.lastIndex)]
    val progress = (vm.runLevels.toFloat() / vm.evolveLevels).coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFDCEEF9), Color(0xFFE8E0F8))))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Evolve", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            "${TIERS[vm.tierIndex].name} (Tier ${vm.tierIndex + 1}) · Frost Cores ${vm.cores} (+${(vm.cores * Balance.CORE_BONUS * 100).toInt()}% coins) · ${vm.maxCars} cars",
            fontSize = 11.sp,
            color = Muted,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Track)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(listOf(Ice, Color(0xFFB197FC))))
            )
        }
        Text("Upgrade levels bought: ${vm.runLevels} / ${vm.evolveLevels}" + (if (vm.maxCars < 11) ". The next train gets ${minOf(vm.maxCars + 2, 11) - vm.maxCars} more cars." else ""), fontSize = 10.sp, color = Muted)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (vm.canEvolve) Color(0xFFB197FC) else PanelHi)
                .clickable(onClick = onEvolve)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (vm.canEvolve) "Evolve to ${next.name}  (+${vm.coreGain} cores)" else "Open the Evolve menu",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (vm.canEvolve) Ink else Muted,
            )
        }
    }
}


/** Shown instead of the game when the previous run crashed, so the error can be copied and reported. */
@Composable
fun CrashScreen(report: String, onContinue: () -> Unit, onReset: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Snow)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Little Express crashed last time", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Sorry about that! Please copy this report (or take a screenshot) and send it to the developer. " +
                "Safe mode is on: post-processing is off and the frame rate is 60.",
            fontSize = 12.sp, color = Muted,
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .padding(10.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    report,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Copy report", Wood, Color.White) {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(report))
            }
            PillButton("Continue", Ice, Color.White, onClick = onContinue)
            PillButton("Reset save", Color(0xFFF3D3CF), Color(0xFFB03A2E), onClick = onReset)
        }
    }
}


// ---------- per-upgrade pop-up: shows the device and animates it when it is upgraded ----------

@Composable
private fun UpgradeDialog(vm: GameViewModel, type: UpgradeType, onClose: () -> Unit) {
    val spec = UPGRADES.firstOrNull { it.type == type } ?: return
    val accent = upgradeColor(type)
    val lvl = vm.level(type)
    val maxed = lvl >= spec.maxLevel
    val locked = !vm.isUnlocked(spec)
    val n = vm.buyCount(spec)
    val cost = vm.bulkCost(spec, n)
    val canBuy = !locked && !maxed && vm.coins >= cost
    val buyNow = androidx.compose.runtime.rememberUpdatedState { vm.buy(spec) }
    val sprites = rememberGameSprites()

    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { t = (it / 1_000_000L) / 1000f } }
    val flash = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    var prev by remember { mutableIntStateOf(lvl) }
    LaunchedEffect(lvl) {
        if (lvl > prev) {
            launch { flash.snapTo(1f); flash.animateTo(0f, tween(1100)) }
            launch { pop.snapTo(1.22f); pop.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 280f)) }
        }
        prev = lvl
    }
    val enter = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f)) }

    val car = carOf(type)
    val place = when {
        type == UpgradeType.SURVIVORS -> "Your crew"
        type == UpgradeType.PLOW -> "Car 1 · the nose"
        else -> "Car ${car + 1}"
    }
    val category = upgradeCategory(spec)

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 380.dp)
                .graphicsLayer { scaleX = enter.value; scaleY = enter.value },
        ) {
            Column(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Panel)
                    .border(3.dp, Ink, RoundedCornerShape(28.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // where it is and what it does
                Text(place, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    category, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.25f))
                        .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(10.dp))
                // the stage: the device on a round spotlight, with confetti when it levels up
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f
                        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0D2), accent.copy(alpha = 0.35f)), c, r), r, c)
                        // slowly turning rays
                        for (i in 0 until 12) {
                            val a = (i * PI / 6).toFloat() + t * 0.25f
                            val ray = Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + cos(a - 0.11f) * r, c.y + sin(a - 0.11f) * r)
                                lineTo(c.x + cos(a + 0.11f) * r, c.y + sin(a + 0.11f) * r)
                                close()
                            }
                            drawPath(ray, Color.White.copy(alpha = 0.28f))
                        }
                        drawCircle(Ink, r - 1.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                        drawOval(Color(0x22000000), Offset(c.x - r * 0.45f, c.y + r * 0.52f), Size(r * 0.9f, r * 0.16f))
                    }
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = if (lvl == 0 && !locked) 0.45f else 1f },
                    ) {
                        val side = min(size.width, size.height)
                        if (!drawDeviceSprite(sprites, type, (size.width - side) / 2f, (size.height - side) / 2f, side, t, 0.3f)) {
                            drawGadget(spec.gadget, accent, t, lvl, flash.value)
                        }
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val f = flash.value
                        if (f > 0.01f) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val cols = listOf(Color(0xFFF28C8C), Color(0xFFFFC85C), Color(0xFF8FDBA8), Color(0xFF8FBCE6), Color(0xFFC9B6F2), Color(0xFFF49AB2))
                            for (i in 0 until 26) {
                                val a = i * 0.7f + i * i * 0.13f
                                val sp = 0.55f + (i % 5) * 0.12f
                                val d = (1f - f) * size.minDimension * 0.55f * sp
                                val p = Offset(c.x + cos(a) * d, c.y + sin(a) * d + (1f - f) * (1f - f) * 60f)
                                rotate(a * 57f + (1f - f) * 400f, p) {
                                    drawRect(cols[i % cols.size].copy(alpha = f.coerceAtMost(1f)), Offset(p.x - 4f, p.y - 2.5f), Size(8f, 5f))
                                }
                            }
                        }
                    }
                    if (flash.value > 0.05f) {
                        Text(
                            "Level up!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer { alpha = flash.value; translationY = -30f * (1f - flash.value) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFE49A))
                                .border(2.dp, Ink, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                    // level badge at the bottom of the stage
                    Text(
                        if (locked) "Locked" else if (lvl == 0) "Not installed yet" else if (spec.maxLevel != Int.MAX_VALUE) "Lv $lvl / ${spec.maxLevel}" else "Lv $lvl",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (locked || lvl == 0) PanelHi else Color(0xFFFFE49A))
                            .border(2.dp, Ink, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                // level pips (or progress to the next x2 milestone for endless upgrades)
                if (!locked) {
                    if (spec.maxLevel <= 20) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                            for (i in 0 until spec.maxLevel) {
                                Box(
                                    Modifier
                                        .size(if (spec.maxLevel > 12) 9.dp else 12.dp)
                                        .clip(CircleShape)
                                        .background(if (i < lvl) accent else PanelHi)
                                        .border(1.5.dp, Ink, CircleShape),
                                )
                            }
                        }
                    } else {
                        val next = vm.nextMilestone(type)
                        if (next != null) {
                            val prevM = Balance.MILESTONES.lastOrNull { it <= lvl } ?: 0
                            val frac = ((lvl - prevM).toFloat() / (next - prevM)).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PanelHi)
                                    .border(1.5.dp, Ink, RoundedCornerShape(6.dp)),
                            ) { Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(accent)) }
                            Text("x2 bonus at Lv $next", fontSize = 11.sp, color = GoldText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // now -> next
                if (locked) {
                    val req = UPGRADES.firstOrNull { it.type == spec.unlockType }
                    Text(
                        if (vm.blockedByTier(spec)) "This car comes with a bigger train.\nEvolve to unlock it." else "Reach ${req?.title} Lv ${spec.unlockLevel} to unlock.",
                        fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center,
                    )
                } else {
                    val eff = spec.eff
                    val nowText = when {
                        eff != null -> if (lvl == 0) "Not installed" else effText(eff, spec.per * lvl)
                        else -> if (lvl == 0) "Not installed" else "Level $lvl"
                    }
                    val nextText = when {
                        maxed -> "Fully upgraded"
                        eff != null -> effText(eff, spec.per * (lvl + 1))
                        else -> spec.effect
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        StatBubble("Now", nowText, PanelHi, Modifier.weight(1f))
                        Text("  >  ", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        StatBubble(if (maxed) "Max" else if (lvl == 0) "After buying" else "Next", nextText, accent.copy(alpha = 0.3f), Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (!locked && !maxed && lvl > 0) {
                    BuyAmountRow(vm, Modifier.fillMaxWidth(0.9f))
                    Spacer(Modifier.height(8.dp))
                }
                // the big button: tap to buy, hold to keep buying
                Column(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (canBuy) Brush.verticalGradient(listOf(Color(0xFFFFC98A), Color(0xFFFFA65C))) else Brush.verticalGradient(listOf(PanelHi, PanelHi)))
                        .border(2.5.dp, Ink, RoundedCornerShape(22.dp))
                        .pointerInput(canBuy) {
                            if (!canBuy) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown()
                                buyNow.value()
                                var wait = 450L
                                while (true) {
                                    val released = withTimeoutOrNull(wait) { waitForUpOrCancellation(); true } ?: false
                                    if (released) break
                                    buyNow.value()
                                    wait = (wait * 0.82f).toLong().coerceAtLeast(90L)
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (locked) "Locked" else if (maxed) "All done!" else if (lvl == 0) "Buy & install" else "Upgrade +$n",
                        fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (canBuy) Ink else Muted,
                    )
                    if (!locked && !maxed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinIcon(Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(fmt(cost), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (canBuy) Ink else Muted)
                        }
                    }
                }
                if (!locked && !maxed) Text("Hold the button to keep upgrading", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 6.dp))
                // golden devices: Gold Polish makes this device's effect x1.5
                if (!locked && lvl > 0 && type != UpgradeType.SURVIVORS) {
                    Spacer(Modifier.height(8.dp))
                    if (vm.isGolden(type)) {
                        Text("Golden device · effect x1.5", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GoldText)
                    } else {
                        ChunkyButton(vm.meta.polish > 0, listOf(Color(0xFFFFE7A0), Color(0xFFF2B84B)), { vm.makeGolden(type) }, Modifier.fillMaxWidth(0.85f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PolishIcon(Modifier.size(16.dp)); Spacer(Modifier.width(5.dp))
                                Text(if (vm.meta.polish > 0) "Make golden (${vm.meta.polish} Gold Polish)" else "Gold Polish needed (wheel, events, pass)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (vm.meta.polish > 0) Ink else Muted)
                            }
                        }
                    }
                }
            }
            // name ribbon on top of the card
            Text(
                spec.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f).compositeOver(Panel), accent.copy(alpha = 0.35f).compositeOver(Panel))))
                    .border(3.dp, Ink, RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
            CloseButton(onClose, Modifier.align(Alignment.TopEnd).padding(top = 6.dp))
        }
    }
}

@Composable
private fun StatBubble(label: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(2.dp, Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
    }
}

private fun upgradeCategory(spec: UpgradeSpec): String = when (spec.eff) {
    Eff.SPEED -> "Speed"
    Eff.INCOME -> "Coins"
    Eff.RECHARGE -> "Boost recharge"
    Eff.BOOSTMULT -> "Boost power"
    Eff.BOOSTLEN -> "Boost time"
    Eff.OFFLINE -> "Offline earnings"
    Eff.STATION -> "Stations"
    Eff.CRATE -> "Supply crates"
    Eff.STORM -> "Snow storms"
    Eff.NIGHT -> "Night time"
    Eff.AURORA -> "Aurora"
    null -> when (spec.type) {
        UpgradeType.ENGINE, UpgradeType.PLOW -> "Speed"
        UpgradeType.WAGON, UpgradeType.HEAT, UpgradeType.FOOD -> "Coins"
        UpgradeType.COAL -> "Boost recharge"
        UpgradeType.BOOSTER -> "Boost power"
        UpgradeType.CONDUCTOR -> "Offline earnings"
        UpgradeType.SURVIVORS -> "Crew"
        else -> "Upgrade"
    }
}


// ---------- shop: its own sheet, opened from the footer ----------

@Composable
private fun ShopIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawRoundRect(color, Offset(.14f * s, .34f * s), Size(.72f * s, .56f * s), CornerRadius(.1f * s))
        drawArc(color, 180f, 180f, false, Offset(.3f * s, .1f * s), Size(.4f * s, .44f * s), style = Stroke(.09f * s))
        drawCircle(Color.White.copy(alpha = .85f), .07f * s, Offset(.5f * s, .6f * s))
    }
}

@Composable
private fun ShopSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit, onGetGems: () -> Unit) {
    val sp = rememberGameSprites()
    SheetFrame(
        "Shop", sp.iconShop, Color(0xFFFFD9B8), onClose, modifier,
        right = { CurrencyPill({ GemIcon(Modifier.size(16.dp)) }, "${vm.gems}", GemPink, onClick = onGetGems, plus = true) },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle("Boosters", Ember, "Go to your bag. Up to ${Balance.MAX_ACTIVE_BOOSTERS} run at once.") }
            items(ShopItem.values().toList().chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (item in pair) ShopCard(vm, item, Modifier.weight(1f))
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (Balance.ADS_ENABLED) {
                item { SectionTitle("Free rewards", Mint, "Watch a short ad") }
                items(listOf(AdReward.BOOSTER, AdReward.COINS)) { r -> AdRow(vm, r) }
            }
            item {
                ChunkyButton(true, listOf(Color(0xFFFFC2DA), Color(0xFFF08DB5)), onGetGems, Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GemIcon(Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                        Text("Need more gems? Open the Gem Shop", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                }
            }
        }
    }
}

internal fun itemColor(item: ShopItem): Color = when (item) {
    ShopItem.COCOA -> Color(0xFFF7A3A3)
    ShopItem.TURBO -> Color(0xFFFFC98A)
    ShopItem.TICKET -> Color(0xFFFFE49A)
    ShopItem.TANK -> Color(0xFFBFE0D2)
    ShopItem.CLEAR -> Color(0xFFB8D8F2)
    ShopItem.RUSH5 -> Color(0xFFFFC98A)
    ShopItem.RUSH10 -> Color(0xFFF49AB2)
    ShopItem.RUSH100 -> Color(0xFFC9B6F2)
}

/** A shop booster as a card: the item on a sunburst, what it does, how many are in the bag and the gem price. */
@Composable
private fun ShopCard(vm: GameViewModel, item: ShopItem, modifier: Modifier = Modifier) {
    val cost = vm.shopCost(item)
    val owned = vm.owned(item)
    val c = itemColor(item)
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .border(2.dp, Ink, RoundedCornerShape(18.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(74.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(70.dp)) {
                val r = size.minDimension / 2f
                drawCircle(c.copy(alpha = 0.45f), r)
                for (i in 0 until 10) {
                    val a = (i * PI / 5).toFloat()
                    val p = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(center.x + cos(a - 0.14f) * r, center.y + sin(a - 0.14f) * r)
                        lineTo(center.x + cos(a + 0.14f) * r, center.y + sin(a + 0.14f) * r); close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.45f))
                }
                drawCircle(Ink, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            ItemIcon(item, Modifier.size(52.dp))
            if (owned > 0) Text(
                "x$owned", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFE49A)).border(1.5.dp, Ink, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp),
            )
        }
        Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text(item.desc, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, lineHeight = 12.sp)
        Spacer(Modifier.height(6.dp))
        ChunkyButton(vm.gems >= cost, listOf(Color(0xFFFFC2DA), Color(0xFFF08DB5)), { vm.buyShop(item) }, Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GemIcon(Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                Text("$cost", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (vm.gems >= cost) Ink else Muted)
            }
        }
    }
}

// ---------- gems ----------

internal val GemPink = Color(0xFFD6336C)

@Composable
internal fun GemIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val outline = Path().apply {
            moveTo(.22f * s, .14f * s); lineTo(.78f * s, .14f * s); lineTo(.96f * s, .38f * s)
            lineTo(.5f * s, .94f * s); lineTo(.04f * s, .38f * s); close()
        }
        drawPath(outline, Brush.linearGradient(listOf(Color(0xFFFF8FB8), Color(0xFFD6336C), Color(0xFF8E1B4B)), Offset(0f, 0f), Offset(s, s)))
        val table = Path().apply { moveTo(.22f * s, .14f * s); lineTo(.78f * s, .14f * s); lineTo(.64f * s, .38f * s); lineTo(.36f * s, .38f * s); close() }
        drawPath(table, Color.White.copy(alpha = 0.45f))
        drawLine(Color.White.copy(alpha = 0.35f), Offset(.04f * s, .38f * s), Offset(.96f * s, .38f * s), .03f * s)
        drawLine(Color.White.copy(alpha = 0.3f), Offset(.36f * s, .38f * s), Offset(.5f * s, .94f * s), .03f * s)
        drawLine(Color.White.copy(alpha = 0.3f), Offset(.64f * s, .38f * s), Offset(.5f * s, .94f * s), .03f * s)
    }
}

/** A little pile of gems: bigger packs show more gems. */
@Composable
private fun GemPile(count: Int, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val spots = listOf(Triple(0f, 0f, 1f), Triple(-0.32f, 0.18f, 0.8f), Triple(0.32f, 0.18f, 0.8f), Triple(-0.16f, -0.22f, 0.7f), Triple(0.18f, -0.24f, 0.7f), Triple(0f, 0.32f, 0.75f))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            for (k in (0 until count.coerceIn(1, spots.size)).reversed()) {
                val (dx, dy, sc) = spots[k]
                GemIcon(Modifier.align(Alignment.Center).offset(x = w * dx, y = w * dy).size(w * 0.5f * sc))
            }
        }
    }
}

@Composable
private fun GemShopSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit, onBack: () -> Unit) {
    val sp = rememberGameSprites()
    SheetFrame(
        "Gem Shop", sp.iconGems, Color(0xFFFFC9DE), onClose, modifier, onBack = onBack,
        right = { CurrencyPill({ GemIcon(Modifier.size(16.dp)) }, "${vm.gems}", GemPink) },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (Balance.ADS_ENABLED) {
                item { SectionTitle("Free with ads", Mint) }
                item { AdRow(vm, AdReward.GEMS) }
            }
            item { SectionTitle("Gem packs", GemPink, if (!Balance.IAP_ENABLED) "Open after launch on Google Play" else null) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GemPack.values().forEachIndexed { i, pack ->
                        val price = vm.gemPackPrice(pack)
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFF0F6), Color.White)))
                                .border(2.dp, Ink, RoundedCornerShape(18.dp))
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (i == GemPack.values().lastIndex) Text(
                                "BEST VALUE", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Ember).padding(horizontal = 5.dp, vertical = 1.dp),
                            ) else Spacer(Modifier.height(13.dp))
                            GemPile(2 + i * 2, Modifier.size(58.dp))
                            Text("${pack.gems}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GemPink)
                            Text(pack.title, fontSize = 9.sp, color = Muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            ChunkyButton(Balance.IAP_ENABLED, listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), { vm.buyGemPack(pack) }, Modifier.fillMaxWidth()) {
                                Text(if (Balance.IAP_ENABLED) price ?: "..." else "Soon", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (Balance.IAP_ENABLED) Ink else Muted, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Trade coins", GoldText, "${vm.gemTradeLeft} of ${Balance.GEM_TRADE_PER_DAY} left today") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GemTrade.values().forEachIndexed { i, trade ->
                        val cost = vm.gemTradeCost(trade)
                        val can = vm.coins >= cost && vm.gemTradeLeft >= trade.gems
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                                .border(2.dp, Ink, RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GemPile(1 + i * 2, Modifier.size(38.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+${trade.gems}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GemPink)
                                ChunkyButton(can, listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)), { vm.tradeForGems(trade) }, Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CoinIcon(Modifier.size(11.dp)); Spacer(Modifier.width(3.dp))
                                        Text(fmt(cost), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (can) Ink else Muted, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("Free gems", XpPurple)
                Text(
                    "Daily gift: ${Balance.DAILY_GEMS} gems, ${Balance.DAILY_GEMS * 4} on every 7th day in a row. Level ups, quests and awards give gems too.",
                    fontSize = 11.sp, color = Muted,
                )
            }
        }
    }
}


// ---------- footer icons, bag, active booster indicators ----------

@Composable
private fun FooterIcon(img: androidx.compose.ui.graphics.ImageBitmap?, open: Boolean, bg: Color, onClick: () -> Unit, badge: Int = 0) {
    Box(Modifier.size(38.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (open) bg.copy(red = bg.red * 0.92f, green = bg.green * 0.88f, blue = bg.blue * 0.86f) else bg)
                .border(if (open) 3.dp else 2.dp, Ink, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (img != null) Image(img, null, Modifier.size(26.dp))
        }
        if (badge > 0) NotifyBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
    }
}

/** Footer button that opens the Finances sheet: a little bar chart; a red dot warns about losses or a breakdown. */
@Composable
private fun FinanceFooterButton(open: Boolean, warn: Boolean, onClick: () -> Unit) {
    val bg = Color(0xFFD5EFD9)
    Box(Modifier.size(38.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (open) bg.copy(red = bg.red * 0.9f, green = bg.green * 0.9f, blue = bg.blue * 0.88f) else bg)
                .border(if (open) 3.dp else 2.dp, Ink, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                val bar = w * 0.22f
                val hs = floatArrayOf(0.38f, 0.62f, 0.9f)
                for (i in 0 until 3) {
                    val x = w * 0.08f + i * (bar + w * 0.1f)
                    drawRoundRect(Color(0xFF3FAE7A), Offset(x, h * (1f - hs[i])), Size(bar, h * hs[i]), CornerRadius(2.dp.toPx()))
                    drawRoundRect(Ink, Offset(x, h * (1f - hs[i])), Size(bar, h * hs[i]), CornerRadius(2.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
                }
            }
        }
        if (warn) Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(12.dp).clip(CircleShape).background(Color(0xFFE5484D)).border(1.5.dp, Ink, CircleShape))
    }
}

/** The red notification dot with a number. Drawn outside the round button so it is never cut off. */
@Composable
internal fun NotifyBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Ember)
            .border(1.5.dp, Panel, RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 9) "9+" else "$count", fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
internal fun ItemIcon(item: ShopItem, modifier: Modifier = Modifier) {
    val img = rememberGameSprites().item[item]
    if (img != null) Image(img, item.title, modifier) else Box(modifier)
}

internal fun fmtClock(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun InventorySheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit, onShop: () -> Unit) {
    val sp = rememberGameSprites()
    SheetFrame(
        "My Bag", sp.iconBag, Color(0xFFCFE6F8), onClose, modifier,
        right = { PillButton("Shop", Color(0xFFFFE1C8), Ink, onClick = onShop) },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionTitle(
                    "Running ${vm.activeBoosters.size} / ${Balance.MAX_ACTIVE_BOOSTERS}", Ice,
                    if (vm.inventoryCount == 0) "Your bag is empty. Buy boosters in the shop." else "Tap a running booster on the right to stop it.",
                )
            }
            items(ShopItem.values().toList()) { item ->
                val owned = vm.owned(item)
                val left = vm.shopLeft(item)
                UpgradeRow(
                    type = UpgradeType.BOOSTER,
                    title = item.title,
                    level = "x$owned",
                    effect = item.desc,
                    note = if (left > 0.0) "Running · ${fmtClock(left)} left" else vm.whyNotUse(item),
                    buttonTop = "Use",
                    buttonBottom = null,
                    enabled = vm.canUse(item),
                    onBuy = { vm.useItem(item) },
                    icon = { ItemIcon(item, Modifier.size(38.dp)) },
                    tint = itemColor(item),
                )
            }
        }
    }
}

/** Round badges for every booster that is running, with a ring that empties as time runs out. */
@Composable
private fun ActiveBoosters(vm: GameViewModel, modifier: Modifier = Modifier) {
    val running = vm.activeBoosters
    if (running.isEmpty()) return
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "boostBadge").animateFloat(
        0.94f, 1.06f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "boostBadgePulse",
    )
    var open by remember { mutableStateOf<ShopItem?>(null) }
    LaunchedEffect(open) { if (open != null) { kotlinx.coroutines.delay(6000); open = null } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        for (item in running) Row(verticalAlignment = Alignment.Top) {
            if (open == item) {
                BoosterInfo(item, vm.shopLeft(item), Modifier.padding(end = 6.dp, top = 2.dp), onStop = { vm.cancelBooster(item); open = null })
            }
            val left = vm.shopLeft(item)
            val chunk = item.durationSec * kotlin.math.ceil(left / item.durationSec)
            val frac = (left / chunk).toFloat().coerceIn(0f, 1f)
            val ring = when (item) { ShopItem.COCOA -> Color(0xFFF28C8C); ShopItem.TURBO -> Color(0xFFFFB86B); ShopItem.TANK -> Color(0xFF8FC2AE); else -> Color(0xFFFFC85C) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(46.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(CircleShape)
                        .clickable { open = if (open == item) null else item },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Panel)
                        drawArc(ring, -90f, 360f * frac, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round), topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()))
                        drawCircle(Ink, style = Stroke(2.dp.toPx()))
                    }
                    ItemIcon(item, Modifier.size(28.dp))
                }
                Text(
                    fmtClock(left), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** A big friendly pop-up in the middle of the screen right after a booster is used. */
@Composable
private fun BoosterPopup(vm: GameViewModel, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    var shown by remember { mutableStateOf<ShopItem?>(null) }
    LaunchedEffect(vm.usedSerial) {
        if (vm.usedSerial == 0) return@LaunchedEffect
        shown = vm.lastUsed
        anim.snapTo(0f)
        anim.animateTo(1f, tween(1700))
        shown = null
    }
    val item = shown ?: return
    val p = anim.value
    val scale = if (p < 0.2f) p / 0.2f * 1.15f else if (p < 0.3f) 1.15f - (p - 0.2f) * 1.5f else 1f
    val alpha = if (p > 0.8f) (1f - p) / 0.2f else 1f
    Column(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha; translationY = -40f * p },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Panel)
                .border(3.dp, Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) { ItemIcon(item, Modifier.size(70.dp)) }
        Text(
            "${item.title}!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFE49A))
                .border(2.dp, Ink, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}


// ---------- cozy press feedback, survivors, ads, booster info ----------

/** Every clickable gets a soft press shade and the cozy click sound (taps on the game scene stay silent). */
private object CozyIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode =
        CozyPressNode(interactionSource)
    override fun hashCode(): Int = -77
    override fun equals(other: Any?): Boolean = other === this
}

private class CozyPressNode(
    private val source: androidx.compose.foundation.interaction.InteractionSource,
) : Modifier.Node(), androidx.compose.ui.node.DrawModifierNode {
    private var pressed = false
    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { i ->
                when (i) {
                    is androidx.compose.foundation.interaction.PressInteraction.Press -> { pressed = true; UiSound.click() }
                    is androidx.compose.foundation.interaction.PressInteraction.Release,
                    is androidx.compose.foundation.interaction.PressInteraction.Cancel -> pressed = false
                }
                invalidateDraw()
            }
        }
    }
    override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() {
        drawContent()
        if (pressed) drawRect(Color(0xFF6B4A3A).copy(alpha = 0.10f))
    }
}

@Composable
private fun SurvivorsCard(vm: GameViewModel, onInfo: () -> Unit) {
    val spec = SURVIVOR_SPEC
    val lvl = vm.level(UpgradeType.SURVIVORS)
    val maxed = lvl >= spec.maxLevel
    val cost = vm.upgradeCost(spec)
    val can = !maxed && vm.coins >= cost
    val sprites = rememberGameSprites()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFE9C7), Color(0xFFFFF4E0))))
            .border(2.5.dp, Ink, RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onInfo), contentAlignment = Alignment.Center) { UpgradeIcon(UpgradeType.SURVIVORS, Ink, Modifier.size(40.dp)) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).clickable(onClick = onInfo)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Survivors", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Text("$lvl/${spec.maxLevel}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ember)
                }
                Text("Rescue up to 10 travellers from the snow. Each one joins the crew, adds +40% coins and starts a story.", fontSize = 11.sp, color = Muted)
                if (lvl > 0) Text("Crew bonus now: +${lvl * 40}% coins", fontSize = 10.sp, color = GoldText, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier
                    .width(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (can) Color(0xFFFFB86B) else PanelHi)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(enabled = can) { vm.buy(spec) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (maxed) "FULL" else "Rescue", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (can) Ink else Muted)
                if (!maxed) Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(fmt(cost), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (can) Ink else Muted)
                }
            }
        }
        if (sprites.survivors.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            sprites.survivors.forEachIndexed { i, img ->
                Image(img, null, Modifier.size(24.dp, 30.dp).graphicsLayer { alpha = if (i < lvl) 1f else 0.25f })
            }
        }
    }
}

@Composable
private fun AdRow(vm: GameViewModel, r: AdReward) {
    val left = vm.adsLeft(r)
    val icon: @Composable () -> Unit = when (r) {
        AdReward.GEMS -> { { GemIcon(Modifier.size(26.dp)) } }
        AdReward.BOOSTER -> { { ItemIcon(ShopItem.TURBO, Modifier.size(32.dp)) } }
        AdReward.COINS -> { { CoinIcon(Modifier.size(24.dp)) } }
    }
    UpgradeRow(
        type = UpgradeType.BOOSTER,
        title = r.title,
        level = "$left/${r.perDay} today",
        effect = r.desc,
        note = when {
            left <= 0 -> "Come back tomorrow for more"
            !vm.adReady -> "Loading the ad..."
            r == AdReward.COINS && vm.adBoostLeft > 0.0 -> "Active · ${fmtClock(vm.adBoostLeft)} left"
            else -> null
        },
        buttonTop = "Watch",
        buttonBottom = "Ad",
        enabled = left > 0 && vm.adReady,
        onBuy = { vm.watchAd(r) },
        priceIcon = 3,
        icon = icon,
    )
}

@Composable
private fun BoosterInfo(item: ShopItem, left: Double, modifier: Modifier = Modifier, onStop: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(
        modifier
            .widthIn(max = 200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(2.dp, Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(item.desc, fontSize = 11.sp, color = Muted)
        Text("Time left: ${fmtClock(left)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldText)
        Spacer(Modifier.height(5.dp))
        ChunkyButton(true, listOf(Color(0xFFFFB9B0), Color(0xFFEF7B72)), { if (confirm) onStop() else confirm = true }, Modifier.fillMaxWidth()) {
            Text(if (confirm) "Tap again to stop" else "Stop booster", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        }
    }
}


// ---------- player level ----------

internal val XpPurple = Color(0xFF8E6FD8)

@Composable
private fun LevelBadge(vm: GameViewModel, onClick: () -> Unit) {
    val frac = (vm.xp.toFloat() / vm.xpNeeded).coerceIn(0f, 1f)
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFEDE4FF))
            drawArc(XpPurple, -90f, 360f * frac, false, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Ink, style = Stroke(1.6.dp.toPx()))
        }
        Avatar(vm, Modifier.size(28.dp))
    }
}

/** After evolving: which train you got, with a picture of it. */
@Composable
private fun NewTrainPopup(vm: GameViewModel) {
    if (!vm.newTrainPending) return
    var cutDone by remember(vm.evolveSerial) { mutableStateOf(false) }
    if (!cutDone) { EvolveCutscene((vm.tierIndex - 1).coerceAtLeast(0), vm.tierIndex) { cutDone = true }; return }
    val idx = vm.tierIndex
    val tier = TIERS[idx]
    CozyCard("New train!", Color(0xFFDCCBFA), onDismiss = vm::dismissNewTrain) {
        Text("Your train evolved into", fontSize = 12.sp, color = Muted)
        Text(tier.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(8.dp))
        TrainPicture(idx, Modifier.fillMaxWidth(0.62f).aspectRatio(0.62f))
        Spacer(Modifier.height(10.dp))
        Text(
            "Speed +${(Balance.TIER_SPEED_BONUS * idx * 100).toInt()}%  ·  Coins x${fmt(Balance.TIER_INCOME.pow(idx), 1)}\nUp to ${vm.maxCars} cars  ·  +${vm.cores} Frost Cores in total",
            fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        val perk = evolvePerk(vm.evolutions)
        Column(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(14.dp)).background(if (perk.milestone) Color(0xFFFFE49A) else Color(0xFFE2F3EA)).border(2.dp, Ink, RoundedCornerShape(14.dp)).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Unlocked: ${perk.title}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(perk.desc, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        BigButton("All aboard!", vm::dismissNewTrain, Modifier.fillMaxWidth(0.7f))
    }
}

/** A train tier drawn from above: its nose and two cars, in the tier's colours. */
@Composable
fun TrainPicture(tierIndex: Int, modifier: Modifier = Modifier) {
    val sp = rememberGameSprites()
    val tier = TIERS[tierIndex.coerceIn(0, TIERS.lastIndex)]
    Canvas(modifier) {
        // train space: 206 wide, nose (26 up) + 2 cars
        val sw = 206f
        val sh = 26f + 26f + 2 * 116f + 10f + 8f
        val k = minOf(size.width / sw, size.height / sh)
        val ox = (size.width - sw * k) / 2f
        val oy = (size.height - sh * k) / 2f
        val body = Color(tier.body)
        fun img(i: androidx.compose.ui.graphics.ImageBitmap?, x: Float, y: Float, w: Float, h: Float, tint: Color? = null) {
            if (i != null) drawSprite(i, ox + x * k, oy + y * k, w * k, h * k, tint = tint)
        }
        val t = tierIndex.coerceIn(0, TIERS.lastIndex)
        for (c in 0 until 2) {
            val top = 52f + c * 126f
            drawTierCoachPreview(t, ox, oy + top * k, k)
            img(sp.carFill, 0f, top - 8f, 206f, 132f, body)
            img(sp.carLine, 0f, top - 8f, 206f, 132f)
            img(sp.carTrims.getOrNull(t), 0f, top - 8f, 206f, 132f)
        }
        if (t == 0) {
            img(sp.noseFills.getOrNull(0), 2f, 46f - 26f + 0f, 202f, 44f, body)
            img(sp.noseLines.getOrNull(0), 2f, 46f - 26f + 0f, 202f, 44f)
        } else {
            img(sp.noseFills.getOrNull(t), 2f, 0f, 202f, 64f, body)
            img(sp.noseLines.getOrNull(t), 2f, 0f, 202f, 64f)
        }
    }
}

@Composable
private fun LevelUpPopup(vm: GameViewModel, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    var shownLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(vm.levelUpSerial) {
        if (vm.levelUpSerial == 0) return@LaunchedEffect
        shownLevel = vm.playerLevel
        anim.snapTo(0f)
        anim.animateTo(1f, tween(2200))
        shownLevel = 0
    }
    if (shownLevel == 0) return
    val p = anim.value
    val scale = if (p < 0.15f) p / 0.15f * 1.15f else if (p < 0.25f) 1.15f - (p - 0.15f) * 1.5f else 1f
    val alpha = if (p > 0.85f) (1f - p) / 0.15f else 1f
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(24.dp))
            .background(Panel)
            .border(3.dp, Ink, RoundedCornerShape(24.dp))
            .padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 10) {
                    val a = (i * PI / 5).toFloat() + p * 3f
                    drawLine(Color(0xFFFFC85C), center + Offset(cos(a), sin(a)) * size.minDimension * 0.36f, center + Offset(cos(a), sin(a)) * size.minDimension * 0.5f, 4.dp.toPx(), StrokeCap.Round)
                }
                drawCircle(Color(0xFFEDE4FF), size.minDimension * 0.33f)
                drawCircle(Ink, size.minDimension * 0.33f, style = Stroke(2.5.dp.toPx()))
            }
            Text("${shownLevel}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = XpPurple)
        }
        Text("Level up!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            GemIcon(Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("+${vm.levelUpGems} gems", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GemPink)
        }
    }
}

// ---------- side rails ----------

@Composable
internal fun RailButton(img: androidx.compose.ui.graphics.ImageBitmap?, label: String, bg: Color, badge: Int = 0, glow: Boolean = false, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "rail").animateFloat(
        1f, 1.08f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(650), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "railPulse",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(46.dp)
                .graphicsLayer { if (glow) { scaleX = pulse; scaleY = pulse } },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bg)
                    .border(2.5.dp, Ink, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) icon() else if (img != null) Image(img, label, Modifier.size(30.dp))
            }
            if (badge > 0) NotifyBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
        }
        Text(
            label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Panel.copy(alpha = 0.9f))
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun LeftRail(vm: GameViewModel, onQuests: () -> Unit, onAwards: () -> Unit, onWheel: () -> Unit, onGift: () -> Unit, controls: @Composable () -> Unit = {}, modifier: Modifier = Modifier) {
    val sp = rememberGameSprites()
    var open by rememberSaveable { mutableStateOf(false) }
    val pending = vm.questsReady + vm.storiesReady + vm.meta.passReady +
        (if (vm.meta.wheelFreeReady) 1 else 0) + (if (vm.dailyReady) 1 else 0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // a single toggle collapses the side icons so they do not cover the scene
        RailButton(null, if (open) "Hide" else "Menu", Color(0xFFEDE4F5), badge = if (open) 0 else pending, glow = !open && pending > 0, icon = { MenuIcon(open, Modifier.size(26.dp)) }, onClick = { UiSound.click(); open = !open })
        androidx.compose.animation.AnimatedVisibility(
            visible = open,
            enter = androidx.compose.animation.expandVertically(animationSpec = tween(260, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(tween(220)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut(tween(160)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val q = vm.questsReady + vm.storiesReady
                RailButton(sp.iconQuest, "Quests", Color(0xFFE2F3EA), badge = q, glow = q > 0, onClick = onQuests)
                if (vm.featureOpen(Feature.EVENTS)) RailButton(null, "Events", Color(currentEvent().color).copy(alpha = 0.6f).compositeOver(Color.White), badge = vm.meta.passReady, icon = { FlakeIcon(Modifier.size(28.dp)) }, onClick = onAwards)
                if (vm.featureOpen(Feature.WHEEL)) RailButton(null, "Wheel", Color(0xFFFFF0C8), badge = if (vm.meta.wheelFreeReady) 1 else 0, glow = vm.meta.wheelFreeReady, icon = { WheelIcon(Modifier.size(30.dp)) }, onClick = onWheel)
                RailButton(sp.iconGift, if (vm.dailyReady) "Ready!" else vm.nextGiftIn, Color(0xFFFFE1E1), badge = if (vm.dailyReady) 1 else 0, glow = vm.dailyReady, onClick = onGift)
            }
        }
        controls()
    }
}

/** The hamburger / close icon on the rail's collapse button. */
@Composable
private fun MenuIcon(open: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val col = Ink
        if (open) {
            drawLine(col, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), 3.2.dp.toPx(), StrokeCap.Round)
            drawLine(col, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), 3.2.dp.toPx(), StrokeCap.Round)
        } else {
            for (i in 0 until 3) {
                val y = h * (0.3f + i * 0.2f)
                drawLine(col, Offset(w * 0.22f, y), Offset(w * 0.78f, y), 3.2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun RightRail(vm: GameViewModel, link: SceneLink, onEvolve: () -> Unit, onJourney: () -> Unit) {
    val sp = rememberGameSprites()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // evolve progress ring
        val frac = (vm.runLevels.toFloat() / vm.evolveLevels).coerceIn(0f, 1f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEvolve),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFFF1EAFF))
                    drawArc(Color(0xFFB9A2EE), -90f, 360f * frac, false, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(Ink, style = Stroke(2.5.dp.toPx()))
                }
                if (sp.iconEvolve != null) Image(sp.iconEvolve, "Evolve", Modifier.size(26.dp))
            }
            Text(
                if (vm.canEvolve) "Evolve!" else "${(frac * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).background(Panel.copy(alpha = 0.9f)).padding(horizontal = 4.dp),
            )
        }
        if (vm.featureOpen(Feature.JOURNEY)) RailButton(sp.iconTrophy, "Journey", Color(0xFFCDE7F8), onClick = onJourney)
        ActiveBoosters(vm)
        // mini-map of the train: tap a car to fly the camera to it, tap MAP for the full train map.
        // Every car gets the same height, shrunk so the whole train fits above the footer.
        val cars = vm.carsNow
        var showMap by remember { mutableStateOf(false) }
        androidx.compose.foundation.layout.BoxWithConstraints {
        val room = if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) 400.dp else maxHeight
        val cell = ((room - 36.dp) / cars).coerceIn(15.dp, 20.dp)
        val numSize = 9.sp
        val listH = (room - 36.dp).coerceAtLeast(cell * 3).coerceAtMost(cell * cars)
        val mapScroll = androidx.compose.foundation.rememberScrollState()
        Column(
            Modifier
                .width(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Panel.copy(alpha = 0.94f))
                .border(2.dp, Ink, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MAP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFCDE7F8))
                    .border(1.5.dp, Ink, RoundedCornerShape(7.dp))
                    .clickable { showMap = true }
                    .padding(vertical = 3.dp),
            )
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(32.dp).height(listH).verticalScroll(mapScroll)) {
            Box(Modifier.width(32.dp).height(cell * cars)) {
                Column {
                    for (i in 0 until cars) {
                        val sel = link.flashCar == i
                        Box(
                            Modifier
                                .padding(vertical = 1.dp)
                                .size(32.dp, cell - 2.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (sel) Color(0xFFFFE49A) else if (i == 0) Color(0xFFF4A7A0) else Color(0xFFFFD9C2))
                                .border(1.2.dp, Ink, RoundedCornerShape(5.dp))
                                .clickable { link.jumpCar = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", fontSize = numSize, lineHeight = numSize, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, softWrap = false)
                        }
                    }
                }
                // what is on screen right now
                val top = (link.camCar.coerceIn(-0.5f, cars.toFloat()))
                val span = link.viewCars.coerceAtMost(cars.toFloat() + 1f)
                Box(
                    Modifier
                        .padding(top = (cell * top.coerceAtLeast(0f)))
                        .size(32.dp, (cell * (span + kotlin.math.min(0f, top))).coerceAtLeast(6.dp).coerceAtMost(cell * cars))
                        .border(2.dp, Ice, RoundedCornerShape(6.dp)),
                )
            }
            }
        }
        }
        if (showMap) TrainMapCard(vm, onPick = { link.jumpCar = it; showMap = false }, onClose = { showMap = false })
    }
}

internal val FOOTER_H = 104.dp
const val ZOOM_MIN = 0.6f
const val ZOOM_MAX = 2.2f

/** Hide/show the UI and zoom the camera in or out (pinch works too). */
@Composable
private fun SceneControls(hidden: Boolean, zoom: Float, onToggleHide: () -> Unit, onZoom: (Float) -> Unit, modifier: Modifier = Modifier, horizontal: Boolean = false, windowOpen: Boolean = false, onWindow: (() -> Unit)? = null) {
    val box = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Panel.copy(alpha = 0.8f))
        .border(2.dp, Ink, RoundedCornerShape(16.dp))
        .padding(4.dp)
    if (horizontal) Row(box, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { SceneControlButtons(hidden, zoom, onToggleHide, onZoom, windowOpen, onWindow) }
    else Column(box, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { SceneControlButtons(hidden, zoom, onToggleHide, onZoom, windowOpen, onWindow) }
}

@Composable
private fun SceneControlButtons(hidden: Boolean, zoom: Float, onToggleHide: () -> Unit, onZoom: (Float) -> Unit, windowOpen: Boolean = false, onWindow: (() -> Unit)? = null) {
    run {
        if (onWindow != null) ControlButton(onWindow) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width; val h = size.height
                // a little window: rounded pane with a sill, or an eye-out arrow when open
                drawRoundRect(if (windowOpen) Ember else Color.Transparent, topLeft = Offset(w * 0.12f, h * 0.1f), size = Size(w * 0.76f, h * 0.7f), cornerRadius = CornerRadius(w * 0.18f, w * 0.18f), alpha = if (windowOpen) 0.18f else 1f)
                drawRoundRect(Ink, topLeft = Offset(w * 0.12f, h * 0.1f), size = Size(w * 0.76f, h * 0.7f), cornerRadius = CornerRadius(w * 0.18f, w * 0.18f), style = Stroke(1.8.dp.toPx()))
                drawLine(Ink, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.8f), 1.4.dp.toPx())
                drawLine(Ink, Offset(w * 0.12f, h * 0.45f), Offset(w * 0.88f, h * 0.45f), 1.4.dp.toPx())
                drawLine(Ink, Offset(w * 0.06f, h * 0.9f), Offset(w * 0.94f, h * 0.9f), 2.2.dp.toPx(), StrokeCap.Round)
            }
        }
        ControlButton(onToggleHide) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val eye = Path().apply {
                    moveTo(w * 0.05f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.02f, w * 0.95f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.05f, h * 0.5f)
                    close()
                }
                drawPath(eye, Ink, style = Stroke(1.8.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawCircle(Ink, w * 0.14f, Offset(w * 0.5f, h * 0.5f))
                if (!hidden) drawLine(Ember, Offset(w * 0.12f, h * 0.88f), Offset(w * 0.88f, h * 0.12f), 2.4.dp.toPx(), StrokeCap.Round)
            }
        }
        ControlButton({ onZoom(1.2f) }, enabled = zoom < ZOOM_MAX - 0.01f) { Text("+", fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
        ControlButton({ onZoom(1f / 1.2f) }, enabled = zoom > ZOOM_MIN + 0.01f) { Text("−", fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
    }
}

@Composable
private fun ControlButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White.copy(alpha = 0.85f) else PanelHi.copy(alpha = 0.6f))
            .border(1.5.dp, Ink.copy(alpha = if (enabled) 1f else 0.4f), CircleShape)
            .clickable(enabled = enabled) { UiSound.click(); onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The whole train at a glance: every car with its devices; tap a car to look at it. */
@Composable
private fun TrainMapCard(vm: GameViewModel, onPick: (Int) -> Unit, onClose: () -> Unit) {
    val sp = rememberGameSprites()
    val cars = vm.carsNow
    CozyCard("Train map", Color(0xFFCDE7F8), onDismiss = onClose, closable = true) {
        Text("Tap a car to go there", fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in 0 until cars) {
                val devices = CAR_LAYOUT.getOrNull(i) ?: emptyList()
                val installed = devices.count { vm.level(it) > 0 }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (i == 0) Color(0xFFFDE2DF) else PanelHi)
                        .border(2.dp, Ink, RoundedCornerShape(14.dp))
                        .clickable { onPick(i) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFFE49A)).border(1.5.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (i) { 0 -> "Engine car"; 1 -> "Utility car"; else -> "Car ${i + 1}" },
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                        )
                        Text("$installed / ${devices.size} devices", fontSize = 10.sp, color = Muted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        devices.forEach { t ->
                            val img = sp.device[t]
                            val on = vm.level(t) > 0
                            Box(Modifier.size(22.dp).graphicsLayer { alpha = if (on) 1f else 0.25f }) {
                                if (img != null) Image(img, null, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }

    }
}

// ---------- daily quests ----------

@Composable
private fun QuestSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    val sp = rememberGameSprites()
    SheetFrame("Quests", sp.iconQuest, Color(0xFFCDEBDC), onClose, modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                // player level card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1EAFF))
                        .border(2.dp, Ink, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Level ${vm.playerLevel}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.85f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel)
                            .border(1.5.dp, Ink, RoundedCornerShape(6.dp)),
                    ) { Box(Modifier.fillMaxHeight().fillMaxWidth((vm.xp.toFloat() / vm.xpNeeded).coerceIn(0f, 1f)).background(XpPurple)) }
                    Text("${vm.xp} / ${vm.xpNeeded} XP · next level: +${vm.levelGems(vm.playerLevel + 1)} gems", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
                    Text("Earn XP from awards, quests, evolving and the daily gift.", fontSize = 10.sp, color = Muted)
                }
            }
            val quests = vm.todaysQuests
            val openDaily = quests.indices.filter { !vm.questClaimed[it] }
            val doneDaily = quests.indices.filter { vm.questClaimed[it] }
            // journey quests: ready ones first, then the closest to done
            val openJourney = JOURNEY_QUESTS.filter { it.id !in vm.journeyClaimed }
                .sortedByDescending { vm.journeyProgress(it).toFloat() / it.goal }
            val doneJourney = JOURNEY_QUESTS.filter { it.id in vm.journeyClaimed }
            if (vm.claimableCount > 1) item {
                ChunkyButton(true, listOf(Color(0xFFFFC98A), Color(0xFFFFA65C)), vm::claimAll, Modifier.fillMaxWidth()) {
                    Text("Claim all (${vm.claimableCount})", fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
            item {
                QuestHeader("Survivor stories", "${vm.storiesReady} ready")
                Spacer(Modifier.height(6.dp))
                StoryRows(vm)
            }
            item { QuestHeader("Daily quests", "New in ${vm.nextGiftIn}") }
            items(openDaily, key = { "d$it" }) { i ->
                val q = quests[i]
                QuestRow(q, vm.questProgress(q), claimed = false, onClaim = { vm.claimQuest(i) })
            }
            if (openDaily.isEmpty()) item { Text("All daily quests done. Come back tomorrow!", fontSize = 11.sp, color = Muted) }
            item { QuestHeader("Journey", "${doneJourney.size} / ${JOURNEY_QUESTS.size} done") }
            items(openJourney, key = { it.id }) { q ->
                QuestRow(q, vm.journeyProgress(q), claimed = false, onClaim = { vm.claimJourney(q) })
            }
            if (doneDaily.isNotEmpty() || doneJourney.isNotEmpty()) {
                item { QuestHeader("Completed", "") }
                items(doneDaily, key = { "dd$it" }) { i -> QuestRow(quests[i], quests[i].goal, claimed = true, onClaim = {}) }
                items(doneJourney, key = { "jd" + it.id }) { q -> QuestRow(q, q.goal, claimed = true, onClaim = {}) }
            }
        }
    }
}

@Composable
private fun QuestHeader(title: String, right: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
        if (right.isNotEmpty()) Text(right, fontSize = 11.sp, color = Muted)
    }
}

private fun questNumber(q: Quest, v: Int): String =
    if (q.stat == QuestStat.DISTANCE) fmtDistance(v.toDouble()) else "$v"

@Composable
private fun QuestRow(q: Quest, prog: Int, claimed: Boolean, onClaim: () -> Unit) {
    val done = prog >= q.goal
    val reward = (if (q.xp > 0) "+${q.xp} XP  " else "") + "+${q.gems} gems"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (claimed) PanelHi.copy(alpha = 0.6f) else if (done) Color(0xFFFFF3D6) else Color.White.copy(alpha = 0.6f))
            .border(2.dp, Ink.copy(alpha = if (done && !claimed) 1f else 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(q.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (claimed) Muted else Ink)
            if (!claimed) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(PanelHi)
                        .border(1.2.dp, Ink, RoundedCornerShape(5.dp)),
                ) { Box(Modifier.fillMaxHeight().fillMaxWidth((prog.toFloat() / q.goal).coerceIn(0f, 1f)).background(Mint)) }
                Text("${questNumber(q, prog)} / ${questNumber(q, q.goal)}   ·   $reward", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
            } else {
                Text(reward, fontSize = 10.sp, color = Muted)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (claimed) {
            Text(
                "Completed", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E9B63),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFF3E9B63), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        } else {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (done) Color(0xFFFFB86B) else PanelHi)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(enabled = done) { onClaim() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "Done!" else "Claim", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Ink else Muted)
            }
        }
    }
}

// ---------- tutorial ----------

/** A centred cozy card with the conductor, used for the welcome and the "all set" screens. */
@Composable
private fun TutorialCard(title: String, step: Int, text: String, button: String, onButton: () -> Unit, onSkip: (() -> Unit)?) {
    val sp = rememberGameSprites()
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { t = (it / 1_000_000L) / 1000f } }
    val enter = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f)) }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxWidth(0.88f).widthIn(max = 380.dp).graphicsLayer { scaleX = enter.value; scaleY = enter.value }) {
            Column(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Panel)
                    .border(3.dp, Ink, RoundedCornerShape(28.dp))
                    .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TutorialSteps(step)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = center
                        val r = size.minDimension / 2f
                        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0D2), Color(0xFFCDE7F8)), c, r), r, c)
                        for (i in 0 until 12) {
                            val a = (i * PI / 6).toFloat() + t * 0.25f
                            val ray = Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + cos(a - 0.11f) * r, c.y + sin(a - 0.11f) * r)
                                lineTo(c.x + cos(a + 0.11f) * r, c.y + sin(a + 0.11f) * r)
                                close()
                            }
                            drawPath(ray, Color.White.copy(alpha = 0.3f))
                        }
                        drawCircle(Ink, r - 1.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                    }
                    val hop = kotlin.math.abs(sin(t * 3f)) * 6f
                    sp.npc?.let { Image(it, "Conductor", Modifier.size(84.dp, 105.dp).graphicsLayer { translationY = -hop }) }
                }
                Spacer(Modifier.height(10.dp))
                Text(text, fontSize = 15.sp, fontFamily = CozyFont, color = Ink, textAlign = TextAlign.Center, lineHeight = 22.sp)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFC98A), Color(0xFFFFA65C))))
                        .border(2.5.dp, Ink, RoundedCornerShape(22.dp))
                        .clickable(onClick = onButton)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(button, fontSize = 18.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink) }
                if (onSkip != null) {
                    Text(
                        "Skip tutorial", fontSize = 12.sp, fontFamily = CozyFont, color = Muted, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onSkip).padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                title, fontSize = 21.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFE49A))
                    .border(3.dp, Ink, RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun TutorialSteps(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..4) {
            Box(
                Modifier
                    .size(if (i == step) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (i <= step) Color(0xFFFFB86B) else PanelHi)
                    .border(1.5.dp, Ink, CircleShape),
            )
        }
    }
}

/** During the tutorial: the conductor with a speech bubble above the footer, and a bouncing arrow at the button to press. */
@Composable
private fun TutorialGuide(vm: GameViewModel, sheetOpen: Boolean, modifier: Modifier = Modifier) {
    val sp = rememberGameSprites()
    val stage = vm.tutorialStage
    val (title, text) = when {
        stage == 1 -> "Your upgrades" to "Tap the gear button down here to open your upgrades."
        stage == 2 && !sheetOpen -> "Buy Engine Power" to "Open the upgrades with the gear and buy Engine Power."
        stage == 2 -> "Buy Engine Power" to "Wait for 30 coins, then tap the glowing Engine Power button."
        else -> "Boost!" to "Tap BOOST a few times. Each tap fills a few seconds of extra income."
    }
    val arrowRight = stage == 3
    val showArrow = !(stage == 2 && sheetOpen)
    val bob by androidx.compose.animation.core.rememberInfiniteTransition(label = "guide").animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "guideBob",
    )
    Box(modifier) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 30.dp)
                .widthIn(max = 360.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCDE7F8))
                    .border(2.5.dp, Ink, CircleShape),
                contentAlignment = Alignment.BottomCenter,
            ) { sp.npc?.let { Image(it, null, Modifier.size(44.dp, 55.dp).graphicsLayer { translationY = 6f - bob * 4f }) } }
            Spacer(Modifier.width(6.dp))
            Column(
                Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                    .background(Panel)
                    .border(2.5.dp, Ink, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    TutorialSteps(stage)
                }
                Text(text, fontSize = 14.sp, fontFamily = CozyFont, color = Ink, lineHeight = 19.sp)
            }
        }
        if (showArrow) {
            Canvas(
                Modifier
                    .align(if (arrowRight) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(start = 20.dp, end = 52.dp)
                    .size(34.dp, 34.dp)
                    .graphicsLayer { translationY = bob * 10.dp.toPx() - 2.dp.toPx() },
            ) {
                val w = size.width
                val h = size.height
                val arrow = Path().apply {
                    moveTo(w * 0.3f, 0f); lineTo(w * 0.7f, 0f); lineTo(w * 0.7f, h * 0.5f); lineTo(w, h * 0.5f)
                    lineTo(w * 0.5f, h); lineTo(0f, h * 0.5f); lineTo(w * 0.3f, h * 0.5f); close()
                }
                drawPath(arrow, Color(0xFFFFB86B))
                drawPath(arrow, Ink, style = Stroke(2.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}


// ---------- opening screen ----------

/** Shows the loading screen while the art is decoded, then fades into the game. */
@Composable
fun FrostRoot(vm: GameViewModel) {
    var ready by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val load = async(kotlinx.coroutines.Dispatchers.Default) { SpriteBank.get(ctx) }
        progress.animateTo(0.85f, tween(1400, easing = FastOutSlowInEasing))
        load.await()
        progress.animateTo(1f, tween(250))
        kotlinx.coroutines.delay(200)
        ready = true
    }
    androidx.compose.animation.Crossfade(ready, animationSpec = tween(450), label = "opening") { done ->
        if (done) GameScreen(vm) else LoadingScreen(progress.value)
    }
}

@Composable
fun LoadingScreen(progress: Float) {
    val tips = listOf(
        "Warming up the engine...",
        "Shovelling coal into the boiler...",
        "Brewing hot cocoa for the crew...",
        "Checking the snow chains...",
        "All aboard!",
    )
    val tip = tips[(progress * (tips.size - 1)).toInt().coerceIn(0, tips.size - 1)]
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F9FD))) {
        Image(
            androidx.compose.ui.res.painterResource(R.drawable.splash_art), "Little Express",
            Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        // the game's name, on a cream signboard near the top of the loading art
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF2FFF8EA))
                    .border(4.dp, Ink, RoundedCornerShape(26.dp))
                    .padding(horizontal = 26.dp, vertical = 10.dp),
            ) {
                Text("Little Express", fontSize = 38.sp, fontFamily = CozyFont, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2F6B4F))
            }
            Text("An idle train adventure", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 8.dp))
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(tip, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Panel)
                    .border(3.dp, Ink, RoundedCornerShape(13.dp))
                    .padding(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFC98A), Color(0xFFFFA65C)))),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(x, size.height), Offset(x + size.height, 0f), size.height * 0.35f)
                            x += size.height * 1.1f
                        }
                        drawRect(Color.White.copy(alpha = 0.35f), size = Size(size.width, size.height * 0.3f))
                    }
                }
            }
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ---------- cozy cards: daily gift and welcome back ----------

@Composable
internal fun CozyCard(title: String, ribbon: Color, onDismiss: () -> Unit, closable: Boolean = false, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val enter = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.9f).widthIn(max = 390.dp).graphicsLayer { scaleX = enter.value; scaleY = enter.value }) {
            Column(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Panel)
                    .border(3.dp, Ink, RoundedCornerShape(28.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            Text(
                title, fontSize = 19.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ribbon)
                    .border(3.dp, Ink, RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 6.dp),
            )
            if (closable) CloseButton(onDismiss, Modifier.align(Alignment.TopEnd).padding(top = 6.dp))
        }
    }
}

@Composable
internal fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, colors: List<Color> = listOf(Color(0xFFFFC98A), Color(0xFFFFA65C))) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(colors))
            .border(2.5.dp, Ink, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 16.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center) }
}

@Composable
private fun DailyGiftCard(vm: GameViewModel, onClose: (() -> Unit)? = null) {
    val ready = vm.dailyReady
    val next = if (ready) vm.dailyNextStreak else vm.dailyStreak + 1
    val weekStart = ((next - 1) / 7) * 7 + 1
    val sp = rememberGameSprites()
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "daily").animateFloat(
        1f, 1.07f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "dailyPulse",
    )
    CozyCard("Daily Gift", Color(0xFFFFC9D6), onDismiss = onClose ?: {}, closable = onClose != null) {
        Text(if (ready) "Day $next in a row" else "${vm.dailyStreak} days in a row", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
        Spacer(Modifier.height(6.dp))
        SpecialCard(vm)
        Spacer(Modifier.height(8.dp))
        // a week of gifts: claimed days are ticked, today glows, the 7th day has a Frost Core
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                val days = if (row == 0) (0..3) else (4..6)
                for (k in days) {
                    val day = weekStart + k
                    val claimed = day < next
                    val today = day == next && ready
                    val big = k == 6
                    Column(
                        Modifier
                            .weight(if (big) 2f else 1f)
                            .graphicsLayer { if (today) { scaleX = pulse; scaleY = pulse } }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (today) Color(0xFFFFE49A) else if (claimed) PanelHi else Color.White.copy(alpha = 0.7f))
                            .border(if (today) 3.dp else 2.dp, Ink.copy(alpha = if (claimed) 0.4f else 1f), RoundedCornerShape(14.dp))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Day $day", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (claimed) Muted else Ink, maxLines = 1, softWrap = false)
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            if (big) Row { CoreIcon(Ice, Modifier.size(22.dp)); GemIcon(Modifier.size(22.dp)) }
                            else if (k % 2 == 1) GemIcon(Modifier.size(22.dp)) else CoinIcon(Modifier.size(24.dp))
                            if (claimed) {
                                Canvas(Modifier.size(22.dp)) {
                                    drawCircle(Mint)
                                    drawCircle(Ink, style = Stroke(1.5.dp.toPx()))
                                    val p = Path().apply { moveTo(size.width * 0.28f, size.height * 0.52f); lineTo(size.width * 0.45f, size.height * 0.7f); lineTo(size.width * 0.74f, size.height * 0.34f) }
                                    drawPath(p, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                }
                            }
                        }
                        Text(
                            if (big) "+${vm.dailyGems(day)} gems, core" else if (k % 2 == 1) "+${vm.dailyGems(day)}" else "coins",
                            fontSize = 9.sp, color = Muted, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (ready) "Today" else "Tomorrow", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CoinIcon(Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
            Text(fmt(vm.dailyCoins(next)), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GoldText)
            Spacer(Modifier.width(12.dp))
            GemIcon(Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
            Text("${vm.dailyGems(next)}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GemPink)
            if (vm.dailyGivesCore(next)) {
                Spacer(Modifier.width(12.dp)); CoreIcon(Ice, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                Text("1", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ice)
            }
        }
        Text("+20 XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpPurple)
        Spacer(Modifier.height(12.dp))
        if (ready) {
            BigButton("Claim my gift", vm::claimDaily, Modifier.fillMaxWidth(0.8f))
            Text("Come back tomorrow to keep your streak going.", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
        } else {
            BigButton("Next gift in ${vm.nextGiftIn}", onClose ?: {}, Modifier.fillMaxWidth(0.8f), listOf(Color(0xFFE6EEF5), Color(0xFFC9D8E6)))
            Text("Today's gift is already in your pocket.", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun fmtHours(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** Back pressed on the main screen: stay or leave. */
@Composable
private fun QuitCard(vm: GameViewModel, onStay: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val sp = rememberGameSprites()
    CozyCard("Leave the train?", Color(0xFFCDE7F8), onDismiss = onStay, closable = true) {
        sp.npcSleep?.let { Image(it, null, Modifier.size(64.dp, 80.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(
            "Your train keeps rolling while you are away and earns coins for up to ${fmtHours(vm.offlineCapSec)}.",
            fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        BigButton("Keep playing", onStay, Modifier.fillMaxWidth(0.8f))
        Spacer(Modifier.height(8.dp))
        BigButton("Quit game", { vm.save(); activity?.finish() }, Modifier.fillMaxWidth(0.8f), listOf(Color(0xFFFFB9B0), Color(0xFFEF7B72)))
    }
}

@Composable
private fun WelcomeBackCard(vm: GameViewModel) {
    val sp = rememberGameSprites()
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { t = (it / 1_000_000L) / 1000f } }
    CozyCard("Welcome back!", Color(0xFFCDE7F8), onDismiss = vm::dismissOffline) {
        Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0D2), Color(0xFFDCCBFA)), center, r), r)
                for (i in 0 until 3) {
                    val ph = (t * 0.4f + i / 3f) % 1f
                    val c = center + Offset(r * 0.25f + ph * r * 0.3f, -r * 0.2f - ph * r * 0.45f)
                    drawCircle(Ink.copy(alpha = 1f - ph), (5f + i * 2f) * density * 0.6f, c, style = Stroke(1.6.dp.toPx()))
                }
                drawCircle(Ink, r - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
            }
            sp.npcSleep?.let { Image(it, null, Modifier.size(72.dp, 90.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text("While you were away, your train kept rolling", fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(fmtDistance(vm.offlineMeters), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ice)
        Text(
            "Away ${fmtHours(vm.offlineAwaySec)}" + (if (vm.offlineAwaySec > vm.offlineCountedSec) " · earnings count up to ${fmtHours(vm.offlineCapSec)}" else ""),
            fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(Modifier.size(26.dp)); Spacer(Modifier.width(6.dp))
            Text("+${fmt(vm.offlineCoins)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GoldText)
        }
        Spacer(Modifier.height(10.dp))
        ParcelBox(vm)
        Spacer(Modifier.height(10.dp))
        if (Balance.ADS_ENABLED) {
            BigButton(if (vm.adReady) "Watch an ad: double it!" else "Loading ad...", vm::doubleOfflineWithAd, Modifier.fillMaxWidth(0.85f), listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)))
            Spacer(Modifier.height(8.dp))
        }
        BigButton("Collect", vm::dismissOffline, Modifier.fillMaxWidth(0.85f))
    }
}

// ---------- cozy frozen UI kit: close button, sheet frame, snow caps, chunky buttons ----------

/** The round close button used everywhere: a soft red button with a chunky shadow and a white cross. */
@Composable
internal fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 34.dp) {
    var pressed by remember { mutableStateOf(false) }
    Box(modifier.size(size + 3.dp)) {
        Box(Modifier.size(size).offset(y = 3.dp).background(Ink, CircleShape))
        Box(
            Modifier
                .size(size)
                .offset(y = if (pressed) 2.dp else 0.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFB9B0), Color(0xFFEF7B72))))
                .border(2.5.dp, Ink, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; UiSound.click(); val ok = tryAwaitRelease(); pressed = false; if (ok) onClick() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(size * 0.38f)) {
                val w = this.size.width
                drawLine(Ink, Offset.Zero, Offset(w, w), 4.6.dp.toPx(), StrokeCap.Round)
                drawLine(Ink, Offset(w, 0f), Offset(0f, w), 4.6.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset.Zero, Offset(w, w), 2.4.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(w, 0f), Offset(0f, w), 2.4.dp.toPx(), StrokeCap.Round)
            }
            // a little shine on top
            Canvas(Modifier.matchParentSize()) {
                drawArc(Color.White.copy(alpha = 0.55f), 200f, 70f, false, Offset(this.size.width * 0.18f, this.size.height * 0.16f),
                    Size(this.size.width * 0.64f, this.size.height * 0.64f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

/** A round back button (arrow), same style as [CloseButton] but blue. */
@Composable
internal fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(37.dp)) {
        Box(Modifier.size(34.dp).offset(y = 3.dp).background(Ink, CircleShape))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFD3E9FA), Color(0xFF9FC5EA))))
                .border(2.5.dp, Ink, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) {
                val w = size.width
                val p = Path().apply { moveTo(w * 0.62f, 0f); lineTo(w * 0.18f, w * 0.5f); lineTo(w * 0.62f, w) }
                drawPath(p, Ink, style = Stroke(4.4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawPath(p, Color.White, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

/** Soft snow piled along the top edge of a panel. */
@Composable
private fun SnowCap(modifier: Modifier = Modifier, seed: Int = 0) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val w = size.width
        val h = size.height
        val bumps = (w / 34.dp.toPx()).toInt().coerceAtLeast(4)
        val p = Path().apply {
            moveTo(0f, 0f); lineTo(0f, h * 0.45f)
            for (i in 0 until bumps) {
                val x0 = w * i / bumps
                val x1 = w * (i + 1) / bumps
                val dip = h * (0.45f + 0.35f * (((i * 7 + seed * 3) % 5) / 4f))
                quadraticTo((x0 + x1) / 2f, dip + h * 0.45f, x1, h * 0.45f)
            }
            lineTo(w, 0f); close()
        }
        drawPath(p, Color.White)
        val edge = Path().apply {
            moveTo(0f, h * 0.45f)
            for (i in 0 until bumps) {
                val x0 = w * i / bumps
                val x1 = w * (i + 1) / bumps
                val dip = h * (0.45f + 0.35f * (((i * 7 + seed * 3) % 5) / 4f))
                quadraticTo((x0 + x1) / 2f, dip + h * 0.45f, x1, h * 0.45f)
            }
        }
        drawPath(edge, Color(0xFFBFD9EC), style = Stroke(1.6.dp.toPx()))
    }
}

/** Little icicles hanging under a panel. */
@Composable
private fun Icicles(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(9.dp)) {
        val w = size.width
        val xs = listOf(0.07f, 0.11f, 0.24f, 0.31f, 0.47f, 0.62f, 0.66f, 0.81f, 0.9f, 0.94f)
        xs.forEachIndexed { i, fx ->
            val x = w * fx
            val l = size.height * (0.55f + 0.45f * ((i * 5 % 3) / 2f))
            val hw = 2.6.dp.toPx()
            val p = Path().apply { moveTo(x - hw, 0f); lineTo(x + hw, 0f); lineTo(x, l); close() }
            drawPath(p, Color(0xFFDDF3FF))
            drawPath(p, Color(0xFF8EC3E3), style = Stroke(1.dp.toPx()))
        }
    }
}

/** A currency counter in a small recessed pill. */
@Composable
internal fun CurrencyPill(icon: @Composable () -> Unit, text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, plus: Boolean = false) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .border(1.8.dp, Ink, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 6.dp, end = if (plus) 3.dp else 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, softWrap = false)
        if (plus) {
            Spacer(Modifier.width(5.dp))
            Box(Modifier.size(18.dp).clip(CircleShape).background(color).border(1.5.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("+", fontSize = 13.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

/**
 * Every bottom sheet uses this frame: a frosty header with snow on top, the title on a ribbon,
 * optional back button, counters on the right and the round close button.
 */
@Composable
internal fun SheetFrame(
    title: String,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    tint: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    right: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel)
            .border(OutlineW, Wood, shape)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.55f).compositeOver(Panel)))),
        ) {
            SnowCap(Modifier.align(Alignment.TopCenter), seed = title.length)
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) { BackButton(onBack); Spacer(Modifier.width(6.dp)) }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)).border(2.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (icon != null) Image(icon, null, Modifier.size(26.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 19.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                right()
                Spacer(Modifier.width(6.dp))
                CloseButton(onClose)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink.copy(alpha = 0.8f)))
        content()
    }
}

/** A chunky 3D button: a dark base under a coloured face that sinks when pressed. Hold to repeat when [repeat]. */
@Composable
internal fun ChunkyButton(
    enabled: Boolean,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    repeat: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val action = androidx.compose.runtime.rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.padding(bottom = 3.dp)) {
        Box(Modifier.matchParentSize().offset(y = 3.dp).clip(shape).background(if (enabled) Ink else Ink.copy(alpha = 0.35f)))
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = if (pressed) 3.dp else 0.dp)
                .clip(shape)
                .background(if (enabled) Brush.verticalGradient(colors) else Brush.verticalGradient(listOf(PanelHi, PanelHi)))
                .border(2.dp, if (enabled) Ink else Ink.copy(alpha = 0.4f), shape)
                .pointerInput(enabled, repeat) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        UiSound.click()
                        action.value()
                        if (repeat) {
                            var wait = 450L
                            while (true) {
                                val released = withTimeoutOrNull(wait) { waitForUpOrCancellation(); true } ?: false
                                if (released) break
                                action.value()
                                wait = (wait * 0.82f).toLong().coerceAtLeast(90L)
                            }
                        } else {
                            waitForUpOrCancellation()
                        }
                        pressed = false
                    }
                }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** A car's header in the upgrades list: tap to fold or open it. */
@Composable
private fun CarHeader(text: String, color: Color, note: String, folded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { UiSound.click(); onToggle() }.padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(note, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(if (folded) "▸" else "▾", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
private fun SmallChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (on) Brush.verticalGradient(listOf(Color(0xFFFFE49A), Color(0xFFFFC85C))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f))))
            .border(2.dp, if (on) Ink else Ink.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
            .clickable { UiSound.click(); onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted, maxLines = 1, softWrap = false)
    }
}

/** A section title inside a sheet: a small ribbon with a line. */
@Composable
internal fun SectionTitle(text: String, color: Color = Ink, note: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        if (note != null) Text(note, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        else Box(Modifier.weight(1f).height(2.dp).background(color.copy(alpha = 0.35f)))
    }
}


// ---------- evolve menu ----------

/** Everything about evolving: this train and the next, progress, what you get, what resets, and every train tier. */
@Composable
private fun EvolveSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    val sp = rememberGameSprites()
    val cur = vm.tierIndex
    val top = cur >= TIERS.lastIndex
    val next = min(cur + 1, TIERS.lastIndex)
    val progress = (vm.runLevels.toFloat() / vm.evolveLevels).coerceIn(0f, 1f)
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(confirm) { if (confirm) { kotlinx.coroutines.delay(3500); confirm = false } }
    SheetFrame(
        "Evolve", sp.iconEvolve, Color(0xFFE3D6FB), onClose, modifier,
        right = { CurrencyPill({ CoreIcon(Ice, Modifier.size(15.dp)) }, "${vm.cores}", Ice) },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // this train -> the next one
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFF1EAFF), Color(0xFFDCEEF9))))
                        .border(2.dp, Ink, RoundedCornerShape(20.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOW", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                        TrainPicture(cur, Modifier.fillMaxWidth().height(120.dp))
                        Text(TIERS[cur].name, fontSize = 13.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                    }
                    Canvas(Modifier.size(28.dp)) {
                        val w = size.width
                        val p = Path().apply { moveTo(w * 0.2f, w * 0.2f); lineTo(w * 0.7f, w * 0.5f); lineTo(w * 0.2f, w * 0.8f) }
                        drawPath(p, Ink, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        drawPath(p, Color(0xFFB9A2EE), style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (top) "TOP TRAIN" else "NEXT", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold, color = XpPurple)
                        TrainPicture(next, Modifier.fillMaxWidth().height(120.dp).graphicsLayer { alpha = if (vm.canEvolve || top) 1f else 0.85f })
                        Text(TIERS[next].name, fontSize = 13.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                    }
                }
            }
            // what the next train unlocks, and this run's route bonus
            item {
                val perk = evolvePerk(vm.evolutions + 1)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (perk.milestone) Color(0xFFFFE49A) else Color(0xFFE2F3EA)).border(2.dp, Ink, RoundedCornerShape(16.dp)).padding(10.dp),
                ) {
                    Text("Next train unlocks: ${perk.title}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(perk.desc, fontSize = 11.sp, color = Muted)
                    vm.routeBonus?.let { r ->
                        Spacer(Modifier.height(4.dp))
                        Text("This run's route: ${routeEmoji(r)} ${r.title} (${r.desc})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ice)
                    }
                }
            }
            // progress
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp)).background(Track).border(2.dp, Ink, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress).background(Brush.horizontalGradient(listOf(Color(0xFFB8D8F2), Color(0xFFB9A2EE)))))
                        Text("${vm.runLevels} / ${vm.evolveLevels} upgrade levels", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                    Text(
                        if (vm.canEvolve) "Ready! Evolve whenever you like." else "Buy ${vm.evolveLevels - vm.runLevels} more upgrade levels to evolve.",
                        fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            // rewards
            item {
                SectionTitle("You get", XpPurple)
                val carsNext = Balance.maxCars(vm.evolutions + 1)
                val rewards = listOf(
                    "+${vm.coreGain} Frost Core${if (vm.coreGain > 1) "s" else ""}" to "+${(Balance.CORE_BONUS * 100).toInt()}% coins each, forever",
                    "Speed +${(Balance.TIER_SPEED_BONUS * 100).toInt()}%" to "a faster train",
                    "Coins x${fmt(Balance.TIER_INCOME, 1)}" to "every metre pays more",
                    (if (carsNext > vm.maxCars) "+${carsNext - vm.maxCars} cars" else "All cars") to "room for $carsNext cars",
                    (if (vm.nextRushMult > 1.01) "Rush x${fmt(vm.nextRushMult, 1)}" else "Rush x1") to "coins for 10 min",
                    "${vm.extraLevels} extra levels" to "buy more before evolving for more cores and a bigger rush",
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    rewards.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { (big, small) ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.7f)).border(1.8.dp, Ink, RoundedCornerShape(14.dp)).padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(big, fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                                    Text(small, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFE1E1).copy(alpha = 0.7f)).padding(8.dp)) {
                        Text("Starts over", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Coins, run upgrades and the unlock chain.", fontSize = 10.sp, color = Muted)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE2F3EA).copy(alpha = 0.8f)).padding(8.dp)) {
                        Text("You keep", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Frost Cores, permanent upgrades, gems, bag, awards, level.", fontSize = 10.sp, color = Muted)
                    }
                }
            }
            item {
                ChunkyButton(
                    vm.canEvolve && !top,
                    if (confirm) listOf(Color(0xFFFFB9B0), Color(0xFFEF7B72)) else listOf(Color(0xFFE3D6FB), Color(0xFFB9A2EE)),
                    { if (confirm) { vm.evolve(); confirm = false; onClose() } else confirm = true },
                    Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            top -> "You have the top train"
                            !vm.canEvolve -> "Keep upgrading to evolve"
                            confirm -> "Tap again to evolve now"
                            else -> "Evolve to ${TIERS[next].name}"
                        },
                        fontSize = 16.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = if (vm.canEvolve && !top) Ink else Muted,
                    )
                }
            }
            item { PrestigeCard(vm) }
            // every train
            item {
                SectionTitle("All trains", Ice, "${cur + 1} of ${TIERS.size}")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TIERS.forEachIndexed { i, t ->
                        val owned = i <= cur
                        Column(
                            Modifier
                                .width(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (i == cur) Color(0xFFFFE49A) else Color.White.copy(alpha = 0.6f))
                                .border(if (i == cur) 2.5.dp else 1.5.dp, Ink.copy(alpha = if (owned) 1f else 0.4f), RoundedCornerShape(14.dp))
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TrainPicture(i, Modifier.size(70.dp, 80.dp).graphicsLayer { alpha = if (owned) 1f else 0.35f })
                            Text(t.name, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (owned) Ink else Muted, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 12.sp)
                            Text("Tier ${i + 1}", fontSize = 9.sp, color = Muted)
                        }
                    }
                }
            }
        }
    }
}


/** The lucky wheel's rail icon. */
@Composable
private fun WheelIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cols = listOf(Color(0xFFFFE49A), Color(0xFFF49AB2), Color(0xFF9FD3EE), Color(0xFFBFE0D2))
        for (i in 0 until 8) drawArc(cols[i % 4], -90f + i * 45f, 45f, true)
        drawCircle(Ink, style = Stroke(2.dp.toPx()))
        drawCircle(Panel, size.minDimension * 0.14f)
        drawCircle(Ink, size.minDimension * 0.14f, style = Stroke(1.5.dp.toPx()))
    }
}
