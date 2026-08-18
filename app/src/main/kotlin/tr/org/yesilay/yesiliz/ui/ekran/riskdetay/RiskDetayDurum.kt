package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktor

/**
 * Risk analizi ekranı — `docs/tasarim-referans.md §2 / 30`.
 *
 * CLAUDE.md §11 sıradaki iş #2. Ana Sayfa/İstatistik'teki risk kartından
 * açılıyor, `risk_sonuc` + o günün `gunluk_ozet`'i birleştiriliyor.
 */
data class RiskDetayDurum(
    val veriVarMi: Boolean = false,
    val bant: Bant? = null,
    val bantEtiketiKisa: String = "",
    val aciklama: String = "",
    val trendMesaji: String? = null,
    val oran: Float = 0f,
    val faktorler: List<EtkileyenFaktor> = emptyList(),
    val oneriler: List<OnerilenEylem> = emptyList(),
    /**
     * [bant] `null` iken gösterilen boş durum metni — hiç veri yokken ile
     * ölçüm yetersizken/eskiyken aynı ekran, farklı gerekçe
     * (CLAUDE.md §12 K30, `RiskDetayViewModel`).
     */
    val bosDurumBaslik: String = "Henüz yeterli veri yok",
    val bosDurumAciklama: String = "Birkaç günlük kullanım biriktiğinde risk " +
        "analizin burada görünecek.",
)

data class OnerilenEylem(
    val id: String,
    val ikon: ImageVector,
    val baslik: String,
    val aciklama: String,
    val eylemMetni: String,
)

/**
 * Öneri kataloğu — her maddenin gerçek bir hedefi var.
 *
 * [OnerilenEylem.id] ekranın dokunma işleyicisinde rotaya çevriliyor;
 * tıklanınca hiçbir şey yapmayan kart bırakılmıyor.
 */
val VarsayilanOneriler = listOf(
    OnerilenEylem(
        id = "bildirim_saatleri",
        ikon = Icons.Filled.DarkMode,
        baslik = "Hatırlatma saatlerini ayarla",
        aciklama = "Gece kullanımını azaltmanın en etkili yolu",
        eylemMetni = "Aç",
    ),
    OnerilenEylem(
        id = "hedef_azalt",
        ikon = Icons.Filled.TrackChanges,
        baslik = "Günlük hedefini gözden geçir",
        aciklama = "Kademeli azaltma daha kalıcı sonuç verir",
        eylemMetni = "Ayarla",
    ),
    OnerilenEylem(
        id = "nefes",
        ikon = Icons.Filled.SelfImprovement,
        baslik = "İki dakikalık nefes molası",
        aciklama = "Kesintisiz kullanımı bölmenin en kısa yolu",
        eylemMetni = "Başlat",
    ),
)

/**
 * Yalnızca **yüksek bantta** eklenen öneri.
 *
 * YEDAM yönlendirmesi düşük ve orta bantta önerilmiyor: ihtiyaç yokken
 * uzman desteği önermek, kullanıcıya olduğundan ağır bir tablo çizer
 * (CLAUDE.md §4 teşhis yasağı). Yüksek bantta bile bu yalnızca bir
 * **teklif** — dokunulmadan hiçbir şey olmuyor.
 */
val YedamOnerisi = OnerilenEylem(
    id = "yedam",
    ikon = Icons.Filled.SupportAgent,
    baslik = "Bir uzmanla konuşmak ister misin?",
    aciklama = "YEDAM ücretsiz ve gizli danışmanlık veriyor",
    eylemMetni = "Gör",
)
