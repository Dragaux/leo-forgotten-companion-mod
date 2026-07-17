package net.jayvera.leo;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Day 15 - the real Leo starts acting differently. Since these are behavior
 * changes on a vanilla wolf rather than a fully custom entity, they're driven
 * from the tick loop instead of custom AI goals.
 *
 * Multiplayer note: searches a bounded radius around each online player rather
 * than the whole loaded world, so it scales reasonably with more players.
 */
public class LeoAbilities {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 20; // once per second
    private static final double SEARCH_RADIUS = 48.0;
    private static final double DETECT_RADIUS = 12.0;
    private static final float PROTECT_HEALTH_THRESHOLD = 6.0f; // 3 hearts
    private static final double IDLE_GROWL_CHANCE = 0.01;
    private static final double MEMORY_GLANCE_CHANCE = 0.02;

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;

        Set<WolfEntity> leos = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            leos.addAll(world.getEntitiesByClass(WolfEntity.class,
                    player.getBoundingBox().expand(SEARCH_RADIUS),
                    w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString())));
        }

        for (WolfEntity leo : leos) {
            warningBark(world, leo);
            shadowDetection(world, leo);
            protectionMode(world, leo);
            idleUnease(world, leo);
            memoryGlance(world, leo, state);
        }
    }

    private static void warningBark(ServerWorld world, WolfEntity leo) {
        List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class,
                leo.getBoundingBox().expand(DETECT_RADIUS), h -> true);
        if (!hostiles.isEmpty()) {
            world.playSound(null, leo.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL,
                    SoundCategory.NEUTRAL, 0.8f, 1.0f);
        }
    }

    private static void shadowDetection(ServerWorld world, WolfEntity leo) {
        List<net.jayvera.leo.entity.LostDogEntity> lostDogs = world.getEntitiesByClass(
                net.jayvera.leo.entity.LostDogEntity.class,
                leo.getBoundingBox().expand(DETECT_RADIUS), d -> true);
        if (!lostDogs.isEmpty()) {
            leo.getLookControl().lookAt(lostDogs.get(0), 30.0f, 30.0f);
            world.spawnParticles(ParticleTypes.SMOKE,
                    leo.getX(), leo.getY() + 0.5, leo.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static void protectionMode(ServerWorld world, WolfEntity leo) {
        if (!(leo.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner)) return;
        if (owner.getHealth() <= PROTECT_HEALTH_THRESHOLD) {
            leo.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1));
            leo.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));
        }
    }

    /** Leo occasionally growls at nothing visible - unease, not a real threat detected. */
    private static void idleUnease(ServerWorld world, WolfEntity leo) {
        if (RANDOM.nextDouble() < IDLE_GROWL_CHANCE) {
            world.playSound(null, leo.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL,
                    SoundCategory.NEUTRAL, 0.5f, 1.3f);
        }
    }

    /** Leo occasionally looks toward wherever the most recent corruption event happened. */
    private static void memoryGlance(ServerWorld world, WolfEntity leo, LeoState state) {
        if (!state.hasLastCorruptionPos) return;
        if (RANDOM.nextDouble() > MEMORY_GLANCE_CHANCE) return;

        leo.getLookControl().lookAt(
                state.lastCorruptionX + 0.5,
                state.lastCorruptionY + 0.5,
                state.lastCorruptionZ + 0.5
        );
    }
}
