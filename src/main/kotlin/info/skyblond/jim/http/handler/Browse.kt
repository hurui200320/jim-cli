package info.skyblond.jim.http.handler

import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.http.castParamNullable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Params: [parent_id: String?]
 * @see [info.skyblond.jim.cli.BrowseCommand].
 * */
fun handleBrowse(params: List<*>): List<String> {
    val parentId = params.castParamNullable(0, String::class)
    return transaction {
        Entry.selectAllByParentEntryId(parentId?.uppercase())
    }.map { it.entryId }
}
