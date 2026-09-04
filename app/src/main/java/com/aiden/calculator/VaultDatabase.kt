package com.aiden.calculator

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_items", primaryKeys = ["id"])
data class VaultItemEntity(
    val id: String,
    val vaultId: VaultId,
    val blobName: String,
    val type: VaultItemType,
    val encryptedName: ByteArray,
    val encryptedMime: ByteArray,
    val encryptedThumbnail: ByteArray?,
    val size: Long,
    val plainSize: Long?,
    val trashState: TrashState,
    val createdAt: Long,
)

fun VaultItem.toEntity() = VaultItemEntity(
    id, vaultId, blobName, type, encryptedName, encryptedMime, encryptedThumbnail, size, plainSize, trashState, createdAt,
)

fun VaultItemEntity.toModel() = VaultItem(
    id, vaultId, blobName, type, encryptedName, encryptedMime, encryptedThumbnail, size, plainSize, trashState, createdAt,
)

data class VaultItemRow(
    val id: String,
    val blobName: String,
    val type: VaultItemType,
    val encryptedName: ByteArray,
    val encryptedMime: ByteArray,
    val encryptedThumbnail: ByteArray?,
    val size: Long,
    val plainSize: Long?,
    val trashState: TrashState,
    val createdAt: Long,
)

fun VaultItemEntity.toRow() = VaultItemRow(
    id, blobName, type, encryptedName, encryptedMime, encryptedThumbnail, size, plainSize, trashState, createdAt,
)

fun VaultItemRow.toModel(vaultId: VaultId) = VaultItem(
    id, vaultId, blobName, type, encryptedName, encryptedMime, encryptedThumbnail, size, plainSize, trashState, createdAt,
)

class VaultConverters {
    @TypeConverter fun vaultToString(value: VaultId) = value.name
    @TypeConverter fun stringToVault(value: String) = VaultId.valueOf(value)
    @TypeConverter fun typeToString(value: VaultItemType) = value.name
    @TypeConverter
    fun stringToType(value: String?) =
        value?.let { runCatching { VaultItemType.valueOf(it) }.getOrNull() } ?: VaultItemType.FILE

    @TypeConverter fun trashToString(value: TrashState) = value.name
    @TypeConverter
    fun stringToTrash(value: String?) =
        value?.let { runCatching { TrashState.valueOf(it) }.getOrNull() } ?: TrashState.ACTIVE
}

@Dao
interface VaultItemDao {
    @Query(
        """
        SELECT id, blobName, type, X'' AS encryptedName, X'' AS encryptedMime,
            NULL AS encryptedThumbnail, size, plainSize, trashState, createdAt
        FROM vault_items
        WHERE vaultId = :vaultId
        ORDER BY createdAt DESC
        LIMIT 1000
        """,
    )
    fun observe(vaultId: VaultId): Flow<List<VaultItemRow>>

    @Query(
        """
        SELECT id, blobName, type, encryptedName, encryptedMime,
            encryptedThumbnail, size, plainSize, trashState, createdAt
        FROM vault_items
        WHERE id = :id AND vaultId = :vaultId
        """,
    )
    suspend fun get(id: String, vaultId: VaultId): VaultItemRow?

    @Query("SELECT encryptedThumbnail FROM vault_items WHERE id = :id AND vaultId = :vaultId")
    suspend fun thumbnail(id: String, vaultId: VaultId): ByteArray?

    @Query("SELECT encryptedName FROM vault_items WHERE id = :id AND vaultId = :vaultId")
    suspend fun encryptedName(id: String, vaultId: VaultId): ByteArray?

