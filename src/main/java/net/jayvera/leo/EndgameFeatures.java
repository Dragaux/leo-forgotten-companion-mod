package net.jayvera.leo;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Items;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Guardian mode, treasure hunts, and night patrol - runs once the ending has resolved.
 * Searches a bounded radius per online player rather than the whole loaded world,
 * so it scales reasonably with more players instead of one giant scan.
 */
public class EndgameFeatures {
    private static final Random RANDOM = new Random();
    private static final int TICK_INTERVAL = 100; // 5 seconds
    private static final double SEARCH_RADIUS = 48.0;

    public static void tick(ServerWorld world, LeoState state) {
        if (!state.endgameApplied) return;
        if (world.getTime() % TICK_INTERVAL != 0) return;

        Set<WolfEntity> leos = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            leos.addAll(world.getEntitiesByClass(WolfEntity.class,
                    player.getBoundingBox().expand(SEARCH_RADIUS),
                    w -> w.hasCustomName() && "Leo".equals(w.getCustomName().getString())));
        }

        for (WolfEntity leo : leos) {
            guardianMode(world, leo);
            treasureHunt(world, leo);
        }
    }

    private static void guardianMode(ServerWorld world, WolfEntity leo) {
        List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class,
                leo.getBoundingBox().expand(10), h -> true);
        if (!hostiles.isEmpty()) {
            world.playSound(null, leo.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL,
                    SoundCategory.NEUTRAL, 1.0f, 0.8f);
        }
    }

    private static void treasureHunt(ServerWorld world, WolfEntity leo) {
        if (RANDOM.nextDouble() > 0.02) return; // rare, so it feels like a discovery

        ItemStack loot = new ItemStack(Items.BONE, 1);
        ItemEntity drop = new ItemEntity(world, leo.getX(), leo.getY() + 0.3, leo.getZ(), loot);
        world.spawnEntity(drop);

        if (leo.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner) {
            owner.sendMessage(
                    Text.literal("Leo found something.").formatted(Formatting.GRAY, Formatting.ITALIC),
                    true
            );
        }
    }
}
