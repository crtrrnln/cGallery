package com.example.cgallery.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SamsungImportManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val albumDao = db.samsungAlbumDao()
    private val folderDao = db.samsungFolderDao()
    private val importer = SamsungGalleryImporter()

    val allAlbums: Flow<List<SamsungAlbumEntity>> = albumDao.getAllAlbums()
    val orphanAlbums: Flow<List<SamsungAlbumEntity>> = albumDao.getOrphanAlbums()
    val allFolders: Flow<List<SamsungFolderEntity>> = folderDao.getAllFolders()

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus = _importStatus.asStateFlow()

    sealed class ImportStatus {
        object Idle : ImportStatus()
        object Loading : ImportStatus()
        data class Success(val albumCount: Int, val folderCount: Int) : ImportStatus()
        data class Error(val message: String) : ImportStatus()
    }

    suspend fun importFromJson(jsonString: String) {
        try {
            _importStatus.value = ImportStatus.Loading
            
            // Parse JSON
            val rawAlbums = importer.parseJson(jsonString)
            
            // Import to database
            val galleryRoot = importer.importAlbums(rawAlbums)
            
            // Clear existing data
            albumDao.deleteAllAlbums()
            folderDao.deleteAllFolders()
            
            // Insert folders
            val folderEntities = galleryRoot.folders.map { folder ->
                SamsungFolderEntity(
                    id = folder.id,
                    name = folder.name,
                    parentId = folder.parentId
                )
            }
            if (folderEntities.isNotEmpty()) {
                folderDao.insertFolders(folderEntities)
            }
            
            // Insert albums directly from rawAlbums with proper folder associations
            val albumEntities = rawAlbums.map { raw ->
                val cover = importer.parseCover(raw.coverRect, raw.defaultCoverPath ?: "")
                // Fallback: if no default_cover_path, use absPath as fallback
                val coverPath = raw.defaultCoverPath ?: raw.absPath ?: ""
                SamsungAlbumEntity(
                    id = raw.bucketId,
                    title = raw.title,
                    path = raw.absPath ?: "",
                    folderId = raw.folderId,
                    coverPath = coverPath,
                    coverCrop = cover.crop?.joinToString(","),
                    sortPrimary = raw.albumOrder,
                    sortSecondary = raw.essentialAlbumOrder
                )
            }
            
            if (albumEntities.isNotEmpty()) {
                albumDao.insertAlbums(albumEntities)
            }
            
            _importStatus.value = ImportStatus.Success(
                albumCount = albumEntities.size,
                folderCount = folderEntities.size
            )
        } catch (e: Exception) {
            _importStatus.value = ImportStatus.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun clearImportedData() {
        albumDao.deleteAllAlbums()
        folderDao.deleteAllFolders()
        _importStatus.value = ImportStatus.Idle
    }

    suspend fun createFolder(name: String, parentId: Long? = null) {
        val folderId = System.currentTimeMillis()
        val folder = SamsungFolderEntity(
            id = folderId,
            name = name,
            parentId = parentId
        )
        folderDao.insertFolder(folder)
    }

    suspend fun moveAlbum(albumId: Long, folderId: Long?) {
        albumDao.moveAlbum(albumId, folderId)
    }

    suspend fun moveFolder(folderId: Long, parentId: Long?) {
        folderDao.moveFolder(folderId, parentId)
    }

    suspend fun renameFolder(folderId: Long, name: String) {
        folderDao.renameFolder(folderId, name)
    }

    suspend fun deleteFolder(folderId: Long) {
        folderDao.deleteFolder(folderId)
        // TODO: Move albums in this folder to orphan status
    }
}
