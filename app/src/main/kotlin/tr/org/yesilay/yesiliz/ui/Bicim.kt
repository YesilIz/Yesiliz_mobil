package tr.org.yesilay.yesiliz.ui

import java.text.NumberFormat
import java.util.Locale

private val TR_BICIM: Locale = Locale.forLanguageTag("tr")

/**
 * Binlik ayraçlı sayı — `3240` → `3.240`.
 *
 * Türkçede binlik ayracı **nokta**, ondalık ayracı virgül. Cihazın yerel
 * ayarına bırakılırsa İngilizce telefonda `3,240` çıkar; tasarımda hep
 * `3.240` yazıyor. Bu yüzden yerel ayar sabitleniyor.
 */
fun Int.binlik(): String = NumberFormat.getIntegerInstance(TR_BICIM).format(this)

/**
 * Puan rozeti metni — `25` → `+25p`.
 *
 * Rozetler yalnızca `+25` yazıyordu ve neyin 25'i olduğu belirsizdi
 * (dakika mı, adım mı, puan mı). `p` eki bunu tek karakterle söylüyor.
 *
 * **Yalnızca rozet/çip için.** Cümle içinde ("25 puan kazandın!") kısaltma
 * kullanılmıyor — orada `p` Türkçeyi bozar ve zaten yer sıkıntısı yok.
 */
fun Int.puanEtiketi(): String = "+${binlik()}p"

/**
 * Dakika → okunur süre. `260` → `4s 20dk`, `45` → `45 dk`, `120` → `2s`.
 *
 * Tasarımda saat/dakika arasında boşluk yok (`4s 20dk`), tek birim varken var
 * (`285 dk`) — ekran 08 ve 09'da böyle.
 */
fun Int.sureMetni(): String {
    val saat = this / 60
    val dakika = this % 60
    return when {
        saat == 0 -> "$dakika dk"
        dakika == 0 -> "${saat}s"
        else -> "${saat}s ${dakika}dk"
    }
}

/**
 * Grafik ekseni için süre — bir saatin ALTINI da gösterir.
 *
 * Selefi tam bölme yaptığı için 8 dakikayı "0s" yazıyordu; ekseni verinin
 * kendi ölçeğinden türetince (grafiğin tepesi 8 dakika olabiliyor) üç
 * etiket birden "0s" çıkıyordu (CLAUDE.md §12 K49).
 */
fun Int.eksenSuresi(): String = when {
    this <= 0 -> "0"
    this < 60 -> "${this}dk"
    else -> "${this / 60}s"
}

/** Grafik ekseni için kısa adım: `8000` → `8b`. */
fun Int.kisaBin(): String = "${this / 1000}b"

/**
 * Geri sayım metni — `725` → `12:05`.
 *
 * Odak seansı, aktif yürüyüş ve nefes egzersizi aynı ifadeyi üç ayrı yerde
 * taşıyordu; ikisi karakteri karakterine aynıydı (CLAUDE.md §12 K49 ailesi:
 * biçim kararı tek yerde yaşar).
 */
fun Int.sayacMetni(): String = "%d:%02d".format(this / SANIYE_DAKIKA, this % SANIYE_DAKIKA)

private const val SANIYE_DAKIKA = 60
