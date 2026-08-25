# Blood Magic

A **server-side** Fabric mod for **Minecraft 26.2** that lets players sacrifice hearts for powerful temporary combat buffs.

Install it on the server only. **Players join with an unmodified vanilla client and need nothing at all** — no mod, no Fabric, no resource pack.

---

## How It Works

- Run **`/blood`** to open the sacrifice menu
- Each buff costs a number of **hearts**
- On activation your **maximum health drops** by that many hearts, and your current health is clamped down with it
- Those hearts **stay gone** for the whole buff, and for a **10-minute regen cooldown** afterwards
- Once the cooldown ends, your maximum health is restored

**Example:** 10 hearts → activate Bloodlust (2 ♥) → 8 hearts max for 8 minutes → 10-minute cooldown → back to 10 hearts

> You cannot activate a buff unless your current health exceeds its cost, and never below 1 heart of remaining capacity.

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

Multiple buffs can run at once — their heart costs stack. A buff cannot be re-taken until its regen cooldown has finished.

---

## Commands

| Command | Permission | Action |
|---------|:----------:|--------|
| `/blood` | everyone | Open the sacrifice menu |
| `/blood status` | everyone | List your active buffs and cooldowns in chat |
| `/blood use <buff>` | everyone | Activate a buff directly, e.g. `/blood use bloodlust` |
| `/blood clear [player]` | level 2 | Drop all buffs, heart debts and cooldowns |

The command root is `/blood`, not `/bloodmagic` — the sibling `item-import` mod already uses that root.

---

## What Players See

Everything is rendered by the vanilla client from ordinary server packets:

- **The menu** is a standard double-chest container. Each buff is a vanilla item showing its cost, duration and current state in the tooltip; active buffs glint. Click one to sacrifice; the barrier in the corner closes it.
- **Timers** are vanilla boss bars — red while the buff runs, blue while your hearts regenerate, one per buff, counting down.
- **Your hearts** shrink and regrow through the normal health bar, because the cost is a real `max_health` modifier.

---

## Installation

**Requirements:**
- A Minecraft **26.2** dedicated server (or a single-player world — it works there too)
- [Fabric Loader](https://fabricmc.net/) **0.19.3+**
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.152.2+26.2**

**Steps:**
1. Install Fabric Loader for Minecraft 26.2 on the **server**
2. Drop `bloodmagic-0.1.1.jar` into the server's `mods/` folder
3. Drop `fabric-api-0.152.2+26.2.jar` into `mods/` as well
4. Start the server

Connecting players change nothing on their end.

---

## Notes for Operators

- **Buff state is persistent** and stored per player in the world save, so buffs and cooldowns survive restarts and relogs.
- **Death clears everything** — buffs, heart debts and cooldowns — and the player respawns at full health.
- **Timers use world game time**, so they stay correct across server restarts.
- **Heart costs are transient attribute modifiers**, rebuilt from the saved buff list whenever a player logs in, respawns, or returns from the End. Removing the mod cleanly returns everyone to normal maximum health.
- Buff list entries saved by version 0.1.0 use a different, restart-unstable clock; they are treated as expired and discarded the first time each player logs in after the update.

---

## Building from Source

```bash
./gradlew build
```

Output: `build/libs/bloodmagic-0.1.1.jar`. Requires Java 25+ and, on a first build, an internet connection for Gradle to fetch Minecraft and mappings.
