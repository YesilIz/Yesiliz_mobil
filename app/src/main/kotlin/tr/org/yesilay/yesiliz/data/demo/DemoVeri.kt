package tr.org.yesilay.yesiliz.data.demo

/**
 * Emülatörde ekranların dolu görünmesi için örnek veri üretir.
 *
 * ## Neden arayüz
 *
 * Gerçek uygulaması `app/src/debug/` **kaynak setinde**; release derlemesinde
 * o dosya derleyiciye hiç girmiyor ve yerine `app/src/release/`'deki no-op
 * geçiyor. Yani bir bayrağı unutmak sahte veriyi kullanıcıya sızdıramaz —
 * koruma derleme zamanında.
 *
 * ## Neden gerekli
 *
 * CLAUDE.md §10: taze emülatörde üçüncü parti uygulama yok, `queryEvents`
 * yalnızca açılıştan beri veri veriyor. `d_efektif = 0`, `alpha = 1.0`,
 * motor hep `S ≈ 0` üretiyor — yani gerçek veriyle emülatörde grafikler
 * boş kalıyor. Döngü doğrulaması gerçek telefon istiyor; tohum yalnızca
 * arayüzü göstermek için.
 *
 * **Tohum motoru atlamıyor:** yalnızca `gunluk_ozet` girdilerini yazıyor,
 * skoru ve bandı gerçek `RiskDeposu.gunuHesapla` üretiyor.
 */
interface DemoVeri {

    /** Bu derlemede demo tohumu var mı — Profil'de satırı göstermek için. */
    val kullanilabilir: Boolean

    /** Son [gun] güne örnek özet yazar ve risk/puan hesabını koşturur. */
    suspend fun tohumla(gun: Int = VARSAYILAN_GUN)

    companion object {
        /** Haftalık grafiği doldurmaya yetecek gün sayısı. */
        const val VARSAYILAN_GUN = 14
    }
}
