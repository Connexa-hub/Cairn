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
import net.sqlcipher.database.SupportFactory

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
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CairnDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        const val DB_NAME = "cairn_encrypted.db"

        @Volatile private var instance: CairnDatabase? = null

        fun getInstance(context: Context, keyManager: DbKeyManager): CairnDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, keyManager).also { instance = it }
            }

        private fun build(context: Context, keyManager: DbKeyManager): CairnDatabase {
            // Loads the native SQLCipher libs; safe to call repeatedly.
            System.loadLibrary("sqlcipher")

            val passphrase = keyManager.getOrCreatePassphrase()
            val factory = SupportFactory(passphrase)

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
