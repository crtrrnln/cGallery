package com.example.cgallery.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "samsung_albums")
data class SamsungAlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val path: String,
    val folderId: Long?,
    val coverPath: String,
    val coverCrop: String?,
    val sortPrimary: Long,
    val sortSecondary: Long
)

@Entity(tableName = "samsung_folders")
data class SamsungFolderEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val parentId: Long?
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
}

@Dao
interface SamsungAlbumDao {
    @Query("SELECT * FROM samsung_albums ORDER BY sortPrimary, sortSecondary")
    fun getAllAlbums(): Flow<List<SamsungAlbumEntity>>

    @Query("SELECT * FROM samsung_albums WHERE folderId IS NULL ORDER BY sortPrimary, sortSecondary")
    fun getOrphanAlbums(): Flow<List<SamsungAlbumEntity>>

    @Query("SELECT * FROM samsung_albums WHERE folderId = :folderId ORDER BY sortPrimary, sortSecondary")
    fun getAlbumsByFolder(folderId: Long): Flow<List<SamsungAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: SamsungAlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<SamsungAlbumEntity>)

    @Query("DELETE FROM samsung_albums")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM samsung_albums WHERE id = :id")
    suspend fun deleteAlbum(id: Long)

    @Query("UPDATE samsung_albums SET folderId = :folderId WHERE id = :albumId")
    suspend fun moveAlbum(albumId: Long, folderId: Long?)
}

@Dao
interface SamsungFolderDao {
    @Query("SELECT * FROM samsung_folders ORDER BY name")
    fun getAllFolders(): Flow<List<SamsungFolderEntity>>

    @Query("SELECT * FROM samsung_folders WHERE parentId IS NULL ORDER BY name")
    fun getRootFolders(): Flow<List<SamsungFolderEntity>>

    @Query("SELECT * FROM samsung_folders WHERE parentId = :parentId ORDER BY name")
    fun getChildFolders(parentId: Long): Flow<List<SamsungFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: SamsungFolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<SamsungFolderEntity>)

    @Query("UPDATE samsung_folders SET parentId = :parentId WHERE id = :folderId")
    suspend fun moveFolder(folderId: Long, parentId: Long?)

    @Query("UPDATE samsung_folders SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, name: String)

    @Query("DELETE FROM samsung_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("DELETE FROM samsung_folders")
    suspend fun deleteAllFolders()
}

@Database(
    entities = [
        AlbumEntity::class,
        AlbumMediaCrossRef::class,
        SamsungAlbumEntity::class,
        SamsungFolderEntity::class
    ],
    version = 2
)
abstract class VirtualAlbumDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun samsungAlbumDao(): SamsungAlbumDao
    abstract fun samsungFolderDao(): SamsungFolderDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS samsung_albums (
                        id INTEGER PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        path TEXT NOT NULL,
                        folderId INTEGER,
                        coverPath TEXT NOT NULL,
                        coverCrop TEXT,
                        sortPrimary INTEGER NOT NULL,
                        sortSecondary INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS samsung_folders (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        parentId INTEGER
                    )
                """)
            }
        }
    }
}
