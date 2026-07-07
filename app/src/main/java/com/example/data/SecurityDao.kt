package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    // --- Asset Operations ---
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: Int): Asset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset): Long

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)

    // --- Scan Operations ---
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<Scan>>

    @Query("SELECT * FROM scans WHERE assetId = :assetId ORDER BY timestamp DESC")
    fun getScansForAsset(assetId: Int): Flow<List<Scan>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Int): Scan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: Scan): Long

    @Update
    suspend fun updateScan(scan: Scan)

    @Delete
    suspend fun deleteScan(scan: Scan)

    @Query("DELETE FROM scans WHERE assetId = :assetId")
    suspend fun deleteScansForAsset(assetId: Int)
}
