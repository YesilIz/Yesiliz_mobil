package tr.org.yesilay.yesiliz.ui.ekran.nefes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.EkranAcikTut
import tr.org.yesilay.yesiliz.ui.EkranDonusTakibi
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.buyukHarf
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane
import tr.org.yesilay.yesiliz.ui.tema.Beyaz

/**
 * Nefes egzersizi — kutu nefesi. `docs/tasarim-referans.md §2 / 16`,
 * `docs/tasarim/ekranlar/16_nefes.png`.
 *
 * Tam ekran, alt gezinme çubuğu yok (`YesilizGezinme.kt`'deki `sekmede`
 * kontrolü bu rotayı hariç tutuyor). Nudge overlay'in "Nefes egzersizi
 * yapmak ister misin? → Evet" derin bağlantısı da buraya düşüyor.
 */
@Composable
fun NefesEkran(
    onKapat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NefesViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val faz = NefesFazi.entries[durum.ilerleme.fazIndeksi]
    EkranAcikTut(aktif = durum.oynatiliyor && !durum.tamamlandi)

    // Arka plana düşen seans duraklıyor: ön planda geçmemiş süre
    // tamamlanmaya sayılmamalı (T4-010). Odak ekranıyla aynı desen —
    // Lifecycle yeni gözlemciyi mevcut RESUMED durumuna eşitlemek için
    // ilk ON_RESUME'u hemen gönderebilir, `EkranDonusTakibi` bunu gerçek
    // dönüşten ayırır (CLAUDE.md §9).
    val sahip = LocalLifecycleOwner.current
    DisposableEffect(sahip) {
        val donusTakibi = EkranDonusTakibi()
        val gozlemci = LifecycleEventObserver { _, olay ->
            when (olay) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                -> {
                    donusTakibi.ayrildi()
                    viewModel.arkaPlanaGitti()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (donusTakibi.dondu()) viewModel.onPlanaDondu()
                }

                else -> Unit
            }
        }
        sahip.lifecycle.addObserver(gozlemci)
        onDispose { sahip.lifecycle.removeObserver(gozlemci) }
    }

    // Nefes al/tut'ta daire büyür, nefes ver/tut'ta küçülür — süre tempoyla eşleniyor.
    val genisMi = faz == NefesFazi.NEFES_AL || faz == NefesFazi.TUT_1
    val olcek by animateFloatAsState(
        targetValue = if (genisMi) 1f else DaralmaOlcegi,
        animationSpec = tween(durationMillis = durum.fazSaniye * 1_000, easing = LinearEasing),
        label = "nefesOlcegi",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Yesil50)
            .navigationBarsPadding()
            .padding(horizontal = Olcu.EkranKenari),
    ) {
        Box(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(modifier = Modifier.height(Bosluk.S))

        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onKapat, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Kapat", tint = MetinBirincil)
            }
            Text(
                text = kalanMetni(durum.ilerleme.toplamKalanSaniye),
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Kutu nefesi".buyukHarf(),
                style = MaterialTheme.typography.labelMedium,
                color = MetinIkincil,
            )
            Text(
                text = "Sakinleşme seansı",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(Olcu.NefesDairesiDis)
                    .clip(CircleShape)
                    .background(YesilAna.copy(alpha = DisHalkaSaydamligi)),
            )
            Box(
                modifier = Modifier
                    .size(Olcu.NefesDairesiIc)
                    .scale(if (durum.tamamlandi) 1f else olcek)
                    .clip(CircleShape)
                    .background(Gradyan.NefesDairesi),
                contentAlignment = Alignment.Center,
            ) {
                if (durum.tamamlandi) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Beyaz,
                        modifier = Modifier.size(Olcu.AvatarBuyuk),
                    )
                } else {
                    Text(
                        text = faz.etiket,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Beyaz,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = if (durum.tamamlandi) {
                // Süre seçilebilir olduğu için sabit yazılamaz.
                "Seansı tamamladın. Kendine ${durum.seansSaniye / 60} dakika ayırdın."
            } else {
                "${durum.fazSaniye} saniye boyunca ${faz.yonerge}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // Puan yazılıp yazılmadığı söyleniyor. Günlük limit dolmuşsa
        // "+5 puan" demek yalan olurdu (`debug.md` Y-8).
        if (durum.tamamlandi) {
            Spacer(modifier = Modifier.height(Bosluk.S))
            Cip(
                metin = if (durum.puanYazildi) {
                    "+${Gorev.NEFES.puan} puan"
                } else {
                    "Bugünün nefes puanı zaten alınmış"
                },
                zemin = YesilNane,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (durum.tamamlandi) {
            // Seans bitince ne yapılacağı söyleniyor. Eskiden sayaç
            // 0:00'a ulaşıp öylece duruyordu: tebrik yok, puan bildirimi
            // yok, dönüş yok (`debug.md` Y-8).
            BirincilButon(metin = "Bitir", onTikla = onKapat)
            Spacer(modifier = Modifier.height(Bosluk.S))
            MetinBaglanti(metin = "Bir kez daha", onTikla = viewModel::bastanBaslat)
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            return@Column
        }

        FazGostergesi(fazIndeksi = durum.ilerleme.fazIndeksi)
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Olcu.BolumArasi),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tempo (kaç saniye nefes) ve SÜRE (seans kaç dakika) iki ayrı
            // kontrol. Tek kontrol varken kullanıcı onu süre sanıyordu
            // (`debug.md` İ-2).
            NefesKontrolu(
                olcu = Olcu.NefesKontrolKucuk,
                zemin = YesilNane,
                onTikla = viewModel::fazSuresiDegistir,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durum.fazSaniye}s",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = YesilKoyu,
                    )
                    Text(
                        text = "tempo",
                        style = MaterialTheme.typography.labelSmall,
                        color = YesilKoyu,
                    )
                }
            }
            NefesKontrolu(
                olcu = Olcu.NefesKontrolBuyuk,
                zemin = YesilAna,
                onTikla = viewModel::oynatDuraklat,
            ) {
                Icon(
                    imageVector = if (durum.oynatiliyor) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (durum.oynatiliyor) "Duraklat" else "Oynat",
                    tint = Beyaz,
                )
            }
            NefesKontrolu(
                olcu = Olcu.NefesKontrolKucuk,
                zemin = YesilNane,
                onTikla = viewModel::seansSuresiDegistir,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durum.seansSaniye / 60} dk",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = YesilKoyu,
                    )
                    Text(
                        text = "süre",
                        style = MaterialTheme.typography.labelSmall,
                        color = YesilKoyu,
                    )
                }
            }
        }

        // "Baştan başlat" artık ayrı bir satırda: alt sıradaki üç yuvarlak
        // tempo / oynat / süre oldu.
        MetinBaglanti(metin = "Baştan başlat", onTikla = viewModel::bastanBaslat)
        Spacer(modifier = Modifier.height(Olcu.Aralik))
    }
}

/** 4 fazlık ilerleme noktaları — `AdimGostergesi` tam genişlik çubuk olduğu için burada uymuyor. */
@Composable
private fun FazGostergesi(fazIndeksi: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(NefesFazi.entries.size) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = Bosluk.Xs)
                    .size(Olcu.NefesFazNoktasi)
                    .clip(CircleShape)
                    .background(if (i == fazIndeksi) YesilAna else Yesil200),
            )
        }
    }
}

/** Dairesel kontrol butonu — `IkonRozeti` tıklama desteklemediği için ayrı. */
@Composable
private fun NefesKontrolu(
    olcu: Dp,
    zemin: Color,
    onTikla: () -> Unit,
    icerik: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(olcu)
            .clip(CircleShape)
            .background(zemin)
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        icerik()
    }
}

private fun kalanMetni(toplamSaniye: Int): String {
    val dakika = toplamSaniye / 60
    val saniye = toplamSaniye % 60
    return "%d:%02d kaldı".format(dakika, saniye)
}

/** Nefes ver/tut fazlarında dairenin küçüldüğü ölçek. */
private const val DaralmaOlcegi = 0.75f

/** Dış halkanın saydamlığı — soluk, dekoratif. */
private const val DisHalkaSaydamligi = 0.15f
