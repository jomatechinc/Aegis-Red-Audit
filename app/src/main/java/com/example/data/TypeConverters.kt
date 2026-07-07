package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SecurityTypeConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // --- Scan Finding Serialization ---
    private val findingsListType = Types.newParameterizedType(List::class.java, ScanFinding::class.java)
    private val findingsAdapter = moshi.adapter<List<ScanFinding>>(findingsListType)

    @TypeConverter
    fun fromFindingsList(findings: List<ScanFinding>?): String {
        return findingsAdapter.toJson(findings ?: emptyList())
    }

    @TypeConverter
    fun toFindingsList(json: String?): List<ScanFinding> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            findingsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Threat Intel Result Serialization ---
    private val intelAdapter = moshi.adapter(ThreatIntelResult::class.java)

    @TypeConverter
    fun fromIntelResult(result: ThreatIntelResult?): String {
        return if (result != null) intelAdapter.toJson(result) else ""
    }

    @TypeConverter
    fun toIntelResult(json: String?): ThreatIntelResult {
        if (json.isNullOrBlank()) return ThreatIntelResult("Unknown")
        return try {
            intelAdapter.fromJson(json) ?: ThreatIntelResult("Unknown")
        } catch (e: Exception) {
            ThreatIntelResult("Unknown")
        }
    }
}
