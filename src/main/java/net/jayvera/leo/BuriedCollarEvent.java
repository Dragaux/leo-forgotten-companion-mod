package net.jayvera.leo;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/** Day 17 - Leo digs up something that was never his. A quiet, one-shot lore beat. */
public class BuriedCollarEvent {
    private static final Random RANDOM = new Random();

    public static void trigger(ServerWorld world, LeoState state) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;
        ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));

        BlockPos base = target.getBlockPos();
        BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                base.add(RANDOM.nextInt(9) - 4, 0, RANDOM.nextInt(9) - 4));

        ItemStack collar = new ItemStack(Items.LEAD);
        collar.setCustomName(Text.literal("Old Collar").formatted(Formatting.DARK_GRAY));
        ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, collar);
        world.spawnEntity(drop);

        target.sendMessage(
                Text.literal("Leo is digging.").formatted(Formatting.GRAY, Formatting.ITALIC), false);
        target.sendMessage(
                Text.literal("He unearths a collar. It isn't his.").formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                false);

        state.fire(17);
    }
}
