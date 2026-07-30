package tr.org.yesilay.yesiliz.ui.ekran.kategori

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.SecenekKarti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.adi

/**
 * Kategorize et — `docs/tasarim-referans.md §2 / 32` + kategori seç
 * alt sayfası (33).
 *
 * ViewModel [KategoriEkran] ile paylaşılmıyor (ayrı NavHost girdisi, ayrı
 * örnek) ama aynı sınıf: eşleme tablosunu okuyup yazan tek yer orası.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorizeEtEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KategoriViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val sayfaDurumu = rememberModalBottomSheetState()

    YesilBaslikliEkran(
        baslik = "Kategorize et",
        onGeri = onGeri,
        icerikZemini = Beyaz,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                if (!durum.yukleniyor && durum.kategorisizler.isEmpty()) {
                    BosDurum(
                        baslik = "Her uygulamanın kategorisi belli",
                        aciklama = "Yeni bir uygulama yüklediğinde ve tablomuz onu " +
                            "tanımadığında burada görünecek.",
                    )
                } else {
                    Text(
                        text = "Henüz kategorisi olmayan ${durum.kategorisizler.size} " +
                            "uygulama var. Birine dokun, kategorisini seç.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                    )

                    // 3 sütunlu ızgara. LazyVerticalGrid kullanılmıyor:
                    // ekranın tamamı zaten kaydırılıyor, iç içe kaydırma
                    // ölçüm hatası verir.
                    durum.kategorisizler.chunked(SUTUN).forEach { satir ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
                        ) {
                            satir.forEach { uygulama ->
                                IzgaraHucresi(
                                    satir = uygulama,
                                    onTikla = { viewModel.uygulamaSecildi(uygulama) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Son satır eksikse boş hücrelerle hizala.
                            repeat(SUTUN - satir.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    durum.secilenUygulama?.let { uygulama ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.uygulamaSecildi(null) },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            KategoriSecIcerik(
                satir = uygulama,
                onKaydet = { kategori -> viewModel.kategoriKaydet(uygulama.paketAdi, kategori) },
            )
        }
    }
}

@Composable
private fun IzgaraHucresi(
    satir: UygulamaSatiri,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onTikla)
            .padding(vertical = Bosluk.S),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UygulamaAvatari(ad = satir.ad, olcu = IzgaraAvatari)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = satir.ad,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Kategori seç alt sayfası — tasarım 33. */
@Composable
private fun KategoriSecIcerik(satir: UygulamaSatiri, onKaydet: (Kategori) -> Unit) {
    var secilen by remember { mutableStateOf(satir.onerilen ?: Kategori.OTHER) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UygulamaAvatari(satir.ad)
            Column(modifier = Modifier.padding(start = Olcu.Aralik)) {
                Text(
                    text = satir.ad,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "Bu uygulama için bir kategori seç",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
            }
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Column(verticalArrangement = Arrangement.spacedBy(Bosluk.S)) {
            SECILEBILIR.forEach { kategori ->
                SecenekKarti(
                    ikon = kategori.ikonu(),
                    baslik = kategori.adi,
                    aciklama = if (kategori == satir.onerilen) "Önerilen" else "",
                    secili = kategori == secilen,
                    onTikla = { secilen = kategori },
                    onayIkonuMu = true,
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Kategoriyi kaydet", onTikla = { onKaydet(secilen) })
    }
}

/**
 * Seçilebilir kategoriler — [Kategori.SISTEM] listede **yok**.
 *
 * Sistem kategorisi risk hesabından düşülüyor (CLAUDE.md §12 K9); kullanıcıya
 * "bu uygulamayı sistem yap" seçeneği vermek, ekran süresini hesaptan
 * çıkarmanın kapısını açardı.
 */
private val SECILEBILIR = listOf(
    Kategori.SOCIAL,
    Kategori.ENTERTAINMENT,
    Kategori.COMMUNICATION,
    Kategori.GAMES,
    Kategori.PRODUCTIVITY,
    Kategori.EDUCATION,
    Kategori.HEALTH,
    Kategori.OTHER,
)

private fun Kategori.ikonu(): ImageVector = when (this) {
    Kategori.SOCIAL -> Icons.Filled.Groups
    Kategori.ENTERTAINMENT -> Icons.Filled.Videocam
    Kategori.COMMUNICATION -> Icons.Filled.Groups
    Kategori.GAMES -> Icons.Filled.SportsEsports
    Kategori.PRODUCTIVITY -> Icons.Filled.WorkOutline
    Kategori.EDUCATION -> Icons.Filled.School
    Kategori.HEALTH -> Icons.Filled.FavoriteBorder
    Kategori.OTHER -> Icons.Filled.MoreHoriz
    Kategori.SISTEM -> Icons.Filled.MoreHoriz
}

private const val SUTUN = 3
private val IzgaraAvatari = Olcu.AvatarKucuk
