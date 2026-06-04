package io.audita.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestLinkEntityTest {

    private static final UUID SMALLER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LARGER  = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @Test
    void canonicalOrderPutsSmallerUuidFirst() {
        UUID[] result = RequestLinkEntity.canonicalOrder(LARGER, SMALLER);
        assertEquals(SMALLER, result[0]);
        assertEquals(LARGER, result[1]);
    }

    @Test
    void canonicalOrderIsIdempotentWhenAlreadyOrdered() {
        UUID[] result = RequestLinkEntity.canonicalOrder(SMALLER, LARGER);
        assertEquals(SMALLER, result[0]);
        assertEquals(LARGER, result[1]);
    }

    @Test
    void canonicalOrderThrowsOnSameUuid() {
        UUID same = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> RequestLinkEntity.canonicalOrder(same, same));
    }

    @Test
    void defaultValuesAreSetOnConstruction() {
        RequestLinkEntity link = new RequestLinkEntity();
        assertNotNull(link.getCreatedAt());
        assertNull(link.getId());
        assertNull(link.getRequestIdA());
        assertNull(link.getRequestIdB());
        assertNull(link.getLinkedBy());
    }

    @Test
    void fieldsCanBeSetAndRetrieved() {
        RequestLinkEntity link = new RequestLinkEntity();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID by = UUID.randomUUID();

        link.setRequestIdA(a);
        link.setRequestIdB(b);
        link.setLinkedBy(by);

        assertEquals(a, link.getRequestIdA());
        assertEquals(b, link.getRequestIdB());
        assertEquals(by, link.getLinkedBy());
    }
}
