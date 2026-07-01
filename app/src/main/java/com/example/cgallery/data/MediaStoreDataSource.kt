package com.example.cgallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalImage(
    val id: Long,
    val uri: Uri
)

class MediaStoreDataSource(private val context: Context) {

    suspend fun fetchImages(): List<LocalImage> = withContext(Dispatchers.IO) {
        val images = mutableListOf<LocalImage>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        val query = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(LocalImage(id, contentUri))
            }
        }
        
        images
    }
}
