package tr.org.yesilay.yesiliz.ui.ekran.yedam

/**
 * YEDAM köprüsünün ekran durumu.
 *
 * `docs/yedam-riza-metni-v1.md` ekranda gösterilecek metnin **birebir**
 * gösterilmesini şart koşuyor: "Kısaltılamaz, özetlenemez; madde
 * atlanırsa rıza o maddeyi kapsamaz." Bu yüzden maddeler burada sabit
 * listeler olarak duruyor ve ekran onları olduğu gibi çiziyor.
 */
data class YedamDurum(
    /** Sunucudan gelen mevcut başvuru; yoksa `null`. */
    val basvuru: BasvuruGorunumu? = null,
    val yukleniyor: Boolean = true,
    val gonderiliyor: Boolean = false,

    // --- rıza formu ---
    val onayVerildi: Boolean = false,
    val iletisimTuru: IletisimTuru = IletisimTuru.TELEFON,
    val iletisimDegeri: String = "",
    val not: String = "",

    /** Kullanıcıya gösterilecek hata; `null` ise hata yok. */
    val hata: String? = null,

    /**
     * Sunucuya ulaşılamadı.
     *
     * Ayrı bir bayrak: "gönderilemedi" ile "reddedildi" aynı şey değil ve
     * kullanıcıya farklı şeyler yapması gerektiğini söylüyorlar.
     */
    val cevrimdisi: Boolean = false,

    /** Oturum yoksa başvuru açılamıyor; ekran giriş öneriyor. */
    val oturumYok: Boolean = false,
) {
    /** Form gönderilebilir mi? Onay kutusu **zorunlu** (rıza metni). */
    val gonderilebilir: Boolean
        get() = onayVerildi && iletisimDegeri.isNotBlank() && !gonderiliyor

    val basvurusuVar: Boolean get() = basvuru != null
}

enum class IletisimTuru(val anahtar: String, val etiket: String) {
    TELEFON("telefon", "Telefon"),
    EPOSTA("eposta", "E-posta"),
}

/**
 * Sunucudan gelen başvuru durumu.
 *
 * **Uydurma durum yok** (CLAUDE.md §4, K37): her alan sunucudan geliyor.
 * Danışmanın adı taşınmıyor — sunucu da göndermiyor.
 */
data class BasvuruGorunumu(
    val id: Int,
    val durum: String,
    val ebeveynOnayiBekleniyor: Boolean,
    val rizaBitisi: String?,
    val temaslar: List<TemasGorunumu>,
) {
    val acikMi: Boolean get() = durum !in KAPANMIS

    /** Kullanıcıya gösterilen açıklama — sunucu durum kodunun karşılığı. */
    val aciklama: String
        get() = when (durum) {
            "ebeveyn_onayi_bekliyor" ->
                "Başvurun alındı. 18 yaşından küçük olduğun için bağlı " +
                    "ebeveyn hesabının onayı bekleniyor; onay gelene kadar " +
                    "hiçbir danışman bilgilerini görmüyor."
            "yeni" -> "Başvurun alındı ve havuza düştü. Bir danışmana atanacak."
            "atandi" -> "Başvurun bir danışmana atandı."
            "temas_kuruldu" -> "Danışman seninle iletişime geçti."
            "ulasilamadi" ->
                "Danışman sana ulaşmayı denedi ama başaramadı. İletişim " +
                    "bilgini kontrol edebilir ya da 115'i arayabilirsin."
            "kapandi" -> "Başvurun kapatıldı."
            "riza_geri_cekildi" -> "Rızanı geri çektin; başvurun kapandı."
            "suresi_doldu" ->
                "Rızanın 90 günlük süresi doldu ve bilgilerin sunucudan silindi."
            else -> ""
        }

    private companion object {
        val KAPANMIS = setOf("kapandi", "riza_geri_cekildi", "suresi_doldu")
    }
}

data class TemasGorunumu(val tur: String, val zaman: String) {
    val etiket: String
        get() = when (tur) {
            "arandi" -> "Arandı"
            "ulasilamadi" -> "Ulaşılamadı"
            "gorusuldu" -> "Görüşüldü"
            else -> tur
        }
}

/**
 * Rıza metninin **paylaşılacak** maddeleri.
 *
 * `docs/yedam-riza-metni-v1.md §1` ile birebir. Kısaltmak rızayı geçersiz
 * kılar: kullanıcı görmediği bir maddeye rıza vermiş sayılamaz.
 */
val PAYLASILACAKLAR = listOf(
    "Adın ve seçtiğin iletişim bilgin (telefon ya da e-posta)",
    "Son 30 günün risk bandın ve eğilimi",
    "Skorunu en çok yükselten kategoriler (örneğin \"sosyal medya\", \"oyun\")",
    "Daha önce yaptıysan, farkındalık anketi sonuçların",
    "Yazmak istersen, kendi eklediğin not",
)

/** Rıza metninin **paylaşılmayacak** maddeleri. */
val PAYLASILMAYACAKLAR = listOf(
    "Hangi uygulamayı ne zaman açtığın",
    "Mesajların, fotoğrafların, rehberin, arama ve tarayıcı geçmişin",
    "Konumun",
    "Şifren",
)
