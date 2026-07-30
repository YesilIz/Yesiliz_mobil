package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.ui.unit.dp

/**
 * DESIGN.md §1 boşluk skalası ve ölçüler.
 *
 * CLAUDE.md §7: **Composable içinde hardcoded dp YASAK** — her ölçü buradan.
 */
object Bosluk {
    val Xs = 4.dp
    val S = 8.dp
    val M = 16.dp
    val L = 24.dp
    val Xl = 32.dp
    val Xxl = 48.dp
}

object Olcu {
    /** ekran kenar boşluğu — sol/sağ 20dp, tüm ekranlarda tutarlı */
    val EkranKenari = 20.dp

    /** kart iç boşluğu */
    val KartIci = 20.dp

    /** dikey öğeler arası standart aralık */
    val Aralik = 16.dp

    /** büyük bölümler arası aralık */
    val BolumArasi = 32.dp

    /** primary/secondary buton yüksekliği */
    val ButonYuksekligi = 56.dp

    /** secondary buton kenarlık kalınlığı */
    val ButonKenarligi = 1.5.dp

    /** ilerleme çubuğu yüksekliği */
    val IlerlemeYuksekligi = 8.dp

    /** girdi alanı kenarlık kalınlığı */
    val GirdiKenarligi = 1.5.dp

    /** görev kartı sol öncelik şeridi */
    val GorevSeridi = 4.dp

    // --- teslim edilen tasarımdan (docs/tasarim-referans.md) ---

    /** yeşil ekran başlığının içerik yüksekliği (başlık satırı) */
    val BaslikYuksekligi = 56.dp

    /** başlık altındaki içeriğin başlığa bindiği yuvarlak köşe */
    val BaslikBindirmesi = 20.dp

    /** alt gezinme çubuğu */
    val AltBarYuksekligi = 72.dp

    /** liste/satır ikon rozeti (yuvarlak veya 12dp kare) */
    val IkonRozeti = 40.dp

    /** onboarding adım göstergesi çubuk kalınlığı */
    val AdimCubugu = 4.dp

    /** çubuk grafik alanı */
    val GrafikYuksekligi = 140.dp

    /** yığın bar (kategori dağılımı) */
    val YiginYuksekligi = 16.dp

    /** dairesel gösterge halka kalınlığı */
    val HalkaKalinligi = 14.dp

    /** liste avatarı (lig satırı) */
    val AvatarKucuk = 40.dp

    /** profil avatarı */
    val AvatarBuyuk = 96.dp

    /** Likert dairesi (1–6) */
    val LikertDairesi = 44.dp

    /** kart kenarlığı — gölge tek başına Yesil50 zeminde ayırmıyor */
    val KartKenarligi = 1.dp

    /** çubuk grafiğin sağındaki eksen etiketi sütunu */
    val GrafikEkseni = 28.dp

    /** çubuk ucu yuvarlaması */
    val CubukYaricapi = 4.dp

    /** ana sayfada ağaç illüstrasyonunun bulunduğu gradyan alan */
    val AgacAlani = 220.dp

    /** ağaç illüstrasyonunun kendi yüksekliği */
    val AgacBoyu = 180.dp

    /** özet şeridindeki dikey ayraç */
    val AyracBoyu = 40.dp

    /** ödül basamakları arasındaki dikey bağlantı çizgisi */
    val BasamakBoslugu = 72.dp

    /** boşluksuz kart için */
    val Sifir = 0.dp

    /** nefes ekranı — dolu iç daire */
    val NefesDairesiIc = 180.dp

    /** nefes ekranı — soluk dış halka (iç dairenin etrafında) */
    val NefesDairesiDis = 260.dp

    /** nefes ekranı — oynat/duraklat ana kontrolü */
    val NefesKontrolBuyuk = 72.dp

    /** nefes ekranı — tempo seçici ve baştan başlat kontrolleri */
    val NefesKontrolKucuk = 56.dp

    /** nefes ekranı — faz göstergesi noktası */
    val NefesFazNoktasi = 8.dp
}

/**
 * Gölge — DESIGN.md: "çok yumuşak, y+2 blur 12, %6 opaklık".
 *
 * Compose'un `shadow()` modifier'ı blur/opaklığı ayrı ayrı almıyor;
 * `elevation` ile yaklaşık karşılığı 2dp. Renk `YesilKoyu` %6.
 */
object Golge {
    val Kart = 2.dp

    /** basılıyken kart batmaz, hafifçe küçülür (%98) */
    const val BasiliOlcek = 0.98f
}
