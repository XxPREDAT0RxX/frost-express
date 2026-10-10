package com.chetna.frostexpress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "New feature!" card: shown once when a side feature opens with the player level. */
@Composable
internal fun FeatureIntroCard(vm: GameViewModel, f: Feature, onOpen: (Feature) -> Unit) {
    CozyCard("New feature!", Color(0xFFFFE49A), onDismiss = { vm.dismissFeature(f) }) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFF6D6), Color(0xFFFFD98A)))).border(3.dp, Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (f) {
                Feature.WHEEL -> Text("🎡", fontSize = 34.sp)
                Feature.EVENTS -> FlakeIcon(Modifier.size(40.dp))
                Feature.JOURNEY -> rememberGameSprites().iconTrophy?.let { androidx.compose.foundation.Image(it, null, Modifier.size(42.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(f.title, fontSize = 20.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text("Unlocked at level ${f.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpPurple)
        Spacer(Modifier.height(6.dp))
        Text(f.desc, fontSize = 13.sp, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        BigButton("Show me", { vm.dismissFeature(f); onOpen(f) }, Modifier.fillMaxWidth(0.7f))
        Spacer(Modifier.height(6.dp))
        Text("Later", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.clickable { vm.dismissFeature(f) }.padding(8.dp))
    }
}

/** After evolving: pick one bonus for this run. */
@Composable
internal fun RouteChoiceCard(vm: GameViewModel) {
    if (vm.routeChoices.isEmpty() || vm.newTrainPending) return
    CozyCard("Choose your route", Color(0xFFCDE7F8), onDismiss = {}) {
        Text("Pick one bonus for this run. It lasts until your next evolve.", fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            vm.routeChoices.toList().forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(2.dp, Ink, RoundedCornerShape(16.dp))
                        .clickable { UiSound.click(); vm.pickRoute(r) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(routeEmoji(r), fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                        Text(r.desc, fontSize = 12.sp, color = Muted)
                    }
                    Text("Pick", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Gold).border(1.5.dp, Ink, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

internal fun routeEmoji(r: RouteBonus): String = when (r) {
    RouteBonus.GOLD_LINE -> "🪙"
    RouteBonus.FAST_TRACK -> "⚡"
    RouteBonus.LONG_BOOST -> "🔥"
    RouteBonus.BUSY_STATIONS -> "🚉"
    RouteBonus.CHEAP_PARTS -> "🔧"
    RouteBonus.NIGHT_SHIFT -> "🌙"
}

/** A small card for today's special. */
@Composable
internal fun SpecialCard(vm: GameViewModel, modifier: Modifier = Modifier) {
    val s = vm.special
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF0C8)).border(2.dp, Ink, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("☀️", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Today's special: ${s.title}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(s.desc + " · a new one every day", fontSize = 11.sp, color = Muted)
        }
    }
}

/** The parcel waiting after a long time away. */
@Composable
internal fun ParcelBox(vm: GameViewModel) {
    val r = vm.parcelReward
    if (!vm.parcelReady && r == null) return
    Column(
        Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFE1C4)).border(2.dp, Ink, RoundedCornerShape(16.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (r == null) {
            Text("📦 A parcel was left for you at a station!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            BigButton("Open parcel", vm::openParcel, Modifier.fillMaxWidth(0.8f), listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)))
        } else {
            Text("📦 Inside the parcel:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
            Text(vm.rewardValue(r), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GoldText, textAlign = TextAlign.Center)
            Text("Come back after a while for another one", fontSize = 10.sp, color = Muted)
        }
    }
}
