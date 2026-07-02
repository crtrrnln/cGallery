package com.example.cgallery.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AlbumGroupManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val groupDao = db.albumGroupDao()
    private val physicalAlbumDao = db.physicalAlbumDao()

    val allGroups: Flow<List<AlbumGroupEntity>> = groupDao.getAllGroups()
    val rootGroups: Flow<List<AlbumGroupEntity>> = groupDao.getRootGroups()

    fun getGroupById(groupId: Long): Flow<AlbumGroupEntity?> {
        return groupDao.getGroupById(groupId)
    }

    suspend fun createGroup(name: String, parentId: Long? = null): Long {
        val existingGroups = groupDao.getAllGroups().first()
        val maxSortOrder = existingGroups.maxOfOrNull { it.sortOrder } ?: -1
        val group = AlbumGroupEntity(
            name = name,
            parentId = parentId,
            sortOrder = maxSortOrder + 1
        )
        return groupDao.insertGroup(group)
    }

    suspend fun updateGroup(group: AlbumGroupEntity) {
        groupDao.updateGroup(group)
    }

    suspend fun deleteGroup(group: AlbumGroupEntity) {
        // Move albums in this group to null (ungrouped)
        val albumsInGroup = physicalAlbumDao.getAlbumsByGroup(group.id).first()
        albumsInGroup.forEach { album ->
            physicalAlbumDao.moveAlbumToGroup(album.bucketName, null)
        }
        groupDao.deleteGroup(group)
    }

    suspend fun moveGroup(groupId: Long, parentId: Long?) {
        groupDao.moveGroup(groupId, parentId)
    }

    suspend fun renameGroup(groupId: Long, name: String) {
        val group = groupDao.getGroupById(groupId).first() ?: return
        groupDao.updateGroup(group.copy(name = name))
    }

    suspend fun moveGroupUp(groupId: Long) {
        val group = groupDao.getGroupById(groupId).first() ?: return
        val parentId = group.parentId
        val siblings = if (parentId == null) {
            groupDao.getRootGroups().first()
        } else {
            groupDao.getChildGroups(parentId).first()
        }
        val currentIndex = siblings.indexOf(group)
        if (currentIndex > 0) {
            val previousGroup = siblings[currentIndex - 1]
            groupDao.updateGroupSortOrder(groupId, previousGroup.sortOrder)
            groupDao.updateGroupSortOrder(previousGroup.id, group.sortOrder)
        }
    }

    suspend fun moveGroupDown(groupId: Long) {
        val group = groupDao.getGroupById(groupId).first() ?: return
        val parentId = group.parentId
        val siblings = if (parentId == null) {
            groupDao.getRootGroups().first()
        } else {
            groupDao.getChildGroups(parentId).first()
        }
        val currentIndex = siblings.indexOf(group)
        if (currentIndex < siblings.size - 1) {
            val nextGroup = siblings[currentIndex + 1]
            groupDao.updateGroupSortOrder(groupId, nextGroup.sortOrder)
            groupDao.updateGroupSortOrder(nextGroup.id, group.sortOrder)
        }
    }

    fun getAlbumsByGroup(groupId: Long?): Flow<List<PhysicalAlbumEntity>> {
        return physicalAlbumDao.getAlbumsByGroup(groupId)
    }
}
