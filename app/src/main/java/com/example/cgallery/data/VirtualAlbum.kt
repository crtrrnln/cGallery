package com.example.cgallery.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "physical_albums")
data class PhysicalAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketName: String,
    val isHidden: Boolean = false,
    val groupId: Long? = null
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

    @Query("SELECT * FROM physical_albums WHERE groupId = :groupId")
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

@Database(
    entities = [
        PhysicalAlbumEntity::class,
        AlbumGroupEntity::class
    ],
    version = 4
)
abstract class VirtualAlbumDatabase : RoomDatabase() {
    abstract fun physicalAlbumDao(): PhysicalAlbumDao
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
