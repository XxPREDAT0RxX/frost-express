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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Palette: snowy exterior (matches the scene) + warm wooden interior (matches the train floor).
// Cozy palette: cream panels, soft pastels and a warm brown outline around everything.
private val Snow = Color(0xFFEEF5FB)
private val Ink = Color(0xFF6B4A3A)       // warm brown: text and outlines
private val Panel = Color(0xFFFFF6E9)     // cream
private val PanelHi = Color(0xFFF3E3CC)
private val Track = Color(0xFFEFE3D3)     // empty part of progress bars
private val Wood = Color(0xFF6B4A3A)      // outline colour for cards and sheets
private val Ice = Color(0xFF5E9CCF)
private val Ember = Color(0xFFF08A5D)
private val Gold = Color(0xFFFFC85C)
private val GoldText = Color(0xFFC98A1E)
private val Mint = Color(0xFF3F9E74)
private val Muted = Color(0xFF9A7A64)
private val Glass = Color(0xFFFFF6E9).copy(alpha = 0.97f)
private val OutlineW = 2.5.dp

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
        content = { CompositionLocalProvider(LocalContentColor provides Ink) { content() } },
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
fun GameScreen(vm: GameViewModel) {
    // 0 = no sheet, 1 = upgrades, 2 = shop. Both slide up over the bottom of the running scene.
    var sheet by remember { mutableIntStateOf(0) }
    val shownSheet = remember { intArrayOf(1) }
    if (sheet != 0) shownSheet[0] = sheet
    val showUpgrades = sheet == 1
    var showSettings by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<UpgradeType?>(null) }
    BackHandler(enabled = sheet != 0) { sheet = 0 }
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.52f).dp
    val sheetPx = with(LocalDensity.current) { sheetHeight.roundToPx() }
    // One progress value drives the sheet height, so opening and closing never jump.
    val sheetP by animateFloatAsState(if (sheet != 0) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "sheet")
    val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(Snow)
    ) {
        // The running scene takes whatever space the sheet leaves free; the footer floats on top of it.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            TrainScene(vm, Modifier.fillMaxSize(), onDeviceTap = { type ->
                vm.tutorialAdvance(1)
                detail = type
            })
            TopHud(
                vm,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                onOpenSettings = { showSettings = true },
                onOpenGems = { sheet = 3 },
            )
            AchievementBanner(
                vm.achievementPopup,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 84.dp),
            )
            ToastBanner(
                vm.toast,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 120.dp),
            )
            val hint = when {
                vm.tutorialStage == 1 -> "Tap the gear to open your upgrades"
                vm.tutorialStage == 2 && !showUpgrades -> "Tap the gear and buy Engine Power"
                vm.tutorialStage == 2 -> "Wait for 20 coins, then buy Engine Power"
                vm.tutorialStage == 3 -> "Now tap BOOST a few times for bonus income"
                else -> null
            }
            if (hint != null) TutorialHint(hint, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp + bottomInset * (1f - sheetP)))
            if (vm.event == GameEvent.CRATE) {
                SupplyCrate(
                    vm::collectCrate,
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp),
                )
            }
            BottomBar(
                vm,
                sheetOpen = showUpgrades,
                onToggleUpgrades = {
                    if (!showUpgrades) vm.tutorialAdvance(1)
                    sheet = if (sheet == 1) 0 else 1
                },
                shopOpen = sheet == 2,
                onToggleShop = { sheet = if (sheet == 2) 0 else 2 },
                pulseGear = vm.tutorialStage == 1 || (vm.tutorialStage == 2 && !showUpgrades),
                pulseBoost = vm.tutorialStage == 3,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(top = 6.dp, bottom = 6.dp + bottomInset * (1f - sheetP)),
            )
        }
        if (sheetP > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minHeight = sheetPx, maxHeight = sheetPx))
                        val h = (sheetPx * sheetP).toInt()
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                    .clipToBounds(),
            ) {
                if (shownSheet[0] == 2) {
                    ShopSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onGetGems = { sheet = 3 })
                } else if (shownSheet[0] == 3) {
                    GemShopSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onBack = { sheet = 2 })
                } else {
                    UpgradesSheet(
                        vm, Modifier.height(sheetHeight),
                        focus = null, onFocusDone = {},
                        onOpenDetail = { detail = it },
                        onClose = { sheet = 0 },
                    )
                }
            }
        }
    }

    detail?.let { UpgradeDialog(vm, it, onClose = { detail = null }) }

    // Pop-ups in order: tutorial welcome, daily reward, welcome back. The rest of the tutorial is shown as hints.
    if (vm.tutorialStage == 0) {
        FrostAlert(
            onDismissRequest = {},
            title = { Text("Welcome aboard!") },
            text = {
                Text("Your train earns coins from the distance it travels, even when the app is closed. Let's buy your first upgrade and try the boost. It takes about a minute.")
            },
            confirmButton = { TextButton(onClick = { vm.tutorialAdvance(0) }) { Text("Start") } },
            dismissButton = { TextButton(onClick = vm::finishTutorial) { Text("Skip") } },
        )
    } else if (vm.tutorialDone && vm.dailyReady) {
        val streak = vm.dailyNextStreak
        FrostAlert(
            onDismissRequest = {},
            title = { Text("Daily reward · Day $streak") },
            text = {
                Text(
                    "${fmt(vm.dailyCoins(streak))} coins" +
                        (if (vm.dailyGivesCore(streak)) ", 1 Frost Core" else "") + ", ${vm.dailyGems(streak)} gems" +
                        "\nCome back every day to grow your streak. Every 7th day gives a Frost Core."
                )
            },
            confirmButton = { TextButton(onClick = vm::claimDaily) { Text("Claim") } },
        )
    } else if (vm.tutorialDone && vm.offlineCoins > 0.0) {
        FrostAlert(
            onDismissRequest = vm::dismissOffline,
            title = { Text("Welcome back!") },
            text = {
                Text("Your train travelled ${fmtDistance(vm.offlineMeters)} while you were away and earned ${fmt(vm.offlineCoins)} coins.")
            },
            confirmButton = { TextButton(onClick = vm::dismissOffline) { Text("Collect") } },
        )
    }

    vm.cloudConflict?.let { cloudJson ->
        FrostAlert(
            onDismissRequest = {},
            title = { Text("Cloud save found") },
            text = {
                Text("Cloud: ${vm.describeCloud(cloudJson)}\nThis device: ${vm.describeLocal()}\n\nWhich progress do you want to keep?")
            },
            confirmButton = { TextButton(onClick = vm::useCloudSave) { Text("Use cloud save") } },
            dismissButton = { TextButton(onClick = vm::keepLocalSave) { Text("Keep this device") } },
        )
    }

    if (showSettings) SettingsDialog(vm, onClose = { showSettings = false })
}

