package tr.org.yesilay.yesiliz.ui.ekran.kategori

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.RiskGirisKarti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Dilim
import tr.org.yesilay.yesiliz.ui.bilesen.veri.YiginBar
import tr.org.yesilay.yesiliz.ui.buyukHarf
import tr.org.yesilay.yesiliz.ui.sureMetni
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.adi
import tr.org.yesilay.yesiliz.ui.tema.avatarRengi
import tr.org.yesilay.yesiliz.ui.tema.grafikRengi

/** Uygulama kategorileri — `docs/tasarim-referans.md §2 / 31`. */
@Composable
fun KategoriEkran(
    onGeri: () -> Unit,
    onRiskDetay: () -> Unit,
    onKategorizeEt: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KategoriViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "Uygulama kategorileri",
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
                RiskGirisKarti(bant = durum.riskBandi, onTikla = onRiskDetay)

                Text(
                    text = "Yüklü uygulamaların otomatik olarak kategorilere ayrılır; " +
                        "her kategoride bugün ne kadar vakit geçirdiğini gör.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                if (!durum.yukleniyor && durum.gruplar.isEmpty()) {
                    BosDurum(
                        baslik = "Bugün henüz kullanım yok",
                        aciklama = "Gün ilerledikçe uygulamaların kategorilere " +
                            "ayrılmış hâlini burada göreceksin.",
                    )
                } else {
                    durum.gruplar.forEach { grup ->
                        GrupSatiri(
                            grup = grup,
                            onAcKapat = { viewModel.grubuAcKapat(grup.kategori) },
                        )
                    }

                    if (durum.toplamDk > 0) {
                        Spacer(modifier = Modifier.height(Bosluk.S))
                        YiginBar(
                            dilimler = durum.gruplar.map { grup ->
                                Dilim(
                                    etiket = grup.kategori.adi,
                                    oran = grup.toplamDk.toFloat() / durum.toplamDk,
                                    renk = grup.kategori.grafikRengi,
                                )
                            },
                        )
                        Text(
                            text = "Bugünkü ekran süresinin %${durum.eglenceOrani}'i " +
                                "sosyal medya ve eğlencede geçti",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinIkincil,
                        )
                    }
                }

                // Kategori dağılımını gönderen bir API yok. Eski düğme
                // yalnızca yerel bir “gönderildi” bildirimi yazıp sahte başarı
                // gösteriyordu; gerçek ebeveyn sınırı açıkça anlatılıyor.
                if (durum.ebeveynAktifMi) {
                    Text(
                        text = EBEVEYN_KATEGORI_ACIKLAMASI,
                        style = MaterialTheme.typography.labelMedium,
                        color = MetinUcuncul,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                MetinBaglanti(
                    metin = "Kategorize et",
                    onTikla = onKategorizeEt,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Kategori ekranının gerçek ebeveyn veri sınırı. */
internal const val EBEVEYN_KATEGORI_ACIKLAMASI =
    "Bu ekrandaki kategori dağılımı ve uygulama adları ebeveyn paneline " +
        "gönderilmez. Eşleşmiş ebeveyn, daha önce sunucuyla paylaşılmış günlük " +
        "toplamları ve yalnızca yeterli veriye dayanan risk durumunu görebilir."

/** Akordiyon: kategori başlığı + açıkken uygulama satırları. */
@Composable
private fun GrupSatiri(grup: KategoriGrubu, onAcKapat: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAcKapat)
                .padding(vertical = Bosluk.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (grup.acik) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = if (grup.acik) "Kapat" else "Aç",
                tint = MetinUcuncul,
            )
            Spacer(modifier = Modifier.width(Bosluk.S))
            Text(
                text = grup.kategori.adi.buyukHarf(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MetinIkincil,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = grup.toplamDk.sureMetni(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
        }

        if (grup.acik) {
            Column(verticalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                grup.uygulamalar.forEach { UygulamaKarti(it) }
            }
        }
    }
}

@Composable
private fun UygulamaKarti(satir: UygulamaSatiri, onTikla: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .border(
                width = Olcu.KartKenarligi,
                color = Ayrac,
                shape = RoundedCornerShape(Yaricap.Buton),
            )
            .then(if (onTikla != null) Modifier.clickable(onClick = onTikla) else Modifier)
            .padding(Bosluk.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UygulamaAvatari(satir.ad)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = satir.ad,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = satir.dakika.sureMetni(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Text(
                text = "bugün",
                style = MaterialTheme.typography.labelMedium,
                color = MetinUcuncul,
            )
        }
    }
}

/**
 * Uygulama avatarı — baş harfler.
 *
 * Gerçek uygulama ikonu çizilmiyor: `Drawable`'ı Compose'a taşımak,
 * her satırda `PackageManager` sorgusu ve marka ikonlarının kendi
 * renkleriyle paletle çatışması demek. Tasarım da baş harf rozeti
 * kullanıyor (31, 32).
 */
@Composable
internal fun UygulamaAvatari(ad: String, olcu: androidx.compose.ui.unit.Dp = Olcu.IkonRozeti) {
    val renk = avatarRengi(ad)
    Box(
        modifier = Modifier
            .size(olcu)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(renk.copy(alpha = ZeminSaydamligi)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // `replaceFirstChar { it.uppercase() }` Locale.ROOT kullanıyordu:
            // "instagram" → "In" değil "IN" değil, Türkçe "İn" gerekiyordu.
            // İkinci harf değişmiyor, yalnızca ilki `buyukHarf()` ile
            // Türkçe-doğru büyütülüyor (`debug.md` üçüncü tarama A2).
            text = ad.take(1).buyukHarf() + ad.drop(1).take(1),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = renk,
        )
    }
}

private const val ZeminSaydamligi = 0.18f
