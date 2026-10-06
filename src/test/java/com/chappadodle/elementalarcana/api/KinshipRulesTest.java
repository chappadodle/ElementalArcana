package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KinshipRulesTest {

    @Test
    void aTonicGivesAFifth() {
        assertEquals(1.2f, KinshipRules.powerFactor(0), 1e-6);
        assertEquals(0.8f, KinshipRules.wardFactor(0), 1e-6);
    }

    @Test
    void aStrongTonicMore() {
        assertEquals(1.35f, KinshipRules.powerFactor(1), 1e-6);
        assertEquals(0.65f, KinshipRules.wardFactor(1), 1e-6);
    }

    @Test
    void neverPastTheCap() {
        assertEquals(1 + KinshipRules.MAX_SHARE, KinshipRules.powerFactor(255), 1e-6);
        assertEquals(1 - KinshipRules.MAX_SHARE, KinshipRules.wardFactor(255), 1e-6);
        assertEquals(KinshipRules.SHARE, KinshipRules.share(-3), 1e-6);
    }
}
