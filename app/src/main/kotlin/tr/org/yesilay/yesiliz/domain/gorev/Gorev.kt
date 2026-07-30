package tr.org.yesilay.yesiliz.domain.gorev

import tr.org.yesilay.yesiliz.risk.Bant

/**
 * Görevin nasıl doğrulandığı.
 *
 * [BEYAN] görevlerde kullanıcı "yaptım" diyor ve puanı alıyor — kanıt
 * istenmiyor. Bu bilinçli: kanıt istemek suçluluk üretir ve `spec-puan.md`
 * "durumu değil çabayı ödüllendir" ilkesine aykırı. Anti-gaming günlük
 * tavanla (`PuanSabitleri.GUNLUK_TAVAN`) sağlanıyor, şüpheyle değil.
 */
enum class DogrulamaTuru(val anahtar: String) {
    OTOMATIK("otomatik"),
    BEYAN("beyan"),
}

/**
 * Görev kataloğu — `docs/spec-puan.md §7.1`.
 *
 * Puanlar ve günlük limitler spekten birebir. Katalog **sabit**: sunucudan
 * gelmiyor, MVP tamamen yerel çalışıyor.
 */
enum class Gorev(
    val id: String,
    val baslik: String,
    val aciklama: String,
    val puan: Int,
    val gunlukLimit: Int,
    val dogrulama: DogrulamaTuru,
) {
    NEFES(
        id = "nefes",
        baslik = "Nefes egzersizi",
        aciklama = "2 dakikalık kutu nefesi",
        puan = 5,
        gunlukLimit = 2,
        dogrulama = DogrulamaTuru.OTOMATIK,
    ),
    DIJITAL_MOLA(
        id = "dijital_mola",
        baslik = "Dijital mola",
        aciklama = "15 dakika ekransız",
        puan = 10,
        gunlukLimit = 2,
        dogrulama = DogrulamaTuru.OTOMATIK,
    ),
    YURUYUS(
        id = "yuruyus",
        baslik = "15 dakika yürüyüş",
        aciklama = "Ekrandan uzaklaşmanın en kolay yolu",
        puan = 45,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.OTOMATIK,
    ),
    RUH_HALI(
        id = "ruh_hali",
        baslik = "Ruh hali kaydı",
        aciklama = "Bugün nasıl hissettiğini işaretle",
        puan = 3,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.BEYAN,
    ),
    UYKU_BEYANI(
        id = "uyku",
        baslik = "Uyku beyanı",
        aciklama = "Dün kaç saat uyuduğunu söyle",
        puan = 3,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.BEYAN,
    ),
    GECE_TEMIZ(
        id = "gece_temiz",
        baslik = "Telefonsuz gece",
        aciklama = "00–06 arası 15 dakikadan az kullanım",
        puan = 10,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.OTOMATIK,
    ),
    TUKETIM_HEDEFI(
        id = "tuketim_hedefi",
        baslik = "Günlük hedefi tuttur",
        aciklama = "Ekran süren hedefinin altında kalsın",
        puan = 25,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.OTOMATIK,
    ),
    ;

    companion object {
        fun idden(id: String): Gorev? = entries.firstOrNull { it.id == id }

        /**
         * `spec-puan.md §7.3` — banda göre önerilen görevler.
         *
         * Yüksek bantta daha çok puan kazanılabiliyor; bu **bilinçli**.
         * Kasıtlı kötüleşme kendini baltalıyor: tüketim hedefi zorlaşıyor ve
         * günlük tavan kaçışı sınırlıyor.
         *
         * Bant henüz belirlenmemişse (ilk günler) koruyucu set veriliyor.
         */
        fun banttanOner(bant: Bant?): List<Gorev> = when (bant) {
            Bant.ORTA -> listOf(YURUYUS, TUKETIM_HEDEFI, GECE_TEMIZ, RUH_HALI)
            Bant.COK -> listOf(YURUYUS, GECE_TEMIZ, TUKETIM_HEDEFI, UYKU_BEYANI)
            else -> listOf(NEFES, DIJITAL_MOLA, RUH_HALI, TUKETIM_HEDEFI)
        }
    }
}
