package com.tabletaide.ide.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitPushFailuresTest {

    @Test
    fun isNonFastForward_detectsMappedPushMessage() {
        assertTrue(
            GitPushFailures.isNonFastForward(
                "Push was rejected because the remote branch moved ahead. Pull or sync first.",
            ),
        )
    }

    @Test
    fun isNonFastForward_ignoresAuthErrors() {
        assertFalse(
            GitPushFailures.isNonFastForward(
                "Push authentication failed. Check the saved token for this git host.",
            ),
        )
    }
}
