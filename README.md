# Frost Express

The world has frozen over. One train keeps running along the frozen track. Start with the small Snow Pup, upgrade it, and evolve it into new trains. You earn coins from the distance travelled.

## Gameplay
- **Income**: speed x coins per metre. The train runs on its own, even while the app is closed (up to 8 hours of offline earnings).
- **Upgrades**: Engine Power, Cargo Wagon, Heat Generator, Food Car, Ice Plow, Coal Stoker, Boost Core, Night Conductor.
- **Boost**: tap BOOST repeatedly. Each tap uses 10% of the boost meter and gives a 3 second income multiplier. When the meter is empty, boost is locked until it fully recharges.
- **Train interior**: a top-down cutaway shows the inside of the train. A worker (NPC) stokes the boiler, drives, runs the heat generator, cooks and eats, hauls cargo and rests, depending on which upgrades you own. Empty dashed slots show what you can still install.
- **Full screen**: the game runs in immersive full screen. The upgrades live on their own page, opened with the gear icon at the bottom left (an orange dot shows when you can afford something).
- **Idle-game basics**: buy x1 / x10 / x100 / MAX selector, and milestone bonuses (x2 effect at levels 25, 50, 100, 200, 300 for Engine, Wagon, Heat and Food).
- **Sound**: cozy synthesized background music and sound effects (buy, boost, evolve, taps). The speaker icon in the top bar mutes everything. Regenerate with `python store_assets/make_audio.py` (needs numpy and ffmpeg).
- **Two kinds of upgrades**: *Run upgrades* (coins) reset when you evolve. *Permanent upgrades* (Frost Cores) stay forever: Eternal Furnace, Permafrost Rails, Head Start, Deep Freeze Core, Hibernation.
- **Upgrade chain**: upgrades unlock one after another (for example Cargo Wagon unlocks at Engine Lv 5). Unlocks are kept after evolving. The tutorial is guided (hints and highlights) and runs on the first launch only.
- **Daily reward** with a login streak (every 7th day gives a Frost Core), a short **tutorial** on first launch, 13 **achievements**, and a **settings** dialog (volumes, reset progress, privacy policy).
- **World**: a 150 s day and night cycle (shown in the top bar) with headlights, snow storms at random intervals (every 100 to 240 s, slow the train 25%), random events (aurora, tap-able supply crate), drag the scene up or down to look along a long train, stations that pass every 400 m and pay a bonus, and a **Second Worker** permanent upgrade that adds another worker to the train.
- **Train paint**: unlock new liveries with each evolve and pick one in Settings.
- **Controls**: tap a device on the train to jump to its upgrade; hold a buy button to keep buying. Settings has a frame rate cap (30/60/90/120) and a post-processing switch (bloom, colour grading, vignette).
- **Evolve**: once you reach the required distance, evolve your train. Coins and upgrades reset, but you unlock the next train (6 tiers) and earn permanent Frost Cores (+25% coins each).

## Ads (planned for after launch)
Rewarded ad boosts (watch an ad for x2 income for 2 minutes) are prepared but OFF. See `Ads.kt` and `Balance.ADS_ENABLED`. Add AdMob, implement `AdProvider`, then switch the flag on.

## Planned for later (needs accounts or servers)
- **In-app purchases** (remove ads, starter pack): hook in `Billing.kt`, flag `Balance.IAP_ENABLED`. Needs Google Play Billing and products created in Play Console.
- **Leaderboards, weekly events, cloud save**: need a backend (for example Google Play Games Services). Not included yet. Android Auto Backup already restores local progress when a user reinstalls or changes phone.

## Google Play sign-in and cloud save (setup)
The code is ready (`CloudSave.kt`, Settings dialog) but switched off with `Balance.PLAY_GAMES_ENABLED = false`, because it needs your Play Console project:
1. In Play Console create the app, then Grow > Play Games Services > Setup and management > Configuration, and create the game project. Enable **Saved games**.
2. Add the credential for Android with your app's package name and the SHA-1 of your **upload/app signing key** (and the debug key while testing).
3. Copy the project id into `app/src/main/res/values/strings.xml` (`game_services_project_id`).
4. Set `Balance.PLAY_GAMES_ENABLED = true`, rebuild. Add yourself as a tester in the Play Games Services testing tab.
Progress is saved automatically every minute and when the app goes to the background. If the cloud save is further along than the device, the player is asked which to keep.

## Run it
1. Open the project folder in the latest Android Studio. Gradle syncs automatically.
2. Pick a phone or emulator and press Run.

## Build an APK in the cloud (no PC needed)
1. Push this project to a GitHub repository (branch `main`).
2. The `Build APK` workflow in `.github/workflows/build-apk.yml` runs on every push. You can also start it from the Actions tab with "Run workflow".
3. When it finishes, download `app-debug.apk` from the latest Release, or from the run's Artifacts section.
4. Install it on your phone (allow installs from unknown sources).

This is a debug build for testing. For the Play Store, build a signed bundle as described below.

## Where to tweak
- `GameData.kt`: the upgrade list, costs, train tier names and colors, and all balance numbers (`Balance`).
- `GameViewModel.kt`: formulas, boost logic, evolve, save and offline earnings.
- `TrainScene.kt`: the 2D flat graphics (mountains, trees, track, train, smoke).
- `GameScreen.kt`: HUD, upgrades page, UI layout and colors.
- `app/build.gradle.kts`: `applicationId`, `versionCode`, `versionName`.

## Publish on the Play Store
1. Change `applicationId` (currently `com.chetna.frostexpress`). It cannot be changed after you publish.
2. Create a keystore and keep it safe. If you lose it you cannot ship updates:
   `keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
3. Create `keystore.properties` in the project root:
   ```
   storeFile=upload.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. Build: `./gradlew bundleRelease` (Windows: `gradlew.bat bundleRelease`). Output: `app/build/outputs/bundle/release/app-release.aab`
   (Or in Android Studio: Build > Generate Signed Bundle.)
5. Create the app in Play Console and upload the `.aab` (keep Play App Signing on).
6. Required forms: privacy policy URL (the app collects no data), Data safety, Content rating, Target audience, Ads declaration (no ads).
7. Store listing graphics: run `pip install pillow` and then `python store_assets/make_store_assets.py`. It creates `icon-512.png` and `feature-graphic-1024x500.png` (edit the script to change text or colors). Take the phone screenshots yourself.
8. New personal developer accounts may need a closed test before they can release to production. Check the current rules in Play Console.
9. `targetSdk` must meet Play's current requirement. If Play Console warns about it, raise `targetSdk` and `compileSdk`.

## Note
This project has not been compiled locally. If Android Studio shows a version warning the first time you open it, accept its suggested update.
