package com.example.galaxyhz.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the panel mode-table parser, using the real dump captured
 * from a Galaxy S20 (SM-G981B, x1s) running Evolution X / Android 16.
 */
public class PanelModeParsingTest {

    private static final String REAL_X1S_DUMP =
        "pdm:0 1440x3200_60NS\r\n" +
        "pdm:1 1080x2400_120HS\r\n" +
        "pdm:2 1080x2400_96HS\r\n" +
        "pdm:3 1080x2400_60NS\r\n" +
        "cpdm:0 1440x3200_60NS\r\n" +
        "cpdm:1 1440x3200_52NS\r\n" +
        "cpdm:2 1440x3200_48NS\r\n" +
        "cpdm:3 1080x2400_120HS\r\n" +
        "cpdm:4 1080x2400_120HS_AID_4_CYCLE\r\n" +
        "cpdm:5 1080x2400_112HS\r\n" +
        "cpdm:6 1080x2400_110HS\r\n" +
        "cpdm:7 1080x2400_112HS\r\n" +
        "cpdm:8 1080x2400_100HS\r\n" +
        "cpdm:9 1080x2400_96HS\r\n" +
        "cpdm:10 1080x2400_70HS\r\n" +
        "cpdm:11 1080x2400_60HS\r\n" +
        "cpdm:12 1080x2400_60NS\r\n";

    @Test
    public void parsesStandardPrimaryModes() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        // The four pdm modes must exist, deduplicated against cpdm twins.
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 120 && m.getScanout().equals("HS")));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 96 && m.getScanout().equals("HS")));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 60 && m.getScanout().equals("NS")));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 60 && m.getWidth() == 1440));
    }

    @Test
    public void deduplicatesPdmOverCpdm() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        long count120 = modes.stream().filter(m -> m.getHz() == 120 && m.getWidth() == 1080).count();
        assertEquals(1, count120);
        // The deduped 120Hz entry must be the primary (pdm) one -> not experimental.
        var m120 = modes.stream().filter(m -> m.getHz() == 120 && m.getWidth() == 1080)
            .findFirst().orElseThrow();
        assertTrue(!m120.getExperimental());
        assertEquals("1", m120.getPanelIndex());
    }

    @Test
    public void keepsHiddenIntermediateModes() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 112));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 110));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 100));
        assertTrue(modes.stream().anyMatch(m -> m.getHz() == 70));
    }

    @Test
    public void hiddenModesAreMarkedExperimental() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        var m112 = modes.stream().filter(m -> m.getHz() == 112).findFirst().orElseThrow();
        assertTrue(m112.getExperimental());
    }

    @Test
    public void ignoresAidVariants() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        // AID cycle variants (e.g. 120HS_AID_4_CYCLE) must not produce a second 120HS entry.
        long count120 = modes.stream().filter(m -> m.getHz() == 120).count();
        assertEquals(1, count120);
    }

    @Test
    public void sortedByHzDescending() {
        var modes = RefreshRateManager.parsePanelTable(REAL_X1S_DUMP);
        var hzList = modes.stream().map(m -> m.getHz()).toList();
        var sorted = hzList.stream().sorted(java.util.Comparator.reverseOrder()).toList();
        assertEquals(sorted, hzList);
    }

    @Test
    public void emptyInputYieldsNothing() {
        assertTrue(RefreshRateManager.parsePanelTable("").isEmpty());
    }
}
