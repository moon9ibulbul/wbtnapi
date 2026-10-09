package com.astral.wbtn.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.astral.wbtn.api.EpisodeItem
import com.astral.wbtn.api.ProductRightItem
import com.astral.wbtn.api.WebtoonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class Downloader(
    private val context: Context,
    private val repository: WebtoonRepository
) {
    var adsUsed = 0
    var dailyPassUsed = 0

    suspend fun downloadEpisodes(
        seriesNo: Int,
        seriesTitle: String,
        episodes: List<EpisodeItem>,
        rightsMap: Map<Int, ProductRightItem>,
        onLog: (String) -> Unit,
        onProgress: (currentEp: Int, totalEps: Int, currentImg: Int, totalImgs: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val safeSeriesTitle = seriesTitle.filter { it.isLetterOrDigit() || it in " -_" }.trim()
        var coinBalance = repository.getCoinBalance()

        onLog("🚀 STARTING DOWNLOAD...")

        episodes.forEachIndexed { index, episode ->
            val epNo = episode.episodeNo
            onLog("\n--- Evaluating Episode $epNo ---")

            val rightInfo = rightsMap[epNo]
            var hasRight = rightInfo?.hasRight == true
            var isInfinite = rightInfo?.infinite == true
            val isFree = episode.productInfo == null || episode.productInfo == false

            if (!isFree && !hasRight) {
                val liveRight = repository.productRight(seriesNo, epNo)
                if (liveRight != null) {
                    hasRight = liveRight.hasRight
                    isInfinite = liveRight.infinite
                }
            }

            if (isFree || hasRight) {
                if (isFree) onLog("🔓 Free public episode.")
                else if (isInfinite) onLog("🔓 Purchased permanently.")
                else onLog("🔓 Unlocked temporarily.")
            } else {
                onLog("🔒 Episode locked. Checking claim/purchase options...")
                val productData = repository.getProduct(seriesNo, epNo)
                var allowsAd = false
                var allowsDailyPass = false
                var price = 3
                val productId = "linewebtoon-WEBTOON-$seriesNo-$epNo"
                var saleTypeStr = "complete"

                productData?.saleUnitList?.forEach { unit ->
                    when (unit.saleUnitType) {
                        "REWARD_AD" -> allowsAd = true
                        "DAILY_PASS" -> allowsDailyPass = true
                        "PREVIEW", "COMPLETE" -> {
                            price = unit.policyPrice
                            saleTypeStr = unit.saleUnitType.lowercase()
                        }
                    }
                }

                if (allowsAd && adsUsed < 100) {
                    onLog("📺 Simulating Ad view (32s)...")
                    delay(32000)
                    repository.getImageSecureToken()
                    repository.buyProduct(productId, "$productId-reward_ad-1", 0)
                    adsUsed++
                    onLog("✅ Ad claimed successfully.")
                } else if (allowsDailyPass && dailyPassUsed < 1) {
                    repository.buyProduct(productId, "$productId-complete_daily_pass-1", 0)
                    dailyPassUsed++
                    onLog("✅ Daily pass claimed.")
                } else {
                    if (coinBalance in 0..<price) {
                        onLog("❌ Insufficient coins (Have: $coinBalance, Need: $price). Skipping.")
                        return@forEachIndexed
                    }
                    repository.buyProduct(productId, "$productId-$saleTypeStr-1", price)
                    coinBalance = maxOf(0, coinBalance - price)
                    onLog("✅ Episode bought with coins.")
                }
            }

            val epInfo = repository.episodeInfoWithLogin(seriesNo, epNo)
            if (epInfo == null) {
                onLog("⚠️ Error fetching images for episode $epNo")
                return@forEachIndexed
            }

            val safeEpTitle = epInfo.episodeTitle.filter { it.isLetterOrDigit() || it in " -_" }.trim()
            val validImages = epInfo.imageInfo.filter { !it.url.contains("capture_warning") }
            val totalImages = validImages.size

            onLog("📥 Saving in: AstralWBTN/$safeSeriesTitle/$safeEpTitle...")

            validImages.forEachIndexed { imgIdx, imgInfo ->
                onProgress(index + 1, episodes.size, imgIdx + 1, totalImages)
                val imgBytes = repository.getStaticContent(imgInfo.url)
                val filename = "%03d.jpg".format(imgIdx + 1)
                saveImageToStorage(safeSeriesTitle, safeEpTitle, filename, imgBytes)
            }

            onLog("✅ Ep $epNo download completed.")
            delay(1500)
        }

        onLog("\n✅ DOWNLOAD PROCESS FINISHED.")
    }

    private fun saveImageToStorage(
        seriesName: String,
        epName: String,
        filename: String,
        bytes: ByteArray
    ) {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/AstralWBTN/$seriesName/$epName"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            val resolver = context.contentResolver
            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    output.write(bytes)
                }
            }
        } else {
            val targetDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AstralWBTN/$seriesName/$epName")
            if (!targetDir.exists()) targetDir.mkdirs()
            val file = File(targetDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
        }
    }
}
