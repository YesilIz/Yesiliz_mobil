package tr.org.yesilay.yesiliz.risk

import kotlin.math.exp
import kotlin.math.min

/**
 * §4.4 — risk skoru hesabı. **SAF FONKSİYON**: girdi → çıktı, yan etki yok.
 *
 * Hesap sırası bozulmaz:
 * ```
 * 1. Yükü İKİYE AYIR    B_tuketim / B_islevsel
 * 2. Örüntü çarpanı     P = 1 + Σ(β_i × p_i)
 * 3. Aktivite sönümü    D = 1 - min(0.15, 0.15 × A)
 * 4. Ham skor           S = B_islevsel + B_tuketim × P × D
 * 5. Davranış skoru     R_b = 100 × (1 - exp(-0.693 × S / 300))
 * 6. Anket skoru        R_s (kesme 29.5 → 50)
 * 7. Birleştir          R = α × R_s + (1-α) × R_b
 * ```
 *
 * §4.6 md.1 — **çarpan ve sönümleme SADECE tüketime uygulanır.** Yoksa
 * sosyal medyadan gelen kompulsiflik cezası ders saatlerini de cezalandırır.
 */
object RiskHesapla {

    /**
     * Hesabın 1–4. adımları — tek güne bağlı ara ürünler.
     *
     * Ayrı bir fonksiyon çünkü `gunluk_ozet` satırı bunları saklıyor (risk
     * detayı ekranını tek sorguya indirmek için) ama o noktada anket ve
     * `d` henüz devrede değil. [hesapla] bunu çağırıyor, yani **tek
     * implementasyon** var ve mevcut vektörlerin hepsi bu yolu geçiyor.
     */
    fun araUrunler(gun: GunlukGirdi): AraUrun {
        // 1) Yükü ikiye ayır
        var bTuketim = 0.0
        var bIslevsel = 0.0
        for ((kategori, dakika) in gun.kategoriDakika) {
            val agirlikli = dakika * agirlik(kategori)
            if (kategori in Sabitler.TUKETIM) bTuketim += agirlikli
            else bIslevsel += agirlikli
        }

        // 2) Örüntü çarpanı — eksik anahtar 0 sayılır
        var P = 1.0
        for ((ad, katsayi) in Sabitler.ORUNTU_KATSAYI) {
            P += katsayi * (gun.oruntu[ad] ?: 0.0)
        }

        // 3) Aktivite sönümlemesi (tavan %15)
        val A = Sabitler.ADIM_PAYI * min(1.0, gun.adim / Sabitler.ADIM_HEDEF) +
            Sabitler.SPOR_PAYI * min(1.0, gun.sporDk / Sabitler.SPOR_HEDEF)
        val D = 1.0 - min(Sabitler.MAKS_SONUMLEME, Sabitler.MAKS_SONUMLEME * A)

        // 4) Ham skor
        return AraUrun(
            bTuketim = bTuketim,
            bIslevsel = bIslevsel,
            P = P,
            A = A,
            D = D,
            S = bIslevsel + bTuketim * P * D,
        )
    }

    /**
     * @param gunGecmisi bu günün hesabının kaç gün önce yapıldığı — bugün
     *   işleniyorsa 0. [VeriDurumu.ESKI]'yi belirler (§4.8 A-yeni,
     *   `gorev2.md` §8 "STALE_DATA").
     */
    fun hesapla(
        gun: GunlukGirdi,
        anket: AnketGirdi? = null,
        d: Int = 0,
        gunGecmisi: Int = 0,
    ): RiskSonuc {
        val (bTuketim, bIslevsel, P, _, D, S) = araUrunler(gun)

        // 5) Davranış skoru — S = S_REF → tam 50
        val rB = 100.0 * (1.0 - exp(-Sabitler.EGIM * S / Sabitler.S_REF))

        // 6) Anket skoru — kesme puanı → tam 50
        val rS = anket?.let { anketSkoru(it.hamPuan) }

        // 7) Birleştir
        val alpha = Alpha.hesapla(
            d = d,
            anketVar = rS != null,
            anketKacGunOnce = anket?.kacGunOnce ?: 0,
        )
        val R = if (rS != null) alpha * rS + (1.0 - alpha) * rB else rB

        return RiskSonuc(
            S = S,
            P = P,
            D = D,
            bTuketim = bTuketim,
            bIslevsel = bIslevsel,
            rB = rB,
            rS = rS,
            alpha = alpha,
            R = R,
            bant = Bantlama.esiktenBant(R),
            surukleyiciler = surukleyiciler(gun.kategoriDakika),
            veriDurumu = veriDurumu(gun, gunGecmisi),
        )
    }

    /**
     * §4.8 (yeni) — [bant] her zaman hesaplanır ama günün ölçümü asgarinin
     * altındaysa ya da hesap günler önceyse iddia edilmemeli.
     *
     * Yetersizlik önce kontrol edilir: hem yetersiz hem eski bir gün için
     * "hangi ölçüm hiç yok" sorusu daha temel — eskilik ancak elde gerçek
     * bir ölçüm varken anlamlı.
     */
    private fun veriDurumu(gun: GunlukGirdi, gunGecmisi: Int): VeriDurumu = when {
        gun.toplamDk < Sabitler.GUN_ASGARI_DK -> VeriDurumu.YETERSIZ
        gunGecmisi > Sabitler.VERI_ESKI_ESIK_GUN -> VeriDurumu.ESKI
        else -> VeriDurumu.HESAPLANDI
    }

    /** Tabloda olmayan kategori [Sabitler.VARSAYILAN_AGIRLIK] alır. */
    private fun agirlik(kategori: String): Double =
        Sabitler.KATEGORI_AGIRLIK[kategori] ?: Sabitler.VARSAYILAN_AGIRLIK

    /**
     * §4.4 md.6 — SAS-SV ham puanı 0-100'e taşınır, kesme puanı tam 50'ye düşer.
     * İki skorun 50'de buluşması ağırlıklı ortalamayı anlamlı kılar (§4.6 md.3).
     *
     * **Public:** anket doldurulduğu anda `anket_yanit.rS` kolonuna yazılıyor
     * (`docs/spec-anket.md §6.4` ham puanla birlikte türetilmiş skoru da
     * saklamayı istiyor). Vektör karşılıkları: `anket_taban_on`,
     * `anket_kesme_noktasi_elli`, `anket_tavan_altmis`.
     */
    fun anketSkoru(hamPuan: Double): Double =
        if (hamPuan <= Sabitler.SASSV_KESME) {
            50.0 * (hamPuan - Sabitler.SASSV_MIN) /
                (Sabitler.SASSV_KESME - Sabitler.SASSV_MIN)
        } else {
            50.0 + 50.0 * (hamPuan - Sabitler.SASSV_KESME) /
                (Sabitler.SASSV_MAKS - Sabitler.SASSV_KESME)
        }

    /**
     * §4.6 md.5 — en çok katkı yapan ilk 3 kategori.
     *
     * Sıralama **kararlı**: eşit katkıda [GunlukGirdi.kategoriDakika]'nın
     * ekleme sırası korunur. Referans Python'un `sorted(..., reverse=True)`
     * davranışıyla birebir aynı olsun diye böyle.
     */
    private fun surukleyiciler(kategoriDakika: Map<String, Double>): List<String> =
        kategoriDakika
            .map { (kategori, dakika) -> kategori to dakika * agirlik(kategori) }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
}
