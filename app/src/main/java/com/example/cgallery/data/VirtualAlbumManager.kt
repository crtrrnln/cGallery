package com.example.cgallery.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class VirtualAlbumManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val dao = db.albumDao()

    val allAlbumsWithMedia: Flow<List<AlbumWithMedia>> = dao.getAllAlbumsWithMedia()

    suspend fun createAlbum(name: String): Long {
        return dao.insertAlbum(AlbumEntity(name = name))
    }

    suspend fun addMediaToAlbum(albumId: Long, mediaId: Long) {
        dao.addMediaToAlbum(AlbumMediaCrossRef(albumId, mediaId))
    }
    
    suspend fun deleteAlbum(album: AlbumEntity) {
        dao.deleteAlbum(album)
    }
}
