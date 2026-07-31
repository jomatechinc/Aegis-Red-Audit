package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SecurityDatabase.getDatabase(application)
    private val repository = SecurityRepository(database.securityDao())

    // UI state flows
    val assets: StateFlow<List<Asset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scans: StateFlow<List<Scan>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow<Int?>(null) // Stores assetId if scanning, else null
    val isScanning: StateFlow<Int?> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Retrieve system variables configured securely in AI Studio secrets panel
    val abuseIpDbKey: String = BuildConfig.GEMINI_API_KEY.let {
        // Fallback or secondary checks if they match mock placeholder names.
        // We also check other potential keys
        if (it == "MY_GEMINI_API_KEY") "" else it
    }
    // We can also let the user inspect or simulate customizable API keys
    private val _customAbuseKey = MutableStateFlow("")
    val customAbuseKey = _customAbuseKey.asStateFlow()

    private val _customVtKey = MutableStateFlow("")
    val customVtKey = _customVtKey.asStateFlow()

    init {
        // Pre-populate database with realistic security targets so the dashboard has immediate gorgeous context
        viewModelScope.launch {
            repository.allAssets.first().let { currentAssets ->
                if (currentAssets.isEmpty()) {
                    seedInitialData()
                }
            }
        }
    }

    fun setCustomKeys(abuseKey: String, vtKey: String) {
        _customAbuseKey.value = abuseKey
        _customVtKey.value = vtKey
    }

    private suspend fun seedInitialData() {
        // Insert some seed assets
        val testAppId = repository.insertAsset(
            Asset(
                name = "Aegis Secure Portal",
                url = "https://example.com",
                description = "Primary corporate public website frontend gateway.",
                category = "Web Application",
                tags = "production,public,frontend"
            )
        ).toInt()

        val localhostId = repository.insertAsset(
            Asset(
                name = "Cloud Sandbox Node",
                url = "8.8.8.8",
                description = "Secure development cloud staging zone host.",
                category = "Cloud Server",
                tags = "staging,aws"
            )
        ).toInt()

        val brokenAppId = repository.insertAsset(
            Asset(
                name = "Vulnerable Demo Target",
                url = "https://httpbin.org/status/418",
                description = "Mock diagnostic target indicating non-HTTPS or missing headers simulation.",
                category = "API Gateway",
                tags = "testing,mock,dev"
            )
        ).toInt()

        // Insert historical completed scans to generate realistic trend graph data immediately
        val curTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        repository.insertScan(
            Scan(
                assetId = testAppId,
                timestamp = curTime - (3 * oneDayMs),
                scanType = "Vulnerability Sweep",
                status = "COMPLETED",
                severityLevel = "LOW",
                riskScore = 18,
                findingsJson = """[
                    {"title": "Missing HSTS", "description": "HSTS header not defined.", "severity": "MEDIUM", "recommendation": "Configure strict transport headers.", "category": "Headers"},
                    {"title": "Server Information Disclosure", "description": "nginx signature visible.", "severity": "LOW", "recommendation": "Set server_tokens off;", "category": "Headers"}
                ]""",
                threatIntelJson = """{"checkedAddress": "93.184.216.34", "abuseScore": 2, "totalReports": 1, "lastReportedAt": "Recent", "virusTotalMaliciousCount": 0, "reputationVerdict": "Clean", "scannerDetails": "Reputation clean."}"""
            )
        )

        repository.insertScan(
            Scan(
                assetId = localhostId,
                timestamp = curTime - (2 * oneDayMs),
                scanType = "Port Scanning Audit",
                status = "COMPLETED",
                severityLevel = "CLEAN",
                riskScore = 0,
                findingsJson = "[]",
                threatIntelJson = """{"checkedAddress": "8.8.8.8", "abuseScore": 0, "totalReports": 0, "lastReportedAt": "Recent", "virusTotalMaliciousCount": 0, "reputationVerdict": "Safe DNS Provider", "scannerDetails": "Google Public DNS. Known safe asset."}"""
            )
        )

        repository.insertScan(
            Scan(
                assetId = brokenAppId,
                timestamp = curTime - oneDayMs,
                scanType = "Vulnerability Sweep",
                status = "COMPLETED",
                severityLevel = "HIGH",
                riskScore = 74,
                findingsJson = """[
                    {"title": "Unsecured SSL Suite", "description": "Expired or missing encryption protocols.", "severity": "HIGH", "recommendation": "Upgrade to modern SSL.", "category": "Encryption"},
                    {"title": "Missing CSP Header", "description": "Content-Security-Policy is completely missing.", "severity": "HIGH", "recommendation": "Configure a strict CSP policy.", "category": "Headers"}
                ]""",
                threatIntelJson = """{"checkedAddress": "54.175.230.252", "abuseScore": 34, "totalReports": 12, "lastReportedAt": "Recent", "virusTotalMaliciousCount": 3, "reputationVerdict": "Suspicious Activity Record", "scannerDetails": "Flagged for active automated crawler scans."}"""
            )
        )
    }

    // --- Asset CRUD ---
    fun addAsset(name: String, url: String, description: String, category: String, tags: String) {
        viewModelScope.launch {
            val asset = Asset(
                name = name,
                url = url,
                description = description,
                category = category,
                tags = tags
            )
            repository.insertAsset(asset)
        }
    }

    fun addAndScanAsset(asset: Asset, scanType: String) {
        viewModelScope.launch {
            val insertedId = repository.insertAsset(asset)
            val fullAsset = asset.copy(id = insertedId.toInt())
            startLiveScan(fullAsset, scanType)
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    // --- Launch Live Scan ---
    fun startLiveScan(asset: Asset, scanType: String) {
        if (_isScanning.value != null) return // Already scanning
        _isScanning.value = asset.id
        _scanError.value = null

        viewModelScope.launch {
            try {
                // Determine API keys to use (either custom supplied or system injected)
                val activeAbuseKey = _customAbuseKey.value.ifBlank { abuseIpDbKey }
                val activeVtKey = _customVtKey.value.ifBlank { "" }

                // Trigger actual background HTTP network audit
                val result = SecurityAuditEngine.executeAudit(
                    targetName = asset.name,
                    targetUrlOrIp = asset.url,
                    scanType = scanType,
                    abuseIpDbKey = activeAbuseKey,
                    virusTotalKey = activeVtKey
                )

                // Convert structures to database representation
                val converters = SecurityTypeConverters()
                val findingsString = converters.fromFindingsList(result.findings)
                val threatIntelString = converters.fromIntelResult(result.intelResult)

                val scanRecord = Scan(
                    assetId = asset.id,
                    scanType = scanType,
                    status = "COMPLETED",
                    severityLevel = result.severityLevel,
                    riskScore = result.riskScore,
                    findingsJson = findingsString,
                    threatIntelJson = threatIntelString
                )

                repository.insertScan(scanRecord)
            } catch (e: Exception) {
                _scanError.value = "Audit Engine Failure: ${e.localizedMessage ?: "Connection interrupted"}"
                val failedScan = Scan(
                    assetId = asset.id,
                    scanType = scanType,
                    status = "FAILED",
                    severityLevel = "CLEAN",
                    riskScore = 0,
                    findingsJson = "[]",
                    threatIntelJson = """{"checkedAddress": "${asset.url}", "abuseScore": 0, "totalReports": 0, "lastReportedAt": "Never", "virusTotalMaliciousCount": 0, "reputationVerdict": "Failed Lookup", "scannerDetails": "Failed: ${e.message}"}"""
                )
                repository.insertScan(failedScan)
            } finally {
                _isScanning.value = null
            }
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            val allScansList = scans.value
            for (scan in allScansList) {
                repository.deleteScan(scan)
            }
        }
    }

    fun loadBenchmarkTargets() {
        viewModelScope.launch {
            val benchmarkAssets = listOf(
                Asset(
                    name = "Production API Gateway",
                    url = "api.stripe.com",
                    description = "Core PCI-DSS payment routing gateway.",
                    category = "API Server",
                    tags = "production,pci-dss,public"
                ),
                Asset(
                    name = "Kubernetes Ingress Node",
                    url = "ingress.github.com",
                    description = "Edge routing and load balancer proxy node.",
                    category = "Infrastructure",
                    tags = "prod,k8s,ingress"
                ),
                Asset(
                    name = "Corporate DNS Resolver",
                    url = "1.1.1.1",
                    description = "Primary recursive DNS server.",
                    category = "Internal Host",
                    tags = "dns,cloudflare,core"
                ),
                Asset(
                    name = "Identity SSO Provider",
                    url = "accounts.google.com",
                    description = "OAuth2 / OIDC authentication portal.",
                    category = "Web Application",
                    tags = "auth,sso,critical"
                )
            )
            for (bAsset in benchmarkAssets) {
                val insertedId = repository.insertAsset(bAsset)
                startLiveScan(bAsset.copy(id = insertedId.toInt()), "Vulnerability Sweep")
            }
        }
    }
}
