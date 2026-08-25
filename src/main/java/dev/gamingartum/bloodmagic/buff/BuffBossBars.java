package dev.gamingartum.bloodmagic.buff;

import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buff timers, drawn with vanilla boss bars.
 *
 * <p>This replaces the mod's old custom HUD element. Boss bars are pure server state pushed
 * over standard packets, so an unmodified client renders them with no mod installed.
 */
public final class BuffBossBars {

    private static final Map<UUID, Map<BloodBuff, ServerBossEvent>> BARS = new HashMap<>();

    private BuffBossBars() {
    }

    /**
     * Brings the player's bars in line with their entries: adds what is missing, drops what is
     * gone, and retitles the rest. Safe to call every tick, though once a second is plenty.
     */
    public static void update(ServerPlayer player, List<ActiveBuffEntry> entries, long now) {
        Map<BloodBuff, ServerBossEvent> bars = BARS.computeIfAbsent(
            player.getUUID(), uuid -> new EnumMap<>(BloodBuff.class));

        List<BloodBuff> stale = new ArrayList<>(bars.keySet());

        for (ActiveBuffEntry entry : entries) {
            BloodBuff buff = entry.buff();
            stale.remove(buff);

            boolean active = entry.isEffectActive(now);
            long remaining = entry.remaining(now);
            long phase = Math.max(1L, entry.phaseLength(now));

            ServerBossEvent bar = bars.get(buff);
            if (bar == null) {
                bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(),
                    buff.barColor, BossEvent.BossBarOverlay.PROGRESS);
                bar.setDarkenScreen(false);
                bar.setPlayBossMusic(false);
                bar.setCreateWorldFog(false);
                bar.addPlayer(player);
                bars.put(buff, bar);
            }

            bar.setColor(active ? buff.barColor : BossEvent.BossBarColor.BLUE);
            bar.setName(active
                ? Component.literal(buff.displayName + "  " + clock(remaining))
                    .withStyle(ChatFormatting.RED)
                : Component.literal(buff.displayName + "  regenerating  " + clock(remaining))
                    .withStyle(ChatFormatting.GRAY));
            bar.setProgress(Math.clamp((float) remaining / (float) phase, 0.0f, 1.0f));
        }

        for (BloodBuff gone : stale) {
            ServerBossEvent bar = bars.remove(gone);
            if (bar != null) bar.removeAllPlayers();
        }

        if (bars.isEmpty()) BARS.remove(player.getUUID());
    }

    /**
     * Tears down every bar for this player. Also called when the ServerPlayer entity is
     * replaced (respawn, End return), because the old bars hold a reference to the dead entity.
     */
    public static void remove(ServerPlayer player) {
        Map<BloodBuff, ServerBossEvent> bars = BARS.remove(player.getUUID());
        if (bars == null) return;
        for (ServerBossEvent bar : bars.values()) {
            bar.removeAllPlayers();
        }
    }

    private static String clock(long ticks) {
        long seconds = ticks / 20L;
        return (seconds / 60L) + ":" + String.format("%02d", seconds % 60L);
    }
}
