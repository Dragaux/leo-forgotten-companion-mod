package net.jayvera.leo;

import net.jayvera.leo.entity.LostDogEntity;
import net.jayvera.leo.entity.ModEntities;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * A rare, brief glimpse of the Lost Dog right at the edge of a player's view -
 * spawned close in front of them, then discarded a few ticks later, before they
 * have time to react. Invulnerable/AI-disabled the whole time, so it can't hurt
 * or be attacked during the glimpse.
 */
public class JumpScare {
    private static final int GLIMPSE_LIFETIME_TICKS = 4; // a fraction of a second

    public static void glimpse(ServerWorld world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d spawnPos = player.getEyePos().add(look.multiply(3.0));

        LostDogEntity glimpse = ModEntities.LOST_DOG.create(world);
        if (glimpse == null) return;

        glimpse.refreshPositionAndAngles(spawnPos.x, spawnPos.y - 0.5, spawnPos.z, 0, 0);
        glimpse.markForQuickRemoval(GLIMPSE_LIFETIME_TICKS);
        world.spawnEntity(glimpse);

        world.playSound(null, player.getBlockPos(), ModSounds.LEO_DISTANT_BARK,
                SoundCategory.AMBIENT, 0.3f, 0.5f);
    }
}
