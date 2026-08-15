package com.cairn.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.cairn.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoneNumbers(numbers: List<PhoneNumberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmails(emails: List<EmailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddresses(addresses: List<AddressEntity>)

    @Query("SELECT * FROM contacts ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): PagingSource<Int, ContactEntity>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun observeById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM phone_numbers WHERE contactId = :contactId")
    fun observeNumbersForContact(contactId: Long): Flow<List<PhoneNumberEntity>>

    @Query("SELECT * FROM emails WHERE contactId = :contactId")
    fun observeEmailsForContact(contactId: Long): Flow<List<EmailEntity>>

    @Query("SELECT * FROM addresses WHERE contactId = :contactId")
    fun observeAddressesForContact(contactId: Long): Flow<List<AddressEntity>>

    @Query("""
        SELECT c.* FROM contacts c
        INNER JOIN phone_numbers p ON p.contactId = c.id
        WHERE p.normalizedNumber = :normalizedNumber
        LIMIT 1
    """)
    suspend fun findByNormalizedNumber(normalizedNumber: String): ContactEntity?

    @Query("""
        SELECT c.* FROM contacts c
        WHERE c.displayName LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY c.displayName ASC LIMIT 50
    """)
    suspend fun searchByName(query: String): List<ContactEntity>

    @Query("UPDATE contacts SET isFavorite = :isFavorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun tagContact(crossRef: ContactTagCrossRef)

    @Delete
    suspend fun untagContact(crossRef: ContactTagCrossRef)

    @Query("""
        SELECT c.* FROM contacts c
        INNER JOIN contact_tags ct ON ct.contactId = c.id
        WHERE ct.tagId = :tagId
        ORDER BY c.displayName ASC
    """)
    fun observeContactsForTag(tagId: Long): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeNotes(ownerType: String, ownerId: Long): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun contactCount(): Int
}
