package net.jayvera.leo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.jayvera.leo.entity.ModEntities;

public class LeoMod implements ModInitializer {
    public static final String MOD_ID = "leo_forgotten_companion";

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModSounds.register();
        ServerTickEvents.END_WORLD_TICK.register(LeoEvents::onWorldTick);
    }
}
