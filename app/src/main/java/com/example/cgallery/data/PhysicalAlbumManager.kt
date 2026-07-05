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
    private val favoritesManager = FavoritesManager(context)
    private val context = context
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    val allAlbums: Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAllAlbums()

    suspend fun syncAlbums(bucketNames: List<String>) {
        val existingAlbums = physicalAlbumDao.getAllAlbums().first()
        val existingBucketNames = existingAlbums.map { it.bucketName }.toSet()
        val maxSortOrder = existingAlbums.maxOfOrNull { it.sortOrder } ?: -1
        bucketNames.forEachIndexed { index, bucketName ->
            if (!existingBucketNames.contains(bucketName)) {
                physicalAlbumDao.insertAlbum(PhysicalAlbumEntity(bucketName = bucketName, isHidden = false, groupId = null, sortOrder = maxSortOrder + 1 + index))
            }
        }
        existingAlbums.forEach { album ->
            if (!bucketNames.contains(album.bucketName)) {
                val file = File(album.bucketName)
                if (!file.exists() || !file.isDirectory) physicalAlbumDao.deleteAlbum(album)
            }
        }
    }
    suspend fun toggleAlbumVisibility(bucketName: String) {
        val album = physicalAlbumDao.getAlbumByBucketName(bucketName).first()
        if (album != null) physicalAlbumDao.updateAlbumVisibility(bucketName, !album.isHidden)
    }
    suspend fun moveAlbumToGroup(bucketName: String, groupId: Long?) = physicalAlbumDao.moveAlbumToGroup(bucketName, groupId)
    suspend fun updateAlbumSortOrder(albumId: Long, sortOrder: Int) = physicalAlbumDao.updateAlbumSortOrder(albumId, sortOrder)
    fun getAlbumsByGroup(groupId: Long?): Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAlbumsByGroup(groupId)

    suspend fun createFolder(folderName: String, parentPath: String? = null, groupId: Long? = null): Result<String> {
        return try {
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

    suspend fun moveFile(src: String, target: String): Result<String> {
        return try {
            val sf = File(src); val tf = File(target)
            if (!sf.exists()) return Result.failure(Exception("no src"))
            if (!tf.exists()) tf.mkdirs()
            val targetFile = File(tf, sf.name)
            if (targetFile.exists()) return Result.failure(Exception("exists"))
            if (sf.renameTo(targetFile)) { Result.success(targetFile.absolutePath) } else {
                sf.copyTo(targetFile, overwrite = false)
                if (sf.delete()) { Result.success(targetFile.absolutePath) } else {
                    targetFile.delete()
                    Result.failure(Exception("fail delete"))
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun copyFile(src: String, target: String): Result<String> {
        return try {
            val sf = File(src); val tf = File(target)
            if (!sf.exists()) return Result.failure(Exception("no src"))
            if (!tf.exists()) tf.mkdirs()
            val targetFile = File(tf, sf.name)
            if (targetFile.exists()) return Result.failure(Exception("exists"))
            sf.copyTo(targetFile, overwrite = false)
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) { Result.failure(e) }
    }

    @Serializable
    data class StructureExport(
        val groups: List<AlbumGroupEntity> = emptyList(),
        val albums: List<PhysicalAlbumEntity> = emptyList(),
        val monitoredFolders: List<MonitoredFolderEntity> = emptyList(),
        val favorites: Set<Long> = emptySet()
    )

    suspend fun exportStructure(): String = withContext(Dispatchers.IO) {
        val groups = groupDao.getAllGroups().first()
        val albums = physicalAlbumDao.getAllAlbums().first()
        val folders = folderDao.getAllFolders().first()
        val favs = favoritesManager.favoriteIds.first()
        json.encodeToString(StructureExport(groups, albums, folders, favs))
    }

    suspend fun importStructure(jsonStr: String) = withContext(Dispatchers.IO) {
        try {
            val data = json.decodeFromString<StructureExport>(jsonStr)
            groupDao.getAllGroups().first().forEach { groupDao.deleteGroup(it) }
            val groupMap = mutableMapOf<Long, Long>()
            data.groups.forEach { g -> groupMap[g.id] = groupDao.insertGroup(g.copy(id = 0)) }
            data.groups.forEach { g ->
                if (g.parentId != null) {
                    val newId = groupMap[g.id]; val newParent = groupMap[g.parentId]
                    if (newId != null && newParent != null) {
                        groupDao.getGroupById(newId).first()?.let { groupDao.updateGroup(it.copy(parentId = newParent)) }
                    }
                }
            }
            data.albums.forEach { alb ->
                val existing = physicalAlbumDao.getAlbumByBucketName(alb.bucketName).first()
                val newGid = alb.groupId?.let { groupMap[it] }
                if (existing != null) {
                    physicalAlbumDao.updateAlbum(existing.copy(groupId = newGid, sortOrder = alb.sortOrder, isHidden = alb.isHidden, customCoverUri = alb.customCoverUri, customCoverCrop = alb.customCoverCrop))
                } else {
                    val f = File(alb.bucketName); if (!f.exists()) f.mkdirs()
                    physicalAlbumDao.insertAlbum(alb.copy(id = 0, groupId = newGid))
                    MediaScannerConnection.scanFile(context, arrayOf(alb.bucketName), null, null)
                }
            }
            data.monitoredFolders.forEach { folderDao.insertFolder(it.copy(id = 0)) }
            data.favorites.forEach { favoritesManager.addFavorite(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun updateAlbumCover(b: String, u: String?, c: String?) = physicalAlbumDao.updateAlbumCover(b, u, c)
    suspend fun updateGroupCover(id: Long, u: String?, c: String?) = groupDao.updateGroupCover(id, u, c)
}
