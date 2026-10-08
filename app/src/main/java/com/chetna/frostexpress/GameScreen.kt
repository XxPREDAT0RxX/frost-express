package com.chetna.frostexpress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Header(vm)
            TrainScene(vm, Modifier.fillMaxWidth().height(220.dp))
            BoostBar(vm)
            Upgrades(vm, Modifier.weight(1f))
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
private fun Header(vm: GameViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("COINS", fontSize = 11.sp, letterSpacing = 3.sp, color = Muted)
            Text(fmt(vm.coins), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Ice)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fmtDistance(vm.runDistance), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("${fmt(vm.speed, 1)} m/s", fontSize = 13.sp, color = Muted)
            Text("${fmt(vm.coinsPerSec, 1)} coins/s", fontSize = 13.sp, color = Muted)
        }
    }
}

@Composable
private fun BoostBar(vm: GameViewModel) {
    val fill = vm.boostMeter.toFloat().coerceIn(0f, 1f)
    val status = when {
        vm.boostLocked -> "Recharging ${(fill * 100).toInt()}% · boost locked until full"
        vm.isBoosting -> "Boost x${fmt(vm.boostMultiplier, 1)} active · ${vm.boostTimeLeft.toInt() + 1}s"
        else -> "Tap BOOST: x${fmt(vm.boostMultiplier, 1)} income, 3s per tap"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(Panel, RoundedCornerShape(8.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(if (vm.boostLocked) Muted else Ember, RoundedCornerShape(8.dp))
                )
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
        Text(status, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun Upgrades(vm: GameViewModel, modifier: Modifier = Modifier) {
    var confirmEvolve by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(UPGRADES) { spec ->
            val lvl = vm.level(spec.type)
            val maxed = lvl >= spec.maxLevel
            val cost = vm.upgradeCost(spec)
            UpgradeRow(
                title = spec.title,
                subtitle = "Lv $lvl${if (spec.maxLevel != Int.MAX_VALUE) "/${spec.maxLevel}" else ""} · ${spec.effect}",
                buttonText = if (maxed) "MAX" else fmt(cost),
                enabled = !maxed && vm.coins >= cost,
                onBuy = { vm.buy(spec) },
            )
        }
        item { EvolveCard(vm, onEvolve = { confirmEvolve = true }) }
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
