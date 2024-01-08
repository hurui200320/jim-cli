package info.skyblond.jim.core

import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
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

fun Meta.prettyString(indent: String = ""): String {
    val sb = StringBuilder()
    sb.append(indent).append("$type: $name").appendLine()
    if (type.needValue){
        sb.append(indent).append("\tValue: ")
        value.lines().forEachIndexed { index, s ->
            if (index != 0 ) sb.append(indent).append("\t     | ")
            sb.append(s).appendLine()
        }
    }
    return sb.toString()
}

fun Entry.prettyString(indent: String = ""): String {
    val sb = StringBuilder()
    sb.append(indent).append("$type: $entryId").appendLine()
    parentEntryId?.let {
        sb.append(indent).append("\tIN: $it").appendLine()
    }
    sb.append(indent).append("\tName: $name").appendLine()
    sb.append(indent).append("\tNote: ")
    note.lines().forEachIndexed { index, s ->
        if (index != 0 ) sb.append(indent).append("\t    | ")
        sb.append(s).appendLine()
    }
    val metadataList = transaction { listMetadata() }
    if (metadataList.isNotEmpty()){
        sb.append(indent).append("\tMetadata:").appendLine()
        metadataList.forEach { meta ->
            sb.append(meta.prettyString("$indent\t\t"))
        }
    }
    return sb.toString()
}
