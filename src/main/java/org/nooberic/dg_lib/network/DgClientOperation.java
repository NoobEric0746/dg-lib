package org.nooberic.dg_lib.network;

public enum DgClientOperation {
    QUERY_STATUS(0),
    SET_STRENGTH(1),
    INCREASE_STRENGTH(2),
    DECREASE_STRENGTH(3),
    PLAY_BASIC_WAVE(4),
    PLAY_PULSE_BY_ID(5),
    CONTROL(6);

    private final int id;

    DgClientOperation(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static DgClientOperation fromId(int id) {
        for (DgClientOperation op : values()) {
            if (op.id == id) {
                return op;
            }
        }
        return QUERY_STATUS;
    }
}
