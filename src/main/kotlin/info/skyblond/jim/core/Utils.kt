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

private fun String.handleKeyword(keyword: String, onKeyword: (String) -> String): String {
    var sIndex = 0
    val result = mutableListOf<Int>()
    while (sIndex < this.length) {
        val i = this.indexOf(keyword, startIndex = sIndex, ignoreCase = true)
        if (i != -1) sIndex = i + keyword.length else break
        result.add(i)
    }
    sIndex = 0
    val sb = StringBuilder()
    while (result.isNotEmpty()) {
        val eIndex = result.removeFirst()
        // handle irrelevant text
        if (sIndex != eIndex)
            sb.append(this.substring(sIndex, eIndex))
        // handle keyword
        sb.append(onKeyword(this.substring(eIndex, eIndex + keyword.length)))
        sIndex = eIndex + keyword.length
    }
    // after loop
    if (sIndex == 0) return this // if not keyword found
    else if (sIndex < this.length) // handle the last one
        sb.append(this.substring(sIndex))
    return sb.toString()
}

private fun String.handleKeyword(keywords: List<String>, onKeyword: (String) -> String): String {
    var s = this
    keywords.forEach { keyword ->
        s = s.handleKeyword(keyword, onKeyword)
    }
    return s
}


fun Meta.prettyString(
    indent: String = "",
    keywords: List<String> = emptyList(),
    onKeyword: (String) -> String = { it }
): String {
    val sb = StringBuilder()
    sb.append(indent).append("$type: ${name.handleKeyword(keywords, onKeyword)}").appendLine()
    if (type.needValue) {
        sb.append(indent).append("\tValue: ")
        value.lines().forEachIndexed { index, s ->
            if (index != 0) sb.append(indent).append("\t     | ")
            sb.append(s.handleKeyword(keywords, onKeyword)).appendLine()
        }
    }
    return sb.toString()
}

fun Entry.prettyString(
    indent: String = "",
    keywords: List<String> = emptyList(),
    onKeyword: (String) -> String = { it }
): String {
    val sb = StringBuilder()
    sb.append(indent).append("$type: ${entryId.handleKeyword(keywords, onKeyword)}").appendLine()
    parentEntryId?.let {
        sb.append(indent).append("\tIN: ${it.handleKeyword(keywords, onKeyword)}").appendLine()
    }
    transaction { Entry.countByParentEntryId(entryId) }.let {
        if (it != 0L) sb.append(indent).append("\tChildren count: $it").appendLine()
    }
    sb.append(indent).append("\tName: ${name.handleKeyword(keywords, onKeyword)}").appendLine()
    sb.append(indent).append("\tNote: ")
    note.lines().forEachIndexed { index, s ->
        if (index != 0) sb.append(indent).append("\t    | ")
        sb.append(s.handleKeyword(keywords, onKeyword)).appendLine()
    }
    val metadataList = transaction { listMetadata() }
    if (metadataList.isNotEmpty()) {
        sb.append(indent).append("\tMetadata:").appendLine()
        metadataList.forEach { meta ->
            sb.append(meta.prettyString("$indent\t\t", keywords, onKeyword))
        }
    }
    return sb.toString()
}
