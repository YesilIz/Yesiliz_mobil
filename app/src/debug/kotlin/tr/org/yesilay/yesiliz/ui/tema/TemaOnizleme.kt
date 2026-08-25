package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.buyukHarf

/**
 * Tema token galerisi — canlı stil kılavuzu.
 *
 * Amacı iki tane:
 *  1. `tema/` dosyalarının DESIGN.md §1 ile uyuştuğunu **gözle** doğrulamak
 *  2. Sonraki ekranları yazarken hangi tokenın neye benzediğini göstermek
 *
 * **`src/debug`'da yaşıyor** (5. tarama G5-012). Bir zamanlar `MainActivity`
 * doğrudan buraya bağlanıyordu; gerçek ana ekran geldiğinden beri hiçbir
 * yerden ulaşılmıyor — `Rota`'da karşılığı yok, `NavHost`'a kayıtlı değil,
 * tek çağıranı kendi `@Preview`'ı. `src/main`'de durduğu sürece release
 * kaynak ağacında "bu ekran nerede açılıyor?" sorusunu doğuruyordu.
 * `AdimTaniAlicisi` / `DemoVeriModulu` ile aynı kurulmuş desen; Android
 * Studio'nun `@Preview` paneli debug kaynak setinde de çalışıyor.
 *
 * DİKKAT: bu dosyada da hardcoded renk/ölçü yok — kural kendine de uygulanıyor.
 * Tek istisna: galeri kutucuklarının boyutu gibi salt sunum ölçüleri.
 */
@Composable
fun TemaOnizleme(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Olcu.EkranKenari, vertical = Bosluk.Xl),
        verticalArrangement = Arrangement.spacedBy(Olcu.BolumArasi),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Bosluk.Xs)) {
            Text("Yeşilİz", style = MaterialTheme.typography.displayLarge)
            Text(
                "Tasarım sistemi · DESIGN.md §1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Bolum("Tipografi") {
            Text("Display 32 Bold", style = MaterialTheme.typography.displayLarge)
            Text("H1 24 Semibold", style = MaterialTheme.typography.headlineLarge)
            Text("H2 20 Semibold", style = MaterialTheme.typography.headlineMedium)
            Text("Body 16 Regular — ığüşöçİĞÜŞÖÇ", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Caption 14 Regular",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("LABEL 12 MEDIUM", style = MaterialTheme.typography.labelMedium)
        }

        Bolum("Renkler") {
            Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                RenkKutusu(YesilAna, "ana")
                RenkKutusu(YesilKoyu, "koyu")
                RenkKutusu(Yesil500, "500")
                RenkKutusu(Yesil200, "200")
                RenkKutusu(Yesil50, "50")
            }
        }

        Bolum("Risk bandı — kırmızı yok") {
            Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                BantRozeti("AZ RİSKLİ", YesilizTema.risk.az)
                BantRozeti("ORTA RİSKLİ", YesilizTema.risk.orta)
                BantRozeti("ÇOK RİSKLİ", YesilizTema.risk.cok)
            }
        }

        Bolum("Butonlar") {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Olcu.ButonYuksekligi),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Başla", style = MaterialTheme.typography.bodyLarge)
            }
            // DESIGN.md §1: kenarlık #00AE42 1.5dp, yeşil metin, şeffaf zemin.
            // M3 varsayılanı kenarlığı `outline` (#E3EDE6) yapıyor — çok soluk kalıyor.
            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Olcu.ButonYuksekligi),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(Olcu.ButonKenarligi, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Şimdi değil", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Bolum("Kart, chip, ilerleme") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Golge.Kart),
            ) {
                Column(
                    modifier = Modifier.padding(Olcu.KartIci),
                    verticalArrangement = Arrangement.spacedBy(Olcu.Aralik),
                ) {
                    Text("Bugünün özeti", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Bugün 4 saat 20 dakika ekrandaydın. Yarın 20 dakika " +
                            "azaltmayı dener misin?",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                        Cip("15 dk")
                        Cip("Doğrulanmış")
                        Cip("Bugün 1 kez")
                    }
                    IlerlemeCubugu(oran = 0.62f)
                }
            }
        }

        Spacer(Modifier.height(Bosluk.Xl))
    }
}

@Composable
private fun Bolum(baslik: String, icerik: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
        Text(
            baslik.buyukHarf(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Bosluk.S), content = icerik)
    }
}

@Composable
private fun RenkKutusu(renk: Color, etiket: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Bosluk.Xs),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(renk, RoundedCornerShape(Yaricap.Cip))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Yaricap.Cip)),
        )
        Text(etiket, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BantRozeti(metin: String, renk: Color) {
    Box(
        Modifier
            .background(renk, HapSekli)
            .padding(horizontal = Olcu.Aralik, vertical = Bosluk.S),
    ) {
        Text(
            metin,
            style = MaterialTheme.typography.labelMedium,
            color = Beyaz,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Cip(metin: String) {
    Box(
        Modifier
            .background(Yesil50, RoundedCornerShape(Yaricap.Cip))
            .padding(horizontal = Bosluk.S + Bosluk.Xs, vertical = Bosluk.Xs + 2.dp),
    ) {
        Text(metin, style = MaterialTheme.typography.labelMedium, color = YesilKoyu)
    }
}

/**
 * DESIGN.md §1: yükseklik 8dp, hap şekli, dolu `#00AE42`, boş `#c4dec9`.
 * Segment yok — organik büyümeyi temsil eden kesintisiz dolgu.
 */
@Composable
private fun IlerlemeCubugu(oran: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Olcu.IlerlemeYuksekligi)
            .background(Yesil200, HapSekli),
    ) {
        Box(
            Modifier
                .fillMaxWidth(oran)
                .height(Olcu.IlerlemeYuksekligi)
                .background(YesilAna, HapSekli),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1600)
@Composable
private fun TemaOnizlemeOnizleme() {
    YesilizTemasi { TemaOnizleme() }
}
