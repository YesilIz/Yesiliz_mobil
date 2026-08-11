package tr.org.yesilay.yesiliz.data.yerel.entity

import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Trend
import tr.org.yesilay.yesiliz.risk.VeriDurumu

/**
 * Bir risk bandı yalnızca yeterli veriden hesaplandıysa gösterilebilir.
 *
 * Room metin alanları eski/bozuk ya da uygulamadan daha yeni bir enum değeri
 * içerebilir. UI akışında `valueOf` kullanmak tek satır yüzünden ekranı
 * kalıcı olarak çökertirdi; bilinmeyen değer güvenli biçimde “gösterme”ye döner.
 */
internal fun RiskSonucEntity.gosterilebilirBant(): Bant? {
    if (veriDurumu != VeriDurumu.HESAPLANDI.name) return null
    return Bant.entries.firstOrNull { it.name == gosterilenBant }
}

/** Histerezis yeniden oynatımında bilinmeyen eski adaylar atlanır. */
internal fun RiskSonucEntity.adayBantVeyaNull(): Bant? =
    Bant.entries.firstOrNull { it.name == adayBant }

/** Bilinmeyen durum, risk iddiası üretmemek için yetersiz kabul edilir. */
internal fun String.veriDurumuVeyaYetersiz(): VeriDurumu =
    VeriDurumu.entries.firstOrNull { it.name == this } ?: VeriDurumu.YETERSIZ

/** Bilinmeyen trend, yön iddiası üretmek yerine gizlenir. */
internal fun String.trendVeyaNull(): Trend? = Trend.entries.firstOrNull { it.name == this }
