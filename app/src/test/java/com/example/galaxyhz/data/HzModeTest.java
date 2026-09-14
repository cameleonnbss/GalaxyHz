package com.example.galaxyhz.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Unit tests for the HzMode mapping (pure JVM, no Android framework). */
public class HzModeTest {

    @Test
    public void fromHz_mapsSupportedRates() {
        assertEquals(HzMode.H120, HzMode.fromHz(120));
        assertEquals(HzMode.H96, HzMode.fromHz(96));
        assertEquals(HzMode.H60, HzMode.fromHz(60));
    }

    @Test
    public void fromHz_defaultsTo60_forUnknownRates() {
        assertEquals(HzMode.H60, HzMode.fromHz(30));
        assertEquals(HzMode.H60, HzMode.fromHz(0));
        assertEquals(HzMode.H60, HzMode.fromHz(144));
    }

    @Test
    public void modes_exposeCorrectHzValues() {
        assertEquals(120, HzMode.H120.getHz());
        assertEquals(96, HzMode.H96.getHz());
        assertEquals(60, HzMode.H60.getHz());
    }
}
