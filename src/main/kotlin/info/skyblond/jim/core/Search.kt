package info.skyblond.jim.core

import info.skyblond.jim.core.db.Entry
import org.jetbrains.exposed.sql.transactions.transaction

fun searchKeywords(keywords: List<String>): Sequence<Entry> {
    // each keyword may give overlapping result, use set to dedupe
    val entryIds = mutableSetOf<String>()
    transaction {
        for (keyword in keywords) {
            // for each keyword, search in id, parent id, name, note
            // and meta's name and value
            Entry.selectAllByKeyword(keyword).forEach {
                entryIds.add(it.entryId)
            }
        }
    }
    return sequence {
        entryIds.forEach { entryId ->
            transaction { Entry.selectById(entryId) }?.let { yield(it) }
        }
    }
}
