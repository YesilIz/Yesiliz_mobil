package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.tema.Beyaz

/**
 * Uygulamanın tek alt sayfa kabuğu.
 *
 * Üç davranışı **tek yerden** garanti ediyor; üçü de tek tek yazıldığında
 * unutuldu ve gerçek cihazda ekranların yarısını kullanılamaz hâle getirdi
 * (`debug.md` Y-1, K-3):
 *
 * 1. **`skipPartiallyExpanded = true`** — sayfa yarı açık başlamıyor.
 *    Yarı açıkken içerik sığmıyor, son öge (çoğu zaman birincil eylem
 *    butonu) ekran dışında kalıyordu. Kategori seçiminde "Kategoriyi
 *    kaydet" butonuna **hiç** ulaşılamıyordu: erişilebilirlik ağacında
 *    sınırları `[0,0][0,0]` görünüyordu.
 * 2. **İçerik kaydırılabilir** — sığmayan içerik sürüklenerek okunabilsin.
 *    `ModalBottomSheet`'in kendi sürüklemesi sayfayı *kapatıyor*, içeriği
 *    kaydırmıyor; iç `verticalScroll` olmadan uzun içerik kırpılıyordu.
 * 3. **`contentWindowInsets`** — M3 1.3'ten beri alt sayfa kenardan kenara;
 *    açıkça verilmezse son buton sistem gezinme çubuğunun altında kalıyor
 *    (CLAUDE.md §9).
 *
 * Ayrıca her çağrı **kendi** `sheetState`'ini alıyor. Ekranlar tek bir
 * `rememberModalBottomSheetState()` örneğini bütün alt sayfalarında
 * paylaşıyordu; biri kapanıp durum `Hidden`'a düştüğünde sıradaki sayfa
 * aynı durumla açılıp hiç çizilmiyordu.
 *
 * @param onKapat perde dokunuşu, aşağı sürükleme ve geri tuşu bu geri
 *   çağrıyı tetikliyor. **Boş bırakılamaz** — `ModalBottomSheet` kendini
 *   yine de gizler, ama bileşimden düşmediği için görünmez bir perde
 *   dokunuşları ve geri tuşunu yutmaya devam eder (`debug.md` K-1).
 * @param kaydirilabilir kendi içinde kaydırma yöneten içerikler (ör. uzun
 *   listeler) için `false` verilir; iç içe iki kaydırma çakışır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltSayfa(
    onKapat: () -> Unit,
    modifier: Modifier = Modifier,
    kaydirilabilir: Boolean = true,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onKapat,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Beyaz,
        contentWindowInsets = { WindowInsets.navigationBars },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (kaydirilabilir) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                ),
            content = icerik,
        )
    }
}
