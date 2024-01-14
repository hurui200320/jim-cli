package info.skyblond.jim.http.handler

import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.http.EntryModel
import info.skyblond.jim.http.EntryModel.Companion.toModel
import info.skyblond.jim.http.MetaModel
import info.skyblond.jim.http.MetaModel.Companion.toModel
import info.skyblond.jim.http.castParam
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Params: [entryId: String, (field_name: String, value: String?)...]
 *
 * Fields name: parent_id (nullable), name (non-null), note(non-null)
 *
 * Return new obj.
 * @see [info.skyblond.jim.cli.UpdateCommand.EntryCommand].
 * */
fun handleUpdateEntry(params: List<*>): EntryModel {
    val entryId = params.castParam(0, String::class).uppercase()
    require(transaction { Entry.existsById(entryId) }) { "Entry $entryId not found" }

    val updateList = params.drop(1).chunked(2).map {
        require(it.size == 2) { "Incomplete pare of parameter" }
        val type = (it[0] as? String)?.lowercase() ?: error("field name is not a non-null string")
        val value = when(type) {
            "parent_id" -> (it[1] as? String)?.uppercase()
            "name" -> it[1] as? String ?: error("value of name is not a non-null string")
            "note" -> it[1] as? String ?: error("value of note is not a non-null string")
            else -> error("unknown field name: $type")
        }
        type to value
    }

    return transaction {
        Entry.selectById(entryId)?.apply {
            updateList.forEach { (field, value) ->
                when(field) {
                    "parent_id" -> parentEntryId = value
                    "name" -> name = value!!
                    "note" -> note = value!!
                }
            }
            update()
        }
    }?.toModel() ?: error("Entry $entryId not found")
}

/**
 * Params: [entryId, name: String, (field_name: String, value: String?)...]
 *
 * Fields name: type (non-null), value (non-null)
 *
 * Return new obj.
 * @see [info.skyblond.jim.cli.UpdateCommand.MetaCommand].
 * */
fun handleUpdateMeta(params: List<*>): MetaModel {
    val entryId = params.castParam(0, String::class).uppercase()
    val name = params.castParam(1, String::class)
    require(transaction { Entry.existsById(entryId) }) { "Entry $entryId not found" }
    require(transaction { Meta.existsByIdAndName(entryId, name) }) { "Meta $name not found" }

    val updateList = params.drop(2).chunked(2).map {
        require(it.size == 2) { "Incomplete pare of parameter" }
        val type = (it[0] as? String)?.lowercase() ?: error("field name is not a non-null string")
        val value = when(type) {
            "type" -> it[1] as? String ?: error("value of type is not a non-null string")
            "value" -> it[1] as? String ?: error("value of name is not a non-null string")
            else -> error("unknown field name: $type")
        }
        type to value
    }


    return transaction {
        Meta.selectByIdAndName(entryId, name)?.apply {
            updateList.forEach { (field, v) ->
                when(field) {
                    "type" -> type = v.let { Metas.Type.valueOf(it.uppercase()) }
                    "value" -> value = v
                }
            }
            update()
        }
    }?.toModel() ?: error("Entry $entryId not found")
}
