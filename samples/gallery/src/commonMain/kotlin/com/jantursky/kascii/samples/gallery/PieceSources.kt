package com.jantursky.kascii.samples.gallery

/** Where a piece lives in `kascii-gallery`, so its page can show the line that builds it and link to the source. */
internal class PieceSource(
    val pkg: String,
    val factory: String,
    val file: String,
)

/** One entry per gallery piece, by id. */
internal val PieceSources: Map<String, PieceSource> =
    mapOf(
        "alpine-dawn" to PieceSource("com.jantursky.kascii.gallery", "alpineDawnPiece", "a/AlpineDawn.kt"),
        "hilltop-night" to PieceSource("com.jantursky.kascii.gallery", "hilltopNightPiece", "a/HilltopNight.kt"),
        "donut" to PieceSource("com.jantursky.kascii.gallery", "donutPiece", "a/Donut.kt"),
        "cube" to PieceSource("com.jantursky.kascii.gallery", "cubePiece", "a/SpinningCube.kt"),
        "heart" to PieceSource("com.jantursky.kascii.gallery", "heartPiece", "a/BeatingHeart.kt"),
        "starfield" to PieceSource("com.jantursky.kascii.gallery", "starfieldPiece", "a/Starfield.kt"),
        "solar-system" to PieceSource("com.jantursky.kascii.gallery", "solarSystemPiece", "a/SolarSystem.kt"),
        "pond-ripples" to PieceSource("com.jantursky.kascii.gallery", "pondRipplesPiece", "a/PondRipples.kt"),
        "bouncing-balls" to PieceSource("com.jantursky.kascii.gallery", "bouncingBallsPiece", "a/BouncingBalls.kt"),
        "rain" to PieceSource("com.jantursky.kascii.gallery", "rainPiece", "a/RainShower.kt"),
        "campfire" to PieceSource("com.jantursky.kascii.gallery", "campfirePiece", "a/Campfire.kt"),
        "fractal-tree" to PieceSource("com.jantursky.kascii.gallery", "fractalTreePiece", "a/FractalTree.kt"),
        "interference-field" to
            PieceSource("com.jantursky.kascii.gallery", "interferencePiece", "a/InterferenceField.kt"),
        "jellyfish" to PieceSource("com.jantursky.kascii.gallery", "jellyfishPiece", "b/Jellyfish.kt"),
        "owl" to PieceSource("com.jantursky.kascii.gallery", "owlPiece", "b/Owl.kt"),
        "analog-clock" to PieceSource("com.jantursky.kascii.gallery", "analogClockPiece", "b/AnalogClock.kt"),
        "hourglass" to PieceSource("com.jantursky.kascii.gallery", "hourglassPiece", "b/Hourglass.kt"),
        "lava-lamp" to PieceSource("com.jantursky.kascii.gallery", "lavaLampPiece", "b/LavaLamp.kt"),
        "mandelbrot" to PieceSource("com.jantursky.kascii.gallery", "mandelbrotPiece", "b/Mandelbrot.kt"),
        "glider-gun" to PieceSource("com.jantursky.kascii.gallery", "gliderGunPiece", "b/GliderGun.kt"),
        "langtons-ant" to PieceSource("com.jantursky.kascii.gallery", "langtonsAntPiece", "b/LangtonsAnt.kt"),
        "flames" to PieceSource("com.jantursky.kascii.gallery", "flamesPiece", "b/Flames.kt"),
        "matrix-rain" to PieceSource("com.jantursky.kascii.gallery", "matrixRainPiece", "b/MatrixRain.kt"),
        "synthwave" to PieceSource("com.jantursky.kascii.gallery", "synthwavePiece", "b/Synthwave.kt"),
        "tunnel" to PieceSource("com.jantursky.kascii.gallery", "tunnelPiece", "b/Tunnel.kt"),
        "crt-filter" to PieceSource("com.jantursky.kascii.gallery", "crtFilterPiece", "b/CrtFilter.kt"),
        "lighthouse" to PieceSource("com.jantursky.kascii.gallery", "lighthousePiece", "c/Lighthouse.kt"),
        "steam-train" to PieceSource("com.jantursky.kascii.gallery", "steamTrainPiece", "c/SteamTrain.kt"),
        "sailboat-sunset" to PieceSource("com.jantursky.kascii.gallery", "sailboatSunsetPiece", "c/SailboatSunset.kt"),
        "desert-caravan" to PieceSource("com.jantursky.kascii.gallery", "desertCaravanPiece", "c/DesertCaravan.kt"),
        "aurora-cabin" to PieceSource("com.jantursky.kascii.gallery", "auroraCabinPiece", "d/AuroraCabin.kt"),
        "rainy-city" to PieceSource("com.jantursky.kascii.gallery", "rainyCityPiece", "d/RainyCity.kt"),
        "cherry-blossom" to PieceSource("com.jantursky.kascii.gallery", "cherryBlossomPiece", "d/CherryBlossom.kt"),
        "rocket-launch" to PieceSource("com.jantursky.kascii.gallery", "rocketLaunchPiece", "d/RocketLaunch.kt"),
        "orbit-sunrise" to PieceSource("com.jantursky.kascii.gallery", "orbitSunrisePiece", "e/OrbitSunrise.kt"),
        "volcano-night" to PieceSource("com.jantursky.kascii.gallery", "volcanoNightPiece", "e/VolcanoNight.kt"),
        "abyss-lure" to PieceSource("com.jantursky.kascii.gallery", "abyssLurePiece", "f/AbyssLure.kt"),
        "witch-moon" to PieceSource("com.jantursky.kascii.gallery", "witchMoonPiece", "f/WitchMoon.kt"),
        "waking-giant" to PieceSource("com.jantursky.kascii.gallery.g", "wakingGiantPiece", "g/WakingGiant.kt"),
        "moon-angler" to PieceSource("com.jantursky.kascii.gallery.g", "moonAnglerPiece", "g/MoonAngler.kt"),
        "mountain-lake" to PieceSource("com.jantursky.kascii.gallery.h", "mountainLakePiece", "h/MountainLake.kt"),
        "windmill-meadow" to
            PieceSource(
                "com.jantursky.kascii.gallery.h",
                "windmillMeadowPiece",
                "h/WindmillMeadow.kt",
            ),
        "jungle-temple" to PieceSource("com.jantursky.kascii.gallery.h", "jungleTemplePiece", "h/JungleTemple.kt"),
        "ice-cave" to PieceSource("com.jantursky.kascii.gallery.h", "iceCavePiece", "h/IceCave.kt"),
        "floating-islands" to
            PieceSource("com.jantursky.kascii.gallery.h", "floatingIslandsPiece", "h/FloatingIslands.kt"),
        "canyon-river" to PieceSource("com.jantursky.kascii.gallery.h", "canyonRiverPiece", "h/CanyonRiver.kt"),
        "snow-village" to PieceSource("com.jantursky.kascii.gallery.h", "snowVillagePiece", "h/SnowVillage.kt"),
        "space-station" to PieceSource("com.jantursky.kascii.gallery.i", "spaceStationPiece", "i/SpaceStation.kt"),
        "moon-base" to PieceSource("com.jantursky.kascii.gallery.i", "moonBasePiece", "i/MoonBase.kt"),
        "observatory" to PieceSource("com.jantursky.kascii.gallery.i", "observatoryPiece", "i/Observatory.kt"),
        "desert-highway" to PieceSource("com.jantursky.kascii.gallery.i", "desertHighwayPiece", "i/DesertHighway.kt"),
        "neon-arcade" to PieceSource("com.jantursky.kascii.gallery.i", "neonArcadePiece", "i/NeonArcade.kt"),
        "clock-tower" to PieceSource("com.jantursky.kascii.gallery.i", "clockTowerPiece", "i/ClockTower.kt"),
        "airship-sky" to PieceSource("com.jantursky.kascii.gallery.i", "airshipSkyPiece", "i/AirshipSky.kt"),
        "whale-song" to PieceSource("com.jantursky.kascii.gallery.j", "whaleSongPiece", "j/WhaleSong.kt"),
        "fox-snow" to PieceSource("com.jantursky.kascii.gallery.j", "foxSnowPiece", "j/FoxSnow.kt"),
        "butterfly-meadow" to
            PieceSource("com.jantursky.kascii.gallery.j", "butterflyMeadowPiece", "j/ButterflyMeadow.kt"),
        "dragon-castle" to PieceSource("com.jantursky.kascii.gallery.j", "dragonCastlePiece", "j/DragonCastle.kt"),
        "koi-pond" to PieceSource("com.jantursky.kascii.gallery.j", "koiPondPiece", "j/KoiPond.kt"),
        "firefly-marsh" to PieceSource("com.jantursky.kascii.gallery.j", "fireflyMarshPiece", "j/FireflyMarsh.kt"),
        "sea-turtle-reef" to PieceSource("com.jantursky.kascii.gallery.j", "seaTurtleReefPiece", "j/SeaTurtleReef.kt"),
        "lightning-storm" to
            PieceSource(
                "com.jantursky.kascii.gallery.k",
                "lightningStormPiece",
                "k/LightningStorm.kt",
            ),
        "fireworks-harbor" to
            PieceSource("com.jantursky.kascii.gallery.k", "fireworksHarborPiece", "k/FireworksHarbor.kt"),
        "galaxy-spiral" to PieceSource("com.jantursky.kascii.gallery.k", "galaxySpiralPiece", "k/GalaxySpiral.kt"),
        "aurora-ribbons" to PieceSource("com.jantursky.kascii.gallery.k", "auroraRibbonsPiece", "k/AuroraRibbons.kt"),
        "liquid-metal" to PieceSource("com.jantursky.kascii.gallery.k", "liquidMetalPiece", "k/LiquidMetal.kt"),
        "topographic-contours" to
            PieceSource("com.jantursky.kascii.gallery.k", "topographicContoursPiece", "k/TopographicContours.kt"),
        "torus-knot" to PieceSource("com.jantursky.kascii.gallery.l", "torusKnotPiece", "l/TorusKnot.kt"),
        "icosphere" to PieceSource("com.jantursky.kascii.gallery.l", "icospherePiece", "l/Icosphere.kt"),
        "mobius-strip" to PieceSource("com.jantursky.kascii.gallery.l", "mobiusStripPiece", "l/MobiusStrip.kt"),
        "tesseract" to PieceSource("com.jantursky.kascii.gallery.l", "tesseractPiece", "l/Tesseract.kt"),
        "dna-helix" to PieceSource("com.jantursky.kascii.gallery.l", "dnaHelixPiece", "l/DnaHelix.kt"),
        "spirograph" to PieceSource("com.jantursky.kascii.gallery.l", "spirographPiece", "l/Spirograph.kt"),
        "reaction-diffusion" to
            PieceSource("com.jantursky.kascii.gallery.m", "reactionDiffusionPiece", "m/ReactionDiffusion.kt"),
        "boids-flock" to PieceSource("com.jantursky.kascii.gallery.m", "boidsFlockPiece", "m/BoidsFlock.kt"),
        "falling-sand" to PieceSource("com.jantursky.kascii.gallery.m", "fallingSandPiece", "m/FallingSand.kt"),
        "voronoi-cells" to PieceSource("com.jantursky.kascii.gallery.m", "voronoiCellsPiece", "m/VoronoiCells.kt"),
        "harmonograph" to PieceSource("com.jantursky.kascii.gallery.m", "harmonographPiece", "m/Harmonograph.kt"),
        "double-pendulum" to
            PieceSource(
                "com.jantursky.kascii.gallery.m",
                "doublePendulumPiece",
                "m/DoublePendulum.kt",
            ),
        "pirate-ship-storm" to
            PieceSource("com.jantursky.kascii.gallery.n", "pirateShipStormPiece", "n/PirateShipStorm.kt"),
        "space-elevator" to PieceSource("com.jantursky.kascii.gallery.n", "spaceElevatorPiece", "n/SpaceElevator.kt"),
        "underwater-city" to
            PieceSource(
                "com.jantursky.kascii.gallery.n",
                "underwaterCityPiece",
                "n/UnderwaterCity.kt",
            ),
        "treehouse-dusk" to
            PieceSource(
                "com.jantursky.kascii.gallery.n",
                "treehouseDuskPiece",
                "n/TreehouseDusk.kt",
            ),
        "wizard-tower" to PieceSource("com.jantursky.kascii.gallery.n", "wizardTowerPiece", "n/WizardTower.kt"),
        "market-street" to PieceSource("com.jantursky.kascii.gallery.n", "marketStreetPiece", "n/MarketStreet.kt"),
        "gear-train" to PieceSource("com.jantursky.kascii.gallery.o", "gearTrainPiece", "o/GearTrain.kt"),
        "candle-flame" to PieceSource("com.jantursky.kascii.gallery.o", "candleFlamePiece", "o/CandleFlame.kt"),
        "snow-globe" to PieceSource("com.jantursky.kascii.gallery.o", "snowGlobePiece", "o/SnowGlobe.kt"),
        "lantern-festival" to
            PieceSource("com.jantursky.kascii.gallery.o", "lanternFestivalPiece", "o/LanternFestival.kt"),
        "vinyl-record" to PieceSource("com.jantursky.kascii.gallery.o", "vinylRecordPiece", "o/VinylRecord.kt"),
        "ocean-waves" to PieceSource("com.jantursky.kascii.gallery.o", "oceanWavesPiece", "o/OceanWaves.kt"),
    )
