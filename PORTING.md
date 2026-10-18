# Porting notes: Steam 'n' Rails (Create Fly, Fabric 26.2) → NeoForge 26.3

This branch, `zp5/26.3`, is a Zoompack 5 port of [cat4blep/Create-Steam-n-Rails-Fly](https://github.com/cat4blep/Create-Steam-n-Rails-Fly)
to NeoForge 26.3. It starts from upstream branch `fabric-26.2`, commit `7c5809079` (release `SNR.FLY-STABLE-1.4`).
Upstream is the git remote `upstream`. It runs on ZP5's NeoForge port of Create Fly (`create-fly-neoforge`, branch
`neoforge-26.3`, mod id `create`), not on Creators-of-Create's NeoForge Create.

Checked on 2026-10-18 against Minecraft 26.3, NeoForge 26.3.0.89, Java 25.0.1 and `create-fly-neoforge-6.0.9+neoforge-26.3.jar`
(sha512 `0821382e…92dd47d0b`, built from create-fly-neoforge `468f3bd6`).

| | |
|---|---|
| Mod id | `railways` (unchanged) |
| Version | `1.4.0-zp5.1` (`neoforge_mod_version`; upstream's `mod_version` stays `SNR.FLY-STABLE-1.4`) |
| Jar | `build/libs/railways-neoforge-26.3-1.4.0-zp5.1.jar` |
| License | LGPL-3.0-or-later (the `LICENSE` file; Modrinth lists GPL-3.0-only). Parts derive from Create (MIT), QSL (Apache-2.0), SecurityCraft (MIT), Neruina (MIT) and FramedBlocks (LGPL) |

## How the port is organised

The goal, as for the Create port, is a small diff against upstream so rebases stay manageable.

- **New code lives in `neoforge/`.**
  - `neoforge/src/main/java/com/railwayteam/railways/neoforge`: the `@Mod` entrypoints, bootstrap-time init, the
    `BuiltInRegistriesMixin` and the GameTests.
  - `neoforge/src/main/resources`: `META-INF/neoforge.mods.toml`, `META-INF/accesstransformer.cfg` (port of
    `railways.accesswidener`), `railways-neoforge.mixins.json`, and the `META-INF/services` entry for Create's
    `CreateRegisterPlugin`.
  - `neoforge/src/fabricStubs`: compile-only copies of Fabric's `@Environment`/`EnvType`, so upstream's 46 annotated
    files compile unchanged. Never packaged (CLASS retention; the JVM ignores annotation types it cannot load).
- **The Fabric platform layer is rewritten in place.** Common code calls `…fabric.*Impl` classes directly, so those
  files keep their package and name and now use NeoForge. Edited upstream lines carry a `// NeoForge:` or `// 26.3:`
  comment; files rewritten wholesale say so in their Javadoc.
- **Fabric-only files are excluded in `build.gradle.kts`, not deleted**: the Fabric datagen entrypoint
  (`RailwaysDataFabric`) and `MixinItemInHandRenderer` (replaced by `RenderHandEvent`), on top of upstream's own
  exclusions.
- **The build stays Kotlin DSL.** Loom is replaced by ModDevGradle 2.0.148; upstream's resource processing
  (connected-texture sprite splitting, data-path normalisation, convention tags, advancements, paint recipes, door
  blockstates) is kept as is.
- **Generated data stays in the 26.2 format.** `processResources` converts it, so upstream's generated resources merge
  cleanly:
  - Fabric load conditions on compat track loot tables and recipes become `neoforge:conditions` (`neoforge:mod_loaded`);
    the legacy Forge root `conditions` list on recipes is dropped (26.3 would read it as loot conditions).
  - `tools/data_26_3.py` (from the Create port) then rewrites loot tables, advancements and models to 26.3 and fails
    the build if a 26.2 construct is left. It needs Python 3 on `PATH` (`-Ppython=<exe>` to override).

### Tools

| Script | Purpose |
|---|---|
| `tools/remap_from_moves.py <src dirs>` | Rewrites classes 26.3 moved (`com.mojang.blaze3d` → `com.mojang.renderpearl`, …) from `tools/class-moves-26.3.txt` (copied from the Create port). Re-run after a rebase |
| `tools/data_26_3.py` | Run by `processResources`; see above. Copied from the Create port |
| `tools/check_mixins.py <MC jar> <NeoForge jar> <Create jar> build/classes/java/main` | Static mixin check over Railways' three mixin configs |
| `tools/check_recipe_refs.py`, `tools/worldgen_26_3.py`, `tools/classtweaker2at.py` | Copied from the Create port for later use; Railways has no worldgen |

## Build

```bash
./gradlew build
```

- **Create dependency.** The Create port is not on a maven. `create_neoforge_dir` (default `../_deps`) must hold
  `create_neoforge_jar`, `create-accesstransformer.cfg` (the jar's `META-INF/accesstransformer.cfg`) and
  `create-interfaces.json` (the Create port's `interfaces.json`). The AT and interface file are applied at compile
  time only; at runtime FML applies Create's AT from its jar.
- **Runs.** `runGameTestServer` runs the `railways` and `create` GameTests and exits. `runServer`, `runClient` (opens
  a window; not run yet). Flags: `-PmixinDebug`, `-PwithJei`, `-PskipFuelChecks` (see "Create port fix this needs").
- **Unit tests.** Upstream's 23 JUnit tests run in Fabric's test environment (Fabric Loader, Fabric API, Fabric's mixin
  registration) and are disabled; the GameTests below replace them.

## Loader, entrypoints and registration

| Fabric | NeoForge |
|---|---|
| `main` entrypoint `RailwaysImpl#onInitialize` | Run from `neoforge.mixin.BuiltInRegistriesMixin`, see below |
| `client` entrypoint `RailwaysClientImpl` | `@Mod(dist = CLIENT) neoforge.RailwaysNeoForgeClient` → `RailwaysClientImpl#onInitializeClient(modBus)` |
| `create_plugin` entrypoint (`RailwaysCreateRegisterPlugin`) | `META-INF/services/com.zurrtum.create.api.registry.CreateRegisterPlugin` (Create's ServiceLoader) |
| `jei_mod_plugin` | JEI's `@JeiPlugin` |
| `FabricLoader` (mods, dirs, env) | Create's `neoforge.Platform` (reads the loading mod list, so it works during bootstrap), `FMLPaths`, `FMLEnvironment` |

**Registration timing.** NeoForge freezes the vanilla registries before it constructs mods; Fabric (with Fabric API)
did not. Create's NeoForge port therefore registers everything during vanilla bootstrap, and Railways does the same:

1. Blocks and fluids: Create calls the plugin from `Blocks`/`Fluids` class init, as on Fabric.
2. Everything else (`RailwaysImpl#onInitialize`: items, block entities, entities, sounds, tabs, …):
   `BuiltInRegistriesMixin` runs it inside `BuiltInRegistries.freeze()`, just before the first registry freezes and
   after Create's own registration at the head of that method. It cannot target `Create.register()`: Create's mixin
   plugin loads the `Create` class before other mods' mixins are prepared.
3. Anything that needs the mod event bus, which does not exist yet during bootstrap, is queued with
   `RailwaysNeoForgeInit.onModBus` and attached when `RailwaysNeoForge` is constructed.

## Fabric API replacements

| Fabric API | NeoForge 26.3 |
|---|---|
| Networking (`PayloadTypeRegistry`, `ServerPlayNetworking`, `ClientPlayNetworking`, `PlayerLookup`) | `RegisterPayloadHandlersEvent` / `RegisterClientPayloadHandlersEvent`, `PacketDistributor`, `ClientPacketDistributor`, `ChunkMap#getPlayers`/`getPlayersWatching` (`PacketSetImpl`, `PlayerSelectionImpl`) |
| `CommandRegistrationCallback`, `ClientCommandRegistrationCallback` | `RegisterCommandsEvent`, `RegisterClientCommandsEvent` |
| Server lifecycle/tick/join callbacks, `END_DATA_PACK_RELOAD` | `ServerStartedEvent`/`ServerStoppedEvent`, `LevelTickEvent.Pre`, `PlayerLoggedInEvent`, `TagsUpdatedEvent.ServerDataLoad` |
| Client tick/level/disconnect callbacks | `ClientTickEvent`, `LevelEvent.Load`, `ClientPlayerNetworkEvent.LoggingOut` |
| `ResourceManagerHelper` reload listeners | `AddServerReloadListenersEvent`, `AddClientReloadListenersEvent` |
| `registerBuiltinResourcePack` | `AddPackFindersEvent` (the three optional packs in `resourcepacks/`) |
| `KeyMappingHelper` | `RegisterKeyMappingsEvent`, `KeyMapping#getKey`, `isActiveAndMatches` |
| `ParticleProviderRegistry` | `RegisterParticleProvidersEvent` |
| `ModelLayerRegistry`, entity renderers | `EntityRenderersEvent.RegisterLayerDefinitions` / `RegisterRenderers` |
| `ModelLoadingPlugin` (block model wrappers) | Create's `AllModels` resolvers, which the Create port applies at the same point |
| `FabricCreativeModeTab` | `CreativeModeTab.builder()` |
| `FabricDefaultAttributeRegistry` | `EntityAttributeCreationEvent` |
| `ExtendedMenuType`, `MenuScreens.register` | `IMenuTypeExtension.create`, `RegisterMenuScreensEvent` (registrate `MenuBuilder`; unused upstream) |
| `UseBlockCallback` | `CommonHooks.onRightClickBlock` |
| Transfer: `FluidStorage.SIDED` for fuel tank / portable fuel interface | Create's `NeoForgeTransfer.registerFluid` + `CachedFluidInventoryBehaviour` (NeoForge `Capabilities.Fluid.BLOCK`) |
| Transfer: `FluidStorage.ITEM` for paint pitchers | `Capabilities.Fluid.ITEM` with `PaintPitcherFluidStorage extends ItemAccessResourceHandler<FluidResource>`; a level is 125 mB (Fabric: 10125 droplets); only whole levels move |
| `FluidVariantAttributes` (paint name), `FluidRenderingRegistry`/`FluidVariantRendering` (paint tint) | A `railways:paint` `FluidType` whose `getDescription(FluidStack)` reads the colour; `RegisterFluidModelsEvent` with a component-aware `FluidTintSource` (Create's port reads it for its own renderers) |
| `FluidConstants.BUCKET` | Create's `BucketFluidInventory.CAPACITY` (Create Fly keeps 81000 units per bucket) |
| Fabric `Event` (`BogeyMenuEvents.ENTRY_REGISTRATION`) | A plain listener list (upstream never fires it either) |

## Minecraft 26.2 → 26.3 changes handled

- Block codecs removed (`codec()`, `simpleCodec`); `PushReaction.DESTROY` → `POPPED`; `RedStoneWireBlock` →
  `RedstoneWireBlock`; `EnderMan` → `Enderman`.
- `Prediction` on `placeItemBackInInventory`/`drop`, `SwingAnimation` on `swing`; `PoseStack#mulPose(Quaternion)` →
  `rotate`.
- `FuelValues` is gone: liquid-fuel burn times use Create's `FuelHelper.burnDuration(level, …)`.
- `BlockItem#updateCustomBlockEntityTag` is static: the fuel tank's stack fix-up moved to `placeBlock`, as in Create's
  `FluidTankItem`.
- `Entity#canSimulateMovement` is final: the possessed conductor overrides `getMoveSimulationType()`.
- `FriendlyByteBuf#writeCollection`/`readList` are gone (track switch sync).
- `ServerLevel#getStructureManager` → `MinecraftServer#getStructureTemplateManager` (handcar assembly).
- GLFW → SDL: key codes and mouse buttons use `InputConstants` (`KEY_LALT`, `MOUSE_BUTTON_LEFT` = 1, …).
- `GameRenderer#setPostEffect` → `setSpectatedEntityPostEffect` (conductor spy shader).
- `submitModel` lost the crumbling argument and gained UV mapping and outline colour (conductor cap layer).
- `ItemInHandRenderer` is gone: hiding the player's hands while possessing a conductor uses NeoForge's
  `RenderHandEvent`.
- Data: see "Generated data" above.

## Disabled, approximated or not ported

- **Worn conductor caps on players.** Fabric API's `ArmorRenderer` has no NeoForge counterpart;
  `ConductorCapItemRenderer` is a no-op. Conductors still draw their caps. TODO: a player render layer.
- **Copycat headstock and buffer item models.** Fabric's `modifyItemModelBeforeBake` has no NeoForge hook; those items
  render with their plain JSON model. The block models are wrapped as upstream.
- **Datagen.** Not run: Fabric's datagen entrypoint is excluded and the loot-table builder in `BuilderTransformers`
  is a no-op stub. The generated resources are checked in.
- **Upstream unit tests.** Disabled (Fabric test environment).
- **Client.** Compiles, but `runClient` has not been run (needs the user's OK). Every `client.*` and
  `conductor_possession.*` mixin is therefore unverified at runtime.

## Create port fix this needs

Build against create-fly-neoforge `468f3bd6` or later ("Fix fluid and item stacks failing to save after
setMaxSize"). Before it, `FluidInventory#setMaxSize` put an `Optional<Integer>` into a 26.3 component patch, so fluid
stacks that passed through a `FluidTank` failed to encode: `FuelRuntimeChecks` crashed the dev server and a filled
fuel tank logged "will not persist" on save. `-PskipFuelChecks` skips that probe if an older Create jar must be used.

## Status

Verified on 2026-10-18:

- **Build.** `./gradlew build` builds `railways-neoforge-26.3-1.4.0-zp5.1.jar` (24 MB, 982 classes, no Fabric classes or
  metadata) and passes upstream's `verifyPublishedNamespaces`. The data converter reports 0 leftover 26.2 constructs.
- **Static mixin check.** `tools/check_mixins.py` reports 11 entries, all reviewed: concatenated `target` strings and
  an inner enum mixin the script cannot parse, an all-caps `@Accessor`, two name-only targets with a second overload
  (unchanged from upstream), one `require = 0` injector, and a Javadoc mention of `@Shadow` the script misreads. One real break
  (`setPostEffect`) was fixed.
- **GameTest server** (dev run, and again with only the built jars in a plain NeoForge devrun, `_jartest`):
  - Railways' dev runtime probes pass: bogey styles and gauges; monorail; paint pitcher components, NeoForge fluid
    handler, cannon splitting and the potato projectile; roller track replacement; Shadow Realm storage, commands,
    persistence and the AddTrain codec. Also the fuel probe: block, block entity, mounted storage codec round trip, movement and liquid fuel draining.
  - `railways:railways_track_joins_create_network`: nine `railways:track_acacia` blocks form a Create track graph whose
    edges carry `railways:acacia`.
  - `railways:fuel_tank_takes_lava_not_water`: through NeoForge's `Capabilities.Fluid.BLOCK`, a fuel tank refuses
    water and takes 1000 mB of lava (81000 Create units).
  - Create's own `water_wheel_drives_press` and `basin_fluid_survives_save` still pass; no block entity fails to save.
- **Content.** 1489 blocks, 473 items and 3 entities in `en_us.json`.
- **Not checked:** the client, trains actually running (assembly, fuel burn, couplers, conductors), and the full ZP5
  pack.

## Tracking upstream

1. `git fetch upstream` and rebase `zp5/26.3` onto `upstream/fabric-26.2` (or a later upstream branch).
2. Re-run `python -I tools/remap_from_moves.py common/src/main/java fabric/src/main/java`.
3. Conflicts should be limited to `// NeoForge:` / `// 26.3:` lines, the rewritten `fabric/**/*Impl` files,
   `build.gradle.kts`, `gradle.properties` and `railways.mixins.json`.
4. `./gradlew build`, `tools/check_mixins.py`, then `./gradlew runGameTestServer`.
