package net.jayvera.leo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

import java.util.Random;

/**
 * Central escalation system. Intensity ramps linearly from 0.0 at Day 0 to
 * 1.0 at Day 20, and drops back to 0 once the ending resolves. Every other
 * horror effect in the mod reads this value to scale its own volume/frequency,
 * so everything gets worse together as the finale approaches instead of each
 * effect escalating independently and inconsistently.
 */
public class Dread {
    private static final Random RANDOM = new Random();
    private static final int HEARTBEAT_INTERVAL_TICKS = 100; // checked every 5 seconds

    public static double intensity(LeoState state) {
        if (state.endgameApplied) return 0.0;
        return Math.min(state.day / 20.0, 1.0);
    }

    /** A rhythmic pulse that starts once the corruption phase begins (Day 10) and builds toward Day 20. */
    public static void heartbeatTick(ServerWorld world, LeoState state) {
        if (state.day < 10 || state.endgameApplied) return;
        if (world.getTime() % HEARTBEAT_INTERVAL_TICKS != 0) return;

        double intensity = intensity(state);
        double chance = 0.05 + intensity * 0.35; // climbs from ~5% to ~40% per check near Day 20
        if (RANDOM.nextDouble() > chance) return;

        float volume = (float) (0.4 + intensity * 0.6);
        float pitch = (float) (0.9 - intensity * 0.3); // deepens as it escalates

        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), ModSounds.CORRUPTION_DRONE,
                    SoundCategory.AMBIENT, volume, pitch);
        }
    }
}
