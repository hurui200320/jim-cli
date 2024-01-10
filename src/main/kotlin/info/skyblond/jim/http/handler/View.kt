package info.skyblond.jim.http.handler

import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.http.EntryModel
import info.skyblond.jim.http.EntryModel.Companion.toModel
import info.skyblond.jim.http.castParam
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Params: [entry_id: String]
 * @see [info.skyblond.jim.cli.ViewCommand].
 * */
fun handleView(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class)
    return transaction {
        Entry.selectById(entryId.uppercase())
    }?.toModel() ?: error("Entry not found")
}
