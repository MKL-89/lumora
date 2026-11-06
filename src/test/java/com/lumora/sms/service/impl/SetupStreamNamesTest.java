package com.lumora.sms.service.impl;

import com.lumora.sms.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SetupStreamNamesTest {

    @Test
    void splitsTrimsAndDropsRepeats() {
        assertEquals(List.of("East", "West"), SetupServiceImpl.streamNames(" East , West, east ,"));
        assertEquals(List.of("Blue Stream"), SetupServiceImpl.streamNames("Blue   Stream"));
    }

    @Test
    void needsBetweenOneAndEightStreams() {
        assertThrows(BusinessRuleViolationException.class, () -> SetupServiceImpl.streamNames(" , "));
        assertThrows(BusinessRuleViolationException.class,
                () -> SetupServiceImpl.streamNames("A,B,C,D,E,F,G,H,I"));
    }
}
