# Blood Magic

A **server-side-only** Fabric mod for **Minecraft 26.2** that lets you sacrifice hearts for powerful temporary combat buffs. Craft a **Blood Altar**, right-click it, choose a buff, and pay with your health.

No client mod required — players connect with a completely vanilla client.

---

## How It Works

- Craft a **Blood Altar** (recipe below) and place it
- Right-click the altar to open the **Blood Altar menu** — a vanilla chest-style interface
- Click a buff to activate it. Each buff costs a number of **hearts** (shown with ♥ icons)
- On activation you immediately **lose that % of your current max health** and gain the buff
- Costs are a **percentage of your current max health**, not a flat number of hearts — this scales correctly if you're running a lifesteal-style mod that grows or shrinks your max health over time
- You **cannot regenerate** the spent health while the buff is active
- After the buff expires there is a **10-minute cooldown** — then your missing health slowly regens back to full

**Example:** 20 max health → activate Blood Ward (40% cost) → lose 8 max health for 3 minutes → 10-min cooldown → health returns

> You cannot activate a buff if it would kill you (current health must exceed the cost).

---

## Crafting the Blood Altar

```
[Nether Bricks] [Redstone] [Nether Bricks]
[Redstone]      [Cauldron] [Redstone]
[Nether Bricks] [Redstone] [Nether Bricks]
```

---

## The 10 Buffs

| Buff | Effect | Cost (% max health) | Duration |
|------|--------|:--------------------:|----------|
| **Blood Rush** | Speed I | 10% | 10 min |
| **Coagulation** | 60% knockback resistance | 20% | 8 min |
| **Bloodlust** | +25% melee damage | 20% | 8 min |
| **Swift Blood** | +25% movement speed | 20% | 6 min |
| **Iron Blood** | +6 armor points | 30% | 5 min |
| **Berserker** | +40% attack speed | 30% | 4 min |
| **Hemorrhage** | Attacks poison enemies for 3s | 30% | 4 min |
| **Blood Ward** | Absorbs the next hit you take | 40% | 3 min |
| **Life Drain** | Hits restore 1 health | 40% | 3 min |
| **Blood Fury** | +50% melee damage | 60% | 2 min |

Multiple buffs can be active at the same time — costs stack.

---

## Active Buff Display

Active buffs appear as **custom potion effects** in the top-right corner of your screen (standard Minecraft effect HUD), each with a unique colored icon and a countdown timer — this is fully vanilla behavior, so it works with no client mod installed.

---

## Installation (Server-Side Only)

**Requirements:**
- Minecraft Java Edition **26.2**
- [Fabric Loader](https://fabricmc.net/) **0.19.3+** on the server
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.152.2+26.2** on the server

**Steps:**
1. Install Fabric Loader for Minecraft 26.2 on your server
2. Download `bloodmagic-0.2.0.jar` from the [Releases](../../releases) page
3. Drop the `.jar` into your server's `mods/` folder
4. Also drop `fabric-api-0.152.2+26.2.jar` into `mods/` if you don't have it already
5. Restart the server

Players join with a **vanilla client** — nothing to install on their end.

---

## Building from Source

```bash
git clone https://github.com/Aldur13/Blood-magic.git
cd Blood-magic
./gradlew build
# Output: build/libs/bloodmagic-0.2.0.jar
```

Requires Java 25+ and an internet connection for the first build (Gradle downloads Minecraft mappings).

---

## Notes & Tips

- **Blood Fury** (60% cost) is the most expensive buff — you need more than 60% of your max health remaining to activate it
- **Blood Ward** consumes immediately on the first hit, then enters its 10-min cooldown early
- **Hemorrhage + Life Drain** stack well together — poison the enemy while healing yourself
- All costs scale with your *current* max health, so this mod plays nicely with lifesteal-style mods that change your max health over time
- The altar's block/item textures are currently placeholders (red nether bricks + redstone block) — swap the models in `assets/bloodmagic/models` for custom art whenever you want
