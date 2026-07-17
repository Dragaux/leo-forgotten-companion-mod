package net.jayvera.leo.entity;

import net.fabricmc.fabric.api.entity.event.v1.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.jayvera.leo.LeoMod;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<LostDogEntity> LOST_DOG = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(LeoMod.MOD_ID, "lost_dog"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, LostDogEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.85f))
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(LOST_DOG, WolfEntity.createWolfAttributes());
    }
}
