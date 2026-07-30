package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
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
)

data class OnerilenEylem(
    val id: String,
    val ikon: ImageVector,
    val baslik: String,
    val aciklama: String,
    val eylemMetni: String,
)

/** Ekranın statik öneri kataloğu — gerçek ayara bağlanması sıradaki iş. */
val VarsayilanOneriler = listOf(
    OnerilenEylem(
        id = "gece_bildirim",
        ikon = Icons.Filled.DarkMode,
        baslik = "Gece 23:00'ten sonra bildirimleri kapat",
        aciklama = "Gece kullanımını azaltmanın en etkili yolu",
        eylemMetni = "Aç",
    ),
    OnerilenEylem(
        id = "hedef_azalt",
        ikon = Icons.Filled.TrackChanges,
        baslik = "Günlük hedefini 20 dk azalt",
        aciklama = "Kademeli azaltma daha kalıcı sonuç verir",
        eylemMetni = "Ayarla",
    ),
)
