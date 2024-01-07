package info.skyblond.jim.core

import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Metas
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun connectToSQLite(dbFile: File): Database {
    dbFile.parentFile?.mkdirs()
    val db = Database.connect(
        "jdbc:sqlite:$dbFile",
        driver = "org.sqlite.JDBC",
    )
    transaction(db) {
        withDataBaseLock {
            SchemaUtils.create(Entries, Metas)
        }
    }
    return db
}
