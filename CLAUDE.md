# CLAUDE.md - Stormcraft-Essence

This file provides guidance to Claude Code (claude.ai/code) when working with the Stormcraft-Essence addon.

## VPS Environment Context

This addon lives on the **Stormcraft production VPS** alongside the core Stormcraft plugin. The server runs under **Pterodactyl** panel management.

### Build & Deployment Workflow

When building the addon JAR, **always build directly into the Pterodactyl plugins directory**:

```bash
cd /var/repos/Stormcraft-Essence
mvn clean package && cp target/stormcraft-essence-*.jar /var/lib/pterodactyl/volumes/31a2482a-dbb7-4d21-8126-bde346cb17db/plugins/
```

After building, restart the Minecraft server via Pterodactyl panel or console command to load the new version.

### Important Notes
- This is a **production environment** - test thoroughly before building
- The addon requires both **Stormcraft** (core) and **Vault** to function
- Configuration files persist in `plugins/Stormcraft-Essence/` subdirectory
- Player data is stored in `plugins/Stormcraft-Essence/playerdata/*.json`

## Project Overview

**Stormcraft-Essence** is an addon for the Stormcraft plugin that adds a storm-fused ability progression system. Players earn **Storm Exposure Level (SEL)** by accumulating essence from storm exposure, which unlocks powerful passive abilities that drain essence when active.

## Core Mechanics

### Storm Exposure Level (SEL)
- Permanent progression metric based on **lifetime essence earned from storms only**
- Calculated as: `SEL = floor(sqrt(totalStormEssence / 10))`
- Does NOT count essence from trading/selling items - only from storm exposure
- Displayed in chat prefixes via PlaceholderAPI: `%stormessence_sel%`

### Passive Abilities
Three storm-fused abilities unlock at different SEL thresholds:

1. **Storm Resistance** (SEL 10+)
   - Reduces storm damage by a percentage
   - Scales with SEL (base 5% + 0.5% per level, max 50%)

2. **Lightning Reflexes** (SEL 15+)
   - Grants Speed effect during storms
   - Amplifier scales with SEL (base Speed I, +1 amp per 10 levels, max Speed IV)

3. **Stormborn** (SEL 20+)
   - Regenerates health when exposed to storms
   - Scales with SEL (base 0.5 HP/s + 0.1 per 5 levels, max 2.0 HP/s)

### Essence Drain System
**Critical Design Rule:** Players cannot accrue essence from storms while passive abilities are active.

- Passives drain essence from Vault balance at configurable rates
- Drain rate scales with:
  - Number of active passives (multiplicative stacking)
  - Player's SEL tier (higher level = more expensive)
- If essence runs out, all passives auto-disable
- This creates strategic choice: "Use powers now or grind more essence?"

## Architecture

### Package Structure

```
dev.ked.stormcraft.essence
 ├─ StormcraftEssencePlugin.java (main plugin class)
 ├─ config/EssenceConfig.java
 ├─ model/
 │   ├─ PlayerEssenceData.java (SEL + active passives)
 │   └─ PassiveAbility.java (enum: STORM_RESISTANCE, LIGHTNING_REFLEXES, STORMBORN)
 ├─ persistence/PlayerDataManager.java (JSON persistence)
 ├─ ability/AbilityManager.java (drain mechanics, effect calculations)
 ├─ listener/
 │   ├─ EssenceAwardListener.java (tracks StormcraftEssenceAwardEvent)
 │   └─ PassiveAbilityListener.java (applies ability effects via Stormcraft events)
 ├─ command/EssenceCommand.java
 └─ integration/PlaceholderAPIIntegration.java
```

### Integration with Core Stormcraft

The addon listens to the `StormcraftEssenceAwardEvent` fired by the core plugin's `DamageTask` whenever a player earns essence from storm exposure. This event includes:
- Player
- Essence amount
- Storm type
- Exposure duration

### Key Events Used

**From Stormcraft Core:**
- `StormcraftEssenceAwardEvent` - Track essence earned from storms (for SEL)
- `StormcraftExposureCheckEvent` - Modify storm damage (Storm Resistance passive)
- `StormcraftStormTickEvent` - Apply Lightning Reflexes + Stormborn effects

## Commands

- `/essence` - Show status (SEL, total essence, balance, active passives, drain rate)
- `/essence toggle <resistance|reflexes|stormborn>` - Toggle passive on/off
- `/essence info [ability]` - View ability details and current effect strength
- `/essence help` - Show command help

## Configuration

**config.yml** includes:
- Unlock level requirements per ability
- Ability scaling formulas (base values, per-level bonuses, caps)
- Drain rate settings (base rate, SEL scaling, multi-passive multiplier, interval)
- MiniMessage-formatted messages

## PlaceholderAPI Integration

For chat prefixes, use:
- `%stormessence_sel%` - Raw SEL number
- `%stormessence_sel_formatted%` - Formatted as `[SEL X]` (empty if 0)
- `%stormessence_total%` - Total storm essence earned
- `%stormessence_balance%` - Current essence balance
- `%stormessence_drain_rate%` - Current drain rate per second
- `%stormessence_active_count%` - Number of active passives
- `%stormessence_active_list%` - Comma-separated list of active abilities
- `%stormessence_has_resistance%` - true/false
- `%stormessence_has_reflexes%` - true/false
- `%stormessence_has_stormborn%` - true/false

Example chat format: `[SEL 25] <PlayerName>: message`

## Development Notes

### Technology Stack
- Paper API 1.21
- Java 21
- Maven
- Dependencies: Stormcraft (core), Vault (hard), PlaceholderAPI (soft)

### Persistence
- Player data stored as JSON in `playerdata/<uuid>.json`
- Tracks: `totalStormEssence`, `activePassives` list
- Auto-saves on player logout and plugin disable

### Performance
- Drain task runs every 20 ticks (1 second) by default
- Only processes online players with active passives
- Minimal overhead when no players have passives enabled

### Future Expansion Ideas
- Active abilities (lightning strike, teleport, shield)
- Ability tree/unlock progression UI
- Essence trading/crafting
- Storm-infused items/enchantments
