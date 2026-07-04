package com.example.cgallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreDataSource(private val context: Context) {

    suspend fun fetchMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Video.VideoColumns.DURATION
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val query = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)",
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            ),
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val bucket = cursor.getString(bucketColumn)?.intern() ?: "Unknown"
                val fullPath = cursor.getString(dataColumn) ?: ""
                val bucketPath = try { java.io.File(fullPath).parent?.intern() ?: "Unknown" } catch (e: Exception) { "Unknown" }
                val path = cursor.getString(pathColumn)?.intern() ?: ""
                val typeInt = cursor.getInt(typeColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L

                val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    MediaType.VIDEO
                } else if (name.lowercase().endsWith(".gif")) {
                    MediaType.GIF
                } else {
                    MediaType.IMAGE
                }

                val contentUri = ContentUris.withAppendedId(
                    if (type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                mediaItems.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        bucketName = bucket,
                        bucketPath = bucketPath,
                        relativePath = path,
                        fullPath = fullPath,
                        type = type,
                        duration = duration,
                        dateAdded = dateAdded
                    )
                )
            }
        }

        mediaItems
    }

    suspend fun fetchMediaFolders(): List<MediaFolder> = withContext(Dispatchers.IO) {
        val media = fetchMedia()
        media.groupBy { it.bucketPath }
            .map { (path, items) ->
                val newest = items.first()
                MediaFolder(
                    path = path,
                    name = items.first().bucketName,
                    itemCount = items.size,
                    lastModified = newest.dateAdded,
                    coverUri = newest.uri.toString()
                )
            }
            .sortedByDescending { it.lastModified }
    }
}
