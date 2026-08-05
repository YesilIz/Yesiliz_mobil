package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.AltSayfa
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.IlerlemeCubugu
import tr.org.yesilay.yesiliz.ui.bilesen.SecenekKarti
import tr.org.yesilay.yesiliz.ui.bilesen.YaprakZemini
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Ödüller — `docs/tasarim-referans.md §2 / 11` + alt sayfalar 17/18/19.
 *
 * Basamak listesi dikey bir yol: daireler arasındaki çizgi kazanılmış
 * bölümde dolu, kilitli bölümde soluk. Tıklanan ödül alt sayfada açılıyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdulEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OdulViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var acikOdul by remember { mutableStateOf<Odul?>(null) }
    var secimOdulu by remember { mutableStateOf<Odul?>(null) }
    var sertifikaOdulu by remember { mutableStateOf<Odul?>(null) }
    var gecmisAcik by remember { mutableStateOf(false) }
    val baglam = androidx.compose.ui.platform.LocalContext.current

    YesilBaslikliEkran(baslik = "Ödüller", onGeri = onGeri, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            YaprakZemini(modifier = Modifier.matchParentSize())
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(Olcu.Aralik))

                PuanOzeti(durum = durum, onGecmis = { gecmisAcik = true })

                Spacer(modifier = Modifier.height(Olcu.BolumArasi))

                durum.basamaklar.forEachIndexed { i, odul ->
                    OdulBasamagi(
                        odul = odul,
                        puan = durum.puan,
                        ilkMi = i == 0,
                        sonMu = i == durum.basamaklar.lastIndex,
                        onTikla = {
                            if (odul.hal == OdulHali.SECIM_BEKLIYOR) {
                                secimOdulu = odul
                            } else {
                                acikOdul = odul
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            }
        }
    }

    acikOdul?.let { odul ->
        AltSayfa(onKapat = { acikOdul = null }) {
            OdulDetayi(
                odul = odul,
                puan = durum.puan,
                onKapat = { acikOdul = null },
                onSertifika = {
                    acikOdul = null
                    sertifikaOdulu = odul
                },
            )
        }
    }

    secimOdulu?.let { odul ->
        AltSayfa(onKapat = { secimOdulu = null }) {
            OdulSecimAlani(
                odul = odul,
                onSec = { secenekId ->
                    viewModel.secYap(odul.id, secenekId)
                    secimOdulu = null
                },
            )
        }
    }

    sertifikaOdulu?.let { odul ->
        AltSayfa(onKapat = { sertifikaOdulu = null }) {
            SertifikaIcerik(
                odulAdi = odul.ad,
                kullaniciAdi = durum.isim,
                bagisTarihi = odul.bagisTarihi.orEmpty(),
                sertifikaNo = odul.sertifikaNo.orEmpty(),
                onPaylas = {
                    baglam.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Yeşilİz'de \"${odul.ad}\" ödülünü kazandım. " +
                                        "Belge no: ${odul.sertifikaNo.orEmpty()}",
                                )
                            },
                            "Sertifikanı paylaş",
                        ),
                    )
                },
                onKapat = { sertifikaOdulu = null },
            )
        }
    }

    if (gecmisAcik) {
        AltSayfa(onKapat = { gecmisAcik = false }) {
            PuanGecmisi(kayitlar = durum.gecmis, onKapat = { gecmisAcik = false })
        }
    }
}

@Composable
private fun PuanOzeti(durum: OdulDurum, onGecmis: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Gradyan.PuanKarti)
            .clickable(onClick = onGecmis)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        IkonRozeti(
            ikon = Icons.Filled.Park,
            zemin = Beyaz.copy(alpha = YariSaydam),
            renk = Beyaz,
        )
        Column {
            Text(
                text = "Mevcut Puanın",
                style = MaterialTheme.typography.bodyLarge,
                color = Beyaz.copy(alpha = AzSaydam),
            )
            Text(
                text = "${durum.puan.binlik()} puan",
                style = MaterialTheme.typography.displayLarge,
                color = Beyaz,
            )
            Text(
                text = "Sonraki ödüle ${durum.sonrakineKalan.binlik()} puan",
                style = MaterialTheme.typography.bodyMedium,
                color = Beyaz.copy(alpha = AzSaydam),
            )
        }
    }
}

