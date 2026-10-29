package com.expiryguard.app.data.backup

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEntryValidatorTest {
    @Test
    fun `only data json and simple image entries are accepted`() {
        assertTrue(BackupEntryValidator.isValid("data.json"))
        assertTrue(BackupEntryValidator.isValid("images/a-1.jpg"))
        assertFalse(BackupEntryValidator.isValid("../data.json"))
        assertFalse(BackupEntryValidator.isValid("images/../secret"))
        assertFalse(BackupEntryValidator.isValid("images\\secret.jpg"))
        assertFalse(BackupEntryValidator.isValid("/data.json"))
        assertFalse(BackupEntryValidator.isValid("images/nested/a.jpg"))
        assertFalse(BackupEntryValidator.isValid("other.json"))
    }

    @Test
    fun `canonical child check rejects traversal`() {
        val root = Files.createTempDirectory("backup-root").toFile()
        try {
            assertTrue(BackupEntryValidator.isSafeChild(root, File(root, "images/a.jpg")))
            assertFalse(BackupEntryValidator.isSafeChild(root, File(root, "../outside")))
            assertFalse(BackupEntryValidator.isSafeChild(root, root))
        } finally {
            root.deleteRecursively()
        }
    }
}
