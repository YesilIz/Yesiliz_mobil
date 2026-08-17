package tr.org.yesilay.yesiliz.nudge

import java.time.Duration

/**
 * Nudge'ın **etkin** zamanlaması — kullanıcının seçtiği eşikten türetiliyor.
 *
 * [NudgeSabitleri.ESIK] artık yalnızca *varsayılan*; kullanıcı Profil ›
 * Hedeflerim'den kendi eşiğini seçebiliyor ve sönüm/polling süreleri o
 * eşikten yeniden hesaplanıyor. Türetme daha önce `NudgeSabitleri`'nde
 * derleme zamanı `val`'lerdeydi; çalışma zamanında değişen bir eşikle
 * bu mümkün değil, o yüzden buraya taşındı — hesabın kendisi aynı.
 *
 * Saf sınıf: Android'e ya da DataStore'a bağlı değil, emülatörsüz test
 * edilebiliyor (`OturumCikar` / `Tetikleyici` ile aynı desen).
 */
data class NudgeAyari(val esik: Duration) {

    /**
     * Canlı izleme aralığı — WorkManager'ın ~15dk asgari periyodu bunun
     * için uygun değil.
     *
     * Eşiğin yarısını veya 20 saniyeyi aşmaz: böylece kısa eşiklerde bile
     * en az bir poll eşiğin altında gerçekleşiyor.
     */
    val pollAraligi: Duration = minOf(
        Duration.ofSeconds(20),
        esik.dividedBy(2).coerceAtLeast(Duration.ofSeconds(3)),
    )

    /**
     * "5 dk mola ver" sonrası sönüm — kullanıcı uygulamadan gerçekten
     * ayrıldığı için kısa bir yeniden-tetiklenme penceresi yeterli.
     * Eşik kısa tutulduğunda sönüm eşiği aşmamalı.
     */
    val sonumMola: Duration = minOf(Duration.ofMinutes(5), esik)

    /**
     * "Devam et" (veya zaman aşımı / dışarı dokunma) sonrası sönüm —
     * kullanıcı **aynı** tüketim bloğunda kalmaya devam ediyor; kısa bir
     * sönüm birkaç dakika içinde yeniden rahatsız ederdi, bu yüzden
     * [sonumMola]'dan daha uzun. Eşik kısa tutulduğunda eşiği aşmamalı.
     */
    val sonumDevam: Duration = minOf(Duration.ofMinutes(10), esik)

    companion object {

        /**
         * Seçilebilecek en kısa eşik.
         *
         * Daha kısası kullanıcıyı dakikada bir dürtmek olurdu; uygulamanın
         * ilkesi "hafif dokunuş, zorlama yok" (CLAUDE.md §3) ve sürekli
         * çıkan bir popup direnç doğurup uygulamayı sildirir (§4).
         */
        const val EN_AZ_DK = 15

        /**
         * Seçilebilecek en uzun eşik.
         *
         * Üst sınır **özelliği anlamlı tutmak için** var: kesintisiz 1,5
         * saatlik tüketim bloğu zaten uzun bir blok; daha yükseği (mesela
         * 12 saat) hatırlatıcıyı hiç tetiklenmez hâle getirir ve kullanıcı
         * açık sandığı bir özellikle kalır (§12 K37). Hatırlatıcıyı
         * istemeyen onu Profil'den **kapatır**, saklamaz.
         */
        const val EN_COK_DK = 90

        /** `−` / `+` adımı ve hızlı seçim çiplerinin katı. */
        const val ADIM_DK = 5

        /**
         * Kullanıcı hiç seçim yapmadıysa geçerli olan ayar.
         *
         * Değeri [NudgeSabitleri.ESIK]'ten geliyor; o sabit dala göre
         * ayrışıyor (§12 K50: `main` 45 dk, `dev` 15 sn). Kullanıcı bir
         * değer seçtiği anda [EN_AZ_DK]–[EN_COK_DK] aralığına giriyor,
         * yani geliştirme dalının kısa eşiği yalnızca "hiç seçilmemiş"
         * durumda yaşıyor.
         */
        val VARSAYILAN = NudgeAyari(NudgeSabitleri.ESIK)

        /** Kullanıcıya sunulan hızlı seçim değerleri (dakika). */
        val HIZLI_SECIM = listOf(15, 30, 45, 60)

        /** Aralık dışına çıkan her değer sınıra çekilir. */
        fun kirp(dakika: Int): Int = dakika.coerceIn(EN_AZ_DK, EN_COK_DK)

        /**
         * Saklanan tercihten ayar üretir. `null` — yani kullanıcı hiç
         * seçmemiş — [VARSAYILAN]'a düşer; seçilmiş değer her zaman
         * [kirp]'ten geçer, çünkü DataStore'daki sayı eski bir sürümden
         * ya da yedekten gelmiş olabilir.
         */
        fun dakikadan(dakika: Int?): NudgeAyari =
            if (dakika == null) VARSAYILAN else NudgeAyari(Duration.ofMinutes(kirp(dakika).toLong()))
    }
}
