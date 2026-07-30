package tr.org.yesilay.yesiliz.ui.ekran.ebeveyn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.bilesen.KodGir
import tr.org.yesilay.yesiliz.ui.bilesen.KodGoster
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.SecenekKarti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.CubukGrafik
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
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
 * Ebeveyn paneli — `docs/tasarim-referans.md §2 / 13, 20-22, 27`.
 *
 * Eşleşme yoksa 3 adımlı kurulum sihirbazı (20→21→22) `ModalBottomSheet`
 * içinde — Ödüller ekranındaki alt sayfa deseniyle aynı (ayrı NavHost rotası
 * yok). Eşleşme varsa gerçek panel + "YEDAM hakkında" ayrı bir sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbeveynEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EbeveynViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var yedamAcik by remember { mutableStateOf(false) }
    val sayfaDurumu = rememberModalBottomSheetState()

    YesilBaslikliEkran(baslik = "Ebeveyn paneli", onGeri = onGeri, modifier = modifier) {
        if (durum.eslesmisMi) {
            EbeveynPaneli(durum = durum, onYedam = { yedamAcik = true })
        }
    }

    if (!durum.eslesmisMi) {
        ModalBottomSheet(onDismissRequest = {}, sheetState = sayfaDurumu, containerColor = Beyaz) {
            when (durum.adim) {
                EslestirmeAdimi.ROL_SECIMI -> RolSecimiAdimi(
                    secilenRol = durum.secilenRol,
                    onRolSec = viewModel::rolSec,
                    onDevam = viewModel::rolOnayla,
                )

                EslestirmeAdimi.KOD -> KodAdimi(
                    uretilenKod = durum.uretilenKod,
                    hata = durum.eslestirmeHatasi,
                    onEslestir = viewModel::eslestir,
                )

                EslestirmeAdimi.BASARILI -> BasariliAdimi(
                    cocukAdi = durum.cocukAdi,
                    rol = durum.secilenRol,
                    onDevam = viewModel::sihirbaziKapat,
                )
            }
        }
    }

    if (yedamAcik) {
        ModalBottomSheet(
            onDismissRequest = { yedamAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            YedamHakkinda(onKapat = { yedamAcik = false })
        }
    }
}

/** Ekran 20 — cihaz rolü seçimi. */
@Composable
private fun RolSecimiAdimi(secilenRol: CihazRolu, onRolSec: (CihazRolu) -> Unit, onDevam: () -> Unit) {
    SihirbazIskeleti {
        IkonRozeti(ikon = Icons.Filled.Groups, zemin = YesilNane, renk = YesilKoyu, olcu = Olcu.AvatarBuyuk, sekil = CircleShape)
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "Ebeveyn paneline hoş geldin",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu paneli kullanmak için ebeveyn ve çocuk cihazlarını " +
                "eşleştirmen gerekiyor. Önce bu cihazın rolünü seç.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Yigin {
            CihazRolu.entries.forEach { rol ->
                SecenekKarti(
                    ikon = if (rol == CihazRolu.EBEVEYN) Icons.Filled.Groups else Icons.Filled.Person,
                    baslik = rol.baslik,
                    aciklama = rol.aciklama,
                    secili = secilenRol == rol,
                    onTikla = { onRolSec(rol) },
                )
            }
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Devam et", onTikla = onDevam)
    }
}

/** Ekran 21 — kod üretme + girme. MOCK: girilen kod doğrulanmıyor, 6 hane yeterli. */
@Composable
private fun KodAdimi(uretilenKod: String, hata: String?, onEslestir: (String) -> Unit) {
    var girilenKod by remember { mutableStateOf(TextFieldValue("")) }

    SihirbazIskeleti {
        Text(
            text = "Cihazları eşleştir",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu kodu çocuğunun cihazındaki Yeşilİz uygulamasına gir.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        KodGoster(kod = uretilenKod)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Kod 10 dakika geçerlidir",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinUcuncul,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "veya",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinUcuncul,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Çocuğunun cihazındaki kodu gir",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        KodGir(deger = girilenKod, onDegisti = { girilenKod = it })
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(YesilNane)
                .padding(Olcu.KartIci),
        ) {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = YesilKoyu)
            Text(
                text = "Kodlar yalnızca bu iki cihaz arasında paylaşılır ve " +
                    "başka kimseyle görülmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinBirincil,
                modifier = Modifier.padding(start = Bosluk.S),
            )
        }
        // Sunucudan gelen hata (kod yok, süresi dolmuş, aynı rol). Renk
        // yeşil — kırmızı yalnızca §12 K10'daki iki istisnada.
        hata?.let { mesaj ->
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = mesaj,
                style = MaterialTheme.typography.bodyMedium,
                color = tr.org.yesilay.yesiliz.ui.tema.Hata,
            )
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(
            metin = "Eşleştir",
            etkin = girilenKod.text.length == 6,
            onTikla = { onEslestir(girilenKod.text) },
        )
    }
}

