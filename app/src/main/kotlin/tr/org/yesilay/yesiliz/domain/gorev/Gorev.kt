package tr.org.yesilay.yesiliz.domain.gorev

import tr.org.yesilay.yesiliz.risk.Bant

/**
 * Görevin nasıl doğrulandığı.
 *
 * [BEYAN] görevlerde kullanıcı "yaptım" diyor ve puanı alıyor — kanıt
 * istenmiyor. Bu bilinçli: kanıt istemek suçluluk üretir ve `spec-puan.md`
 * "durumu değil çabayı ödüllendir" ilkesine aykırı. Anti-gaming günlük
 * tavanla (`PuanSabitleri.GUNLUK_TAVAN`) sağlanıyor, şüpheyle değil.
 *
 * ## Niçin üç değer
 *
 * Eskiden yalnızca OTOMATIK ve BEYAN vardı ve ikisi arasına sıkışan
 * görevler yanlış tarafa düşüyordu. Ana ekranın görev detayı `when`
 * bloğunda NEFES ve YURUYUS dışındaki her şey `else` dalına, yani
 * "Tamamlandı olarak işaretle" butonuna gidiyordu — OTOMATIK ilan edilmiş
 * GECE_TEMIZ ve TUKETIM_HEDEFI dahil. Bu iki görev **zaten** günlük
 * özetten puanlanıyor, elle işaretlenince aynı gün için ikinci kez
 * sayılıyordu. Üstelik kayıt `dogrulamaTuru = "otomatik"` olarak
 * yazıldığı için denetim izi de yanıltıcıydı (`debug.md` K-9).
 *
 * Ayrım artık açık: eylemi uygulama içinde yapılan görevler ([AKTIVITE])
 * ile ölçümden doğrulananlar ([OTOMATIK]) aynı kutuda değil.
 */
enum class DogrulamaTuru(val anahtar: String) {
    /**
     * Günlük özetten doğrulanıyor — kullanıcı **elle işaretleyemez**.
     * Puanı [tr.org.yesilay.yesiliz.domain.puan.PuanHesapla.gunlukPuan]
     * veriyor, görev listesinde yalnızca durumu gösteriliyor.
     */
    OTOMATIK("otomatik"),

    /** Uygulama içindeki bir aktivite tamamlanınca yazılıyor. */
    AKTIVITE("aktivite"),

    /** Kullanıcının beyanı; kanıt istenmiyor. */
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
        dogrulama = DogrulamaTuru.AKTIVITE,
    ),
    DIJITAL_MOLA(
        id = "dijital_mola",
        baslik = "Dijital mola",
        aciklama = "15 dakika ekransız",
        puan = 10,
        gunlukLimit = 2,
        // OTOMATIK ilan edilmişti ama doğrulayan hiçbir şey yoktu: ekran
        // kapalı süresini ölçen bir akış hiç yazılmamıştı, kullanıcı elle
        // işaretliyordu. Beyan olduğunu söylemek, olmadığı bir doğrulamayı
        // iddia etmekten dürüst.
        dogrulama = DogrulamaTuru.BEYAN,
    ),
    YURUYUS(
        id = "yuruyus",
        baslik = "15 dakika yürüyüş",
        aciklama = "Ekrandan uzaklaşmanın en kolay yolu",
        puan = 45,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.AKTIVITE,
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

    // --- Aktiviteler sekmesi (`debug.md` İ-1) ---

    /**
     * Odak seansı — telefonu bırakıp 15/25/45 dakika geçirmek.
     *
     * [DIJITAL_MOLA]'nın gerçekten doğrulanmış hâli: ekranın kapalı
     * kaldığı süre ölçülüyor, kullanıcı beyanı değil.
     */
    ODAK(
        id = "odak",
        baslik = "Odak seansı",
        aciklama = "Telefonu bırak, süre dolana kadar açma",
        puan = 20,
        gunlukLimit = 3,
        dogrulama = DogrulamaTuru.AKTIVITE,
    ),

    /** 20-20-20 göz molası — 20 saniye uzağa bakmak. */
    GOZ_MOLASI(
        id = "goz_molasi",
        baslik = "20-20-20 göz molası",
        aciklama = "20 saniye boyunca uzağa bak",
        puan = 3,
        gunlukLimit = 4,
        dogrulama = DogrulamaTuru.AKTIVITE,
    ),

    /** Gece kapanışı — yatmadan önceki kısa ritüel. */
    GECE_KAPANISI(
        id = "gece_kapanisi",
        baslik = "Gece kapanışı",
        aciklama = "Günü kapat, telefonu yarına bırak",
        puan = 8,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.AKTIVITE,
    ),

    /**
     * "Ekransız ne yaptın?" — kart seçimi.
     *
     * **Serbest metin yok** (CLAUDE.md §4): kullanıcı hazır kartlardan
     * seçiyor. `ruh_hali` ile aynı desen.
     */
    EKRANSIZ(
        id = "ekransiz",
        baslik = "Ekransız ne yaptın?",
        aciklama = "Bugün ekran dışında yaptığın bir şeyi işaretle",
        puan = 4,
        gunlukLimit = 1,
        dogrulama = DogrulamaTuru.BEYAN,
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
