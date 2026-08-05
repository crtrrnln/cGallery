package com.example.cgallery.data
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreDataSource(private val context: Context) {
    suspend fun fetchMedia(since: Long = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        
        try {
            val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            
            // Fetch Images
            val imgProj = arrayOf(
                MediaStore.Images.Media._ID, 
                MediaStore.Images.Media.DISPLAY_NAME, 
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME, 
                MediaStore.Images.Media.DATA, 
                MediaStore.Images.Media.DATE_ADDED
            )
            
            context.contentResolver.query(
                imageUri, 
                imgProj, 
                if (since > 0) "${MediaStore.Images.Media.DATE_ADDED} > ?" else null, 
                if (since > 0) arrayOf(since.toString()) else null, 
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(imgProj[0])
                val nameCol = c.getColumnIndexOrThrow(imgProj[1])
                val buckCol = c.getColumnIndexOrThrow(imgProj[2])
                val dataCol = c.getColumnIndexOrThrow(imgProj[3])
                val dateCol = c.getColumnIndexOrThrow(imgProj[4])
                
                while (c.moveToNext()) {
                    try {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: ""
                        val buck = c.getString(buckCol)?.intern() ?: "???"
                        val full = c.getString(dataCol) ?: ""
                        val bPath = if (full.isNotEmpty()) File(full).parent ?: "???" else "???"
                        val type = if (name.lowercase().endsWith(".gif")) MediaType.GIF else MediaType.IMAGE
                        items.add(MediaItem(id, ContentUris.withAppendedId(imageUri, id), name, buck, bPath.intern(), "", full, type, 0L, c.getLong(dateCol)))
                    } catch (e: Exception) {
                        android.util.Log.e("MediaStoreDataSource", "Error processing image item", e)
                    }
                }
            }
            
            // Fetch Videos
            val vidProj = arrayOf(
                MediaStore.Video.Media._ID, 
                MediaStore.Video.Media.DISPLAY_NAME, 
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME, 
                MediaStore.Video.Media.DATA, 
                MediaStore.Video.Media.DATE_ADDED, 
                MediaStore.Video.Media.DURATION
            )
            
            context.contentResolver.query(
                videoUri, 
                vidProj, 
                if (since > 0) "${MediaStore.Video.Media.DATE_ADDED} > ?" else null, 
                if (since > 0) arrayOf(since.toString()) else null, 
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(vidProj[0])
                val nameCol = c.getColumnIndexOrThrow(vidProj[1])
                val buckCol = c.getColumnIndexOrThrow(vidProj[2])
                val dataCol = c.getColumnIndexOrThrow(vidProj[3])
                val dateCol = c.getColumnIndexOrThrow(vidProj[4])
                val durCol = c.getColumnIndexOrThrow(vidProj[5])
                
                while (c.moveToNext()) {
                    try {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: ""
                        val buck = c.getString(buckCol)?.intern() ?: "???"
                        val full = c.getString(dataCol) ?: ""
                        val bPath = if (full.isNotEmpty()) File(full).parent ?: "???" else "???"
                        items.add(MediaItem(id, ContentUris.withAppendedId(videoUri, id), name, buck, bPath.intern(), "", full, MediaType.VIDEO, c.getLong(durCol), c.getLong(dateCol)))
                    } catch (e: Exception) {
                        android.util.Log.e("MediaStoreDataSource", "Error processing video item", e)
                    }
                }
            }
            
            items.sortByDescending { it.dateAdded }
        } catch (e: SecurityException) {
            android.util.Log.e("MediaStoreDataSource", "Permission denied accessing MediaStore", e)
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "Error fetching media", e)
        }
        
        items
    }

    suspend fun fetchMediaFolders(): List<MediaFolder> = withContext(Dispatchers.IO) {
        fetchMedia().groupBy { it.bucketPath }.map { (p, i) -> val n = i.first(); MediaFolder(p, i.first().bucketName, i.size, n.dateAdded, n.uri.toString()) }.sortedByDescending { it.lastModified }
    }

    suspend fun getDetailedStorageStats(): DetailedStorageStats = withContext(Dispatchers.IO) {
        val proj = arrayOf(MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME, MediaStore.Files.FileColumns.VOLUME_NAME, MediaStore.Files.FileColumns.DATA)
        val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        var tISize = 0L; var tVSize = 0L; var iCount = 0; var vCount = 0
        val vMap = mutableMapOf<String, VolumeStats>(); val bMap = mutableMapOf<String, BucketStats>()
        context.contentResolver.query(MediaStore.Files.getContentUri("external"), proj, "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)", args, null)?.use { c ->
            val sCol = c.getColumnIndexOrThrow(proj[0]); val tCol = c.getColumnIndexOrThrow(proj[1]); val bCol = c.getColumnIndexOrThrow(proj[2]); val vCol = c.getColumnIndexOrThrow(proj[3]); val dCol = c.getColumnIndexOrThrow(proj[4])
            while (c.moveToNext()) {
                val size = c.getLong(sCol); val type = c.getInt(tCol); val bName = c.getString(bCol) ?: "Unknown"; val vol = c.getString(vCol) ?: "Internal"; val data = c.getString(dCol) ?: ""; val bPath = data.substringBeforeLast('/', "").intern()
                val isV = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                if (isV) { tVSize += size; vCount++ } else { tISize += size; iCount++ }
                val vs = vMap.getOrPut(vol) { VolumeStats(vol, 0L, 0L, 0, 0, 0) }
                vMap[vol] = if (isV) vs.copy(videoSize = vs.videoSize + size, vCount = vs.vCount + 1, count = vs.count + 1) else vs.copy(imageSize = vs.imageSize + size, iCount = vs.iCount + 1, count = vs.count + 1)
                val bs = bMap.getOrPut(bPath) { BucketStats(bName, bPath, vol, 0L, 0L, 0, 0, 0) }
                bMap[bPath] = if (isV) bs.copy(videoSize = bs.videoSize + size, vCount = bs.vCount + 1, count = bs.count + 1) else bs.copy(imageSize = bs.imageSize + size, iCount = bs.iCount + 1, count = bs.count + 1)
            }
        }; DetailedStorageStats(tISize, tVSize, iCount, vCount, vMap.values.toList().sortedByDescending { it.imageSize + it.videoSize }, bMap.values.toList().sortedByDescending { it.imageSize + it.videoSize })
    }

    suspend fun updateMediaDate(id: Long, type: MediaType, newDateSeconds: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = if (type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentUri = ContentUris.withAppendedId(uri, id)
            val values = android.content.ContentValues().apply {
                put(MediaStore.Files.FileColumns.DATE_ADDED, newDateSeconds)
                put(MediaStore.Files.FileColumns.DATE_MODIFIED, newDateSeconds)
            }
            val result = context.contentResolver.update(contentUri, values, null, null) > 0
            if (!result) {
                android.util.Log.w("MediaStoreDataSource", "Failed to update date for media item $id")
            }
            result
        } catch (e: SecurityException) {
            android.util.Log.e("MediaStoreDataSource", "Permission denied updating media date", e)
            false
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "Error updating media date", e)
            false
        }
    }

    suspend fun moveToTrash(item: MediaItem): TrashItemEntity? = withContext(Dispatchers.IO) {
        try {
            val trashDir = File(context.getExternalFilesDir(null), ".trash")
            if (!trashDir.exists()) trashDir.mkdirs()
            
            val sourceFile = File(item.fullPath)
            if (!sourceFile.exists()) return@withContext null
            
            val destFile = File(trashDir, "${System.currentTimeMillis()}_${item.displayName}")
            
            if (sourceFile.renameTo(destFile)) {
                // Remove from MediaStore
                context.contentResolver.delete(item.uri, null, null)
                
                TrashItemEntity(
                    mediaStoreId = item.id,
                    originalPath = item.fullPath,
                    trashPath = destFile.absolutePath,
                    fileName = item.displayName,
                    mediaType = item.type.name
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "Error moving to trash", e)
            null
        }
    }

    suspend fun restoreFromTrash(trashEntry: TrashItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashEntry.trashPath)
            if (!trashFile.exists()) return@withContext false
            
            val originalFile = File(trashEntry.originalPath)
            val parentDir = originalFile.parentFile
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs()
            
            if (trashFile.renameTo(originalFile)) {
                // Scan back into MediaStore
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(originalFile.absolutePath),
                    null
                ) { _, _ -> }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "Error restoring from trash", e)
            false
        }
    }

    suspend fun deletePermanently(trashEntry: TrashItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashEntry.trashPath)
            if (trashFile.exists()) {
                trashFile.delete()
            } else {
                true // Already gone
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "Error deleting permanently", e)
            false
        }
    }
}

data class DetailedStorageStats(val tISize: Long, val tVSize: Long, val iCount: Int, val vCount: Int, val volumes: List<VolumeStats>, val buckets: List<BucketStats>)
data class VolumeStats(val name: String, val imageSize: Long, val videoSize: Long, val count: Int, val iCount: Int, val vCount: Int)
data class BucketStats(val name: String, val path: String, val volumeName: String, val imageSize: Long, val videoSize: Long, val count: Int, val iCount: Int, val vCount: Int)
