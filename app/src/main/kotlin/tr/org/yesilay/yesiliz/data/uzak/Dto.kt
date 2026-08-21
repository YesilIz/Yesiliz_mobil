package tr.org.yesilay.yesiliz.data.uzak

import kotlinx.serialization.Serializable

/**
 * Ağ modelleri — `backend/app/semalar.py` ile **birebir** aynı alan adları.
 *
 * `@SerialName` haritalaması bilinçli olarak yok: iki taraf aynı yazımı
 * kullanınca alan eklerken tek yerde unutma riski kalmıyor. Sunucu tarafı
 * da bu yüzden `snake_case` yerine Kotlin'in yazımını izliyor.
 *
 * **Ham kullanım olayı burada YOK.** Uygulama adı, oturum başlangıç/bitişi
 * hiçbir DTO'da geçmiyor — sunucuya yalnızca günlük özet ve skor gidiyor
 * (CLAUDE.md §4).
 */

// --- hesap ---

@Serializable
data class KayitIstek(val ad: String, val eposta: String, val sifre: String)

@Serializable
data class GirisIstek(val eposta: String, val sifre: String)

@Serializable
data class YenileIstek(val yenileme: String)

@Serializable
data class SifreSifirlaIstek(val eposta: String)

@Serializable
data class JetonlarDto(
    val erisim: String,
    val yenileme: String,
    val ad: String,
    val eposta: String,
    /** Profil simgesi; yeni telefonda girişte geri gelsin diye. */
    val avatar: String = "",
)

/**
 * Profil güncelleme — e-posta alanı **yok** (bkz. `HesapDeposu.profilGuncelle`).
 */
@Serializable
data class ProfilIstek(
    val ad: String,
    val avatar: String,
    val mevcutSifre: String? = null,
    val yeniSifre: String? = null,
)

@Serializable
data class ProfilDto(val ad: String, val eposta: String, val avatar: String = "")

// --- özet senkronu ---

@Serializable
data class GunlukOzetDto(
    val tarih: String,
    val socialDk: Double = 0.0,
    val entertainmentDk: Double = 0.0,
    val gamesDk: Double = 0.0,
    val communicationDk: Double = 0.0,
    val productivityDk: Double = 0.0,
    val educationDk: Double = 0.0,
    val healthDk: Double = 0.0,
    val otherDk: Double = 0.0,
    val gece: Double = 0.0,
    val oturum: Double = 0.0,
    val acilis: Double = 0.0,
    val sabah: Double = 0.0,
    val uykuOncesi: Double = 0.0,
    val yerDegis: Double = 0.0,
    val adim: Double = 0.0,
    val sporDk: Double = 0.0,
    val S: Double = 0.0,
)

@Serializable
data class RiskSonucDto(
    val tarih: String,
    val S: Double,
    val R: Double,
    val gosterilenBant: String,
    val trend: String,
    val veriDurumu: String,
)

@Serializable
data class SenkronIstek(
    val ozetler: List<GunlukOzetDto> = emptyList(),
    val riskler: List<RiskSonucDto> = emptyList(),
    val haftalikPuan: Int = 0,
    /**
     * Toplam puan — **bilgilendirici** (U7).
     *
     * Panelin puan düzeltme formu mevcut toplamı gösteriyor ve sunucu onu
     * başka türlü bilemiyor. Otorite değil: puanı cihaz hesaplıyor ve
     * A13 (istemci beyanlı lig puanı) burada da geçerli.
     */
    val toplamPuan: Int? = null,
    /**
     * "Ligde görün" tercihi.
     *
     * Geriye uyumluluk için senkron gövdesinde de taşınır. Güncel istemci
     * değişikliği ayrı görünürlük ucuna anında gönderir.
     */
    val ligdeGorunur: Boolean? = null,
)

@Serializable
data class SenkronYanit(val ozetSayisi: Int, val riskSayisi: Int)

// --- lig ---

@Serializable
data class LigSatiriDto(
    val sira: Int,
    val isim: String,
    val puan: Int,
    val benMiyim: Boolean = false,
    val arkadasMi: Boolean = false,
)

@Serializable
data class LigYanit(
    val ligAdi: String,
    val kalanGun: Int,
    val yukselen: Int,
    val dusen: Int,
    val siralama: List<LigSatiriDto>,
    val katildiMi: Boolean,
    /** Kullanıcı başkalarının sıralamasında görünüyor mu. */
    val gorunuyorMuyum: Boolean = true,
)

@Serializable
data class LigKatilIstek(val gorunur: Boolean)

@Serializable
data class LigGorunurlukIstek(val gorunur: Boolean)

