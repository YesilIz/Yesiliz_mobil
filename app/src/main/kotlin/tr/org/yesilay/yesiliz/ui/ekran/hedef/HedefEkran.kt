package tr.org.yesilay.yesiliz.ui.ekran.hedef

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.nudge.NudgeAyari
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.sureMetni
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.SelfImprovement
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.AnahtarSatiri
import tr.org.yesilay.yesiliz.nudge.NudgeIzni

/** Günlük hedef — `docs/tasarim-referans.md §2 / 34`. */
@Composable
fun HedefEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HedefViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "Günlük hedef",
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DaireselGosterge(oran = durum.oran) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = durum.secilenDk.sureMetni(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "günlük ekran hedefi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinIkincil,
                            )
                        }
                    }
                }

                // Bugünün kullanımı yalnızca veri varsa — izin verilmemiş ya
                // da ilk gün olan kullanıcıya "%0 kullandın" demek yanlış.
                durum.bugunDk?.let { bugun ->
                    Text(
                        text = "Bugün ${bugun.sureMetni()} kullandın · %${durum.yuzde}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AyarSeridi(
                    deger = durum.secilenDk,
                    enAz = HedefSinir.EN_AZ,
                    enCok = HedefSinir.EN_COK,
                    onAzalt = viewModel::azalt,
                    onArttir = viewModel::arttir,
                )

                BolumBasligi("Hızlı seçim")
                Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                    HedefSinir.hizliSecim(durum.kayitliHedefDk).forEach { dakika ->
                        HizliCip(
                            dakika = dakika,
                            secili = dakika == durum.secilenDk,
                            // Listeye kayıtlı hedef de ekleniyor (HedefSinir);
                            // hiçbir şey değiştirilmemişken o çip "seçili"
                            // görünüyor ama KAYITLI değer olduğu anlaşılmıyordu.
                            kayitliMi = dakika == durum.kayitliHedefDk,
                            onTikla = { viewModel.ayarla(dakika) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Şerit yalnızca GÜNLÜK HEDEF için doğru. Mola eşiği anında
                // geçerli oluyor (§12 K53: servis eşiği her poll turunda
                // okuyor), yani şerit mola bölümünün altındayken yapılan
                // işi yapılmamış gösteriyordu (5. tarama G5-029).
                BilgiSeridi(ebeveynAktifMi = durum.ebeveynAktifMi)

                MolaEsigiBolumu(durum = durum, viewModel = viewModel)

                BirincilButon(
                    metin = if (durum.kaydedildi) "Kaydedildi" else "Kaydet",
                    onTikla = viewModel::kaydet,
                    etkin = durum.degistiMi,
                )
            }
        }
    }
}

/**
 * Kesintisiz ekran süresi eşiği — nudge bu süre dolunca çıkıyor.
 *
 * Aynı ekranda duruyor çünkü kullanıcı için tek bir soru: "günde ne kadar,
 * bir oturuşta ne kadar". Sınırlar [NudgeAyari]'de ve seçim onlarla kırpılıyor
 * — 12 saatlik bir eşik hatırlatıcıyı hiç tetiklenmez hâle getirirdi.
 */
