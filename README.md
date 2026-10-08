# Frost Express

The world has frozen over. One train keeps running along the frozen track. Start with the small Snow Pup, upgrade it, and evolve it into new trains. You earn coins from the distance travelled.

## Gameplay
- **Income**: speed x coins per metre. The train runs on its own, even while the app is closed (up to 8 hours of offline earnings).
- **Upgrades**: Engine Power, Cargo Wagon, Heat Generator, Food Car, Ice Plow, Coal Stoker, Boost Core, Night Conductor.
- **Boost**: tap BOOST repeatedly. Each tap uses 10% of the boost meter and gives a 3 second income multiplier. When the meter is empty, boost is locked until it fully recharges.
- **Train interior**: a top-down cutaway shows the inside of the train. A worker (NPC) stokes the boiler, drives, runs the heat generator, cooks and eats, hauls cargo and rests, depending on which upgrades you own. Empty dashed slots show what you can still install.
- **Full screen**: the game runs in immersive full screen. The upgrades live on their own page, opened with the gear icon at the bottom left (an orange dot shows when you can afford something).
- **Idle-game basics**: buy x1 / x10 / x100 / MAX selector, and milestone bonuses (x2 effect at levels 25, 50, 100, 200, 300 for Engine, Wagon, Heat and Food).
- **Evolve**: once you reach the required distance, evolve your train. Coins and upgrades reset, but you unlock the next train (6 tiers) and earn permanent Frost Cores (+25% coins each).

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
