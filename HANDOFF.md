# Little Express — handoff notes (for a new chat)

Paste this file (or point the new chat to it in the repo) so it can continue where we left off.

## Who / how
- Owner: **Himanshu** (not "Chetna"). Assistant persona: **Smriti**. Replies in **short Roman Hinglish**; all in-game text, code and docs in English.
- Himanshu tests on a **Nothing Phone 3a**. PC: Pentium G4560, 24 GB RAM, GTX 1050 Ti 4 GB (too weak for local AI video).
- Nothing builds locally: everything builds on **GitHub Actions** (~9 min). Visuals are checked through CI screenshots (Roborazzi, `screens.zip` in each release).

## Project
- Native Android idle **train simulator**, Kotlin 2.0.21, Jetpack Compose Canvas. Package `com.chetna.frostexpress`, app name **Little Express** (old name "Frost Express" is gone from all docs).
- Repo: `github.com/XxPREDAT0RxX/frost-express` (private), branch `main`.
- Each push to `main` → workflow **Build APK** → release `build-N` with `app-debug.apk`, `app-releaseTest.apk` (R8, debug-signed, installs over earlier builds), `app-release.aab`, `mapping.txt`, `screens.zip`.
- Download an asset: `gh api repos/XxPREDAT0RxX/frost-express/releases/tags/build-N --jq '.assets[]|"\(.id) \(.name)"'`, then `curl -sL -H "Accept: application/octet-stream" -H "Authorization: token $(gh auth token)" -o FILE https://api.github.com/repos/XxPREDAT0RxX/frost-express/releases/assets/ID`.
- Last APK sent: **v95 = build-155**.
- Workflow **Blender render** (`.github/workflows/blender.yml`) runs on changes in `blender/**`: installs Blender 4.2.3, runs every `blender/*.py` with `-- out/<name>`, publishes `blender-N` release with `<name>_frames.zip` and `<name>_lineup.png`.

## Key code
- `GameViewModel.kt` — economy (fares × passengers × satisfaction, wages/fuel/upkeep/interest), prestige, trader (`traderCost` = income × 45 s / 150 s), station logic (`collectStation`, `stationTick`, `depart()`, auto-depart 25 s).
- `GameData.kt` — `Balance` numbers (stations every 150–300 s, `AUTO_DEPART_SEC`).
- `Countries.kt` — 6 countries incl. **Paddylands** (zone 6 = Paddy Village).
- `TrainScene.kt` — whole scene: biomes (`ZONES`), decor (types fixed per item id), stations (big platforms `ST_LEN 520`, `ST_PW 104`, building types 0–4), station pan (`sceneStationPan`, train slides right at a stop), riders with fixed seats (`ScenePax`, `syncRoster`, `stationExchange`, `Leaver`, `Arrival`, `walkPath`), boarding/leaving walks seat↔aisle↔car door↔station building, moonlit night wash.
- `MeshPuppet.kt` — skinned-mesh 2D animator for the chibi sprites (`MeshPuppet` front view idle/walk, `MeshSide` profile walk, `drawChibi()` turning between them).
- `Sprites.kt` — `chibis` (seated, eyes open/blink), `chibiFull` (full body, 8 px padding), `chibiSide` (profile, null for #3), `drawPuppet`.
- `GameScreen.kt` — HUD, sheets; when a sheet opens the scene is laid out wider and drawn scaled (`link.viewScale`), side rails and bottom bar hide; `DepartButton`; compact `SurvivorsCard`.
- `blender/chibis.py` — six toon-shaded 3D chibis (c1 dadi, c2 girl yellow beanie, c3 glasses+striped sweater, c4 beard orange puffer, c5 beige fur hat, c6 white trapper hat) rendered to `idle_00..23`, `walk_00..07`, `side_00..07` (three-quarter view), `sit_00..23` at 176×248. Latest render: `blender-5`. **Not yet in the game.**

## Art pipeline
- Gemini sheets on pale mint `#E0F5E9`; cut with numpy/scipy/PIL (flood-fill background from the border, soft alpha, pull edge colour inward to kill mint fringe), save `.webp` in `app/src/main/res/drawable-nodpi/`.
- Characters in game now: flat chibi sheet art (6 characters) with mesh animation, blinking on seats.

## Release status
- Store graphics regenerated: `store_assets/play/` (7 screenshots, feature graphic, icon) via `store_assets/make_store_assets.py <screens folder>`.
- Upload key created and sent to Himanshu (`upload.jks`, `KEY-INFO.txt`, signed `LittleExpress-1.0.0-signed.aab`). **Key files are only with Himanshu** — not in the repo.
- CI signs the AAB if repo secrets `UPLOAD_KEYSTORE_B64` and `UPLOAD_KEYSTORE_PASSWORD` exist (Himanshu still has to add them; the assistant cannot reach Actions secrets).
- Privacy policy: `PRIVACY.md` + an `index.html` page; Himanshu must host it (public repo + GitHub Pages) and put the link in Play Console.
- Play Console steps left: create app, upload AAB, listing, content rating, Data Safety; check the 12-testers/14-days closed-testing rule for new personal accounts. Launch target was 24 Oct but Himanshu is fine delaying it.

## Open threads
1. **VRoid plan (current):** Himanshu installs VRoid Studio, makes one character (dadi), exports `.vrm` (reduce polygons, slightly bigger head) and sends it. Then: add the VRM add-on to the Blender CI, animate idle/walk/sit/blink on the model's own bones, toon render to sprites, send a preview. If liked, make the other five.
2. Fallback ready: Blender chibis (`blender/chibis.py`) — integrate into the game as frame animations (seat, platform, boarding, turning) if VRoid does not work out.
3. Station road may still bob when scrolling at a stop — waiting for a screen recording from Himanshu.
4. Possibly keep the Boost button visible while a sheet is open (ask).
5. Policy: no more new features before launch; only bug fixes unless Himanshu asks.
