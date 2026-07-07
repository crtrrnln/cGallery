package com.example.cgallery.data
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreDataSource(private val context: Context) {
    suspend fun fetchMedia(since: Long = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>(); val proj = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME, MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Video.VideoColumns.DURATION)
        val sel = if (since > 0) "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.DATE_ADDED} > ?" else "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val args = if (since > 0) arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(), since.toString()) else arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        context.contentResolver.query(MediaStore.Files.getContentUri("external"), proj, sel, args, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(proj[0]); val nameCol = c.getColumnIndexOrThrow(proj[1]); val buckCol = c.getColumnIndexOrThrow(proj[2]); val dataCol = c.getColumnIndexOrThrow(proj[4]); val tCol = c.getColumnIndexOrThrow(proj[5]); val dateCol = c.getColumnIndexOrThrow(proj[6]); val durCol = c.getColumnIndex(proj[7])
            while (c.moveToNext()) {
                val id = c.getLong(idCol); val name = c.getString(nameCol) ?: ""; val buck = c.getString(buckCol)?.intern() ?: "???"; val full = c.getString(dataCol) ?: ""; val bPath = full.substringBeforeLast('/', "???").intern()
                val type = if (c.getInt(tCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else if (name.lowercase().endsWith(".gif")) MediaType.GIF else MediaType.IMAGE
                items.add(MediaItem(id, ContentUris.withAppendedId(if (type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), name, buck, bPath, "", full, type, if (durCol != -1) c.getLong(durCol) else 0L, c.getLong(dateCol)))
            }
        }; items
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
            context.contentResolver.update(contentUri, values, null, null) > 0
        } catch (e: Exception) { false }
    }
}

data class DetailedStorageStats(val tISize: Long, val tVSize: Long, val iCount: Int, val vCount: Int, val volumes: List<VolumeStats>, val buckets: List<BucketStats>)
data class VolumeStats(val name: String, val imageSize: Long, val videoSize: Long, val count: Int, val iCount: Int, val vCount: Int)
data class BucketStats(val name: String, val path: String, val volumeName: String, val imageSize: Long, val videoSize: Long, val count: Int, val iCount: Int, val vCount: Int)
