# debug.md — Yeşilİz açık kusur listesi

> **Cihaz:** Samsung SM-A217F (Galaxy A21s) · Android 12 / API 31 · 720×1600 @280dpi · 3 tuşlu gezinme
> **Tarih:** 1 Ağustos 2026 · **Derleme:** `fix/gercek-telefon-onkosullari` @ `170a3c4`
>
> **İKİNCİ TARAMA.** Birinci tarama (31 Temmuz) `docs/tarama-01.md`'de arşivlendi;
> oradaki **9 kritik + 12 yüksek + 18 orta kusurun ve 5 kapsam kararının tamamı**
> kapatıldı ve her biri bu cihazda doğrulandı.
>
> Bu turun yöntemi:
> - `[cihaz]` — `pm clear` ile sıfırlanıp uçtan uca gezildi; her ekran `uiautomator`
>   ile dökülüp **sınırları ölçüldü**, sonuç ekran görüntüsü + Room veritabanı
>   (`run-as … cat databases/yesiliz.db`) ile doğrulandı. Backend USB üzerinden
>   (`adb reverse tcp:8000 tcp:8000`) gerçekten bağlandı.
> - `[kod]` — sistematik arama: ölü tıklama, çağrılmayan ViewModel fonksiyonu,
>   okunmayan tercih, kaydırmasız ekran, ana iş parçacığında bloklayan çağrı,
>   Composable içinde sabit dp/renk, kopyalanmış eşleme.
>
> Birinci taramanın kapsamadığı alanların bir kısmı bu turda kapsandı (aktif
> yürüyüş, backend'li senkron, ebeveyn onayı akışı); kalanlar §5'te.

---

## §0 · Bu turda ne değişti

Birinci tarama zincirin **etrafındaki** kusurları buluyordu. İkinci tarama iki
şey buldu: daha derin katmanlar ve **düzeltmelerin kendi açtığı** kusurlar.
Sekiz bulgunun **beşi bu gecenin regresyonu** — 125 dosyada 6.800 satır
değiştiğine göre beklenen bir sonuç ve taramanın niçin gerektiğinin kanıtı.

```
UsageStats → Room → core-risk → risk_sonuc → StateFlow → Composable
```

Zincir temiz kurulumda artık **doğru** davranıyor: `gunluk_ozet` tek satır
(yalnızca kurulum günü), `puan_gunluk` seri 1, `risk_sonuc` gerçek skor.
Backend'e gerçek cihazdan `POST /auth/kayit 201` · `GET /lig 200` ·
`POST /lig/katil 200` · `POST /ozet 200` gitti.

**Bu turda bulunan sekiz kusurun sekizi de aynı gece düzeltildi.** Aşağısı kayıt
amaçlı; açık iş §4'te.

---

## §1 · KRİTİK

### ✅ K-1 · Anket ekranı çöküyor — iç içe iki dikey kaydırma `[cihaz]`

**`ui/ekran/onboarding/OnboardingIskelet.kt` · `AnketEkran.kt`**

Onboarding 3/4'ten sonra uygulama çöküyordu. **Temiz kurulumda hiçbir kullanıcı
ana ekrana ulaşamıyordu.**

```
java.lang.IllegalStateException: Vertically scrollable component was measured
with an infinity maximum height constraints, which is disallowed.
```

**Sebep — bu gecenin regresyonu.** Birinci taramanın O-1 bulgusunu (onboarding
içeriği kaydırılamıyor) düzeltirken `OnboardingIskelet`'in içerik alanına
`verticalScroll` eklendi. `AnketEkran`'ın **zaten** kendi kaydırması vardı; iç
içe iki dikey kaydırma, içtekinin sonsuz yükseklik kısıtıyla ölçülmesi demek ve
Compose bunu reddediyor.

İkinci kaynak da aynı gece eklenmişti: `wrapContentHeight(unbounded = true)`
(Y-1'i çözmek için denenmiş, işe yaramamıştı) alt ağaçtaki yükseklik kısıtını
sonsuza çeviriyordu — Compose'un hata mesajı bu modifier'ı adıyla sayıyor.

**Düzeltme:** `OnboardingIskelet` `kaydirilabilir` parametresi aldı; kendi
kaydırmasını yöneten ekranlar `false` veriyor (şu an yalnızca `AnketEkran`).
`unbounded` geri alındı, niçin geri alındığı yerinde yazıldı.

---

## §2 · YÜKSEK

### ✅ Y-1 · Onboarding ikincil bağlantısı dokunulamıyor `[cihaz]`

**`ui/ekran/onboarding/OnboardingIskelet.kt`**

Karşılama ekranındaki **"Nasıl çalışır?"** ve dört onboarding ekranındaki
**"Şimdi değil"** ekranda görünüyor ama dokunulamıyordu.

Ölçüm (ComposeView 1448px):

| Öge | Sınırlar | Yükseklik |
|---|---|---|
| İçerik (ScrollView) | `[35,150][685,1334]` | 1184px |
| Birincil buton | `[35,1334][685,1432]` | 98px ✓ |
| **İkincil bağlantı** | `[35,1439][685,1448]` | **9px** ✗ |

Metin 1474'te çiziliyordu — ComposeView'ın (1448) **dışında**. Erişilebilirlik
ağacında sınırlar `[0,0][0,0]`.

`MetinBaglanti` bir `TextButton` ve bu yerleşimde kalan ana eksen alanına göre
ölçülüp kendi asgari yüksekliğinin altına iniyordu.

**Denenen ve yetmeyen üç yol** — tekrarlanmasın diye kayıtta:
alt bloğu ayrı bir `Column`'a almak · `wrapContentHeight(unbounded = true)`
(üstelik K-1'i doğurdu) · `weight(1f, fill = false)` tek başına (hedefi
kurtarıyor ama butonları ekranın dibinden kaldırıyor).

**Düzeltme:** ikincil eylem sabit yükseklikli tıklanabilir bir `Box` içinde düz
`SatirBaglanti`. `Arrangement.SpaceBetween` + `weight(1f, fill = false)` ile
birlikte butonlar dipte kalıyor ve hedef her zaman dokunulabilir oluyor.

### ✅ Y-2 · Kurulum tarihi filtresi ilk koşuda uygulanmıyor `[cihaz]`

**`ui/gezinme/AcilisViewModel.kt`**

Birinci taramanın K-4'ü için worker'a "kurulumdan öncesini telafi etme" filtresi
eklenmişti ama **ilk koşuda hiç uygulanmıyordu**: `AcilisViewModel`
`simdiTazele()`'yi `init`'in başında, `kurulumuIsaretle()`'yi ondan sonra
çağırıyordu. Worker `ilkKurulumTarihi`'ni `null` görüyor, filtre devreye
girmiyordu.

Temiz kurulumda ölçülen:

| | `gunluk_ozet` | `puan_gunluk` | ana ekran |
|---|---|---|---|
| önce | 8 satır (25 Tem – 1 Ağu), 6'sı boş | 8 satır | "2 gün · 80 puan" |
| sonra | **1 satır** (kurulum günü) | **1 satır** | "1 gün · 38 puan" |

Boş günlerin puan vermemesi `olayVarMi` ile zaten düzelmişti; bu düzeltme
satırların **hiç yazılmamasını** sağlıyor. Aksi hâlde İstatistik ekranı
kurulumdan önceki günleri grafiğe çiziyor ve haftalık karşılaştırma uydurma bir
haftayla yapılıyordu.

### ✅ Y-3 · Az veriden maksimum iddia — örüntü oranları `[cihaz]`

**`core-risk/.../EtkileyenFaktorler.kt` · `Sabitler.kt`**

Birinci taramanın K-7'sinin aynısı, farklı faktörlerde. Örüntüler (gece,
kesintisiz oturum, sosyal medya oranı) günün toplam süresine bölünüyor; toplam
birkaç dakikayken bölme uç değerler üretiyor.

Cihazda ölçülen (temiz kurulum, saat 01:45):

```
gunluk_ozet: toplamDk 0,57 · gece 1,0 · acilisSayisi 2
Risk analizi: üstte "Düşük" risk düzeyi, hemen altında "Gece kullanımı → Yüksek"
```

34 saniyelik kullanımın **tamamı** gece penceresine düştüğü için oran 1,0
çıkıyordu. Uygulamayı akşam kuran her kullanıcı ilk gece bunu görüyor.

**Düzeltme:** `Sabitler.FAKTOR_ASGARI_DK = 15.0`. Eşiğin altında kullanım
oranları seviye iddia etmiyor, `BILINMIYOR` dönüyor ve çubuk boş kalıyor. Eşik
**geçici** — A3 kalibrasyonuyla gözden geçirilecek (CLAUDE.md §12 A3).

---

## §3 · ORTA

| # | Bulgu | Kaynak |
|---|---|---|
| **✅ O-1** | **"Veri yok" gerekçesi yanlış faktöre veriliyordu.** `RiskDetayEkran.kt` — üç kullanım faktörü de "Profil › Adım ve hareket iznini açarsan burada görünecek" diyordu. O gerekçe yalnızca hareket faktörü için doğru; kullanım oranlarında sebep yetersiz ölçüm. Metin artık faktöre göre değişiyor | `[cihaz]` |
| **✅ O-2** | **Odak ve göz molası ekranlarında kaydırma yok.** Bu gece yazılan iki ekran, birinci taramanın O-1'iyle aynı hatayı tekrarlıyordu: büyük yazı tipi ölçeğinde ve küçük ekranda içerik kırpılır | `[kod]` |
| **✅ O-3** | **Nefes bitiş metni süreyi sabit söylüyordu.** `NefesEkran.kt` — seans süresi seçilebilir olduktan sonra (İ-2) "Kendine iki dakika ayırdın" 1 ve 5 dakikalık seanslarda yanlıştı | `[cihaz]` |
| **✅ O-4** | **Görev → ikon eşlemesi iki ViewModel'de kopyalanmıştı.** `AnaViewModel` ve `OdulViewModel`; `Gorev`'e yeni değer eklenince ikisi birden düzeltilmek zorundaydı — dört yeni aktivite eklenirken ikisi de derleme hatası verdi. `ui/GorevGorunum.kt`'de tek yere alındı | `[kod]` |
| **✅ O-5** | **XML yorumunda çift tire manifest'i iki kez patlattı.** Türkçe yorumda tire-tire ayraç yazmak refleks; XML'de yorum içinde yasak. `processDebugMainManifest` `MergeFailureException` veriyor ama sebebi söylemiyor. CLAUDE.md §9'a tuzak olarak eklendi | `[kod]` |

### Taranıp temiz çıkanlar `[kod]`

Bu aramalar yapıldı ve **bulgu üretmedi** — bir dahaki turda tekrarlanmasın:

| Arama | Sonuç |
|---|---|
| `onTikla = {}` / `onClick = {}` | 4 tane; **dördü de bilinçli** (devre dışı "Kişilerden bul", tek seçenekli kart, tema önizlemesi ×2) |
| `TODO` / `FIXME` / `HACK` | **sıfır** |
| Çağrılmayan ViewModel fonksiyonu | **sıfır** (birinci turda üç taneydi: `cikisYap`, `eslesmeyiKaldir`, `ligdeGorun`) |
| Okunmayan `Tercihler` alanı | **sıfır** |
| Composable içinde sabit renk | **sıfır** |
| Composable içinde sabit `dp` | 4 tane, hepsi dosya düzeyinde `private val` — token değil yerel ölçü |
| `runBlocking` | 4 tane, hepsi OkHttp interceptor / `Authenticator` içinde, gerekçesi yazılı |
| `ModalBottomSheet` doğrudan kullanımı | **sıfır** — hepsi `ui/bilesen/AltSayfa.kt` üzerinden |
| Kaydırmasız tam ekran | 2 tane (`NefesEkran`, `YuruyusEkran`); ikisi de sabit yerleşimli, cihazda doğrulandı |

---

## §4 · AÇIK İŞLER

Kusur değil; kapsam ya da altyapı.

| İş | Not |
|---|---|
| **Backend dağıtımı** | Sunucu adresi çalışma zamanında okunuyor (CLAUDE.md §12 K24) ve gerçek cihaz USB üzerinden `adb reverse` ile yerel sunucuya bağlanıyor. **Kalıcı dağıtım + HTTPS alan adı hâlâ yok.** Release `ag_guvenligi.xml` şifresiz trafiği yalnızca `10.0.2.2` için açıyor; debug kaynak seti kendi gevşek yapılandırmasını getiriyor ve yayınlanan APK'ya giremiyor |
| **Uzman/Yeşilay paneli** | Web tarafı — hiç başlanmadı, MVP kritik yolunda değil |
| **`FAKTOR_ASGARI_DK = 15`** | Y-3'ün eşiği geçici. A3 kalibrasyonuyla birlikte gözden geçirilecek |
| **Ebeveyn onayının doğrulanabilirliği** | Beyan usulü; uygulama onayı doğrulayamıyor ve bunu ekranda söylüyor. Gerçek doğrulama §12 A5'e bağlı |
| *Faz 3* | ML katmanı (scikit-learn · XGBoost · SHAP), A1–A3 kalibrasyonu — pilot verisi bekliyor |

---

## §5 · Bu taramanın kapsamadıkları

Dürüstlük gereği: aşağıdakiler denenmedi, "çalışıyor" denemez.

- **Widget'lar** (`EkranSuresiWidget`, `GununSorusuWidget`) — sağlayıcı olarak
  kayıtlı oldukları `dumpsys appwidget` ile doğrulandı, ama ana ekrana
  yerleştirme `uiautomator` ile otomatikleştirilemedi. **İki turdur kapsanmıyor;
  bir dahaki turda elle yapılmalı.**
- **Nudge overlay'in kendisi** — servis ayağa kalkıyor (1. tarama Y-9) ama
  tetikleyici eşiği tarama süresince oluşmadı; overlay hiç çizilmedi.
- **Ebeveyn eşleştirmesinin iki cihazlı hâli** — tek cihaz var. Kod yolu ve
  sunucu doğrulaması pytest'ten geçti.
- **Uzun süreli yürüyüş** — foreground service 20 saniyelik ekran kapalı
  testiyle doğrulandı (sayaç 0:11 → 0:31); 15 dakikalık tam oturum ve pil
  etkisi ölçülmedi.
- **Odak seansının bozulma yolu** — ekran açılınca seansın bozulduğu koddan ve
  birim testten doğrulandı, cihazda 15 dakika beklenmedi.
- **YEDAM onayı ekranı** — yalnızca `Bant.COK` iken erişilebiliyor; bu turda
  temiz kurulumda bant `AZ` kaldı.
- **Release derlemesi** — her şey `debug` üzerinde doğrulandı. Release'in kendi
  ağ yapılandırması ve `DemoVeri` no-op'u ayrıca sınanmalı.
- **`GECIS_2_3` gerçek yükseltme yolu** — emülatör testi (`GecisTest`) 1→3 ve
  2→3'ü doğruluyor; cihazda da sürüm 2 veritabanı 3'e taşındı ve çökme olmadı,
  ama veri dolu bir 2 sürümü veritabanıyla yükseltme yapılmadı.

---

## §6 · Bu gecenin özeti

| | |
|---|---|
| Commit | 16 |
| Değişen dosya | 125 · yaklaşık +6.800 / −660 satır |
| Kapatılan kusur | **9 K + 12 Y + 18 O** (1. tarama) · **1 K + 3 Y + 5 O** (2. tarama) |
| Uygulanan kapsam kararı | 5 (İ-1 … İ-5) |
| Test | 147 birim (app) + 22 (core-risk) + 30 cihaz + 53 backend = **252** · hepsi geçiyor |
| Yeni CLAUDE.md kararı | K20–K26; K15 tersine çevrildi |
| Yeni bilinen tuzak | 6 (CLAUDE.md §9) |
| Yeni backend ucu | `DELETE /auth/hesap` · `lig_uyelik.gorunur` (Alembic) |