    @Query("SELECT encryptedMime FROM vault_items WHERE id = :id AND vaultId = :vaultId")
    suspend fun encryptedMime(id: String, vaultId: VaultId): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItemEntity)

    @Query("UPDATE vault_items SET trashState = :state WHERE id IN (:ids) AND vaultId = :vaultId")
    suspend fun setTrash(ids: List<String>, vaultId: VaultId, state: TrashState)

    @Query("UPDATE vault_items SET plainSize = :plainSize WHERE id = :id AND vaultId = :vaultId")
    suspend fun updatePlainSize(id: String, vaultId: VaultId, plainSize: Long)

    @Query("DELETE FROM vault_items WHERE id = :id AND vaultId = :vaultId")
    suspend fun delete(id: String, vaultId: VaultId)

    @Query(
        """
        SELECT id, blobName, type, encryptedName, encryptedMime,
            encryptedThumbnail, size, plainSize, trashState, createdAt
        FROM vault_items
        WHERE vaultId = :vaultId
        """,
    )
    suspend fun all(vaultId: VaultId): List<VaultItemRow>

    @Query(
        """
        SELECT id, blobName, type, encryptedName, encryptedMime,
            encryptedThumbnail, size, plainSize, trashState, createdAt
        FROM vault_items
        WHERE vaultId = :vaultId AND id IN (:ids)
        """,
    )
    suspend fun byIds(vaultId: VaultId, ids: List<String>): List<VaultItemRow>

    @Query("DELETE FROM vault_items WHERE vaultId = :vaultId")
    suspend fun deleteVault(vaultId: VaultId)
}

@Database(entities = [VaultItemEntity::class], version = 5, exportSchema = false)
@TypeConverters(VaultConverters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun items(): VaultItemDao

    companion object {
        fun deleteCorruptRows(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                DELETE FROM vault_items
                WHERE id IS NULL
                    OR vaultId IS NULL OR vaultId NOT IN ('ONE', 'TWO')
                    OR blobName IS NULL
                    OR type IS NULL OR type NOT IN ('PHOTO', 'VIDEO', 'FILE')
                    OR encryptedName IS NULL
                    OR length(encryptedName) > 8192
                    OR encryptedMime IS NULL
                    OR length(encryptedMime) > 2048
                    OR size IS NULL
                    OR trashState IS NULL OR trashState NOT IN ('ACTIVE', 'TRASHED')
                    OR createdAt IS NULL
                """.trimIndent(),
            )
            database.execSQL(
                """
                UPDATE vault_items
                SET encryptedThumbnail = NULL
                WHERE encryptedThumbnail IS NOT NULL
                    AND length(encryptedThumbnail) > 1048576
                """.trimIndent(),
            )
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vault_items ADD COLUMN plainSize INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vault_items_repaired (
                        id TEXT NOT NULL,
                        vaultId TEXT NOT NULL,
                        blobName TEXT NOT NULL,
                        type TEXT NOT NULL,
                        encryptedName BLOB NOT NULL,
                        encryptedMime BLOB NOT NULL,
                        encryptedThumbnail BLOB,
                        size INTEGER NOT NULL,
                        plainSize INTEGER,
                        trashState TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO vault_items_repaired (
                        id, vaultId, blobName, type, encryptedName, encryptedMime,
                        encryptedThumbnail, size, plainSize, trashState, createdAt
                    )
                    SELECT
                        id, vaultId, blobName, type, encryptedName, encryptedMime,
                        encryptedThumbnail, size, plainSize, trashState, createdAt
                    FROM vault_items
                    WHERE id IS NOT NULL
                        AND vaultId IN ('ONE', 'TWO')
                        AND blobName IS NOT NULL
                        AND type IN ('PHOTO', 'VIDEO', 'FILE')
                        AND encryptedName IS NOT NULL
                        AND encryptedMime IS NOT NULL
                        AND size IS NOT NULL
                        AND trashState IN ('ACTIVE', 'TRASHED')
                        AND createdAt IS NOT NULL
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE vault_items")
                database.execSQL("ALTER TABLE vault_items_repaired RENAME TO vault_items")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rows without a valid vault cannot be safely attributed after an old/corrupt install.
                deleteCorruptRows(database)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                deleteCorruptRows(database)
            }
        }
    }
}
