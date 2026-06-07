package com.example.classcast

import com.example.classcast.model.ClassItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ClassItem data class and related business logic.
 *
 * These run on the JVM (no Android framework needed).
 * Run with: ./gradlew test
 */
class ClassItemTest {

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun `default classId is empty string`() {
        val item = ClassItem()
        assertEquals("", item.classId)
    }

    @Test
    fun `default status is active`() {
        val item = ClassItem()
        assertEquals("active", item.status)
    }

    @Test
    fun `default createdAt is null`() {
        val item = ClassItem()
        assertNull(item.createdAt)
    }

    @Test
    fun `default description is empty string`() {
        val item = ClassItem()
        assertEquals("", item.description)
    }

    // ── Copy semantics ────────────────────────────────────────────────────

    @Test
    fun `copy with classId preserves other fields`() {
        val original = ClassItem(
            courseName = "Android Dev",
            courseCode = "CS301",
            ownerId = "uid123",
            status = "active"
        )
        val copy = original.copy(classId = "docABC")
        assertEquals("docABC", copy.classId)
        assertEquals("Android Dev", copy.courseName)
        assertEquals("CS301", copy.courseCode)
        assertEquals("uid123", copy.ownerId)
        assertEquals("active", copy.status)
    }

    @Test
    fun `copy with status change creates new object`() {
        val active = ClassItem(courseName = "Math", status = "active")
        val inactive = active.copy(status = "inactive")
        assertEquals("inactive", inactive.status)
        assertEquals("active", active.status) // original unchanged
    }

    // ── Status business rules ─────────────────────────────────────────────

    @Test
    fun `active status string is recognized`() {
        val item = ClassItem(status = "active")
        assertTrue(item.status == "active")
    }

    @Test
    fun `inactive status string is recognized`() {
        val item = ClassItem(status = "inactive")
        assertTrue(item.status == "inactive")
    }

    @Test
    fun `unknown status is neither active nor inactive`() {
        val item = ClassItem(status = "pending")
        assertFalse(item.status == "active")
        assertFalse(item.status == "inactive")
    }

    // ── Equality ──────────────────────────────────────────────────────────

    @Test
    fun `two items with same data are equal`() {
        val a = ClassItem(classId = "1", courseName = "CS", courseCode = "CS101", ownerId = "u1", status = "active")
        val b = ClassItem(classId = "1", courseName = "CS", courseCode = "CS101", ownerId = "u1", status = "active")
        assertEquals(a, b)
    }

    @Test
    fun `items with different classId are not equal`() {
        val a = ClassItem(classId = "1", courseName = "CS")
        val b = ClassItem(classId = "2", courseName = "CS")
        assertNotEquals(a, b)
    }

    @Test
    fun `items with different ownerId are not equal`() {
        val a = ClassItem(classId = "1", ownerId = "ownerA")
        val b = ClassItem(classId = "1", ownerId = "ownerB")
        assertNotEquals(a, b)
    }
}
