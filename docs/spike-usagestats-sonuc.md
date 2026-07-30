# spike-usagestats-sonuc.md — UsageStats Spike

> **DURUM: ÖN TESPİT — spike TAMAMLANMADI.**
> Bu belge yalnızca **ortam seviyesinde** doğrulananları kaydediyor: API'ler cihazda
> var mı, izin ekranları çözülüyor mu, emülatörde ne kadar geçmiş veri oluşuyor.
> Asıl spike (CLAUDE.md §11) Kotlin'den `queryUsageStats` / `queryEvents`
> çağırıp **dönen verinin şeklini ve derinliğini** ölçmek — o henüz yapılmadı,
> çünkü o sırada Gradle projesi henüz kurulmamıştı.

**Tarih:** 2026-07-28
**Ortam:** AVD `Yesiliz_Pixel7_API36` · Android 16 / API 36 ·
`system-images;android-36;google_apis_playstore;arm64-v8a` · macOS arm64

---

## 1. DOĞRULANANLAR

| Kontrol | Komut | Sonuç |
|---|---|---|
| `usagestats` sistem servisi | `adb shell service check usagestats` | ✅ found |
| Kullanım erişimi izin ekranı | `resolve-activity -a android.settings.USAGE_ACCESS_SETTINGS` | ✅ `Settings$UsageAccessSettingsActivity` |
| Overlay izin ekranı (nudge ön koşulu) | `resolve-activity -a ...MANAGE_OVERLAY_PERMISSION` | ✅ `Settings$OverlaySettingsActivity` |
| Health Connect | `pm list packages` | ✅ `com.google.android.healthconnect.controller` — Android 16'da **platforma gömülü**, ayrı kurulum gerekmiyor |

Yani `data/kullanim/` ve `data/saglik/` sarmalayıcılarının dayanacağı API yüzeyi
emülatörde eksiksiz. Kullanıcıyı izin vermeye yönlendiren iki intent de çözülüyor —
onboarding §5.2 ekranının "İzin ver" butonu bir yere gidebilir.

---

## 2. KRİTİK BULGU — emülatör döngüyü doğrulayamaz

Taze emülatörde **hiç üçüncü parti uygulama yok** (`pm list packages -3` → 0).
Ve `dumpsys usagestats` şunu gösteriyor:

```
In-memory daily   stats  timeRange="7/28/2026, 11:49 – 11:52 AM"
In-memory weekly  stats  timeRange="7/28/2026, 11:49 – 11:52 AM"
In-memory monthly stats  timeRange="7/28/2026, 11:49 – 11:52 AM"
In-memory yearly  stats  timeRange="7/28/2026, 11:49 – 11:52 AM"
```

Dört kova da yalnızca **açılıştan bu yana geçen 3 dakikayı** kapsıyor. Sonuçları:

1. **`B_tuketim` her zaman 0 olur.** `social` / `entertainment` / `games`
   kategorilerinde hiç dakika birikmez → `S ≈ 0` → `R_b ≈ 0` → bant hep AZ.
   Risk motoru teknik olarak çalışır ama **ayırt edici hiçbir şey üretmez.**
2. **`d_efektif = 0`** (spec-risk §4.5). Davranış verisi günü yok demektir,
   dolayısıyla `alpha = 1.0` — skor %100 ankete dayanır. **Alpha eğrisinin
   hiçbir kolu emülatörde tetiklenmez**; `d < 7`, `d < 28`, `d ≥ 28` dallarının
   üçü de test edilemez.
3. CLAUDE.md §10'daki *"Android: kurulumda geçmiş ✅ haftalar (hacim)"* iddiası
   **kullanılmış bir cihaz** varsayıyor. Taze kurulumda geçmiş sıfır — iddia
   yanlış değil ama koşullu, prototip demosu bunu hesaba katmalı.

### Ne yapmalı

| Yol | Ne verir | Ne vermez |
|---|---|---|
| **Gerçek telefon (USB hata ayıklama)** — önerilen | Haftalarca gerçek kullanım geçmişi, gerçek kategori dağılımı, gerçek gece örüntüsü; overlay ve foreground service'in üretici pil yönetimi altındaki davranışı | — |
| Emülatöre Play Store'dan uygulama kurup elle kullanmak | API doğrulaması, birkaç saatlik sentetik veri | Geçmiş derinliği, gerçekçi örüntü, üretici davranışı |
| Room'a sahte gün üretici (`YerelSahteKullanimDeposu`) | UI ve bant/histerezis akışının demosu | API doğrulaması |

**Karar önerisi:** API doğrulaması emülatörde, döngü doğrulaması gerçek telefonda.
Demo günü için üçüncü seçenek (sahte veri üretici) da yedek olarak yazılmalı —
sahnede telefonun 7 günlük geçmişine güvenmek riskli.

---

## 3. HENÜZ YANITLANMAMIŞ — asıl spike soruları

Bunlar Gradle projesi kurulduktan sonra Kotlin'den ölçülecek:

1. `queryUsageStats(INTERVAL_DAILY, ...)` gerçek bir telefonda **kaç gün** geriye gidiyor?
   (spec-risk §4.5 "geçmiş gün → 0.6 kalite ağırlığı" varsayımı buna dayanıyor)
2. `queryEvents()` ne kadar saklanıyor? Örüntü metrikleri (`gece`, `oturum`,
   `acilis`, `sabah`, `uyku_oncesi`) yalnızca event'lerden çıkarılabiliyor —
   saklama süresi kısaysa `P` çarpanı geçmiş günler için hesaplanamaz.
3. `oturum` metriği için **kesintisiz blok** tanımı: hangi event çifti bir oturumu
   kapatıyor? (`ACTIVITY_PAUSED` mi, `SCREEN_NON_INTERACTIVE` mi?)
4. Ekran kapalıyken arka planda çalan uygulama (Spotify, YouTube arka plan) süreye
   dahil oluyor mu? Dahilse `entertainment` şişer — CLAUDE.md §4'deki
   "sadece süre/sıklık/kategori" kuralını bozmadan nasıl ayıklanır?
5. `queryUsageStats` günlük kovası cihazın **saat dilimi/gün sınırı** ile bizim
   `GunlukOzetEntity.tarih` sınırımız aynı mı?

---

## 4. TEKRAR ÜRETİM

```bash
emulator -avd Yesiliz_Pixel7_API36 -no-snapshot-load &
adb wait-for-device
adb shell service check usagestats
adb shell cmd package resolve-activity -a android.settings.USAGE_ACCESS_SETTINGS
adb shell dumpsys usagestats | grep -E 'In-memory|timeRange'
```
