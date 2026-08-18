package tr.org.yesilay.yesiliz.ui.ekran.lig


/**
 * Lig ekranı durumu — `docs/tasarim-referans.md §2 / 10` ve boş durum **37**.
 *
 * Lig seviyeleri Fidan → Filiz → Ağaç → Koru → Orman (CLAUDE.md §12 K1).
 */
data class LigDurum(
    val ligAdi: String = "",
    val kalanGun: Int = 0,
    val yukselen: Int = 0,
    val dusen: Int = 0,
    val siralama: List<LigSatiriModel> = emptyList(),
    /**
     * false ise **boş durum** (37) gösteriliyor: kullanıcı ne lige katılmış
     * ne de arkadaş eklemiş.
     */
    val katildiMi: Boolean = false,
    /**
     * Kullanıcı başkalarının sıralamasında görünüyor mu (Profil › "Ligde
     * görün"). `false` iken ekran bunu söylüyor — sessizce gizlenmek,
     * tercihin çalışıp çalışmadığını belirsiz bırakırdı (`debug.md` Y-5).
     */
    val gorunuyorum: Boolean = true,
    /** Sıralama sunucudan değil yerel yedekten geliyor (`debug.md` O-14). */
    val cevrimdisi: Boolean = false,
    /**
     * Sunucu ayakta ama kimlik yok — kullanıcı misafir ya da oturumu düşmüş.
     *
     * [cevrimdisi] ile ayrı tutuluyor çünkü kullanıcının yapması gereken şey
     * farklı: biri beklemek, diğeri giriş yapmak. İkisi aynı karta
     * indirgenince misafir kullanıcı "Lig şu anda kullanılamıyor" görüyor ve
     * yapabileceği bir şey olmadığını sanıyordu.
     */
    val oturumYok: Boolean = false,
    /**
     * "Yeşilay ile paylaşım" kapalı — puan sunucuya **hiç gitmiyor**.
     *
     * Lig puanı `POST /ozet` ile taşınıyor ve o uç rıza kapısının arkasında
     * (§12 K19). Kapalıyken kullanıcı sıralamada kendi satırını 0 puanla
     * görüyordu ve bunun sebebi hiçbir yerde yazmıyordu: puan kazanmaya
     * devam ettiği hâlde lig hiç güncellenmiyordu. Kapıyı kaldırmak değil,
     * **söylemek** doğru olan (§12 K37).
     */
    val puanPaylasimiKapali: Boolean = false,
    val yukleniyor: Boolean = true,
)

data class LigSatiriModel(
    val sira: Int,
    val isim: String,
    val puan: Int,
    val benMiyim: Boolean = false,
    val arkadasMi: Boolean = false,
)

/** Arkadaş ekle ekranı — tasarım 36. */
data class ArkadasEkleDurum(
    val davetKodu: String = "",
    val girilenKod: String = "",
    val kodHatasi: String? = null,
    val kodIslemde: Boolean = false,
    val kodlaEklendi: Boolean = false,
    val kopyalandi: Boolean = false,
    val yukleniyor: Boolean = true,
)
