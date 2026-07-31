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

/** Veritabanı kurulurken uygulanacak tüm geçişler. */
val TUM_GECISLER = arrayOf(GECIS_1_2, GECIS_2_3)
