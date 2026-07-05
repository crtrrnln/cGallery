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
        val proj = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME, MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Video.VideoColumns.DURATION)
        val sort = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val sel = if (since > 0) "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.DATE_ADDED} > ?" else "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val args = if (since > 0) arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(), since.toString()) else arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

        context.contentResolver.query(MediaStore.Files.getContentUri("external"), proj, sel, args, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(proj[0]); val nameCol = c.getColumnIndexOrThrow(proj[1]); val buckCol = c.getColumnIndexOrThrow(proj[2])
            val pathCol = c.getColumnIndexOrThrow(proj[3]); val dataCol = c.getColumnIndexOrThrow(proj[4]); val typeCol = c.getColumnIndexOrThrow(proj[5])
            val dateCol = c.getColumnIndexOrThrow(proj[6]); val durCol = c.getColumnIndex(proj[7])
            while (c.moveToNext()) {
                val id = c.getLong(idCol); val name = c.getString(nameCol) ?: ""; val buck = c.getString(buckCol)?.intern() ?: "???"
                val full = c.getString(dataCol) ?: ""; val bPath = try { File(full).parent?.intern() ?: "???" } catch (e: Exception) { "???" }
                val rel = c.getString(pathCol)?.intern() ?: ""; val tInt = c.getInt(typeCol); val date = c.getLong(dateCol); val dur = if (durCol != -1) c.getLong(durCol) else 0L
                val type = if (tInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else if (name.lowercase().endsWith(".gif")) MediaType.GIF else MediaType.IMAGE
                val uri = ContentUris.withAppendedId(if (type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                items.add(MediaItem(id, uri, name, buck, bPath, rel, full, type, dur, date))
            }
        }
        items
    }

    suspend fun fetchMediaFolders(): List<MediaFolder> = withContext(Dispatchers.IO) {
        fetchMedia().groupBy { it.bucketPath }.map { (p, i) -> val n = i.first(); MediaFolder(p, i.first().bucketName, i.size, n.dateAdded, n.uri.toString()) }.sortedByDescending { it.lastModified }
    }
}
