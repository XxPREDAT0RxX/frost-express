package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmokeTest {
    private fun run(evo: Int, lv: String, coins: String, away: Long) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("frost_express", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("tut", true).putInt("tstage", 4).putInt("evo", evo)
            .putString("levels", lv).putString("coins", coins)
            .putLong("last", System.currentTimeMillis() - away * 1000).commit()
        val vm = GameViewModel(ctx as Application)
        val tick = GameViewModel::class.java.declaredMethods.first { it.name == "tick" }
        tick.isAccessible = true
        repeat(3000) { tick.invoke(vm, 0.1) }
        vm.collectStation()
        check(vm.coins.isFinite()) { "coins not finite" }
    }

    @Test fun legacy() = run(0, "12,6,5,3,2,1,0,0,1,1", "48250", 7200)
    @Test fun midEvo() = run(5, "40,30,30,30,30,30,30,30,30,30", "1e30", 36000)
    @Test fun highEvo() = run(40, "200,200,200,200,200,200,200,200,200,200", "1e200", 86400)
    @Test fun fresh() = run(0, "", "0", 0)
}
