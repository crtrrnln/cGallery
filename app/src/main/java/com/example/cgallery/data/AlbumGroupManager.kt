package com.example.cgallery.data
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AlbumGroupManager(context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context); private val groupDao = db.albumGroupDao(); private val physDao = db.physicalAlbumDao()
    val allGroups: Flow<List<AlbumGroupEntity>> = groupDao.getAllGroups()
    val rootGroups: Flow<List<AlbumGroupEntity>> = groupDao.getRootGroups()
    fun getGroupById(id: Long): Flow<AlbumGroupEntity?> = groupDao.getGroupById(id)

    suspend fun createGroup(name: String, parentId: Long? = null): Long {
        val existing = groupDao.getAllGroups().first(); val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
        return groupDao.insertGroup(AlbumGroupEntity(name = name, parentId = parentId, sortOrder = maxSort + 1))
    }

    suspend fun updateGroup(g: AlbumGroupEntity) = groupDao.updateGroup(g)

    suspend fun deleteGroup(g: AlbumGroupEntity) {
        physDao.getAlbumsByGroup(g.id).first().forEach { physDao.moveAlbumToGroup(it.bucketName, null) }
        groupDao.getAllGroups().first().filter { it.parentId == g.id }.forEach { groupDao.moveGroup(it.id, null) }
        groupDao.deleteGroup(g)
    }

    suspend fun moveGroup(id: Long, pid: Long?) = groupDao.moveGroup(id, pid)
    suspend fun renameGroup(id: Long, name: String) { val g = groupDao.getGroupById(id).first() ?: return; groupDao.updateGroup(g.copy(name = name)) }

    suspend fun moveGroupUp(id: Long) {
        val g = groupDao.getGroupById(id).first() ?: return
        val siblings = if (g.parentId == null) groupDao.getRootGroups().first() else groupDao.getChildGroups(g.parentId).first()
        val idx = siblings.indexOf(g)
        if (idx > 0) { val prev = siblings[idx - 1]; groupDao.updateGroupSortOrder(id, prev.sortOrder); groupDao.updateGroupSortOrder(prev.id, g.sortOrder) }
    }

    suspend fun moveGroupDown(id: Long) {
        val g = groupDao.getGroupById(id).first() ?: return
        val siblings = if (g.parentId == null) groupDao.getRootGroups().first() else groupDao.getChildGroups(g.parentId).first()
        val idx = siblings.indexOf(g)
        if (idx < siblings.size - 1) { val next = siblings[idx + 1]; groupDao.updateGroupSortOrder(id, next.sortOrder); groupDao.updateGroupSortOrder(next.id, g.sortOrder) }
    }

    suspend fun updateGroupSortOrder(id: Long, s: Int) = groupDao.updateGroupSortOrder(id, s)
    suspend fun deleteGroup(id: Long) {
        physDao.getAlbumsByGroup(id).first().forEach { physDao.moveAlbumToGroup(it.bucketName, null) }
        groupDao.getChildGroups(id).first().forEach { groupDao.moveGroup(it.id, null) }
        groupDao.getGroupById(id).first()?.let { groupDao.deleteGroup(it) }
    }
    fun getAlbumsByGroup(id: Long?): Flow<List<PhysicalAlbumEntity>> = physDao.getAlbumsByGroup(id)
}