/** Ekran 22 — eşleştirme başarılı. */
@Composable
private fun BasariliAdimi(cocukAdi: String, rol: CihazRolu, onDevam: () -> Unit) {
    SihirbazIskeleti {
        IkonRozeti(
            ikon = Icons.Filled.CheckCircle,
            renk = Beyaz,
            zemin = YesilAna,
            olcu = Olcu.AvatarBuyuk,
            sekil = CircleShape,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Cip(metin = "Eşleştirme başarılı", ikon = Icons.Filled.CheckCircle, zemin = YesilNane, renk = YesilAna)
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "$cocukAdi'in cihazıyla eşleştirildi",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Artık ekran süresini, hedeflerini ve ilerlemesini bu " +
                "panelden takip edebilirsin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(Yesil50)
                .padding(Olcu.Aralik),
        ) {
            SatirCift("Eşleşen cihaz", "$cocukAdi'in cihazı")
            Spacer(modifier = Modifier.height(Bosluk.S))
            SatirCift("Rol", if (rol == CihazRolu.EBEVEYN) "Çocuk cihazı" else "Ebeveyn cihazı")
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Ebeveyn paneline git", onTikla = onDevam)
    }
}

@Composable
private fun SatirCift(etiket: String, deger: String) {
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

@Composable
private fun SihirbazIskeleti(icerik: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = icerik,
    )
}

/** Ekran 13 — gerçek eşleşme sonrası panel. Ham veri değil özet (`docs/spec-veri.md`). */
@Composable
private fun EbeveynPaneli(durum: EbeveynDurum, onYedam: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Olcu.EkranKenari),
        ) {
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KullaniciAvatari(isim = durum.cocukAdi)
                Spacer(modifier = Modifier.width(Bosluk.M))
                Column {
                    Text(
                        text = durum.cocukAdi,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Cip(metin = "${durum.cocukYasi} yaşında")
                }
            }
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            Kart {
                Text(text = "ORTALAMA", style = MaterialTheme.typography.labelMedium, color = MetinIkincil)
                Text(
                    text = durum.ekranSuresiOrtalama,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(text = "ekran süresi", style = MaterialTheme.typography.bodyMedium, color = MetinIkincil)
                Text(text = durum.tarihAraligi, style = MaterialTheme.typography.bodyMedium, color = MetinUcuncul)
                Spacer(modifier = Modifier.height(Bosluk.M))
                CubukGrafik(cubuklar = durum.ekranCubuklari, vurgulanan = durum.ekranVurgu)
            }

            Spacer(modifier = Modifier.height(Olcu.Aralik))

            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozeti(ikon = Icons.Filled.Groups)
                    Spacer(modifier = Modifier.width(Bosluk.M))
                    Column {
                        Text(
                            text = "Durum: ${durum.bant}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MetinBirincil,
                        )
                        Text(text = durum.bantMesaji, style = MaterialTheme.typography.bodyMedium, color = MetinIkincil)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Olcu.Aralik))

            Kart {
                Text(text = "Bildirimler", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MetinBirincil)
                durum.bildirimler.forEachIndexed { i, bildirim ->
                    if (i > 0) KartAyraci()
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null, tint = MetinUcuncul)
                        Text(
                            text = bildirim.baslik,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MetinBirincil,
                            modifier = Modifier.weight(1f).padding(start = Bosluk.S),
                        )
                        Text(text = bildirim.zaman, style = MaterialTheme.typography.bodyMedium, color = MetinUcuncul)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Olcu.Aralik))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Yaricap.Buton))
                    .background(YesilNane)
                    .padding(Olcu.KartIci),
            ) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = YesilKoyu)
                Text(
                    text = "${durum.cocukAdi}'in gizliliğini korumak için yalnızca özet " +
                        "bilgiler paylaşılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinBirincil,
                    modifier = Modifier.padding(start = Bosluk.S),
                )
            }

            Spacer(modifier = Modifier.height(Olcu.Aralik))
            MetinBaglanti(metin = "YEDAM hakkında", onTikla = onYedam)
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        }
    }
}

/** Ekran 27 — bilgilendirme. "YEDAM onayı" (köprü/rıza akışı) İLE KARIŞTIRILMAZ. */
@Composable
private fun YedamHakkinda(onKapat: () -> Unit) {
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
        IkonRozeti(ikon = Icons.Filled.HeadsetMic, zemin = YesilKoyu, renk = Beyaz, olcu = Olcu.AvatarBuyuk, sekil = CircleShape)
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "YEDAM hakkında",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Yeşilay Danışmanlık Merkezi, madde ve teknoloji bağımlılığı " +
                "konusunda ücretsiz, gizli ve profesyonel destek sunar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(YesilNane)
                .padding(Olcu.KartIci),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Call, contentDescription = null, tint = YesilAna)
            Column(modifier = Modifier.padding(start = Bosluk.S)) {
                Text(
                    text = "115 — YEDAM Hattı",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                )
                Text(text = "7/24, ücretsiz ve gizli", style = MaterialTheme.typography.bodyMedium, color = MetinIkincil)
            }
        }
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "Görüşmeler tamamen gizlidir",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Uzman psikologlarca yürütülür",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        // Gerçek arama başlatma (ör. ACTION_DIAL) kapsam dışı — bilgilendirme yeterli.
        BirincilButon(metin = "115'i ara", onTikla = {})
    }
}
