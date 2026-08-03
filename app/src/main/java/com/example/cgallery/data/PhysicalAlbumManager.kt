package com.example.cgallery.data
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhysicalAlbumManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val physicalAlbumDao = db.physicalAlbumDao()
    private val groupDao = db.albumGroupDao()
    private val folderDao = db.monitoredFolderDao()
    private val statsDao = db.inboxStatsDao()
    private val favouritesManager = FavouritesManager(context)
    private val settingsRepo = AppSettingsRepository(context)
    private val context = context
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    val allAlbums: Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAllAlbums()

    suspend fun syncAlbums(bucketNames: List<String>) = withContext(Dispatchers.IO) {
        try {
            val existingAlbums = physicalAlbumDao.getAllAlbums().first()
            val existingMap = existingAlbums.associateBy { it.bucketName }
            val newBuckets = bucketNames.toSet()
            
            val toInsert = mutableListOf<PhysicalAlbumEntity>()
            var maxSort = existingAlbums.maxOfOrNull { it.sortOrder } ?: -1
            
            bucketNames.distinct().forEach { bucket ->
                if (!existingMap.containsKey(bucket)) {
                    maxSort++
                    toInsert.add(PhysicalAlbumEntity(bucketName = bucket, isHidden = false, groupId = null, sortOrder = maxSort))
                }
            }
            
            if (toInsert.isNotEmpty()) {
                toInsert.forEach { physicalAlbumDao.insertAlbum(it) }
            }
            
            existingAlbums.forEach { album ->
                if (!newBuckets.contains(album.bucketName)) {
                    // Keep the album if the directory still exists on disk, even if empty or not in MediaStore.
                    // This prevents newly created empty albums from being deleted before use.
                    val f = File(album.bucketName)
                    if (!f.exists()) {
                         physicalAlbumDao.deleteAlbum(album)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash - sync can retry next time
            android.util.Log.e("PhysicalAlbumManager", "Error syncing albums", e)
        }
    }

    suspend fun toggleAlbumVisibility(bucketName: String) {
        val album = physicalAlbumDao.getAlbumByBucketName(bucketName).first()
        if (album != null) physicalAlbumDao.updateAlbumVisibility(bucketName, !album.isHidden)
    }
    suspend fun moveAlbumToGroup(bucketName: String, groupId: Long?) = physicalAlbumDao.moveAlbumToGroup(bucketName, groupId)
    suspend fun deleteAlbum(bucketName: String) {
        physicalAlbumDao.getAlbumByBucketName(bucketName).first()?.let { physicalAlbumDao.deleteAlbum(it) }
    }
    suspend fun updateAlbumSortOrder(albumId: Long, sortOrder: Int) = physicalAlbumDao.updateAlbumSortOrder(albumId, sortOrder)
    fun getAlbumsByGroup(groupId: Long?): Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAlbumsByGroup(groupId)

    suspend fun createFolder(folderName: String, parentPath: String? = null, groupId: Long? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parentDir = if (parentPath != null) File(parentPath) else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!parentDir.exists()) parentDir.mkdirs()
            val newFolder = File(parentDir, folderName)
            if (newFolder.exists()) { Result.failure(Exception("exists already")) } else {
                if (newFolder.mkdirs()) {
                    val maxSort = physicalAlbumDao.getAllAlbums().first().maxOfOrNull { it.sortOrder } ?: -1
                    physicalAlbumDao.insertAlbum(PhysicalAlbumEntity(bucketName = newFolder.absolutePath, isHidden = false, groupId = groupId, sortOrder = maxSort + 1))
                    MediaScannerConnection.scanFile(context, arrayOf(newFolder.absolutePath), null, null)
                    Result.success(newFolder.absolutePath)
                } else { Result.failure(Exception("failed")) }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun moveFile(src: String, target: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sf = File(src)
            val tf = File(target)
            
            if (!sf.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist"))
            }
            
            if (!tf.exists()) {
                if (!tf.mkdirs()) {
                    return@withContext Result.failure(Exception("Failed to create target directory"))
                }
            }
            
            val targetFile = nextFreeFile(tf, sf.name)
            
            if (sf.renameTo(targetFile)) {
                Result.success(targetFile.absolutePath)
            } else {
                // Try copy + delete as fallback
                sf.copyTo(targetFile, overwrite = false)
                if (sf.delete()) {
                    Result.success(targetFile.absolutePath)
                } else {
                    targetFile.delete()
                    Result.failure(Exception("Failed to delete source file after copy"))
                }
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("File operation failed: ${e.message}"))
        }
    }

    suspend fun copyFile(src: String, target: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sf = File(src)
            val tf = File(target)
            
            if (!sf.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist"))
            }
            
            if (!tf.exists()) {
                if (!tf.mkdirs()) {
                    return@withContext Result.failure(Exception("Failed to create target directory"))
                }
            }
            
            val targetFile = nextFreeFile(tf, sf.name)
            sf.copyTo(targetFile, overwrite = false)
            Result.success(targetFile.absolutePath)
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("File operation failed: ${e.message}"))
        }
    }

    private fun nextFreeFile(dir: File, name: String): File {
        var f = File(dir, name); if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (f.exists()) { f = File(dir, "$base ($i)$ext"); i++ }
        return f
    }
    @Serializable
    data class StructureExport(
        val version: Int = 2,
        val exportedAt: Long = System.currentTimeMillis(),
        val settings: AppSettings? = null,
        val groups: List<AlbumGroupEntity> = emptyList(),
        val albums: List<PhysicalAlbumEntity> = emptyList(),
        val monitoredFolders: List<MonitoredFolderEntity> = emptyList(),
        val favourites: Set<Long> = emptySet(),
        val favouriteCoverUri: String? = null,
        val favouriteCoverCrop: String? = null,
        val stats: InboxStatsEntity? = null
    )

    suspend fun exportStructure(): String = withContext(Dispatchers.IO) {
        val groups = groupDao.getAllGroups().first()
        val albums = physicalAlbumDao.getAllAlbums().first()
        val folders = folderDao.getAllFolders().first()
        val favs = favouritesManager.favouriteIds.first()
        val favCover = favouritesManager.favouriteCover.first()
        val settings = settingsRepo.settingsFlow.first()
        val stats = statsDao.getStats().first()
        json.encodeToString(StructureExport(
            settings = settings,
            groups = groups,
            albums = albums,
            monitoredFolders = folders,
            favourites = favs,
            favouriteCoverUri = favCover.first,
            favouriteCoverCrop = favCover.second,
            stats = stats
        ))
    }

    suspend fun importStructure(jsonStr: String) = withContext(Dispatchers.IO) {
        try {
            val data = json.decodeFromString<StructureExport>(jsonStr)
            
            // Restore Settings if present (Version 2+)
            data.settings?.let { settingsRepo.applyImportedSettings(it) }

            // Clear and Restore Group Structure
            physicalAlbumDao.resetAllGroupLinks()
            groupDao.getAllGroups().first().forEach { groupDao.deleteGroup(it) }
            val groupMap = mutableMapOf<Long, Long>()
            data.groups.sortedBy { if (it.parentId == null) 0 else 1 }.forEach { g -> 
                groupMap[g.id] = groupDao.insertGroup(g.copy(id = 0, parentId = null)) 
            }
            // Second pass for parent links
            data.groups.forEach { g ->
                if (g.parentId != null) {
                    val newId = groupMap[g.id]; val newParent = groupMap[g.parentId]
                    if (newId != null && newParent != null) {
                        groupDao.getGroupById(newId).first()?.let { groupDao.updateGroup(it.copy(parentId = newParent)) }
                    }
                }
            }

            // Restore Albums with mapped Group IDs - Merging with physical reality
            data.albums.forEach { alb ->
                val existing = physicalAlbumDao.getAlbumByBucketName(alb.bucketName).first()
                val newGid = alb.groupId?.let { groupMap[it] }
                if (existing != null) {
                    physicalAlbumDao.updateAlbum(existing.copy(groupId = newGid, sortOrder = alb.sortOrder, isHidden = alb.isHidden, customCoverUri = alb.customCoverUri, customCoverCrop = alb.customCoverCrop))
                } else {
                    // Only restore the entry if the folder actually exists on disk.
                    // We don't want to create ghost folders (mkdirs) for deleted content.
                    val f = File(alb.bucketName)
                    if (f.exists()) {
                        physicalAlbumDao.insertAlbum(alb.copy(id = 0, groupId = newGid))
                        MediaScannerConnection.scanFile(context, arrayOf(alb.bucketName), null, null)
                    }
                }
            }

            // Restore Monitored Folders (Replace existing)
            folderDao.getAllFolders().first().forEach { folderDao.deleteFolder(it) }
            data.monitoredFolders.forEach { folderDao.insertFolder(it.copy(id = 0)) }

            // Restore Favourites
            favouritesManager.replaceFavourites(data.favourites, data.favouriteCoverUri, data.favouriteCoverCrop)
            
            // Restore Stats
            data.stats?.let { statsDao.updateStats(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun updateAlbumCover(b: String, u: String?, c: String?) = physicalAlbumDao.updateAlbumCover(b, u, c)
    suspend fun updateGroupCover(id: Long, u: String?, c: String?) = groupDao.updateGroupCover(id, u, c)
    suspend fun updateGroupSortOrder(id: Long, sortOrder: Int) = groupDao.updateGroupSortOrder(id, sortOrder)
}

