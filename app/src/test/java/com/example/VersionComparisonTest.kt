package com.example

import org.junit.Assert.*
import org.junit.Test

class VersionComparisonTest {

    private fun isNewerVersion(installed: String, latest: String): Boolean {
        return try {
            val cleanInstalled = installed.trim().removePrefix("v").removePrefix("V")
            val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")
            
            val installedParts = cleanInstalled.split(".")
            val latestParts = cleanLatest.split(".")
            
            val length = maxOf(installedParts.size, latestParts.size)
            for (i in 0 until length) {
                val installedPart = installedParts.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                val latestPart = latestParts.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                
                if (latestPart > installedPart) return true
                if (installedPart > latestPart) return false
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            latest != installed
        }
    }

    @Test
    fun testSameVersion_doesNotTriggerUpdate() {
        assertFalse(isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(isNewerVersion("1.0.1", "v1.0.1"))
        assertFalse(isNewerVersion("v1.0.1", "1.0.1"))
    }

    @Test
    fun testOlderInstalled_triggersUpdate() {
        assertTrue(isNewerVersion("1.0", "1.0.1"))
        assertTrue(isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(isNewerVersion("1.0.1", "1.0.2"))
    }

    @Test
    fun testNewerInstalled_doesNotTriggerUpdate() {
        assertFalse(isNewerVersion("1.0.2", "1.0.1"))
        assertFalse(isNewerVersion("2.0.0", "1.0.1"))
    }

    @Test
    fun testTwoDigitSubversion_correctComparison() {
        assertTrue(isNewerVersion("1.0.9", "1.0.10"))
        assertFalse(isNewerVersion("1.0.10", "1.0.9"))
    }
}
