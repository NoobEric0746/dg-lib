package org.nooberic.dg_lib.service;

@FunctionalInterface
public interface StrengthFeedbackListener {
    void onStrengthFeedback(int channelAStrength, int channelBStrength, int channelALimit, int channelBLimit);
}
