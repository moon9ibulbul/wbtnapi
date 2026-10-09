package com.astral.wbtn.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class WebtoonApiResponse<T>(
    val message: ApiMessage<T>?
)

data class ApiMessage<T>(
    val result: T?,
    @SerializedName("error_code") val errorCode: String?,
    val message: String?
)

data class RsaKeyResult(
    val keyName: String,
    val nvalue: String,
    val evalue: String,
    val sessionKey: String
)

data class MemberInfoResult(
    val nickname: String,
    val email: String?
)

data class TitleInfoWrapper(
    val titleInfo: TitleInfoDetail
)

data class TitleInfoDetail(
    val titleNo: Int,
    val title: String?,
    val titleName: String?,
    val totalEpisodeCount: Int
)

data class EpisodeListWrapper(
    val episodeList: EpisodeListContent
)

data class EpisodeListContent(
    val episode: List<EpisodeItem>
)

data class EpisodeItem(
    val episodeNo: Int,
    val episodeTitle: String,
    val productInfo: Boolean? = null,
    var allowsAd: Boolean = false,
    var price: Int = 0
)

data class ProductRightListWrapper(
    val rightList: List<ProductRightItem>?
)

data class ProductRightItem(
    val episodeNo: Int,
    val hasRight: Boolean,
    val infinite: Boolean
)

data class ProductWrapper(
    val product: ProductDetail?
)

data class ProductDetail(
    val episodeTitle: String?,
    val saleUnitList: List<SaleUnit>?
)

data class SaleUnit(
    val saleUnitType: String?,
    val policyPrice: Int = 0
)

data class CoinBalanceWrapper(
    val balance: CoinBalance?
)

data class CoinBalance(
    val amount: Int
)

data class EpisodeInfoWrapper(
    val episodeInfo: EpisodeInfoDetail?
)

data class EpisodeInfoDetail(
    val episodeTitle: String,
    val imageInfo: List<ImageInfo>
)

data class ImageInfo(
    val url: String
)
