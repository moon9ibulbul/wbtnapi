package com.astral.wbtn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astral.wbtn.api.EpisodeItem
import com.astral.wbtn.api.ProductRightItem
import com.astral.wbtn.api.TitleInfoDetail
import com.astral.wbtn.api.WebtoonRepository
import com.astral.wbtn.download.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: WebtoonRepository,
    downloader: Downloader
) {
    val coroutineScope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var coinBalance by remember { mutableIntStateOf(-1) }
    var showLoginDialog by remember { mutableStateOf(false) }

    var urlInput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var seriesTitle by remember { mutableStateOf("") }
    var seriesNo by remember { mutableIntStateOf(0) }

    var episodeList by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    val selectedEpisodes = remember { mutableStateMapOf<Int, Boolean>() }
    var rightsMap by remember { mutableStateOf<Map<Int, ProductRightItem>>(emptyMap()) }

    var rangeInput by remember { mutableStateOf("") }

    var consoleLogs by remember { mutableStateOf("AstralWBTN Console Ready.\n") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgressText by remember { mutableStateOf("") }

    fun appendLog(msg: String) {
        consoleLogs += "$msg\n"
    }

    fun extractSeriesNo(input: String): Int? {
        val trimmed = input.trim()
        if (trimmed.all { it.isDigit() }) return trimmed.toIntOrNull()
        val regex = Regex("""title_no=(\d+)""")
        val match = regex.find(trimmed)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AstralWBTN", fontWeight = FontWeight.Bold, color = Color(0xFF00DC64))
                        if (isLoggedIn) {
                            Text("🟢 $username", fontSize = 14.sp)
                            if (coinBalance >= 0) {
                                Text("🪙 $coinBalance", fontSize = 14.sp, color = Color(0xFFFFD700))
                            }
                        } else {
                            Text("🔴 Not Logged In", fontSize = 14.sp, color = Color.Red)
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { showLoginDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text(if (isLoggedIn) "Account" else "Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL or Series ID (e.g. 2154)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parsedNo = extractSeriesNo(urlInput)
                        if (parsedNo == null) {
                            appendLog("❌ Invalid URL or Series ID.")
                            return@Button
                        }
                        seriesNo = parsedNo
                        isAnalyzing = true
                        appendLog("\nAnalysing series ID: $seriesNo...")

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val info = repository.titleInfo(seriesNo)
                                if (info == null) {
                                    withContext(Dispatchers.Main) {
                                        appendLog("❌ Series not found.")
                                        isAnalyzing = false
                                    }
                                    return@launch
                                }

                                seriesTitle = info.titleName ?: info.title ?: "Series $seriesNo"
                                val rights = repository.productRightList(seriesNo)
                                val rMap = rights.associateBy { it.episodeNo }

                                val publicEps = repository.episodeList(seriesNo, info.totalEpisodeCount)
                                val knownNos = publicEps.map { it.episodeNo }.toSet()

                                val missingNos = rights.map { it.episodeNo }.filter { it !in knownNos }
                                val premiumEps = mutableListOf<EpisodeItem>()

                                for (epNo in missingNos) {
                                    val prodData = repository.getProduct(seriesNo, epNo)
                                    var allowsAd = false
                                    var price = 0
                                    val title = prodData?.episodeTitle ?: "Episode $epNo"

                                    prodData?.saleUnitList?.forEach { unit ->
                                        if (unit.saleUnitType == "REWARD_AD") allowsAd = true
                                        else if (unit.saleUnitType in listOf("PREVIEW", "COMPLETE")) price = unit.policyPrice
                                    }

                                    premiumEps.add(
                                        EpisodeItem(
                                            episodeNo = epNo,
                                            episodeTitle = title,
                                            productInfo = true,
                                            allowsAd = allowsAd,
                                            price = price
                                        )
                                    )
                                }

                                val allEps = (publicEps + premiumEps).sortedByDescending { it.episodeNo }

                                withContext(Dispatchers.Main) {
                                    rightsMap = rMap
                                    episodeList = allEps
                                    selectedEpisodes.clear()
                                    appendLog("✅ Analysis complete. Found ${allEps.size} episodes.")
                                    isAnalyzing = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    appendLog("❌ Error: ${e.message}")
                                    isAnalyzing = false
                                }
                            }
                        }
                    },
                    enabled = !isAnalyzing && isLoggedIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00DC64))
                ) {
                    Text("Analyze")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (episodeList.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = rangeInput,
                        onValueChange = { rangeInput = it },
                        label = { Text("Quick Select (e.g. 1-5, 8, 10)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = {
                        val set = RangeParser.parseRangeString(rangeInput)
                        episodeList.forEach { ep ->
                            if (ep.episodeNo in set) {
                                selectedEpisodes[ep.episodeNo] = true
                            }
                        }
                        appendLog("Selected range: ${set.sorted()}")
                    }) {
                        Text("Select")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            selectedEpisodes.clear()
                            rangeInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(8.dp)
                ) {
                    items(episodeList) { ep ->
                        val isChecked = selectedEpisodes[ep.episodeNo] ?: false
                        val right = rightsMap[ep.episodeNo]

                        val statusText = when {
                            ep.productInfo != true -> "[FREE]"
                            right?.hasRight == true -> if (right.infinite) "[PURCHASED]" else "[UNLOCKED Temp]"
                            ep.allowsAd && ep.price > 0 -> "[PAID: ${ep.price}c / ADS]"
                            ep.allowsAd -> "[ONLY ADS/PASS]"
                            else -> "[PAID: ${ep.price}c]"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { selectedEpisodes[ep.episodeNo] = it }
                            )
                            Text(
                                text = "Ep ${ep.episodeNo} - ${ep.episodeTitle} $statusText",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val toDownload = episodeList.filter { selectedEpisodes[it.episodeNo] == true }
                        if (toDownload.isEmpty()) {
                            appendLog("⚠️ No episodes selected.")
                            return@Button
                        }

                        isDownloading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            downloader.downloadEpisodes(
                                seriesNo = seriesNo,
                                seriesTitle = seriesTitle,
                                episodes = toDownload,
                                rightsMap = rightsMap,
                                onLog = { log ->
                                    coroutineScope.launch(Dispatchers.Main) { appendLog(log) }
                                },
                                onProgress = { curEp, totEp, curImg, totImg ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        downloadProgressText = "Ep $curEp/$totEp | Image $curImg/$totImg"
                                    }
                                }
                            )
                            val updatedBalance = repository.getCoinBalance()
                            withContext(Dispatchers.Main) {
                                coinBalance = updatedBalance
                                isDownloading = false
                                downloadProgressText = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00DC64))
                ) {
                    Text(if (isDownloading) "Downloading... $downloadProgressText" else "📥 DOWNLOAD SELECTED")
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Enter URL or ID and click Analyze to view episodes.", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Console Log:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                Text(consoleLogs, color = Color.Green, fontSize = 11.sp)
            }
        }
    }

    if (showLoginDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoggingIn by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoggingIn) showLoginDialog = false },
            title = { Text("Login to LINE Webtoon") },
            text = {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingIn = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = repository.login(email, password)
                            if (success) {
                                val info = repository.getMemberInfo()
                                val balance = repository.getCoinBalance()
                                withContext(Dispatchers.Main) {
                                    isLoggedIn = true
                                    username = info?.nickname ?: "User"
                                    coinBalance = balance
                                    appendLog("✅ Logged in as $username")
                                    isLoggingIn = false
                                    showLoginDialog = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    appendLog("❌ Login failed.")
                                    isLoggingIn = false
                                }
                            }
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text(if (isLoggingIn) "Logging in..." else "Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
