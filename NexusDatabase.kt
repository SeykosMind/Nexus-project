package com.nexus.data.local

import androidx.room.*
import com.nexus.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE path = :path")
    suspend fun getByPath(path: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int

    @Query("SELECT * FROM documents WHERE content LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchByContent(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchByName(query: String): List<DocumentEntity>

    @Query("SELECT extension, COUNT(*) as cnt FROM documents GROUP BY extension")
    suspend fun countByExtension(): List<ExtCount>

    @Query("SELECT SUM(sizeBytes) FROM documents")
    suspend fun totalSize(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(docs: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents WHERE path NOT IN (:existingPaths)")
    suspend fun deleteOrphans(existingPaths: List<String>)

    @Query("SELECT * FROM documents ORDER BY indexedAt DESC LIMIT 20")
    suspend fun recentlyIndexed(): List<DocumentEntity>
}

data class ExtCount(val extension: String, val cnt: Int)

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "nexus_db"
    }
}
