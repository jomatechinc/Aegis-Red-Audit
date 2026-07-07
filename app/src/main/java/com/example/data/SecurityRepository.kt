package com.example.data

import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val securityDao: SecurityDao) {

    val allAssets: Flow<List<Asset>> = securityDao.getAllAssets()
    val allScans: Flow<List<Scan>> = securityDao.getAllScans()

    suspend fun getAssetById(id: Int): Asset? = securityDao.getAssetById(id)

    suspend fun insertAsset(asset: Asset): Long = securityDao.insertAsset(asset)

    suspend fun updateAsset(asset: Asset) = securityDao.updateAsset(asset)

    suspend fun deleteAsset(asset: Asset) {
        securityDao.deleteScansForAsset(asset.id)
        securityDao.deleteAsset(asset)
    }

    fun getScansForAsset(assetId: Int): Flow<List<Scan>> = securityDao.getScansForAsset(assetId)

    suspend fun getScanById(id: Int): Scan? = securityDao.getScanById(id)

    suspend fun insertScan(scan: Scan): Long = securityDao.insertScan(scan)

    suspend fun updateScan(scan: Scan) = securityDao.updateScan(scan)

    suspend fun deleteScan(scan: Scan) = securityDao.deleteScan(scan)
}
