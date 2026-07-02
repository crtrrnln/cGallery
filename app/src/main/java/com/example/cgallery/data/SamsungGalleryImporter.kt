package com.example.cgallery.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.long

@Serializable
data class SamsungAlbumRaw(
    @SerialName("__bucketID")
    val bucketId: Long,
    @SerialName("__Title")
    val title: String,
    @SerialName("album_order")
    val albumOrder: Long,
    @SerialName("__absPath")
    val absPath: String? = null,
    @SerialName("default_cover_path")
    val defaultCoverPath: String? = null,
    @SerialName("cover_rect")
    val coverRect: String = "",
    @SerialName("__albumType")
    val albumType: Int = 0,
    @SerialName("__albumLevel")
    val albumLevel: Int = 0,
    @SerialName("essential_album_order")
    val essentialAlbumOrder: Long = 0L,
    @SerialName("folder_id")
    val folderId: Long? = null,
    @SerialName("folder_name")
    val folderName: String? = null
)

data class AlbumNode(
    val id: Long,
    val title: String,
    val path: String,
    val groupId: Long? = null,
    val cover: Cover,
    val sortWeight: Pair<Long, Long>
)

data class FolderNode(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
    val albums: MutableList<AlbumNode> = mutableListOf(),
    val subfolders: MutableList<FolderNode> = mutableListOf()
)

data class Cover(
    val path: String,
    val crop: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Cover
        if (path != other.path) return false
        if (crop != null) {
            if (other.crop == null) return false
            if (!crop.contentEquals(other.crop)) return false
        } else if (other.crop != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + (crop?.contentHashCode() ?: 0)
        return result
    }
}

data class GalleryRoot(
    val folders: List<FolderNode>,
    val orphanAlbums: List<AlbumNode>
)

class SamsungGalleryImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseJson(jsonString: String): List<SamsungAlbumRaw> {
        return json.decodeFromString(jsonString)
    }

    fun parseCover(coverRect: String, defaultPath: String): Cover {
        val segments = coverRect.split(";")
        val crop = if (segments.size >= 4) {
            try {
                floatArrayOf(
                    segments[0].toFloatOrNull() ?: 0f,
                    segments[1].toFloatOrNull() ?: 0f,
                    segments[2].toFloatOrNull() ?: 1f,
                    segments[3].toFloatOrNull() ?: 1f
                )
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }
        return Cover(path = defaultPath, crop = crop)
    }

    fun importAlbums(rawAlbums: List<SamsungAlbumRaw>): GalleryRoot {
        // Step 1: Normalize input
        val normalized = rawAlbums.map { raw ->
            SamsungAlbumRaw(
                bucketId = raw.bucketId,
                title = raw.title.trim(),
                albumOrder = raw.albumOrder,
                absPath = raw.absPath,
                defaultCoverPath = raw.defaultCoverPath,
                coverRect = raw.coverRect,
                albumType = raw.albumType,
                albumLevel = raw.albumLevel,
                essentialAlbumOrder = raw.essentialAlbumOrder,
                folderId = raw.folderId,
                folderName = raw.folderName?.trim()
            )
        }

        // Step 2: Create AlbumNode map
        val albumMap = normalized.associate { raw ->
            val cover = parseCover(raw.coverRect, raw.defaultCoverPath ?: "")
            val sortWeight = Pair(raw.albumOrder, raw.essentialAlbumOrder)
            raw.bucketId to AlbumNode(
                id = raw.bucketId,
                title = raw.title,
                path = raw.absPath ?: "",
                groupId = raw.folderId,
                cover = cover,
                sortWeight = sortWeight
            )
        }

        // Step 3: Create Folder registry from folder_id
        val folderRegistry = mutableMapOf<Long, FolderNode>()
        normalized.forEach { raw ->
            raw.folderId?.let { folderId ->
                if (!folderRegistry.containsKey(folderId)) {
                    folderRegistry[folderId] = FolderNode(
                        id = folderId,
                        name = raw.folderName ?: "Folder $folderId"
                    )
                }
            }
        }

        // Step 4: Assign albums to folders or orphan list
        val folderAlbums = mutableMapOf<Long, MutableList<AlbumNode>>()
        val orphanAlbums = mutableListOf<AlbumNode>()

        normalized.forEach { raw ->
            val albumNode = albumMap[raw.bucketId] ?: return@forEach
            if (raw.folderId != null) {
                folderAlbums.getOrPut(raw.folderId) { mutableListOf() }.add(albumNode)
            } else {
                orphanAlbums.add(albumNode)
            }
        }

        // Step 5: Build folder hierarchy (flat for now - no parent folder info in JSON)
        val folders = folderRegistry.values.toList()

        // Step 6: Assign albums to folders
        folders.forEach { folder ->
            val albumsInFolder = folderAlbums[folder.id] ?: emptyList()
            folder.albums.addAll(albumsInFolder)
        }

        // Step 7: Apply sorting rules
        val sortedOrphanAlbums = orphanAlbums.sortedWith(compareBy({ it.sortWeight.first }, { it.sortWeight.second }))
        
        folders.forEach { folder ->
            folder.albums.sortWith(compareBy({ it.sortWeight.first }, { it.sortWeight.second }))
        }
        
        val sortedFolders = folders.sortedBy { folder ->
            if (folder.albums.isNotEmpty()) {
                folder.albums.minOf { it.sortWeight.first }
            } else {
                Long.MAX_VALUE
            }
        }

        // Step 8: Return GalleryRoot
        return GalleryRoot(
            folders = sortedFolders,
            orphanAlbums = sortedOrphanAlbums
        )
    }
}
