package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigRatesTest {

    @Test
    void ratesScaleChances() {
        assertEquals(0.25, ConfigRates.scaled(0.25, 1.0), 1e-9);
        assertEquals(0.5, ConfigRates.scaled(0.25, 2.0), 1e-9);
        assertEquals(0.0, ConfigRates.scaled(0.25, 0.0), 1e-9);
    }

    @Test
    void chancesStayChances() {
        assertEquals(1.0, ConfigRates.scaled(1.0 / 3.0, 3.0), 1e-9);
        assertEquals(1.0, ConfigRates.scaled(0.5, 10.0), 1e-9);
        assertEquals(0.0, ConfigRates.scaled(0.5, -1.0), 1e-9);
    }
}
