package com.example.cgallery.data
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
@Entity(tableName = "physical_albums", indices = [Index(value = ["bucketName"], unique = true)])
data class PhysicalAlbumEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val bucketName: String, val isHidden: Boolean = false, val groupId: Long? = null, val sortOrder: Int = 0, val customCoverUri: String? = null, val customCoverCrop: String? = null)

@Serializable
@Entity(tableName = "album_groups")
data class AlbumGroupEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val parentId: Long? = null, val sortOrder: Int = 0, val customCoverUri: String? = null, val customCoverCrop: String? = null)

@Dao
interface PhysicalAlbumDao {
    @Query("SELECT * FROM physical_albums ORDER BY sortOrder")
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
    @Query("UPDATE physical_albums SET customCoverUri = :uri, customCoverCrop = :crop WHERE bucketName = :bucketName")
    suspend fun updateAlbumCover(bucketName: String, uri: String?, crop: String?)
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
    @Query("UPDATE album_groups SET customCoverUri = :uri, customCoverCrop = :crop WHERE id = :groupId")
    suspend fun updateGroupCover(groupId: Long, uri: String?, crop: String?)
}

@Dao
interface InboxOperationDao {
    @Query("SELECT * FROM inbox_operations WHERE status = 'Queued' OR status = 'Failed' ORDER BY createdAt ASC")
    fun getPendingOperations(): Flow<List<InboxOperationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: InboxOperationEntity): Long
    @Update
    suspend fun updateOperation(operation: InboxOperationEntity)
    @Query("SELECT * FROM inbox_operations WHERE id = :id")
    suspend fun getOperationById(id: Long): InboxOperationEntity?
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items WHERE status IN ('Detected', 'Queued', 'Processing', 'Verifying', 'WaitingForUser', 'RetryPending') ORDER BY detectedTimestamp DESC")
    fun getPendingItems(): Flow<List<InboxItemEntity>>
    @Query("SELECT * FROM inbox_items ORDER BY detectedTimestamp DESC")
    fun getAllItems(): Flow<List<InboxItemEntity>>
    @Query("SELECT * FROM inbox_items WHERE status = 'Completed' ORDER BY processingTimestamp DESC")
    fun getCompletedItems(): Flow<List<InboxItemEntity>>
    @Query("SELECT * FROM inbox_items WHERE id = :id")
    suspend fun getItemById(id: Long): InboxItemEntity?
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
    fun fromList(v: List<String>) = Json.encodeToString(v)
    @TypeConverter
    fun toList(v: String) = Json.decodeFromString<List<String>>(v)
    @TypeConverter
    fun fromMap(v: Map<String, Int>) = Json.encodeToString(v)
    @TypeConverter
    fun toMap(v: String) = Json.decodeFromString<Map<String, Int>>(v)
    @TypeConverter
    fun fromStatus(v: InboxStatus) = v.name
    @TypeConverter
    fun toStatus(v: String) = InboxStatus.valueOf(v)
    @TypeConverter
    fun fromOperation(v: InboxOperationType) = v.name
    @TypeConverter
    fun toOperation(v: String) = InboxOperationType.valueOf(v)
    @TypeConverter
    fun fromOpStatus(v: OperationStatus) = v.name
    @TypeConverter
    fun toOpStatus(v: String) = OperationStatus.valueOf(v)
}

@Database(entities = [PhysicalAlbumEntity::class, AlbumGroupEntity::class, InboxItemEntity::class, MonitoredFolderEntity::class, InboxStatsEntity::class, InboxOperationEntity::class], version = 9)
@TypeConverters(Converters::class)
abstract class VirtualAlbumDatabase : RoomDatabase() {
    abstract fun physicalAlbumDao(): PhysicalAlbumDao
    abstract fun albumGroupDao(): AlbumGroupDao
    abstract fun inboxDao(): InboxDao
    abstract fun monitoredFolderDao(): MonitoredFolderDao
    abstract fun inboxStatsDao(): InboxStatsDao
    abstract fun inboxOperationDao(): InboxOperationDao
    companion object {
        @Volatile private var INSTANCE: VirtualAlbumDatabase? = null
        fun getDatabase(ctx: android.content.Context): VirtualAlbumDatabase {
            return INSTANCE ?: synchronized(this) { Room.databaseBuilder(ctx.applicationContext, VirtualAlbumDatabase::class.java, "virtual_albums.db").fallbackToDestructiveMigration().build().also { INSTANCE = it } }
        }
    }
}
