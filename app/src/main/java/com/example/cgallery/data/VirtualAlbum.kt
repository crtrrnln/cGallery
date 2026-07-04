package com.example.cgallery.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "physical_albums", indices = [Index(value = ["bucketName"], unique = true)])
data class PhysicalAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketName: String,
    val isHidden: Boolean = false,
    val groupId: Long? = null,
    val sortOrder: Int = 0
)

@Entity(tableName = "album_groups")
data class AlbumGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0
)

@Dao
interface PhysicalAlbumDao {
    @Query("SELECT * FROM physical_albums")
    fun getAllAlbums(): Flow<List<PhysicalAlbumEntity>>

    @Query("SELECT * FROM physical_albums WHERE bucketName = :bucketName")
    fun getAlbumByBucketName(bucketName: String): Flow<PhysicalAlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: PhysicalAlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: PhysicalAlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: PhysicalAlbumEntity)

    @Query("UPDATE physical_albums SET isHidden = :isHidden WHERE bucketName = :bucketName")
    suspend fun updateAlbumVisibility(bucketName: String, isHidden: Boolean)

    @Query("UPDATE physical_albums SET groupId = :groupId WHERE bucketName = :bucketName")
    suspend fun moveAlbumToGroup(bucketName: String, groupId: Long?)

    @Query("UPDATE physical_albums SET sortOrder = :sortOrder WHERE id = :albumId")
    suspend fun updateAlbumSortOrder(albumId: Long, sortOrder: Int)

    @Query("SELECT * FROM physical_albums WHERE groupId = :groupId ORDER BY sortOrder")
    fun getAlbumsByGroup(groupId: Long?): Flow<List<PhysicalAlbumEntity>>
}

@Dao
interface AlbumGroupDao {
    @Query("SELECT * FROM album_groups ORDER BY sortOrder")
    fun getAllGroups(): Flow<List<AlbumGroupEntity>>

    @Query("SELECT * FROM album_groups WHERE id = :groupId")
    fun getGroupById(groupId: Long): Flow<AlbumGroupEntity?>

    @Query("SELECT * FROM album_groups WHERE parentId IS NULL ORDER BY sortOrder")
    fun getRootGroups(): Flow<List<AlbumGroupEntity>>

    @Query("SELECT * FROM album_groups WHERE parentId = :parentId ORDER BY sortOrder")
    fun getChildGroups(parentId: Long): Flow<List<AlbumGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AlbumGroupEntity): Long

    @Update
    suspend fun updateGroup(group: AlbumGroupEntity)

    @Delete
    suspend fun deleteGroup(group: AlbumGroupEntity)

    @Query("UPDATE album_groups SET parentId = :parentId WHERE id = :groupId")
    suspend fun moveGroup(groupId: Long, parentId: Long?)

    @Query("UPDATE album_groups SET sortOrder = :sortOrder WHERE id = :groupId")
    suspend fun updateGroupSortOrder(groupId: Long, sortOrder: Int)
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items WHERE status = 'Pending' ORDER BY detectedTimestamp DESC")
    fun getPendingItems(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE status = 'Completed' ORDER BY processingTimestamp DESC")
    fun getCompletedItems(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE mediaStoreId = :mediaStoreId")
    suspend fun getItemByMediaStoreId(mediaStoreId: Long): InboxItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: InboxItemEntity): Long

    @Update
    suspend fun updateItem(item: InboxItemEntity)

    @Query("UPDATE inbox_items SET status = :status, processingTimestamp = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: InboxStatus, timestamp: Long)

    @Delete
    suspend fun deleteItem(item: InboxItemEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM inbox_items WHERE mediaStoreId = :mediaStoreId)")
    suspend fun exists(mediaStoreId: Long): Boolean
}

@Dao
interface MonitoredFolderDao {
    @Query("SELECT * FROM monitored_folders")
    fun getAllFolders(): Flow<List<MonitoredFolderEntity>>

    @Query("SELECT * FROM monitored_folders WHERE isEnabled = 1")
    suspend fun getEnabledFoldersSync(): List<MonitoredFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: MonitoredFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: MonitoredFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: MonitoredFolderEntity)
}

@Dao
interface InboxStatsDao {
    @Query("SELECT * FROM inbox_stats WHERE id = 1")
    fun getStats(): Flow<InboxStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: InboxStatsEntity)
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)
    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)

    @TypeConverter
    fun fromMap(value: Map<String, Int>) = Json.encodeToString(value)
    @TypeConverter
    fun toMap(value: String) = Json.decodeFromString<Map<String, Int>>(value)

    @TypeConverter
    fun fromStatus(value: InboxStatus) = value.name
    @TypeConverter
    fun toStatus(value: String) = InboxStatus.valueOf(value)

    @TypeConverter
    fun fromOperation(value: InboxOperation) = value.name
    @TypeConverter
    fun toOperation(value: String) = InboxOperation.valueOf(value)
}

@Database(
    entities = [
        PhysicalAlbumEntity::class,
        AlbumGroupEntity::class,
        InboxItemEntity::class,
        MonitoredFolderEntity::class,
        InboxStatsEntity::class
    ],
    version = 7
)
@TypeConverters(Converters::class)
abstract class VirtualAlbumDatabase : RoomDatabase() {
    abstract fun physicalAlbumDao(): PhysicalAlbumDao
    abstract fun albumGroupDao(): AlbumGroupDao
    abstract fun inboxDao(): InboxDao
    abstract fun monitoredFolderDao(): MonitoredFolderDao
    abstract fun inboxStatsDao(): InboxStatsDao

    companion object {
        @Volatile
        private var INSTANCE: VirtualAlbumDatabase? = null

        fun getDatabase(context: android.content.Context): VirtualAlbumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VirtualAlbumDatabase::class.java,
                    "virtual_albums.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
