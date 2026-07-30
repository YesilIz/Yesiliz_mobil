package tr.org.yesilay.yesiliz.data.yerel

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room tip dönüştürücüleri.
 *
 * Tarih `LocalDate` olarak tutuluyor ve **ISO metin** olarak yazılıyor
 * (`2026-07-28`). Epoch gün sayısından daha uzun ama veritabanını elle
 * incelerken okunabilir olması prototip boyunca daha değerli.
 *
 * Zaman damgaları epoch milisaniye — `Instant` yerine `Long`.
 */
class Donusturucu {

    @TypeConverter
    fun tariheDonustur(metin: String?): LocalDate? = metin?.let(LocalDate::parse)

    @TypeConverter
    fun tarihtenDonustur(tarih: LocalDate?): String? = tarih?.toString()

    @TypeConverter
    fun aninaDonustur(milis: Long?): Instant? = milis?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun andanDonustur(an: Instant?): Long? = an?.toEpochMilli()

    /**
     * `surukleyiciler` listesi virgülle saklanır. Kategori anahtarlarında
     * virgül yok, ayrıştırma güvenli.
     */
    @TypeConverter
    fun listeyeDonustur(metin: String?): List<String> =
        if (metin.isNullOrEmpty()) emptyList() else metin.split(",")

    @TypeConverter
    fun listedenDonustur(liste: List<String>?): String = liste?.joinToString(",") ?: ""
}
