package com.example.cgallery.data

import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PhysicalAlbumManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val physicalAlbumDao = db.physicalAlbumDao()
    private val groupDao = db.albumGroupDao()
    private val context = context

    val allAlbums: Flow<List<PhysicalAlbumEntity>> = physicalAlbumDao.getAllAlbums()

    suspend fun syncAlbums(bucketNames: List<String>) {
        val existingAlbums = physicalAlbumDao.getAllAlbums().first()
        val existingBucketNames = existingAlbums.map { it.bucketName }.toSet()
        val maxSortOrder = existingAlbums.maxOfOrNull { it.sortOrder } ?: -1

        // Add new albums
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

        // Remove albums that no longer exist in MediaStore
        existingAlbums.forEach { album ->
            if (!bucketNames.contains(album.bucketName)) {
                physicalAlbumDao.deleteAlbum(album)
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

    suspend fun createFolder(folderName: String, parentPath: String? = null): Result<String> {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val parentDir = if (parentPath != null) File(parentPath) else picturesDir

            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            val newFolder = File(parentDir, folderName)
            if (newFolder.exists()) {
                Result.failure(Exception("Folder already exists"))
            } else {
                val success = newFolder.mkdirs()
                if (success) {
                    // Add to database
                    physicalAlbumDao.insertAlbum(
                        PhysicalAlbumEntity(
                            bucketName = newFolder.absolutePath,
                            isHidden = false,
                            groupId = null
                        )
                    )
                    Result.success(newFolder.absolutePath)
                } else {
                    Result.failure(Exception("Failed to create folder"))
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

            // Try atomic rename first
            val renamed = sourceFile.renameTo(targetFile)
            if (renamed) {
                Result.success(targetFile.absolutePath)
            } else {
                // Fallback to copy-delete if rename fails (e.g. cross-partition)
                sourceFile.copyTo(targetFile, overwrite = false)
                val deleted = sourceFile.delete()
                if (deleted) {
                    Result.success(targetFile.absolutePath)
                } else {
                    // Clean up the copied file if source deletion failed
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
}
