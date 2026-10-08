# Frost Express

Duniya jam chuki hai. Ek train jame hue track pe chalti rehti hai. Chhoti Snow Pup se shuru karo, upgrades karo, evolve karke nayi trains unlock karo. Kamai distance (chale hue metres) pe hoti hai.

## Gameplay
- **Income**: speed x coins per metre. Train khud chalti hai, offline bhi (max 8 ghante).
- **Upgrades**: Engine Power, Cargo Wagon, Heat Generator, Food Car, Ice Plow, Coal Stoker, Boost Core, Night Conductor.
- **Boost**: BOOST ko baar baar tap karo. Har tap meter ka 10% leta hai aur 3 sec ka income multiplier deta hai. Meter khatam to boost band, full recharge hone tak wait.
- **Evolve**: required distance poori hone pe evolve karo. Coins aur upgrades reset, par nayi train (6 tiers) aur permanent Frost Cores (+25% coins each) milte hain.

## Run karna
1. Android Studio (latest) me `FrostExpress` folder open karo. Gradle sync apne aap hoga.
2. Phone/emulator select karke Run dabao.

## Tweak kahan karna hai
- `GameData.kt`: upgrades ki list, costs, tiers ke naam/colors, aur saare balance numbers (`Balance`).
- `GameViewModel.kt`: formulas, boost logic, evolve, save/offline.
- `TrainScene.kt`: 2D flat graphics (pahad, ped, track, train, dhuan).
- `GameScreen.kt`: UI layout aur colors.
- `app/build.gradle.kts`: `applicationId`, `versionCode`, `versionName`.

## Play Store pe launch
1. `applicationId` badlo (abhi `com.chetna.frostexpress`). Ye ID baad me change nahi hoti.
2. Keystore banao (safe rakho, kho diya to update nahi de paoge):
   `keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
3. Root folder me `keystore.properties` banao:
   ```
   storeFile=upload.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. Build: `./gradlew bundleRelease` (Windows: `gradlew.bat bundleRelease`). Output: `app/build/outputs/bundle/release/app-release.aab`
   (Ya Android Studio: Build > Generate Signed Bundle.)
5. Play Console me app banao, `.aab` upload karo (Play App Signing on rakho).
6. Zaruri forms: privacy policy URL (app koi data collect nahi karta), Data safety, Content rating, Target audience, Ads declaration (no ads).
7. Store listing graphics: `pip install pillow` karke `python store_assets/make_store_assets.py` chalao. Isse `icon-512.png` aur `feature-graphic-1024x500.png` ban jaate hain (script me text/colors edit kar sakte ho). Phone screenshots khud lene hain.
8. Naye personal developer accounts ke liye Google production se pehle closed testing maang sakta hai. Console me latest rule check kar lena.
9. `targetSdk` Play ki current requirement se match hona chahiye. Console warn kare to `targetSdk`/`compileSdk` badha dena.

## Note
Ye project yahan compile nahi kiya gaya (sandbox me Android SDK nahi hai). Android Studio me pehli baar open karte time koi version warning aaye to Studio ka suggested update accept kar lo.
