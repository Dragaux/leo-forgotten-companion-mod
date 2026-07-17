package net.jayvera.leo;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent LEO_DISTANT_BARK = register("leo.distant_bark");
    public static final SoundEvent LOST_DOG_HOWL = register("leo.lost_dog_howl");
    public static final SoundEvent CORRUPTION_DRONE = register("ambient.corruption_drone");

    private static SoundEvent register(String path) {
        Identifier id = new Identifier(LeoMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        // classloading trigger - no-op, forces the static fields above to init
    }
}
