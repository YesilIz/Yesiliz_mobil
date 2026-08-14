package tr.org.yesilay.yesiliz.ui.ekran.istatistik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.risk.Bant
import kotlin.math.roundToInt
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.SatirBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.RiskGirisKarti
import tr.org.yesilay.yesiliz.ui.bilesen.UyariSeridi
import tr.org.yesilay.yesiliz.ui.bilesen.YenileDugmesi
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.CubukGrafik
import tr.org.yesilay.yesiliz.ui.bilesen.veri.YiginBar
import tr.org.yesilay.yesiliz.ui.bilesen.veri.YiginLejant
import tr.org.yesilay.yesiliz.ui.kisaBin
import tr.org.yesilay.yesiliz.ui.eksenSuresi
import tr.org.yesilay.yesiliz.ui.sureMetni
import tr.org.yesilay.yesiliz.ui.tema.AdimMavi
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.HapSekli
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.YesilNane
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * İstatistik — `docs/tasarim-referans.md §2 / 09`.
 *
 * Üç kart: ekran süresi, kategori dağılımı, adım. Hepsi aynı iskeleti
 * paylaşıyor (etiket · büyük değer · tarih · grafik · karşılaştırma satırı).
 */
@Composable
fun IstatistikEkran(
    modifier: Modifier = Modifier,
    onRiskDetay: () -> Unit = {},
    viewModel: IstatistikViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val tazeleniyor by viewModel.tazeleniyor.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "İstatistik",
        modifier = modifier,
        sagEylem = { YenileDugmesi(onTikla = viewModel::tazele, donuyor = tazeleniyor) },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(Olcu.Aralik))

            AralikSecici(secili = durum.aralik, onSec = viewModel::aralikSecildi)

            durum.riskBandi?.let { bant ->
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                RiskGirisKarti(bant = bant, onTikla = onRiskDetay)
            }

            Spacer(modifier = Modifier.height(Olcu.Aralik))

            if (durum.gunSayisi == 0) {
                BosDurum(
                    baslik = "Henüz yeterli veri yok",
                    aciklama = "Birkaç gün ölçelim. Kullanımın biriktikçe burada " +
                        "günlük dağılımını ve karşılaştırmalarını göreceksin.",
                )
                return@Column
            }

            Yigin {
                OlcumKarti(
                    etiket = durum.ekranKartBasligi,
                    deger = durum.ekranGosterilen.sureMetni(),
                    birim = "ekran süresi",
                    tarih = if (durum.secimVarMi) "" else durum.tarihAraligi,
                    karsilastirma = durum.ekranAltMetni,
                    azaldi = durum.ekranAzaldi,
                    onOrtalamayaDon = if (durum.secimVarMi) viewModel::secimiKaldir else null,
                ) {
                    CubukGrafik(
                        cubuklar = durum.ekranCubuklari,
                        vurgulanan = durum.ekranVurgu,
                        secili = durum.seciliIndeks,
                        onCubukSecildi = viewModel::gunSecildi,
                        renk = YesilAna,
                        soluk = Yesil200,
                        eksenBicimi = { it.roundToInt().eksenSuresi() },
                    )
                }

                // Penceredeki bazı günler ölçülemedi. Grafikte boş yer
                // görünüyor; ortalamanın hangi günlerden hesaplandığını
                // söylemezsek kullanıcı eksik veriyi "az kullanmışım" diye
                // okur.
                if (durum.olculenGunSayisi in 1 until durum.gunSayisi) {
                    UyariSeridi(
                        metin = "${durum.gunSayisi} günün ${durum.olculenGunSayisi} tanesi " +
                            "ölçüldü. Ortalama yalnızca o günlerden hesaplandı; " +
                            "ölçülemeyen günler grafikte boş görünüyor.",
                    )
                }

                Kart {
                    Text(
                        text = "Kategori dağılımı",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    // Gün seçiliyken dağılım da o güne ait — hangi veriye
                    // baktığı belirsiz kalmasın.
                    if (durum.secimVarMi) {
                        Text(
                            text = durum.seciliBaslik,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinIkincil,
                        )
                    }
                    Spacer(modifier = Modifier.height(Olcu.Aralik))
                    YiginBar(dilimler = durum.kategoriler)
                    Spacer(modifier = Modifier.height(Olcu.Aralik))
                    YiginLejant(dilimler = durum.kategoriler)
                }

                OlcumKarti(
                    etiket = durum.ekranKartBasligi,
                    deger = durum.adimGosterilen.binlik(),
                    birim = "adım",
                    tarih = if (durum.secimVarMi) "" else durum.tarihAraligi,
                    karsilastirma = durum.adimAltMetni,
                    azaldi = !durum.adimArtti,
                    onOrtalamayaDon = if (durum.secimVarMi) viewModel::secimiKaldir else null,
                ) {
                    CubukGrafik(
                        cubuklar = durum.adimCubuklari,
                        vurgulanan = durum.adimVurgu,
                        secili = durum.seciliIndeks,
                        onCubukSecildi = viewModel::gunSecildi,
                        renk = YesilAna,
                        soluk = AdimMavi,
                        eksenBicimi = { if (it < 1000f) it.roundToInt().toString() else it.roundToInt().kisaBin() },
                    )
                }

                // Adım sayısı Fit/Samsung Health'ten %10-20 farklı çıkıyor
                // ve kullanıcı bunu hata sanıyor. Fark gerçek: o uygulamalar
                // ham sayacı kendi hareket sınıflandırıcılarından geçirip
                // yanlış pozitifleri eliyor. Kaynağı söylemek farkı
                // açıklanabilir kılıyor.
                durum.adimKaynagi?.let { kaynak ->
                    Text(
                        text = "Adım kaynağı: $kaynak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!durum.secimVarMi) {
                    Text(
                        text = "Bir güne dokunarak o günün dökümünü görebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        }
    }
}

/** Gün · Hafta · Ay — seçili olan beyaz kart gibi öne çıkıyor. */
@Composable
private fun AralikSecici(secili: Aralik, onSec: (Aralik) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HapSekli)
            .background(YesilNane)
            .padding(Bosluk.Xs),
    ) {
        Aralik.entries.forEach { aralik ->
            val aktif = aralik == secili
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(HapSekli)
                    .background(if (aktif) Beyaz else Color.Transparent)
                    .clickable { onSec(aralik) }
                    .padding(vertical = Bosluk.S),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = aralik.etiket,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (aktif) FontWeight.Bold else FontWeight.Normal,
                    color = if (aktif) MetinBirincil else MetinIkincil,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Ekran süresi ve adım kartlarının ortak iskeleti. */
@Composable
private fun OlcumKarti(
    etiket: String,
    deger: String,
    birim: String,
    tarih: String,
    karsilastirma: String,
    azaldi: Boolean,
    onOrtalamayaDon: (() -> Unit)? = null,
    grafik: @Composable () -> Unit,
) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Seçim varken başlık bir TARİH ("24 Temmuz Perşembe"); onu
            // `BolumBasligi`'nden geçirmek büyük harfe çevirip dar sütunda
            // harf harf sardırıyordu. Bölüm başlığı yalnızca "ORTALAMA"
            // gibi kısa etiketler için.
            if (onOrtalamayaDon == null) {
                BolumBasligi(etiket, modifier = Modifier.weight(1f))
            } else {
                Text(
                    text = etiket,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinIkincil,
                    modifier = Modifier.weight(1f),
                )
            }
            // Seçimden çıkış yolu her zaman görünür olmalı — kullanıcı
            // grafiğe dokunduktan sonra ortalamaya nasıl döneceğini
            // aramak zorunda kalmasın.
            onOrtalamayaDon?.let { SatirBaglanti(metin = "Ortalamaya dön", onTikla = it) }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = deger,
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
            )
            Text(
                text = " $birim",
                style = MaterialTheme.typography.headlineMedium,
                color = MetinBirincil,
                modifier = Modifier.padding(bottom = Bosluk.Xs),
            )
        }
        Text(
            text = tarih,
            style = MaterialTheme.typography.bodyLarge,
            color = MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        grafik()
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
        ) {
            Icon(
                imageVector = if (azaldi) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = YesilAna,
                modifier = Modifier.size(Olcu.Aralik),
            )
            Text(
                text = karsilastirma,
                style = MaterialTheme.typography.bodyLarge,
                color = MetinBirincil,
            )
        }
    }
}

/**
 * Risk analizi giriş kartı — `docs/tasarim-referans.md §2 / 09`.
 * `riskdetay` ekranına (30) açılıyor. Koyu yeşil gradyan — kırmızı YOK,
 * uyarı ikonu risk bandını değil "bir şey var, bak" iması taşıyor.
 */
/** Grafik ekseninin tavanı — tasarımda ekran 8s, adım 8b'de sabit. */
