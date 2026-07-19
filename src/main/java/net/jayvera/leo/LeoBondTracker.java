package net.jayvera.leo;

import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Tracks how much time Leo has actually spent near his owner - a flavor payoff, not a gate on other events. */
public class LeoBondTracker {
    private static final int CHECK_INTERVAL_TICKS = 20; // once per second
    private static final double NEAR_RADIUS = 16.0;
    private static final int TRUST_MILESTONE_TICKS = 6000; // ~5 minutes of real proximity

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayerEntity player : world.getPlayers()) {
            List<WolfEntity> nearbyLeos = world.getEntitiesByClass(WolfEntity.class,
                    player.getBoundingBox().expand(NEAR_RADIUS),
                    w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString()));

            if (!nearbyLeos.isEmpty()) {
                boolean wasBelowMilestone = state.leoBondTicks < TRUST_MILESTONE_TICKS;
                state.leoBondTicks += CHECK_INTERVAL_TICKS;
                if (wasBelowMilestone && state.leoBondTicks >= TRUST_MILESTONE_TICKS) {
                    player.sendMessage(
                            Text.literal("Leo trusts you now.").formatted(Formatting.GRAY, Formatting.ITALIC),
                            false
                    );
                }
                state.markDirty();
            }
        }
    }
}
