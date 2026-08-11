package tr.org.yesilay.yesiliz.data.saglik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.data.yerel.entity.AdimSayacEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Yalnızca debug APK'da bulunan donanım adım sayacı tanısı (`gorev2.md` §2).
 *
 * `SaglikTaniAlicisi`'nin (Health Connect) donanım sayacı karşılığı —
 * aynı desen: `adb shell am broadcast` ile tetiklenir, sonuç logcat'e yazılır.
 * Adım sapmalarının nedenini (sensör mi, birikim hesabı mı, yeniden
 * başlatma mı) ayırt etmek için gereken sekiz bilgiden ([AdimSayacOkuyucu],
 * [AdimBirikim] kdoc'larında adı geçenler) production'da tutulmayanları
 * burada bir kerelik topluyor.
 *
 * Bu çağrının kendisi bir örnekleme tetikliyor (`AdimDeposu.ornekle()`
 * çağrılıyor) — pasif bir geçmiş sorgusu değil, "şu an" durumunun tanısı.
 * Bu yüzden "son sensör okuma zamanı" ile "son başarılı kayıt zamanı"
 * aynı ana denk düşüyor: ikisi de bu tanı isteğinin anı.
 *
 * ```bash
 * adb shell am broadcast -a tr.org.yesilay.yesiliz.ADIM_TANI
 * adb logcat -s YesilizAdimTani
 * ```
 */
class AdimTaniAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bekleyen = goAsync()
        val simdi = Instant.now()
        val saatDilimi = Clock.systemDefaultZone().zone
        val bugun = LocalDate.now(saatDilimi)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val okuyucu = AdimSayacOkuyucu(context)
                val dao = YesilizDb.al(context).adimSayacDao()

                val oncekiKayit = dao.gunun(bugun)
                val hamSayac = okuyucu.anlikSayac()

                // Yeniden başlatma tespiti [AdimBirikim] ile aynı kural:
                // ham değer önceki örnekten küçükse sayaç sıfırlanmış demektir.
                val yenidenBaslatildiMi = oncekiKayit != null && hamSayac != null &&
                    hamSayac < oncekiKayit.sonSayac

                // Tanının kendisi güncel bir örnekleme de tetikliyor —
                // "hesaplanan günlük adım" bu çağrıdan SONRAKİ hâli yansıtsın.
                val onceki = oncekiKayit?.let { AdimDurumu(it.toplam, it.sonSayac) }
                val yeniDurum = hamSayac?.let { adimBirikimGuncelle(onceki, it) }
                if (yeniDurum != null) {
                    dao.kaydet(AdimSayacEntity(bugun, yeniDurum.toplam, yeniDurum.sonSayac))
                }

                Log.i(
                    ETIKET,
                    "gun=$bugun saatDilimi=$saatDilimi " +
                        "veriKaynagi=donanim_sayaci " +
                        "cihazDestekliyor=${okuyucu.cihazDestekliyor()} " +
                        "izinVarMi=${okuyucu.izinVarMi()} " +
                        "hamSayac=$hamSayac " +
                        "gunBaslangicSayaci=${oncekiKayit?.sonSayac} " +
                        "hesaplananGunlukAdimOncesi=${oncekiKayit?.toplam} " +
                        "hesaplananGunlukAdimSonrasi=${yeniDurum?.toplam} " +
                        "yenidenBaslatildiMi=$yenidenBaslatildiMi " +
                        "cihazAcikKalmaSuresiDk=${SystemClock.elapsedRealtime() / 60_000} " +
                        "sonOkumaVeKayitZamani=$simdi",
                )
            } catch (hata: Throwable) {
                Log.e(ETIKET, "Adım tanısı çalışmadı", hata)
            } finally {
                bekleyen.finish()
            }
        }
    }

    private companion object {
        const val ETIKET = "YesilizAdimTani"
    }
}
