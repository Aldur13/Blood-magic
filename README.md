# Blood Magic

A Fabric mod for **Minecraft 26.2** that lets you sacrifice hearts for powerful temporary combat buffs. Press **B** to open the menu, choose a buff, and pay with your health.

---

## How It Works

- Open the **Blood Magic menu** by pressing **`B`** in-game
- Each buff costs a number of **hearts** (shown with ♥ icons)
- On activation you immediately **lose those hearts** and gain the buff
- You **cannot regenerate** the spent hearts while the buff is active
- After the buff expires there is a **10-minute cooldown** — then your missing hearts slowly regen back to full

**Example:** 10 hearts → activate Bloodlust (2♥ cost) → 8 hearts max for 8 minutes → 10-min cooldown → hearts return

> You cannot activate a buff if it would kill you (health must exceed the cost).

---

## The 10 Buffs

| Buff | Effect | Heart Cost | Duration |
|------|--------|:----------:|----------|
| **Crimson Sight** | Night vision | 1 ♥ | 10 min |
| **Coagulation** | 60% knockback resistance | 2 ♥ | 8 min |
| **Bloodlust** | +25% melee damage | 2 ♥ | 8 min |
| **Swift Blood** | +25% movement speed | 2 ♥ | 6 min |
| **Iron Blood** | +6 armor points | 3 ♥ | 5 min |
| **Berserker** | +40% attack speed | 3 ♥ | 4 min |
| **Hemorrhage** | Attacks poison enemies for 3s | 3 ♥ | 4 min |
| **Blood Ward** | Absorbs the next hit you take | 4 ♥ | 3 min |
| **Life Drain** | Hits restore 1 health | 4 ♥ | 3 min |
| **Blood Fury** | +50% melee damage | 6 ♥ | 2 min |

Multiple buffs can be active at the same time — costs stack.

---

## Active Buff Display

Active buffs appear as **custom potion effects** in the top-right corner of your screen (standard Minecraft effect HUD), each with a unique colored icon and a countdown timer. A second custom HUD bar shows cooldown progress after the buff expires.

---

## Installation

**Requirements:**
- Minecraft Java Edition **26.2**
- [Fabric Loader](https://fabricmc.net/) **0.19.3+**
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.152.2+26.2**

**Steps:**
1. Install Fabric Loader for Minecraft 26.2
2. Download `bloodmagic-0.1.0.jar` from the [Releases](../../releases) page
3. Drop the `.jar` into your `.minecraft/mods/` folder
4. Also drop `fabric-api-0.152.2+26.2.jar` into `mods/` if you don't have it already
5. Launch Minecraft with the Fabric profile

---

## Controls

| Key | Action |
|-----|--------|
| **B** | Open / close Blood Magic buff menu |
| **ESC** | Close the menu without activating |

---

## Building from Source

```bash
git clone https://github.com/Aldur13/Blood-magic.git
cd Blood-magic
./gradlew build
# Output: build/libs/bloodmagic-0.1.0.jar
```

Requires Java 25+ and an internet connection for the first build (Gradle downloads Minecraft mappings).

---

## Notes & Tips

- **Blood Fury** (6♥) is the most expensive buff — don't activate it if you're below 7 hearts
- **Blood Ward** consumes immediately on the first hit, then enters its 10-min cooldown early
- **Hemorrhage + Life Drain** stack well together — poison the enemy while healing yourself
- The B key only works when no other screen is open (inventory, pause menu, etc.)
