package tr.org.yesilay.yesiliz.nudge

import tr.org.yesilay.yesiliz.data.kullanim.HamOlay
import tr.org.yesilay.yesiliz.data.kullanim.OlayTuru

/**
 * "Şu an ön planda ne var" — **SAF FONKSİYON**.
 *
 * [Tetikleyici] gibi bu da Android görmüyor; `NefesFazHesapla` /
 * `OturumCikar` / `AdimBirikim` emsali (CLAUDE.md §6, ekran/servis mantığı
 * saf bir alt-hesaba ayrıştırılabiliyorsa ayrılır).
 *
 * Niçin ayrı bir hesap: `Tetikleyici` "kaç dakikadır tüketimdesin"
 * sorusunu **oturumlardan** cevaplıyor ve saf fonksiyon olarak doğruydu.
 * Hatalı olan onu besleyen soruydu — servis ön plandaki uygulamayı
 * "penceredeki en son biten oturum" ile tahmin ediyordu. Oturum akışı
 * kendi paketimizi elediği için (`KullanimOkuyucu.sonPencereOlaylari`)
 * kullanıcı Yeşilİz'e geçtiğinde tüketim oturumu hiç kapanmıyor, bayrak
 * `true` takılıyordu (§12 K51).
 */
object OnPlanTespit {

    /**
     * Penceredeki ham olayları yürüyerek ön plandaki paketi bulur.
     *
     * @param olaylar pencere içindeki **ham** olaylar — kendi paketimiz de
     *   dahil olmalı (`KullanimOkuyucu.sonPencereHamOlaylari`); sırasızsa
     *   burada sıralanır
     * @param oncekiOnPlan bir önceki poll'ün sonucu. Pencerede hiç ön plan
     *   olayı yoksa kullanıcı aynı uygulamada kalmış demektir; bu olmadan
     *   pencereden uzun süren her oturum "ön plan bilinmiyor"a düşerdi.
     * @return ön plandaki paket; ekran kapalıysa ya da hiç bilinmiyorsa
     *   `null` — çağıran bunu **fail-closed** yorumlamalı (§4, §12 K37)
     */
    fun onPlandakiPaket(olaylar: List<HamOlay>, oncekiOnPlan: String? = null): String? {
        var onPlan = oncekiOnPlan
        for (olay in olaylar.sortedBy { it.an }) {
            when (olay.tur) {
                OlayTuru.ON_PLANA_GELDI -> onPlan = olay.paketAdi

                // Yalnızca ön plandaki paketin çıkışı ön planı boşaltır;
                // arka plandaki bir uygulamanın PAUSED olayı ilgisiz.
                OlayTuru.ARKA_PLANA_GITTI -> if (onPlan == olay.paketAdi) onPlan = null

                // Ekran kapalıyken ön planda bir şey yok: overlay'in
                // gösterilecek kimsesi de yok.
                OlayTuru.EKRAN_KAPANDI -> onPlan = null

                // Ekranın açılması tek başına bir uygulamayı öne getirmiyor;
                // asıl bilgi ardından gelen ON_PLANA_GELDI'de.
                OlayTuru.EKRAN_ACILDI -> Unit
            }
        }
        return onPlan
    }

    /**
     * Kullanıcı **şu an** tüketim kategorisinde bir uygulamada mı.
     *
     * [onPlanPaketi] `null` ise (ekran kapalı, ön plan hiç okunamadı)
     * cevap `false`: nudge çıkmaz. Ölçemediğimiz bir şeyi "evet" saymak
     * §4'ün "yapılmayan işi yapıldı gösterme" ailesine girerdi.
     */
    fun tuketimdeMi(onPlanPaketi: String?, tuketimPaketiMi: (String) -> Boolean): Boolean =
        onPlanPaketi != null && tuketimPaketiMi(onPlanPaketi)
}
