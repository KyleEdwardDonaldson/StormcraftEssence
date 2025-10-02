# Stormcraft-Essence

**Storm Exposure Level (SEL) progression system with passive and active abilities for Stormcraft**

Stormcraft-Essence is an addon for the [Stormcraft](../Stormcraft) plugin that adds a storm-fused ability progression system. Players earn **Storm Exposure Level (SEL)** by accumulating essence from storm exposure, which unlocks powerful passive and active abilities.

## Overview

**Stormcraft-Essence** handles all economy features for Stormcraft, including essence rewards, Storm Exposure Level (SEL) progression, and powerful storm-themed abilities. This plugin manages the Vault economy integration and tracks player progression through surviving storms.

### Architecture

- **Stormcraft** fires `StormcraftEssenceAwardEvent` when players survive storm exposure
- **Stormcraft-Essence** listens for these events and deposits essence via Vault
- **Decoupled design** - Stormcraft has no direct Vault dependency

## Features

### Economy System
- **Vault integration** - Deposits essence as economy currency
- **Event-driven** - Listens to Stormcraft's essence award events
- **Configurable rewards** - Base essence per tick, storm type multipliers
- **Zone multipliers** - Bonus essence in dangerous zones (Stormlands, Storm Zone)

### Storm Exposure Level (SEL)
- Earned by surviving storm exposure
- Formula: `SEL = floor(sqrt(totalEssence / 10))`
- Example progression: 100 essence = SEL 3, 1000 = SEL 10, 10000 = SEL 31
- SEL progression tracks separately from spendable balance

### Passive Abilities

| Ability | SEL Required | Effect |
|---------|--------------|--------|
| **Storm Resistance** | 5 | Reduces storm damage taken |
| **Lightning Reflexes** | 15 | Increased movement speed during storms |
| **Stormborn** | 30 | Regenerate health when exposed to storms |
| **Stormrider** | 100 | Flight in storms, glide outside, no fall damage (toggle with compass) |

### Active Compass Abilities

All active abilities are triggered by right-clicking a compass. Use **Sneak + Right-Click** to cycle between unlocked abilities.

| Ability | SEL Required | Cost | Cooldown | Effect |
|---------|--------------|------|----------|--------|
| **Storm Sense** | 10 | 10 essence | 60s | Particle trail showing path to nearest storm edge |
| **Eye of the Storm** | 25 | 500 essence | 20 min | 10-minute storm immunity bubble |
| **Stormcaller** | 40 | 100 essence | 30s | Summon lightning strike at target location |
| **Stormclear** | 50 | 2000 essence | 60 min | Push all nearby storms away with temporary speed boost |
| **Stormrider** | 100 | Free | None | Toggle flight mode in storms |

### Storm Infusion System
- **Infusion Pedestals** - Place with `/storm infuse` command
- **Time-based progression** - Leave items in pedestal during storms
- **SEL-gated tiers** - Higher SEL unlocks stronger infusions
- **Armor infusion** - Reduce storm damage by up to 95% at max tier
- **Visual effects** - Particles show infusion progress

## Commands

### Player Commands
- `/essence` - View your SEL and essence balance
- `/essence toggle <ability>` - Enable/disable a passive ability
- `/essence info <ability>` - View detailed ability information
- `/storm infuse` - Place infusion pedestal (costs essence)

## Installation

### Requirements
- **Stormcraft** plugin (required)
- **Vault** plugin (required for economy)
- Paper/Spigot server running Minecraft 1.21+

### Setup
1. Install Stormcraft and Vault
2. Drop `Stormcraft-Essence.jar` into your server's `plugins/` folder
3. Restart the server
4. Configure ability settings in `plugins/Stormcraft-Essence/config.yml`

## Configuration

### Essence Economy (config.yml)

```yaml
# Economy settings
economy:
  enabled: true
  essencePerTick: 0.1  # Base essence per exposure check
  essenceMultipliers:  # Storm type multipliers
    shortWeak: 1.0
    medium: 2.0
    longDangerous: 4.0

# Infusion system
infusion:
  pedestal_cost: 1000.0  # Essence cost to place pedestal

# Essence drain for passive abilities
essenceDrain:
  baseDrainPerSecond: 0.5
  drainIntervalTicks: 20
  selScaling: true
  drainPerTenLevels: 0.2
  multiPassiveMultiplier: 1.5

# Passive ability unlock levels
passiveAbilities:
  stormResistance:
    unlockLevel: 5
    baseReduction: 0.20
    perLevelBonus: 0.01
    maxReduction: 0.80
  lightningReflexes:
    unlockLevel: 15
    baseSpeed: 1
    per10LevelsBonus: 1
    maxSpeed: 3
  stormborn:
    unlockLevel: 30
    baseRegen: 0.5
    per5LevelsBonus: 0.1
    maxRegen: 2.0
  stormrider:
    unlockLevel: 100
```

