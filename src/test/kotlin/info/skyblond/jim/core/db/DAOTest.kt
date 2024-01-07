package info.skyblond.jim.core.db

import info.skyblond.jim.core.connectToSQLite
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DAOTest {
    private lateinit var dbFile: File
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        dbFile = File.createTempFile("jim-test", ".db")
        dbFile.deleteOnExit()
        db = connectToSQLite(dbFile)
    }

    @Test
    fun `test duplicate`() {
        transaction(db) {
            Entry("ID").insert()
            Meta("ID", "name", Metas.Type.TAG).insert()
        }
        // exposed throws an error on a duplicate primary key
        assertThrows<ExposedSQLException> {
            transaction(db) {
                Entry("ID", Entries.Type.BOX).insert()
            }
        }
        assertThrows<ExposedSQLException> {
            transaction(db) {
                Meta("ID", "name", Metas.Type.TEXT, "?").insert()
            }
        }
    }

    @Test
    fun `test ref missing entry`() {
        // error from our code using "require"
        assertThrows<IllegalArgumentException> {
            transaction(db) {
                Entry("BID", parentEntryId = "?").insert()
            }
        }
        assertThrows<IllegalArgumentException> {
            transaction(db) {
                Meta("?", "name", Metas.Type.TAG).insert()
            }
        }
    }

    @Test
    fun `test delete entry`() {
        transaction(db) {
            Entry("ID", Entries.Type.BOX).insert()
            Entry("ID2", Entries.Type.BOX, parentEntryId = "ID").insert()
            Entry("ID3", Entries.Type.BOX, parentEntryId = "ID2").insert()
            Meta("ID2", "name", Metas.Type.TAG).insert()
        }
        transaction(db) {
            Entry.deleteById("ID2")
        }
        transaction(db) {
            assertEquals("ID", Entry.selectById("ID3")!!.parentEntryId)
            assertTrue { Meta.selectAllById("ID2").isEmpty() }
        }

    }

}
