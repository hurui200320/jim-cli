package info.skyblond.jim.core

import com.google.gson.stream.JsonReader
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringWriter
import kotlin.test.assertEquals

class ExportImportTest {
    private lateinit var dbFile: File
    private lateinit var db: Database

    private val json = """
            [
              {
                "entryId": "L0001",
                "type": "LOCATION",
                "parentEntryId": null,
                "name": "test location",
                "note": "something for this location\r\nof course\r\n\r\nwith multiple lines!",
                "metadata": []
              },
              {
                "entryId": "B001",
                "type": "BOX",
                "parentEntryId": "L0001",
                "name": "test box #1",
                "note": "something simple",
                "metadata": [
                  {
                    "name": "a_note",
                    "type": "TEXT",
                    "value": "A loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong note\r\n\r\n\r\nwith\r\n\r\nmul\r\n\r\ntiple\r\n\r\nlines!!!!!"
                  }
                ]
              },
              {
                "entryId": "B002",
                "type": "BOX",
                "parentEntryId": "L0001",
                "name": "test box #2",
                "note": "",
                "metadata": []
              },
              {
                "entryId": "I0001",
                "type": "ITEM",
                "parentEntryId": "B002",
                "name": "An item",
                "note": "LOL",
                "metadata": [
                  {
                    "name": "a_tag",
                    "type": "TAG",
                    "value": ""
                  }
                ]
              }
            ]
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        dbFile = File.createTempFile("jim-test", ".db")
        dbFile.deleteOnExit()
        db = connectToSQLite(dbFile)
    }

    @Test
    fun `test import then export`() {
        importDataFromJson(JsonReader(json.reader()), false, db) {}
        val writer = StringWriter()
        exportDataToJson(createPrettyJsonWriter(writer), db)
        assertEquals(json, writer.buffer.toString())
    }

    @Test
    fun `test import should skip`() {
        // import once
        importDataFromJson(JsonReader(json.reader()), false, db) {}
        // import again
        val r = importDataFromJson(JsonReader(json.reader()), false, db) {}
        assertEquals(4, r.entrySkipCounter)
        assertEquals(2, r.metadataSkipCounter)
    }

    @Test
    fun `test import should overwrite`() {
        // import once
        importDataFromJson(JsonReader(json.reader()), false, db) {}

        // alter the content in db
        transaction(db) {
            Entry.selectById("L0001")!!.also {
                it.note = "altered"
                it.update()
            }
            Meta.selectByIdAndName("B001", "a_note")!!.also {
                it.value = "altered"
                it.update()
            }
        }

        // import again
        val r = importDataFromJson(JsonReader(json.reader()), true, db) {}
        assertEquals(4, r.entryOverwriteCounter)
        assertEquals(2, r.metadataOverwriteCounter)

        // verify
        val writer = StringWriter()
        exportDataToJson(createPrettyJsonWriter(writer), db)
        assertEquals(json, writer.buffer.toString())
    }


}
