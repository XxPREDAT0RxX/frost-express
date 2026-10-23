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
- `blender/npcs.py` — 16 toon-shaded 3D NPCs from specs: the original six c1..c6 (dadi, beanie girl, glasses+stripes, beard puffer, fur hat, trapper) plus n07 little boy, n08 grandpa, n09 office worker, n10 student girl, n11/n12 Paddylands village woman/man, n13 conductor, n14 hoodie teen, n15 aunty, n16 little girl (skin tones, child scale, pagri, dupatta, hood, caps, earmuffs, headphones, props). Renders `idle_00..23`, `walk_00..07`, `side_00..07`, `sit_00..23` at 176×248. Latest render: `blender-8`. Old `chibis.py` is in `blender/old/`.
- **In game:** `store_assets/make_npc_strips.py <unzipped npcs folder>` packs them into `res/drawable-nodpi/npc_<id>_{idle,walk,side}.webp` strips (half size, cropped) and writes `NpcAtlas.kt`. `NpcDraw.kt` (`drawNpc`, `drawNpcSeated`) plays them; `TrainScene.kt` uses them (via `npcLook(sprite)`) for seated riders, boarding/leaving walks inside the train and walkers on the platform, in cold biomes. Warm biomes still use the `summer` sprites. Old flat chibi art is now only a fallback.

- `blender/scenery.py` — every roadside item (67: trees, plants, rocks, village, winter/harbour props, vehicles) plus station buildings `station_t0..t4`, bench and lamp, same toon look. Latest render: `blender-11`. `store_assets/make_env_sprites.py <unzipped scenery folder> [names]` fits each render into the old sprite's slot (`store_assets/env_slots.json`: canvas + drawn box) and overwrites `spr_<name>.webp` / `station_*.webp`, so no code changes are needed. The Blender workflow renders only the scripts a push changed.

- `blender/train.py` — top-down train: nose caps for all 25 tiers (`nose*_fill` tinted body + `nose*_line` outline/decor via a holdout pass), coach walls `car_fill`/`car_line` (side glass) and `car_roof`. Fixed frames match the draw rects, so `store_assets/make_train_sprites.py <folder>` just converts to webp. Latest: `blender-13`.
- `blender/npcs_summer.py` — 8 warm-weather NPCs (s01..s08) built with the npcs.py kit (`SPEC_HOOK`); packed with `make_npc_strips.py <npcs> <npcs_summer>` into `NpcAtlas.SUMMER`; hot biomes use them (`npcLook(sprite, warm)`). Latest: `blender-16`.
- (replaced, see projected.py) `blender/scenery_anim.py` — looping frames (sway for trees/palms/bamboo/grass/flowers, turbine, campfire, lighthouse) reusing scenery.py (`ANIM_HOOK`); packed into `spra_<name>.webp` strips by `make_env_sprites.py`; `SCENE_ANIM_FPS` in Sprites.kt, `spriteFrame` in TrainScene. Latest: `blender-15`. Scenery statics latest: `blender-18`.
- Upgrade devices / furniture art kept on purpose (already detailed).

- **Environment = the original painted art** (stations and all `spr_*` restored; the Blender toon scenery was rejected as unrealistic/off-style). `blender/projected.py` puts painted sprites (`blender/painted/*.png`) onto puffed 3D shapes and animates them (wind bend with light, turbine blades, campfire flame); `store_assets/make_projected_strips.py <folder>` writes `spra_<game name>.webp` (36 strips: plants and trees, plus house lantern and smoke, well rope, flags, lighthouse beam, boats, scarf, crane hook, paddy, bed net, haystack, street lamp) that `spriteFrame` plays with cross-fade. `scenery_sample.py` / `scenery_anim_sample.py` / `scenery.py` are kept only as experiments. Render workflow can be run by hand: dispatch with input `scripts`.
- Himanshu's rule: new art/animations go to him as samples first, into the game only after he says yes.

## Art pipeline
- Gemini sheets on pale mint `#E0F5E9`; cut with numpy/scipy/PIL (flood-fill background from the border, soft alpha, pull edge colour inward to kill mint fringe), save `.webp` in `app/src/main/res/drawable-nodpi/`.
- Characters in game now: 16 Blender NPCs as frame animations (see above); flat chibi sheet art + `MeshPuppet` only as fallback.

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
