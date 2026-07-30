package tr.org.yesilay.yesiliz.domain.kategori

/**
 * Paket adı → kategori. **Saf sınıf** — Context tutmaz, dosya okumaz.
 *
 * Tablo dışarıdan verilir ([KategoriTablosu] yükler), böylece birim testte
 * emülatör gerekmez.
 *
 * Öncelik sırası:
 * ```
 * 1. kullanıcı seçimi     — "bu benim için iş uygulaması" der, tablo susar
 * 2. tablo                — assets/kategori_tablosu.json
 * 3. Android beyanı       — ApplicationInfo.category, zayıf yedek
 * 4. other                — hiçbiri tutmazsa
 * ```
 *
 * Kullanıcı seçiminin en üstte olması bilinçli: kategori ağırlıkları
 * kişiye göre değişir (birinin "eğlence"si diğerinin işi). Ama bu
 * suistimale de açık — Instagram'ı "iş" işaretleyen kullanıcı skorunu
 * düşürebilir. Karşı önlem örüntü denetimi (§10): "iş" kategorisinde
 * gece 02:00'de 90 dk kesintisiz kullanım varsa işaretlenir.
 * O denetim günlük özet boru hattına eklenecek.
 */
class KategoriEsleyici(
    private val tablo: Map<String, Kategori>,
    private val kullaniciSecimleri: Map<String, Kategori> = emptyMap(),
) {

    /**
     * @param paketAdi tam paket adı, ör. `com.instagram.android`
     * @param androidKategorisi [android.content.pm.ApplicationInfo.category];
     *   bilinmiyorsa null geç
     */
    fun bul(paketAdi: String, androidKategorisi: Int? = null): Kategori {
        kullaniciSecimleri[paketAdi]?.let { return it }
        tablo[paketAdi]?.let { return it }
        androidKategorisi?.let { Kategori.androidKategorisinden(it) }?.let { return it }
        return Kategori.VARSAYILAN
    }

    /**
     * Ham kullanım listesini kategori sürelerine dağıtır — core-risk'in
     * `GunlukGirdi.kategoriDakika` girdisi bu.
     *
     * [Kategori.SISTEM] paketleri **düşülür**: launcher ve SystemUI ekran
     * süresi sayılmaz. Bu yüzden dönen haritanın toplamı girdi toplamından
     * küçük olabilir.
     *
     * Anahtarlar [Kategori.anahtar] dizeleri — core-risk bunları bekliyor.
     * Ekleme sırası korunur (LinkedHashMap): `surukleyiciler` sıralamasında
     * eşitlik olursa bu sıra belirleyici.
     */
    fun kategorilereDagit(
        kullanim: List<PaketSuresi>,
        androidKategorileri: Map<String, Int> = emptyMap(),
    ): Map<String, Double> {
        val sonuc = LinkedHashMap<String, Double>()
        for ((paket, dakika) in kullanim) {
            val kategori = bul(paket, androidKategorileri[paket])
            if (!kategori.hesabaGirer) continue
            sonuc[kategori.anahtar] = (sonuc[kategori.anahtar] ?: 0.0) + dakika
        }
        return sonuc
    }
}

/** Bir paketin bir gündeki toplam ön plan süresi (dakika). */
data class PaketSuresi(val paketAdi: String, val dakika: Double)