@Composable
private fun MolaEsigiBolumu(durum: HedefDurum, viewModel: HedefViewModel) {
    val baglam = LocalContext.current

    BolumBasligi("Mola hatırlatıcı")

    // Anahtar Profil › Gizlilik'ten buraya taşındı: açma düğmesi ile eşiği
    // iki ayrı ekranda tutmak, tek bir özelliği ikiye bölüyordu. Ayrıca bu
    // bir gizlilik ayarı değil — cihazdan hiçbir veri çıkarmıyor, yanındaki
    // iki satır (Yeşilay paylaşımı, ligde görün) ise gerçek rıza kapısıydı
    // ve aynı başlık altında durmak ikisinin ağırlığını bulandırıyordu
    // (5. tarama G5-034).
    Kart(icBosluk = Olcu.Aralik) {
        AnahtarSatiri(
            ikon = Icons.Filled.SelfImprovement,
            etiket = "Mola hatırlatıcı",
            acik = durum.molaHatirlaticiAcik,
            onDegis = { acik ->
                viewModel.molaHatirlaticiAyarla(acik)
                // Overlay izni olmadan popup çizilemiyor; anahtarı açan
                // kullanıcıyı sistem ekranına bir kez yönlendiriyoruz.
                if (acik && !NudgeIzni.verildiMi(baglam) && NudgeIzni.ayarEkraniVarMi(baglam)) {
                    baglam.startActivity(NudgeIzni.ayarIntent(baglam))
                }
            },
        )
    }

    Text(
        text = "Kesintisiz ${durum.secilenNudgeDk.sureMetni()} ekranda kalınca " +
            "kısa bir mola önerisi çıkar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MetinIkincil,
    )

    AyarSeridi(
        deger = durum.secilenNudgeDk,
        enAz = NudgeAyari.EN_AZ_DK,
        enCok = NudgeAyari.EN_COK_DK,
        onAzalt = viewModel::nudgeAzalt,
        onArttir = viewModel::nudgeArttir,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
        NudgeAyari.HIZLI_SECIM.forEach { dakika ->
            HizliCip(
                dakika = dakika,
                secili = dakika == durum.secilenNudgeDk,
                onTikla = { viewModel.nudgeAyarla(dakika) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Varsayılana dönmek yalnızca kayıtlı bir seçim varken anlamlı.
    if (durum.nudgeSecilmisMi) {
        Text(
            text = "Varsayılana dön",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = YesilAna,
            modifier = Modifier.clickable(onClick = viewModel::nudgeVarsayilanaDon),
        )
    }

    // Hatırlatıcı kapalıyken eşiğin bir etkisi yok; bunu söylememek,
    // çalışmayan bir ayarı çalışıyormuş gibi göstermek olurdu (§12 K37).
    // Anahtar artık hemen yukarıda, o yüzden yönlendirme metni gerekmiyor.
    if (!durum.molaHatirlaticiAcik) {
        Text(
            text = "Hatırlatıcı kapalıyken bu eşik uygulanmıyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MetinUcuncul,
        )
    }
}

/**
 * `−` … `+` şeridi — tasarım 34.
 *
 * **Değeri yazmıyor.** Ekran aynı sayıyı üç kez gösteriyordu (halka "4s 45dk",
 * şerit "4 saat 45 dakika", seçili çip "4s 45dk") ve ikisi farklı yazım
 * kuralındaydı. Değeri günlük hedefte halka, mola bölümünde üstteki cümle
 * söylüyor; şerit yalnızca kontrol (5. tarama G5-028).
 */
@Composable
private fun AyarSeridi(
    deger: Int,
    enAz: Int,
    enCok: Int,
    onAzalt: () -> Unit,
    onArttir: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        // Şerit değeri yazmayı bırakınca tam genişlikte kalması ortada geniş
        // bir boşluk bırakıyordu — "kontrol" değil "eksik bir şey" gibi
        // duruyordu. Bant artık iki düğmeyi sarıyor.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Yaricap.Kart))
                .background(Yesil50)
                .padding(Bosluk.S),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Olcu.BolumArasi),
        ) {
            AyarDugmesi(
                ikon = Icons.Filled.Remove,
                aciklama = "Azalt",
                zemin = Beyaz,
                renk = MetinBirincil,
                etkin = deger > enAz,
                onTikla = onAzalt,
            )
            AyarDugmesi(
                ikon = Icons.Filled.Add,
                aciklama = "Arttır",
                zemin = YesilAna,
                renk = Beyaz,
                etkin = deger < enCok,
                onTikla = onArttir,
            )
        }
    }
}

@Composable
private fun AyarDugmesi(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    aciklama: String,
    zemin: androidx.compose.ui.graphics.Color,
    renk: androidx.compose.ui.graphics.Color,
    etkin: Boolean,
    onTikla: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Olcu.IkonRozeti)
            .clip(CircleShape)
            .background(if (etkin) zemin else Ayrac)
            .clickable(enabled = etkin, onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ikon,
            contentDescription = aciklama,
            tint = if (etkin) renk else MetinUcuncul,
        )
    }
}

@Composable
private fun HizliCip(
    dakika: Int,
    secili: Boolean,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    kayitliMi: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(Olcu.LikertDairesi)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(if (secili) Yesil50 else Beyaz)
            .border(
                width = Olcu.KartKenarligi,
                color = if (secili) YesilAna else Ayrac,
                shape = RoundedCornerShape(Yaricap.Cip),
            )
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dakika.sureMetni(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (secili) FontWeight.SemiBold else FontWeight.Normal,
                color = if (secili) YesilAna else MetinIkincil,
            )
            if (kayitliMi) {
                Text(
                    text = "kayıtlı",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                )
            }
        }
    }
}

/** Bilgi şeridi. */
@Composable
private fun BilgiSeridi(ebeveynAktifMi: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(Yesil50)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = YesilAna,
        )
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = hedefBilgiMetni(ebeveynAktifMi),
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

/**
 * Sunucu hedef aşım bildirimi göndermiyor. Eşleşmiş ebeveyn yalnızca kullanıcı
 * paylaşımı açtıysa oluşan haftalık özeti panelden okuyabiliyor; şerit gerçek
 * ürün davranışından daha güçlü bir gözetim güvencesi vermemeli.
 */
internal fun hedefBilgiMetni(ebeveynAktifMi: Boolean): String =
    if (ebeveynAktifMi) {
        "Paylaşım açıksa, eşleşmiş ebeveyn haftalık özetini panelden görebilir. " +
            "Değişiklikler yarından itibaren geçerli olur."
    } else {
        "Değişiklikler yarından itibaren geçerli olur."
    }
