package net.jayvera.leo;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Full progress tracker for the storyline, persisted into the world save.
 * Nothing here needs player setup - it's created automatically on first tick.
 */
public class LeoState extends PersistentState {
    public long tick = 0;
    public int day = 0;

    public boolean leoFound = false;
    public final Set<Integer> firedEvents = new HashSet<>(); // one-shot story beats: 4,7,10,15,17,18,19,20

    // Day 7 - the phantom "Leo?" wolf
    public UUID strangerWolfId = null;
    public int strangerUnseenTicks = 0;

    // Day 10 - corruption phase
    public boolean corruptionActive = false;
    public int corruptionEventCount = 0;
    public boolean hasLastCorruptionPos = false;
    public int lastCorruptionX = 0;
    public int lastCorruptionY = 0;
    public int lastCorruptionZ = 0;

    // Final event
    public boolean finalEventActive = false;
    public UUID lostDogId = null;
    public int finalChoice = 0; // 0 = undecided, 1 = destroyed, 2 = helped
    public boolean endgameApplied = false;

    public static LeoState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(LeoState::fromNbt, LeoState::new, "leo_forgotten_companion");
    }

    public boolean hasFired(int eventId) {
        return firedEvents.contains(eventId);
    }

    public void fire(int eventId) {
        firedEvents.add(eventId);
        markDirty();
    }

    public void recordCorruptionLocation(int x, int y, int z) {
        lastCorruptionX = x;
        lastCorruptionY = y;
        lastCorruptionZ = z;
        hasLastCorruptionPos = true;
        corruptionEventCount++;
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("tick", tick);
        nbt.putInt("day", day);
        nbt.putBoolean("leoFound", leoFound);
        nbt.putIntArray("firedEvents", firedEvents.stream().mapToInt(Integer::intValue).toArray());

        if (strangerWolfId != null) nbt.putUuid("strangerWolfId", strangerWolfId);
        nbt.putInt("strangerUnseenTicks", strangerUnseenTicks);

        nbt.putBoolean("corruptionActive", corruptionActive);
        nbt.putInt("corruptionEventCount", corruptionEventCount);
        nbt.putBoolean("hasLastCorruptionPos", hasLastCorruptionPos);
        nbt.putInt("lastCorruptionX", lastCorruptionX);
        nbt.putInt("lastCorruptionY", lastCorruptionY);
        nbt.putInt("lastCorruptionZ", lastCorruptionZ);

        nbt.putBoolean("finalEventActive", finalEventActive);
        if (lostDogId != null) nbt.putUuid("lostDogId", lostDogId);
        nbt.putInt("finalChoice", finalChoice);
        nbt.putBoolean("endgameApplied", endgameApplied);

        return nbt;
    }

    public static LeoState fromNbt(NbtCompound nbt) {
        LeoState state = new LeoState();
        state.tick = nbt.getLong("tick");
        state.day = nbt.getInt("day");
        state.leoFound = nbt.getBoolean("leoFound");
        for (int id : nbt.getIntArray("firedEvents")) state.firedEvents.add(id);

        if (nbt.containsUuid("strangerWolfId")) state.strangerWolfId = nbt.getUuid("strangerWolfId");
        state.strangerUnseenTicks = nbt.getInt("strangerUnseenTicks");

        state.corruptionActive = nbt.getBoolean("corruptionActive");
        state.corruptionEventCount = nbt.getInt("corruptionEventCount");
        state.hasLastCorruptionPos = nbt.getBoolean("hasLastCorruptionPos");
        state.lastCorruptionX = nbt.getInt("lastCorruptionX");
        state.lastCorruptionY = nbt.getInt("lastCorruptionY");
        state.lastCorruptionZ = nbt.getInt("lastCorruptionZ");

        state.finalEventActive = nbt.getBoolean("finalEventActive");
        if (nbt.containsUuid("lostDogId")) state.lostDogId = nbt.getUuid("lostDogId");
        state.finalChoice = nbt.getInt("finalChoice");
        state.endgameApplied = nbt.getBoolean("endgameApplied");

        return state;
    }
}