/** Bir basamak: solda daire + bağlantı çizgisi, sağda metin. */
@Composable
private fun OdulBasamagi(
    odul: Odul,
    puan: Int,
    ilkMi: Boolean,
    sonMu: Boolean,
    onTikla: () -> Unit,
) {
    val kazanildi = odul.hal == OdulHali.KAZANILDI
    val kilitli = odul.hal == OdulHali.KILITLI
    val secimBekliyor = odul.hal == OdulHali.SECIM_BEKLIYOR
    val yogunluk = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTikla),
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // üst bağlantı — ilk basamakta yok
            Box(
                modifier = Modifier
                    .width(Olcu.KartKenarligi)
                    .height(if (ilkMi) Olcu.Sifir else Bosluk.M)
                    .background(if (kilitli) Ayrac else YesilAna),
            )

            when (odul.hal) {
                OdulHali.KAZANILDI -> IkonRozeti(
                    ikon = odul.ikon,
                    renk = YesilKoyu,
                    zemin = YesilNane,
                    olcu = Olcu.IkonRozeti,
                    sekil = CircleShape,
                )

                OdulHali.AKTIF, OdulHali.SECIM_BEKLIYOR -> Box(
                    modifier = Modifier
                        .size(Olcu.IkonRozeti)
                        .clip(CircleShape)
                        .background(YesilKoyu)
                        .border(Olcu.ButonKenarligi, YesilAna, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = odul.ikon, contentDescription = null, tint = Beyaz)
                }

                OdulHali.KILITLI -> Box(
                    modifier = Modifier.size(Olcu.IkonRozeti),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(Olcu.IkonRozeti)) {
                        val kalem = with(yogunluk) { Olcu.KartKenarligi.toPx() }
                        drawCircle(
                            color = Ayrac,
                            style = Stroke(
                                width = kalem * 2f,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(8f, 8f),
                                    0f,
                                ),
                            ),
                        )
                    }
                    Icon(
                        imageVector = odul.ikon,
                        contentDescription = null,
                        tint = MetinUcuncul,
                    )
                }
            }

            // alt bağlantı — son basamakta yok
            Box(
                modifier = Modifier
                    .width(Olcu.KartKenarligi)
                    .height(if (sonMu) Olcu.Sifir else Olcu.BasamakBoslugu)
                    .background(
                        if (kazanildi || odul.hal == OdulHali.AKTIF || secimBekliyor) {
                            YesilAna
                        } else {
                            Ayrac
                        },
                    ),
            )
        }

        Column(modifier = Modifier.padding(top = Olcu.Aralik)) {
            Text(
                text = "${odul.esik.binlik()} Puan",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (kilitli) MetinUcuncul else YesilAna,
            )
            Text(
                text = odul.ad,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (kilitli) MetinUcuncul else MetinBirincil,
            )
            when (odul.hal) {
                OdulHali.KAZANILDI -> TamamlandiSatiri()
                OdulHali.SECIM_BEKLIYOR -> SecimBekliyorSatiri()
                OdulHali.AKTIF -> {
                    Spacer(modifier = Modifier.height(Bosluk.S))
                    IlerlemeCubugu(oran = puan.toFloat() / odul.esik)
                    Spacer(modifier = Modifier.height(Bosluk.Xs))
                    Text(
                        text = "${(odul.esik - puan).coerceAtLeast(0).binlik()} puan kaldı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )
                }

                OdulHali.KILITLI -> Unit
            }
        }
    }
}

@Composable
private fun TamamlandiSatiri() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Bosluk.Xs),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = YesilAna,
            modifier = Modifier.size(Olcu.Aralik),
        )
        Text(
            text = "Tamamlandı",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = YesilAna,
        )
    }
}

/** Kademe kazanıldı ama birden çok seçenek var, kullanıcı henüz seçmedi. */
@Composable
private fun SecimBekliyorSatiri() {
    Text(
        text = "Ödülünü seç →",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = YesilAna,
    )
}

/**
 * Seçim alt sayfası — kademe kazanıldığında birden çok seçenek varsa açılır.
 *
 * Seçim tek yönlü: bir kart seçilip [BirincilButon]'a basılınca
 * [onSec] çağrılır ve alt sayfa kapanır. Geri dönüş yok (CLAUDE.md §4).
 */
