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
        String note = state.corruptionEventCount >= 15 ? " Leo endured more than most companions ever have." : "";
        broadcast(world, "The howling stops. Leo feels stronger." + note, Formatting.GRAY);
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
            // Owner assigned to whichever player is nearest, so it gets real
