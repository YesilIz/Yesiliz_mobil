package tr.org.yesilay.yesiliz.data.depo

import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Verilerimi kalıcı sil" ve "Hesabımı kalıcı sil" (ekran 25/26) — ikisi de
 * MVP'de aynı işlemi yapıyor: gerçek hesap sistemi Faz 2'de geliyor
 * (CLAUDE.md §11 "Hesap ekranları"), o zamana kadar "hesabı silmek" ile
 * "yerel veriyi silmek" arasında fark yok.
 *
 * Ham kullanım olayları/özetler Room'da, tekil tercihler (hedef, ilk kurulum
 * tarihi, ebeveyn eşleşmesi vb.) DataStore'da — ikisi de temizlenmeli, yoksa
 * "silindi" dedikten sonra hedef/eşleşme gibi izler kalır.
 */
@Singleton
class VeriYonetimiDeposu @Inject constructor(
    private val db: YesilizDb,
    private val tercihler: Tercihler,
) {
    suspend fun tumVerileriSil() {
        db.clearAllTables()
        tercihler.sifirla()
    }
}
