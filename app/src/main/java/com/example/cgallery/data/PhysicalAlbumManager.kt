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
    private val context = context

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val allAlbums: Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAllAlbums()

    suspend fun syncAlbums(bucketNames: List<String>) {
        val existingAlbums = physicalAlbumDao.getAllAlbums().first()
        val existingBucketNames = existingAlbums.map { it.bucketName }.toSet()
        val maxSortOrder = existingAlbums.maxOfOrNull { it.sortOrder } ?: -1

        bucketNames.forEachIndexed { index, bucketName ->
            if (!existingBucketNames.contains(bucketName)) {
                physicalAlbumDao.insertAlbum(
                    PhysicalAlbumEntity(
                        bucketName = bucketName,
                        isHidden = false,
                        groupId = null,
                        sortOrder = maxSortOrder + 1 + index
                    )
                )
            }
        }

        existingAlbums.forEach { album ->
            if (!bucketNames.contains(album.bucketName)) {
                val file = File(album.bucketName)
                if (!file.exists() || !file.isDirectory) {
                    physicalAlbumDao.deleteAlbum(album)
                }
            }
        }
    }

    suspend fun toggleAlbumVisibility(bucketName: String) {
        val album = physicalAlbumDao.getAlbumByBucketName(bucketName).first()
        if (album != null) {
            physicalAlbumDao.updateAlbumVisibility(bucketName, !album.isHidden)
        }
    }

    suspend fun moveAlbumToGroup(bucketName: String, groupId: Long?) {
        physicalAlbumDao.moveAlbumToGroup(bucketName, groupId)
    }

    suspend fun updateAlbumSortOrder(albumId: Long, sortOrder: Int) {
        physicalAlbumDao.updateAlbumSortOrder(albumId, sortOrder)
    }

    fun getAlbumsByGroup(groupId: Long?): Flow<List<PhysicalAlbumEntity>> {
        return physicalAlbumDao.getAlbumsByGroup(groupId)
    }

    suspend fun createFolder(folderName: String, parentPath: String? = null, groupId: Long? = null): Result<String> {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val parentDir = if (parentPath != null) File(parentPath) else picturesDir

            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            val newFolder = File(parentDir, folderName)
            if (newFolder.exists()) {
                Result.failure(Exception("Album already exists"))
            } else {
                val success = newFolder.mkdirs()
                if (success) {
                    val existingAlbums = physicalAlbumDao.getAllAlbums().first()
                    val maxSortOrder = existingAlbums.maxOfOrNull { it.sortOrder } ?: -1
                    physicalAlbumDao.insertAlbum(
                        PhysicalAlbumEntity(
                            bucketName = newFolder.absolutePath,
                            isHidden = false,
                            groupId = groupId,
                            sortOrder = maxSortOrder + 1
                        )
                    )
                    MediaScannerConnection.scanFile(context, arrayOf(newFolder.absolutePath), null, null)

                    Result.success(newFolder.absolutePath)
                } else {
                    Result.failure(Exception("Failed to create album"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFile(sourcePath: String, targetFolderPath: String): Result<String> {
        return try {
            val sourceFile = File(sourcePath)
            val targetFolder = File(targetFolderPath)

            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file does not exist"))
            }

            if (!targetFolder.exists()) {
                targetFolder.mkdirs()
            }

            val targetFile = File(targetFolder, sourceFile.name)

            if (targetFile.exists()) {
                return Result.failure(Exception("Target file already exists"))
            }

            val renamed = sourceFile.renameTo(targetFile)
            if (renamed) {
                Result.success(targetFile.absolutePath)
            } else {
                sourceFile.copyTo(targetFile, overwrite = false)
                val deleted = sourceFile.delete()
                if (deleted) {
                    Result.success(targetFile.absolutePath)
                } else {
                    targetFile.delete()
                    Result.failure(Exception("Failed to delete source file after copy"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFile(sourcePath: String, targetFolderPath: String): Result<String> {
        return try {
            val sourceFile = File(sourcePath)
            val targetFolder = File(targetFolderPath)

            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file does not exist"))
            }

            if (!targetFolder.exists()) {
                targetFolder.mkdirs()
            }

            val targetFile = File(targetFolder, sourceFile.name)

            if (targetFile.exists()) {
                return Result.failure(Exception("Target file already exists"))
            }

            sourceFile.copyTo(targetFile, overwrite = false)
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class StructureExport(
        val groups: List<AlbumGroupEntity> = emptyList(),
        val albums: List<PhysicalAlbumEntity> = emptyList(),
        val monitoredFolders: List<MonitoredFolderEntity> = emptyList()
    )

    suspend fun exportStructure(): String = withContext(Dispatchers.IO) {
        val groups = groupDao.getAllGroups().first()
        val albums = physicalAlbumDao.getAllAlbums().first()
        val folders = folderDao.getAllFolders().first()
        json.encodeToString(StructureExport(groups, albums, folders))
    }

    suspend fun importStructure(jsonStr: String) = withContext(Dispatchers.IO) {
        try {
            val data = json.decodeFromString<StructureExport>(jsonStr)
            val existingGroups = groupDao.getAllGroups().first()
            existingGroups.forEach { groupDao.deleteGroup(it) }

            val groupNameMap = mutableMapOf<Long, Long>()

            data.groups.forEach { group ->
                val newId = groupDao.insertGroup(group.copy(id = 0))
                groupNameMap[group.id] = newId
            }

            data.groups.forEach { group ->
                if (group.parentId != null) {
                    val oldParentId = group.parentId
                    val newId = groupNameMap[group.id]
                    val newParentId = groupNameMap[oldParentId]
                    if (newId != null && newParentId != null) {
                        val currentGroup = groupDao.getGroupById(newId).first()
                        if (currentGroup != null) {
                            groupDao.updateGroup(currentGroup.copy(parentId = newParentId))
                        }
                    }
                }
            }

            data.albums.forEach { importedAlbum ->
                val existing = physicalAlbumDao.getAlbumByBucketName(importedAlbum.bucketName).first()
                val newGroupId = importedAlbum.groupId?.let { groupNameMap[it] }
                
                if (existing != null) {
                    physicalAlbumDao.updateAlbum(
                        existing.copy(
                            groupId = newGroupId,
                            sortOrder = importedAlbum.sortOrder,
                            isHidden = importedAlbum.isHidden,
                            customCoverUri = importedAlbum.customCoverUri,
                            customCoverCrop = importedAlbum.customCoverCrop
                        )
                    )
                } else {
                    val folder = File(importedAlbum.bucketName)
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    
                    physicalAlbumDao.insertAlbum(
                        importedAlbum.copy(
                            id = 0,
                            groupId = newGroupId
                        )
                    )
                    MediaScannerConnection.scanFile(context, arrayOf(importedAlbum.bucketName), null, null)
                }
            }

            data.monitoredFolders.forEach { importedFolder ->
                folderDao.insertFolder(importedFolder.copy(id = 0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateAlbumCover(bucketName: String, uri: String?, crop: String?) {
        physicalAlbumDao.updateAlbumCover(bucketName, uri, crop)
    }

    suspend fun updateGroupCover(groupId: Long, uri: String?, crop: String?) {
        groupDao.updateGroupCover(groupId, uri, crop)
    }
}