@Composable
private fun CoinIcon(modifier: Modifier = Modifier) {
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
private fun TopHud(vm: GameViewModel, modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}, onOpenGems: () -> Unit = {}) {
    val progress = (vm.runLevels.toFloat() / vm.evolveLevels).coerceIn(0f, 1f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(OutlineW, Wood, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(fmt(vm.coins), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldText, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GemPink.copy(alpha = 0.14f))
                    .clickable(onClick = onOpenGems)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemIcon(Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${vm.gems}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GemPink)
                Spacer(Modifier.width(3.dp))
                Text("+", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GemPink)
            }
            Spacer(Modifier.width(6.dp))
            SpeakerButton(vm.soundOn, vm::toggleSound)
            Spacer(Modifier.width(6.dp))
            MenuButton(onOpenSettings)
            Spacer(Modifier.width(8.dp))
            StatChip("${fmt(vm.speed, 1)} m/s", Ice)
            Spacer(Modifier.width(6.dp))
            StatChip("${fmt(vm.coinsPerSec, 1)}/s", Mint)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TIERS[vm.tierIndex].name, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Track)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Brush.horizontalGradient(listOf(Ice, Mint)))
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Evolve ${vm.runLevels}/${vm.evolveLevels}", fontSize = 10.sp, color = Muted)
        }
        TimeChip(vm)
    }
}

