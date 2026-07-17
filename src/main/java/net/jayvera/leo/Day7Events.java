package net.jayvera.leo;

import net.jayvera.leo.entity.LostDogEntity;
import net.jayvera.leo.entity.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Day 7 - "The Stranger". A wolf that looks exactly like Leo shows up,
 * doesn't take damage, doesn't eat, and vanishes the moment nobody is
 * looking directly at it.
 */
public class Day7Events {
    private static final Random RANDOM = new Random();
    private static final int UNSEEN_TICKS_TO_DESPAWN = 40; // 2 seconds unobserved

    public static void trigger(ServerWorld world, LeoState state) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;
        ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));

        BlockPos base = target.getBlockPos();
        BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                base.add(RANDOM.nextInt(13) - 6, 0, RANDOM.nextInt(13) - 6));

        LostDogEntity stranger = ModEntities.LOST_DOG.create(world);
        if (stranger == null) return;

        stranger.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        stranger.setCustomName(Text.literal("Leo?").formatted(Formatting.DARK_GRAY));
        stranger.setCustomNameVisible(true);
        world.spawnEntity(stranger);

        state.strangerWolfId = stranger.getUuid();
        state.strangerUnseenTicks = 0;
        state.fire(7);

        target.sendMessage(
                Text.literal("A wolf that looks exactly like Leo is watching you.")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                false
        );
    }

    /** Called every tick while a stranger wolf is alive - despawns it if no player is looking at it. */
    public static void tickDespawnCheck(ServerWorld world, LeoState state) {
        if (state.strangerWolfId == null) return;

        Entity entity = world.getEntity(state.strangerWolfId);
        if (entity == null) {
            state.strangerWolfId = null;
            state.markDirty();
            return;
        }

        boolean seen = false;
        for (PlayerEntity player : world.getPlayers()) {
            if (isLookingAt(player, entity)) {
                seen = true;
                break;
            }
        }

        if (seen) {
            state.strangerUnseenTicks = 0;
        } else {
            state.strangerUnseenTicks++;
            if (state.strangerUnseenTicks > UNSEEN_TICKS_TO_DESPAWN) {
                entity.discard();
                state.strangerWolfId = null;
                state.strangerUnseenTicks = 0;
                state.markDirty();
            }
        }
    }

    private static boolean isLookingAt(PlayerEntity player, Entity target) {
        double distance = player.getPos().distanceTo(target.getPos());
        if (distance > 32) return false;

        Vec3d toTarget = target.getPos().subtract(player.getEyePos()).normalize();
        Vec3d look = player.getRotationVec(1.0f);
        return look.dotProduct(toTarget) > 0.9; // roughly a 25-degree cone
    }
}
