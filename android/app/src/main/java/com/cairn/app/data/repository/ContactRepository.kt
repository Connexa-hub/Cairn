package com.cairn.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cairn.app.data.local.dao.ContactDao
import com.cairn.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val dao: ContactDao
) {
    fun pagedContacts(): Flow<PagingData<ContactEntity>> =
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 30, enablePlaceholders = false)) {
            dao.observeAll()
        }.flow

    fun favorites(): Flow<List<ContactEntity>> = dao.observeFavorites()

    fun contact(id: Long): Flow<ContactEntity?> = dao.observeById(id)
    fun numbersFor(contactId: Long): Flow<List<PhoneNumberEntity>> = dao.observeNumbersForContact(contactId)
    fun emailsFor(contactId: Long): Flow<List<EmailEntity>> = dao.observeEmailsForContact(contactId)
    fun addressesFor(contactId: Long): Flow<List<AddressEntity>> = dao.observeAddressesForContact(contactId)
    fun notesFor(contactId: Long): Flow<List<NoteEntity>> = dao.observeNotes("contact", contactId)
    fun tags(): Flow<List<TagEntity>> = dao.observeTags()

    suspend fun toggleFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun addNote(contactId: Long, body: String) =
        dao.addNote(NoteEntity(ownerType = "contact", ownerId = contactId, body = body))

    suspend fun createTag(name: String, colorHex: String) = dao.addTag(TagEntity(name = name, colorHex = colorHex))
    suspend fun applyTag(contactId: Long, tagId: Long) = dao.tagContact(ContactTagCrossRef(contactId, tagId))
    suspend fun removeTag(contactId: Long, tagId: Long) = dao.untagContact(ContactTagCrossRef(contactId, tagId))

    suspend fun findByNumber(normalizedNumber: String): ContactEntity? =
        dao.findByNormalizedNumber(normalizedNumber)

    suspend fun searchByName(query: String): List<ContactEntity> = dao.searchByName(query)

    /** Bulk import from android.provider.ContactsContract — see ContactsImporter for the cursor-reading side. */
    suspend fun importBatch(
        contacts: List<ContactEntity>,
        numbers: List<PhoneNumberEntity>,
        emails: List<EmailEntity>,
        addresses: List<AddressEntity>
    ) {
        dao.upsertAll(contacts)
        if (numbers.isNotEmpty()) dao.upsertPhoneNumbers(numbers)
        if (emails.isNotEmpty()) dao.upsertEmails(emails)
        if (addresses.isNotEmpty()) dao.upsertAddresses(addresses)
    }
}
