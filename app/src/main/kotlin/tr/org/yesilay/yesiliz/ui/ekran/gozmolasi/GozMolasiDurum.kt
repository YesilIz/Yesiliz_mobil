package tr.org.yesilay.yesiliz.ui.ekran.gozmolasi

data class GozMolasiDurum(
    val ilerleme: GozMolasiIlerleme = GozMolasiIlerleme(
        kalanSaniye = GozMolasiHesapla.BAKIS_SANIYE,
        oran = 0f,
        tamamlandiMi = false,
    ),
    val basladi: Boolean = false,
    val puanYazildi: Boolean = false,
)
