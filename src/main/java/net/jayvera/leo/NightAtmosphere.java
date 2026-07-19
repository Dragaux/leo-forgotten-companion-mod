package net.jayvera.leo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Ambient horror touches across the whole timeline. Everything here scales
 * with Dread.intensity(state), so it's quiet and rare early on and gets
 * louder/more frequent the closer Day 20 gets:
 * - Day 1-3: a low audible drone, occasionally with a visible "..." cue.
 * - Day 7+: fake footsteps behind a lone player at night, escalating.
 * - Day 15+: a jump-scare glimpse of the Lost Dog, escalating - and once
 *   intensity is high enough, it can happen in daylight too.
 */
public class NightAtmosphere {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 100; // every 5 seconds
    private static final double ALONE_RADIUS = 30.0;

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (state.endgameApplied) return;

        double intensity = Dread.intensity(state);

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (state.day < 3) {
                if (world.isNight() && RANDOM.nextDouble() < 0.03) {
                    playEarlyUnease(world, player);
                }
                continue;
            }

            if (state.day < 7) continue;

            boolean nightMultiplier = world.isNight();
            double footstepChance = (0.05 + intensity * 0.15) * (nightMultiplier ? 1.0 : 0.2);
            if (isAlone(world, player) && RANDOM.nextDouble() < footstepChance) {
                playFakeFootsteps(world, player, intensity);
            }

            if (state.day >= 15 && !state.finalEventActive) {
                double glimpseChance = 0.006 + intensity * 0.03;
                boolean glimpseWindow = world.isNight() || intensity > 0.85; // near the end, even daylight isn't safe
                if (glimpseWindow && RANDOM.nextDouble() < glimpseChance) {
                    JumpScare.glimpse(world, player);
                }
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

    private static void playFakeFootsteps(ServerWorld world, ServerPlayerEntity player, double intensity) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle) * (3 + RANDOM.nextDouble() * 2);
        double dz = Math.sin(angle) * (3 + RANDOM.nextDouble() * 2);
        BlockPos pos = player.getBlockPos().add((int) dx, 0, (int) dz);
        float volume = (float) (0.5 + intensity * 0.5);
        world.playSound(null, pos, SoundEvents.BLOCK_GRASS_STEP, SoundCategory.AMBIENT, volume, 0.8f);
    }

    private static void playEarlyUnease(ServerWorld world, ServerPlayerEntity player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = 6 + RANDOM.nextDouble() * 5;
        BlockPos pos = player.getBlockPos().add(
                (int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
        world.playSound(null, pos, ModSounds.CORRUPTION_DRONE, SoundCategory.AMBIENT, 0.55f, 0.7f);

        if (RANDOM.nextDouble() < 0.35) {
            player.sendMessage(Text.literal("...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC), true);
        }
    }
}
