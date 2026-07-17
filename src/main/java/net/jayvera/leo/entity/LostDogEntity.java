package net.jayvera.leo.entity;

import net.jayvera.leo.LeoEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The Lost Dog - a corrupted wolf that copies Leo's name and appearance.
 * Invulnerable until the final confrontation begins (see LeoState.finalEventActive),
 * at which point it can be destroyed or helped.
 */
public class LostDogEntity extends WolfEntity {

    public LostDogEntity(EntityType<? extends WolfEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomName(Text.literal("Leo?").formatted(Formatting.DARK_GRAY));
        this.setCustomNameVisible(true);
        this.setTamed(false);
        this.setAiDisabled(false);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean vulnerable = !(this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw)
                || net.jayvera.leo.LeoState.get(sw).finalEventActive;

        if (!vulnerable) {
            return false; // Copy. Follow. Vanish. Can't be hurt outside the final event.
        }

        boolean result = super.damage(source, amount);

        if (result && this.getHealth() <= 0.0f && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw2) {
            LeoEvents.onLostDogDestroyed(sw2, this);
        }
        return result;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw
                && net.jayvera.leo.LeoState.get(sw).finalEventActive
                && player.getStackInHand(hand).isOf(Items.BONE)) {
            LeoEvents.onLostDogHelped(sw, this);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }
}
