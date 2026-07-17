package net.jayvera.leo;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Day 15 - the real Leo starts acting differently. Since these are behavior
 * changes on a vanilla wolf rather than a fully custom entity, they're driven
 * from the tick loop instead of custom AI goals: simpler to reason about and
 * doesn't require mixing into WolfEntity's goal selector.
 */
public class LeoAbilities {
    private static final int CHECK_INTERVAL_TICKS = 20; // once per second
    private static final double DETECT_RADIUS = 12.0;
    private static final float PROTECT_HEALTH_THRESHOLD = 6.0f; // 3 hearts

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;

        List<WolfEntity> leos = world.getEntitiesByClass(WolfEntity.class,
                new Box(world.getSpawnPos()).expand(1e6),
                w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString()));

        for (WolfEntity leo : leos) {
            warningBark(world, leo);
            shadowDetection(world, leo);
            protectionMode(world, leo);
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
}
