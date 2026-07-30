package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.ui.graphics.Color
import tr.org.yesilay.yesiliz.domain.kategori.Kategori

/**
 * Kategori → grafik rengi.
 *
 * Palet `docs/tasarim-referans.md §1`'den. **Yalnızca kategori dağılımı,
 * lejant ve lig avatarı** için — bir kategori rengi risk bandını göstermez.
 * Bant rengi her zaman [RiskAz]/[RiskOrta]/[RiskCok].
 *
 * [Kategori.SISTEM] risk hesabına girmiyor ([Kategori.hesabaGirer] false),
 * dağılım grafiğinde de gösterilmiyor — yine de bir renk döndürüyoruz ki
 * `when` dalı eksik kalmasın.
 */
val Kategori.grafikRengi: Color
    get() = when (this) {
        Kategori.SOCIAL -> KategoriSosyal
        Kategori.ENTERTAINMENT -> KategoriEglence
        Kategori.COMMUNICATION -> KategoriIletisim
        Kategori.GAMES -> KategoriOyun
        Kategori.PRODUCTIVITY -> KategoriUretkenlik
        Kategori.EDUCATION -> Yesil500
        Kategori.HEALTH -> YesilAna
        Kategori.OTHER -> KategoriDiger
        Kategori.SISTEM -> MetinUcuncul
    }

/** Ekranda görünen Türkçe kategori adı. */
val Kategori.adi: String
    get() = when (this) {
        Kategori.SOCIAL -> "Sosyal medya"
        Kategori.ENTERTAINMENT -> "Eğlence"
        Kategori.COMMUNICATION -> "İletişim"
        Kategori.GAMES -> "Oyun"
        Kategori.PRODUCTIVITY -> "Üretkenlik"
        Kategori.EDUCATION -> "Eğitim"
        Kategori.HEALTH -> "Sağlık"
        Kategori.OTHER -> "Diğer"
        Kategori.SISTEM -> "Sistem"
    }

/**
 * Lig avatarı rengi — isimden türetiliyor.
 *
 * Sunucudan renk gelmiyor, gelmesi de gereksiz; aynı isim her cihazda aynı
 * rengi alsın diye deterministik. Kategori paletinin canlı tonları
 * kullanılıyor (tasarımda lig satırları tam olarak bu renkleri taşıyor).
 */
private val AvatarPaleti = listOf(
    KategoriSosyal,
    KategoriEglence,
    KategoriIletisim,
    KategoriOyun,
    KategoriUretkenlik,
    KategoriDiger,
)

fun avatarRengi(isim: String): Color =
    if (isim.isEmpty()) YesilKoyu
    else AvatarPaleti[(isim.sumOf { it.code } % AvatarPaleti.size)]
