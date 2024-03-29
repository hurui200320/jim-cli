package info.skyblond.jim.core.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

private fun checkEntryId(entryId: String) {
    require(entryId.isNotBlank()) { "Entry id must not be blank" }
    require(entryId.uppercase() == entryId) { "Entry id must be uppercase: $entryId" }
    require(entryId.length <= 20) { "Entry id must be no longer than 20 chars: $entryId" }
}

/**
 * DAO of [Metas].
 *
 * The [entryId], see [Entry.entryId].
 * The [name] can be anything but no longer than 2048 chars.
 * The [type] and [value] follows the description from [Metas.Type].
 * */
data class Meta(
    val entryId: String,
    val name: String,
    var type: Metas.Type,
    var value: String = ""
) {
    init {
        require(name.length <= 2048) { "Name must be no longer than 2048 chars: $name" }
    }

    fun insert() {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(Entry.existsById(entryId)) { "Entry $entryId does not exist" }
        Metas.insert {
            it[entryId] = this@Meta.entryId
            it[name] = this@Meta.name
            it[type] = this@Meta.type
            it[value] = if (this@Meta.type.needValue) this@Meta.value else ""
        }
    }

    fun update() {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(Entry.existsById(entryId)) { "Entry $entryId does not exist" }
        Metas.update({ (Metas.entryId eq entryId) and (Metas.name eq name) }) {
            it[type] = this@Meta.type
            it[value] = if (this@Meta.type.needValue) this@Meta.value else ""
        }
    }

    fun insertOrUpdate() = if (existsByIdAndName(entryId, name)) update() else insert()

    fun delete() = deleteByIdAndName(entryId, name)

    fun getEntry() = Entry.selectById(entryId)
        ?: error("Database inconsistent: entry $entryId has metadata but no entry record")

    companion object {
        private fun ResultRow.parse() = Meta(
            entryId = this[Metas.entryId],
            name = this[Metas.name],
            type = this[Metas.type],
            value = this[Metas.value]
        )

        fun existsByIdAndName(entryId: String, name: String) =
            Metas.select {
                (Metas.entryId eq entryId) and (Metas.name eq name)
            }.count() > 0

        fun selectByIdAndName(entryId: String, name: String) =
            Metas.select {
                (Metas.entryId eq entryId) and (Metas.name eq name)
            }.firstOrNull()?.parse()

        fun deleteByIdAndName(entryId: String, name: String) =
            Metas.deleteWhere {
                (Metas.entryId eq entryId) and (Metas.name eq name)
            }

        fun selectAllById(entryId: String) =
            Metas.select { Metas.entryId eq entryId }
                .orderBy(Metas.type to SortOrder.ASC, Metas.entryId to SortOrder.ASC)
                .map { it.parse() }

        fun deleteAllById(entryId: String) =
            Metas.deleteWhere { Metas.entryId eq entryId }
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
    val type: Entries.Type = inferType(entryId),
    var parentEntryId: String? = null,
    var name: String = "",
    var note: String = "",
) {
    init {
        checkEntryId(entryId)
        require(name.length <= 2048) { "Name must be no longer than 2048 chars: $name" }
    }

    private fun checkParent() = parentEntryId?.let {
        require(it != entryId) {"Parent cannot be entry itself"}
        val parent = selectById(it)
        require(parent != null) { "Parent entry $it does not exist" }
        when (type) {
            // location can only have location as parent
            Entries.Type.LOCATION -> require(parent.type in listOf(Entries.Type.LOCATION))
            // box and item can have location and box as parent
            Entries.Type.BOX, Entries.Type.ITEM -> require(
                parent.type in listOf(
                    Entries.Type.LOCATION,
                    Entries.Type.BOX
                )
            ) { "Entry type $type cannot have ${parent.type} as parent" }
        }
    }

    fun insert() {
        checkParent()
        Entries.insert {
            it[entryId] = this@Entry.entryId
            it[type] = this@Entry.type
            it[parentEntryId] = this@Entry.parentEntryId
            it[name] = this@Entry.name
            it[note] = this@Entry.note
        }
    }

    fun update() {
        checkParent()
        Entries.update({ Entries.entryId eq entryId }) {
            it[type] = this@Entry.type
            it[parentEntryId] = this@Entry.parentEntryId
            it[name] = this@Entry.name
            it[note] = this@Entry.note
        }
    }

    fun insertOrUpdate() = if (existsById(entryId)) update() else insert()

    fun delete() {
        // delete all related entry
        Meta.deleteAllById(entryId)
        // update all child's parent to our parent
        Entries.update({ Entries.parentEntryId eq entryId }) {
            it[parentEntryId] = this@Entry.parentEntryId
        }
        // delete our entry
        Entries.deleteWhere { Entries.entryId eq this@Entry.entryId }
    }

    fun listMetadata() = Meta.selectAllById(entryId)

    companion object {
        fun inferType(entryId: String) = when (entryId.first()) {
            'L' -> Entries.Type.LOCATION
            'B' -> Entries.Type.BOX
            'I' -> Entries.Type.ITEM
            else -> throw IllegalArgumentException("Cannot infer type from entry id: $entryId")
        }

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
         *   + [entryId] contains [keyword], or
         *   + [parentEntryId] contains [keyword], or
         *   + [name] contains [keyword], or
         *   + [note] contains [keyword], or
         *   + [Meta.name] contains [keyword], or
         *   + [Meta.value] contains [keyword].
         * Case-insensitive.
         * */
        fun selectAllByKeyword(keyword: String) = Entries
            .join(Metas, JoinType.LEFT, onColumn = Entries.entryId, otherColumn = Metas.entryId)
            .slice(Entries.columns)
            .select {
                val k = "%$keyword%".lowercase()
                (Entries.entryId.lowerCase() like k) or
                (Entries.parentEntryId.lowerCase() like k) or
                (Entries.name.lowerCase() like k) or
                        (Entries.note.lowerCase() like k) or
                        (Metas.name.lowerCase() like k) or
                        (Metas.value.lowerCase() like k)
            }
            .orderBy(Entries.entryId)
            .asSequence()
            .map { it.parse() }

        fun selectAllByParentEntryId(parentEntryId: String?) = Entries
            .select { Entries.parentEntryId eq parentEntryId }
            .orderBy(Entries.entryId)
            .map { it.parse() }

        fun countByParentEntryId(parentEntryId: String?) = Entries
            .select { Entries.parentEntryId eq parentEntryId }
            .count()
    }
}
