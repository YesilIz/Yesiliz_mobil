package tr.org.yesilay.yesiliz.data.saglik

/**
 * Donanım adım sayacının bir güne düşen payı — **saf hesap**.
 *
 * `Sensor.TYPE_STEP_COUNTER` gün başına adım vermiyor: **açılıştan beri**
 * biriken tek bir sayı veriyor, cihaz yeniden başlayınca sıfırdan başlıyor.
 * Yani "bugün kaç adım" sorusunun cevabı sensörde yok; örnekleyip **farkını**
 * biriktirmek gerekiyor. Biriken değer [AdimDurumu.toplam], sayacın en son
 * görülen ham hâli [AdimDurumu.sonSayac].
 *
 * Android'e dokunmadığı için emülatörsüz test edilebiliyor —
 * `OturumCikar` ve `NefesFazHesapla` ile aynı desen (CLAUDE.md §6).
 */
data class AdimDurumu(
    /** Bugün biriktirilen adım. */
    val toplam: Long = 0,

    /** Sayacın son örnekteki ham değeri — bir sonraki farkın tabanı. */
    val sonSayac: Long = 0,
)

/**
 * Yeni bir örnekle durumu ilerletir.
 *
 * Üç hâl var:
 *
 * | Durum | Ne yapılır | Niçin |
 * |---|---|---|
 * | [onceki] yok | `toplam = 0`, taban kurulur | Sayacın o ana kadarki değeri günün tamamına ait değil; sahiplenmek gün başına toplu bir sıçrama yazmak olurdu |
 * | `sayac >= sonSayac` | fark eklenir | Olağan hâl |
 * | `sayac < sonSayac` | `sayac` eklenir | Yeniden başlatma; sayaç sıfırlandığı için okunan değerin tamamı yeniden başlatmadan sonra atılmış |
 *
 * Günün ilk örneğinde sıfırdan başlamanın bedeli, son örnek ile gece yarısı
 * arasındaki adımların kaybı. Örnekleme sıklaştıkça bu pay küçülüyor;
 * **eksik saymak fazla saymaya yeğ**, çünkü aktivite skoru düşürüyor
 * (`docs/spec-risk.md`, sönümleme tavanı %15) — uydurulan her adım riski
 * haksız yere aşağı çeker.
 */
fun adimBirikimGuncelle(onceki: AdimDurumu?, sayac: Long): AdimDurumu = when {
    onceki == null -> AdimDurumu(toplam = 0, sonSayac = sayac)
    sayac >= onceki.sonSayac ->
        AdimDurumu(toplam = onceki.toplam + (sayac - onceki.sonSayac), sonSayac = sayac)

    else -> AdimDurumu(toplam = onceki.toplam + sayac, sonSayac = sayac)
}
