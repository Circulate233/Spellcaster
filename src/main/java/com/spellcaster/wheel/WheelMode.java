package com.spellcaster.wheel;

public enum WheelMode {

    FATE_NORMAL,
    FATE_STRICT,
    CHAOS;

    public WheelMode nextClientMode() {
        return switch (this) {
            case FATE_NORMAL -> FATE_STRICT;
            case FATE_STRICT -> CHAOS;
            default -> FATE_NORMAL;
        };
    }

    public boolean isStrict() {
        return this == FATE_STRICT;
    }

    public boolean isChaos() {
        return this == CHAOS;
    }
}
