package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("frost_express", Context.MODE_PRIVATE)

    var coins by mutableDoubleStateOf(0.0); private set
    var runDistance by mutableDoubleStateOf(0.0); private set
    var cores by mutableIntStateOf(0); private set
    var evolutions by mutableIntStateOf(0); private set
    var boostMeter by mutableDoubleStateOf(1.0); private set
    var boostTimeLeft by mutableDoubleStateOf(0.0); private set
    var boostLocked by mutableStateOf(false); private set
    var offlineCoins by mutableDoubleStateOf(0.0); private set
    var offlineMeters by mutableDoubleStateOf(0.0); private set
    val levels = mutableStateListOf<Int>()

    // ---- derived values ----
    fun level(t: UpgradeType): Int = levels[t.ordinal]

    val tierIndex: Int get() = min(evolutions, TIERS.lastIndex)

    val speed: Double
        get() = (Balance.BASE_SPEED + Balance.SPEED_PER_ENGINE * level(UpgradeType.ENGINE)) *
            (1.0 + Balance.PLOW_BONUS * level(UpgradeType.PLOW)) *
            (1.0 + Balance.TIER_SPEED_BONUS * tierIndex)

    val coinsPerMeter: Double
        get() = (1.0 + Balance.WAGON_BONUS * level(UpgradeType.WAGON)) *
            (1.0 + Balance.HEAT_BONUS * level(UpgradeType.HEAT)) *
            (1.0 + Balance.FOOD_BONUS * level(UpgradeType.FOOD)) *
            Balance.TIER_INCOME.pow(tierIndex) *
            (1.0 + Balance.CORE_BONUS * cores)

    val boostMultiplier: Double
        get() = Balance.BOOST_BASE_MULT + Balance.BOOST_MULT_PER_LEVEL * level(UpgradeType.BOOSTER)

    val isBoosting: Boolean get() = boostTimeLeft > 0.0
    val incomeMultiplier: Double get() = if (isBoosting) boostMultiplier else 1.0
    val coinsPerSec: Double get() = speed * coinsPerMeter * incomeMultiplier
    private val rechargePerSec: Double
        get() = Balance.BOOST_RECHARGE_PER_SEC * (1.0 + Balance.COAL_BONUS * level(UpgradeType.COAL))
    private val offlineRate: Double
        get() = min(1.0, Balance.OFFLINE_BASE + Balance.CONDUCTOR_BONUS * level(UpgradeType.CONDUCTOR))

    val evolveDistance: Double get() = Balance.EVOLVE_BASE_DISTANCE * Balance.EVOLVE_GROWTH.pow(evolutions)
    val canEvolve: Boolean get() = runDistance >= evolveDistance
    val coreGain: Int get() = max(1, floor(sqrt(runDistance / 1000.0)).toInt())

    fun upgradeCost(spec: UpgradeSpec): Double =
        floor(spec.baseCost * spec.growth.pow(level(spec.type)))

    init {
        load()
        startLoop()
    }

    // ---- actions ----
    fun buy(spec: UpgradeSpec) {
        val lvl = level(spec.type)
        if (lvl >= spec.maxLevel) return
        val cost = upgradeCost(spec)
        if (coins >= cost) {
            coins -= cost
            levels[spec.type.ordinal] = lvl + 1
        }
    }

    /** Boost button ka har tap: meter ka 10% lagta hai, 3 sec ka income multiplier milta hai. */
    fun boost() {
        if (boostLocked || boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) return
        boostMeter = max(0.0, boostMeter - Balance.BOOST_COST_PER_TAP)
        boostTimeLeft = min(Balance.BOOST_MAX_SEC, boostTimeLeft + Balance.BOOST_SEC_PER_TAP)
        if (boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) boostLocked = true // full recharge tak wait
    }

    fun evolve() {
        if (!canEvolve) return
        cores += coreGain
        evolutions++
        coins = 0.0
        runDistance = 0.0
        for (i in levels.indices) levels[i] = 0
        boostMeter = 1.0
        boostTimeLeft = 0.0
        boostLocked = false
        save()
    }

    fun dismissOffline() {
        offlineCoins = 0.0
        offlineMeters = 0.0
    }

    fun save() {
        prefs.edit()
            .putString("coins", coins.toString())
            .putString("dist", runDistance.toString())
            .putInt("cores", cores)
            .putInt("evo", evolutions)
            .putString("levels", levels.joinToString(","))
            .putLong("last", System.currentTimeMillis())
            .apply()
    }

    // ---- internals ----
    private fun load() {
        coins = prefs.getString("coins", "0")?.toDoubleOrNull() ?: 0.0
        runDistance = prefs.getString("dist", "0")?.toDoubleOrNull() ?: 0.0
        cores = prefs.getInt("cores", 0)
        evolutions = prefs.getInt("evo", 0)
        val saved = prefs.getString("levels", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        levels.clear()
        UpgradeType.values().indices.forEach { levels.add(saved.getOrElse(it) { 0 }) }

        val last = prefs.getLong("last", 0L)
        if (last > 0L) {
            val secs = ((System.currentTimeMillis() - last) / 1000).coerceIn(0L, Balance.OFFLINE_CAP_SEC)
            val meters = speed * secs * offlineRate
            val gain = meters * coinsPerMeter
            if (gain >= 1.0) {
                runDistance += meters
                coins += gain
                offlineCoins = gain
                offlineMeters = meters
            }
        }
    }

    private fun tick(dt: Double) {
        val meters = speed * dt
        runDistance += meters
        coins += meters * coinsPerMeter * incomeMultiplier
        if (boostTimeLeft > 0.0) boostTimeLeft = max(0.0, boostTimeLeft - dt)
        if (boostMeter < 1.0) boostMeter = min(1.0, boostMeter + rechargePerSec * dt)
        if (boostLocked && boostMeter >= 1.0) boostLocked = false
    }

    private fun startLoop() {
        viewModelScope.launch {
            var lastTick = System.nanoTime()
            var sinceSave = 0.0
            while (true) {
                delay(100)
                val now = System.nanoTime()
                val dt = (now - lastTick) / 1e9
                lastTick = now
                tick(dt)
                sinceSave += dt
                if (sinceSave >= 5.0) {
                    save()
                    sinceSave = 0.0
                }
            }
        }
    }
}