@Composable
private fun OdulSecimAlani(odul: Odul, onSec: (String) -> Unit) {
    var secilenId by remember(odul.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        Text(
            text = "Ödülünü seç",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "${odul.esik.binlik()} puanı topladın. Hangi ödülü istersin?",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        odul.secenekler.forEach { secenek ->
            SecenekKarti(
                ikon = secenek.ikon,
                baslik = secenek.ad,
                aciklama = secenek.aciklama,
                secili = secilenId == secenek.id,
                onTikla = { secilenId = secenek.id },
                modifier = Modifier.padding(bottom = Bosluk.S),
            )
        }

        Spacer(modifier = Modifier.height(Bosluk.S))
        BirincilButon(
            metin = "Seçimi onayla",
            etkin = secilenId != null,
            onTikla = { secilenId?.let(onSec) },
        )
    }
}

/** Alt sayfa 18/19 — kilitli ve kazanılmış ödül aynı iskelet. */
@Composable
private fun OdulDetayi(
    odul: Odul,
    puan: Int,
    onKapat: () -> Unit,
    onSertifika: () -> Unit,
) {
    val kazanildi = odul.hal == OdulHali.KAZANILDI

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onKapat, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = MetinBirincil)
            }
        }

        IkonRozeti(
            ikon = odul.ikon,
            renk = Beyaz,
            zemin = YesilKoyu,
            fircaZemin = if (kazanildi) Gradyan.OdulRozeti else null,
            olcu = Olcu.AvatarBuyuk,
            sekil = CircleShape,
        )

        if (kazanildi) {
            Spacer(modifier = Modifier.height(Bosluk.S))
            Cip(metin = "Tamamlandı", ikon = Icons.Filled.Check, zemin = YesilNane, renk = YesilAna)
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = odul.ad,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = odul.aciklama,
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        if (kazanildi) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Yaricap.Buton))
                    .background(Yesil50)
                    .padding(Olcu.Aralik),
            ) {
                odul.bagisTarihi?.let { DetaySatiri("Bağış tarihi", it) }
                odul.sertifikaNo?.let {
                    Spacer(modifier = Modifier.height(Bosluk.S))
                    DetaySatiri("Sertifika no", it)
                }
            }
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            BirincilButon(metin = "Sertifikayı görüntüle", onTikla = onSertifika)
        } else {
            val oran = (puan.toFloat() / odul.esik).coerceIn(0f, 1f)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${puan.binlik()} / ${odul.esik.binlik()} puan",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "%${(oran * 100).toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )
            }
            Spacer(modifier = Modifier.height(Bosluk.S))
            IlerlemeCubugu(oran = oran)
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = "Kazanmana ${(odul.esik - puan).coerceAtLeast(0).binlik()} puan kaldı",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            BirincilButon(metin = "Puan kazanmaya devam et", onTikla = onKapat)
        }
    }
}

@Composable
private fun DetaySatiri(etiket: String, deger: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = deger,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )
    }
}

/** Alt sayfa 17 — puan geçmişi. Tüm değerler pozitif, ceza yok. */
@Composable
private fun PuanGecmisi(kayitlar: List<PuanKaydi>, onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Puan geçmişi",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onKapat) {
                Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = MetinBirincil)
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(YesilNane)
                .padding(Olcu.Aralik),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
        ) {
            Icon(Icons.Filled.Park, contentDescription = null, tint = YesilAna)
            Text(
                text = "Bu hafta toplam",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+${kayitlar.sumOf { it.puan }.binlik()} puan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = YesilAna,
            )
        }

        kayitlar.forEach { kayit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Olcu.Aralik),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
            ) {
                IkonRozeti(ikon = kayit.ikon, renk = YesilKoyu, zemin = Yesil50)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kayit.baslik,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = kayit.zaman,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                    )
                }
                Text(
                    text = "+${kayit.puan}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = YesilAna,
                )
            }
        }
    }
}

/** Gradyan kart üstündeki beyaz dolgunun opaklığı. */
private const val YariSaydam = 0.2f

/** Gradyan kart üstündeki ikincil metnin opaklığı. */
private const val AzSaydam = 0.85f
