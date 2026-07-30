package tr.org.yesilay.yesiliz.domain.lig

import kotlin.random.Random

/**
 * Davet kodu üretimi — tasarım 36'daki `YSZ-7K2Q` biçimi.
 *
 * **Saf fonksiyon**, `Random` dışarıdan veriliyor: testte belirlenimci
 * olabilsin.
 *
 * Karıştırılabilir karakterler alfabede **yok** (`I`, `O`, `0`, `1`).
 * Kod sesli olarak paylaşılabiliyor ("ye-se-ze tire yedi ka iki ku") ve
 * `O`/`0` ayrımı telefonda anlaşılmıyor.
 */
object DavetKodu {

    const val ONEK = "YSZ"
    const val UZUNLUK = 4

    /** I, O, 0, 1 bilinçli olarak dışarıda. */
    private const val ALFABE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun uret(rastgele: Random = Random.Default): String {
        val govde = (1..UZUNLUK)
            .map { ALFABE[rastgele.nextInt(ALFABE.length)] }
            .joinToString("")
        return "$ONEK-$govde"
    }

    /** Kullanıcının girdiği kodun biçimsel geçerliliği. */
    fun gecerliMi(kod: String): Boolean {
        val temiz = kod.trim().uppercase()
        if (!temiz.startsWith("$ONEK-")) return false
        val govde = temiz.removePrefix("$ONEK-")
        return govde.length == UZUNLUK && govde.all { it in ALFABE }
    }
}
