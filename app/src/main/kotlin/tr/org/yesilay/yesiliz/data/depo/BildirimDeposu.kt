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
import tr.org.yesilay.yesiliz.ui.sureMetni

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

    /** Geri alma için silinmeden önce okunuyor. */
    suspend fun getir(id: Long): BildirimEntity? = dao.getir(id)

    /** Kaydırarak silme — geri alma için satırın kendisi çağırana dönüyor. */
    suspend fun sil(id: Long) {
        dao.sil(id)
    }

    /** "Tümünü temizle". Geri alınamaz; ekran önce onay soruyor. */
    suspend fun hepsiniSil() {
        dao.hepsiniSil()
    }

    /** Geri alma — silinen satır aynı içerikle yeniden yazılıyor. */
    suspend fun geriYukle(kayit: BildirimEntity) {
        dao.ekle(kayit)
    }

    suspend fun gorevTamamlandi(gorev: Gorev) = yaz(
        tur = BildirimTuru.PUAN,
        baslik = "${gorev.puan} puan kazandın!",
        govde = "${gorev.baslik} görevini tamamladın.",
    )

    suspend fun hedefTutturuldu(ekranDk: Int, hedefDk: Int) = birKezGunde(
        tur = BildirimTuru.HEDEF,
        baslik = "Ekran süresi hedefine ulaştın",
        govde = "Bugün ${ekranDk.sureMetni()} ile hedefinin altındasın.",
    )

    suspend fun seriIlerledi(gun: Int) = birKezGunde(
        tur = BildirimTuru.SERI,
        baslik = "$gun gün üst üste",
        govde = "Serin sürüyor, çarpanın da onunla büyüyor.",
    )

    suspend fun molaHatirlatildi(dakika: Int) = yaz(
        tur = BildirimTuru.MOLA,
        baslik = "Mola vakti",
        govde = "$dakika dakikadır aralıksız ekrandasın. Kısa bir ara iyi gelebilir.",
    )

    /**
     * Panelden yapılan puan düzeltmesi (K56).
     *
     * **Dil bilinçli:** "ceza" kelimesi geçmiyor, emir kipi ve suçlayıcı
     * ton yok (§4 "Emir kipi / korkutma"). Gerekçe olduğu gibi
     * gösteriliyor — gerekçesiz bir düşürme, kuralın yasakladığı cezanın
     * ta kendisi olurdu.
     *
     * `birKezGunde` **kullanılmıyor**: aynı gün birden çok düzeltme
     * yapılabilir ve her biri kullanıcının görmesi gereken ayrı bir
     * karar. Tekrar koruması `puan_duzeltme.bildirildi` bayrağında.
     */
    suspend fun puanDuzeltildi(delta: Int, gerekce: String) = yaz(
        tur = BildirimTuru.DUZELTME,
        baslik = "Puanında bir düzeltme yapıldı",
        govde = "${if (delta > 0) "+" else ""}$delta puan. Gerekçe: $gerekce",
    )

    /**
     * Panelden yayımlanan duyuru (§5.6).
     *
     * `birKezGunde` kullanılmıyor: aynı gün birden çok duyuru
     * yayımlanabilir. Tekrar koruması `Tercihler.sonDuyuruId`'de.
     */
    suspend fun duyuru(baslik: String, metin: String) =
        yaz(BildirimTuru.DUYURU, baslik, metin)

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

    private companion object {
        const val LISTE_ADET = 50
        const val SAKLAMA_GUN = 60L
    }
}
