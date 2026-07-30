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
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.CubukGrafik
import tr.org.yesilay.yesiliz.ui.bilesen.veri.YiginBar
import tr.org.yesilay.yesiliz.ui.bilesen.veri.YiginLejant
import tr.org.yesilay.yesiliz.ui.kisaBin
import tr.org.yesilay.yesiliz.ui.kisaSaat
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

    YesilBaslikliEkran(baslik = "İstatistik", modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(Olcu.Aralik))

            AralikSecici(secili = durum.aralik, onSec = viewModel::aralikSecildi)

            durum.riskBandi?.let { bant ->
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                RiskKarti(bant = bant, onTikla = onRiskDetay)
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
                    etiket = "Ortalama",
                    deger = durum.ekranOrtalama.sureMetni(),
                    birim = "ekran süresi",
                    tarih = durum.tarihAraligi,
                    karsilastirma = durum.ekranFark,
                    azaldi = durum.ekranAzaldi,
                ) {
                    CubukGrafik(
                        cubuklar = durum.ekranCubuklari,
                        vurgulanan = durum.ekranVurgu,
                        renk = YesilAna,
                        soluk = Yesil200,
                        eksenEtiketleri = listOf(EkranTavani.kisaSaat(), (EkranTavani / 2).kisaSaat(), "0"),
                    )
                }

                Kart {
                    Text(
                        text = "Kategori dağılımı",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Spacer(modifier = Modifier.height(Olcu.Aralik))
                    YiginBar(dilimler = durum.kategoriler)
                    Spacer(modifier = Modifier.height(Olcu.Aralik))
                    YiginLejant(dilimler = durum.kategoriler)
                }

                OlcumKarti(
                    etiket = "Ortalama",
                    deger = durum.adimOrtalama.binlik(),
                    birim = "adım",
                    tarih = durum.tarihAraligi,
                    karsilastirma = durum.adimFark,
                    azaldi = !durum.adimArtti,
                ) {
                    CubukGrafik(
                        cubuklar = durum.adimCubuklari,
                        vurgulanan = durum.adimVurgu,
                        renk = YesilAna,
                        soluk = AdimMavi,
                        eksenEtiketleri = listOf(AdimTavani.kisaBin(), (AdimTavani / 2).kisaBin(), "0"),
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
    grafik: @Composable () -> Unit,
) {
    Kart {
        BolumBasligi(etiket)
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
@Composable
private fun RiskKarti(bant: Bant, onTikla: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Gradyan.PuanKarti)
            .clickable(onClick = onTikla)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
    ) {
        IkonRozeti(
            ikon = Icons.Filled.WarningAmber,
            zemin = Beyaz.copy(alpha = 0.15f),
            renk = Beyaz,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Risk analizi: ${bant.kisaEtiket()} düzey",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Beyaz,
            )
            Text(
                text = "Sonuçları ve önerilen eylemleri gör",
                style = MaterialTheme.typography.bodySmall,
                color = Beyaz.copy(alpha = 0.8f),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Beyaz,
        )
    }
}

/** Ekranda gösterilen kısa bant adı — `Bant.etiket` ("ORTA RİSKLİ") teşhis gibi duruyor. */
private fun Bant.kisaEtiket(): String = when (this) {
    Bant.AZ -> "Düşük"
    Bant.ORTA -> "Orta"
    Bant.COK -> "Yüksek"
}

/** Grafik ekseninin tavanı — tasarımda ekran 8s, adım 8b'de sabit. */
private const val EkranTavani = 480
private const val AdimTavani = 8_000
