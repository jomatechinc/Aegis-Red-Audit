package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetDetailScreen(
    assetId: Int,
    viewModel: SecurityViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val scans by viewModel.scans.collectAsState()
    val isScanningId by viewModel.isScanning.collectAsState()
    val isScanningThis = isScanningId == assetId

    val asset = remember(assets, assetId) { assets.find { it.id == assetId } }

    // Scans for this asset
    val assetScans = remember(scans, assetId) {
        scans.filter { it.assetId == assetId }.sortedByDescending { it.timestamp }
    }

    var activeTab by remember { mutableStateOf("VULNERABILITIES") } // VULNERABILITIES or THREAT_INTEL or HISTORY
    val activeScan = assetScans.firstOrNull() // Display findings of latest scan

    // Parse current findings
    val converters = SecurityTypeConverters()
    val findings = remember(activeScan) {
        if (activeScan != null) {
            converters.toFindingsList(activeScan.findingsJson)
        } else {
            emptyList()
        }
    }

    // Parse current threat intelligence details
    val threatIntel = remember(activeScan) {
        if (activeScan != null) {
            converters.toIntelResult(activeScan.threatIntelJson)
        } else {
            null
        }
    }

    if (asset == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TacticalDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Target profile asset not found.", color = AuditingCrimson)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDark)
    ) {
        // --- TOOLBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back", tint = BrightWhiteText)
            }
            Text(
                text = "SECURITY AUDIT LOGS",
                color = BrightWhiteText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val report = SecurityReportExporter.generateAssetReportMarkdown(
                            asset = asset,
                            scan = activeScan,
                            findings = findings,
                            intel = threatIntel
                        )
                        SecurityReportExporter.shareReport(context, "Executive Security Audit Report - ${asset.name}", report)
                    },
                    modifier = Modifier.testTag("export_report_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Audit Report", tint = CyanIntel)
                }

                IconButton(
                    onClick = { viewModel.startLiveScan(asset, "Vulnerability Sweep") },
                    enabled = !isScanningThis,
                    modifier = Modifier.testTag("detail_refresh_scan")
                ) {
                    if (isScanningThis) {
                        CircularProgressIndicator(color = AuditingCrimson, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Re-Scan Target", tint = AuditingCrimson)
                    }
                }
            }
        }

        // --- ASSET DETAILS SUMMARY CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
            border = BorderStroke(1.dp, BorderMuted),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = asset.name,
                            color = BrightWhiteText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = asset.url,
                            color = MutedGreyText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Risk rating indicator in premium px-3 py-1 bg-[#93000A] style
                    if (activeScan != null) {
                        val isHighAlert = activeScan.severityLevel == "CRITICAL" || activeScan.severityLevel == "HIGH"
                        val badgeBg = if (isHighAlert) DarkCrimson else SurfaceGunmetalLight
                        val badgeText = if (isHighAlert) AuditingCrimson else DefendingGreen
                        
                        Box(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(100.dp))
                                .border(1.dp, badgeText.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "RISK ${activeScan.riskScore}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = badgeText
                            )
                        }
                    }
                }

                if (asset.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = asset.description, color = MutedGreyText, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceGunmetalLight, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = asset.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightWhiteText
                        )
                    }

                    if (asset.tags.isNotBlank()) {
                        asset.tags.split(",").forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(SurfaceGunmetalLight.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag.trim(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGreyText
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION TABS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Pair("VULNERABILITIES", "ALERTS"),
                Pair("THREAT_INTEL", "THREAT FEEDS"),
                Pair("HISTORY", "HISTORIC AUDITS")
            )

            tabs.forEach { (tabId, tabName) ->
                val isSelected = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) AuditingCrimson.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(100.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) AuditingCrimson else SurfaceGunmetalLight,
                            RoundedCornerShape(100.dp)
                        )
                        .clickable { activeTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        color = if (isSelected) BrightWhiteText else MutedGreyText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- TAB BODY CONTENTS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (activeScan == null) {
                // Asset hasn't been scanned yet
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar Audit",
                            tint = MutedGreyText,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Defensive Audits Executed Yet",
                            color = BrightWhiteText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.startLiveScan(asset, "Vulnerability Sweep") },
                            colors = ButtonDefaults.buttonColors(containerColor = AuditingCrimson),
                            modifier = Modifier.testTag("launch_scan_initial_button")
                        ) {
                            Text("RUN SYSTEM SCAN", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                when (activeTab) {
                    "VULNERABILITIES" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (threatIntel != null) {
                                val reputationColor = when (threatIntel.reputationVerdict.uppercase()) {
                                    "MALICIOUS" -> AuditingCrimson
                                    "SUSPICIOUS" -> AccentAmber
                                    else -> DefendingGreen
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, reputationColor.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(reputationColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = reputationColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "REAL-TIME THREAT INTEL VERDICT",
                                                color = MutedGreyText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = threatIntel.reputationVerdict,
                                                color = reputationColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Abuse Confidence",
                                                color = MutedGreyText,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${threatIntel.abuseScore}%",
                                                color = if (threatIntel.abuseScore > 50) AuditingCrimson else if (threatIntel.abuseScore > 10) AccentAmber else DefendingGreen,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (threatIntel.virusTotalMaliciousCount > 0) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "VT Flags",
                                                    color = MutedGreyText,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "${threatIntel.virusTotalMaliciousCount}",
                                                    color = AuditingCrimson,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (findings.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Clean Status",
                                            tint = DefendingGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Target Configuration Secure",
                                            color = BrightWhiteText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "All passive configuration and SSL header audits passed clean.",
                                            color = MutedGreyText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize().weight(1f)
                                ) {
                                    items(findings) { finding ->
                                        DetailedFindingCard(finding = finding)
                                    }
                                }
                            }
                        }
                    }

                    "THREAT_INTEL" -> {
                        if (threatIntel == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No Threat Intelligence data recorded.", color = MutedGreyText)
                            }
                        } else {
                            ThreatIntelView(intel = threatIntel, viewModel = viewModel)
                        }
                    }

                    "HISTORY" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(assetScans) { historicalScan ->
                                HistoricalScanRow(scan = historicalScan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedFindingCard(finding: ScanFinding) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val severityColor = when (finding.severity.uppercase()) {
        "CRITICAL" -> AuditingCrimson
        "HIGH" -> AuditingCrimson
        "MEDIUM" -> AccentAmber
        else -> DefendingGreen
    }

    // Generate practical devops / server config remediation code snippet
    val codeSnippet = remember(finding) {
        when {
            finding.title.contains("Strict-Transport-Security", ignoreCase = true) || finding.title.contains("HSTS", ignoreCase = true) ->
                "# Nginx Configuration\nadd_header Strict-Transport-Security \"max-age=31536000; includeSubDomains; preload\" always;"
            finding.title.contains("Content-Security-Policy", ignoreCase = true) || finding.title.contains("CSP", ignoreCase = true) ->
                "# Nginx Header Patch\nadd_header Content-Security-Policy \"default-src 'self'; script-src 'self'; object-src 'none';\" always;"
            finding.title.contains("X-Frame-Options", ignoreCase = true) || finding.title.contains("Clickjacking", ignoreCase = true) ->
                "# Security Header Patch\nadd_header X-Frame-Options \"DENY\" always;\nadd_header X-Content-Type-Options \"nosniff\" always;"
            finding.title.contains("TLS", ignoreCase = true) || finding.title.contains("SSL", ignoreCase = true) ->
                "# Nginx Modern TLS Config\nssl_protocols TLSv1.2 TLSv1.3;\nssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;"
            finding.title.contains("Port", ignoreCase = true) || finding.title.contains("Open", ignoreCase = true) ->
                "# Linux UFW Firewall Rule\nsudo ufw deny 21/tcp\nsudo ufw deny 23/tcp\nsudo ufw reload"
            else ->
                "# Remediate ${finding.title}\n# Follow security policy guidelines:\n# ${finding.recommendation}"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
        border = BorderStroke(1.dp, SurfaceGunmetalLight),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(severityColor, RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = finding.title,
                        color = BrightWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Badge(
                    containerColor = severityColor.copy(alpha = 0.15f),
                    contentColor = severityColor
                ) {
                    Text(
                        text = finding.category.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = finding.description, color = MutedGreyText, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SurfaceGunmetalLight)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = DefendingGreen,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Column {
                    Text(
                        text = "RECOMMENDED REMEDIATION:",
                        color = DefendingGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = finding.recommendation,
                        color = BrightWhiteText,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Copyable Remediation Code Patch snippet
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalDark),
                border = BorderStroke(1.dp, BorderMuted),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIG PATCH SNIPPET",
                            color = CyanIntel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(codeSnippet))
                                    Toast.makeText(context, "Remediation patch copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Patch",
                                tint = CyanIntel,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COPY PATCH",
                                color = CyanIntel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = codeSnippet,
                        color = BrightWhiteText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatIntelView(intel: ThreatIntelResult, viewModel: SecurityViewModel) {
    var editKeys by remember { mutableStateOf(false) }

    val customAbuse by viewModel.customAbuseKey.collectAsState()
    val customVt by viewModel.customVtKey.collectAsState()

    var abuseKeyInput by remember { mutableStateOf(customAbuse) }
    var vtKeyInput by remember { mutableStateOf(customVt) }

    val reputationColor = when (intel.reputationVerdict.uppercase()) {
        "MALICIOUS" -> AuditingCrimson
        "SUSPICIOUS" -> AccentAmber
        else -> DefendingGreen
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Reputation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
                border = BorderStroke(1.dp, SurfaceGunmetalLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "THREAT INTEL REPUTATION ANALYSIS",
                        color = BrightWhiteText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "RESOLVED HOST ADDRESS", color = MutedGreyText, fontSize = 10.sp)
                            Text(
                                text = intel.checkedAddress,
                                color = CyanIntel,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Badge(
                            containerColor = reputationColor.copy(alpha = 0.15f),
                            contentColor = reputationColor,
                            modifier = Modifier.border(1.dp, reputationColor, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = intel.reputationVerdict.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SurfaceGunmetalLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // AbuseIPDB Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "ABUSEIPDB CONFIDENCE", color = MutedGreyText, fontSize = 10.sp)
                            Text(
                                text = "${intel.abuseScore}%",
                                color = if (intel.abuseScore > 50) AuditingCrimson else if (intel.abuseScore > 10) AccentAmber else DefendingGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "TOTAL REPORTS RECORDED", color = MutedGreyText, fontSize = 10.sp)
                            Text(
                                text = intel.totalReports.toString(),
                                color = BrightWhiteText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // VirusTotal stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "VIRUSTOTAL MALICIOUS DETECTIONS", color = MutedGreyText, fontSize = 10.sp)
                            Text(
                                text = "${intel.virusTotalMaliciousCount} Engines",
                                color = if (intel.virusTotalMaliciousCount > 0) AuditingCrimson else DefendingGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            tint = reputationColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SurfaceGunmetalLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SENSOR CLOUD DETAILS:",
                        color = MutedGreyText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = intel.scannerDetails,
                        color = BrightWhiteText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
                border = BorderStroke(1.dp, SurfaceGunmetalLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THREAT INTEL CREDENTIALS",
                            color = BrightWhiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )

                        TextButton(
                            onClick = {
                                if (editKeys) {
                                    viewModel.setCustomKeys(abuseKeyInput, vtKeyInput)
                                }
                                editKeys = !editKeys
                            },
                            modifier = Modifier.testTag("edit_keys_toggle")
                        ) {
                            Text(
                                text = if (editKeys) "SAVE" else "EDIT KEYS",
                                color = AuditingCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "To query real API servers instead of local secure sensors, add your API keys here.",
                        color = MutedGreyText,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (editKeys) {
                        OutlinedTextField(
                            value = abuseKeyInput,
                            onValueChange = { abuseKeyInput = it },
                            label = { Text("AbuseIPDB API Key", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("abuse_key_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = vtKeyInput,
                            onValueChange = { vtKeyInput = it },
                            label = { Text("VirusTotal API Key", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vt_key_field")
                        )
                    } else {
                        val hasAbuse = viewModel.abuseIpDbKey.isNotBlank() || customAbuse.isNotBlank()
                        val hasVt = customVt.isNotBlank()

                        CredentialStatusRow(label = "AbuseIPDB Service Integration", isActive = hasAbuse)
                        CredentialStatusRow(label = "VirusTotal Engine Query Provider", isActive = hasVt)
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialStatusRow(label: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = BrightWhiteText, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (isActive) DefendingGreen else MutedGreyText, RoundedCornerShape(3.dp))
            )
            Text(
                text = if (isActive) "ACTIVE" else "PASSIVE DEFAULTS",
                color = if (isActive) DefendingGreen else MutedGreyText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun HistoricalScanRow(scan: Scan) {
    val scoreColor = when (scan.severityLevel) {
        "CRITICAL" -> AuditingCrimson
        "HIGH" -> AuditingCrimson
        "MEDIUM" -> AccentAmber
        else -> DefendingGreen
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
        border = BorderStroke(1.dp, SurfaceGunmetalLight),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = scan.scanType,
                    color = BrightWhiteText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(scan.timestamp)),
                    color = MutedGreyText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "RISK INDEX",
                    color = MutedGreyText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${scan.riskScore}/100",
                    color = scoreColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
