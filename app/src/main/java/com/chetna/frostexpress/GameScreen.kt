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
            }, link = link)
            TopHud(
                vm,
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                onOpenSettings = { showSettings = true },
                onOpenGems = { sheet = 3 },
                onOpenLevel = { sheetTab = 2; sheet = 1 },
            )
            // side rails: they use the snowy space left and right of the train
            LeftRail(
                vm,
                onQuests = { sheet = if (sheet == 5) 0 else 5 },
                onAwards = { sheetTab = 2; sheet = 1 },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 122.dp, start = 8.dp),
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
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 122.dp, end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RightRail(vm, link, onEvolve = { sheetTab = 0; sheet = 1 })
                ActiveBoosters(vm)
            }
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
            NewTrainPopup(vm)
            BoosterPopup(vm, Modifier.align(Alignment.Center))
            BottomBar(
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
                } else if (shownSheet[0] == 4) {
                    InventorySheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 }, onShop = { sheet = 2 })
                } else if (shownSheet[0] == 5) {
                    QuestSheet(vm, Modifier.height(sheetHeight), onClose = { sheet = 0 })
                } else {
                    UpgradesSheet(
                        vm, Modifier.height(sheetHeight),
                        focus = if (vm.tutorialStage == 2) UpgradeType.ENGINE else null, onFocusDone = {},
                        startTab = sheetTab, onTabShown = { sheetTab = 0 },
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
        TutorialCard(
            title = "Welcome aboard!",
            step = 0,
            text = "The world outside has frozen, but your little train keeps rolling. It earns coins for every metre it travels, even while the app is closed.\n\nLet me show you the ropes. It only takes a minute!",
            button = "Let's go!",
            onButton = { vm.tutorialAdvance(0) },
            onSkip = vm::finishTutorial,
        )
    } else if (vm.tutorialDone && vm.dailyReady) {
        DailyGiftCard(vm)
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
private fun TopHud(vm: GameViewModel, modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}, onOpenGems: () -> Unit = {}, onOpenLevel: () -> Unit = {}) {
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
            LevelBadge(vm, onOpenLevel)
            Spacer(Modifier.width(8.dp))
            CoinIcon(Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(fmt(vm.coins), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = GoldText, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
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
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatChip("${fmt(vm.speed, 1)} m/s", Ice)
            Spacer(Modifier.width(5.dp))
            StatChip("+${fmt(vm.coinsPerSec, 1)}/s", Mint)
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
            Text("Evolve ${vm.runLevels}/${vm.evolveLevels}", fontSize = 10.sp, color = Muted, maxLines = 1, softWrap = false)
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
                TextButton(onClick = { UiSound.click();  vm.resetAll(); confirmReset = false; onClose() }) { Text("Reset", color = Color(0xFFB03A2E)) }
            },
            dismissButton = { TextButton(onClick = { UiSound.click();  confirmReset = false }) { Text("Cancel") } },
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
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        softWrap = false,
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
    openSheet: Int = 0,
    onToggleShop: () -> Unit = {},
    onToggleGems: () -> Unit = {},
    onToggleBag: () -> Unit = {},
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
    val affordable = UPGRADES.count { spec ->
        vm.isUnlocked(spec) && vm.level(spec.type) < spec.maxLevel && vm.coins >= vm.upgradeCost(spec)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel.copy(alpha = 0.95f))
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
        FooterIcon(sprites.iconBag, openSheet == 4, Color(0xFFDDEEFB), onToggleBag, badge = vm.inventoryCount)
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
    onClose: () -> Unit,
) {
    var confirmEvolve by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(startTab) }
    LaunchedEffect(startTab) { if (startTab != 0) { tab = startTab; onTabShown() } }
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
            TextButton(onClick = { UiSound.click(); onClose() }) { Text("Close", fontSize = 12.sp) }
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
                    )
                }
            } else {
                item {
                    Text("Reset when you evolve. Hold a buy button to keep buying.", fontSize = 11.sp, color = Muted)
                }
            item { SurvivorsCard(vm, onInfo = { onOpenDetail(UpgradeType.SURVIVORS) }) }
            // Show every unlocked upgrade plus the next locked one; the rest stay a surprise.
            val chain = UPGRADES.filter { it.type != UpgradeType.SURVIVORS }
            val firstLocked = chain.indexOfFirst { !vm.isUnlocked(it) }
            val visibleUpgrades = if (firstLocked < 0) chain else chain.take(firstLocked + 1)
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
                )
            }
            if (firstLocked >= 0 && firstLocked + 1 < chain.size) item {
                Text("${chain.size - firstLocked - 1} more upgrades to discover...", fontSize = 11.sp, color = Muted)
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
        Column(horizontalAlignment = Alignment.End) {
            Text("+${a.xp} XP", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Color(0xFF8E6FD8) else Muted)
            Text(if (a.cores > 0) "+${a.cores} core" else "coins", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (done) Mint else Muted)
        }
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Color(0xFF9FC5EA) else Panel)
                    .border(2.dp, Ink, RoundedCornerShape(10.dp))
                    .clickable { vm.buyAmount = amount }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted)
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
            // close button
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 0.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF7A3A3))
                    .border(2.5.dp, Ink, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(12.dp)) {
                    drawLine(Ink, Offset.Zero, Offset(size.width, size.height), 3.dp.toPx(), StrokeCap.Round)
                    drawLine(Ink, Offset(size.width, 0f), Offset(0f, size.height), 3.dp.toPx(), StrokeCap.Round)
                }
            }
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
            rememberGameSprites().iconShop?.let { Image(it, null, Modifier.size(22.dp)) }
            Spacer(Modifier.width(6.dp))
            Text("Shop", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GemIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("${vm.gems}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GemPink)
            Spacer(Modifier.width(8.dp))
            PillButton("Get gems", GemPink, Color.White, onClick = onGetGems)
            TextButton(onClick = { UiSound.click(); onClose() }) { Text("Close", fontSize = 12.sp) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text("Buy boosters with gems. They go to your bag, use them whenever you like.", fontSize = 11.sp, color = Muted)
            }
            if (Balance.ADS_ENABLED) {
                items(listOf(AdReward.BOOSTER, AdReward.COINS)) { r -> AdRow(vm, r) }
            }
            items(ShopItem.values().toList()) { item ->
                val cost = vm.shopCost(item)
                val owned = vm.owned(item)
                UpgradeRow(
                    type = when (item) {
                        ShopItem.COCOA -> UpgradeType.FOOD
                        ShopItem.TURBO -> UpgradeType.ENGINE
                        ShopItem.TICKET -> UpgradeType.CONDUCTOR
                        ShopItem.TANK -> UpgradeType.BOOSTER
                        ShopItem.CLEAR -> UpgradeType.PLOW
                    },
                    title = item.title,
                    level = if (owned > 0) "In bag: $owned" else "",
                    effect = item.desc,
                    note = null,
                    buttonTop = "Buy",
                    buttonBottom = "$cost",
                    enabled = vm.gems >= cost,
                    onBuy = { vm.buyShop(item) },
                    icon = { ItemIcon(item, Modifier.size(34.dp)) },
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
            TextButton(onClick = { UiSound.click(); onBack() }) { Text("< Shop", fontSize = 12.sp) }
            Text("Gem Shop", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GemIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("${vm.gems}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GemPink)
            TextButton(onClick = { UiSound.click(); onClose() }) { Text("Close", fontSize = 12.sp) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (Balance.ADS_ENABLED) {
                item { Text("FREE WITH ADS", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = GemPink) }
                item { AdRow(vm, AdReward.GEMS) }
            }
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

/** The red notification dot with a number. Drawn outside the round button so it is never cut off. */
@Composable
private fun NotifyBadge(count: Int, modifier: Modifier = Modifier) {
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
private fun ItemIcon(item: ShopItem, modifier: Modifier = Modifier) {
    val img = rememberGameSprites().item[item]
    if (img != null) Image(img, item.title, modifier) else Box(modifier)
}

private fun fmtClock(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun InventorySheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit, onShop: () -> Unit) {
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
                .background(Brush.horizontalGradient(listOf(Color(0xFFDDEEFB), Color(0xFFEAF6F0))))
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rememberGameSprites().iconBag?.let { Image(it, null, Modifier.size(22.dp)) }
            Spacer(Modifier.width(6.dp))
            Text("My Bag", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            PillButton("Shop", Color(0xFFFFE1C8), Ink, onClick = onShop)
            TextButton(onClick = { UiSound.click(); onClose() }) { Text("Close", fontSize = 12.sp) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    if (vm.inventoryCount == 0) "Your bag is empty. Buy boosters in the shop with gems." else "Tap Use to switch a booster on. Timed boosters stack up to 30 minutes.",
                    fontSize = 11.sp, color = Muted,
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
                    note = when {
                        left > 0.0 -> "Active · ${fmtClock(left)} left"
                        owned > 0 && item == ShopItem.CLEAR && !vm.shopAvailable(item) -> "Use it during a snow storm"
                        owned > 0 && item == ShopItem.TANK && !vm.shopAvailable(item) -> "Your boost meter is already full"
                        owned > 0 && !vm.shopAvailable(item) -> "Already stacked to 30 minutes"
                        else -> null
                    },
                    buttonTop = "Use",
                    buttonBottom = null,
                    enabled = vm.canUse(item),
                    onBuy = { vm.useItem(item) },
                    icon = { ItemIcon(item, Modifier.size(34.dp)) },
                )
            }
        }
    }
}

/** Round badges for every booster that is running, with a ring that empties as time runs out. */
@Composable
private fun ActiveBoosters(vm: GameViewModel, modifier: Modifier = Modifier) {
    val running = listOf(ShopItem.COCOA, ShopItem.TURBO, ShopItem.TICKET).filter { vm.shopLeft(it) > 0.0 }
    if (running.isEmpty()) return
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "boostBadge").animateFloat(
        0.94f, 1.06f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "boostBadgePulse",
    )
    var open by remember { mutableStateOf<ShopItem?>(null) }
    LaunchedEffect(open) { if (open != null) { kotlinx.coroutines.delay(4000); open = null } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        for (item in running) Row(verticalAlignment = Alignment.Top) {
            if (open == item) {
                BoosterInfo(item, vm.shopLeft(item), Modifier.padding(end = 6.dp, top = 2.dp).clickable { open = null })
            }
            val left = vm.shopLeft(item)
            val chunk = item.durationSec * kotlin.math.ceil(left / item.durationSec)
            val frac = (left / chunk).toFloat().coerceIn(0f, 1f)
            val ring = when (item) { ShopItem.COCOA -> Color(0xFFF28C8C); ShopItem.TURBO -> Color(0xFFFFB86B); else -> Color(0xFFFFC85C) }
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
                Text("Rescue a traveller from the snow. Each one joins the crew and adds +40% coins.", fontSize = 11.sp, color = Muted)
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
        if (sprites.survivors.isNotEmpty()) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            sprites.survivors.forEachIndexed { i, img ->
                Image(img, null, Modifier.size(28.dp, 35.dp).graphicsLayer { alpha = if (i < lvl) 1f else 0.25f })
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
private fun BoosterInfo(item: ShopItem, left: Double, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = 190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(2.dp, Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(item.desc, fontSize = 11.sp, color = Muted)
        Text("Time left: ${fmtClock(left)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldText)
    }
}


// ---------- player level ----------

private val XpPurple = Color(0xFF8E6FD8)

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
            drawArc(XpPurple, -90f, 360f * frac, false, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Ink, style = Stroke(2.dp.toPx()))
        }
        Text("${vm.playerLevel}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
    }
}

/** After evolving: which train you got, with a picture of it. */
@Composable
private fun NewTrainPopup(vm: GameViewModel) {
    var seen by remember { mutableIntStateOf(vm.evolveSerial) }
    if (vm.evolveSerial == seen) return
    val idx = vm.tierIndex
    val tier = TIERS[idx]
    CozyCard("New train!", Color(0xFFDCCBFA), onDismiss = { seen = vm.evolveSerial }) {
        Text("Your train evolved into", fontSize = 12.sp, color = Muted)
        Text(tier.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(8.dp))
        TrainPicture(idx, Modifier.fillMaxWidth(0.62f).aspectRatio(0.62f))
        Spacer(Modifier.height(10.dp))
        Text(
            "Speed +${(Balance.TIER_SPEED_BONUS * idx * 100).toInt()}%  ·  Coins x${fmt(Balance.TIER_INCOME.pow(idx), 1)}\nUp to ${vm.maxCars} cars  ·  +${vm.cores} Frost Cores in total",
            fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        BigButton("All aboard!", { seen = vm.evolveSerial }, Modifier.fillMaxWidth(0.7f))
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
        val t = tierIndex.coerceIn(0, 5)
        for (c in 0 until 2) {
            val top = 52f + c * 126f
            drawRoundRect(Color(0xFFEFE6D8), Offset(ox + 8f * k, oy + (top + 8f) * k), Size(190f * k, 100f * k), CornerRadius(8f * k))
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
private fun RailButton(img: androidx.compose.ui.graphics.ImageBitmap?, label: String, bg: Color, badge: Int = 0, glow: Boolean = false, onClick: () -> Unit) {
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
                if (img != null) Image(img, label, Modifier.size(30.dp))
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
private fun LeftRail(vm: GameViewModel, onQuests: () -> Unit, onAwards: () -> Unit, modifier: Modifier = Modifier) {
    val sp = rememberGameSprites()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RailButton(sp.iconQuest, "Quests", Color(0xFFE2F3EA), badge = vm.questsReady, glow = vm.questsReady > 0, onClick = onQuests)
        RailButton(sp.iconTrophy, "Awards", Color(0xFFFFF0C8), onClick = onAwards)
        RailButton(sp.iconGift, if (vm.dailyReady) "Ready!" else vm.nextGiftIn, Color(0xFFFFE1E1), badge = if (vm.dailyReady) 1 else 0, glow = vm.dailyReady, onClick = vm::giftHint)
    }
}

@Composable
private fun RightRail(vm: GameViewModel, link: SceneLink, onEvolve: () -> Unit) {
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
        // mini-map of the train: tap a car to fly the camera to it, tap MAP for the full train map
        val cars = vm.carsNow
        val cell = if (cars > 8) 16.dp else 20.dp
        var showMap by remember { mutableStateOf(false) }
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
                            Text("${i + 1}", fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
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
        if (showMap) TrainMapCard(vm, onPick = { link.jumpCar = it; showMap = false }, onClose = { showMap = false })
    }
}

/** The whole train at a glance: every car with its devices; tap a car to look at it. */
@Composable
private fun TrainMapCard(vm: GameViewModel, onPick: (Int) -> Unit, onClose: () -> Unit) {
    val sp = rememberGameSprites()
    val cars = vm.carsNow
    CozyCard("Train map", Color(0xFFCDE7F8), onDismiss = onClose) {
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
        Spacer(Modifier.height(10.dp))
        BigButton("Close", onClose, Modifier.fillMaxWidth(0.6f))
    }
}

// ---------- daily quests ----------

@Composable
private fun QuestSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    val sp = rememberGameSprites()
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
                .background(Brush.horizontalGradient(listOf(Color(0xFFE2F3EA), Color(0xFFF1EAFF))))
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sp.iconQuest?.let { Image(it, null, Modifier.size(22.dp)) }
            Spacer(Modifier.width(6.dp))
            Text("Quests", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { UiSound.click(); onClose() }) { Text("Close", fontSize = 12.sp) }
        }
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
                Text(text, fontSize = 13.sp, color = Ink, textAlign = TextAlign.Center, lineHeight = 18.sp)
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
                ) { Text(button, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                if (onSkip != null) {
                    Text(
                        "Skip tutorial", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onSkip).padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center,
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
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    TutorialSteps(stage)
                }
                Text(text, fontSize = 12.sp, color = Ink, lineHeight = 16.sp)
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
            androidx.compose.ui.res.painterResource(R.drawable.splash_art), "Frost Express",
            Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
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
private fun CozyCard(title: String, ribbon: Color, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
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
                title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ribbon)
                    .border(3.dp, Ink, RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, colors: List<Color> = listOf(Color(0xFFFFC98A), Color(0xFFFFA65C))) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(colors))
            .border(2.5.dp, Ink, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center) }
}

@Composable
private fun DailyGiftCard(vm: GameViewModel) {
    val next = vm.dailyNextStreak
    val weekStart = ((next - 1) / 7) * 7 + 1
    val sp = rememberGameSprites()
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "daily").animateFloat(
        1f, 1.07f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "dailyPulse",
    )
    CozyCard("Daily Gift", Color(0xFFFFC9D6), onDismiss = {}) {
        Text("Day $next in a row", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
        Spacer(Modifier.height(10.dp))
        // a week of gifts: claimed days are ticked, today glows, the 7th day has a Frost Core
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                val days = if (row == 0) (0..3) else (4..6)
                for (k in days) {
                    val day = weekStart + k
                    val claimed = day < next
                    val today = day == next
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
        Text("Today", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
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
        BigButton("Claim my gift", vm::claimDaily, Modifier.fillMaxWidth(0.8f))
        Text("Come back tomorrow to keep your streak going.", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(Modifier.size(26.dp)); Spacer(Modifier.width(6.dp))
            Text("+${fmt(vm.offlineCoins)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GoldText)
        }
        Spacer(Modifier.height(12.dp))
        if (Balance.ADS_ENABLED) {
            BigButton(if (vm.adReady) "Watch an ad: double it!" else "Loading ad...", vm::doubleOfflineWithAd, Modifier.fillMaxWidth(0.85f), listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)))
            Spacer(Modifier.height(8.dp))
        }
        BigButton("Collect", vm::dismissOffline, Modifier.fillMaxWidth(0.85f))
    }
}
