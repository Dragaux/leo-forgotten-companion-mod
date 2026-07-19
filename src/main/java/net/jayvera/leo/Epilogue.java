package net.jayvera.leo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/** The story doesn't fully end at Day 20 - a few things still echo afterward. */
public class Epilogue {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL_TICKS = 1200; // once a minute

    public static void tick(ServerWorld world, LeoState state) {
        if (state.day == 25 && !state.hasFired(25)) {
            secondHowl(world, state);
        }
        if (state.day == 30 && !state.hasFired(30)) {
            trueEnding(world, state);
        }

        if (!state.endgameApplied) return;
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (RANDOM.nextDouble() > 0.05) return;

        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), ModSounds.LEO_DISTANT_BARK,
                    SoundCategory.AMBIENT, 0.25f, 0.6f);
        }
    }

    private static void secondHowl(ServerWorld world, LeoState state) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), ModSounds.LOST_DOG_HOWL,
                    SoundCategory.AMBIENT, 0.5f, 1.2f);
            player.sendMessage(
                    Text.literal("A distant howl answers - fainter this time, further away.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                    false
            );
        }
        state.fire(25);
    }

    private static void trueEnding(ServerWorld world, LeoState state) {
        String choiceNote = state.finalChoice == 2
                ? "Two wolves walk beside you now, and neither remembers being afraid."
                : "One wolf walks beside you now, steadier than he's ever been.";
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(
                    Text.literal("Thirty days. " + state.corruptionEventCount + " small horrors endured. " + choiceNote)
                            .formatted(Formatting.GOLD, Formatting.ITALIC),
                    false
            );
        }
        state.fire(30);
    }
}
