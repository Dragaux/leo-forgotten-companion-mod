package net.jayvera.leo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.jayvera.leo.entity.ModEntities;

/** Client-only: renders the Lost Dog with the vanilla wolf model but a custom corrupted texture. */
public class LeoModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.LOST_DOG, LostDogEntityRenderer::new);
    }
}
