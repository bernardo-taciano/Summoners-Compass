import com.google.gson.annotations.SerializedName

data class ChampionResponse(
    @SerializedName("data")
    val data: Map<String, Champion> // No change needed here
)

data class Champion(
    @SerializedName("id") val id: String,
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("title") val title: String,
    @SerializedName("blurb") val blurb: String,
    @SerializedName("info") val info: ChampionInfo,
    @SerializedName("image") val image: ChampionImage,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("partype") val partype: String,
    @SerializedName("stats") val stats: ChampionStats
)

data class ChampionImage(
    @SerializedName("full") val full: String,
    @SerializedName("sprite") val sprite: String,
    @SerializedName("group") val group: String,
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
    @SerializedName("w") val w: Int,
    @SerializedName("h") val h: Int
)

data class ChampionInfo(
    @SerializedName("attack") val attack: Int,
    @SerializedName("defense") val defense: Int,
    @SerializedName("magic") val magic: Int,
    @SerializedName("difficulty") val difficulty: Int
)

data class ChampionStats(
    @SerializedName("hp") val hp: Int,
    @SerializedName("hpperlevel") val hpperlevel: Double,
    @SerializedName("mp") val mp: Int,
    @SerializedName("mpperlevel") val mpperlevel: Double,
    @SerializedName("movespeed") val movespeed: Int,
    @SerializedName("armor") val armor: Int,
    @SerializedName("armorperlevel") val armorperlevel: Double,
    @SerializedName("spellblock") val spellblock: Int,
    @SerializedName("spellblockperlevel") val spellblockperlevel: Double,
    @SerializedName("attackrange") val attackrange: Int,
    @SerializedName("hpregen") val hpregen: Double,
    @SerializedName("hpregenperlevel") val hpregenperlevel: Double,
    @SerializedName("mpregen") val mpregen: Double,
    @SerializedName("mpregenperlevel") val mpregenperlevel: Double,
    @SerializedName("crit") val crit: Double,
    @SerializedName("critperlevel") val critperlevel: Double,
    @SerializedName("attackdamage") val attackdamage: Int,
    @SerializedName("attackdamageperlevel") val attackdamageperlevel: Double,
    @SerializedName("attackspeedperlevel") val attackspeedperlevel: Double,
    @SerializedName("attackspeed") val attackspeed: Double
)
