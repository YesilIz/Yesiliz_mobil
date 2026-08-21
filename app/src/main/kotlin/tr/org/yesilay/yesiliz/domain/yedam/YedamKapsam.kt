package tr.org.yesilay.yesiliz.domain.yedam

/**
 * YEDAM rızasının kapsam anahtarları.
 *
 * `docs/yedam-riza-metni-v1.md §2` ile **birebir**: ekrandaki metin insan
 * diliyle, buradaki anahtarlar makine diliyle aynı şeyi söylüyor. Sunucu
 * tarafında da aynı liste var (`app/uclar/yedam.py` `GECERLI_KAPSAM`);
 * ikisi ayrışırsa sunucu 422 dönüyor, sessiz bir uyumsuzluk oluşmuyor.
 */
object YedamKapsam {
    /** Ad + iletişim tercihi ve değeri. */
    const val KIMLIK = "kimlik"

    /** 90 günlük bant, trend, veri durumu. */
    const val RISK = "risk"

    /** En çok katkı yapan 3 kategori. */
    const val SURUKLEYICI = "surukleyici"

    /** SAS-SV yanıt geçmişi. */
    const val ANKET = "anket"

    /**
     * Kategori dakikaları ve örüntüler.
     *
     * **Ayrı bir rızaya bağlı:** `gunluk_ozet` sunucuya yalnızca "Yeşilay
     * ile paylaşım" açıkken gidiyor (CLAUDE.md §12 K19). YEDAM rızası o
     * kapıyı açmıyor; paylaşım kapalıysa danışman bu bölümü boş görüyor.
     */
    const val DAVRANIS = "davranis"

    /** Kullanıcının kendi yazdığı not. */
    const val NOT = "not"

    /**
     * Rıza ekranında **her zaman** onaylanan çekirdek.
     *
     * `davranis` burada yok: kullanıcı onu ayrıca zaten yönetiyor ve
     * YEDAM ekranından ikinci kez sormak, kapalı olan bir kapıyı açmış
     * gibi görünürdü.
     */
    val VARSAYILAN = setOf(KIMLIK, RISK, SURUKLEYICI, ANKET, NOT)

    /** Ekranda gösterilecek rıza metninin sürümü. */
    const val METIN_SURUMU = "v1"
}
