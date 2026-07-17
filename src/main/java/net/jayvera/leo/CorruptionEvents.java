package net.jayvera.leo;

import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Day 10 - "The Corruption Begins". Small, easy-to-miss world changes:
 * a sign here, a swapped chest item there, an animal that just isn't there anymore,
 * the world dimming for a moment, an item just not being in your inventory anymore,
 * dead patches spreading in the grass, campfires snuffing out.
 * Runs on a low random chance each tick between day 10 and day 15, and gets
 * slightly more frequent the more events have already fired - it escalates.
 */
public class CorruptionEvents {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 200; // every 10 seconds
    private static final double BASE_EVENT_CHANCE = 0.05;
    private static final double MAX_EVENT_CHANCE = 0.15;
    private static final double ESCALATION_PER_EVENT = 0.003;

    private static final int AMBIENT_INTERVAL_TICKS = 1200; // roughly once a minute

    public static void tick(ServerWorld world, LeoState state) {
        if (world.getTime() % AMBIENT_INTERVAL_TICKS == 0 && RANDOM.nextDouble() < 0.4) {
            playAmbientDrone(world);
        }

        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;

        double chance = Math.min(BASE_EVENT_CHANCE + state.corruptionEventCount * ESCALATION_PER_EVENT, MAX_EVENT_CHANCE);
        if (RANDOM.nextDouble() > chance) return;

        List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;
        PlayerEntity player = players.get(RANDOM.nextInt(players.size()));

        switch (RANDOM.nextInt(7)) {
            case 0 -> placeSign(world, player, state);
            case 1 -> swapChestItem(world, player, state);
            case 2 -> vanishAnimal(world, player, state);
            case 3 -> darknessPulse(world, player, state);
            case 4 -> vanishItem(world, player, state);
            case 5 -> deadPatch(world, player, state);
            default -> extinguishCampfire(world, player, state);
        }
    }

    private static void playAmbientDrone(ServerWorld world) {
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), net.jayvera.leo.ModSounds.CORRUPTION_DRONE,
                    net.minecraft.sound.SoundCategory.AMBIENT, 0.35f, 1.0f);
        }
    }

    private static void placeSign(ServerWorld world, PlayerEntity player, LeoState state) {
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
            state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static void swapChestItem(ServerWorld world, PlayerEntity player, LeoState state) {
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
                state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
                return;
            }
        }
    }

    private static void vanishAnimal(ServerWorld world, PlayerEntity player, LeoState state) {
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
        state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
    }

    private static void darknessPulse(ServerWorld world, PlayerEntity player, LeoState state) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0));
        player.sendMessage(
                Text.literal("The world dims for a moment.")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                false
        );
        BlockPos pos = player.getBlockPos();
        state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
    }

    private static void vanishItem(ServerWorld world, PlayerEntity player, LeoState state) {
        PlayerInventory inventory = player.getInventory();
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) nonEmptySlots.add(i);
        }
        if (nonEmptySlots.isEmpty()) return;

        int slot = nonEmptySlots.get(RANDOM.nextInt(nonEmptySlots.size()));
        ItemStack stack = inventory.getStack(slot);
        String name = stack.getName().getString();
        stack.decrement(1);

        player.sendMessage(
                Text.literal("Something took your " + name + ".")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                false
        );
        BlockPos pos = player.getBlockPos();
        state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
    }

    /** A small dead patch spreads in the grass nearby - grass blocks quietly turn to coarse dirt. */
    private static void deadPatch(ServerWorld world, PlayerEntity player, LeoState state) {
        BlockPos base = player.getBlockPos();
        BlockPos center = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                base.add(RANDOM.nextInt(13) - 6, 0, RANDOM.nextInt(13) - 6)).down();

        if (!world.getBlockState(center).isOf(Blocks.GRASS_BLOCK)) return;

        int changed = 0;
        for (BlockPos pos : BlockPos.iterate(center.add(-1, 0, -1), center.add(1, 0, 1))) {
            if (world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK) && RANDOM.nextDouble() < 0.6) {
                world.setBlockState(pos, Blocks.COARSE_DIRT.getDefaultState());
                changed++;
            }
        }
        if (changed > 0) {
            state.recordCorruptionLocation(center.getX(), center.getY(), center.getZ());
        }
    }

    /** Any lit campfire nearby quietly snuffs out. */
    private static void extinguishCampfire(ServerWorld world, PlayerEntity player, LeoState state) {
        Box searchBox = player.getBoundingBox().expand(16);
        for (BlockPos pos : BlockPos.iterate(
                BlockPos.ofFloored(searchBox.minX, searchBox.minY, searchBox.minZ),
                BlockPos.ofFloored(searchBox.maxX, searchBox.maxY, searchBox.maxZ))) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isOf(Blocks.CAMPFIRE) && blockState.get(CampfireBlock.LIT)) {
                world.setBlockState(pos, blockState.with(CampfireBlock.LIT, false));
                player.sendMessage(
                        Text.literal("The fire goes out, though there's no wind.")
                                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                        false
                );
                state.recordCorruptionLocation(pos.getX(), pos.getY(), pos.getZ());
                return;
            }
        }
    }
}
