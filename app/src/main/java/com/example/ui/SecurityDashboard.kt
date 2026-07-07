package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun SecurityDashboard(
    viewModel: SecurityViewModel,
    onNavigateToAsset: (Asset) -> Unit,
    onNavigateToAssetList: () -> Unit
) {
    val assets by viewModel.assets.collectAsState()
    val scans by viewModel.scans.collectAsState()

    var selectedSeverityFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedTargetFilter by remember { mutableStateOf("ALL") }

    // Aggregate statistics
    val totalAssets = assets.size
    val totalScans = scans.size
    val criticalScans = scans.filter { it.severityLevel == "CRITICAL" || it.severityLevel == "HIGH" }.size

    val avgRiskScore = if (scans.isNotEmpty()) {
        scans.map { it.riskScore }.average().toInt()
    } else {
        0
    }

    // Extract all findings across all scans for our visual feed
    val converters = SecurityTypeConverters()
    val allFindingsWithAssets = remember(scans, assets) {
        scans.flatMap { scan ->
            val asset = assets.find { it.id == scan.assetId }
            val findingsList = converters.toFindingsList(scan.findingsJson)
            findingsList.map { finding -> Pair(finding, asset) }
        }
    }

    val targetOptions = remember(assets) {
        listOf("ALL") + assets.map { it.name }.distinct()
    }

    val categoryOptions = remember(allFindingsWithAssets) {
        listOf("ALL") + allFindingsWithAssets.map { it.first.category }.distinct()
    }

    val filteredFindings = remember(allFindingsWithAssets, selectedSeverityFilter, selectedCategoryFilter, selectedTargetFilter) {
        allFindingsWithAssets.filter { (finding, asset) ->
            val severityMatch = selectedSeverityFilter == "ALL" || finding.severity.equals(selectedSeverityFilter, ignoreCase = true)
            val categoryMatch = selectedCategoryFilter == "ALL" || finding.category.equals(selectedCategoryFilter, ignoreCase = true)
            val targetMatch = selectedTargetFilter == "ALL" || (asset != null && asset.name.equals(selectedTargetFilter, ignoreCase = true))
            severityMatch && categoryMatch && targetMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // --- HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OPERATION CONSOLE",
                        color = MutedGreyText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AegisRed ",
                            color = BrightWhiteText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Nexus",
                            color = AuditingCrimson,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // Decorative Shield with Heart icon wrapper from design HTML
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(SurfaceGunmetalLight, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderMuted, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Icon",
                        tint = CyanIntel,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // --- QUICK SCAN INPUT FEED ---
        item {
            var urlInput by remember { mutableStateOf("") }
            val isScanningId by viewModel.isScanning.collectAsState()
            val activeScanningAsset = remember(isScanningId, assets) {
                assets.find { it.id == isScanningId }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
                border = BorderStroke(1.dp, BorderMuted),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_scan_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "LAUNCH ON-DEMAND SWEEP",
                        color = BrightWhiteText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Input a website URL to execute real-time threat intelligence and configuration sweeps.",
                        color = MutedGreyText,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("e.g. example.com, 8.8.8.8", color = MutedGreyText, fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_scan_url_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText,
                                focusedBorderColor = CyanIntel,
                                unfocusedBorderColor = BorderMuted,
                                focusedContainerColor = TacticalDark,
                                unfocusedContainerColor = TacticalDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = CyanIntel,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    val cleanedUrl = urlInput.trim()
                                    var name = cleanedUrl
                                        .replace("https://", "")
                                        .replace("http://", "")
                                    val slashIdx = name.indexOf('/')
                                    if (slashIdx != -1) {
                                        name = name.substring(0, slashIdx)
                                    }
                                    
                                    val existingAsset = assets.find { it.url.equals(cleanedUrl, ignoreCase = true) || it.url.equals(name, ignoreCase = true) }
                                    if (existingAsset != null) {
                                        viewModel.startLiveScan(existingAsset, "Vulnerability Sweep")
                                    } else {
                                        val newAsset = Asset(
                                            name = "Target: $name",
                                            url = cleanedUrl,
                                            description = "On-demand real-time scanned target.",
                                            category = "Web Application",
                                            tags = "on-demand,scan-feed"
                                        )
                                        viewModel.addAndScanAsset(newAsset, "Vulnerability Sweep")
                                    }
                                    urlInput = ""
                                }
                            },
                            enabled = isScanningId == null && urlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuditingCrimson,
                                contentColor = BrightWhiteText,
                                disabledContainerColor = SurfaceGunmetalLight,
                                disabledContentColor = MutedGreyText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("quick_scan_button")
                        ) {
                            Text(
                                text = "SCAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (activeScanningAsset != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalDark),
                            border = BorderStroke(1.dp, AuditingCrimson.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AuditingCrimson,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "AUDITING TARGET...",
                                        color = AuditingCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = activeScanningAsset.name,
                                        color = BrightWhiteText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Performing live DNS resolution, TLS certificate probing, HTTP headers vulnerability audit, and real-time AbuseIPDB & VirusTotal threat intelligence checks...",
                                    color = MutedGreyText,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- STATS OVERVIEW GRIDS ---
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                val formattedAssets = String.format("%02d", totalAssets)
                val formattedScans = String.format("%02d", totalScans)
                val formattedCriticals = String.format("%02d", criticalScans)

                // Total Assets Card
                StatCard(
                    title = "Target Assets",
                    value = formattedAssets,
                    icon = Icons.Default.Dns,
                    color = CyanIntel,
                    progress = 0.5f,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAssetList() }
                )
                // Total Scans Card
                StatCard(
                    title = "Active Scans",
                    value = formattedScans,
                    icon = Icons.Default.Radar,
                    color = CyanIntel,
                    progress = 0.75f,
                    modifier = Modifier.weight(1f)
                )
                // Critical Finds
                StatCard(
                    title = "Threats Found",
                    value = formattedCriticals,
                    icon = Icons.Default.Warning,
                    color = AuditingCrimson,
                    isThreatStyle = true,
                    footerText = if (criticalScans > 0) "High Risk Alerts" else "Passed Safe",
                    modifier = Modifier.weight(1f)
                )
                // Risk Index
                val riskColor = if (avgRiskScore > 50) AuditingCrimson else if (avgRiskScore > 20) AccentAmber else DefendingGreen
                StatCard(
                    title = "Avg Risk Score",
                    value = "$avgRiskScore%",
                    icon = Icons.Default.Speed,
                    color = riskColor,
                    isThreatStyle = avgRiskScore > 50,
                    footerText = if (avgRiskScore > 50) "Critical Level" else "Acceptable Risk",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- TREND ANALYSIS CHART ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
                border = BorderStroke(1.dp, SurfaceGunmetalLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HISTORICAL RISK INDEX TREND",
                                color = BrightWhiteText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Aggregate threat vulnerability fluctuations",
                                color = MutedGreyText,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trend Icon",
                            tint = AuditingCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scans.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Insufficient historical audit records to plot trend.\nPerform more asset scans.",
                                color = MutedGreyText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        SecurityTrendGraph(scans = scans)
                    }
                }
            }
        }

        // --- RECENT SCANS SUMMARY TABLE ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
                border = BorderStroke(1.dp, BorderMuted),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RECENT DEFENSIVE AUDITS",
                                color = BrightWhiteText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tactical view of latest configuration sweeps",
                                color = MutedGreyText,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyanIntel,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (scans.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No scans completed yet. Try on-demand sweep above.",
                                color = MutedGreyText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        val recentScansList = scans.sortedByDescending { it.timestamp }.take(4)
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recentScansList.forEach { scan ->
                                val targetAsset = assets.find { it.id == scan.assetId }
                                val statusColor = when (scan.severityLevel.uppercase()) {
                                    "CRITICAL" -> AuditingCrimson
                                    "HIGH" -> AuditingCrimson
                                    "MEDIUM" -> AccentAmber
                                    else -> DefendingGreen
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TacticalDark, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (targetAsset != null) onNavigateToAsset(targetAsset)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Status Circle
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusColor, RoundedCornerShape(100.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetAsset?.name ?: "Unknown Target",
                                            color = BrightWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = scan.scanType,
                                                color = MutedGreyText,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .background(BorderMuted, RoundedCornerShape(100.dp))
                                            )
                                            Text(
                                                text = dateFormat.format(Date(scan.timestamp)),
                                                color = MutedGreyText,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    // Risk Score badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (scan.riskScore > 50) DarkCrimson else SurfaceGunmetalLight,
                                                RoundedCornerShape(100.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "RISK ${scan.riskScore}%",
                                            color = statusColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FILTER ACTION BAR ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VULNERABILITY ALERTS FEED",
                        color = BrightWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Filtered (${filteredFindings.size})",
                        color = MutedGreyText,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Filters Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Severity Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val severities = listOf("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW")
                        severities.forEach { severity ->
                            val isSelected = selectedSeverityFilter == severity
                            val chipColor = when (severity) {
                                "CRITICAL" -> AuditingCrimson
                                "HIGH" -> AuditingCrimson
                                "MEDIUM" -> AccentAmber
                                "LOW" -> DefendingGreen
                                else -> MutedGreyText
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) chipColor.copy(alpha = 0.25f) else Color.Transparent,
                                        RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) chipColor else BorderMuted,
                                        RoundedCornerShape(100.dp)
                                    )
                                    .clickable { selectedSeverityFilter = severity }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = severity,
                                    color = if (isSelected) BrightWhiteText else MutedGreyText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Row 2: Target & Category Dropdown selectors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Target Selector
                        Box(modifier = Modifier.weight(1f)) {
                            var expanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceGunmetal, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("FILTER BY TARGET", color = MutedGreyText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(selectedTargetFilter.uppercase(), color = BrightWhiteText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MutedGreyText, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(SurfaceGunmetalLight)
                                ) {
                                    targetOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = BrightWhiteText, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                            onClick = {
                                                selectedTargetFilter = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Category Selector
                        Box(modifier = Modifier.weight(1f)) {
                            var expanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceGunmetal, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("VULNERABILITY TYPE", color = MutedGreyText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(selectedCategoryFilter.uppercase(), color = BrightWhiteText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MutedGreyText, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(SurfaceGunmetalLight)
                                ) {
                                    categoryOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = BrightWhiteText, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                            onClick = {
                                                selectedCategoryFilter = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FEED ITEMS ---
        if (filteredFindings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, SurfaceGunmetalLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Safe Shield",
                                tint = DefendingGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Zero vulnerable findings matched for current filters.",
                                color = BrightWhiteText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan more website assets to collect real threat analytics logs.",
                                color = MutedGreyText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredFindings) { (finding, asset) ->
                VulnerabilityAlertRow(finding = finding, asset = asset, onNavigateToAsset = {
                    if (asset != null) onNavigateToAsset(asset)
                })
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isThreatStyle: Boolean = false,
    progress: Float? = null,
    footerText: String? = null
) {
    val containerBg = if (isThreatStyle) DarkThreatBg else SurfaceGunmetal
    val borderStrokeColor = if (isThreatStyle) DarkCrimson else BorderMuted

    Card(
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.2.dp, borderStrokeColor),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = if (isThreatStyle) AuditingCrimson else MutedGreyText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = color,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            )
            
            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                // Beautiful rounded linear progress indicator matching Tailwind style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(SurfaceGunmetalLight, RoundedCornerShape(100.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(100.dp))
                    )
                }
            } else if (footerText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isThreatStyle) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = footerText,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }
}

@Composable
fun SecurityTrendGraph(scans: List<Scan>) {
    // Sort scans by timestamp ascending
    val sortedScans = remember(scans) {
        scans.sortedBy { it.timestamp }.takeLast(6)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Pad the graph with mock historical points to ensure there are always 6 beautiful bars
            val displayBars = remember(sortedScans) {
                val list = mutableListOf<Pair<Int, String>>()
                val missingCount = 6 - sortedScans.size
                if (missingCount > 0) {
                    // Add decorative standard height bars like the HTML design
                    val defaultHeights = listOf(40, 65, 50, 85, 60, 95)
                    for (i in 0 until missingCount) {
                        list.add(Pair(defaultHeights[i % defaultHeights.size], "MOCK"))
                    }
                }
                sortedScans.forEach { scan ->
                    list.add(Pair(scan.riskScore, scan.severityLevel))
                }
                list.takeLast(6)
            }

            displayBars.forEach { (riskScore, severity) ->
                // Color mapping: standard grey, peak purple, or threat red
                val barColor = when {
                    severity == "MOCK" && riskScore == 85 -> CyanIntel
                    severity == "MOCK" && riskScore == 95 -> AuditingCrimson
                    severity == "CRITICAL" || severity == "HIGH" || riskScore >= 75 -> AuditingCrimson
                    severity == "MEDIUM" || riskScore >= 40 -> AccentAmber
                    else -> SurfaceGunmetalLight // #2F3033 / #353439 style
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 5.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Height percentage (coerce to ensure valid height)
                    val heightPercent = (riskScore.toFloat() / 100f).coerceIn(0.15f, 1.0f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightPercent)
                            .background(
                                color = barColor,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time indicators at the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            if (sortedScans.isNotEmpty()) {
                val cols = 6
                val missing = cols - sortedScans.size
                for (i in 0 until missing) {
                    Text(
                        text = "HIST-${i+1}",
                        color = MutedGreyText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                sortedScans.forEach { scan ->
                    Text(
                        text = dateFormat.format(Date(scan.timestamp)),
                        color = MutedGreyText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(text = "Hist-01", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Hist-02", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Hist-03", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Hist-04", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Hist-05", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Now", color = MutedGreyText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun VulnerabilityAlertRow(
    finding: ScanFinding,
    asset: Asset?,
    onNavigateToAsset: () -> Unit
) {
    val alertColor = when (finding.severity.uppercase()) {
        "CRITICAL" -> AuditingCrimson
        "HIGH" -> AuditingCrimson
        "MEDIUM" -> AccentAmber
        else -> DefendingGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
        border = BorderStroke(1.dp, SurfaceGunmetalLight),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAsset() }
            .testTag("vuln_alert_row")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Severity left-edge bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(alertColor, RoundedCornerShape(2.dp))
                    .align(Alignment.CenterVertically)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = finding.title,
                        color = BrightWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = alertColor.copy(alpha = 0.15f),
                        contentColor = alertColor,
                        modifier = Modifier.border(1.dp, alertColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = finding.severity,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = finding.description,
                    color = MutedGreyText,
                    fontSize = 11.sp,
                    maxLines = 2
                )

                if (asset != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = CyanIntel,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${asset.name} (${asset.url})",
                            color = CyanIntel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
