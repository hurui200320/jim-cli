package info.skyblond.jim.http.handler

import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.http.EntryModel
import info.skyblond.jim.http.EntryModel.Companion.toModel
import info.skyblond.jim.http.castParam
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Params: [entryId: String]
 * @see [info.skyblond.jim.cli.DeleteCommand.EntryCommand].
 * */
fun handleDeleteEntry(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class).uppercase()

    return transaction {
        val e = Entry.selectById(entryId)
        require(e != null) { "Entry $entryId not found" }
        // get model before deleted
        e.toModel().also { e.delete() }
    }
}

/**
 * Params: [entryId, name: String]
 * @see [info.skyblond.jim.cli.DeleteCommand.MetaCommand].
 * */
fun handleDeleteMeta(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class).uppercase()
    val name = params.castParam(1, String::class)

    return transaction {
        val meta = Meta.selectByIdAndName(entryId, name)
        require(meta != null) { "Meta $name on entry $entryId not found" }
        meta.getEntry().toModel().also { meta.delete() }
    }
}
