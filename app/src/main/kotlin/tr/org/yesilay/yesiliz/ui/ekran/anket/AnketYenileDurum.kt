package tr.org.yesilay.yesiliz.ui.ekran.anket

import tr.org.yesilay.yesiliz.ui.ekran.onboarding.SasSv
import java.time.LocalDate

/**
 * SAS-SV'nin yeniden çözülmesi — `docs/spec-anket.md §6.4` "4 haftada bir
 * tekrarlanır".
 *
 * Onboarding'deki [tr.org.yesilay.yesiliz.ui.ekran.onboarding.OnboardingDurum]
 * ile aynı yanıt yapısını kullanıyor ama ayrı bir durum sınıfı: onboarding
 * durumu yaş, cinsiyet ve hedef gibi ilk kuruluma özgü alanlar da taşıyor,
 * onları tekrar sormanın anlamı yok.
 */
data class AnketYenileDurum(
    /** 0 = cevaplanmadı, 1..6 Likert. */
    val yanitlar: List<Int> = List(SasSv.MADDELER.size) { 0 },
    val sayfa: Int = 0,
    val bitti: Boolean = false,
    /** Önceki değerlendirmenin ham puanı — sonuç ekranındaki karşılaştırma. */
    val oncekiPuan: Double? = null,
    val oncekiTarih: LocalDate? = null,
    /** Kaydedilen yeni ham puan. */
    val hamPuan: Int = 0,
    /** Trend grafiği — geçmiş tüm ham puanlar, eskiden yeniye. */
    val gecmis: List<Pair<LocalDate, Double>> = emptyList(),
) {
    val ilkMadde: Int get() = sayfa * SasSv.SAYFA_BOYU

    val sonMadde: Int
        get() = (ilkMadde + SasSv.SAYFA_BOYU).coerceAtMost(SasSv.MADDELER.size)

    val sonSayfaMi: Boolean get() = sonMadde >= SasSv.MADDELER.size

    val sayfaTamamMi: Boolean get() = (ilkMadde until sonMadde).all { yanitlar[it] > 0 }

    val ilerleme: Float get() = yanitlar.count { it > 0 }.toFloat() / yanitlar.size
}
