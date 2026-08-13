package tr.org.yesilay.yesiliz.ui.ekran.istatistik

import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Dilim
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private val TR: Locale = Locale.forLanguageTag("tr")

/**
 * Gün / Hafta / Ay segmenti.
 *
 * Pencereler **takvim hizalı**: "son 7 gün" değil "bu hafta". Kullanıcı
 * "Pzt–Paz" beklerken kayan pencere görürse grafiği okuyamaz.
 * Hafta Pazartesi başlıyor (`WeekFields.ISO`), Türkiye'de de böyle.
 */
enum class Aralik(val etiket: String) {
    GUN("Gün"),
    HAFTA("Hafta"),
    AY("Ay"),
    ;

    /** Seçili pencerenin [başlangıç, bitiş] sınırları. */
    fun pencere(bugun: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        GUN -> bugun to bugun
        HAFTA -> bugun.with(WeekFields.ISO.dayOfWeek(), 1) to bugun.with(WeekFields.ISO.dayOfWeek(), 7)
        AY -> bugun.withDayOfMonth(1) to bugun
    }

    /** Karşılaştırma için bir önceki pencere. */
    fun oncekiPencere(bugun: LocalDate): Pair<LocalDate, LocalDate> {
        val (bas, bit) = pencere(bugun)
        return when (this) {
            GUN -> bas.minusDays(1) to bas.minusDays(1)
            HAFTA -> bas.minusWeeks(1) to bit.minusWeeks(1)
            AY -> bas.minusMonths(1) to bas.minusDays(1)
        }
    }

    /** Karşılaştırma cümlesinin öznesi: "Dün'e göre…", "Geçen haftaya göre…" */
    val karsilastirmaOznesi: String
        get() = when (this) {
            GUN -> "Düne göre"
            HAFTA -> "Geçen haftaya göre"
            AY -> "Geçen aya göre"
        }

    /** Çubuk etiketi: gün adı (Pzt) ya da ayın günü (14). */
    fun etiketle(tarih: LocalDate): String = when (this) {
        GUN, HAFTA -> tarih.format(GUN_ADI)
        AY -> tarih.dayOfMonth.toString()
    }

    private companion object {
        val GUN_ADI: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", TR)
    }
}

/**
 * İstatistik ekranı durumu — `docs/tasarim-referans.md §2 / 09`.
 *
 * [gunSayisi] sıfırsa ekran **boş durum** gösteriyor: kullanıcının ilk günü
 * ya da izin henüz verilmedi. Uydurma çubuk çizmek yerine "birkaç gün
 * ölçelim" demek doğru (CLAUDE.md §10).
 */
data class IstatistikDurum(
    val aralik: Aralik = Aralik.HAFTA,
    val gunSayisi: Int = 0,
    /** Penceredeki ham satırlar — gün seçimi bunun üzerinden çalışıyor. */
    val gunler: List<GunlukOzetEntity> = emptyList(),
    val tarihAraligi: String = "",
    val ekranOrtalama: Int = 0,
    val ekranCubuklari: List<Cubuk> = emptyList(),
    val ekranVurgu: Int = -1,
    val ekranFark: String = "",
    val ekranAzaldi: Boolean = true,
    val kategoriler: List<Dilim> = emptyList(),
    val adimOrtalama: Int = 0,
    val adimCubuklari: List<Cubuk> = emptyList(),
    val adimVurgu: Int = -1,
    val adimFark: String = "",
    val adimArtti: Boolean = true,
    /** `null` — henüz `risk_sonuc` yok, kart hiç gösterilmiyor. */
    val riskBandi: Bant? = null,

    // --- gün seçimi (grafiğe dokunma) ---
    /** -1 = seçim yok, kartlar ortalamayı gösteriyor. */
    val seciliIndeks: Int = -1,
    val seciliGun: LocalDate? = null,
    /** "24 Temmuz Perşembe" */
    val seciliBaslik: String = "",
    val ekranSecili: Int = 0,
    val adimSecili: Int = 0,
    val ekranSeciliFark: String = "",
    val adimSeciliFark: String = "",
) {
    val secimVarMi: Boolean get() = seciliIndeks >= 0

    /** Kart başlığı: seçim varken tarih, yokken "ORTALAMA". */
    /**
     * Ölçüm kartının etiketi.
     *
     * "Gün" sekmesinde de "Ortalama" yazıyordu; tek günün ortalaması
     * olmaz (`debug.md` O-12).
     */
    val ekranKartBasligi: String
        get() = when {
            secimVarMi -> seciliBaslik
            aralik == Aralik.GUN -> "Bugün"
            else -> "Ortalama"
        }

    val ekranGosterilen: Int get() = if (secimVarMi) ekranSecili else ekranOrtalama

    val adimGosterilen: Int get() = if (secimVarMi) adimSecili else adimOrtalama

    val ekranAltMetni: String get() = if (secimVarMi) ekranSeciliFark else ekranFark

    val adimAltMetni: String get() = if (secimVarMi) adimSeciliFark else adimFark
}
