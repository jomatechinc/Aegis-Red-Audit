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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Asset
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssetListScreen(
    viewModel: SecurityViewModel,
    onNavigateToAsset: (Asset) -> Unit
) {
    val assets by viewModel.assets.collectAsState()
    val scans by viewModel.scans.collectAsState()
    val isScanningId by viewModel.isScanning.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var assetToEdit by remember { mutableStateOf<Asset?>(null) }

    var targetName by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("") }
    var targetDescription by remember { mutableStateOf("") }
    var targetCategory by remember { mutableStateOf("Web Application") }
    var targetTags by remember { mutableStateOf("") }

    // Reset fields helper
    val resetForm = {
        targetName = ""
        targetUrl = ""
        targetDescription = ""
        targetCategory = "Web Application"
        targetTags = ""
        assetToEdit = null
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    showAddDialog = true
                },
                containerColor = AuditingCrimson,
                contentColor = BrightWhiteText,
                modifier = Modifier.testTag("add_asset_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Target Asset")
            }
        },
        containerColor = TacticalDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TacticalDark)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "AUDIT TARGETS",
                        color = BrightWhiteText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Manage targets and trigger defensive vulnerability audits",
                        color = MutedGreyText,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = AuditingCrimson,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (assets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "No Assets",
                            tint = MutedGreyText,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Audit Assets Configured",
                            color = BrightWhiteText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the [+] button to add your first website target.",
                            color = MutedGreyText,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(assets) { asset ->
                        // Get latest scan for this asset to display severity badge on card
                        val latestScan = scans.filter { it.assetId == asset.id }
                            .maxByOrNull { it.timestamp }

                        AssetCard(
                            asset = asset,
                            latestSeverity = latestScan?.severityLevel,
                            latestRiskScore = latestScan?.riskScore,
                            isScanning = isScanningId == asset.id,
                            onCardClick = { onNavigateToAsset(asset) },
                            onEditClick = {
                                assetToEdit = asset
                                targetName = asset.name
                                targetUrl = asset.url
                                targetDescription = asset.description
                                targetCategory = asset.category
                                targetTags = asset.tags
                                showAddDialog = true
                            },
                            onDeleteClick = { viewModel.deleteAsset(asset) },
                            onScanClick = {
                                viewModel.startLiveScan(asset, "Vulnerability Sweep")
                            }
                        )
                    }
                }
            }
        }

        // --- Add/Edit Dialog ---
        if (showAddDialog) {
            val isEdit = assetToEdit != null
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    resetForm()
                },
                title = {
                    Text(
                        text = if (isEdit) "EDIT AUDIT TARGET" else "ADD AUDIT TARGET",
                        color = BrightWhiteText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                containerColor = SurfaceGunmetal,
                shape = RoundedCornerShape(12.dp),
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            label = { Text("Target Name", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("asset_name_input")
                        )

                        OutlinedTextField(
                            value = targetUrl,
                            onValueChange = { targetUrl = it },
                            label = { Text("URL / Host IP Address", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            placeholder = { Text("example.com or 8.8.8.8", color = MutedGreyText.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("asset_url_input")
                        )

                        OutlinedTextField(
                            value = targetDescription,
                            onValueChange = { targetDescription = it },
                            label = { Text("Description", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Category selection
                        Column {
                            Text(
                                text = "CATEGORY",
                                color = MutedGreyText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val categories = listOf("Web Application", "API Server", "Cloud Host", "Internal Target")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = targetCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) AuditingCrimson.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) AuditingCrimson else SurfaceGunmetalLight,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { targetCategory = cat }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) BrightWhiteText else MutedGreyText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = targetTags,
                            onValueChange = { targetTags = it },
                            label = { Text("Tags (comma separated)", color = MutedGreyText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuditingCrimson,
                                unfocusedBorderColor = SurfaceGunmetalLight,
                                focusedTextColor = BrightWhiteText,
                                unfocusedTextColor = BrightWhiteText
                            ),
                            placeholder = { Text("production, public, aws", color = MutedGreyText.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (targetName.isNotBlank() && targetUrl.isNotBlank()) {
                                if (isEdit) {
                                    val currentId = assetToEdit?.id ?: 0
                                    viewModel.updateAsset(
                                        Asset(
                                            id = currentId,
                                            name = targetName,
                                            url = targetUrl,
                                            description = targetDescription,
                                            category = targetCategory,
                                            tags = targetTags
                                        )
                                    )
                                } else {
                                    viewModel.addAsset(
                                        name = targetName,
                                        url = targetUrl,
                                        description = targetDescription,
                                        category = targetCategory,
                                        tags = targetTags
                                    )
                                }
                                showAddDialog = false
                                resetForm()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuditingCrimson),
                        modifier = Modifier.testTag("submit_asset_button")
                    ) {
                        Text("SAVE TARGET", color = BrightWhiteText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        resetForm()
                    }) {
                        Text("CANCEL", color = MutedGreyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun AssetCard(
    asset: Asset,
    latestSeverity: String?,
    latestRiskScore: Int?,
    isScanning: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val badgeColor = when (latestSeverity?.uppercase()) {
        "CRITICAL" -> AuditingCrimson
        "HIGH" -> AuditingCrimson
        "MEDIUM" -> AccentAmber
        "LOW" -> DefendingGreen
        "CLEAN" -> DefendingGreen
        else -> MutedGreyText
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGunmetal),
        border = BorderStroke(1.dp, BorderMuted),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("asset_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main content row with category circle icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category round icon box (Tailwind: h-10 w-10 rounded-full bg-[#1A1C1E] border border-[#44474E])
                val icon = when (asset.category.lowercase()) {
                    "web application", "web app" -> Icons.Default.Language
                    "api server", "api" -> Icons.Default.Hub
                    "cloud host", "cloud" -> Icons.Default.Cloud
                    else -> Icons.Default.Dns
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGunmetalLight, RoundedCornerShape(100.dp))
                        .border(1.dp, BorderMuted, RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Category Icon",
                        tint = CyanIntel,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Info columns
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name,
                        color = BrightWhiteText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = asset.url,
                        color = MutedGreyText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // High contrast badge exactly like px-3 py-1 bg-[#93000A] text-[#FFDAD6]
                if (latestSeverity != null) {
                    val isHighAlert = latestSeverity == "CRITICAL" || latestSeverity == "HIGH"
                    val labelBg = if (isHighAlert) DarkCrimson else SurfaceGunmetalLight
                    val labelText = if (isHighAlert) AuditingCrimson else DefendingGreen
                    Box(
                        modifier = Modifier
                            .background(labelBg, RoundedCornerShape(100.dp))
                            .border(1.dp, labelText.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = latestSeverity.uppercase(),
                            color = labelText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(SurfaceGunmetalLight.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "UNSCANNED",
                            color = MutedGreyText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (asset.description.isNotBlank()) {
                Text(
                    text = asset.description,
                    color = MutedGreyText,
                    fontSize = 11.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tags and actions footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display up to 2 tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (asset.tags.isNotBlank()) {
                        asset.tags.split(",").take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(SurfaceGunmetalLight.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tag.trim(),
                                    color = MutedGreyText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Target",
                            tint = MutedGreyText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Target",
                            tint = AuditingCrimson.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) SurfaceGunmetalLight else AuditingCrimson
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("scan_trigger_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = AuditingCrimson,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = BrightWhiteText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "SCAN",
                                    color = if (isScanning) MutedGreyText else BrightWhiteText,
                                    fontSize = 10.sp,
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
