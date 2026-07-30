package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.ui.graphics.vector.ImageVector

/** Bir ödül basamağının durumu. */
enum class OdulHali { KAZANILDI, AKTIF, KILITLI }

/**
 * Ödüller ekranı — `docs/tasarim-referans.md §2 / 11`.
 *
 * Basamaklar **yukarıdan aşağıya** azalan eşikle diziliyor (tasarımda en üstte
 * 10.000, en altta 500) — kullanıcı nereden geldiğini görüyor.
 */
data class OdulDurum(
    /** Sertifikada görünen ad — hesap oturumundan geliyor. */
    val isim: String = "",
    val puan: Int = 0,
    val sonrakiEsik: Int = 0,
    val basamaklar: List<Odul> = emptyList(),
    val gecmis: List<PuanKaydi> = emptyList(),
) {
    val sonrakineKalan: Int get() = (sonrakiEsik - puan).coerceAtLeast(0)
}

data class Odul(
    val id: String,
    val ad: String,
    val esik: Int,
    val ikon: ImageVector,
    val hal: OdulHali,
    val aciklama: String,
    val bagisTarihi: String? = null,
    val sertifikaNo: String? = null,
)

/**
 * Puan geçmişi satırı — `docs/tasarim-referans.md §2 / 17`.
 *
 * [puan] her zaman pozitif. **Ceza puanı yok** (CLAUDE.md §4): eksi değer
 * suçluluk üretiyor ve uygulamayı sildiriyor.
 */
data class PuanKaydi(
    val baslik: String,
    val zaman: String,
    val puan: Int,
    val ikon: ImageVector,
)
