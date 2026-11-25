// .engine-sync/release_gate.gradle.kts: installed by voice-rpg-engine (scripts/sync_engine_to_games.sh); don't edit
// in the game. Applied from app/build.gradle.kts.
//
// A release build (assembleRelease, bundleRelease) first runs the unit tests in the release variant. There the
// engine's release-gate tests are strict, among them the sprite contract (docs/SPRITE_PLAN.md in the engine):
// a game whose battle sprites fall short can't be released. Debug builds are not affected.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("testReleaseUnitTest")
}
