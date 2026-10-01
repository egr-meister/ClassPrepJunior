package com.classprep.junior.domain

import com.classprep.junior.domain.planning.Validation
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {
    @Test fun subjectNames() {
        assertNotNull(Validation.subjectName("   ", emptyList()))
        assertNotNull(Validation.subjectName("a".repeat(31), emptyList()))
        assertNotNull(Validation.subjectName(" mathematics ", listOf("Mathematics")))
        assertNull(Validation.subjectName("Mathematics", emptyList()))
        assertNull(Validation.subjectName("a".repeat(30), emptyList()))
    }

    @Test fun itemNamesAndNotes() {
        assertNotNull(Validation.itemName(""))
        assertNotNull(Validation.itemName("x".repeat(61)))
        assertNull(Validation.itemName("Notebook"))
        assertNotNull(Validation.note("n".repeat(121)))
        assertNull(Validation.note("n".repeat(120)))
    }
}
