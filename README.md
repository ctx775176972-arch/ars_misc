# Ars Misc

Ars Nouveau gathering, combat, and spell utility additions for Minecraft 1.21.1 and NeoForge. Current version: **0.0.1**.

## Mining rings

Equip these accessories in active Curios ring slots to add augments to the Break glyph without changing the saved spell or adding mana costs. Only the highest equipped tier of each series applies. Fortune remains capped at IV, including augments already present in the spell.

| Item | Added augments |
| --- | --- |
| Lesser Lucky Miner Ring | Fortune x2 |
| Greater Lucky Miner Ring | Fortune x4 |
| Lesser Blasting Miner Ring | Amplify x1, AOE x2 |
| Greater Blasting Miner Ring | Amplify x2, AOE x2, Pierce x2 |

All four rings are crafted in the Enchanting Apparatus with eight Arcane Pedestals and no Source cost. Lesser rings use a Ring of Potential as the central reagent; greater rings use the corresponding lesser ring.

| Ring | Pedestal materials |
| --- | --- |
| Lesser Lucky Miner | Diamond Pickaxe, Rabbit's Foot, 4 Gold Ingots, 2 Emeralds |
| Greater Lucky Miner | Netherite Pickaxe, 2 Rabbit's Feet, 3 Gold Blocks, 2 Emerald Blocks |
| Lesser Blasting Miner | Diamond Pickaxe, 5 TNT, 2 Source Gems |
| Greater Blasting Miner | Netherite Pickaxe, 5 End Crystals, 2 Source Gem Blocks |

## Experimental content

Additional content remains under development: spell critical accessories, a Decay accessory, an advanced Enchanter's Sword, an automatic Enchanter's Pocket Watch, and Life Sense, Regeneration, and Sacrificial Shield glyphs. Their presence does not imply finalized balance, recipes, or progression.

English and Simplified Chinese game localization are included. Repository documentation and source comments are written in English.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.250 or later in the 21.1 series
- Ars Nouveau 5.13.3
- Curios 9.5.1
- GeckoLib 4.9.3

Sauce Library 0.0.62 is embedded in the built mod through Jar-in-Jar. It retains its own license; Ars Misc is licensed under MIT.

## Building

Use Java 21 and the included Gradle wrapper. Local dependency binaries are excluded from this repository. Obtain the matching upstream mod files and place them at these paths before building:

```text
libs/ars_nouveau-1.21.1-5.13.3.jar
libs/curios-neoforge-9.5.1+1.21.1.jar
libs/geckolib-neoforge-1.21.1-4.9.3.jar
libs/maven/com/alexthw/sauce/sauce-1.21.1/0.0.62/sauce-1.21.1-0.0.62.jar
```

The local Maven metadata for Sauce is included. The upstream source URL and license for the embedded library are recorded in `src/main/resources/THIRD_PARTY_NOTICES.md` and `src/main/resources/SAUCE-LICENSE.txt`.

```powershell
./gradlew.bat build
./gradlew.bat runGameTestServer
```

On other platforms, use `./gradlew`. The output is `build/libs/ars_misc-0.0.1.jar`. GameTest sources are kept separate from the production JAR. The current mining behavior, recipe loading, and experimental mechanics have server GameTest coverage; full modpack behavior also requires in-game testing.

## License

MIT for Ars Misc source code. Third-party dependencies remain subject to their respective upstream licenses.

