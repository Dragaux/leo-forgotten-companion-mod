package net.jayvera.leo;

import net.jayvera.leo.entity.LostDogEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Master dispatcher. Runs every world tick and drives the full timeline:
 *
 * Day 1-3   normal - only watching for a wolf named Leo
 * Day 4     first sign - distant bark, no source
 * Day 7     the stranger - phantom "Leo?" wolf, vanishes unobserved
 * Day 10-14 the corruption - signs, chest swaps, animals vanishing
 * Day 15+   the real Leo - warning bark, shadow detection, protection mode
 * Day 20    the last howl - Lost Dog, player chooses destroy or help
 * post-end  guardian mode, treasure hunt, night patrol
 */
public class LeoEvents {
    private static final Random RANDOM = new Random();
    private static final long TICKS_PER_DAY = 24000L;

    public static void onWorldTick(ServerWorld world) {
        if (world.getPlayers().isEmpty()) return;

        LeoState state = LeoState.get(world);
        state.tick++;

        if (state.tick % TICKS_PER_DAY == 0) {
            state.day++;
            state.markDirty();
        }

        if (!state.leoFound && state.day >= 3) {
            checkOrSpawnLeo(world, state);
        }

        if (state.day == 4 && !state.hasFired(4) && world.isNight()) {
            fireDay4Event(world, state);
        }

        if (state.day == 7 && !state.hasFired(7)) {
            Day7Events.trigger(world, state);
        }
        if (state.strangerWolfId != null) {
            Day7Events.tickDespawnCheck(world, state);
        }

        if (state.day == 10 && !state.hasFired(10)) {
            state.corruptionActive = true;
            state.fire(10);
            broadcast(world, "Something about this world feels different.", Formatting.DARK_GRAY);
        }
        if (state.corruptionActive && state.day < 15) {
            CorruptionEvents.tick(world, state);
        }

        if (state.day == 15 && !state.hasFired(15)) {
            state.corruptionActive = false;
            state.fire(15);
            broadcast(world, "Leo doesn't leave your side anymore.", Formatting.GRAY);
        }
        if (state.day >= 15 && !state.finalEventActive) {
            LeoAbilities.tick(world, state);
        }

        if (state.day == 20 && !state.hasFired(20)) {
            FinalEvent.trigger(world, state);
        }

        if (state.endgameApplied) {
            EndgameFeatures.tick(world, state);
        }
    }

    private static void checkOrSpawnLeo(ServerWorld world, LeoState state) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = player.getBoundingBox().expand(64);
            List<WolfEntity> found = world.getEntitiesByClass(WolfEntity.class, searchBox,
                    w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString()));
            if (!found.isEmpty()) {
                state.leoFound = true;
                state.markDirty();
                return;
            }
        }

        List<ServerPlayerEntity> players = world.getPlayers();
        ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));
        BlockPos base = target.getBlockPos();

        BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                base.add(RANDOM.nextInt(21) - 10, 0, RANDOM.nextInt(21) - 10));

        WolfEntity leo = EntityType.WOLF.create(world);
        if (leo == null) return;

        leo.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        leo.setCustomName(Text.literal("Leo"));
        leo.setCustomNameVisible(true);
        leo.setTamed(true);
        world.spawnEntity(leo);

        state.leoFound = true;
        state.markDirty();

        target.sendMessage(
                Text.literal("A wolf approaches. It already has a collar.")
                        .formatted(Formatting.GRAY, Formatting.ITALIC),
                false
        );
    }

    private static void fireDay4Event(ServerWorld world, LeoState state) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), ModSounds.LEO_DISTANT_BARK,
                    SoundCategory.NEUTRAL, 0.6f, 1.0f);
            player.sendMessage(
                    Text.literal("Leo is looking for something.")
                            .formatted(Formatting.GRAY, Formatting.ITALIC),
                    false
            );
        }
        state.fire(4);
    }

    // --- callbacks from LostDogEntity ---

    public static void onLostDogDestroyed(ServerWorld world, LostDogEntity dog) {
        LeoState state = LeoState.get(world);
        if (!state.finalEventActive) return;
        FinalEvent.onDestroyed(world, state);
    }

    public static void onLostDogHelped(ServerWorld world, LostDogEntity dog) {
        LeoState state = LeoState.get(world);
        if (!state.finalEventActive) return;
        FinalEvent.onHelped(world, state, dog);
    }

    private static void broadcast(ServerWorld world, String message, Formatting color) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.literal(message).formatted(color, Formatting.ITALIC), false);
        }
    }
}
