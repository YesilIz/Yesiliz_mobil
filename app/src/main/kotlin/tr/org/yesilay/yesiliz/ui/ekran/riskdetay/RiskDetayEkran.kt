package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktor
import tr.org.yesilay.yesiliz.risk.FaktorSeviyesi
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.IlerlemeCubugu
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisna
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisnaZemin
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.RuhHaliBas
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Risk analizi — `docs/tasarim-referans.md §2 / 30`.
 * İstatistik ekranındaki risk kartından açılıyor.
 */
@Composable
fun RiskDetayEkran(
    onGeri: () -> Unit,
    onEylem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RiskDetayViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(baslik = "Risk analizi", onGeri = onGeri, modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(Olcu.Aralik))

            if (!durum.veriVarMi || durum.bant == null) {
                Kart {
                    BosDurum(
                        baslik = "Henüz yeterli veri yok",
                        aciklama = "Birkaç günlük kullanım biriktiğinde risk " +
                            "analizin burada görünecek.",
                    )
                }
                return@YesilBaslikliEkran
            }

            Kart {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DaireselGosterge(
                        oran = durum.oran,
                        renkFirca = Gradyan.RiskHalkasi,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = durum.bantEtiketiKisa,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "risk düzeyi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinUcuncul,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Bosluk.M))
                    Text(
                        text = durum.aciklama,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                        textAlign = TextAlign.Center,
                    )
                    durum.trendMesaji?.let { mesaj ->
                        Spacer(modifier = Modifier.height(Bosluk.S))
                        Text(
                            text = mesaj,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = YesilAna,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            BolumBasligi("Etkileyen faktörler")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart {
                Yigin {
                    durum.faktorler.forEach { faktor -> FaktorSatiri(faktor) }
                }
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            BolumBasligi("Önerilen eylemler")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Yigin {
                durum.oneriler.forEach { oneri ->
                    Kart {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
                        ) {
                            IkonRozeti(ikon = oneri.ikon)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = oneri.baslik,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = oneri.aciklama,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MetinUcuncul,
                                )
                            }
                            Cip(
                                metin = oneri.eylemMetni,
                                zemin = YesilNane,
                                renk = YesilAna,
                                onTikla = { onEylem(oneri.id) },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        }
    }
}

@Composable
private fun FaktorSatiri(faktor: EtkileyenFaktor) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = faktor.ad,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Cip(
                metin = faktor.seviye.etiket,
                zemin = faktor.seviye.zemin(),
                renk = faktor.seviye.renk(),
            )
        }
        Spacer(modifier = Modifier.height(Bosluk.Xs))
        IlerlemeCubugu(
            oran = faktor.oran.toFloat(),
            renk = faktor.seviye.renk(),
        )
        Spacer(modifier = Modifier.height(Bosluk.M))
    }
}

/**
 * [FaktorSeviyesi.YUKSEK] → `KirmiziIstisna` — CLAUDE.md §12 K10'daki iki
 * istisnadan biri, risk analizi ekranında yalnızca burada kullanılır.
 */
private fun FaktorSeviyesi.renk(): Color = when (this) {
    FaktorSeviyesi.DUSUK -> YesilAna
    FaktorSeviyesi.ORTA -> RuhHaliBas
    FaktorSeviyesi.YUKSEK -> KirmiziIstisna
}

private fun FaktorSeviyesi.zemin(): Color = when (this) {
    FaktorSeviyesi.DUSUK -> YesilNane
    FaktorSeviyesi.ORTA -> RuhHaliBas.copy(alpha = 0.15f)
    FaktorSeviyesi.YUKSEK -> KirmiziIstisnaZemin
}
