package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.AltSayfa
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.YuzeyKirik
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Lig — `docs/tasarim-referans.md §2 / 10`.
 *
 * Ligde **düşme cezası puan silmiyor**; yalnızca bir alt gruba geçiliyor.
 * Kazanılan puan hiçbir koşulda geri alınmaz (CLAUDE.md §4).
 */
@Composable
fun LigEkran(
    onArkadasEkle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LigViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var kuralAcik by remember { mutableStateOf(false) }

    // İçerik zemini beyaz: sıralama satırları `Yesil50` dolgulu, zemin de
    // `Yesil50` olsaydı satırlar kaybolurdu.
    YesilBaslikliEkran(baslik = "Lig", icerikZemini = Beyaz, modifier = modifier) {
        // Boş durum (tasarım 37): kullanıcı ne lige katılmış ne de arkadaşı
        // var. Sahte bir sıralama göstermektense ne yapabileceğini söylemek
        // doğru — aynı ilke İstatistik'in boş durumunda da geçerli.
        if (!durum.yukleniyor && !durum.katildiMi) {
            BosLig(onKatil = viewModel::ligeKatil, onArkadasEkle = onArkadasEkle)
            return@YesilBaslikliEkran
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Bosluk.S),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = Olcu.BolumArasi,
            ),
        ) {
            item { LigBasligi(durum, onKuralAcikla = { kuralAcik = true }) }
            // Sunucuya ulaşılamadığında sıralama yerel yedekten geliyor.
            // Bunu söylemeden göstermek, demo günü "backend çalışıyor"
            // sanılmasına yol açardı (`debug.md` O-14).
            if (durum.cevrimdisi) {
                item { CevrimdisiNotu(ornekVeriVarMi = durum.siralama.isNotEmpty()) }
            }
            // "Ligde görün" kapalıysa söyleniyor. Kullanıcı kendi satırını
            // görmeye devam ediyor ama başkaları görmüyor; bunu bilmeden
            // sıralamanın neden bu kadar kısa olduğunu anlayamazdı.
            if (!durum.gorunuyorum) {
                item { GorunmuyorNotu() }
            }
            items(durum.siralama, key = { it.sira }) { satir ->
                LigSatiri(satir)
            }
            item {
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                IkincilButon(metin = "Arkadaş davet et", onTikla = onArkadasEkle)
            }
        }
    }

    if (kuralAcik) {
        AltSayfa(onKapat = { kuralAcik = false }) {
            LigKurallari(durum = durum, onKapat = { kuralAcik = false })
        }
    }
}

/**
 * Lig kuralları — yükselme/düşme çiplerine dokununca açılıyor.
 *
 * `docs/spec-puan.md §8` ile birebir. Ceza yolu olmadığı ayrıca yazılı:
 * düşmek puan kaybettirmiyor.
 */
@Composable
private fun LigKurallari(durum: LigDurum, onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        Text(
            text = "Lig nasıl işliyor?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        listOf(
            "Her hafta pazartesi yeni bir tur başlıyor; sıralama haftalık " +
                "puanına göre kuruluyor.",
            "Hafta sonunda ilk ${durum.yukselen} kişi bir üst lige geçiyor, " +
                "son ${durum.dusen} kişi bir alt lige iniyor.",
            "Ligler: Fidan → Filiz → Ağaç → Koru → Orman.",
            "Düşmek puan kaybettirmiyor. Kazandığın hiçbir puan geri " +
                "alınmıyor; yalnızca haftaya hangi grupta yarışacağın " +
                "değişiyor.",
            "Ligde görünmek istemiyorsan Profil › Gizlilik'ten " +
                "kapatabilirsin; kendi sıranı görmeye devam edersin.",
        ).forEach { madde ->
            Text(
                text = "• $madde",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                modifier = Modifier.padding(bottom = Bosluk.S),
            )
        }
        Spacer(modifier = Modifier.height(Bosluk.S))
        BirincilButon(metin = "Anladım", onTikla = onKapat)
    }
}

