package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.ArkadasDao
import tr.org.yesilay.yesiliz.data.yerel.entity.ArkadasEntity
import tr.org.yesilay.yesiliz.domain.lig.Arkadas
import tr.org.yesilay.yesiliz.domain.lig.DavetKodu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigOzeti
import tr.org.yesilay.yesiliz.domain.lig.LigSatiri
import tr.org.yesilay.yesiliz.domain.lig.OnerilenKisi
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LigDeposu]'nun MVP uygulaması — CLAUDE.md §12 K7'de vaat edilen sınıf.
 *
 * Sıralama **bilinçli olarak sahte** (backend Faz 2). Arkadaş defteri ve
 * davet kodu ise gerçek: Room ve DataStore'da yaşıyor, uygulama yeniden
 * açıldığında duruyor. Böylece Lig sekmesinin boş/dolu geçişi ve davet
 * akışı demo gününde gerçekten çalışıyor.
 *
 * Faz 2'de `RetrofitLigDeposu` bu sınıfın yerine geçiyor; `LigViewModel`
 * ve ekranlar değişmiyor.
 */
@Singleton
class YerelSahteLigDeposu @Inject constructor(
    private val arkadasDao: ArkadasDao,
    private val tercihler: Tercihler,
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : LigDeposu {

    override fun ligAkisi(): Flow<LigOzeti> = combine(
        tercihler.ligeKatildiMi,
        arkadasDao.hepsiAkisi(),
        puanDeposu.toplamPuan,
    ) { katildi, arkadaslar, puan ->
        val eklenenler = arkadaslar.filter { it.durum == ArkadasEntity.DURUM_EKLENDI }

        LigOzeti(
            ligAdi = LIG_ADI,
            kalanGun = KALAN_GUN,
            yukselen = YUKSELEN,
            dusen = DUSEN,
            siralama = siralamaKur(eklenenler.map { it.isim }, puan),
            // Boş durum: ne lige katılmış ne de arkadaşı var.
            katildiMi = katildi || eklenenler.isNotEmpty(),
        )
    }

    override fun arkadaslarAkisi(): Flow<List<Arkadas>> =
        arkadasDao.hepsiAkisi().map { satirlar ->
            satirlar.map {
                Arkadas(
                    isim = it.isim,
                    davetBekliyorMu = it.durum == ArkadasEntity.DURUM_DAVET,
                )
            }
        }

    override suspend fun davetKodu(): String =
        tercihler.davetKodu.first() ?: DavetKodu.uret().also { tercihler.davetKodunuKaydet(it) }

    override suspend fun ligeKatil() {
        tercihler.ligeKatildiIsaretle()
    }

    override suspend fun davetGonder(isim: String) {
        arkadasDao.kaydet(
            ArkadasEntity(
                isim = isim,
                durum = ArkadasEntity.DURUM_DAVET,
                eklenmeTarihi = LocalDate.now(saat),
            ),
        )
    }

    override suspend fun onerilenler(): List<OnerilenKisi> {
        val mevcut = arkadasDao.hepsiAkisi().first().map { it.isim }.toSet()
        return ORNEK_ONERILER.map { it.copy(davetGonderildiMi = it.isim in mevcut) }
    }

    /**
     * Sıralamayı kurar.
     *
     * Kullanıcının kendi puanı **gerçek** (`puan_gunluk` toplamı); diğer
     * satırlar sabit. Kullanıcı listeye puanına göre yerleştiriliyor, sabit
     * bir sıraya değil — puan kazandıkça ligde yükseldiğini görüyor.
     * Eklenen arkadaşlar da listeye giriyor.
     */
    private fun siralamaKur(arkadasIsimleri: List<String>, puan: Int): List<LigSatiri> {
        val sahteler = SAHTE_RAKIPLER.map { (isim, p) -> Triple(isim, p, false) }
        val arkadaslar = arkadasIsimleri.mapIndexed { i, isim ->
            // Arkadaş puanı sunucudan gelmiyor; isimden türetilen sabit bir
            // değer kullanılıyor ki liste her açılışta zıplamasın.
            Triple(isim, ARKADAS_TABAN + (isim.sumOf { it.code } % ARKADAS_ARALIK), true)
        }
        val ben = Triple(BEN, puan, false)

        return (sahteler + arkadaslar + ben)
            .sortedByDescending { it.second }
            .mapIndexed { indeks, (isim, p, arkadasMi) ->
                LigSatiri(
                    sira = indeks + 1,
                    isim = isim,
                    puan = p,
                    benMiyim = isim == BEN,
                    arkadasMi = arkadasMi,
                )
            }
    }

    private companion object {
        const val LIG_ADI = "Filiz Ligi"
        const val KALAN_GUN = 5
        const val YUKSELEN = 7
        const val DUSEN = 7

        /** Hesap sistemi Faz 2 — o zamana kadar sabit (AnaViewModel ile aynı). */
        const val BEN = "Turan"

        const val ARKADAS_TABAN = 900
        const val ARKADAS_ARALIK = 600

        val SAHTE_RAKIPLER = listOf(
            "Elif" to 1_420,
            "Mert" to 1_380,
            "Zeynep" to 1_310,
            "Ahmet" to 1_275,
            "Buse" to 1_220,
            "Kerem" to 1_190,
            "İrem" to 1_140,
            "Deniz" to 1_040,
            "Ada" to 980,
        )

        /**
         * Önerilen kişiler — **rehberden okunmuyor** (CLAUDE.md §4).
         * Sabit örnek liste; Faz 2'de sunucudan gelecek.
         */
        val ORNEK_ONERILER = listOf(
            OnerilenKisi("Ece Kılıç", "3 ortak arkadaş"),
            OnerilenKisi("Mert Aydın", "1 ortak arkadaş"),
            OnerilenKisi("Selin Öz", "Yeşilİz kullanıyor"),
        )
    }
}
