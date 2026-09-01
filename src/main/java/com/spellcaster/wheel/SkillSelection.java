package com.spellcaster.wheel;

import lombok.Getter;

@Getter
public final class SkillSelection {

    private final int entryIndex;
    private final int skillId;
    private final int nextCursor;

    private SkillSelection(int entryIndex, int skillId, int nextCursor) {
        this.entryIndex = entryIndex;
        this.skillId = skillId;
        this.nextCursor = nextCursor;
    }

    public static SkillSelection none(int cursor) {
        return new SkillSelection(-1, -1, cursor);
    }

    public static SkillSelection of(int entryIndex, int skillId, int nextCursor) {
        return new SkillSelection(entryIndex, skillId, nextCursor);
    }

    public boolean isSelected() {
        return this.entryIndex >= 0;
    }

}
