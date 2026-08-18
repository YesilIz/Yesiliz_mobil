package tr.org.yesilay.yesiliz.ui.ekran.bildirim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.SatirBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/** Bildirimler — `docs/tasarim-referans.md §2 / 35`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BildirimEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BildirimViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val sonSilinen by viewModel.sonSilinen.collectAsStateWithLifecycle()
    var temizleOnayi by remember { mutableStateOf(false) }
    val karMesajDurumu = remember { SnackbarHostState() }

    // Silme geri alınabilir olmalı: yanlışlıkla kaydıran kullanıcı kaydı
    // kalıcı olarak kaybetmemeli.
    LaunchedEffect(sonSilinen) {
        val kayit = sonSilinen ?: return@LaunchedEffect
        val sonuc = karMesajDurumu.showSnackbar(
            message = "Bildirim silindi",
            actionLabel = "Geri al",
            duration = SnackbarDuration.Short,
        )
        if (sonuc == SnackbarResult.ActionPerformed) {
            viewModel.silmeyiGeriAl()
        } else {
            viewModel.geriAlmaSuresiDoldu()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        YesilBaslikliEkran(
            baslik = "Bildirimler",
            onGeri = onGeri,
            icerikZemini = Beyaz,
            // Başlığın sağ eylem alanı 96dp ile SINIRLI (`debug.md` Y-2
            // düzeltmesi). "Tümünü oku" metin bağlantısı ile çöp ikonu
            // birlikte sığmıyor ve ikisi de görünmez oluyordu; ikon burada
            // kalıyor, metin bağlantısı listenin başına taşındı.
            sagEylem = {
                if (durum.gruplar.isNotEmpty()) {
                    IconButton(onClick = { temizleOnayi = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Tümünü temizle",
                            tint = Beyaz,
                        )
                    }
                }
            },
        ) {
            if (!durum.yukleniyor && durum.gruplar.isEmpty()) {
                Column(modifier = Modifier.padding(vertical = Olcu.EkranKenari)) {
                    BosDurum(
                        baslik = "Henüz bildirim yok",
                        aciklama = "Görev tamamladıkça ve hedefini tutturdukça " +
                            "buraya düşecek.",
                    )
                }
                return@YesilBaslikliEkran
            }

            // Düz `Column` + `verticalScroll` idi; kaydırarak silme için
            // her satırın kendi `SwipeToDismissBox`'ı gerekiyor ve uzun
            // listede tamamını bileşimde tutmak gereksiz.
            LazyColumn(
                contentPadding = PaddingValues(vertical = Olcu.EkranKenari),
                verticalArrangement = Arrangement.spacedBy(Bosluk.S),
            ) {
                // Okunmamış yoksa eylem de yok — tıklanınca hiçbir şey
                // olmayan bir bağlantı göstermek, düzeltmeye çalıştığımız
                // sorunun aynısı.
                if (durum.okunmamisVarMi) {
                    item(key = "tumunu-oku") {
                        SatirBaglanti(metin = "Tümünü oku", onTikla = viewModel::tumunuOku)
                    }
                }

                durum.gruplar.forEach { grup ->
                    item(key = "baslik-${grup.grup.name}") {
                        Column {
                            BolumBasligi(metin = grup.grup.baslik)
                            Spacer(modifier = Modifier.height(Bosluk.S))
                        }
                    }
                    items(grup.satirlar, key = { it.id }) { satir ->
                        KaydirilabilirBildirim(
                            satir = satir,
                            onSil = { viewModel.sil(satir.id) },
                        )
                    }
                    item(key = "bosluk-${grup.grup.name}") {
                        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
                    }
                }
            }
        }

        // Varsayılan M3 snackbar'ının eylem rengi MOR; projede o palet yok
        // (DESIGN.md §1). Zemin koyu yeşil, eylem açık yeşil.
        SnackbarHost(
            hostState = karMesajDurumu,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(Olcu.EkranKenari),
        ) { veri ->
            Snackbar(
                snackbarData = veri,
                containerColor = YesilKoyu,
                contentColor = Beyaz,
                actionColor = Yesil200,
            )
        }
    }

    if (temizleOnayi) {
        // Toplu silme geri alınamıyor (tek tek silmenin aksine), o yüzden
        // önce soruluyor. Kırmızı yok — onay dili destekleyici (§4).
        AlertDialog(
            onDismissRequest = { temizleOnayi = false },
            title = { Text("Tüm bildirimler silinsin mi?") },
            text = {
                Text(
                    "Defterdeki bütün kayıtlar kalkacak. Bu işlem geri " +
                        "alınamıyor; kazandığın puanlar etkilenmiyor.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hepsiniSil()
                        temizleOnayi = false
                    },
                ) {
                    Text("Temizle", color = YesilKoyu)
                }
            },
            dismissButton = {
                TextButton(onClick = { temizleOnayi = false }) {
                    Text("Vazgeç", color = MetinIkincil)
                }
            },
        )
    }
}

/**
 * Sağa kaydırınca çöp kovası çıkan satır.
 *
 * Yalnızca **tek yön** (başlangıçtan sona): iki yönlü kaydırma listede
 * yanlışlıkla tetiklenmeye çok açık ve buradaki tek eylem silme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaydirilabilirBildirim(
    satir: BildirimSatiriModel,
    onSil: () -> Unit,
) {
    val durum = rememberSwipeToDismissBoxState(
        confirmValueChange = { hedef ->
            if (hedef == SwipeToDismissBoxValue.StartToEnd) {
                onSil()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = durum,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Yaricap.Kart))
                    .background(Yesil50)
                    .padding(horizontal = Olcu.KartIci),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = YesilKoyu,
                )
            }
        },
    ) {
        BildirimSatiri(satir)
    }
}

@Composable
private fun BildirimSatiri(satir: BildirimSatiriModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(if (satir.okundu) Beyaz else Yesil50)
            .then(
                if (satir.okundu) {
                    Modifier.border(
                        width = Olcu.KartKenarligi,
                        color = Ayrac,
                        shape = RoundedCornerShape(Yaricap.Kart),
                    )
                } else {
                    Modifier
                },
            )
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        IkonRozeti(ikon = satir.ikon, zemin = Beyaz)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = satir.baslik,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = satir.govde,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = satir.zamanEtiketi,
                style = MaterialTheme.typography.labelMedium,
                color = MetinUcuncul,
            )
        }
        if (!satir.okundu) {
            Box(
                modifier = Modifier
                    .size(NoktaBoyu)
                    .clip(RoundedCornerShape(NoktaBoyu / 2))
                    .background(YesilAna),
            )
        }
    }
}

private val NoktaBoyu: Dp = Bosluk.S
