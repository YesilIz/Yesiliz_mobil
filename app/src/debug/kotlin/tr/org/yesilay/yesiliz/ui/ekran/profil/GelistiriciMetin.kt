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
    "Yayınlanan sürüm canlı sunucuya kendiliğinden bağlanır; burayı " +
        "değiştirmen gerekmez. Debug derlemesinin varsayılanı 10.0.2.2 ve " +
        "o adres yalnızca emülatörde çalışır — gerçek cihazda geliştirme " +
        "makinenin yerel ağ adresini yaz."

internal const val SUNUCU_ADRESI_ORNEK = "http://192.168.1.20:8000/"