## How It Works

### Event-Driven Architecture

1. **Stormcraft fires event** - When player survives storm exposure, Stormcraft fires `StormcraftEssenceAwardEvent`
2. **Essence listener processes** - Stormcraft-Essence's `EssenceAwardListener` runs at LOWEST priority
3. **Vault deposit** - Essence deposited to player's economy balance via Vault
4. **SEL tracking** - Total essence tracked separately for SEL calculation
5. **Extensible** - Other plugins can listen to the event and modify essence amounts

### Compass Navigation
- Your compass automatically points away from the nearest storm
- Always active when holding a compass

### Ability Cycling
1. Enable abilities with `/essence toggle <ability>`
2. **Right-Click** compass to use selected ability
3. **Sneak + Right-Click** to cycle to next unlocked ability
4. Selected ability name displays on screen

### Essence Economy
- Essence awarded automatically during storms (via Stormcraft event)
- Passive abilities drain essence over time when enabled
- Active abilities consume essence on use
- Drain rate increases with SEL and number of active passives
- If essence runs out, all passive abilities auto-disable

### Infusion System
1. Use `/storm infuse` to place a pedestal (costs essence)
2. Place armor/items on pedestal by right-clicking
3. Leave items for 24-48 hours (real-world time)
4. Progress: 0-70% in first 24h, 70-100% in second 24h
5. Max tier depends on your SEL (SEL 100 = Tier 5 = 95% protection)
6. Retrieve items by sneak+left-click on pedestal

## Permissions

- `stormcraft.essence.use` - Use /essence command (default: true)
- `stormcraft.essence.admin` - Admin commands (default: op)

## PlaceholderAPI Support

- `%stormcraft_essence_sel%` - Player's Storm Exposure Level
- `%stormcraft_essence_total%` - Total storm essence earned
- `%stormcraft_essence_balance%` - Current essence balance

## API Usage

### For Plugin Developers

**Listening to Essence Events:**

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onEssenceAward(StormcraftEssenceAwardEvent event) {
    Player player = event.getPlayer();
    double essence = event.getEssenceAmount();
    StormType type = event.getStormType();
    Location location = event.getLocation();

    // Modify essence amount
    event.setEssenceAmount(essence * 1.5);

    // Or cancel the event
    if (someCondition) {
        event.setCancelled(true);
    }
}
```

**Accessing Player Data:**

```java
// Get player's SEL
StormcraftEssencePlugin essence = (StormcraftEssencePlugin) Bukkit.getPluginManager().getPlugin("Stormcraft-Essence");
PlayerDataManager pdm = essence.getPlayerDataManager();
PlayerEssenceData data = pdm.getPlayerData(player);
int sel = data.getStormExposureLevel();

// Check active abilities
Set<PassiveAbility> active = data.getActivePassives();
boolean hasStormResist = active.contains(PassiveAbility.STORM_RESISTANCE);
```

## Building

```bash
mvn clean package
```

Output: `target/stormcraft-essence-0.1.0.jar`

## Economy Balancing

The essence drain system is designed to make passive abilities a **luxury sink** rather than permanent upgrades:

**Income (Passive Storm Farming):**
- ~$5/minute from storm exposure (base)

**Drain Costs (Passive Abilities):**
- 1 passive active: $0.50-1.00/second (~$30-60/minute)
- 2 passives active: $1.00-2.00/second (~$60-120/minute)
- 3 passives active: $1.50-3.00/second (~$90-180/minute)

**Result:** Players must actively engage in high-value activities (events, dungeons, trading) to sustain multiple passive abilities.

---

## Troubleshooting

### Passive Abilities Not Working
1. Check SEL meets unlock requirement: `/essence info <ability>`
2. Verify essence balance is sufficient: `/essence`
3. Check if ability is enabled: `/essence toggle <ability>`
4. Look for errors in console logs

### SEL Not Increasing
1. Verify essence is from **storm exposure only** (not trading)
2. Check `StormcraftEssenceAwardEvent` is firing (enable debug mode in Stormcraft)
3. Ensure Vault integration is working: `/vault-info`

### Compass Abilities Not Triggering
1. Check SEL meets unlock requirement
2. Verify ability is off cooldown: `/essence`
3. Must be holding a compass in main hand
4. Check console for errors

---

## Roadmap

- [ ] Ability tree UI with branching unlock paths
- [ ] Essence crafting system (combine crystals for special items)
- [ ] Storm-infused weapons with special abilities
- [ ] Group abilities (share buffs with party members)
- [ ] Essence trading marketplace integration

---

## License

Proprietary - Quetzal's Stormcraft Server
**Developer:** Kyle Edward Donaldson (kyle@ked.dev)

---

**Version:** 0.1.0
**Last Updated:** 2025-10-02
**Minecraft Version:** Paper 1.21.9
