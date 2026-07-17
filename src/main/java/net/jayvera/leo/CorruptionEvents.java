package net.jayvera.leo;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Day 10 - "The Corruption Begins". Small, easy-to-miss world changes:
 * a sign here, a swapped chest item there, an animal that just isn't there anymore.
 * Runs on a low random chance each tick between day 10 and day 15 so it never
 * feels scripted or predictable.
 */
public class CorruptionEvents {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 200; // every 10 seconds, low odds each time
    private static final double EVENT_CHANCE = 0.05;

    private static final int AMBIENT_INTERVAL_TICKS = 1200; // roughly once a minute

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % AMBIENT_INTERVAL_TICKS == 0 && RANDOM.nextDouble() < 0.4) {
            playAmbientDrone(world);
        }

        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (RANDOM.nextDouble() > EVENT_CHANCE) return;

        List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;
        PlayerEntity player = players.get(RANDOM.nextInt(players.size()));

        switch (RANDOM.nextInt(3)) {
            case 0 -> placeSign(world, player);
            case 1 -> swapChestItem(world, player);
            default -> vanishAnimal(world, player);
        }
    }

    private static void playAmbientDrone(ServerWorld world) {
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), net.jayvera.leo.ModSounds.CORRUPTION_DRONE,
                    net.minecraft.sound.SoundCategory.AMBIENT, 0.35f, 1.0f);
        }
    }

    private static void placeSign(ServerWorld world, PlayerEntity player) {
        BlockPos base = player.getBlockPos();
        BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                base.add(RANDOM.nextInt(17) - 8, 0, RANDOM.nextInt(17) - 8));

        if (!world.getBlockState(pos).isAir()) return;

        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState());
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof SignBlockEntity sign) {
            sign.changeText(text -> text.withMessage(1, Text.literal("He remembers you.")), true);
            sign.markDirty();
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private static void swapChestItem(ServerWorld world, PlayerEntity player) {
        BlockPos base = player.getBlockPos();
        Box searchBox = new Box(base).expand(16);

        for (BlockPos pos : BlockPos.iterate(
                BlockPos.ofFloored(searchBox.minX, searchBox.minY, searchBox.minZ),
                BlockPos.ofFloored(searchBox.maxX, searchBox.maxY, searchBox.maxZ))) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity chest) {
                ItemStack bone = new ItemStack(Items.BONE);
                bone.setCustomName(Text.literal("Leo's Favorite").formatted(Formatting.GRAY));
                chest.setStack(RANDOM.nextInt(chest.size()), bone);
                chest.markDirty();
                return;
            }
        }
    }

    private static void vanishAnimal(ServerWorld world, PlayerEntity player) {
        Box searchBox = player.getBoundingBox().expand(20);
        List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class, searchBox,
                a -> !(a.hasCustomName() && "Leo".equals(a.getCustomName().getString())));

        if (animals.isEmpty()) return;
        AnimalEntity victim = animals.get(RANDOM.nextInt(animals.size()));
        BlockPos pos = victim.getBlockPos();
        victim.discard();

        for (int i = 0; i < 12; i++) {
            world.spawnParticles(ParticleTypes.ASH,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    1, 0.3, 0.05, 0.3, 0.0);
        }
        player.sendMessage(
                Text.literal("Paw prints lead away from where it stood.")
                        .formatted(Formatting.GRAY, Formatting.ITALIC),
                false
        );
    }
}
