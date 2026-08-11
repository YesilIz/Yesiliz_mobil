package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bir ödül basamağının durumu.
 *
 * [SECIM_BEKLIYOR]: eşik ve gün koşulu geçildi ama kademenin birden çok
 * seçeneği var (örn. "Market kuponu" / "Fidan bağışı") ve kullanıcı henüz
 * birini seçmedi. Seçim yapılınca [KAZANILDI]'ya döner.
 */
enum class OdulHali { KAZANILDI, PLANLANAN, AKTIF, SECIM_BEKLIYOR, KILITLI }

/**
 * Ödüller ekranı — `docs/tasarim-referans.md §2 / 11`.
 *
 * Basamaklar **yukarıdan aşağıya** azalan eşikle diziliyor (tasarımda en üstte
 * 10.000, en altta 500) — kullanıcı nereden geldiğini görüyor.
 */
data class OdulDurum(
    /** Uygulama içi başarı kartında görünen ad — hesap oturumundan geliyor. */
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
    /** Gerçek teslimat/bağış sistemi bu ödül için devrede mi? */
    val teslimatEtkinMi: Boolean = true,
    /**
     * Kademenin sunduğu seçenekler. Tek elemanlıysa (ya da boşsa) seçim
     * ekranı hiç açılmıyor — bugüne kadarki otomatik davranış. Birden
     * fazlaysa [OdulHali.SECIM_BEKLIYOR] devreye giriyor.
     */
    val secenekler: List<Secenek> = emptyList(),
)

/** Bir kademenin seçilebilir alternatiflerinden biri (örn. kupon ya da fidan). */
data class Secenek(
    val id: String,
    val ad: String,
    val ikon: ImageVector,
    val aciklama: String,
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
