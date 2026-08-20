package com.cairn.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cairn.app.data.local.dao.CallLogDao
import com.cairn.app.data.local.dao.ContactDao
import com.cairn.app.data.local.entity.*
import com.cairn.app.data.security.DbKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ContactEntity::class,
        PhoneNumberEntity::class,
        EmailEntity::class,
        AddressEntity::class,
        TagEntity::class,
        ContactTagCrossRef::class,
        NoteEntity::class,
        CallLogEntity::class,
        CallLogFts::class,
        StatsCacheEntity::class
    ],
    version = 1,
    // exportSchema disabled: Room's schema round-trip (exporting, then
    // deserializing an existing schema.json to diff against on the next
    // build) hits a confirmed, open Google issue-tracker bug
    // (issuetracker.google.com/issues/400483860) — an AbstractMethodError
    // from a binary mismatch between Room's own bundled, pre-compiled
    // kotlinx.serialization generated classes and whatever
    // kotlinx-serialization-core version resolves on the build classpath.
    // Not something reliably fixable by pinning a version here, since
    // Room's side of that mismatch is fixed at whatever Room 2.8.4 itself
    // was built against — a future dependency bump could reintroduce it
    // just as easily. Schema-history tracking was a nice-to-have, not
    // something the app depends on to function, so removing it entirely
    // is the robust fix rather than chasing this upstream landmine.
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CairnDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao

    /**
     * PRAGMA/VACUUM aren't statement types Room's compiler can classify as
     * SELECT/INSERT/UPDATE/DELETE, so they can't be @Query DAO methods —
     * that's what broke KSP codegen originally. Running them directly
     * against the underlying SupportSQLiteDatabase is the correct pattern
     * for maintenance statements like these. Both are blocking calls —
     * callers (CallLogRepository) dispatch them on Dispatchers.IO.
     */
    fun runIntegrityCheckBlocking(): Boolean {
        openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
            return cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
        }
    }

    fun vacuumBlocking() {
        openHelper.writableDatabase.execSQL("VACUUM")
    }

    companion object {
        const val DB_NAME = "cairn_encrypted.db"

        @Volatile private var instance: CairnDatabase? = null

        fun getInstance(context: Context, keyManager: DbKeyManager): CairnDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, keyManager).also { instance = it }
            }

        private fun build(context: Context, keyManager: DbKeyManager): CairnDatabase {
            // Loads the native SQLCipher libs; safe to call repeatedly.
            // Uses net.zetetic:sqlcipher-android (SupportOpenHelperFactory), the
            // actively-maintained successor to the now-deprecated
            // net.zetetic:android-database-sqlcipher / SupportFactory — see
            // https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/
            System.loadLibrary("sqlcipher")

            val passphrase = keyManager.getOrCreatePassphrase()
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(context, CairnDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // WAL improves concurrent read/write throughput at 500K+ rows
                        db.execSQL("PRAGMA journal_mode=WAL")
                        db.execSQL("PRAGMA synchronous=NORMAL")
                    }
                })
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }

        /**
         * Every schema change ships an explicit migration — never
         * fallbackToDestructiveMigration(), since this is a 10-year archive
         * and data loss on upgrade is unacceptable.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Placeholder for the first real schema change, e.g.:
                // db.execSQL("ALTER TABLE call_logs ADD COLUMN isSpamReported INTEGER NOT NULL DEFAULT 0")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
