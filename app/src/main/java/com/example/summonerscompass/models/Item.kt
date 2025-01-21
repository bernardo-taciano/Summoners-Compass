import com.google.gson.annotations.SerializedName

data class ItemResponse(
    @SerializedName("data")
    val data: Map<String, Item>
)

data class Item(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("colloq") val colloq: String,
    @SerializedName("plaintext") val plaintext: String,
    @SerializedName("into") val into: List<String>?,
    @SerializedName("image") val image: ItemImage,
    @SerializedName("gold") val gold: ItemGold,
    @SerializedName("tags") val tags: List<String>
)

data class ItemImage(
    @SerializedName("full") val full: String,
    @SerializedName("sprite") val sprite: String,
    @SerializedName("group") val group: String,
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
    @SerializedName("w") val w: Int,
    @SerializedName("h") val h: Int
)

data class ItemGold(
    @SerializedName("base") val base: Int,
    @SerializedName("purchasable") val purchasable: Boolean,
    @SerializedName("total") val total: Int,
    @SerializedName("sell") val sell: Int
)
