package com.chetna.frostexpress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Region names in the order the train passes them (same order as the scene's regions). */
val REGION_NAMES = listOf("Frozen Forest", "Buried Town", "Ice Fields", "Old Wind Farm", "Frozen Harbour", "Survivor Camps")
private val REGION_SPRITES = listOf(
    listOf(R.drawable.spr_frost_pine_a, R.drawable.spr_frost_pine_b, R.drawable.spr_dead_a),
    listOf(R.drawable.spr_ruin_a, R.drawable.spr_car, R.drawable.spr_lamp),
    listOf(R.drawable.spr_ice, R.drawable.spr_cliff, R.drawable.spr_rock_a),
    listOf(R.drawable.spr_turbine, R.drawable.spr_tank, R.drawable.spr_crane),
    listOf(R.drawable.spr_lighthouse, R.drawable.spr_ship, R.drawable.spr_buoy),
    listOf(R.drawable.spr_tent, R.drawable.spr_campfire, R.drawable.spr_frost_pine_a),
)
private val REGION_SKY = listOf(0xFFCDEBDC, 0xFFFFE0CC, 0xFFD6ECFA, 0xFFFFF0C8, 0xFFCFE3F6, 0xFFFFD9E2)

// ---------------- small shared pieces ----------------

@Composable
private fun RewardIcon(r: Reward, modifier: Modifier = Modifier) {
    when (r) {
        is Reward.Coins -> CoinIcon(modifier)
        is Reward.Gems -> GemIcon(modifier)
        is Reward.Item -> ItemIcon(r.item, modifier)
        is Reward.Flakes -> FlakeIcon(modifier)
        is Reward.Polish -> PolishIcon(modifier)
        is Reward.Paint -> {
            val l = LIVERIES.getOrNull(r.livery)
            Canvas(modifier) {
                drawCircle(Color(l?.body ?: 0xFFB0578A))
                drawCircle(Color(l?.accent ?: 0xFFFFFFFF), size.minDimension * 0.22f)
                drawCircle(Ink, style = Stroke(1.5.dp.toPx()))
            }
        }
        is Reward.Passengers -> Canvas(modifier) {
            val s = size.minDimension
            drawCircle(Color(0xFFB0578A), s * 0.2f, Offset(s * 0.5f, s * 0.28f))
            drawRoundRect(Color(0xFFB0578A), Offset(s * 0.2f, s * 0.52f), Size(s * 0.6f, s * 0.42f), androidx.compose.ui.geometry.CornerRadius(s * 0.25f))
        }
    }
}

/** A snowflake: the weekly event currency. */
@Composable
internal fun FlakeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.46f
        for (i in 0 until 6) {
            val a = i * PI.toFloat() / 3f
            val e = c + Offset(cos(a), sin(a)) * r
            drawLine(Color(0xFF6B4A3A), c, e, size.minDimension * 0.2f, StrokeCap.Round)
        }
        for (i in 0 until 6) {
            val a = i * PI.toFloat() / 3f
            val e = c + Offset(cos(a), sin(a)) * r
            drawLine(Color(0xFF9FD3EE), c, e, size.minDimension * 0.1f, StrokeCap.Round)
        }
        drawCircle(Color.White, size.minDimension * 0.12f)
    }
}

/** Gold Polish: makes one device golden. */
@Composable
internal fun PolishIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawRoundRect(Ink, Offset(s * 0.22f, s * 0.3f), Size(s * 0.56f, s * 0.64f), androidx.compose.ui.geometry.CornerRadius(s * 0.14f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE7A0), Color(0xFFF2B84B))), Offset(s * 0.27f, s * 0.35f), Size(s * 0.46f, s * 0.54f), androidx.compose.ui.geometry.CornerRadius(s * 0.1f))
        drawRect(Ink, Offset(s * 0.38f, s * 0.12f), Size(s * 0.24f, s * 0.2f))
        drawLine(Color.White, Offset(s * 0.36f, s * 0.45f), Offset(s * 0.36f, s * 0.7f), s * 0.07f, StrokeCap.Round)
    }
}

