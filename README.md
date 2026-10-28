# Ars Misc

Ars Nouveau gathering, combat, defense, and exploration additions for Minecraft 1.21.1 and NeoForge. Current version: **0.1.0**.

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

## Combat and defense

Spell Critical Glasses provide spell critical chance and restore mana on successful critical hits. The Necklace of Mana Mastery provides critical damage and converts maximum mana into spell power. The Enchanter's Belt increases spell damage and clears ordinary hurt cooldowns before spell damage.

Focus of Paladin enhances Abjuration glyphs, reduces their mana cost, and adds absorption health to Heal and Regeneration. Consuming absorption health through damage cleanses one harmful effect. Dispel removes only beneficial effects from non-allied targets and leaves allied status effects intact; native entity-specific dispel behavior remains intact. Regeneration accepts one Amplify for level II; the focus's free Amplify can raise it to level III.

Final Barrier consumes mana to block damage and prevent death. Insufficient mana still completes the current protection, then disables the barrier and starts its cooldown. A message and sound announce when it is ready for manual reactivation.

## Glyphs and exploration

- **Life Sense** reveals nearby living creatures through walls.
- **Loot Sense** reveals nearby unopened loot containers, including container vehicles, without generating their loot.
- **Regeneration**, **Sacrificial Shield**, and **Crystal Shield** provide healing, absorption health, and damage reduction.
- **Crystallization** deals damage and grants one to four amethyst shards on direct kills. Extend Time or Duration Down replaces damage with a timed general spell-resistance reduction, up to 30 percentage points at level III.
- **Flight** grants temporary flight.
- **Decay** converts healing into damage on targets already affected by Wither.
- **Soul Harvest** grants permanent, configurable maximum mana and mana regeneration growth on confirmed kills.
- **Avada Kedavra** deals overwhelming damage followed by an execution attempt, subject to totems and death protection.

Soul Harvest and Avada Kedavra remain available in creative mode, but their scribing recipes are disabled by default. Enable either recipe under `[glyph_recipes]` in `config/ars_misc-common.toml`, then reload recipes or restart.

Looking at an Ars Nouveau portal displays its destination dimension and coordinates in the native block tooltip.

## Casting tools and equipment

The Enchanter's Pocket Watch periodically casts an inscribed Self spell. Enchanter's Field directly applies inscribed effects to eligible creatures in a configurable radius, paying both upkeep and spell costs. The watch, field, and Final Barrier share one dedicated **Casting Device** slot and one configurable toggle key.

Curios-equipped spellbooks support casting, spell selection, editing, quick-cast keys, and a compact HUD. Aiming at a Scribe's Table holding a scribeable item and pressing the cast key inscribes the selected spell instead of casting, using the item's native scribing rules. The Combat Enchanter's Wand preserves complete inscribed spells, casts with the left mouse button, supports holding to repeat, and reduces its spell costs by 20 mana.

The Netherite Enchanter's Sword grants a main-hand spell damage multiplier. Its melee-triggered inscribed spells bypass armor and inherit Attack Damage; other casting tools receive only the attribute bonus. Two Enchanting Apparatus upgrade paths accept an Enchanter's Sword or a Netherite Sword and preserve their custom data.

Netherite mage armor extends the native mage armor upgrade routes and thread system. Fortification, Resilience, and Flight threads add armor scaling, armor toughness, and flight. A one-time spellbook blessing provides additional maximum mana, mana regeneration, and spell power while the book is equipped.

## Integration and usability

Break spells inherit the caster's Curios loot context and active effects for block harvesting. Touch targeting uses entity interaction range for player entity targets. Amplified Cut can disable shields. Additional fixes cover contained-entity trading and spellbooks on lecterns. An optional JEI integration hides repetitive Reactive enchantment application recipes without disabling the enchantment.

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
libs/nuggets-neoforge-1.21.1-1.1.0.48.jar
libs/maven/com/alexthw/sauce/sauce-1.21.1/0.0.62/sauce-1.21.1-0.0.62.jar
```

The local Maven metadata for Sauce is included. The upstream source URL and license for the embedded library are recorded in `src/main/resources/THIRD_PARTY_NOTICES.md` and `src/main/resources/SAUCE-LICENSE.txt`. The optional JEI integration also requires `libs/jei-1.21.1-neoforge-19.70.0.458.jar` on the compile classpath; JEI is not bundled or required at runtime.

```powershell
./gradlew.bat build
./gradlew.bat runGameTestServer
```

On other platforms, use `./gradlew`. The output is `build/libs/ars_misc-0.1.0.jar`. GameTest sources are kept separate from the production JAR. Gathering, combat, defense, casting, recipe loading, and compatibility behavior have server GameTest coverage; client visuals and full modpack behavior also require in-game testing.

## License

MIT for Ars Misc source code. Third-party dependencies remain subject to their respective upstream licenses.

