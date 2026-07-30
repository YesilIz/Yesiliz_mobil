package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Geçmişi indir" — KVKK veri taşınabilirliği.
 *
 * **Cihazda üretiliyor, sunucu gerekmiyor.** Tasarım 24 bir PDF raporundan
 * söz ediyor ama PDF üretimi backend işi ve çevrimdışı çalışmıyor; ham veri
 * dışa aktarımı taşınabilirlik yükümlülüğünü zaten karşılıyor ve
 * kullanıcının verisini gerçekten *taşınabilir* kılıyor (Excel'de açılır,
 * başka bir uygulamaya verilir).
 *
 * Dosya `cacheDir` altına yazılıyor: kalıcı depolamada tutmanın anlamı yok,
 * paylaşıldıktan sonra sistem temizleyebilir.
 */
@Singleton
class DisaAktarDeposu @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val ozetDao: GunlukOzetDao,
    private val riskDao: RiskSonucDao,
    private val gorevDao: GorevTamamlamaDao,
    private val anketDao: AnketYanitDao,
    private val saat: Clock,
) {

    /** JSON ve CSV üretip paylaşma niyeti döndürür. */
    suspend fun disaAktar(): Intent = withContext(Dispatchers.IO) {
        val klasor = File(baglam.cacheDir, KLASOR).apply { mkdirs() }
        val bugun = LocalDate.now(saat)

        val json = File(klasor, "yesiliz_$bugun.json").apply { writeText(jsonUret()) }
        val csv = File(klasor, "yesiliz_gunluk_$bugun.csv").apply { writeText(csvUret()) }

        val uriler = arrayListOf(uri(json), uri(csv))
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriler)
            putExtra(Intent.EXTRA_SUBJECT, "Yeşilİz verilerim")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Tam döküm.
     *
     * `kullanim_olay` **dahil edilmiyor**: uygulama adları ve oturum
     * bilgisi cihazın en hassas verisi ve 30 günden eskisi zaten siliniyor.
     * Kullanıcı kendi verisini isterse bunu da eklemek ayrı bir karar
     * (CLAUDE.md §4'ün ruhu: minimum veri, minimum yayılma).
     */
    private suspend fun jsonUret(): String {
        val ozetler = ozetDao.sonGunler(GUN_SINIRI)
        val riskler = riskDao.sonGunler(GUN_SINIRI)
        val gorevler = gorevDao.hepsi()
        val anketler = anketDao.hepsi()

        return buildString {
            appendLine("{")
            appendLine("""  "uygulama": "Yeşilİz",""")
            appendLine("""  "disaAktarmaTarihi": "${LocalDate.now(saat)}",""")
            appendLine("""  "not": "Uygulama kullanım olayları (hangi uygulama, ne zaman) bu dosyaya dahil edilmez; cihazda kalır.",""")

            appendLine("""  "gunlukOzet": [""")
            ozetler.forEachIndexed { i, o ->
                append("    ")
                append(o.jsonSatiri())
                appendLine(if (i == ozetler.lastIndex) "" else ",")
            }
            appendLine("  ],")

            appendLine("""  "riskSonuc": [""")
            riskler.forEachIndexed { i, r ->
                append("""    {"tarih": "${r.tarih}", "S": ${r.S}, "R": ${r.R}, """)
                append(""""bant": "${r.gosterilenBant}", "trend": "${r.trend}"}""")
                appendLine(if (i == riskler.lastIndex) "" else ",")
            }
            appendLine("  ],")

            appendLine("""  "gorevTamamlama": [""")
            gorevler.forEachIndexed { i, g ->
                append("""    {"tarih": "${g.tarih}", "gorev": "${g.gorevId}", "puan": ${g.puan}}""")
                appendLine(if (i == gorevler.lastIndex) "" else ",")
            }
            appendLine("  ],")

            appendLine("""  "degerlendirme": [""")
            anketler.forEachIndexed { i, a ->
                append("""    {"tarih": "${a.tarih}", "hamPuan": ${a.hamPuan}}""")
                appendLine(if (i == anketler.lastIndex) "" else ",")
            }
            appendLine("  ]")
            append("}")
        }
    }

    /** Günlük özet — Excel'de doğrudan açılabilsin diye ayrı CSV. */
    private suspend fun csvUret(): String = buildString {
        appendLine(
            "tarih;sosyal_dk;eglence_dk;oyun_dk;iletisim_dk;uretkenlik_dk;egitim_dk;" +
                "saglik_dk;diger_dk;toplam_dk;adim;spor_dk;skor",
        )
        ozetDao.sonGunler(GUN_SINIRI).forEach { o ->
            appendLine(
                listOf(
                    o.tarih,
                    o.socialDk, o.entertainmentDk, o.gamesDk, o.communicationDk,
                    o.productivityDk, o.educationDk, o.healthDk, o.otherDk,
                    o.toplam(), o.adim, o.sporDk, o.S,
                ).joinToString(";"),
            )
        }
    }

    private fun uri(dosya: File) =
        FileProvider.getUriForFile(baglam, "${baglam.packageName}.dosyalar", dosya)

    private companion object {
        const val KLASOR = "disa_aktarim"

        /** Bir yıllık geçmiş — daha eskisi zaten nadiren tutuluyor. */
        const val GUN_SINIRI = 365
    }
}

private fun GunlukOzetEntity.toplam(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk

private fun GunlukOzetEntity.jsonSatiri(): String = buildString {
    append("""{"tarih": "$tarih", """)
    append(""""sosyalDk": $socialDk, "eglenceDk": $entertainmentDk, """)
    append(""""oyunDk": $gamesDk, "iletisimDk": $communicationDk, """)
    append(""""uretkenlikDk": $productivityDk, "egitimDk": $educationDk, """)
    append(""""saglikDk": $healthDk, "digerDk": $otherDk, """)
    append(""""toplamDk": ${toplam()}, "adim": $adim, "sporDk": $sporDk, "skor": $S}""")
}
