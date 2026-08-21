package tr.org.yesilay.yesiliz.ui.ekran.yedam

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.domain.yedam.Yedam
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.bilesen.YedamAraButonu
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.ekran.hesap.HesapAlani
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * YEDAM köprüsü — açık rıza ve başvuru (M1, M4).
 *
 * Eskiden bu ekran yalnızca 115'i çeviricide açıyordu ve KDoc'u
 * "YEDAM'a veri ya da geri arama talebi gönderen bir entegrasyonu
 * yoktur" diyordu. `docs/panel-plani.md §0` bunu projenin en büyük yarım
 * kalan vaadi olarak işaretlemişti; köprünün YEDAM tarafı artık var.
 *
 * **Rıza metni birebir gösteriliyor.** `docs/yedam-riza-metni-v1.md §1`:
 * "Kısaltılamaz, özetlenemez; madde atlanırsa rıza o maddeyi kapsamaz."
 * Maddeler [PAYLASILACAKLAR] / [PAYLASILMAYACAKLAR] listelerinden geliyor.
 *
 * **115 her durumda açık.** Başvuru gönderilemese, sunucuya ulaşılamasa
 * ya da köprü kapalı olsa bile telefon butonu duruyor: destek yolu bir
 * sunucunun ayakta olmasına bağlı olamaz.
 */