/** Shows the time of day (and a storm warning when a snow storm is active). */
@Composable
private fun TimeChip(vm: GameViewModel) {
    val (phase, secs) = vm.dayPhase()
    val night = phase == "Night" || phase == "Dusk"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) {
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
        Spacer(Modifier.width(5.dp))
        Text("$phase · next in ${secs}s", fontSize = 10.sp, color = Muted)
        if (vm.stormLeft > 0.0) {
            Spacer(Modifier.width(8.dp))
            Text(
                "Storm ${vm.stormLeft.toInt() + 1}s",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Ice,
                modifier = Modifier
                    .background(Ice.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        for (item in listOf(ShopItem.COCOA, ShopItem.TURBO, ShopItem.TICKET)) {
            val left = vm.shopLeft(item)
            if (left <= 0.0) continue
            val label = when (item) { ShopItem.COCOA -> "Coins x2"; ShopItem.TURBO -> "Speed x1.5"; else -> "Stations x3" }
            Spacer(Modifier.width(6.dp))
            Text(
                "$label ${left.toInt() / 60}:${(left.toInt() % 60).toString().padStart(2, '0')}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GoldText,
                modifier = Modifier
                    .background(Gold.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(PanelHi)
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
                .clip(RoundedCornerShape(20.dp))
                .background(Wood)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarIcon(Color.White, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Achievement: $shown", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
private fun LockIcon(color: Color, modifier: Modifier = Modifier) {
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
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Ice)
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
private fun StarIcon(color: Color, modifier: Modifier = Modifier) {
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
private fun PillButton(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
                }
                SettingsCard("Graphics") {
                    Text("Frame rate", fontSize = 12.sp, color = Muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 60, 90, 120).forEach { fps ->
                            val on = vm.fpsCap == fps
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) Wood else Track)
                                    .clickable { vm.changeFps(fps) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$fps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Muted)
                            }
                        }
                    }
                    Text("Your phone may cap this at its screen refresh rate. Lower values save battery.", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                    SettingToggle("Post-processing", vm.postFx) { vm.changePostFx(it) }
                    Text("Light bloom, colour grading and vignette. Turn off on slow phones.", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
                }
                SettingsCard("Train paint") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LIVERIES.forEachIndexed { i, l ->
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
                    }
                    Text("A new paint unlocks with each evolve.", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
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
                Text("Frost Express v${BuildConfigVersion}", fontSize = 10.sp, color = Muted)
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
                TextButton(onClick = { vm.resetAll(); confirmReset = false; onClose() }) { Text("Reset", color = Color(0xFFB03A2E)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

private const val PRIVACY_URL = "https://github.com/XxPREDAT0RxX/frost-express/blob/main/PRIVACY.md"
private const val BuildConfigVersion = "1.0"

@Composable
private fun SpeakerButton(on: Boolean, onClick: () -> Unit) {
    val c = if (on) Ice else Muted
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(PanelHi)
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
private fun StatChip(text: String, color: Color) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun BottomBar(
    vm: GameViewModel,
    sheetOpen: Boolean,
    onToggleUpgrades: () -> Unit,
    shopOpen: Boolean = false,
    onToggleShop: () -> Unit = {},
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
        vm.boostLocked -> "Recharging ${(fill * 100).toInt()}%"
        vm.isBoosting -> "x${fmt(vm.boostMultiplier, 1)} active · ${vm.boostTimeLeft.toInt() + 1}s"
        else -> "Tap BOOST · x${fmt(vm.boostMultiplier, 1)} income"
    }
    val canBuySomething = UPGRADES.any { spec ->
        vm.level(spec.type) < spec.maxLevel && vm.coins >= vm.upgradeCost(spec)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel.copy(alpha = 0.95f))
            .border(OutlineW, Wood, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .border(if (pulseGear) 3.dp else 0.dp, Ember.copy(alpha = pulse), CircleShape)
                .clip(CircleShape)
                .background(if (sheetOpen) Color(0xFF9FC5EA) else Color(0xFFCDE7F8))
                .border(2.dp, Ink, CircleShape)
                .clickable(onClick = onToggleUpgrades),
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(Ink, Modifier.size(22.dp))
            if (canBuySomething && !sheetOpen) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(10.dp)
                        .background(Ember, CircleShape)
                        .border(1.5.dp, Panel, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (shopOpen) Color(0xFFFFC9A0) else Color(0xFFFFE1C8))
                .border(2.dp, Ink, CircleShape)
                .clickable(onClick = onToggleShop),
            contentAlignment = Alignment.Center,
        ) {
            ShopIcon(Ember, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Track)
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
            Text(status, fontSize = 10.sp, color = Muted)
        }
        if (Balance.ADS_ENABLED) {
            Spacer(Modifier.width(8.dp))
            Pill(
                label = if (vm.adBoostLeft > 0.0) "x2 ${vm.adBoostLeft.toInt()}s" else "AD x2",
                bg = Mint, fg = Ink, enabled = vm.adBoostLeft <= 0.0, onClick = vm::watchAdBoost,
            )
        }
        Spacer(Modifier.width(10.dp))
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
            Text("BOOST!", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (vm.boostLocked) Muted else Ink)
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
    onClose: () -> Unit,
) {
    var confirmEvolve by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Tapping a device on the train jumps to its upgrade and highlights it for a moment.
    androidx.compose.runtime.LaunchedEffect(focus) {
        if (focus != null) {
            tab = 0
            kotlinx.coroutines.delay(90)
            listState.animateScrollToItem((1 + UPGRADES.indexOfFirst { it.type == focus }).coerceAtLeast(0))
            kotlinx.coroutines.delay(2500)
            onFocusDone()
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Panel)
            .border(OutlineW, Wood, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Upgrades", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (tab != 1) {
                CoinIcon(Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(fmt(vm.coins), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldText)
            } else {
                CoreIcon(Ice, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("${vm.coresAvailable}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ice)
            }
            TextButton(onClick = onClose) { Text("Close", fontSize = 12.sp) }
        }
        TabRow(tab, { tab = it }, Modifier.padding(horizontal = 12.dp))
        if (tab == 0) BuyAmountRow(vm, Modifier.padding(horizontal = 12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (tab == 2) {
                item {
                    Text(
                        "${vm.unlocked.size}/${ACHIEVEMENTS.size} unlocked. Rewards: coins, or Frost Cores for the big ones.",
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
                    )
                }
            } else {
                item {
                    Text("Reset when you evolve. Hold a buy button to keep buying.", fontSize = 11.sp, color = Muted)
                }
            // Show every unlocked upgrade plus the next locked one; the rest stay a surprise.
            val firstLocked = UPGRADES.indexOfFirst { !vm.isUnlocked(it) }
            val visibleUpgrades = if (firstLocked < 0) UPGRADES else UPGRADES.take(firstLocked + 1)
            items(visibleUpgrades) { spec ->
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
                    level = if (locked) "Locked" else "Lv $lvl${if (spec.maxLevel != Int.MAX_VALUE) "/${spec.maxLevel}" else ""}",
                    effect = if (locked && vm.blockedByTier(spec)) "Evolve your train to add the next car" else if (locked) "Reach ${req?.title} Lv ${spec.unlockLevel} (now Lv ${if (req != null) vm.level(req.type) else 0})" else spec.effect,
                    note = if (locked) null else when {
                        mult > 1.0 && next != null -> "Bonus x${fmt(mult)} · next x2 at Lv $next"
                        mult > 1.0 -> "Bonus x${fmt(mult)}"
                        next != null -> "Next x2 bonus at Lv $next"
                        else -> null
                    },
                    buttonTop = if (locked) "Locked" else if (maxed) "MAX" else "+$n",
                    buttonBottom = if (maxed || locked) null else fmt(cost),
                    enabled = !locked && !maxed && vm.coins >= cost,
                    onBuy = { vm.buy(spec) },
                    locked = locked,
                    highlight = focus == spec.type,
                    onInfo = if (locked) null else ({ onOpenDetail(spec.type) }),
                )
            }
            if (firstLocked >= 0 && firstLocked + 1 < UPGRADES.size) item {
                Text("${UPGRADES.size - firstLocked - 1} more upgrades to discover...", fontSize = 11.sp, color = Muted)
            }
        }
            if (tab == 0) item { EvolveCard(vm, onEvolve = { confirmEvolve = true }) }
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
                TextButton(onClick = { vm.evolve(); confirmEvolve = false }) { Text("Evolve") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEvolve = false }) { Text("Cancel") }
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
private fun CoreIcon(color: Color, modifier: Modifier = Modifier) {
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
    Row(modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Upgrades", "Permanent", "Awards").forEachIndexed { i, label ->
            val on = selected == i
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Color(0xFFFFC9A0) else PanelHi)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp))
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
private fun AchievementRow(a: Achievement, done: Boolean) {
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
        Text(
            if (a.cores > 0) "+${a.cores} core" else "coins",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (done) Mint else Muted,
        )
    }
}

@Composable
private fun BuyAmountRow(vm: GameViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(1 to "x1", 10 to "x10", 100 to "x100", 0 to "MAX").forEach { (amount, label) ->
            val on = vm.buyAmount == amount
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Ice else Panel)
                    .clickable { vm.buyAmount = amount }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Muted)
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
) {
    val buyNow = androidx.compose.runtime.rememberUpdatedState(onBuy)
    val accent = if (locked) Muted else upgradeColor(type)
    Row(
        Modifier
            .fillMaxWidth()
            .border(if (highlight) 3.dp else 2.dp, if (highlight) Ember else Ink.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.18f))
                .then(if (onInfo != null) Modifier.clickable { onInfo() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (locked) LockIcon(Muted, Modifier.size(20.dp)) else if (icon != null) icon() else UpgradeIcon(type, accent, Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).then(if (onInfo != null) Modifier.clickable { onInfo() } else Modifier)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(level, fontSize = 10.sp, color = accent, fontWeight = FontWeight.SemiBold)
            }
            Text(effect, fontSize = 11.sp, color = Muted)
            if (note != null) Text(note, fontSize = 10.sp, color = GoldText)
        }
        Column(
            Modifier
                .width(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) accent else PanelHi)
                .border(2.dp, Ink, RoundedCornerShape(12.dp))
                // Tap to buy once; hold to keep buying, faster the longer you hold.
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
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
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(buttonTop, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) Color.White else Muted)
            if (buttonBottom != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        priceIcon == 2 -> GemIcon(Modifier.size(10.dp))
                        priceIcon == 3 -> {}
                        core || priceIcon == 1 -> CoreIcon(if (enabled) Color.White else Muted, Modifier.size(9.dp))
                        else -> CoinIcon(Modifier.size(9.dp))
                    }
                    Spacer(Modifier.width(3.dp))
                    Text(buttonBottom, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (enabled) Color.White else Muted)
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
                .clickable(enabled = vm.canEvolve, onClick = onEvolve)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (vm.canEvolve) "Evolve to ${next.name}  (+${vm.coreGain} cores)" else "Buy more upgrades to evolve",
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
        Text("Frost Express crashed last time", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    val color = gadgetColor(spec.gadget)
    val lvl = vm.level(type)
    val maxed = lvl >= spec.maxLevel
    val locked = !vm.isUnlocked(spec)
    val n = vm.buyCount(spec)
    val cost = vm.bulkCost(spec, n)
    val canBuy = !locked && !maxed && vm.coins >= cost
    val buyNow = androidx.compose.runtime.rememberUpdatedState { vm.buy(spec) }

    val mats = rememberMaterials()
    val sprites = rememberGameSprites()
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = (it / 1_000_000L) / 1000f }
    }
    val flash = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    var prev by remember { mutableIntStateOf(lvl) }
    LaunchedEffect(lvl) {
        if (lvl > prev) {
            launch { flash.snapTo(1f); flash.animateTo(0f, tween(900)) }
            launch { pop.snapTo(1.16f); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
        }
        prev = lvl
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Panel)
                .border(OutlineW, Wood, RoundedCornerShape(24.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(spec.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(
                    if (locked) "Locked" else "Lv $lvl" + (if (spec.maxLevel != Int.MAX_VALUE) "/${spec.maxLevel}" else ""),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color,
                )
            }
            Spacer(Modifier.height(8.dp))
            // the device, on a patch of the train's wooden floor
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFEBDDBF), Color(0xFFD8C298))))
                    .border(OutlineW, Wood, RoundedCornerShape(16.dp)),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    var py = 14f * density
                    while (py < size.height) {
                        drawLine(Color(0x1A5A4A3B), Offset(0f, py), Offset(size.width, py), 1.5f * density)
                        py += 30f * density
                    }
                }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                ) {
                    val side = min(size.width, size.height) * 1.05f
                    val ox = (size.width - side) / 2f
                    val oy = (size.height - side) / 2f
                    if (drawDeviceSprite(sprites, type, ox, oy, side, t, 0.6f)) {
                        val f = flash.value
                        if (f > 0.01f) {
                            val c0 = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(Color(0xFFFFE680).copy(alpha = f * 0.8f), side * (0.15f + 0.4f * (1f - f)), c0, style = Stroke(side * 0.04f * f))
                            for (i in 0 until 8) {
                                val a = (PI / 4 * i).toFloat() + 0.3f
                                val d = side * (0.2f + 0.3f * (1f - f))
                                drawCircle(Color.White.copy(alpha = f), side * 0.02f, Offset(c0.x + cos(a) * d, c0.y + sin(a) * d))
                            }
                        }
                    } else withMaterial(mats.forType(type), 0f, 0f, size.width, size.height, density / 3f) {
                        if (spec.eff != null) {
                            val side = min(size.width, size.height) * 0.92f
                            drawDevice(type, (size.width - side) / 2f, (size.height - side) / 2f, side, t, maxOf(lvl, 1), flash.value, 0.6f)
                        } else {
                            drawGadget(spec.gadget, color, t, lvl, flash.value)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (locked) {
                val req = UPGRADES.firstOrNull { it.type == spec.unlockType }
                Text(if (vm.blockedByTier(spec)) "This car comes with a bigger train. Evolve to unlock it." else "Reach ${req?.title} Lv ${spec.unlockLevel} to unlock.", fontSize = 12.sp, color = Muted)
            } else {
                val eff = spec.eff
                if (eff != null) {
                    Text("Now: " + (if (lvl == 0) "no bonus yet" else effText(eff, spec.per * lvl)), fontSize = 12.sp, color = Ink)
                    if (!maxed) Text("Next level: " + effText(eff, spec.per * (lvl + 1)), fontSize = 12.sp, color = GoldText, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Each level: ${spec.effect}", fontSize = 12.sp, color = Ink)
                    vm.nextMilestone(type)?.let { Text("Double bonus at Lv $it", fontSize = 12.sp, color = GoldText, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Close") }
                Column(
                    Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canBuy) color else PanelHi)
                        // Tap to buy once; hold to keep buying, faster the longer you hold.
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
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (locked) "Locked" else if (maxed) "MAX" else "Upgrade +$n",
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (canBuy) Color.White else Muted,
                    )
                    if (!locked && !maxed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinIcon(Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(fmt(cost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (canBuy) Color.White else Muted)
                        }
                    }
                }
            }
        }
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
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Panel)
            .border(1.5.dp, Ember.copy(alpha = 0.55f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFE3C2), Color(0xFFFFF1DC))))
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShopIcon(Ember, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Shop", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GemIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("${vm.gems}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GemPink)
            Spacer(Modifier.width(8.dp))
            PillButton("Get gems", GemPink, Color.White, onClick = onGetGems)
            TextButton(onClick = onClose) { Text("Close", fontSize = 12.sp) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text("Boosters bought with gems. Timed boosters stack up to 30 minutes.", fontSize = 11.sp, color = Muted)
            }
            items(ShopItem.values().toList()) { item ->
                val cost = vm.shopCost(item)
                val left = vm.shopLeft(item)
                val avail = vm.shopAvailable(item)
                UpgradeRow(
                    type = when (item) {
                        ShopItem.COCOA -> UpgradeType.FOOD
                        ShopItem.TURBO -> UpgradeType.ENGINE
                        ShopItem.TICKET -> UpgradeType.CONDUCTOR
                        ShopItem.TANK -> UpgradeType.BOOSTER
                        ShopItem.CLEAR -> UpgradeType.PLOW
                    },
                    title = item.title,
                    level = if (left > 0.0) "Active ${left.toInt() / 60}:${(left.toInt() % 60).toString().padStart(2, '0')}" else "",
                    effect = item.desc,
                    note = when {
                        item == ShopItem.CLEAR && !avail -> "Only during a snow storm"
                        item == ShopItem.TANK && !avail -> "Your boost meter is already full"
                        !avail -> "Maximum 30 minutes stacked"
                        else -> null
                    },
                    buttonTop = "Buy",
                    buttonBottom = "$cost",
                    enabled = avail && vm.gems >= cost,
                    onBuy = { vm.buyShop(item) },
                    priceIcon = 2,
                )
            }
        }
    }
}

// ---------- gems ----------

private val GemPink = Color(0xFFD6336C)

@Composable
private fun GemIcon(modifier: Modifier = Modifier) {
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

@Composable
private fun GemShopSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Panel)
            .border(1.5.dp, GemPink.copy(alpha = 0.55f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD6E5), Color(0xFFF3E3FF))))
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("< Shop", fontSize = 12.sp) }
            Text("Gem Shop", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GemIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("${vm.gems}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GemPink)
            TextButton(onClick = onClose) { Text("Close", fontSize = 12.sp) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Text("GEM PACKS", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = GemPink) }
            items(GemPack.values().toList()) { pack ->
                val price = vm.gemPackPrice(pack)
                UpgradeRow(
                    type = UpgradeType.BOOSTER,
                    title = pack.title,
                    level = "",
                    effect = "+${pack.gems} gems",
                    note = if (!Balance.IAP_ENABLED) "Opens after launch on Google Play" else null,
                    buttonTop = if (Balance.IAP_ENABLED) "Buy" else "Soon",
                    buttonBottom = price,
                    enabled = Balance.IAP_ENABLED,
                    onBuy = { vm.buyGemPack(pack) },
                    priceIcon = 3,
                    icon = { GemIcon(Modifier.size(24.dp)) },
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("TRADE COINS", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = GemPink)
                Text("Up to ${Balance.GEM_TRADE_PER_DAY} gems a day. ${vm.gemTradeLeft} left today.", fontSize = 11.sp, color = Muted)
            }
            items(GemTrade.values().toList()) { trade ->
                val cost = vm.gemTradeCost(trade)
                UpgradeRow(
                    type = UpgradeType.BOOSTER,
                    title = "${trade.gems} gems",
                    level = "",
                    effect = "Pay with coins",
                    note = if (vm.gemTradeLeft < trade.gems) "Daily limit reached" else null,
                    buttonTop = "Trade",
                    buttonBottom = fmt(cost),
                    enabled = vm.coins >= cost && vm.gemTradeLeft >= trade.gems,
                    onBuy = { vm.tradeForGems(trade) },
                    icon = { GemIcon(Modifier.size(22.dp)) },
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("FREE GEMS", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = GemPink)
                Text(
                    "The daily reward gives ${Balance.DAILY_GEMS} gems, and ${Balance.DAILY_GEMS * 5} on every 7th day in a row.",
                    fontSize = 11.sp, color = Muted,
                )
            }
        }
    }
}
