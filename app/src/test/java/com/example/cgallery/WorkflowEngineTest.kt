package com.example.cgallery

import com.example.cgallery.data.InboxStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkflowEngineTest {
    @Test
    fun testStatusTransitions() {
        // Simple verification that our new statuses exist and are accessible
        val status = InboxStatus.Detected
        assertEquals("Detected", status.name)
        
        val queued = InboxStatus.Queued
        assertEquals("Queued", queued.name)
        
        val completed = InboxStatus.Completed
        assertEquals("Completed", completed.name)
    }
}
