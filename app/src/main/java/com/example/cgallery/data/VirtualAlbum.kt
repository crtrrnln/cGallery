package com.example.cgallery.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long? = null
)

@Entity(tableName = "album_groups")
data class AlbumGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "album_media",
    primaryKeys = ["albumId", "mediaId"]
)
data class AlbumMediaCrossRef(
    val albumId: Long,
    val mediaId: Long
)

data class AlbumMediaId(
    val albumId: Long,
    val mediaId: Long
)

data class AlbumWithMedia(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "albumId",
        entity = AlbumMediaCrossRef::class
    )
    val mediaItems: List<AlbumMediaCrossRef>
) {
    val mediaIds: List<Long> get() = mediaItems.map { it.mediaId }
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaToAlbum(crossRef: AlbumMediaCrossRef)

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumWithMedia(albumId: Long): AlbumWithMedia?

    @Transaction
    @Query("SELECT * FROM albums")
    fun getAllAlbumsWithMedia(): Flow<List<AlbumWithMedia>>

    @Query("SELECT * FROM albums WHERE groupId = :groupId")
    fun getAlbumsByGroup(groupId: Long?): Flow<List<AlbumEntity>>

    @Query("UPDATE albums SET groupId = :groupId WHERE id = :albumId")
    suspend fun moveAlbumToGroup(albumId: Long, groupId: Long?)
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

@Database(
    entities = [
        AlbumEntity::class,
        AlbumGroupEntity::class,
        AlbumMediaCrossRef::class
    ],
    version = 2
)
abstract class VirtualAlbumDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun albumGroupDao(): AlbumGroupDao

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