/**
 * Sunucuya ulaşılamadığında gösterilen not.
 *
 * Metin iki hâlli, çünkü altındaki liste iki türlü doluyor: debug
 * derlemesinde yerel yedek örnek sıralama veriyor, release'de sahte veri
 * hiç örneklenmediği için liste **boş** kalıyor (`di/DepoBaglama.kt`).
 * "Aşağıdaki sıralama örnek veri" cümlesini boş listenin üstünde
 * göstermek okuyanı yanıltırdı.
 *
 * @param ornekVeriVarMi altında gösterilecek satır var mı.
 */
@Composable
private fun CevrimdisiNotu(ornekVeriVarMi: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(YuzeyKirik)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MetinUcuncul,
        )
        Spacer(modifier = Modifier.width(Bosluk.S))
        Text(
            text = if (ornekVeriVarMi) {
                "Sunucuya ulaşılamıyor. Aşağıdaki sıralama örnek veri; " +
                    "bağlantı kurulunca gerçek lig görünecek."
            } else {
                "Sunucuya ulaşılamıyor. Bağlantı kurulunca lig sıralaman " +
                    "burada görünecek."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}

@Composable
private fun GorunmuyorNotu() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(YesilNane)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = YesilKoyu,
        )
        Spacer(modifier = Modifier.width(Bosluk.S))
        Text(
            text = "Ligde görünmüyorsun. Kendi sıranı görüyorsun ama " +
                "başkaları seni görmüyor. Profil › Gizlilik'ten " +
                "değiştirebilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

/** Lig — boş durum (tasarım 37). */
@Composable
private fun BosLig(onKatil: () -> Unit, onArkadasEkle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        Image(
            painter = painterResource(tr.org.yesilay.yesiliz.R.drawable.agac_evre1_cicek),
            contentDescription = null,
            modifier = Modifier.height(Olcu.AgacBoyu),
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Henüz kimseyle yarışmıyorsun",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bir lige katıl ya da arkadaşlarını davet et, birlikte " +
                "ilerlemenizi görün.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinUcuncul,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        BirincilButon(metin = "Lige katıl", onTikla = onKatil)
        Spacer(modifier = Modifier.height(Bosluk.S))
        IkincilButon(metin = "Arkadaş davet et", onTikla = onArkadasEkle)
    }
}

@Composable
private fun LigBasligi(durum: LigDurum, onKuralAcikla: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Olcu.KartIci),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Olcu.AvatarBuyuk)
                .clip(CircleShape)
                .background(Beyaz)
                .border(Olcu.ButonKenarligi, YesilAna, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Park,
                contentDescription = null,
                tint = YesilAna,
                modifier = Modifier.size(Olcu.IkonRozeti),
            )
        }
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = durum.ligAdi,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "${durum.kalanGun} gün kaldı",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        // İki çip hap biçimli buton gibi duruyor -- biri vurgulu biri soluk
        // -- ve filtre sanılıyordu; hiçbiri tıklanmıyordu (`debug.md` O-16).
        // Aslında bilgi etiketi olduklarına göre yapacakları en doğru şey
        // ne anlama geldiklerini anlatmak.
        Row(horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
            Cip(
                metin = "İlk ${durum.yukselen} yükselir",
                ikon = Icons.Filled.ArrowUpward,
                zemin = YesilNane,
                onTikla = onKuralAcikla,
            )
            Cip(
                metin = "Son ${durum.dusen} düşer",
                ikon = Icons.Filled.ArrowDownward,
                zemin = Yesil50,
                renk = MetinUcuncul,
                onTikla = onKuralAcikla,
            )
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
    }
}

@Composable
private fun LigSatiri(satir: LigSatiriModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(if (satir.benMiyim) YesilNane else Yesil50)
            .then(
                if (satir.benMiyim) {
                    Modifier.border(
                        Olcu.ButonKenarligi,
                        YesilAna,
                        RoundedCornerShape(Yaricap.Buton),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Olcu.Aralik, vertical = Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        Text(
            text = "${satir.sira}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(SiraGenisligi),
        )
        KullaniciAvatari(isim = satir.isim)
        Text(
            text = satir.isim,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (satir.benMiyim) FontWeight.Bold else FontWeight.Medium,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = satir.puan.binlik(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
    }
}

private val SiraGenisligi = Bosluk.L
