package tr.org.yesilay.yesiliz.data.yerel

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room şema geçişleri.
 *
 * ## Neden `fallbackToDestructiveMigration` kaldırıldı
 *
 * Prototip boyunca yıkıcı geri düşüş kullanılıyordu: şema değişince
 * veritabanı silinip yeniden kuruluyordu. Yayında bunun anlamı, **her
 * güncellemede kullanıcının tüm geçmişinin silinmesi**. Ekran süresi
 * geçmişi bu uygulamanın biriktirdiği tek değerli şey; onu bir sürüm
 * yükseltmesiyle kaybetmek uygulamayı sildiren türden bir hata.
 *
 * ## Bundan sonraki kural
 *
 * **Entity değiştiren her PR bir `Migration` de yazmak zorunda.** Unutulursa
 * uygulama açılışta `IllegalStateException` ile düşüyor — sessizce veri
 * silmektense gürültülü çökmek yeğ. `GecisTest` bunu emülatörde
 * doğruluyor.
 *
 * DDL'ler `app/schemas/2.json`'dan **birebir** alındı; elle yazmak,
 * Room'un beklediği şemadan sapma riski taşıyor (sütun sırası, `NOT NULL`,
 * indeks adı — hepsi doğrulanıyor).
 */

/**
 * Sürüm 1 → 2 (30 Tem 2026).
 *
 * Beş yeni tablo eklendi; var olan hiçbir tablo değişmedi, o yüzden veri
 * taşıma adımı yok — yalnızca `CREATE TABLE`.
 */
val GECIS_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ruh_hali` " +
                "(`tarih` TEXT NOT NULL, `deger` REAL NOT NULL, PRIMARY KEY(`tarih`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `uyku_beyani` " +
                "(`tarih` TEXT NOT NULL, `aralik` TEXT NOT NULL, PRIMARY KEY(`tarih`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `farkindalik_testi` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tarih` TEXT NOT NULL, " +
                "`yanitlar` TEXT NOT NULL, `hamPuan` INTEGER NOT NULL, `seviye` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bildirim` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tur` TEXT NOT NULL, " +
                "`baslik` TEXT NOT NULL, `govde` TEXT NOT NULL, `zaman` INTEGER NOT NULL, " +
                "`okundu` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bildirim_zaman` ON `bildirim` (`zaman`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `arkadas` " +
                "(`isim` TEXT NOT NULL, `durum` TEXT NOT NULL, " +
                "`eklenmeTarihi` TEXT NOT NULL, PRIMARY KEY(`isim`))",
        )
    }
}

/**
 * Sürüm 2 → 3 (31 Tem 2026).
 *
 * `gunluk_ozet.olayVarMi` eklendi — "satır var" ile "veri var" ayrımı
 * (`debug.md` K-4, K-5).
 *
 * Varsayılan `0` (veri yok) ve ardından geriye dönük düzeltme: ölçülmüş
 * kullanımı olan satırlar `1`'e çekiliyor. `acilisSayisi > 0` ölçüt olarak
 * kategori dakikalarından daha güvenilir — `sistem` kategorisi risk
 * hesabına girmediği için (CLAUDE.md §12 K9) gerçekten kullanılmış bir
 * günün kategori toplamı sıfır olabiliyor, ama açılış sayılıyor.
 */
val GECIS_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `gunluk_ozet` ADD COLUMN `olayVarMi` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "UPDATE `gunluk_ozet` SET `olayVarMi` = 1 WHERE `acilisSayisi` > 0",
        )
    }
}

/**
 * Sürüm 3 → 4 (3 Ağu 2026).
 *
 * `adim_sayac` eklendi — donanım adım sayacının günlük birikimi
 * (`debug.md` K-0). Var olan hiçbir tablo değişmedi, veri taşıma adımı yok.
 */
val GECIS_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `adim_sayac` " +
                "(`tarih` TEXT NOT NULL, `toplam` INTEGER NOT NULL, " +
                "`sonSayac` INTEGER NOT NULL, PRIMARY KEY(`tarih`))",
        )
    }
}

/**
 * Sürüm 4 → 5 (4 Ağu 2026).
 *
 * `odul_secimi` eklendi — bazı ödül kademelerinde market kuponu yerine
 * fidan bağışı gibi alternatifler seçilebiliyor. Var olan hiçbir tablo
 * değişmedi, veri taşıma adımı yok.
 */
val GECIS_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `odul_secimi` " +
                "(`kademeId` TEXT NOT NULL, `secenekId` TEXT NOT NULL, " +
                "`tarih` TEXT NOT NULL, PRIMARY KEY(`kademeId`))",
        )
    }
}

/**
 * Sürüm 5 → 6 (5 Ağustos 2026).
 *
 * `risk_sonuc.veriDurumu` eklendi (`tr.org.yesilay.yesiliz.risk.VeriDurumu`)
 * — `gunluk_ozet.olayVarMi` neyi *ayırt ettiyse* (`GECIS_2_3`), bu da onu
 * risk bandına taşıyor: `olayVarMi = 0` olan bir günün `risk_sonuc` satırı
 * varsa (worker her günü işlediği için genelde vardır) o satır aslında
 * "AZ RİSKLİ" değil "hiç ölçüm yok" demek. Geriye dönük düzeltme aynı
 * mantık: `gunluk_ozet` ile eşleşip `olayVarMi = 0` olan satırlar
 * `YETERSIZ`'e çekiliyor, geri kalan `HESAPLANDI` varsayılanında kalıyor
 * (`ESKI` yalnızca gösterim anında türetilir, satırda hiç saklanmaz —
 * bkz. `RiskDetayViewModel`).
 */
val GECIS_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `risk_sonuc` ADD COLUMN `veriDurumu` TEXT NOT NULL DEFAULT 'HESAPLANDI'",
        )
        db.execSQL(
            "UPDATE `risk_sonuc` SET `veriDurumu` = 'YETERSIZ' WHERE `tarih` IN " +
                "(SELECT `tarih` FROM `gunluk_ozet` WHERE `olayVarMi` = 0)",
        )
    }
}

/**
 * Sürüm 6 → 7 (6 Ağustos 2026).
 *
 * `aktivite_oturumu` eklendi — uygulamanın kendi yürüyüş oturumları artık
 * kalıcı (`gorev2.md` §4). Var olan hiçbir tablo değişmediği için veri
 * taşıma adımı yok.
 */
val GECIS_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `aktivite_oturumu` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`baslangic` INTEGER NOT NULL, `bitis` INTEGER NOT NULL, `tarih` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aktivite_oturumu_tarih` " +
                "ON `aktivite_oturumu` (`tarih`)",
        )
    }
}

/** Veritabanı kurulurken uygulanacak tüm geçişler. */
val TUM_GECISLER = arrayOf(GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5, GECIS_5_6, GECIS_6_7)
