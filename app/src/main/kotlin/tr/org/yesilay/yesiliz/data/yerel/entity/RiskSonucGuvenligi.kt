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

/**
 * Histerezis yeniden oynatımında bilinmeyen eski adaylar atlanır.
 *
 * **Ölçülmemiş gün de atlanır.** Gecelik worker ölçüm bulamadığı güne de
 * satır yazıyor ve o satırın adayı `R = 0` üzerinden `AZ` çıkıyor; bunlar
 * gözlem sayılınca takipçide `mevcutBant = AZ` kuruluyor ve gerçekten
 * ölçülen ilk gün "bant değişimi" sayılıp [Sabitler.GUN_YUKARI] gün
 * bekletiliyordu — anketini yenileyip `adayBant = COK` üreten kullanıcı
 * ekranda "Düşük" görüyordu. `alpha`'nın `d` girdisi bu ayrımı zaten
 * yapıyor (`GunlukOzetDao.gunSayisiEKadar`, `olayVarMi = 1`); histerezis
 * yapmıyordu (CLAUDE.md §9 "Satır var ≠ veri var", §12 K52).
 */
internal fun RiskSonucEntity.histerezisAdayi(): Bant? {
    if (veriDurumu.veriDurumuVeyaYetersiz() != VeriDurumu.HESAPLANDI) return null
    return Bant.entries.firstOrNull { it.name == adayBant }
}

/** Bilinmeyen durum, risk iddiası üretmemek için yetersiz kabul edilir. */
internal fun String.veriDurumuVeyaYetersiz(): VeriDurumu =
    VeriDurumu.entries.firstOrNull { it.name == this } ?: VeriDurumu.YETERSIZ

/** Bilinmeyen trend, yön iddiası üretmek yerine gizlenir. */
internal fun String.trendVeyaNull(): Trend? = Trend.entries.firstOrNull { it.name == this }
