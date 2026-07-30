package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import tr.org.yesilay.yesiliz.data.yerel.dao.KategoriEslemeDao
import tr.org.yesilay.yesiliz.data.yerel.entity.KategoriEslemeEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.domain.kategori.KategoriEsleyici
import tr.org.yesilay.yesiliz.domain.kategori.KategoriTablosu

/**
 * Kategori eşlemesinin veri katmanı. UI buradan okur (CLAUDE.md §6).
 *
 * İki iş yapıyor:
 *  1. `assets/kategori_tablosu.json`'u ilk açılışta Room'a **seed** eder
 *  2. Room'daki güncel eşlemeden [KategoriEsleyici] üretir
 *
 * Seed'in `OnConflictStrategy.IGNORE` kullanması bilinçli: kullanıcı bir
 * uygulamanın kategorisini değiştirmişse (`kaynak = kullanici`) yeni sürümde
 * tablo yeniden seed edilse bile o satır **ezilmez**.
 */
class KategoriDeposu(
    private val dao: KategoriEslemeDao,
    private val tabloOkuyucu: suspend () -> String,
) {

    /**
     * Tablo boşsa assets'ten doldurur. Doluysa hiçbir şey yapmaz —
     * her açılışta 3.100 satır yazmanın anlamı yok.
     *
     * @return eklenen satır sayısı (zaten doluysa 0)
     */
    suspend fun seedEt(zorla: Boolean = false): Int {
        if (!zorla && dao.sayi() > 0) return 0

        val eslemeler = KategoriTablosu.coz(tabloOkuyucu()).map { (paket, kategori) ->
            KategoriEslemeEntity(
                paketAdi = paket,
                kategori = kategori.anahtar,
                kaynak = KategoriEslemeEntity.KAYNAK_TABLO,
            )
        }
        dao.seedEt(eslemeler)
        return eslemeler.size
    }

    /**
     * Room'daki tüm eşlemelerden bir [KategoriEsleyici] kurar.
     *
     * Kullanıcı seçimleri ayrı harita olarak veriliyor ki eşleyicinin
     * öncelik sırası (kullanıcı > tablo > Android beyanı > other) korunsun.
     */
    suspend fun esleyiciAl(): KategoriEsleyici {
        val hepsi = dao.hepsi()
        val tablo = HashMap<String, Kategori>(hepsi.size)
        val kullanici = HashMap<String, Kategori>()

        for (satir in hepsi) {
            val kategori = Kategori.anahtardan(satir.kategori)
            if (satir.kaynak == KategoriEslemeEntity.KAYNAK_KULLANICI) {
                kullanici[satir.paketAdi] = kategori
            } else {
                tablo[satir.paketAdi] = kategori
            }
        }
        return KategoriEsleyici(tablo, kullanici)
    }

    /** Kullanıcı bir uygulamanın kategorisini değiştirdiğinde. */
    suspend fun kullaniciSecti(paketAdi: String, kategori: Kategori) {
        dao.kullaniciSecimi(
            KategoriEslemeEntity(
                paketAdi = paketAdi,
                kategori = kategori.anahtar,
                kaynak = KategoriEslemeEntity.KAYNAK_KULLANICI,
            ),
        )
    }

    companion object {
        const val VARLIK_ADI = "kategori_tablosu.json"

        /** Uygulama içinde kullanılan hâli — tabloyu `assets`'ten okur. */
        fun olustur(baglam: Context, dao: KategoriEslemeDao) = KategoriDeposu(dao) {
            baglam.assets.open(VARLIK_ADI).bufferedReader().use { it.readText() }
        }
    }
}
