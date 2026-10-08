package com.chetna.frostexpress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Ink = Color(0xFF0E1A24)
private val Panel = Color(0xFF182836)
private val Ice = Color(0xFF7FD8FF)
private val Ember = Color(0xFFFF9F43)
private val Muted = Color(0xFF8FA6B8)

@Composable
fun FrostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ice,
            onPrimary = Ink,
            background = Ink,
            surface = Panel,
            onSurface = Color(0xFFEAF4FA),
            onBackground = Color(0xFFEAF4FA),
            secondary = Muted,
        ),
        content = content,
    )
}

@Composable
fun GameScreen(vm: GameViewModel) {
    var showUpgrades by remember { mutableStateOf(false) }
    BackHandler(enabled = showUpgrades) { showUpgrades = false }

    Box(Modifier.fillMaxSize().background(Ink)) {
        // The scene fills the whole screen; the HUD floats on top of it.
        TrainScene(vm, Modifier.fillMaxSize())

        TopHud(
            vm,
            Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        BottomBar(
            vm,
            onOpenUpgrades = { showUpgrades = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        AnimatedVisibility(
            visible = showUpgrades,
            enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(),
        ) {
            UpgradesPage(vm, onBack = { showUpgrades = false })
        }
    }

    if (vm.offlineCoins > 0.0) {
        AlertDialog(
            onDismissRequest = vm::dismissOffline,
            title = { Text("Welcome back!") },
            text = {
                Text("Your train travelled ${fmtDistance(vm.offlineMeters)} while you were away and earned ${fmt(vm.offlineCoins)} coins.")
            },
            confirmButton = { TextButton(onClick = vm::dismissOffline) { Text("Collect") } },
        )
    }
}

@Composable
private fun TopHud(vm: GameViewModel, modifier: Modifier = Modifier) {
    val progress = (vm.runDistance / vm.evolveDistance).toFloat().coerceIn(0f, 1f)
    Column(
        modifier
            .fillMaxWidth()
            .background(Ink.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("COINS", fontSize = 10.sp, letterSpacing = 3.sp, color = Muted)
                Text(fmt(vm.coins), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ice)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmtDistance(vm.runDistance), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("${fmt(vm.speed, 1)} m/s", fontSize = 12.sp, color = Muted)
                Text("${fmt(vm.coinsPerSec, 1)} coins/s", fontSize = 12.sp, color = Muted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TIERS[vm.tierIndex].name, fontSize = 12.sp, color = Muted)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(Panel, RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Ice, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun BottomBar(vm: GameViewModel, onOpenUpgrades: () -> Unit, modifier: Modifier = Modifier) {
    val fill = vm.boostMeter.toFloat().coerceIn(0f, 1f)
    val status = when {
        vm.boostLocked -> "Recharging ${(fill * 100).toInt()}% · locked until full"
        vm.isBoosting -> "Boost x${fmt(vm.boostMultiplier, 1)} active · ${vm.boostTimeLeft.toInt() + 1}s"
        else -> "Tap BOOST: x${fmt(vm.boostMultiplier, 1)} income, 3s per tap"
    }
    val canBuySomething = UPGRADES.any { spec ->
        vm.level(spec.type) < spec.maxLevel && vm.coins >= vm.upgradeCost(spec)
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(Ink.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(Panel, CircleShape)
                .clickable(onClick = onOpenUpgrades),
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(Ice, Modifier.size(28.dp))
            if (canBuySomething) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(12.dp)
                        .background(Ember, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Panel, RoundedCornerShape(6.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(if (vm.boostLocked) Muted else Ember, RoundedCornerShape(6.dp))
                )
            }
            Text(status, fontSize = 11.sp, color = Muted)
        }
        if (Balance.ADS_ENABLED) {
            Spacer(Modifier.width(8.dp))
            Button(onClick = vm::watchAdBoost, enabled = vm.adBoostLeft <= 0.0) {
                Text(if (vm.adBoostLeft > 0.0) "x2 ${vm.adBoostLeft.toInt()}s" else "AD x2")
            }
        }
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = vm::boost,
            enabled = !vm.boostLocked,
            colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Ink),
        ) {
            Text("BOOST", fontWeight = FontWeight.ExtraBold)
        }
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
private fun UpgradesPage(vm: GameViewModel, onBack: () -> Unit) {
    var confirmEvolve by remember { mutableStateOf(false) }

    Surface(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}, // swallow touches so the scene below is not clickable
        color = Ink,
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("← Back", fontSize = 16.sp) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                    Text("COINS", fontSize = 10.sp, letterSpacing = 3.sp, color = Muted)
                    Text(fmt(vm.coins), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ice)
                }
            }
            Text(
                "Upgrades",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { BuyAmountRow(vm) }
                items(UPGRADES) { spec ->
                    val lvl = vm.level(spec.type)
                    val maxed = lvl >= spec.maxLevel
                    val n = vm.buyCount(spec)
                    val cost = vm.bulkCost(spec, n)
                    val next = vm.nextMilestone(spec.type)
                    val mult = vm.milestoneMult(spec.type)
                    UpgradeRow(
                        title = spec.title,
                        subtitle = "Lv $lvl${if (spec.maxLevel != Int.MAX_VALUE) "/${spec.maxLevel}" else ""} · ${spec.effect}" +
                            (if (mult > 1.0) "\nMilestone bonus x${fmt(mult)}" else "") +
                            (if (next != null) "\nNext x2 bonus at Lv $next" else ""),
                        buttonText = if (maxed) "MAX" else "+$n\n${fmt(cost)}",
                        enabled = !maxed && vm.coins >= cost,
                        onBuy = { vm.buy(spec) },
                    )
                }
                item { EvolveCard(vm, onEvolve = { confirmEvolve = true }) }
            }
        }
    }

    if (confirmEvolve) {
        AlertDialog(
            onDismissRequest = { confirmEvolve = false },
            title = { Text("Evolve your train?") },
            text = {
                Text("Coins and upgrades will reset. You will get ${vm.coreGain} Frost Core(s) (permanent bonus) and unlock the next train.")
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

@Composable
private fun BuyAmountRow(vm: GameViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1 to "x1", 10 to "x10", 100 to "x100", 0 to "MAX").forEach { (amount, label) ->
            val on = vm.buyAmount == amount
            Box(
                Modifier
                    .weight(1f)
                    .background(if (on) Ice else Panel, RoundedCornerShape(10.dp))
                    .clickable { vm.buyAmount = amount }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = if (on) Ink else Color.White)
            }
        }
    }
}

@Composable
private fun UpgradeRow(
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onBuy: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 13.sp, color = Muted)
            }
            Button(onClick = onBuy, enabled = enabled) { Text(buttonText) }
        }
    }
}

@Composable
private fun EvolveCard(vm: GameViewModel, onEvolve: () -> Unit) {
    val next = TIERS[min(vm.evolutions + 1, TIERS.lastIndex)]
    val progress = (vm.runDistance / vm.evolveDistance).toFloat().coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Evolve", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Current: ${TIERS[vm.tierIndex].name} (Tier ${vm.tierIndex + 1}) · Frost Cores: ${vm.cores} (+${vm.cores * 25}% coins)",
                fontSize = 13.sp,
                color = Muted,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Ink, RoundedCornerShape(5.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Ice, RoundedCornerShape(5.dp))
                )
            }
            Text(
                "${fmtDistance(vm.runDistance)} / ${fmtDistance(vm.evolveDistance)}",
                fontSize = 12.sp,
                color = Muted,
            )
            Button(onClick = onEvolve, enabled = vm.canEvolve, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.canEvolve) "Evolve to ${next.name}  (+${vm.coreGain} cores)" else "Travel further to evolve")
            }
        }
    }
}
