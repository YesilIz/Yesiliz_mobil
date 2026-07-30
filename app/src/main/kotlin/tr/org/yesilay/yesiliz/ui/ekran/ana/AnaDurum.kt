package tr.org.yesilay.yesiliz.ui.ekran.ana

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ana Sayfa'nın gördüğü veri — `docs/tasarim-referans.md §2 / 08`.
 *
 * Room entity'si ya da `core-risk` çıktısı buraya doğrudan gelmez
 * (CLAUDE.md §6); depo katmanı bu modele çevirir.
 *
 * [veriVarMi] false iken ekran **boş durum** gösteriyor. Bu gerçek
 * kullanıcının 1. günü: izin yeni verildi, henüz özet çıkarılmadı.
 * Uydurma sayı göstermek yerine "birkaç gün ölçelim" demek doğru
 * (CLAUDE.md §10, `d_efektif = 0`).
 */
data class AnaDurum(
    val selamlama: String = "",
    val isim: String = "",
    val serideGun: Int = 0,
    val agacEvresi: Int = 1,
    val puan: Int = 0,
    val ekranDakika: Int = 0,
    val hedefDakika: Int = 0,
    val adim: Int = 0,
    val ruhHali: Float = VARSAYILAN_RUH_HALI,
    val gorevler: List<GorevOzeti> = emptyList(),
    val veriVarMi: Boolean = false,
    val izinVarMi: Boolean = true,
    /** Ebeveyn paneli eşleştirmesi tamamlandıysa 4. istatistik çipi görünür (ekran 08b). */
    val ebeveynAktifMi: Boolean = false,
)

/** Ruh hali slider'ının başlangıcı — orta nokta, yargısız. */
const val VARSAYILAN_RUH_HALI = 0.5f

/** Bugünün görevleri listesindeki tek satır. */
data class GorevOzeti(
    val id: String,
    val baslik: String,
    val aciklama: String,
    val puan: Int,
    val ikon: ImageVector,
    val tamamlandi: Boolean = false,
)