@Serializable
data class ArkadasDto(val isim: String, val davetBekliyorMu: Boolean)

@Serializable
data class DavetIstek(val kod: String)

@Serializable
data class KodYanit(val kod: String)

// --- ödül ---

@Serializable
data class OdulSecimIstek(val kademeId: String, val secenekId: String)

@Serializable
data class OdulSecimiDto(val kademeId: String, val secenekId: String)

// --- ebeveyn ---

@Serializable
data class EslestirmeKoduIstek(val rol: String)

@Serializable
data class EslestirIstek(val kod: String, val rol: String)

@Serializable
data class EbeveynOzetDto(
    val cocukAdi: String,
    val gunlukTarihler: List<String> = emptyList(),
    val gunlukDakikalar: List<Int>,
    val ortalamaDk: Int,
    val bant: String? = null,
    val trend: String? = null,
    val sonSinyalSaatOnce: Int? = null,
)

// --- YEDAM ---
//
// `docs/panel-plani.md §11` · rıza metni `docs/yedam-riza-metni-v1.md`.
//
// **İki rıza bağımsız.** "Yeşilay ile paylaşım" günlük özetin sunucuya
// gitmesini yönetiyor (K19); YEDAM rızası danışmanın görmesini. Bu yüzden
// sürükleyici gönderimi `/ozet`'e eklenmedi, ayrı uç oldu — `/ozet`
// paylaşımı kapalı kullanıcıda hiç çağrılmıyor ve özellik tam da korumak
// istediği kişide sessizce ölü kalırdı.

@Serializable
data class YedamBasvuruIstek(
    /** Kullanıcının okuduğu rıza metninin sürümü — cihaz koyuyor. */
    val metinSurumu: String,
    val kapsam: List<String>,
    /** `telefon` | `eposta` */
    val iletisimTuru: String,
    val iletisimDegeri: String,
    val kullaniciNotu: String = "",
    /** Beyan edilen yaş; `null` ise sunucu **reşit saymıyor** (K26, §9). */
    val beyanYas: Int? = null,
)

@Serializable
data class YedamBasvuruYanit(
    val basvuruId: Int,
    val durum: String,
    val rizaSonGecerlilik: String,
    val ebeveynOnayiBekleniyor: Boolean = false,
)

@Serializable
data class YedamTemasDto(val tur: String, val zaman: String)

@Serializable
data class YedamDurumYanit(
    val basvuruId: Int? = null,
    val durum: String = "",
    val olusturma: String? = null,
    val rizaSonGecerlilik: String? = null,
    val ebeveynOnayiBekleniyor: Boolean = false,
    val temaslar: List<YedamTemasDto> = emptyList(),
)

/** "14 Ağustos 10:32'de bakıldı." Danışmanın adı **taşınmıyor**. */
@Serializable
data class ErisimKaydiDto(val zaman: String, val kapsam: String)

/**
 * Sürükleyici — **sıra taşıyor, katkı değeri değil**.
 *
 * `core-risk` sürükleyicileri sıralı bir isim listesi olarak veriyor
 * (`RiskHesapla.surukleyiciler`); katkıyı hesaplayıp atıyor. Sıradan bir
 * yüzde uydurmak, ölçülmemiş sayıyı ölçülmüş gibi göstermek olurdu (§9).
 */
@Serializable
data class SurukleyiciDto(val tarih: String, val faktor: String, val sira: Int)

@Serializable
data class SurukleyiciIstek(val kayitlar: List<SurukleyiciDto>)

@Serializable
data class AnketYanitDto(
    val tarih: String,
    /** 10 maddenin Likert puanları; ölçek birebir korunuyor (K12). */
    val maddePuanlari: List<Int>,
    val hamPuan: Double,
    val rS: Double,
)

@Serializable
data class YedamGecmisIstek(
    val surukleyiciler: List<SurukleyiciDto> = emptyList(),
    val anketler: List<AnketYanitDto> = emptyList(),
)

@Serializable
data class EbeveynYedamOnayIstek(val rizaId: Int, val onay: Boolean)


// --- puan düzeltmesi (K56) ---

/**
 * Panelden yapılmış tekil düzeltme.
 *
 * [id] **sunucu id'si** ve cihazda birincil anahtar oluyor: aynı deltanın
 * iki kez uygulanması fiziksel olarak imkânsız kalsın diye. Personelin
 * adı taşınmıyor — kullanıcıya kimin yaptığı değil, ne yapıldığı ve niçin.
 */
@Serializable
data class PuanDuzeltmeDto(
    val id: Int,
    val delta: Int,
    val gerekce: String,
    val zaman: String,
)