@Composable
fun YedamOnayEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    onGiris: () -> Unit = {},
    gorunum: YedamViewModel = hiltViewModel(),
) {
    val durum by gorunum.durum.collectAsState()

    YesilBaslikliEkran(
        baslik = "YEDAM desteği",
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
                Basligi()

                when {
                    durum.yukleniyor -> Text(
                        text = "Yükleniyor…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )

                    durum.basvurusuVar -> BasvuruDurumu(
                        basvuru = durum.basvuru!!,
                        onTazele = gorunum::tazele,
                    )

                    else -> RizaFormu(durum, gorunum, onGiris)
                }

                Spacer(Modifier.height(Bosluk.S))
                Text(
                    text = Yedam.CALISMA_SAATLERI,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
                // Telefon her durumda: destek yolu sunucuya bağlı olamaz.
                YedamAraButonu()
                MetinBaglanti(
                    metin = "Şimdi değil",
                    onTikla = onGeri,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Basligi() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(DaireBoyu).clip(CircleShape).background(Yesil50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SupportAgent,
                contentDescription = null,
                tint = YesilKoyu,
                modifier = Modifier.size(Olcu.IkonRozeti),
            )
        }
    }
    Text(
        text = "YEDAM desteği istiyorum",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MetinBirincil,
    )
    Text(
        text = "YEDAM (Yeşilay Danışmanlık Merkezi) ücretsiz ve gizli " +
            "danışmanlık veriyor. Buradan destek istersen, bir danışman " +
            "seninle iletişime geçebilsin diye bazı bilgilerin paylaşılması " +
            "gerekiyor. Ne paylaşıldığını tek tek aşağıda yazdık.",
        style = MaterialTheme.typography.bodyLarge,
        color = MetinIkincil,
    )
}

@Composable
private fun RizaFormu(
    durum: YedamDurum,
    gorunum: YedamViewModel,
    onGiris: () -> Unit,
) {
    MaddeListesi("Paylaşılacak bilgiler", PAYLASILACAKLAR)
    MaddeListesi("Paylaşılmayacak bilgiler", PAYLASILMAYACAKLAR)

    BilgiSeridi(
        "Bunlar zaten hiçbir zaman toplanmıyor ve telefonundan çıkmıyor.",
    )

    Aciklama(
        "Kim görecek",
        "Yalnızca başvurunun atandığı YEDAM danışmanı. Danışmanlar " +
            "kullanıcı arayamaz; yalnızca kendilerine atanan başvuruyu " +
            "açabilirler.",
    )
    Aciklama(
        "Ne kadar süre",
        "90 gün. Süre dolduğunda risk ve anket bilgilerin sunucudan " +
            "silinir, danışmanın erişimi kapanır.",
    )
    Aciklama(
        "Her bakış sana bildirilir",
        "Danışman bilgilerine her baktığında burada bir kayıt görürsün. " +
            "Profil › Gizlilik'ten her zaman bakabilirsin.",
    )
    Aciklama(
        "İstediğin an geri alabilirsin",
        "Profil › Gizlilik › \"YEDAM rızamı geri çek\". Geri çektiğin anda " +
            "danışmanın erişimi kesilir ve risk ile anket bilgilerin " +
            "sunucudan silinir.",
    )
    Aciklama(
        "Bu bir teşhis değil",
        "Yeşilİz bir sağlık hizmeti değildir, tanı koymaz. Danışman kendi " +
            "değerlendirmesini yapar.",
    )

    Text(
        text = "Sana nasıl ulaşalım?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MetinBirincil,
    )
    Row {
        IletisimTuru.entries.forEach { tur ->
            // `Cip`'in seçili hâli yok; seçim zemin/metin rengiyle
            // gösteriliyor — ayrı bir bileşen açmaya değmez.
            val seciliMi = durum.iletisimTuru == tur
            Cip(
                metin = tur.etiket,
                zemin = if (seciliMi) YesilAna else Yesil50,
                renk = if (seciliMi) Beyaz else MetinBirincil,
                onTikla = { gorunum.iletisimTuruDegisti(tur) },
            )
            Spacer(Modifier.width(Bosluk.S))
        }
    }
    HesapAlani(
        etiket = if (durum.iletisimTuru == IletisimTuru.TELEFON) "Telefon" else "E-posta",
        deger = durum.iletisimDegeri,
        onDegis = gorunum::iletisimDegeriDegisti,
        klavyeTuru = if (durum.iletisimTuru == IletisimTuru.TELEFON) {
            KeyboardType.Phone
        } else {
            KeyboardType.Email
        },
    )
    HesapAlani(
        etiket = "Eklemek istediğin bir şey var mı? (isteğe bağlı)",
        deger = durum.not,
        onDegis = gorunum::notDegisti,
    )

    OnayKutusu(
        metin = "Yukarıdakileri okudum, YEDAM danışmanının bu bilgileri " +
            "görmesine izin veriyorum.",
        secili = durum.onayVerildi,
        onDegis = gorunum::onayDegisti,
    )

    if (durum.oturumYok) {
        UyariSeridi("YEDAM başvurusu için hesabınla giriş yapman gerekiyor.")
        IkincilButon(metin = "Giriş yap", onTikla = onGiris)
    }
    durum.hata?.let { UyariSeridi(it) }
    if (durum.cevrimdisi) {
        UyariSeridi(
            "Sunucuya ulaşılamadı, başvurun gönderilemedi. Bağlantını " +
                "kontrol edip tekrar deneyebilir ya da ${Yedam.NUMARA}'i " +
                "doğrudan arayabilirsin.",
        )
    }

    BirincilButon(
        metin = "Başvuruyu gönder",
        onTikla = gorunum::gonder,
        etkin = durum.gonderilebilir,
        islemde = durum.gonderiliyor,
    )
}

@Composable
private fun BasvuruDurumu(basvuru: BasvuruGorunumu, onTazele: () -> Unit) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = YesilAna,
            )
            Spacer(Modifier.width(Bosluk.S))
            Text(
                text = "Başvurun kayıtlı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
            )
        }
        Spacer(Modifier.height(Bosluk.S))
        Text(
            text = basvuru.aciklama,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }

    if (basvuru.temaslar.isNotEmpty()) {
        Text(
            text = "Görüşme kayıtları",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )
        basvuru.temaslar.forEach { temas ->
            Text(
                text = "${temas.etiket} · ${temas.zaman.take(10)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }
    }

    basvuru.rizaBitisi?.let {
        Text(
            text = "Rızan ${it.take(10)} tarihine kadar geçerli.",
            style = MaterialTheme.typography.bodySmall,
            color = MetinIkincil,
        )
    }

    IkincilButon(metin = "Durumu yenile", onTikla = onTazele)
}

@Composable
private fun MaddeListesi(baslik: String, maddeler: List<String>) {
    Text(
        text = baslik,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MetinBirincil,
    )
    maddeler.forEach { madde ->
        Text(
            text = "•  $madde",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}

@Composable
private fun Aciklama(baslik: String, metin: String) {
    Text(
        text = baslik,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MetinBirincil,
    )
    Text(text = metin, style = MaterialTheme.typography.bodyMedium, color = MetinIkincil)
}

@Composable
private fun BilgiSeridi(metin: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(YesilNane)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = YesilAna)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

/** Hata şeridi — kırmızı **yok** (§4); Yeşilİz'de hata durumları da yeşil. */
@Composable
private fun UyariSeridi(metin: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(Yesil50)
            .padding(Olcu.KartIci),
    ) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = YesilKoyu,
        )
    }
}

private val DaireBoyu = Olcu.AvatarBuyuk
