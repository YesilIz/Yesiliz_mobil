package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.yerel.dao.BildirimDao
import tr.org.yesilay.yesiliz.data.yerel.entity.BildirimEntity
import tr.org.yesilay.yesiliz.domain.bildirim.BildirimTuru
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama içi bildirim defteri (tasarım 35).
 *
 * **Tek yazma noktası.** Puan, hedef, seri, lig ve mola olayları buradan
 * geçiyor; ekran yalnızca okuyor. Sistem bildirimi göndermek ayrı bir iş
 * (`arkaplan/HatirlatmaWorker`), burası bildirimin *kaydı* — bildirim izni
 * kapalı kullanıcı da olan biteni bu ekrandan görebiliyor.
 *
 * Dil kuralı: hepsi olumlu ya da nötr, emir kipi yok (CLAUDE.md §4).
 */
@Singleton
class BildirimDeposu @Inject constructor(
    private val dao: BildirimDao,
    private val saat: Clock,
) {

    fun sonlar(adet: Int = LISTE_ADET): Flow<List<BildirimEntity>> = dao.sonlarAkisi(adet)

    val okunmamisSayisi: Flow<Int> = dao.okunmamisSayisiAkisi()

    suspend fun tumunuOku() {
        dao.tumunuOku()
    }

    suspend fun gorevTamamlandi(gorev: Gorev) = yaz(
        tur = BildirimTuru.PUAN,
        baslik = "${gorev.puan} puan kazandın!",
        govde = "${gorev.baslik} görevini tamamladın.",
    )

    suspend fun hedefTutturuldu(ekranDk: Int, hedefDk: Int) = birKezGunde(
        tur = BildirimTuru.HEDEF,
        baslik = "Ekran süresi hedefine ulaştın",
        govde = "Bugün ${sureMetni(ekranDk)} ile hedefinin altındasın.",
    )

    suspend fun seriIlerledi(gun: Int) = birKezGunde(
        tur = BildirimTuru.SERI,
        baslik = "$gun gün üst üste",
        govde = "Serin sürüyor, çarpanın da onunla büyüyor.",
    )

    suspend fun ligSirasi(sira: Int, ligAdi: String) = birKezGunde(
        tur = BildirimTuru.LIG,
        baslik = "Ligde $sira. sıraya yükseldin",
        govde = "$ligAdi'nde harika gidiyorsun.",
    )

    suspend fun molaHatirlatildi(dakika: Int) = yaz(
        tur = BildirimTuru.MOLA,
        baslik = "Mola vakti",
        govde = "$dakika dakikadır aralıksız ekrandasın. Kısa bir ara iyi gelebilir.",
    )

    suspend fun degerlendirmeYenilenebilir(gecenGun: Int) = birKezGunde(
        tur = BildirimTuru.DEGERLENDIRME,
        baslik = "Değerlendirmeni yenileyebilirsin",
        govde = "Son değerlendirmenin üzerinden $gecenGun gün geçti.",
    )

    suspend fun hatirlatma(baslik: String, govde: String) =
        yaz(BildirimTuru.HATIRLATMA, baslik, govde)

    /** Defter sonsuza kadar büyümesin — ham olaylarla aynı mantık. */
    suspend fun eskileriTemizle() {
        dao.eskileriSil(Instant.now(saat).minus(SAKLAMA_GUN, ChronoUnit.DAYS))
    }

    private suspend fun yaz(tur: BildirimTuru, baslik: String, govde: String) {
        dao.ekle(
            BildirimEntity(
                tur = tur.anahtar,
                baslik = baslik,
                govde = govde,
                zaman = Instant.now(saat),
            ),
        )
    }

    /**
     * Aynı türden bildirimi günde bir kez yazar.
     *
     * Worker gün içinde birden çok kez koşuyor (kullanıcı uygulamayı açtı,
     * cihaz yeniden başladı) — bu koruma olmadan "hedefine ulaştın" satırı
     * defterde beşe katlanırdı.
     */
    private suspend fun birKezGunde(tur: BildirimTuru, baslik: String, govde: String) {
        val gunBasi = Instant.now(saat).minus(1, ChronoUnit.DAYS)
        if (dao.sayi(tur.anahtar, gunBasi) > 0) return
        yaz(tur, baslik, govde)
    }

    private fun sureMetni(dakika: Int): String {
        val saatKismi = dakika / DAKIKA_SAAT
        val dakikaKismi = dakika % DAKIKA_SAAT
        return if (saatKismi > 0) "${saatKismi}s ${dakikaKismi}dk" else "${dakikaKismi}dk"
    }

    private companion object {
        const val LISTE_ADET = 50
        const val SAKLAMA_GUN = 60L
        const val DAKIKA_SAAT = 60
    }
}
