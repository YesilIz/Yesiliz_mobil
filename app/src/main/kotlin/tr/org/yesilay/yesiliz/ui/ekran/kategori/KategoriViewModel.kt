package tr.org.yesilay.yesiliz.ui.ekran.kategori

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.data.kullanim.UygulamaAdlari
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Bant
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Uygulama kategorileri.
 *
 * Uygulama başına süre `kullanim_olay` satırlarından **Kotlin'de**
 * toplanıyor, yeni bir DAO sorgusu eklenmedi: bu ekran yalnızca "bugün"ü
 * gösteriyor ve o günün satır sayısı birkaç yüz. Ham olaylar 30 gün
 * saklandığı için geçmişe dönük uygulama dökümü zaten mümkün değil
 * (`GunlukOzetWorker.HAM_OLAY_SAKLAMA_GUN`), o yüzden sorgu yazmanın
 * kazancı olmazdı.
 */
@HiltViewModel
class KategoriViewModel @Inject constructor(
    private val olayDao: KullanimOlayDao,
    private val kategoriDeposu: KategoriDeposu,
    private val uygulamaAdlari: UygulamaAdlari,
    private val bildirimDeposu: BildirimDeposu,
    private val tercihler: Tercihler,
    private val riskDao: RiskSonucDao,
    private val guncelGun: GuncelGun,
) : ViewModel() {

    private val _durum = MutableStateFlow(KategoriDurum())
    val durum: StateFlow<KategoriDurum> = _durum.asStateFlow()

    init {
        viewModelScope.launch {
            guncelGun.akis.collectLatest(::yukle)
        }
    }

    private suspend fun yukle(bugun: LocalDate = guncelGun.bugun()) {
        val esleyici = kategoriDeposu.esleyiciAl()

        // paket → toplam dakika
        val sureler = olayDao.gunun(bugun)
            .groupBy { it.paketAdi }
            .mapValues { (_, olaylar) ->
                olaylar.sumOf { Duration.between(it.baslangic, it.bitis).seconds }
                    .let { (it / SANIYE_DAKIKA.toDouble()).roundToInt() }
            }
            .filterValues { it > 0 }

        val satirlar = sureler.map { (paket, dakika) ->
            UygulamaSatiri(
                paketAdi = paket,
                ad = uygulamaAdlari.ad(paket),
                dakika = dakika,
                kategori = esleyici.bul(paket),
            )
        }

        // Sistem paketleri gösterilmiyor — risk hesabına da girmiyorlar
        // (CLAUDE.md §12 K9), telefonun kendi arayüzü kullanıcıya
        // "kategorize edilecek uygulama" olarak sunulamaz.
        val gorunur = satirlar.filter { it.kategori != Kategori.SISTEM }

        val gruplar = gorunur
            .groupBy { it.kategori }
            .map { (kategori, uygulamalar) ->
                KategoriGrubu(
                    kategori = kategori,
                    toplamDk = uygulamalar.sumOf { it.dakika },
                    uygulamalar = uygulamalar.sortedByDescending { it.dakika },
                )
            }
            .sortedByDescending { it.toplamDk }

        _durum.update {
            it.copy(
                gruplar = gruplar.mapIndexed { i, grup -> grup.copy(acik = i == 0) },
                toplamDk = gorunur.sumOf { satir -> satir.dakika },
                riskBandi = riskDao.sonGunler(1).firstOrNull()
                    ?.let { risk -> Bant.valueOf(risk.gosterilenBant) },
                ebeveynAktifMi = tercihler.ebeveynEslesmisMi.first(),
                kategorisizler = kategorisizleriBul(esleyici, sureler.keys),
                yukleniyor = false,
            )
        }
    }

    /**
     * Kategorisi belirlenememiş uygulamalar.
     *
     * Ölçüt: eşleyici `OTHER` döndürüyor **ve** kullanıcı henüz bir seçim
     * yapmamış. Tablonun bilmediği uygulamalar `OTHER`'a düşüyor
     * (`Kategori.VARSAYILAN`) — kullanıcının düzeltebileceği tam olarak
     * bu küme.
     */
    private fun kategorisizleriBul(
        esleyici: tr.org.yesilay.yesiliz.domain.kategori.KategoriEsleyici,
        kullanilanlar: Set<String>,
    ): List<UygulamaSatiri> {
        // Kullanılanlar önce: kullanıcı kendi kullandığı uygulamayı
        // sınıflandırdığında etkiyi hemen görüyor.
        val adaylar = (kullanilanlar + uygulamaAdlari.yuklenenler()).distinct()

        return adaylar
            .filter { esleyici.bul(it) == Kategori.OTHER }
            .map { paket ->
                UygulamaSatiri(
                    paketAdi = paket,
                    ad = uygulamaAdlari.ad(paket),
                    dakika = 0,
                    kategori = Kategori.OTHER,
                    onerilen = null,
                )
            }
            .sortedBy { it.ad }
            .take(IZGARA_UST_SINIR)
    }

    fun grubuAcKapat(kategori: Kategori) {
        _durum.update { durum ->
            durum.copy(
                gruplar = durum.gruplar.map { grup ->
                    if (grup.kategori == kategori) grup.copy(acik = !grup.acik) else grup
                },
            )
        }
    }

    fun uygulamaSecildi(satir: UygulamaSatiri?) {
        _durum.update { it.copy(secilenUygulama = satir) }
    }

    /**
     * Kullanıcının kategori seçimini kaydeder.
     *
     * Kaydettikten sonra liste yeniden yükleniyor ki dağılım hemen
     * düzelsin. Günlük özetin (`gunluk_ozet`) kategori dakikaları gece
     * worker'ında yeniden hesaplanıyor — burada tetiklemek, o günün ham
     * olaylarını yeniden işlemek demek olurdu ve bu ekranın işi değil.
     */
    fun kategoriKaydet(paketAdi: String, kategori: Kategori) {
        viewModelScope.launch {
            kategoriDeposu.kullaniciSecti(paketAdi, kategori)
            _durum.update { it.copy(secilenUygulama = null) }
            yukle()
        }
    }

    /**
     * Ebeveyn paneline **kategori özeti** gönderir.
     *
     * Uygulama adı gitmiyor — `docs/spec-veri.md`: ebeveyne ham veri değil
     * özet gösterilir. Buton zaten yalnızca eşleşme varken görünüyor.
     */
    fun ebeveyneGonder() {
        viewModelScope.launch {
            bildirimDeposu.ebeveyneGonderildi()
            _durum.update { it.copy(ebeveyneGonderildi = true) }
        }
    }

    private companion object {
        const val SANIYE_DAKIKA = 60
        /** Izgarada gösterilecek en fazla uygulama — ekran kalabalıklaşmasın. */
        const val IZGARA_UST_SINIR = 24
    }
}
