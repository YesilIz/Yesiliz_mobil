package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesmeler
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * 18 yaş altı — ebeveyn onayı beyanı.
 *
 * ## Niçin var
 *
 * CLAUDE.md §4: "18 yaş altı: ebeveyn onayı zorunlu, KVKK ayrı rejim."
 * Onboarding 3/4'te yaş 15 girildiğinde bile "Devam" doğrudan ankete
 * geçiyor, onay sorulmuyor, uyarı çıkmıyor ve akış hiçbir yerde
 * ayrışmıyordu. Üstelik varsayılan yaş **17**'ydi — yani varsayılan
 * kullanıcı zaten 18 altıydı (`debug.md` Y-11).
 *
 * ## Niçin bu kadarı
 *
 * Beyan usulü onay, doğrulanabilir bir onay değil; bunu bilerek ve
 * yazarak yapıyoruz. Gerçek doğrulama (ebeveyn e-postasına bağlantı,
 * kimlik eşleştirme) ebeveyn panelinin kapsamına giriyor ve §12 A5 hâlâ
 * açık bir karar. Beyanın kendisi yine de anlamlı: kullanıcıya rejimin
 * varlığını bildiriyor ve devam etmeyi bilinçli bir adım hâline
 * getiriyor.
 *
 * **Özellik kısıtı yok** (kullanıcı kararı): 18 altı kullanıcı
 * uygulamanın tamamını kullanabiliyor. Veri toplama kapsamı zaten
 * herkes için aynı ve dar.
 */
@Composable
fun EbeveynOnayiEkran(
    yas: Int,
    onayli: Boolean,
    onOnayDegisti: (Boolean) -> Unit,
    onDevam: () -> Unit,
    onGeri: () -> Unit,
    onSozlesme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        // Yaş adımıyla aynı numara: bu ekran akışa YENİ bir adım eklemiyor,
        // 18 altı kullanıcı için o adımın devamı. Gösterge 4'ten 5'e
        // çıksaydı yetişkin ve çocuk akışlarının uzunluğu farklı görünürdü.
        adim = 3,
        ilerleMetni = "Onaylıyorum, devam et",
        onIlerle = onDevam,
        ilerleEtkin = onayli,
        ikincilMetin = "Yaşımı düzeltmek istiyorum",
        onIkincil = onGeri,
        modifier = modifier,
    ) {
        IkonRozeti(
            ikon = Icons.Filled.FamilyRestroom,
            zemin = YesilNane,
            renk = YesilKoyu,
            olcu = Olcu.AvatarBuyuk,
            sekil = CircleShape,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Ebeveyn onayı gerekiyor",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "$yas yaşındasın. 18 yaşından küçük kullanıcıların " +
                "verilerinin işlenmesi için KVKK gereği ebeveyn ya da " +
                "yasal vasi onayı aranıyor.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Kart))
                .background(YesilNane)
                .padding(Olcu.KartIci),
        ) {
            Text(
                text = "Ebeveynine göstermen gerekenler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = "Uygulama telefon kullanım sürelerini ölçüyor; " +
                    "mesajlarını, fotoğraflarını, rehberini ve konumunu " +
                    "okumuyor. Ölçüm telefonunda kalıyor ve sen izin " +
                    "vermedikçe hiçbir yere gönderilmiyor. Ayrıntılar " +
                    "Gizlilik Politikası'nda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        OnayKutusu(
            metin = "Ebeveynim ya da yasal vasim Gizlilik Politikası'nı " +
                "okudu ve uygulamayı kullanmama onay verdi.",
            secili = onayli,
            onDegis = onOnayDegisti,
            baglantilar = mapOf(
                "Gizlilik Politikası" to { onSozlesme(Sozlesmeler.GIZLILIK_POLITIKASI) },
            ),
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu bir beyandır; uygulama onayı doğrulayamaz. Yanlış " +
                "beyan sorumluluğu kullanıcıya aittir.",
            style = MaterialTheme.typography.labelMedium,
            color = MetinIkincil,
            textAlign = TextAlign.Start,
        )
    }
}
