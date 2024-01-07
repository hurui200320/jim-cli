package info.skyblond.jim.core.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * Table of entries.
 *
 * An entry is a record of location, box, or item.
 * It can have parent, representing:
 *   + sub location from a bigger location. Like a room in a house.
 *   + Box placed in other box or stored in a location.
 *   + Item stored in a box or stored in a location.
 * Entries can have name and note, helping user distinguish and find them.
 *
 * One should never use this definition directly. Use DAO.
 * */
object Entries : Table("t_entry") {
    /**
     * Entry ID, normally is the barcode. Max length: 20 chars.
     *
     * For convenience, Location may start with `L`;
     * Box may start with `B`;
     * Item may start with `I`.
     * */
    val entryId = varchar("entry_id", length = 20).uniqueIndex()

    /**
     * Entry type, enum. Max length: 32 chars.
     *
     * In case you don't use prefix to distinguish entries.
     * */
    val type = enumerationByName("entry_type", length = 32, klass = Type::class)

    /**
     * Parent entry ID.
     *
     * A location can only be placed in other locations, thus the parent can only be LOCATION;
     * A box can be placed in other boxes or locations, thus the parent can be BOX or LOCATION;
     * An item can be placed in other boxes or locations, thus the parent can be BOX or LOCATION.
     *
     * When parent entry is deleted, this field will be set to parent's parent.
     * */
    val parentEntryId = reference("parent_id", entryId).nullable()
    // SQLite don't follow those cascade rules, don't bother write them.

    /**
     * A short name for this entry. Max length 2048 chars.
     *
     * For Location, it can be a simple representation;
     * For box, it can be empty, or describe the color, size, shape, etc.;
     * For item, it can be a simple description.
     *
     * 2048 chars should be enough, try to be brief.
     * */
    val name = varchar("entry_name", length = 2048)

    /**
     * A detailed description for this entry. No soft limit (only hard limit from DB).
     *
     * For location, maybe the detailed address;
     * For box, maybe describe what in here;
     * For item, write whatever you want.
     * */
    val note = text("note")

    override val primaryKey = PrimaryKey(entryId)

    // TODO: Currently no performance issues. When encountered, try indexes.

    /**
     * Entry types
     * */
    enum class Type {
        /**
         * A physical location where the items and boxes are stored.
         * */
        LOCATION,

        /**
         * A small closure that contains other boxes or items.
         * */
        BOX,

        /**
         * An item, the smallest unit in this system.
         * */
        ITEM
    }
}

/**
 * The metadata of an entry.
 * */
object Metas : Table("t_metadata") {
    /**
     * Meta to which entry. Referenced by id.
     *
     * When target entry is deleted, all related metadata will be deleted.
     * */
    val entryId = reference(
        "entry_id", Entries.entryId,
        // delete when parent entry is deleted
        onDelete = ReferenceOption.CASCADE,
        // update when parent entry is updated
        onUpdate = ReferenceOption.CASCADE,
    )

    /**
     * The Name of this metadata. Should be brief and self-explanatory.
     * Max length 2048 chars.
     *
     * Normally, this is the name of some attributes, like purchased price, date, etc.
     * */
    val name = varchar("meta_name", length = 2048)

    /**
     * Type of the metadata [value], enum. Max length: 32 chars.
     * */
    val type = enumerationByName("meta_type", length = 32, klass = Type::class)

    /**
     * The value of the metadata. No soft limit (only hard limit from DB).
     * */
    val value = text("meta_value")

    override val primaryKey = PrimaryKey(arrayOf(entryId, name))

    /**
     * Type of the values.
     * */
    enum class Type {
        /**
         * A tag represents an attribute, like:
         *   + `lithium_battery` to mark an item have a battery inside
         *   + `fragile` to notice an item is fragile, should handle with care
         *   + `strong_magnet`, it's pretty self-explanatory
         * The value is ignored, keep it empty.
         *
         * Entries can be filtered by the presence of a given tag.
         * */
        TAG,

        /**
         * Plain text.
         *
         * This type of metadata doesn't filter entries. It just stores notes.
         * */
        TEXT,

        // TODO: other types like timestamp, integer, decimals.
        //       Those types require compute or compare,
        //       like find entries by range of date, some value bigger than something, etc.
        //       Should focus on SQLite's implementation, try not do it in software.
        //       Implement on needs.
        // TODO: Every new type of metadata should be supported by both terminal and http api.
    }
}
