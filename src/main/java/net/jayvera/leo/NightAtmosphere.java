package net.jayvera.leo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Ambient horror touches across the whole timeline:
 * - Day 1-3: an almost-imperceptible sound, extremely rare, no message at all -
 *   "nothing changes" should still technically be true, this is just a seed of doubt.
 * - Day 7+: fake footsteps behind a lone player at night.
 * - Day 15+: a rare jump-scare glimpse of the Lost Dog.
 */
public class NightAtmosphere {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 100; // every 5 seconds
    private static final double ALONE_RADIUS = 30.0;
    private static final double FOOTSTEP_CHANCE = 0.05;
    private static final double GLIMPSE_CHANCE = 0.006;
    private static final double EARLY_UNEASE_CHANCE = 0.004;

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (!world.isNight()) return;
        if (state.endgameApplied) return;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (state.day < 3) {
                if (RANDOM.nextDouble() < EARLY_UNEASE_CHANCE) {
                    playEarlyUnease(world, player);
                }
                continue;
            }

            if (state.day < 7) continue;
            if (!isAlone(world, player)) continue;

            if (RANDOM.nextDouble() < FOOTSTEP_CHANCE) {
                playFakeFootsteps(world, player);
            }

            if (state.day >= 15 && !state.finalEventActive && RANDOM.nextDouble() < GLIMPSE_CHANCE) {
                JumpScare.glimpse(world, player);
            }
        }
    }

    private static boolean isAlone(ServerWorld world, ServerPlayerEntity player) {
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other == player) continue;
            if (other.getPos().distanceTo(player.getPos()) < ALONE_RADIUS) return false;
        }
        return true;
    }

    private static void playFakeFootsteps(ServerWorld world, ServerPlayerEntity player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle) * (3 + RANDOM.nextDouble() * 2);
        double dz = Math.sin(angle) * (3 + RANDOM.nextDouble() * 2);
        BlockPos pos = player.getBlockPos().add((int) dx, 0, (int) dz);
        world.playSound(null, pos, SoundEvents.BLOCK_GRASS_STEP, SoundCategory.AMBIENT, 0.6f, 0.8f);
    }

    private static void playEarlyUnease(ServerWorld world, ServerPlayerEntity player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = 8 + RANDOM.nextDouble() * 6;
        BlockPos pos = player.getBlockPos().add(
                (int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
        // Deliberately quiet/low - meant to be almost missed, no chat message at all.
        world.playSound(null, pos, ModSounds.CORRUPTION_DRONE, SoundCategory.AMBIENT, 0.08f, 1.3f);
    }
}
