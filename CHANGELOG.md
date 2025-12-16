# ElytraStamina Plugin - Changelog

**Author:** https://github.com/MaT3joo
**CHANGELOG.md was AI generated**

# IMPORTANT!

Some changes in this plugin version have not yet been tested due to time constraints.
Nevertheless, if any bugs are discovered, I will address them immediately.

## Overview

ElytraStamina is a Minecraft plugin that implements a stamina system for players using Elytra (wings). Players consume stamina while gliding and can use firework rockets to boost, which also costs stamina. The system includes regeneration, penalties for overuse, and a visual boss bar display.

---

## Major Changes & Logic Updates

### 1. **Rocket Use Detection - RIGHT_CLICK_AIR Action**

**Previous Behavior:** Rocket stamina cost was triggered on both left-click and right-click interactions.

**New Behavior:** Rocket stamina cost now only applies when the player performs a **RIGHT_CLICK_AIR** action.

**Technical Implementation:**

```java
event.getAction() == Action.RIGHT_CLICK_AIR
```

This ensures that only intentional right-click rocket usage consumes stamina, preventing accidental stamina loss from left-click actions.

**Configuration:** `rocket-cost: 5.0` (default, divided by 2 in code)

---

### 2. **Zero Stamina Flight Blocker**

**Previous Behavior:** Players could attempt to glide even with zero stamina, and the system would disable flight mid-flight.

**New Behavior:** Players are now **blocked from starting flight** when stamina is at or below 0.

**Technical Implementation:**

```java
if (current <= 0 && event.isGliding()) {
    event.setCancelled(true);
}
```

This event cancellation prevents the EntityToggleGlideEvent, stopping the player from entering glide mode when stamina is depleted.

---

### 3. **No-Stamina Attempt Penalty System**

**New Feature:** Tracks how many times a player attempts to glide with insufficient stamina below the threshold.

**Fields Added:**

- `noStaminaAttempts` - Tracks failed attempts per player
- `penaltiesCount` - Counts total penalties applied
- `staminaResetThreshold` - Threshold below which attempts are counted (default: 2.0)
- `maxNoStaminaAttempts` - Maximum allowed attempts before penalty (default: 3)
- `penaltyVelocity` - Velocity applied as punishment (default: -3.0)

**Logic:**

1. When stamina falls below `staminaResetThreshold` (2.0), attempt counter increments
2. After 3 failed attempts, player receives downward velocity (knockback)
3. Counter resets when stamina recovers above threshold
4. Counter also resets after penalty is applied

**Configuration Parameters:**

- `stamina-reset-threshold: 2.0`
- `max-no-stamina-attempts: 3`
- `penalty-velocity: -3.0`

---

### 4. **Improved Stamina Drain and Regeneration Logic**

**Previous Behavior:** Stamina regenerated whenever player wasn't gliding.

**New Behavior:** Stamina only regenerates when player is:

- NOT gliding AND
- On the ground (touching solid block)

**Technical Implementation:**

```java
boolean touchedGround = true;
if (isFlying) {
    touchedGround = false;
    // ... drain logic
} else if (onGround) {
    touchedGround = true;
}

if (!isFlying && touchedGround && current < maxStamina) {
    current += regenPerTick;
}
```

**Benefits:**

- Players in water/air cannot regenerate stamina
- Forces tactical landing to recover stamina
- Increases difficulty and strategic gameplay

---

### 5. **Better Stamina Task Management**

**Improvements:**

- Task only runs when needed (when stamina isn't full)
- Properly stops when all players have full stamina
- Automatic cleanup prevents memory leaks

**Task Behavior:**

- Runs every 1 tick (20 times per second)
- Monitors all online players
- Stops automatically when no player has less than max stamina

---

### 6. **Player Lifecycle Management**

**New Features:**

- `onQuit()` now cleans up:
  - Boss bar display
  - No-stamina attempt counter
  - Penalties count tracker

**Benefits:** Prevents memory leaks and ensures clean state for player rejoin

---

### 7. **Configuration Reload Command**

**New Command:** `/elytrastamina reload`

**Functionality:**

- Requires permission: `elytrastamina.reload`
- Reloads plugin configuration from disk
- Updates all stamina parameters without restart

**Reloaded Values:**

- `max-stamina`
- `glide-drain-per-second`
- `rocket-cost`
- `regen-per-second`
- `stamina-reset-threshold`
- `penalty-velocity`
- `max-no-stamina-attempts`

---

### 8. **Boss Bar Visual Enhancement**

**Previous Style:** GREEN color, SEGMENTED_10 style

**New Style:** PURPLE color, SOLID style

**Purpose:** Better visual distinction and modern appearance (to be real, it just looks better in my opinion)

---

### 9. **Enhanced Command Structure**

**Commands:**

- `/elytrastamina reload` - Reload configuration
- `/elytrastamina <player> <amount>` - Set specific player's stamina

**New Features:**

- Usage help message when no arguments provided
- Proper permission checking
- Player validation before stamina modification

---

## Configuration File (config.yml)

```yaml
# Maximum stamina capacity
max-stamina: 60.0

# Stamina drained per second while gliding
glide-drain-per-second: 1.0

# Stamina cost for rocket boost (divided by 2 in code - it used to consume 2x value for some reason idk :p)
rocket-cost: 10.0

# Stamina regenerated per second on ground
regen-per-second: 0.3

# Stamina threshold for attempt tracking
stamina-reset-threshold: 2.0

# Maximum failed glide attempts below threshold before penalty
max-no-stamina-attempts: 3

# Velocity applied as penalty (negative = downward)
penalty-velocity: -3.0
```

---

## Permissions

| Permission             | Command                            | Effect               |
| ---------------------- | ---------------------------------- | -------------------- |
| `elytrastamina.reload` | `/elytrastamina reload`            | Reload configuration |
| `elytrastamina.set`    | `/elytrastamina <player> <amount>` | Set player's stamina |

---

## Game Mechanics Summary

### Stamina Drain

- **Active Gliding:** 1.0 stamina/second (configurable)
- **Rocket Boost:** 5.0 stamina per use (configurable)
- **Flight Block:** If stamina ≤ 0, cannot start gliding

### Stamina Regeneration

- **Rate:** 0.3 stamina/second on ground (configurable)
- **Condition:** Player must be on ground AND NOT gliding
- **No Regen:** In water, mid-air, or gliding

### Penalty System

- **Threshold:** Below 2.0 stamina triggers attempt tracking
- **Penalty Trigger:** 3 failed glide attempts (configurable)
- **Penalty Effect:** Downward velocity (-3.0) knocks player down

---

## Data Structures

### Player Tracking

- `stamina: Map<UUID, Double>` - Current stamina value
- `bossBars: Map<UUID, BossBar>` - Visual stamina display
- `noStaminaAttempts: Map<UUID, Integer>` - Failed glide attempt count
- `penaltiesCount: Map<UUID, Integer>` - Total penalties applied to player

---

## Version History

- **Latest:** Rockets only trigger on RIGHT_CLICK_AIR, improved stamina mechanics, penalty system
- **Previous:** Initial stamina system with basic drain/regen
