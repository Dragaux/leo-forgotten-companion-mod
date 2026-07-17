package net.jayvera.leo;

import net.jayvera.leo.entity.LostDogEntity;
import net.jayvera.leo.entity.ModEntities;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/** Day 20 - "The Last Howl". The Lost Dog returns, this time able to be fought or fed. */
public class FinalEvent {
    private static final Random RANDOM = new Random();

    public static void trigger(ServerWorld world, LeoState state) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;
        ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));

        BlockPos base = target.getBlockPos();
        BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, base.add(3, 0, 0));

        LostDogEntity dog = ModEntities.LOST_DOG.create(world);
        if (dog == null) return;

        dog.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        world.spawnEntity(dog);

        for (ServerPlayerEntity player : players) {
            world.playSound(null, player.getBlockPos(), net.jayvera.leo.ModSounds.LOST_DOG_HOWL,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);
            player.sendMessage(
                    Text.literal("It wants to replace him. Destroy it, or feed it a bone to help it.")
                            .formatted(Formatting.DARK_RED),
                    false
            );
        }

        state.lostDogId = dog.getUuid();
        state.finalEventActive = true;
        state.fire(20);
        state.markDirty();
    }

    public static void onDestroyed(ServerWorld world, LeoState state) {
        state.finalChoice = 1;
        state.finalEventActive = false;
        state.markDirty();
        broadcast(world, "The howling stops. Leo feels stronger.", Formatting.GRAY);
        applyEndgame(world, state);
    }

    public static void onHelped(ServerWorld world, LeoState state, LostDogEntity dog) {
        state.finalChoice = 2;
        state.finalEventActive = false;
        state.markDirty();

        WolfEntity forgotten = net.minecraft.entity.EntityType.WOLF.create(world);
        if (forgotten != null) {
            forgotten.refreshPositionAndAngles(dog.getX(), dog.getY(), dog.getZ(), 0, 0);
            forgotten.setCustomName(Text.literal("The Forgotten").formatted(Formatting.DARK_GRAY));
            forgotten.setCustomNameVisible(true);
            forgotten.setTamed(true);
            world.spawnEntity(forgotten);
        }
        dog.discard();

        broadcast(world, "The Lost Dog isn't lost anymore.", Formatting.GRAY);
        applyEndgame(world, state);
    }

    private static void applyEndgame(ServerWorld world, LeoState state) {
        if (state.endgameApplied) return;
        state.endgameApplied = true;
        state.markDirty();

        List<WolfEntity> leos = world.getEntitiesByClass(WolfEntity.class,
                new net.minecraft.util.math.Box(world.getSpawnPos()).expand(1e6),
                w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString()));
        for (WolfEntity leo : leos) {
            leo.setHealth(leo.getMaxHealth());
        }

        broadcast(world, "Leo settles in. Guardian mode active - he'll watch the base at night.", Formatting.GREEN);
    }

    private static void broadcast(ServerWorld world, String message, Formatting color) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.literal(message).formatted(color, Formatting.ITALIC), false);
        }
    }
}
