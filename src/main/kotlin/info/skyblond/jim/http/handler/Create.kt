package info.skyblond.jim.http.handler

import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.http.EntryModel
import info.skyblond.jim.http.EntryModel.Companion.toModel
import info.skyblond.jim.http.castParam
import info.skyblond.jim.http.castParamNullable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Params: [entryId: String, type, parentId: String?, name, note: String]
 * Auto infer type when type is null.
 * @see [info.skyblond.jim.cli.CreateCommand.EntryCommand].
 * */
fun handleCreateEntry(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class).uppercase()
    val type = params.castParamNullable(1, String::class)
        ?.uppercase()?.let { Entries.Type.valueOf(it) } ?: Entry.inferType(entryId)
    val parentId = params.castParamNullable(2, String::class)?.uppercase()
    val name = params.castParam(3, String::class)
    val note = params.castParam(4, String::class)

    require(transaction { !Entry.existsById(entryId) }) {"Entry $entryId already exists"}

    return transaction {
        Entry(
            entryId = entryId,
            type = type,
            parentEntryId = parentId,
            name = name,
            note = note
        ).also { it.insert() }
    }.toModel()
}

/**
 * Params: [entryId, name, type, value: String]
 * @see [info.skyblond.jim.cli.CreateCommand.MetaCommand].
 * */
fun handleCreateMeta(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class).uppercase()
    val name = params.castParam(1, String::class)
    val type = params.castParam(2, String::class).uppercase()
        .let { Metas.Type.valueOf(it) }
    val value = params.castParam(3, String::class)

    require(transaction { Entry.existsById(entryId) }) {"Entry $entryId not found"}
    require(transaction { !Meta.existsByIdAndName(entryId, name) }) {"Meta $name already exists" }


    return transaction {
        Meta(
            entryId = entryId,
            type = type,
            name = name,
            value = value
        ).also { it.insert() }.getEntry()
    }.toModel()
}
