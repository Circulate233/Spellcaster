package com.spellcaster.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SkillSequenceSelector {

    private SkillSequenceSelector() {}

    public static SkillSelection select(int[] sequence, int cursor, WheelMode mode, SkillAvailability availability,
        Random random) {
        if (sequence == null || sequence.length == 0 || availability == null || mode == null) {
            return SkillSelection.none(0);
        }

        int start = normalizeCursor(cursor, sequence.length);
        if (mode.isStrict()) {
            int skillId = sequence[start];
            if (skillId < 0 || !availability.isAvailable(skillId)) {
                return SkillSelection.none(start);
            }

            return SkillSelection.of(start, skillId, (start + 1) % sequence.length);
        }

        if (mode.isChaos()) {
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < sequence.length; ++i) {
                if (sequence[i] >= 0 && availability.isAvailable(sequence[i])) {
                    available.add(i);
                }
            }

            if (available.isEmpty()) {
                return SkillSelection.none(start);
            }

            Random source = random == null ? new Random() : random;
            int index = available.get(source.nextInt(available.size()));
            return SkillSelection.of(index, sequence[index], (index + 1) % sequence.length);
        }

        for (int offset = 0; offset < sequence.length; ++offset) {
            int index = (start + offset) % sequence.length;
            if (sequence[index] >= 0 && availability.isAvailable(sequence[index])) {
                return SkillSelection.of(index, sequence[index], (index + 1) % sequence.length);
            }
        }

        return SkillSelection.none(start);
    }

    private static int normalizeCursor(int cursor, int size) {
        int normalized = cursor % size;
        return normalized < 0 ? normalized + size : normalized;
    }
}