@Composable
private fun TabSwitch(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, badges: List<Int> = emptyList()) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PanelHi)
            .border(2.dp, Ink, RoundedCornerShape(16.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = selected == i
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (on) Brush.verticalGradient(listOf(Color.White, Color(0xFFE3D6FB))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(if (on) 2.dp else 0.dp, if (on) Ink else Color.Transparent, RoundedCornerShape(13.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Ink else Muted, maxLines = 1, softWrap = false)
                    val b = badges.getOrElse(i) { 0 }
                    if (b > 0) { Spacer(Modifier.width(4.dp)); NotifyBadge(b) }
                }
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, bg: Color = Color.White.copy(alpha = 0.72f), content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(2.dp, Ink, RoundedCornerShape(18.dp)).padding(10.dp),
        content = content,
    )
}

// ---------------- Events: the weekly event and the Frost Pass ----------------

@Composable
fun EventsSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val ev = currentEvent()
    SheetFrame(
        "Events", rememberGameSprites().iconGift, Color(ev.color), onClose, modifier,
        right = { CurrencyPill({ FlakeIcon(Modifier.size(15.dp)) }, "${vm.meta.flakes}", Ice) },
    ) {
        TabSwitch(listOf(ev.name, "Frost Pass"), tab, { tab = it }, listOf(0, vm.meta.passReady))
        if (tab == 0) EventTab(vm, ev) else PassTab(vm)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EventTab(vm: GameViewModel, ev: WeeklyEvent) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(bg = Color(ev.color).copy(alpha = 0.35f)) {
                Text(ev.name, fontSize = 20.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                Text(ev.desc, fontSize = 12.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("Ends in ${eventTimeLeft()} · snowflakes come from stations, mini-games, the wheel and the pass", fontSize = 10.sp, color = Muted)
            }
        }
        item { SectionTitle("Snowflake shop", Ice, "This week's paint leaves on Monday") }
        items(flakeShop(ev)) { o ->
            val owned = o.reward is Reward.Paint && (o.title in vm.meta.boughtOffers || (o.reward as Reward.Paint).livery in vm.meta.paints)
            val can = !owned && vm.meta.flakes >= o.cost
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.72f)).border(2.dp, Ink, RoundedCornerShape(16.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color(ev.color).copy(alpha = 0.4f)).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                    RewardIcon(o.reward, Modifier.size(28.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(o.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                ChunkyButton(can, listOf(Color(0xFFD3E9FA), Color(0xFF9FC5EA)), { vm.buyFlakeOffer(o) }, Modifier.width(84.dp)) {
                    if (owned) Text("Owned", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        FlakeIcon(Modifier.size(13.dp)); Spacer(Modifier.width(3.dp))
                        Text("${o.cost}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (can) Ink else Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PassTab(vm: GameViewModel) {
    val m = vm.meta
    val tier = m.passTier
    val inTier = if (tier >= PASS_TIERS) 1f else (m.passStars % PASS_STARS_PER_TIER) / PASS_STARS_PER_TIER.toFloat()
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Card(bg = Color(0xFFE3D6FB).copy(alpha = 0.6f)) {
                Text(passSeasonName(), fontSize = 18.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                Text("Tier $tier of $PASS_TIERS · ${passDaysLeft()} days left · earn stars from quests, stations, crates, stories and mini-games", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Track).border(1.5.dp, Ink, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(inTier).background(Brush.horizontalGradient(listOf(Color(0xFFB8D8F2), Color(0xFFB9A2EE)))))
                    Text(if (tier >= PASS_TIERS) "Complete!" else "${m.passStars % PASS_STARS_PER_TIER} / $PASS_STARS_PER_TIER stars", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                }
                if (!m.passPremium) {
                    Spacer(Modifier.height(8.dp))
                    Text("Unlock the Premium track for this month", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChunkyButton(Balance.IAP_ENABLED, listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), vm::buyPassWithMoney, Modifier.weight(1f)) {
                            Text(if (Balance.IAP_ENABLED) (vm.passPrice() ?: "Buy") else "Buy · soon", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (Balance.IAP_ENABLED) Ink else Muted, maxLines = 1, softWrap = false)
                        }
                        ChunkyButton(vm.gems >= PASS_PREMIUM_GEMS, listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)), vm::buyPremiumPass, Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GemIcon(Modifier.size(14.dp)); Text(" $PASS_PREMIUM_GEMS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            }
                        }
                    }
                    if (!Balance.IAP_ENABLED) Text("Paying with money opens after the game launches on Google Play.", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        items((1..PASS_TIERS).toList()) { t ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (t <= tier) Color(0xFFB9A2EE) else PanelHi).border(2.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$t", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (t <= tier) Color.White else Muted) }
                Spacer(Modifier.width(6.dp))
                PassCell(vm, t, false, Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                PassCell(vm, t, true, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PassCell(vm: GameViewModel, t: Int, premium: Boolean, modifier: Modifier) {
    val m = vm.meta
    val r = if (premium) passPremium(t) else passFree(t)
    val claimed = t in (if (premium) m.passClaimedPremium else m.passClaimedFree)
    val ready = !claimed && t <= m.passTier && (!premium || m.passPremium)
    val locked = premium && !m.passPremium
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (ready) Color(0xFFFFF3D6) else if (premium) Color(0xFFFFF0C8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f))
            .border(if (ready) 2.dp else 1.5.dp, if (ready) Ember else Ink.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(enabled = ready) { vm.claimPass(t, premium) }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).graphicsLayer { alpha = if (locked) 0.4f else 1f }) { RewardIcon(r, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(5.dp))
        Text(
            if (claimed) "Claimed" else r.label(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (claimed || locked) Muted else Ink, maxLines = 2, lineHeight = 12.sp, modifier = Modifier.weight(1f),
        )
        if (locked) LockIcon(Muted, Modifier.size(12.dp))
    }
}

// ---------------- Lucky wheel ----------------

@Composable
fun WheelDialog(vm: GameViewModel, onClose: () -> Unit) {
    val rot = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableIntStateOf(-1) }
    var target by remember { mutableIntStateOf(-1) }
    LaunchedEffect(target) {
        if (target < 0) return@LaunchedEffect
        spinning = true
        val n = WHEEL.size
        val sliceDeg = 360f / n
        // the pointer is at the top; bring the middle of the chosen slice under it
        val end = rot.value - (rot.value % 360f) + 360f * 5 + (360f - (target * sliceDeg + sliceDeg / 2f))
        var lastTick = -1
        rot.animateTo(end, tween(3600, easing = FastOutSlowInEasing)) {
            val tick = (value / sliceDeg).toInt()
            if (tick != lastTick) { lastTick = tick; UiSound.click() }
        }
        result = target
        target = -1
        spinning = false
        Haptics.confirm()
    }
    CozyCard("Lucky Wheel", Color(0xFFFFE49A), onDismiss = { if (!spinning) onClose() }, closable = true) {
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot.value }) {
                val n = WHEEL.size
                val sweep = 360f / n
                val r = size.minDimension / 2f
                WHEEL.forEachIndexed { i, s ->
                    drawArc(Color(s.color), -90f + i * sweep, sweep, true, Offset(size.width / 2 - r, size.height / 2 - r), Size(r * 2, r * 2))
                    drawArc(Ink, -90f + i * sweep, sweep, true, Offset(size.width / 2 - r, size.height / 2 - r), Size(r * 2, r * 2), style = Stroke(2.dp.toPx()))
                }
                drawCircle(Ink, r - 1.5.dp.toPx(), style = Stroke(4.dp.toPx()))
            }
            // icons ride on the wheel
            WHEEL.forEachIndexed { i, s ->
                val a = Math.toRadians((-90.0 + (i + 0.5) * 360.0 / WHEEL.size + rot.value)).toFloat()
                Box(Modifier.offset(x = (cos(a) * 82).dp, y = (sin(a) * 82).dp).size(30.dp)) { RewardIcon(s.reward, Modifier.fillMaxSize()) }
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel).border(3.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("SPIN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            }
            // pointer
            Canvas(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(26.dp)) {
                val p = Path().apply { moveTo(size.width * 0.2f, 0f); lineTo(size.width * 0.8f, 0f); lineTo(size.width * 0.5f, size.height); close() }
                drawPath(p, Color(0xFFE5484D))
                drawPath(p, Ink, style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (result >= 0) "You won: ${vm.rewardValue(WHEEL[result].reward)}" else "Spin for coins, gems, boosters and Gold Polish",
            fontSize = 13.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = if (result >= 0) GoldText else Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        val start = { kind: Int -> if (!spinning) { result = -1; vm.spinWheel(kind) { target = it } } }
        ChunkyButton(!spinning && vm.canSpin(0), listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)), { start(0) }, Modifier.fillMaxWidth(0.85f)) {
            Text(if (vm.meta.wheelFreeReady) "Free spin" else "Free spin used today", fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
        }
        if (Balance.ADS_ENABLED) ChunkyButton(!spinning && vm.canSpin(1), listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), { start(1) }, Modifier.fillMaxWidth(0.85f)) {
            Text("Watch an ad to spin (${WHEEL_AD_SPINS - vm.meta.wheelAdsUsed} left)", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        }
        ChunkyButton(!spinning && vm.canSpin(2), listOf(Color(0xFFFFC2DA), Color(0xFFF08DB5)), { start(2) }, Modifier.fillMaxWidth(0.85f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spin for ", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                GemIcon(Modifier.size(14.dp)); Text(" $WHEEL_GEM_SPIN", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            }
        }
    }
}

// ---------------- Journey: route, album, style, friends ----------------

@Composable
fun JourneySheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    SheetFrame("Journey", rememberGameSprites().iconTrophy, Color(0xFFCDE7F8), onClose, modifier) {
        TabSwitch(listOf("Route", "Album", "Style", "Friends"), tab, { tab = it })
        when (tab) {
            0 -> RouteTab(vm)
            1 -> AlbumTab(vm)
            2 -> StyleTab(vm)
            else -> FriendsTab(vm)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RouteTab(vm: GameViewModel) {
    val km = vm.totalDistance / 1000.0
    val next = JOURNEY.indexOfFirst { it.km > km }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        item {
            Card(bg = Color(0xFFFFF0C8).copy(alpha = 0.7f)) {
                val ch2 = km >= JOURNEY[WARM_CITY].km
                val goal = if (ch2) JOURNEY.last() else JOURNEY[WARM_CITY]
                Text(if (ch2) "Chapter 2: Bring them home" else "Chapter 1: To the Warm City", fontSize = 18.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    if (ch2) "The Warm City sends you back north to find everyone still out in the cold. Warm City bonus: +${(WARM_CITY_BONUS * 100).toInt()}% coins forever."
                    else "Rumours say the snow melts in the south. Reaching the Warm City gives +${(WARM_CITY_BONUS * 100).toInt()}% coins forever.",
                    fontSize = 12.sp, color = Muted,
                )
                Text("${fmt(km, 1)} km of ${goal.km} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Track).border(1.2.dp, Ink, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((km / goal.km).toFloat().coerceIn(0f, 1f)).background(Ember))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        items(JOURNEY.indices.toList()) { i ->
            val leg = JOURNEY[i]
            val done = km >= leg.km
            val isNext = i == next
            if (i == WARM_CITY + 1) SectionTitle("Chapter 2: Bring them home", XpPurple)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Box(Modifier.width(34.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val x = size.width / 2
                        if (i > 0) drawLine(if (done) Ember else Track, Offset(x, 0f), Offset(x, size.height / 2), 6.dp.toPx())
                        if (i < JOURNEY.lastIndex) drawLine(if (km >= JOURNEY[i + 1].km) Ember else Track, Offset(x, size.height / 2), Offset(x, size.height), 6.dp.toPx())
                        drawCircle(if (done) Ember else if (isNext) Color(0xFFFFE49A) else Color.White, 11.dp.toPx())
                        drawCircle(Ink, 11.dp.toPx(), style = Stroke(2.dp.toPx()))
                    }
                    Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Color.White else Ink)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(leg.name, fontSize = if (i == WARM_CITY || i == JOURNEY.lastIndex) 16.sp else 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = if (done || isNext) Ink else Muted)
                    Text(
                        if (done) "Arrived" else if (isNext) "${fmt(leg.km - km, 1)} km to go" else "${leg.km} km",
                        fontSize = 11.sp, color = if (isNext) GoldText else Muted, fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GemIcon(Modifier.size(13.dp)); Spacer(Modifier.width(3.dp))
                    Text("${leg.gems}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Muted else GemPink)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.AlbumTab(vm: GameViewModel) {
    val sp = rememberGameSprites()
    var card by remember { mutableIntStateOf(-1) }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Tap sparkles in the snow to find keepsakes. Each full set: +${(SET_BONUS * 100).toInt()}% coins forever. Sets done: ${vm.meta.setsDone} / 6.",
                fontSize = 11.sp, color = Muted,
            )
        }
        item {
            SectionTitle("Postcards", Ice, "${vm.meta.postcards.size} / 6 regions visited")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in REGION_NAMES.indices) {
                    val got = r in vm.meta.postcards
                    Box(Modifier.width(120.dp).clickable(enabled = got) { card = r }.graphicsLayer { alpha = if (got) 1f else 0.4f }) {
                        PostcardArt(r, Modifier.fillMaxWidth())
                        if (!got) Box(Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(Panel).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) { LockIcon(Muted, Modifier.size(16.dp)) }
                    }
                }
            }
        }
        items(REGION_NAMES.indices.toList()) { r ->
            val n = (0 until 6).count { vm.meta.hasItem(r, it) }
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(REGION_NAMES[r], fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(if (n == 6) "Set complete!" else "$n / 6", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (n == 6) Mint else Muted)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until 6) {
                        val got = vm.meta.hasItem(r, i)
                        Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(42.dp).graphicsLayer { alpha = if (got) 1f else 0.22f }) {
                                sp.keepsakes[r][i]?.let { Image(it, null, Modifier.fillMaxSize()) }
                            }
                            Text(if (got) COLLECTIBLE_NAMES[r][i] else "?", fontSize = 8.sp, lineHeight = 9.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
    if (card >= 0) PostcardDialog(card, vm.meta.postcards.size, onClose = { card = -1 })
}

/** A little picture of a region: its sky, snow and three of its landmarks. */
@Composable
fun PostcardArt(region: Int, modifier: Modifier = Modifier) {
    val imgs = REGION_SPRITES[region].map { androidx.compose.ui.graphics.ImageBitmap.imageResource(it) }
    Box(
        modifier.aspectRatio(1.5f).clip(RoundedCornerShape(10.dp)).background(Color(REGION_SKY[region])).border(2.dp, Ink, RoundedCornerShape(10.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawOval(Color.White, Offset(-w * 0.2f, h * 0.62f), Size(w * 1.4f, h * 0.8f))
            fun put(i: Int, x: Float, y: Float, s: Float) {
                val side = (h * s).toInt()
                drawImage(imgs[i], dstOffset = androidx.compose.ui.unit.IntOffset((w * x - side / 2f).toInt(), (h * y - side / 2f).toInt()), dstSize = androidx.compose.ui.unit.IntSize(side, side))
            }
            put(1, 0.72f, 0.5f, 0.75f)
            put(0, 0.3f, 0.52f, 0.62f)
            put(2, 0.52f, 0.66f, 0.42f)
        }
        Text(
            REGION_NAMES[region], fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(Panel.copy(alpha = 0.85f)).padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun PostcardDialog(region: Int, count: Int, onClose: () -> Unit) {
    CozyCard("Postcard", Color(REGION_SKY[region]), onDismiss = onClose, closable = true) {
        PostcardArt(region, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Greetings from ${REGION_NAMES[region]}", fontSize = 16.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text("Postcards collected: $count / 6", fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(10.dp))
        BigButton("Lovely!", onClose, Modifier.fillMaxWidth(0.6f))
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.StyleTab(vm: GameViewModel) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionTitle("Paint", Ember, "New paints come with evolving, events and the Frost Pass")
            LIVERIES.indices.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (i in row) {
                        val l = LIVERIES[i]
                        val open = vm.liveryUnlocked(i)
                        val on = vm.liveryIndex == i
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (i == 0) Color(TIERS[vm.tierIndex].body) else Color(l.body))
                                    .border(if (on) 3.5.dp else 2.dp, if (on) Ember else Ink, CircleShape)
                                    .clickable(enabled = open) { vm.chooseLivery(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!open) LockIcon(Color.White, Modifier.size(16.dp))
                                else Box(Modifier.size(12.dp).clip(CircleShape).background(if (i == 0) Color(TIERS[vm.tierIndex].accent) else Color(l.accent)))
                            }
                            Text(
                                if (open) l.name else if (i < 5) "Evolve $i" else if (i == 9) "Pass" else "Event",
                                fontSize = 9.sp, color = if (open) Ink else Muted, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("Horn", XpPurple, "Tap to hear it") }
        items(HORNS.indices.toList()) { i ->
            val h = HORNS[i]
            val owned = i in vm.meta.horns
            val on = vm.meta.horn == i
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.7f)).border(if (on) 2.5.dp else 1.5.dp, if (on) Ember else Ink, RoundedCornerShape(14.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(h.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                ChunkyButton(owned || vm.gems >= h.gems, listOf(Color(0xFFE3D6FB), Color(0xFFB9A2EE)), { vm.buyHorn(i) }, Modifier.width(96.dp)) {
                    if (owned) Text(if (on) "In use" else "Use", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    else Row(verticalAlignment = Alignment.CenterVertically) { GemIcon(Modifier.size(13.dp)); Text(" ${h.gems}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                }
            }
        }
        item { SectionTitle("Cosy decor", Mint, "Shows in every car") }
        items(CarDecor.values().toList()) { d ->
            val owned = d.name in vm.meta.decor
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, Ink, RoundedCornerShape(14.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(d.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(d.desc, fontSize = 11.sp, color = Muted)
                }
                ChunkyButton(!owned && vm.gems >= d.gems, listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), { vm.buyDecor(d) }, Modifier.width(84.dp)) {
                    if (owned) Text("Owned", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                    else Row(verticalAlignment = Alignment.CenterVertically) { GemIcon(Modifier.size(13.dp)); Text(" ${d.gems}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                }
            }
        }
        item {
            SectionTitle("Golden devices", GoldText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PolishIcon(Modifier.size(30.dp)); Spacer(Modifier.width(8.dp))
                Text(
                    "Gold Polish: ${vm.meta.polish}. Open any device and polish it: it turns golden and its effect grows x1.5. Golden now: ${vm.meta.golden.size}.",
                    fontSize = 11.sp, color = Muted,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FriendsTab(vm: GameViewModel) {
    val m = vm.meta
    var code by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionTitle("Gifts for friends", GemPink, "Your player id: ${m.playerId}")
            Card {
                Text("Make today's gift code and send it to a friend. Each code gives 5 gems to the friend who opens it. You get 2 gems for sharing once a day.", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(6.dp))
                if (code != null) {
                    Text(code!!, fontSize = 18.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                }
                ChunkyButton(true, listOf(Color(0xFFFFC2DA), Color(0xFFF08DB5)), {
                    val c = vm.makeGiftCode(); code = c
                    try { clip.setText(androidx.compose.ui.text.AnnotatedString(c)) } catch (e: Exception) { }
                }, Modifier.fillMaxWidth()) {
                    Text(if (code == null) "Make a gift code" else "Copied! Paste it to a friend", fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
        }
        item {
            Card {
                Text("Open a friend's gift", fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                Text("Up to 3 gifts a day. Codes work until the next day.", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Ink, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    if (input.isEmpty()) Text("FROST-XXXX-0000-XXXX", fontSize = 14.sp, color = Muted.copy(alpha = 0.6f))
                    BasicTextField(input, { input = it.uppercase().take(24) }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(6.dp))
                ChunkyButton(input.length >= 18, listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), { msg = vm.redeemCode(input); if (msg.startsWith("Gift opened")) input = "" }, Modifier.fillMaxWidth()) {
                    Text("Open gift", fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                }
                if (msg.isNotEmpty()) Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (msg.startsWith("Gift opened")) Mint else Ember, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item {
            SectionTitle("Personal records", Ice, if (Balance.PLAY_GAMES_ENABLED) "Global leaderboards in Google Play Games" else null)
            Card {
                val rows = listOf(
                    "Longest run" to fmtDistance(m.bestRun),
                    "Top speed" to "${fmt(m.bestSpeed, 1)} m/s",
                    "Most passengers" to "${m.bestPassengers}",
                    "Best BOOST combo" to "x${m.bestCombo}",
                    "Total distance" to fmtDistance(vm.totalDistance),
                    "Wolves chased off" to "${m.wolvesChased}",
                    "Mini-games played" to "${m.minigamesPlayed}",
                    "Trains evolved" to "${vm.evolutions}",
                )
                rows.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(k, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
                        Text(v, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                }
            }
        }
    }
}

// ---------------- survivor stories (inside the Quests sheet) ----------------

@Composable
fun StoryRows(vm: GameViewModel) {
    val sp = rememberGameSprites()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        STORIES.forEachIndexed { i, story ->
            val open = vm.storyUnlocked(i)
            val ch = vm.storyChapter(i)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (open) Color(0xFFFFF4E0) else PanelHi.copy(alpha = 0.5f)).border(2.dp, Ink.copy(alpha = if (open) 1f else 0.35f), RoundedCornerShape(16.dp)).padding(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.size(40.dp, 50.dp).graphicsLayer { alpha = if (open) 1f else 0.3f }) { sp.survivors.getOrNull(i)?.let { Image(it, null, Modifier.fillMaxSize()) } }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(story.who, fontSize = 13.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                        Text("Chapter ${minOf(vm.meta.storyStep[i] + 1, 3)} / 3", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
                    }
                    when {
                        !open -> Text("Rescue survivor ${i + 1} (Survivors upgrade) to start this story.", fontSize = 11.sp, color = Muted)
                        ch == null -> Text("Story complete. ${story.who.substringBefore(',')} is home on the train.", fontSize = 11.sp, color = Mint, fontWeight = FontWeight.Bold)
                        else -> {
                            Text(ch.text, fontSize = 11.sp, color = Ink, lineHeight = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            val prog = vm.storyProgress(i)
                            val done = prog >= ch.amount
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${ch.goal} · ${if (ch.stat == QuestStat.DISTANCE) fmtDistance(prog.toDouble()) else "$prog"} / ${if (ch.stat == QuestStat.DISTANCE) fmtDistance(ch.amount.toDouble()) else "${ch.amount}"}", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
                                    Box(Modifier.padding(top = 2.dp).fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(prog / ch.amount.toFloat()).background(Mint))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                ChunkyButton(done, listOf(Color(0xFFFFC98A), Color(0xFFFFA65C)), { vm.claimStory(i) }, Modifier.width(78.dp)) {
                                    Text(if (done) "Next" else ch.reward.label(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Ink else Muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- pop-ups: trader, SOS, finds, crits, the Warm City ----------------

@Composable
fun MetaPopups(vm: GameViewModel) {
    vm.trader?.let { deals ->
        CozyCard("Trader train", Color(0xFFFFD9B8), onDismiss = vm::dismissTrader, closable = true) {
            Text("\"Fresh from the south! Coins only, friend.\"", fontSize = 13.sp, fontFamily = CozyFont, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            deals.forEach { d ->
                val cost = vm.traderCost(d)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp)) { RewardIcon(d.reward, Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(8.dp))
                    Text(d.reward.label(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                    ChunkyButton(vm.coins >= cost, listOf(Color(0xFFFFE49A), Color(0xFFFFC85C)), { vm.acceptDeal(d) }, Modifier.width(100.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { CoinIcon(Modifier.size(12.dp)); Text(" ${fmt(cost)}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                    }
                }
            }
        }
    }
    if (vm.event == GameEvent.SOS) {
        CozyCard("SOS signal", Color(0xFFFFC9C4), onDismiss = { vm.answerSos(false) }) {
            Text("Flares in the snow! A small group is stranded beside the line.", fontSize = 13.sp, fontFamily = CozyFont, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            BigButton("Help them aboard", { vm.answerSos(true) }, Modifier.fillMaxWidth(0.85f))
            Spacer(Modifier.height(6.dp))
            BigButton("Keep going", { vm.answerSos(false) }, Modifier.fillMaxWidth(0.85f), listOf(Color(0xFFE6EEF5), Color(0xFFC9D8E6)))
        }
    }
    if (vm.warmCity) {
        CozyCard("The Warm City!", Color(0xFFFFE49A), onDismiss = vm::dismissWarmCity) {
            PostcardArt(5, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                "After 2,000 km of snow, the tracks run into green fields. The passengers cheer, Pip barks, and Old Tomas plays his violin. You made it.\n\nThe Warm City thanks you: +${(WARM_CITY_BONUS * 100).toInt()}% coins forever.\n\nChapter 2 begins: the mayor asks you to head back north and bring everyone else home. Six new stops lead to Home Station.",
                fontSize = 13.sp, fontFamily = CozyFont, color = Ink, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            BigButton("Start Chapter 2", vm::dismissWarmCity, Modifier.fillMaxWidth(0.85f))
        }
    }
    var card by remember { mutableIntStateOf(-1) }
    var seenCards by remember { mutableIntStateOf(vm.postcardSerial) }
    if (vm.postcardSerial != seenCards) { seenCards = vm.postcardSerial; if (vm.zone >= 0 && vm.meta.postcards.size > 1) card = vm.zone }
    if (card >= 0) PostcardDialog(card, vm.meta.postcards.size, onClose = { card = -1 })
}

/** Floating "+coins LUCKY!" when a crit lands and "Keepsake found" when one is picked up. */
@Composable
fun FloatingPops(vm: GameViewModel, modifier: Modifier = Modifier) {
    val crit = remember { Animatable(1f) }
    LaunchedEffect(vm.critSerial) { if (vm.critSerial > 0) { crit.snapTo(0f); crit.animateTo(1f, tween(1600, easing = LinearEasing)) } }
    val found = remember { Animatable(1f) }
    LaunchedEffect(vm.foundSerial) { if (vm.foundSerial > 0) { found.snapTo(0f); found.animateTo(1f, tween(2600, easing = LinearEasing)) } }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (crit.value < 1f) {
            val p = crit.value
            Text(
                "+${fmt(vm.critAmount)} LUCKY!", fontSize = 22.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = GoldText,
                modifier = Modifier.graphicsLayer {
                    translationY = -120f * p; alpha = if (p > 0.7f) (1f - p) / 0.3f else 1f
                    val sc = if (p < 0.15f) 0.6f + p / 0.15f * 0.5f else 1.1f - (p - 0.15f) * 0.12f
                    scaleX = sc; scaleY = sc
                }.clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF3C4).copy(alpha = 0.85f)).border(2.dp, Ink, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        if (found.value < 1f) {
            val p = found.value
            val sp = rememberGameSprites()
            val (r, i) = vm.meta.found.lastOrNull()?.split(":")?.let { it[0].toInt() to it[1].toInt() } ?: (0 to 0)
            Column(
                Modifier.offset(y = 90.dp).graphicsLayer { alpha = if (p > 0.8f) (1f - p) / 0.2f else 1f; translationY = -30f * p }
                    .clip(RoundedCornerShape(18.dp)).background(Panel.copy(alpha = 0.92f)).border(2.dp, Ink, RoundedCornerShape(18.dp)).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                sp.keepsakes.getOrNull(r)?.getOrNull(i)?.let { Image(it, null, Modifier.size(56.dp)) }
                Text("Keepsake found!", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                Text(vm.lastFound, fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }
}

// ---------------- mini-games ----------------

/** The pulsing "play" chip that offers a mini-game for a short while. */
@Composable
fun MinigamePrompt(vm: GameViewModel, modifier: Modifier = Modifier) {
    val g = vm.minigamePrompt ?: return
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "mg").animateFloat(
        0.94f, 1.06f, androidx.compose.animation.core.infiniteRepeatable(tween(500), androidx.compose.animation.core.RepeatMode.Reverse), label = "mgp",
    )
    Row(
        modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFE49A), Color(0xFFFFC85C))))
            .border(2.5.dp, Ink, RoundedCornerShape(20.dp)).clickable { vm.startMinigame() }.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(g.title, fontSize = 14.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.width(6.dp))
        Text("Play", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Ember).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun MinigameDialog(vm: GameViewModel) {
    val game = vm.activeMinigame ?: return
    val total = when (game) { MiniGame.SNOW -> 8f; MiniGame.COAL -> 6f; MiniGame.TICKETS -> 10f }
    var left by remember(game) { mutableFloatStateOf(total) }
    var score by remember(game) { mutableIntStateOf(0) }
    var started by remember(game) { mutableStateOf(false) }
    var over by remember(game) { mutableStateOf(false) }
    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        var last = 0L
        while (left > 0f) {
            androidx.compose.runtime.withFrameNanos { now -> if (last != 0L) left = (left - (now - last) / 1e9f).coerceAtLeast(0f); last = now }
        }
        over = true
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.fillMaxWidth(0.92f).widthIn(max = 420.dp).clip(RoundedCornerShape(26.dp)).background(Panel).border(3.dp, Ink, RoundedCornerShape(26.dp)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(game.title, fontSize = 22.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
            Text(game.hint, fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Score $score", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                Text("${kotlin.math.ceil(left).toInt()}s", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (left < 3f) Ember else Ink)
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Track)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(left / total).background(Ember))
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFFE6F0F8)).border(2.dp, Ink, RoundedCornerShape(18.dp))) {
                if (!started) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BigButton("Start!", { started = true }, Modifier.fillMaxWidth(0.5f))
                    }
                } else if (over) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Time!", fontSize = 26.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Score $score", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GoldText)
                        Spacer(Modifier.height(10.dp))
                        BigButton("Collect reward", { vm.finishMinigame(game, score) }, Modifier.fillMaxWidth(0.7f))
                    }
                } else when (game) {
                    MiniGame.SNOW -> SnowGame { score++ }
                    MiniGame.COAL -> CoalGame { score++ }
                    MiniGame.TICKETS -> TicketGame { ok -> if (ok) score++ else score = (score - 1).coerceAtLeast(0) }
                }
            }
            if (!started) {
                Spacer(Modifier.height(8.dp))
                Text("Not now", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.cancelMinigame() }.padding(6.dp))
            }
        }
    }
}

@Composable
private fun SnowGame(onHit: () -> Unit) {
    val piles = remember { mutableStateListOf<Offset>().apply { repeat(5) { add(Offset(0.12f + Math.random().toFloat() * 0.76f, 0.12f + Math.random().toFloat() * 0.76f)) } } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        // the roof of the car under the snow
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color(0xFFD95D39).copy(alpha = 0.25f), Offset(size.width * 0.06f, size.height * 0.06f), Size(size.width * 0.88f, size.height * 0.88f), androidx.compose.ui.geometry.CornerRadius(24f))
        }
        piles.forEachIndexed { k, p ->
            Box(
                Modifier.offset(x = w * p.x - 28.dp, y = w * p.y - 22.dp).size(56.dp, 44.dp).clickable {
                    Haptics.tap(); onHit()
                    piles[k] = Offset(0.12f + Math.random().toFloat() * 0.76f, 0.12f + Math.random().toFloat() * 0.76f)
                },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawOval(Color(0xFFBFD9EC), Offset(0f, size.height * 0.35f), Size(size.width, size.height * 0.65f))
                    drawOval(Color.White, Offset(size.width * 0.05f, size.height * 0.1f), Size(size.width * 0.9f, size.height * 0.75f))
                    drawOval(Ink, Offset(size.width * 0.05f, size.height * 0.1f), Size(size.width * 0.9f, size.height * 0.75f), style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun CoalGame(onShovel: () -> Unit) {
    val heat = remember { Animatable(0f) }
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(taps) { heat.snapTo((heat.value + 0.12f).coerceAtMost(1f)); heat.animateTo(0f, tween(1400)) }
    Box(Modifier.fillMaxSize().clickable { Haptics.tap(); taps++; onShovel() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize(0.7f)) {
            val w = size.width
            drawRoundRect(Color(0xFF4B525C), Offset(w * 0.1f, w * 0.1f), Size(w * 0.8f, w * 0.8f), androidx.compose.ui.geometry.CornerRadius(30f))
            drawRoundRect(Ink, Offset(w * 0.1f, w * 0.1f), Size(w * 0.8f, w * 0.8f), androidx.compose.ui.geometry.CornerRadius(30f), style = Stroke(4.dp.toPx()))
            val g = 0.3f + heat.value * 0.7f
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE680).copy(alpha = g), Color(0xFFFF7A2F).copy(alpha = g))), Offset(w * 0.25f, w * 0.35f), Size(w * 0.5f, w * 0.4f), androidx.compose.ui.geometry.CornerRadius(20f))
            for (i in 0 until 5) drawCircle(Color(0xFF2B2A33), w * 0.05f, Offset(w * (0.32f + i * 0.09f), w * 0.72f))
        }
        Text("TAP!", fontSize = 28.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.offset(y = (-6).dp))
    }
}

@Composable
private fun TicketGame(onAnswer: (Boolean) -> Unit) {
    val colors = listOf(Color(0xFFE5484D), Color(0xFF2F8FE0), Color(0xFF3FAE5A))
    var want by remember { mutableIntStateOf((0..2).random()) }
    var flash by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(flash) { if (flash != null) { kotlinx.coroutines.delay(250); flash = null } }
    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        // the passenger's ticket
        Box(
            Modifier.size(150.dp, 80.dp).clip(RoundedCornerShape(10.dp)).background(colors[want]).border(3.dp, Ink, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("TICKET", fontSize = 22.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Color.White) }
        Text(when (flash) { true -> "Right coach!"; false -> "Oops, wrong coach"; null -> "Which coach?" }, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = when (flash) { true -> Mint; false -> Ember; null -> Muted })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.forEachIndexed { i, c ->
                Box(
                    Modifier.size(78.dp, 56.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.85f)).border(3.dp, Ink, RoundedCornerShape(12.dp))
                        .clickable { val ok = i == want; Haptics.tap(); onAnswer(ok); flash = ok; want = (0..2).random() },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize(0.7f)) {
                        for (k in 0 until 3) drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(size.width * (0.05f + k * 0.33f), size.height * 0.2f), Size(size.width * 0.24f, size.height * 0.35f), androidx.compose.ui.geometry.CornerRadius(6f))
                    }
                }
            }
        }
    }
}

// ---------------- the evolve cutscene ----------------

/** A short scene before the "New train!" card: the old train pulls away, a flash, the new one rolls in. */
@Composable
fun EvolveCutscene(fromTier: Int, toTier: Int, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(3400, easing = LinearEasing)); onDone() }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, dismissOnBackPress = false)) {
        val t = p.value
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C2A44), Color(0xFF3B4F78)))).clickable { }, contentAlignment = Alignment.Center) {
            // falling snow
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 60) {
                    val x = ((i * 0.618f) % 1f) * size.width
                    val y = (((i * 0.414f) % 1f) * size.height + t * size.height * 1.4f) % size.height
                    drawCircle(Color.White.copy(alpha = 0.8f), (1.5f + i % 3).dp.toPx(), Offset(x, y))
                }
            }
            // old train leaves upwards
            if (t < 0.45f) {
                TrainPicture(fromTier, Modifier.fillMaxWidth(0.55f).aspectRatio(0.62f).graphicsLayer { translationY = -t / 0.45f * 1600f; alpha = 1f - t / 0.45f * 0.5f })
            }
            // new train rolls in from below and settles
            if (t > 0.5f) {
                val k = ((t - 0.5f) / 0.35f).coerceIn(0f, 1f)
                val ease = 1f - (1f - k) * (1f - k) * (1f - k)
                TrainPicture(toTier, Modifier.fillMaxWidth(0.62f).aspectRatio(0.62f).graphicsLayer { translationY = (1f - ease) * 1400f; scaleX = 0.9f + 0.1f * ease; scaleY = 0.9f + 0.1f * ease })
            }
            // flash between them
            val flash = (1f - kotlin.math.abs(t - 0.48f) / 0.08f).coerceIn(0f, 1f)
            if (flash > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash)))
            Text(
                if (t < 0.48f) "Evolving..." else TIERS[toTier].name,
                fontSize = 30.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            )
        }
    }
}


// ---------------- profile ----------------

/** The player's picture: their own photo or one of the train's characters. */
@Composable
fun Avatar(vm: GameViewModel, modifier: Modifier = Modifier) {
    val sp = rememberGameSprites()
    Box(modifier.clip(CircleShape).background(Color(0xFFFFF0D2)), contentAlignment = Alignment.Center) {
        val photo = vm.avatarPhoto
        if (vm.avatar == -1 && photo != null) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            val img = if (vm.avatar <= 0) sp.npc else sp.survivors.getOrNull(vm.avatar - 1)
            // show the head: the sprite is 32 x 40 with the head at the top
            if (img != null) Image(img, null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f; translationY = size.height * 0.2f })
        }
    }
}

@Composable
fun ProfileSheet(vm: GameViewModel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(vm.playerName) }
    SheetFrame("Profile", rememberGameSprites().iconTrophy, Color(0xFFE3D6FB), onClose, modifier) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF1EAFF), Color(0xFFDCEEF9)))).border(2.dp, Ink, RoundedCornerShape(20.dp)).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Box(Modifier.size(96.dp).clip(CircleShape).border(3.dp, Ink, CircleShape).clickable { picking = true }) { Avatar(vm, Modifier.fillMaxSize()) }
                        Text(
                            "Edit", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).clip(RoundedCornerShape(8.dp)).background(XpPurple).border(1.5.dp, Ink, RoundedCornerShape(8.dp)).clickable { picking = true }.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (editing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(180.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).border(2.dp, Ink, RoundedCornerShape(10.dp)).padding(8.dp)) {
                                BasicTextField(name, { name = it.take(18) }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold))
                            }
                            Spacer(Modifier.width(6.dp))
                            ChunkyButton(true, listOf(Color(0xFFBFE0D2), Color(0xFF8FC2AE)), { vm.rename(name); name = vm.playerName; editing = false }, Modifier.width(64.dp)) {
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            }
                        }
                    } else {
                        Text(vm.playerName, fontSize = 22.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.clickable { editing = true })
                        Text("Tap your name to change it", fontSize = 10.sp, color = Muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Level ${vm.playerLevel}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = XpPurple)
                    Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Panel).border(1.5.dp, Ink, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth((vm.xp.toFloat() / vm.xpNeeded).coerceIn(0f, 1f)).background(XpPurple))
                        Text("${vm.xp} / ${vm.xpNeeded} XP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                    Text("Next level: +${vm.levelGems(vm.playerLevel + 1)} gems · XP comes from awards, quests, stories, evolving and the daily gift", fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                SectionTitle("Awards", GoldText, "${vm.unlocked.size} / ${ACHIEVEMENTS.size} earned")
            }
            items(ACHIEVEMENTS.sortedBy { if (it.id in vm.unlocked) 0 else 1 }) { a -> AchievementRow(a, a.id in vm.unlocked) }
        }
    }
    if (picking) AvatarPicker(vm, onClose = { picking = false })
}

@Composable
private fun AvatarPicker(vm: GameViewModel, onClose: () -> Unit) {
    val sp = rememberGameSprites()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { vm.setAvatarPhoto(uri); onClose() }
    }
    CozyCard("Profile picture", Color(0xFFE3D6FB), onDismiss = onClose, closable = true) {
        ChunkyButton(true, listOf(Color(0xFFFFC2DA), Color(0xFFF08DB5)), {
            launcher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, Modifier.fillMaxWidth(0.85f)) {
            Text("Choose a photo", fontSize = 15.sp, fontFamily = CozyFont, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(8.dp))
        Text("or pick a character", fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(6.dp))
        val all = listOf(0) + (1..sp.survivors.size).toList()
        all.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { i ->
                    val img = if (i == 0) sp.npc else sp.survivors[i - 1]
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFF0D2))
                            .border(if (vm.avatar == i) 3.dp else 1.5.dp, if (vm.avatar == i) Ember else Ink, CircleShape)
                            .clickable { vm.chooseAvatar(i); onClose() },
                    ) {
                        if (img != null) Image(img, null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f; translationY = size.height * 0.2f })
                    }
                }
            }
        }
    }
}
