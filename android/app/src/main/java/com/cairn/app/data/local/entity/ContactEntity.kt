package com.cairn.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index("displayName"), Index("isFavorite")]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    /** Android ContactsContract lookup key, used only for re-import/dedupe, never uploaded */
    val androidLookupKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "phone_numbers",
    indices = [Index("contactId"), Index("normalizedNumber")]
)
data class PhoneNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val number: String,
    /** Digits-only, no country-code assumptions beyond stripping symbols — used for search & join to call_logs */
    val normalizedNumber: String,
    val label: String = "mobile" // mobile | home | work | other
)

@Entity(tableName = "emails", indices = [Index("contactId")])
data class EmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val address: String,
    val label: String = "other"
)

@Entity(tableName = "addresses", indices = [Index("contactId")])
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val street: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val label: String = "home"
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6750A4"
)

@Entity(
    tableName = "contact_tags",
    primaryKeys = ["contactId", "tagId"],
    indices = [Index("tagId")]
)
data class ContactTagCrossRef(
    val contactId: Long,
    val tagId: Long
)

@Entity(tableName = "notes", indices = [Index("ownerType", "ownerId")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: String, // "contact" | "call"
    val ownerId: Long,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)
