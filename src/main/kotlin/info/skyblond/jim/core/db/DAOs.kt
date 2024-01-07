package info.skyblond.jim.core.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

private fun checkEntryId(entryId: String) {
    require(entryId.uppercase() == entryId) { "Entry id must be uppercase: $entryId" }
    require(entryId.length <= 20) { "Entry id must be no longer than 20 chars: $entryId" }
}

/**
 * DAO of [Metadatas].
 *
 * The [entryId] must be uppercase and no longer than 20 chars.
 * The [name] can be anything but no longer than 2048 chars.
 * The [type] and [value] follows the description from [Metadatas.Type].
 * */
data class Metadata(
    val entryId: String,
    val name: String,
    var type: Metadatas.Type,
    var value: String
) {
    init {
        checkEntryId(entryId)
        require(name.length <= 2048) { "Name must be no longer than 2048 chars: $name" }
    }

    fun insert() {
        require(Entry.existsById(entryId)) { "Entry $entryId does not exist" }
        Metadatas.insert {
            it[entryId] = this@Metadata.entryId
            it[name] = this@Metadata.name
            it[type] = this@Metadata.type
            it[value] = this@Metadata.value
        }
    }

    fun update() {
        require(Entry.existsById(entryId)) { "Entry $entryId does not exist" }
        Metadatas.update({ Entries.entryId eq entryId }) {
            it[type] = this@Metadata.type
            it[value] = this@Metadata.value
        }
    }

    fun insertOrUpdate() = if (existsByIdAndName(entryId, name)) update() else insert()

    fun delete() = deleteByIdAndName(entryId, name)

    fun getEntry() = Entry.selectById(entryId)
        ?: error("Database inconsistent: entry $entryId has metadata but no entry record")

    companion object {
        private fun ResultRow.parse() = Metadata(
            entryId = this[Metadatas.entryId],
            name = this[Metadatas.name],
            type = this[Metadatas.type],
            value = this[Metadatas.value]
        )

        fun existsByIdAndName(entryId: String, name: String) =
            Metadatas.select {
                (Metadatas.entryId eq entryId) and (Metadatas.name eq name)
            }.count() > 0

        fun selectByIdAndName(entryId: String, name: String) =
            Metadatas.select {
                (Metadatas.entryId eq entryId) and (Metadatas.name eq name)
            }.firstOrNull()?.parse()

        fun deleteByIdAndName(entryId: String, name: String) =
            Metadatas.deleteWhere {
                (Metadatas.entryId eq entryId) and (Metadatas.name eq name)
            }

        fun selectAllById(entryId: String) =
            Metadatas.select { Metadatas.entryId eq entryId }
                .orderBy(Metadatas.entryId)
                .map { it.parse() }

        fun deleteAllById(entryId: String) =
            Metadatas.deleteWhere { Metadatas.entryId eq entryId }
    }
}

/**
 * DAO of [Entries].
 *
 * The [entryId] must be uppercase and no longer than 20 chars.
 * The [type] will be automatically inferred as long as you follow the LBI naming rule.
 * The [parentEntryId] must exist when insert or update, if not null.
 * The [name] can be anything but no longer than 2048 chars.
 * The [note] can be anything with no length limit, but SQLite's limit applies.
 * */
data class Entry(
    val entryId: String,
    val type: Entries.Type = when (entryId.first()) {
        'L' -> Entries.Type.LOCATION
        'B' -> Entries.Type.BOX
        'I' -> Entries.Type.ITEM
        else -> throw IllegalArgumentException("Cannot infer type from entry id: $entryId")
    },
    var parentEntryId: String? = null,
    var name: String = "",
    var note: String = "",
) {
    init {
        checkEntryId(entryId)
        require(name.length <= 2048) { "Name must be no longer than 2048 chars: $name" }
    }

    fun insert() {
        parentEntryId?.let {
            require(existsById(it)) { "Parent entry $entryId does not exist" }
        }
        Entries.insert {
            it[entryId] = this@Entry.entryId
            it[type] = this@Entry.type
            it[parentEntryId] = this@Entry.parentEntryId
            it[name] = this@Entry.name
            it[note] = this@Entry.note
        }
    }

    fun update() {
        parentEntryId?.let {
            require(existsById(it)) { "Parent entry $entryId does not exist" }
        }
        Entries.update({ Entries.entryId eq entryId }) {
            it[parentEntryId] = this@Entry.parentEntryId
            it[name] = this@Entry.name
            it[note] = this@Entry.note
        }
    }

    fun insertOrUpdate() = if (existsById(entryId)) update() else insert()

    fun delete() {
        // delete all related entry
        Metadata.deleteAllById(entryId)
        // update all child's parent to our parent
        Entries.update({ Entries.parentEntryId eq entryId }) {
            it[parentEntryId] = this@Entry.parentEntryId
        }
        // delete our entry
        Entries.deleteWhere { Entries.entryId eq this@Entry.entryId }
    }

    fun listMetadata() = Metadata.selectAllById(entryId)

    companion object {
        private fun ResultRow.parse() = Entry(
            entryId = this[Entries.entryId],
            type = this[Entries.type],
            parentEntryId = this[Entries.parentEntryId],
            name = this[Entries.name],
            note = this[Entries.note],
        )

        fun existsById(entryId: String) = Entries.select { Entries.entryId eq entryId }.count() > 0
        fun selectById(entryId: String) = Entries.select { Entries.entryId eq entryId }.firstOrNull()?.parse()
        fun deleteById(entryId: String) = selectById(entryId)?.delete()

        /**
         * Select all [Entry] which:
         *   + [name] contains [keyword], or
         *   + [note] contains [keyword], or
         *   + [Metadata.name] contains [keyword], or
         *   + [Metadata.value] contains [keyword].
         * */
        fun selectAllByKeyword(keyword: String) = Entries
            .join(Metadatas, JoinType.INNER, onColumn = Entries.entryId, otherColumn = Metadatas.entryId)
            .slice(Entries.columns)
            .select {
                (Entries.name like "%$keyword%") or (Entries.note like "%$keyword%") or
                        (Metadatas.name like "%$keyword%") or (Metadatas.value like "%$keyword%")
            }
            .orderBy(Entries.entryId)
            .map { it.parse() }


        /**
         * Select all [Entry] which has metadata with type of tag that having name containing [tagKeyword]
         * */
        fun selectAllByTagKeyword(tagKeyword: String) = Entries
            .join(Metadatas, JoinType.INNER, onColumn = Entries.entryId, otherColumn = Metadatas.entryId)
            .slice(Entries.columns)
            .select {
                (Metadatas.type eq Metadatas.Type.TAG) and (Metadatas.name like "%$tagKeyword%")
            }
            .orderBy(Entries.entryId)
            .map { it.parse() }

        fun selectAllByParentEntryId(parentEntryId: String?) = Entries
            .select { Entries.parentEntryId eq parentEntryId }
            .orderBy(Entries.entryId)
            .map { it.parse() }
    }
}
