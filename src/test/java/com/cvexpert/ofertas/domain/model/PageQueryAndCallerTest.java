package com.cvexpert.ofertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class PageQueryAndCallerTest {

    @Test
    void pageLimitsFollowTheContract() {
        assertDoesNotThrow(() -> new PageQuery(1, 100, null));
        assertThrows(InvalidRequestException.class, () -> new PageQuery(0, 10, null));
        assertThrows(InvalidRequestException.class, () -> new PageQuery(1, 0, null));
        assertThrows(InvalidRequestException.class, () -> new PageQuery(1, 101, null));
    }

    @Test
    void blankSearchMeansNoFilter() {
        PageQuery q = new PageQuery(1, 10, "   ");
        assertNull(q.search());
        assertFalse(q.hasSearch());
        assertEquals("kotlin", new PageQuery(1, 10, "  kotlin ").search());
    }

    @Test
    void callerNeedsAUserId() {
        assertThrows(InvalidRequestException.class, () -> new Caller(" ", "a@example.com", null));
    }

    @Test
    void onlyTheAdminRolePassesRequireAdmin() {
        assertDoesNotThrow(Fixtures.ADMIN::requireAdmin);
        assertThrows(ForbiddenException.class, Fixtures.ANA::requireAdmin);
        assertThrows(ForbiddenException.class, () -> new Caller("x", "x@example.com", "editor").requireAdmin());
    }
}
