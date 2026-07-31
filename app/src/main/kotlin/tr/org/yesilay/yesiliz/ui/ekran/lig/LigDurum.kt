package tr.org.yesilay.yesiliz.ui.ekran.lig

import tr.org.yesilay.yesiliz.domain.lig.OnerilenKisi

/**
 * Lig ekranı durumu — `docs/tasarim-referans.md §2 / 10` ve boş durum **37**.
 *
 * Lig seviyeleri Fidan → Filiz → Ağaç → Koru → Orman (CLAUDE.md §12 K1).
 */
data class LigDurum(
    val ligAdi: String = "",
    val kalanGun: Int = 0,
    val yukselen: Int = 7,
    val dusen: Int = 7,
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
    val onerilenler: List<OnerilenKisi> = emptyList(),
    val kopyalandi: Boolean = false,
    val yukleniyor: Boolean = true,
)
