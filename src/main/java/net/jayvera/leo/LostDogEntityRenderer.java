package net.jayvera.leo;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.Identifier;

/**
 * Reuses the vanilla wolf model/animation but always shows the corrupted
 * texture, regardless of the entity's tamed/angry/collar state. Registered
 * specifically for ModEntities.LOST_DOG, so every entity passed in here is
 * always a LostDogEntity at runtime even though the override signature has
 * to match the superclass's WolfEntity parameter type.
 */
public class LostDogEntityRenderer extends WolfEntityRenderer {
    private static final Identifier LOST_DOG_TEXTURE =
            new Identifier(LeoMod.MOD_ID, "textures/entity/lost_dog/lost_dog.png");

    public LostDogEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(WolfEntity entity) {
        return LOST_DOG_TEXTURE;
    }
}
