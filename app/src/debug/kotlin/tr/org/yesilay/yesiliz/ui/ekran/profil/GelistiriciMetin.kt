package tr.org.yesilay.yesiliz.ui.ekran.profil

/**
 * Geliştirici alt sayfasının metinleri — **debug sürümü**.
 *
 * Kaynak setine ayrılmasının sebebi `DemoVeri` ile aynı: geliştirme
 * sunucusunun şifresiz adresleri (`10.0.2.2`, LAN örneği) yayınlanan
 * APK'nın içine **girmesin**. Alt sayfa release'de zaten görünmüyor
 * (`durum.demoKullanilabilir` false) ama string sabitleri derlemeye
 * giriyordu; görev 1'in tamamlanma ölçütü "release APK içinde 10.0.2.2
 * ya da şifresiz HTTP adresi bulunmuyor" diyor.
 */
internal const val SUNUCU_ADRESI_ACIKLAMA =
    "Bu derleme de canlı sunucuya kendiliğinden bağlanır; burayı " +
        "değiştirmen gerekmez. Yalnızca yerel backend'e bağlanacaksan yaz: " +
        "emülatörde 10.0.2.2, gerçek cihazda geliştirme makinenin yerel ağ " +
        "adresi (ya da `adb reverse` ile 127.0.0.1). Canlı sunucuya dönmek " +
        "için \"Varsayılana dön\"."

internal const val SUNUCU_ADRESI_ORNEK = "http://10.0.2.2:8000/"
