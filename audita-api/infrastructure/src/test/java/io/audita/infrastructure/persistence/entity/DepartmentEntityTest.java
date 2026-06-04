package io.audita.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentEntityTest {

    @Test
    void defaultValuesAreSetOnConstruction() {
        DepartmentEntity dept = new DepartmentEntity();

        assertTrue(dept.isActive());
        assertEquals(0, dept.getDisplayOrder());
        assertNotNull(dept.getCreatedAt());
        assertNotNull(dept.getUpdatedAt());
    }

    @Test
    void nameCanBeSetAndRetrieved() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setName("Engineering");
        assertEquals("Engineering", dept.getName());
    }

    @Test
    void codeCanBeSetAndRetrieved() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setCode("ENG");
        assertEquals("ENG", dept.getCode());
    }

    @Test
    void codeDefaultsToNull() {
        DepartmentEntity dept = new DepartmentEntity();
        assertNull(dept.getCode());
    }

    @Test
    void activeFlagCanBeToggled() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setActive(false);
        assertFalse(dept.isActive());
    }

    @Test
    void displayOrderCanBeSet() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDisplayOrder(5);
        assertEquals(5, dept.getDisplayOrder());
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        DepartmentEntity dept = new DepartmentEntity();
        OffsetDateTime before = dept.getUpdatedAt();

        dept.onUpdate();

        assertFalse(dept.getUpdatedAt().isBefore(before));
    }
}
