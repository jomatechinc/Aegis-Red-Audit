package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val description: String,
    val category: String, // e.g. "Web Application", "API Server", "Internal Host"
    val tags: String,     // Comma-separated tags, e.g. "production,auth,public"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scans")
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assetId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String,      // "Vulnerability Sweep", "SSL/TLS Audit", "Port Scanning"
    val status: String,        // "COMPLETED", "RUNNING", "FAILED"
    val severityLevel: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW", "CLEAN"
    val riskScore: Int,        // 0 to 100
    val findingsJson: String,  // Detailed findings list as serialized JSON or structured string
    val threatIntelJson: String // Rich data retrieved from Threat Intelligence (AbuseIPDB, VT, etc.)
)

data class ScanFinding(
    val title: String,
    val description: String,
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"
    val recommendation: String,
    val category: String // "Headers", "SSL", "Ports", "Configuration"
)

data class ThreatIntelResult(
    val checkedAddress: String,
    val abuseScore: Int = 0,
    val totalReports: Int = 0,
    val lastReportedAt: String = "Never",
    val virusTotalMaliciousCount: Int = 0,
    val reputationVerdict: String = "Clean",
    val scannerDetails: String = "Standard intelligence feed lookup complete."
)
