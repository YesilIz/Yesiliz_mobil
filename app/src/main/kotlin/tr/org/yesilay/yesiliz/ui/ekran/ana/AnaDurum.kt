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
    val avatarId: String = "",
    val serideGun: Int = 0,
    /**
     * Seri bugün joker hakkıyla mı korundu.
     *
     * Hedef kaçırıldığında seri sıfırlanmıyor, donuyor (haftada bir).
     * Kullanıcı sayının niye artmadığını görebilmeli — sessizce donmak,
     * serinin bozulduğunu sanmasına yol açar.
     */
    val seriJokerli: Boolean = false,
    val agacEvresi: Int = 1,
    val puan: Int = 0,
    val ekranDakika: Int = 0,
    val hedefDakika: Int = 0,
    val adim: Int = 0,
    val ruhHali: Float = VARSAYILAN_RUH_HALI,
    /** Açık pencere kaydedildiyse slider "işaretlendi" görünümüne geçer. */
    val ruhHaliKaydedildi: Boolean = false,
    /** "Sabah" / "Öğlen" / "Akşam" — kartın alt başlığı. */
    val ruhHaliPenceresi: String = "",
    /** Bugün kaç pencere dolduruldu (0..3). */
    val ruhHaliDoldurulan: Int = 0,
    val gorevler: List<GorevOzeti> = emptyList(),
    val veriVarMi: Boolean = false,
    val izinVarMi: Boolean = true,
    /**
     * Verilmemiş izinler — ana ekrandaki hatırlatıcı kartı bunları sayıyor.
     *
     * İzinler artık karşılama akışında isteniyor ama atlanabiliyor; atlayan
     * kullanıcının Profil'in derinliklerine inmesi gerekiyordu. Kart
     * kapatılabilir: bilinçli reddeden kullanıcıyı sürekli dürtmek §3'ün
     * "hafif dokunuş, zorlama yok" ilkesine aykırı.
     */
    val eksikIzinler: List<EksikIzin> = emptyList(),
    /** Yenile düğmesi dönerken. */
    val tazeleniyor: Boolean = false,
    /** Ebeveyn paneli eşleştirmesi tamamlandıysa 4. istatistik çipi görünür (ekran 08b). */
    val ebeveynAktifMi: Boolean = false,
    /** Okunmamış bildirim sayısı — başlıktaki zil rozeti (tasarım 35). */
    val okunmamisBildirim: Int = 0,
    /**
     * SAS-SV değerlendirmesinin üzerinden 4 hafta geçtiyse ana ekranda
     * hatırlatma kartı belirir (`docs/spec-anket.md §6.4`). Kullanıcı
     * "Sonra" derse o gün için kapanır.
     */
    val degerlendirmeYenilensin: Boolean = false,
) {
    /** Seri çipinin metni; joker harcanan günde bunu da söylüyor. */
    val seriEtiketi: String
        get() = if (seriJokerli) "$serideGun gün · joker" else "$serideGun gün"
}

/**
 * Ana ekranda hatırlatılan eksik izin.
 *
 * Yalnızca **veri toplamayı engelleyen** izinler burada: kullanım erişimi
 * ve adım kaynakları. Bildirim ve overlay izni uygulamanın ölçümünü
 * etkilemiyor, onlar kendi ekranlarında isteniyor.
 */
enum class EksikIzin(val baslik: String, val aciklama: String) {
    KULLANIM(
        baslik = "Ekran süreni göremiyoruz",
        aciklama = "Kullanım erişimi olmadan hangi uygulamada ne kadar " +
            "kaldığını ölçemiyoruz.",
    ),
    ADIM(
        baslik = "Adımlarını göremiyoruz",
        aciklama = "Hareket verin olmadan aktivitenin risk skoruna " +
            "yansıması eksik kalıyor.",
    ),
}

/** Ruh hali slider'ının başlangıcı — orta nokta, yargısız. */
const val VARSAYILAN_RUH_HALI = 0.5f

/** Bugünün görevleri listesindeki tek satır. */
data class GorevOzeti(
    val id: String,
    val baslik: String,
    val aciklama: String,
    val puan: Int,
    val ikon: ImageVector,
    /** Bugün kaç kez yapıldı — günlük limiti 1'den büyük görevler için. */
    val yapilan: Int = 0,
    val gunlukLimit: Int = 1,
    val tamamlandi: Boolean = false,
)
