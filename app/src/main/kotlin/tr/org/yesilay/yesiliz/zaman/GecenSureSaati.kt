package tr.org.yesilay.yesiliz.zaman

import android.os.SystemClock
import javax.inject.Inject

/** Cihaz derin uykudayken geçen süreyi de içeren monoton saat. */
class GecenSureSaati @Inject constructor() {
    fun simdiMs(): Long = SystemClock.elapsedRealtime()
}

/** Duvar saati değişikliklerinden etkilenmeyen sayaç hesabı. */
internal object GecenSureHesapla {
    fun saniye(baslangicMs: Long, simdiMs: Long): Int =
        ((simdiMs - baslangicMs).coerceAtLeast(0L) / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    fun dolduMu(baslangicMs: Long, simdiMs: Long, toplamSaniye: Int): Boolean =
        saniye(baslangicMs, simdiMs) >= toplamSaniye
}
