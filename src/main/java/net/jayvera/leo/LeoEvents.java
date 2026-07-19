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
 * Day 0     immediate stinger - the very first thing that happens
 * Day 1-3   normal (mostly) - only watching for a wolf named Leo
 * Day 4     first sign - distant bark, no source
 * Day 7     the stranger - phantom "Leo?" wolf, vanishes unobserved
 * Day 10-14 the corruption - signs, chest swaps, animals vanishing
 * Day 15+   the real Leo - warning bark, shadow detection, protection mode
 * Day 17    buried collar - a quiet lore beat
 * Day 18-19 escalating dread before the end
 * Day 20    the last howl - Lost Dog, player chooses destroy or help
 * post-end  guardian mode, treasure hunt, night patrol, and an extended
 *           epilogue running through Day 30
 */
public class LeoEvents {
    private static final Random RANDOM = new Random();

    public static void onWorldTick(ServerWorld world) {
        if (world.getPlayers().isEmpty()) return;

        LeoState state = LeoState.get(world);

        long minecraftDay = world.getTimeOfDay() / 24000L;
        if (minecraftDay > state.day) {
            state.day = (int) minecraftDay;
            state.markDirty();
        }

        if (!state.hasFired(0)) {
            fireImmediateStinger(world, state);
        }

        if (!state.leoFound && state.day >= 3) {
            checkOrSpawnLeo(world, state);
        }

        NightAtmosphere.tick(world, state);
        Dread.heartbeatTick(world, state);
        LeoBondTracker.tick(world, state);
        LeoSymptoms.tick(world, state);

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

        if (state.day == 17 && !state.hasFired(17)) {
            BuriedCollarEvent.trigger(world, state);
        }

        if (state.day == 18 && !state.hasFired(18)) {
            broadcast(world, "The nights feel longer now.", Formatting.DARK_GRAY);
            state.fire(18);
        }
        if (state.day == 19 && !state.hasFired(19)) {
            broadcast(world, "Something is close. Leo won't stop watching the tree line.", Formatting.DARK_RED);
            state.fire(19);
        }

        if (state.day == 20 && !state.hasFired(20)) {
            FinalEvent.trigger(world, state);
        }

        if (state.endgameApplied) {
            EndgameFeatures.tick(world, state);
        }
        Epilogue.tick(world, state);
    }

    private static void fireImmediateStinger(ServerWorld world, LeoState state) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), ModSounds.CORRUPTION_DRONE,
                    SoundCategory.AMBIENT, 0.6f, 0.5f);
            player.sendMessage(
                    Text.literal("Somewhere, something is looking for its person.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                    false
            );
        }
        state.fire(0);
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
        leo.setOwnerUuid(target.getUuid()); // real owner - activates vanilla follow/sit/teleport-back AI
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
