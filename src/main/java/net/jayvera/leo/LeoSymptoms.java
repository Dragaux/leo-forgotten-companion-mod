package net.jayvera.leo;

import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Leo isn't untouched by any of this. From Day 10 onward he shows small
 * signs of it himself - and from Day 15 he sometimes actively walks toward
 * wherever the corruption last touched the world, and has nights that look
 * like nightmares.
 */
public class LeoSymptoms {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 40; // every 2 seconds
    private static final double SEARCH_RADIUS = 48.0;

    public static void tick(ServerWorld world, LeoState state) {
        if (state.day < 10 || state.finalEventActive) return;
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;

        double intensity = Dread.intensity(state);

        Set<WolfEntity> leos = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            leos.addAll(world.getEntitiesByClass(WolfEntity.class,
                    player.getBoundingBox().expand(SEARCH_RADIUS),
                    w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString())));
        }

        for (WolfEntity leo : leos) {
            eyeGlow(world, leo, intensity);

            if (state.day >= 15) {
                nightTerror(world, leo);
                leadTowardMemory(world, leo, state);
            }
        }
    }

    private static void eyeGlow(ServerWorld world, WolfEntity leo, double intensity) {
        double chance = 0.02 + intensity * 0.05;
        if (RANDOM.nextDouble() > chance) return;

        world.spawnParticles(ParticleTypes.SOUL,
                leo.getX(), leo.getY() + leo.getHeight() * 0.8, leo.getZ(),
                3, 0.05, 0.05, 0.05, 0.0);
    }

    /** Leo whimpers and stirs at night as if something's wrong - reuses the growl sound at a different pitch. */
    private static void nightTerror(ServerWorld world, WolfEntity leo) {
        if (!world.isNight()) return;
        if (RANDOM.nextDouble() > 0.01) return;

        world.playSound(null, leo.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL,
                SoundCategory.NEUTRAL, 0.5f, 1.5f);
        world.spawnParticles(ParticleTypes.SMOKE,
                leo.getX(), leo.getY() + 0.6, leo.getZ(), 4, 0.15, 0.1, 0.15, 0.01);
    }

    private static void leadTowardMemory(ServerWorld world, WolfEntity leo, LeoState state) {
        if (!state.hasLastCorruptionPos) return;
        if (RANDOM.nextDouble() > 0.015) return;

        leo.getNavigation().startMovingTo(
                state.lastCorruptionX + 0.5,
                state.lastCorruptionY,
                state.lastCorruptionZ + 0.5,
                0.6);
    }
}
