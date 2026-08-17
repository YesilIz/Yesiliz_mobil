package tr.org.yesilay.yesiliz.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import tr.org.yesilay.yesiliz.data.depo.BeyanDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.domain.gorev.UykuAraligi
import java.time.Clock
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Widget'ların okuduğu veri.
 *
 * Glance `GlanceAppWidget`'ları Hilt ile **enjekte edilemiyor** — sistem
 * onları kendisi kuruyor. `@EntryPoint` bu yüzden gerekli: bağımlılıklar
 * çalışma anında Hilt grafiğinden alınıyor. Alternatif (Room'u widget içinde
 * elle kurmak) iki ayrı veritabanı örneği demek olurdu.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetGirisNoktasi {
    fun ozetDao(): GunlukOzetDao
    fun tercihler(): Tercihler
    fun beyanDeposu(): BeyanDeposu
    fun saat(): Clock
}

data class EkranSuresiVerisi(
    val dakika: Int = 0,
    val hedefDk: Int = 0,
    val veriVarMi: Boolean = false,
) {
    val oran: Float get() = (dakika.toFloat() / hedefDk.coerceAtLeast(1)).coerceIn(0f, 1f)

    /** `1s 42dk` — widget'ta yer az, kısa biçim. */
    fun metin(): String {
        val saat = dakika / DAKIKA_SAAT
        val kalan = dakika % DAKIKA_SAAT
        return if (saat > 0) "${saat}s ${kalan}dk" else "${kalan}dk"
    }

    companion object {
        private const val DAKIKA_SAAT = 60

        /**
         * Günlük özet satırından widget verisi — **SAF FONKSİYON**.
         *
         * Eşleme buraya ayrıldı ki "satır var, ölçüm yok" durumu Android
         * olmadan sınanabilsin; kusur tam olarak orada yaşıyordu.
         *
         * [GunlukOzetEntity.olayVarMi]'ye bakıyor, `ozet != null`'a **değil**:
         * gecelik worker ölçüm bulamadığı güne de sıfır dolu bir satır
         * yazıyor (CLAUDE.md §9 "Satır var ≠ veri var"). Satırın varlığını
         * veri saymak, widget'ın hiç ölçülmemiş bir gün için `%0 · 0dk`
         * göstermesi demekti — yani kullanıcıya hedefini tutturmuş gibi
         * görünen, ölçülmemiş bir sayı (§12 K39). Uygulamanın kendi ana
         * ekranı (`AnaViewModel`) aynı gün için doğru soruyu soruyordu;
         * iki yüzey birbirine zıt şey söylüyordu.
         */
        fun olustur(ozet: GunlukOzetEntity?, hedefDk: Int): EkranSuresiVerisi {
            val olculduMu = ozet?.olayVarMi == true
            return EkranSuresiVerisi(
                dakika = if (olculduMu && ozet != null) {
                    (ozet.socialDk + ozet.entertainmentDk + ozet.gamesDk + ozet.communicationDk +
                        ozet.productivityDk + ozet.educationDk + ozet.healthDk + ozet.otherDk)
                        .roundToInt()
                } else {
                    0
                },
                hedefDk = hedefDk,
                veriVarMi = olculduMu,
            )
        }
    }
}

data class GununSorusuVerisi(
    val cevaplandiMi: Boolean = false,
    val secilen: UykuAraligi? = null,
)

object WidgetVerisi {

    private fun giris(baglam: Context): WidgetGirisNoktasi =
        EntryPointAccessors.fromApplication(
            baglam.applicationContext,
            WidgetGirisNoktasi::class.java,
        )

    /**
     * Ölçüm yoksa sıfır göstermek yerine "veri yok" — widget'ta uydurma
     * sayı, ekrandakinden daha yanıltıcı olur (kullanıcı ona bakıp
     * uygulamayı hiç açmıyor). Karar [EkranSuresiVerisi.olustur]'da.
     */
    suspend fun ekranSuresi(baglam: Context): EkranSuresiVerisi {
        val g = giris(baglam)
        val bugun = LocalDate.now(g.saat())
        return EkranSuresiVerisi.olustur(
            ozet = g.ozetDao().getir(bugun),
            hedefDk = g.tercihler().gunlukHedefDk.first(),
        )
    }

    suspend fun gununSorusu(baglam: Context): GununSorusuVerisi {
        val g = giris(baglam)
        val bugun = LocalDate.now(g.saat())
        val cevap = g.beyanDeposu().uykuGetir(bugun)
        return GununSorusuVerisi(cevaplandiMi = cevap != null, secilen = cevap)
    }

    /** Widget'tan gelen cevabı kaydeder ve puanı verir. */
    suspend fun uykuCevapla(baglam: Context, aralik: UykuAraligi) {
        val g = giris(baglam)
        g.beyanDeposu().uykuKaydet(LocalDate.now(g.saat()), aralik)
    }
}
