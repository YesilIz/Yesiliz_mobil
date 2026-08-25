# Yeşilİz — 5. Tarama: Gereksizlik / "bu niye böyle" denetimi

> **Bu rapor yalnızca tespittir.** Hiçbir kaynak dosya değiştirilmedi, hiçbir şey
> silinmedi, hiçbir git yazma komutu çalıştırılmadı. Değiştirilen tek dosya budur.
> Karar kullanıcıya aittir.

---

## 0. Kapsam ve ortam

| | |
|---|---|
| Tarih | 17 Ağustos 2026, 16:30–17:20 (Europe/Istanbul) |
| Dal | **`fix/turan`** · HEAD `85d498c` "Lig puanı sunucuya gitmiyordu…" |
| Çalışma ağacı | Temiz (yalnızca izlenmeyen `Dökümanlar/`, `debug aahtarları/`, `yesiliz-cihaz-yedek/`) |
| Cihaz | Samsung **SM-A217F** · Android 12 (API 31) · `RF8NA115KCD` |
| İkinci hedef | `emulator-5554` bağlıydı, **kullanılmadı** |
| Kurulum | Cihazdaki APK `lastUpdateTime = 2026-08-17 15:38` — bu daldan bugün kurulmuş. **Yeniden kurulmadı**, yalnızca gezildi |
| Backend | Cihaz canlı Railway'e bağlı. **Hiçbir yazma isteği gönderilmedi**; yalnızca uygulamanın kendi `GET /lig` / `POST /ozet` trafiği |
| Cihaz değişikliği | Ekran uyandırıldı, uygulama gezildi. İzin/saat/saat dilimi **değiştirilmedi**, `pm clear`/`uninstall` **yapılmadı**, puan yazan hiçbir aktivite tamamlanmadı |
| Kanıt | `/tmp/tarama-05/ss/*.png` (21 ekran görüntüsü + `uiautomator` dökümleri), `/tmp/tarama-05/*.txt` (sayım çıktıları) |

### Ölçüm tabanı

| | |
|---|---|
| Kotlin dosyası | 321 (`app` 231 main · 5 debug · 3 release · 65 test · 7 androidTest; `core-risk` 7 main · 3 test) |
| Kotlin satırı | 41.409 (main tarafı 33.320) |
| Yorum yoğunluğu (main) | boş olmayan 31.431 satırın **7.361'i (%23,4)** yorum — bu bilinçli (§7) |
| Test | core-risk 30 · app birim 332 · cihaz 58 · backend 119 `test_` (3'ü smoke) |
| Şema | sürüm **9** · 15 entity · 15 DAO · 8 migration |
| Drawable | 13 PNG (`drawable-nodpi`, 4,7 MB) + 5 mipmap yoğunluğu + 2 vektör |

---

## 1. Ölçülebilir fazlalık

Bu bölümdeki her madde bir **sayıya** dayanıyor: 0 kullanım, ulaşılamaz kod,
birebir ikinci uygulama, ya da koda karşı doğrulanmış yanlış yorum. Tartışma
gerektirmiyor.

---

### G5-001 — `gorev1.md` / `gorev2.md` / `kubragorev.md` depoda yok; 35 yerden referans veriliyor

- **Tür:** doküman + yorum
- **Konum:** 16 kaynak dosyası + `CLAUDE.md` 5 satır + `app/build.gradle.kts:85` + `tools/kontrol_baslik.py:4`
- **Ne var:** Kodda ve KDoc'larda bu dosyalara **35 referans** var. Örnekler:

  ```
  core-risk/.../RiskHesapla.kt:68        `gorev2.md` §8 "STALE_DATA"
  core-risk/.../Sabitler.kt:148,162      gorev2.md §8 / §8-9
  .../domain/kullanim/AktiviteTekillestir.kt:8,26,37   gorev2.md §6
  .../data/saglik/SaglikOkuyucu.kt:134,163            gorev2.md §4 / §4-§6
  .../data/yerel/Gecisler.kt:150                      gorev2.md §4
  .../di/VeriModulu.kt:106                            gorev2.md §3
  app/src/androidTest/.../GecisTest.kt:179,196,228,356 gorev2.md §8/§10, §4
  app/build.gradle.kts:85               "(`gorev1.md` son iki kriter)"
  tools/kontrol_baslik.py:4             "`kubragorev.md` §C"
  ```

  Dosya dağılımı: `gorev2.md` **33** referans, `gorev1.md` 1, `kubragorev.md` 1.
- **Niye gereksiz:** Üç dosya da **depoda yok ve git geçmişinde hiç olmamış**
  (`git log --all --diff-filter=A --name-only | grep -i gorev` → yalnızca
  `docs/gorev-dagilimi.md` ve `domain/gorev/` kaynakları). Yani bir okuyucu
  "`gorev2.md` §6'daki çakışma toleransı" yazan bir KDoc'u görüp o belgeyi
  **hiçbir yerde açamıyor**. Referans, açıklama yerine bir çıkmaz sokak.
- **Silinirse ne olur:** Davranış değişmez. 16 kaynak dosyasında ~23 yorum satırı
  ya silinir ya da gerekçe yerinde yazılır (bazıları zaten K30/K31/K32/K33
  olarak CLAUDE.md'ye geçmiş — referans oraya çevrilebilir).
- **Risk:** Yok. Yalnızca yorum.
- **Gerekçesi var mı:** Yok. K30–K33 bu görevlerin **sonuçlarını** kaydetmiş,
  görev dosyalarının kendisi hiç eklenmemiş.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-002 — `app/build.gradle.kts:305` yorumu yalan söylüyor: "ViewModel'ler henüz Hilt'siz"

- **Tür:** yorum (yalan)
- **Konum:** `app/build.gradle.kts:304-306`
- **Ne var:**

  ```kotlin
  // Tek Activity + NavHost (CLAUDE.md §6). ViewModel'ler henüz Hilt'siz —
  // `viewModel()` fabrikası yetiyor, DI sırası arka plan işleriyle gelecek.
  implementation(libs.androidx.navigation.compose)
  ```
- **Niye gereksiz:** Kodda **27 adet `@HiltViewModel`** (25 dosyada) ve
  **60 `hiltViewModel()`** çağrısı var; çıplak `viewModel()` çağrısı **sıfır**.
  Yorum, projenin en temel DI kararının tersini söylüyor ve bağımlılık listesini
  okuyan biri "burada Hilt yok" sanıyor. Prompt §4'ün "en tehlikelisi bu" dediği
  sınıf: kod değişmiş, açıklama eski davranışı anlatıyor.
- **Silinirse ne olur:** İki satır yorum gider; davranış değişmez.
- **Risk:** Yok.
- **Gerekçesi var mı:** Yok — K7 tam tersini kaydediyor.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (27/60/0 sayıldı)

---

### G5-003 — `RiskDeposu.dEfektif` KDoc'u "şimdilik 0" diyor; alan üç turdur gerçek sayıyla doluyor

- **Tür:** yorum (yalan)
- **Konum:** `app/src/main/kotlin/.../data/depo/RiskDeposu.kt:149-154`
- **Ne var:**

  ```kotlin
  /**
   * Alpha eğrisinin `d` girdisi (spec-risk.md §4.5).
   *
   * `canliGun` = biriken özet satırı sayısı. `gecmisGun` kurulumda geriye
   * dönük çekilen gün — 0,6 ağırlıkla sayılıyor, şimdilik 0.
   */
  private suspend fun dEfektif(tarih: LocalDate): Int = Alpha.dEfektif(
      GecmisPencere(
          canliGun = ozetDao.gunSayisiEKadar(tarih),
          gecmisGun = tercihler.anlik().gecmisGunSayisi,
      ),
  )
  ```
- **Niye gereksiz:** `GecmisVeriDeposu.kt:17` kendi KDoc'unda "`Tercihler.gecmisGunSayisi`'nin
  **gerçek üreticisi**" diyor ve izin onaylandıktan sonra alanı gerçek gün sayısıyla
  dolduruyor (CLAUDE.md §11 "Biten" tablosunda kayıtlı). Yani `gecmisGun` artık 0
  değil. Bu KDoc, risk skorunun **alpha ağırlığını** okuyan birini yanlış yönlendiriyor
  — üstelik A2/A3 kalibrasyonu tam da bu girdinin üzerinde duracak.
- **Silinirse ne olur:** Tek cümle düzeltilir (", şimdilik 0" atılır). Davranış değişmez.
- **Risk:** Yok.
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-004 — İki bildirim türü hiç üretilmiyor; boş durum metni bunlardan birini kullanıcıya **vaat ediyor**

- **Tür:** ölü kod + UI metni
- **Konum:**
  - `data/depo/BildirimDeposu.kt:74-78` (`ligSirasi`) — **0 çağrı**
  - `data/depo/BildirimDeposu.kt:86-90` (`degerlendirmeYenilenebilir`) — **0 çağrı**
  - `domain/bildirim/BildirimTuru.kt:22` (`LIG`), `:31` (`DEGERLENDIRME`)
  - `ui/ekran/bildirim/BildirimEkran.kt:122-124` (boş durum metni)
- **Ne var:** Sekiz `BildirimTuru` değerinden altısı gerçekten yazılıyor
  (`PUAN`, `HEDEF`, `SERI`, `MOLA`, `HATIRLATMA`, `EBEVEYN`=geriye dönük okuma).
  `LIG` ve `DEGERLENDIRME`'yi yazan iki fonksiyonun **hiçbir çağıranı yok**
  (`rg` ile app/src'nin tamamında arandı; testler dahil sıfır).

  Aynı anda bildirim defterinin boş durumu şunu yazıyor — cihazda görüldü
  (`/tmp/tarama-05/ss/19-bildirim.png`):

  > **Henüz bildirim yok**
  > Görev tamamladıkça, hedefini tutturdukça ve **ligde sıran değiştikçe** buraya düşecek.

- **Niye gereksiz:** "Ligde sıran değiştikçe buraya düşecek" cümlesi hiçbir zaman
  gerçekleşmiyor: `ligSirasi()` çağrılmadığı için `LIG` türünde tek bir satır bile
  yazılmıyor. Bu, §4'ün "yapılmayan işi yapıldı gösterme" maddesinin daha yumuşak
  bir hâli — söz verilen ama gelmeyen bir bildirim. `degerlendirmeYenilenebilir`
  ise gereksiz çünkü aynı işi **ana ekrandaki kart** zaten yapıyor
  (`AnaEkran.kt:921` "4 hafta dolduğunda beliren değerlendirme hatırlatması").
- **Silinirse ne olur:** İki fonksiyon (10 satır), iki enum değeri (+KDoc'ları,
  6 satır) ve boş durum cümlesinin üçüncü kolu gider. **Alternatif:** ya iki
  yazıcı bağlanır (`LigViewModel` sıra değişiminde, `AnaViewModel` 4 hafta
  dolunca), ya da fonksiyonlar+enumlar silinip metin
  "Görev tamamladıkça ve hedefini tutturdukça buraya düşecek." olur.
- **Risk:** `BildirimTuru.anahtardan()` veritabanındaki eski `lig`/`degerlendirme`
  satırlarını okuyabiliyor; enum değerini silmeden önce o satırların var olup
  olmadığına bakmak gerek (üretilmedikleri için büyük olasılıkla yok).
- **Gerekçesi var mı:** Yok. `EBEVEYN` değerinin niçin **korunduğu** KDoc'ta
  yazılı (DBG-064 geriye dönük okuma); bu ikisinde öyle bir not yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-005 — "Örnek kişiler" bölümü koşulsuz çiziliyor; release'de başlık + açıklama boş bir listenin üstünde kalıyor

- **Tür:** ölü UI + sürtünme
- **Konum:** `ui/ekran/lig/ArkadasEkleEkran.kt:141-160` · besleyen
  `data/depo/RetrofitLigDeposu.kt:91` (`= emptyList()`) ve
  `data/depo/YerelSahteLigDeposu.kt:157-170` (üç sabit isim)
- **Ne var:** Cihazda (`/tmp/tarama-05/ss/21-arkadas-alt.png`), ekranın alt yarısı:

  > [Kişilerden bul] *(devre dışı, gri)*
  > Rehberine erişmiyoruz; kişi listesi hiçbir zaman okunmuyor. Davet kodunu paylaşman yeterli.
  > **ÖRNEK KİŞİLER**
  > Bu liste örnektir; kişi önerisi için gereken sosyal bağlantı bilgisini toplamıyoruz.
  > E · Ece Kılıç · Örnek kişi · **Ekle**
  > M · Mert Aydın · Örnek kişi · **Ekle**
  > S · Selin Öz · Örnek kişi · **Ekle**

- **Niye gereksiz:** İki ayrı sorun var ve ikincisi ölçüm.
  1. Bölüm başlığı ve açıklama paragrafı **`if` içinde değil** — `durum.onerilenler`
     boşken bile çiziliyor. `RetrofitLigDeposu.onerilenler()` **her zaman**
     `emptyList()` döndürdüğü için, sunucuya bağlı bir kullanıcı (yani release'deki
     **her** kullanıcı) "ÖRNEK KİŞİLER" başlığını ve altında "Bu liste örnektir…"
     açıklamasını görüyor, ama **liste yok**. Kendini açıklamayan boş bir bölüm.
  2. Debug'da görünen üç isim uydurma; "Ekle" düğmesi gerçek bir davet akışını
     tetikliyor (`viewModel.davetGonder(kisi.isim)`) ve o davet hiçbir zaman
     birine ulaşmıyor.

  Kişi önerisi diye bir özellik **yok** ve olmayacak (K14, §4 rehber yasağı). Bu
  bölüm var olmayan bir özelliğin yerini tutuyor, ve var olmadığını söyleyebilmek
  için iki ayrı açıklama paragrafı gerekiyor.
- **Silinirse ne olur:** `ArkadasEkleEkran`'dan ~35 satır (başlık, iki paragraf,
  `OnerilenSatiri` bileşeni), `LigDeposu.onerilenler()` arayüz metodu, üç
  uygulaması, `OnerilenKisi` data class'ı, `LigDurum.onerilenler` alanı,
  `YedekliLigDeposu`/`SunucuLigDeposu` delegasyonu ve `YerelSahteLigDeposu`'ndaki
  üç sabit isim gider. Kabaca **90–110 satır, 7 dosya**. "Kişilerden bul" devre
  dışı düğmesi ve onun açıklaması da aynı kararla birlikte gözden geçirilebilir
  (o teslim edilen tasarımda var — bkz. §4).
- **Risk:** Düşük. Ekranın gerçek işi (davet kodu üretme/kopyalama/paylaşma ve
  kod girme) bu bölümden bağımsız.
- **Gerekçesi var mı:** **K14** rehberin okunmamasını gerekçelendiriyor ama
  "örnek kişiler listesi" kararı hiçbir K maddesinde yok; `debug.md` O-13 yalnızca
  **başlığın adını** düzeltmiş ("Önerilen" → "Örnek"), bölümün var olup olmaması
  tartışılmamış. CLAUDE.md §11 bunu "hâlâ bilinçli sahte olanlar" arasında sayıyor.
- **Bu görüş mü, ölçüm mü:** (1) **ölçüm** (koşulsuz çizim + `emptyList()`),
  (2) **görüş** (bölümün tamamen kaldırılması)

---

### G5-006 — Aynı işi yapan üç süre biçimleyici, üç farklı çıktı üretiyor

- **Tür:** çift (üçlü) uygulama
- **Konum:**
  - `ui/Bicim.kt:38` — `fun Int.sureMetni()` *(genel, 13 kullanım)*
  - `data/depo/BildirimDeposu.kt:124` — `private fun sureMetni(dakika: Int)`
  - `ui/ekran/ebeveyn/EbeveynViewModel.kt:171` — `private fun sureMetni(dakika: Int)`
- **Ne var:** Üçü de "dakika → saat+dakika metni" yapıyor, üçü de farklı sonuç veriyor:

  | girdi | `Bicim.sureMetni` | `BildirimDeposu.sureMetni` | `EbeveynViewModel.sureMetni` |
  |---|---|---|---|
  | 45 | `45 dk` | `45dk` | `0s 45dk` |
  | 120 | `2s` | `2s 0dk` | `2s 0dk` |
  | 285 | `4s 45dk` | `4s 45dk` | `4s 45dk` |

  (Dördüncü bir biçim daha var — `ui/ekran/hedef/HedefEkran.kt:340 uzunSure()`
  "4 saat 45 dakika" — ama o **bilinçli olarak uzun biçim**; bu maddeye dahil değil.)
- **Niye gereksiz:** Aynı kavramın üç ayrı gerçeklemesi. Kullanıcı 45 dakikalık
  bir değeri bildirimde `45dk`, ekranda `45 dk`, ebeveyn panelinde `0s 45dk`
  olarak görüyor. §7 "Sabitler tek yerde" ilkesinin biçimleyici karşılığı yok
  ve bu boşluk üç kopya üretmiş.
- **Silinirse ne olur:** İki private kopya (8 satır) silinir, `Bicim.sureMetni()`
  import edilir. `EbeveynViewModel`'in "her zaman `Xs Ydk`" davranışı değişir —
  bu **kullanıcıya görünen** bir değişiklik, o yüzden karar gerekiyor (tek satırlık
  bir parametre `sifirDakikayiGoster: Boolean` de çözüm olabilir).
- **Risk:** Düşük ama sıfır değil: `EbeveynViewModel`'in KDoc'u (`/** 240 → 4s 0dk. */`)
  sabit genişlikli hizalama için yazılmış olabilir; ebeveyn panelinin tasarımına
  bakılmalı.
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (üç uygulama, farklı çıktı doğrulandı)

---

### G5-007 — `mm:ss` sayaç metni üç kez yazılmış; ikisi karakteri karakterine aynı

- **Tür:** çift (üçlü) uygulama
- **Konum:**
  - `ui/ekran/odak/OdakHesapla.kt:75-76` — `"%d:%02d".format(kalanSaniye / 60, kalanSaniye % 60)`
  - `ui/ekran/yuruyus/YuruyusHesapla.kt:50-51` — `"%d:%02d".format(saniye / SANIYE_DAKIKA, saniye % SANIYE_DAKIKA)`
  - `ui/ekran/nefes/NefesEkran.kt:348-352` — aynı hesap + `" kaldı"` eki
- **Ne var:** İlk ikisi **birebir aynı ifade**, yalnızca sabit adı farklı
  (`60` ve `SANIYE_DAKIKA = 60`). Üçüncüsü aynı hesabı yapıp sonuna metin ekliyor.
  Her birinin ayrı testi de var (`OdakHesaplaTest`, `YuruyusHesapla` testleri).
- **Niye gereksiz:** Bir satırlık saf bir dönüşümün üç kopyası ve iki ayrı test kümesi.
  `ui/Bicim.kt` tam olarak bu iş için var ve zaten altı biçimleyici taşıyor.
- **Silinirse ne olur:** ~8 satır kod + birkaç test senaryosu birleşir. Davranış aynı.
- **Risk:** Yok.
- **Gerekçesi var mı:** Yok. (`OdakHesapla`/`YuruyusHesapla`'nın **saf sınıf** olması
  gerekçeli — ama `Bicim.kt` de saf; `androidx.compose.ui.text` importu yalnızca
  `kalinIsle` için, biçimleyiciler bağımsız.)
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-008 — Bant kısa etiketi iki yerde, birebir aynı `when`

- **Tür:** çift uygulama
- **Konum:**
  - `ui/bilesen/RiskGirisKarti.kt:83-87` — `fun Bant.kisaEtiket()` *(public)*
  - `ui/ekran/riskdetay/RiskDetayViewModel.kt:124-128` — `private fun bantKisaEtiket(bant: Bant)`
- **Ne var:** İkisi de `AZ → "Düşük"`, `ORTA → "Orta"`, `COK → "Yüksek"`.
  Public olanının KDoc'unda gerekçe yazılı (`Bant.etiket` "ORTA RİSKLİ" teşhis
  gibi duruyor, §4); private olanında hiçbir açıklama yok.
- **Niye gereksiz:** `RiskDetayViewModel` zaten `core-risk`'in `Bant`'ını
  kullanıyor; var olan public uzantıyı çağırması yeterli. İki kopya, §4'ün dil
  kuralını iki ayrı yerde yeniden uygulamak demek — biri değişirse ekranlar
  birbirinden ayrışır.
- **Silinirse ne olur:** 5 satır + import. Davranış aynı.
- **Risk:** Yok. (Not: `kisaEtiket()`'in `ui/bilesen/RiskGirisKarti.kt` içinde
  yaşaması ayrı bir konu — bir bileşen dosyası, alan-etiketi eşlemesi için
  alışılmadık bir ev.)
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-009 — YEDAM 115 çalışma saatleri dört yerde sabit yazılı, arama kodu üç yerde kopya

- **Tür:** çift (dörtlü) uygulama
- **Konum:**
  - `ui/ekran/profil/ProfilEkran.kt:1327-1328` + `:1334-1345` (arama)
  - `ui/ekran/yedam/YedamOnayEkran.kt:109-110` + `:116-120` (arama)
  - `ui/ekran/ebeveyn/EbeveynEkran.kt:726-727` + `:733-745` (arama)
  - `assets/bilgi_bankasi.json:196` (metin içinde)
- **Ne var:** `"Çalışma saatleri: hafta içi 08:30–00:00, cumartesi 08:30–17:30."`
  cümlesi dört ayrı yerde; `Intent(ACTION_DIAL, Uri.parse("tel:115"))` bloğu
  (yorumuyla birlikte ~10 satır) üç ayrı yerde.
- **Niye gereksiz:** YEDAM açıklamasının **varlığı** gerekli (§4, K37 — bu
  raporun §6'sında da öyle listeleniyor). Sorun **kopya sayısı**: Yeşilay hat
  saatlerini değiştirdiği gün dört dosya birden düzeltilmek zorunda ve üçünü
  düzeltip birini unutmak sessizce yanlış bilgi bırakır — bir destek hattı için
  bunun bedeli yüksek.
- **Silinirse ne olur:** Hiçbir şey silinmiyor; **tek yere taşınıyor**. Küçük bir
  `ui/bilesen/YedamKarti.kt` (ya da `domain/yedam/Yedam.kt` sabitleri) üç ekranı
  da besler: ~30 satır tekrar → ~25 satırlık tek bileşen.
- **Risk:** Düşük; üç ekranın yerleşimi farklı olduğu için bileşen yalnızca metin
  + arama eylemini paylaşmalı, düzeni değil.
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (4 kopya sayıldı)

---

### G5-010 — Beş tema tokenı hiçbir yerden okunmuyor; biri "gradyan" adı altında tek renk

- **Tür:** ölü kod
- **Konum:**
  - `ui/tema/Boyut.kt:16` — `Bosluk.Xxl = 48.dp` — **0 kullanım**
  - `ui/tema/Boyut.kt:46` — `Olcu.GirdiKenarligi = 1.5.dp` — **0 kullanım**
  - `ui/tema/Boyut.kt:49` — `Olcu.GorevSeridi = 4.dp` — **0 kullanım**
  - `ui/tema/Boyut.kt:143` — `Golge.BasiliOlcek = 0.98f` — **0 kullanım**
  - `ui/tema/Gradyan.kt:16-18` — `Gradyan.Baslik` — **0 kullanım**
- **Ne var:** `Gradyan.Baslik` ayrıca kendi içinde tuhaf:

  ```kotlin
  /** Ana sayfa / İstatistik / Lig / Profil yeşil başlığı. */
  val Baslik: Brush = Brush.verticalGradient(
      listOf(YesilAna, YesilAna),
  )
  ```

  Aynı rengin kendisine geçtiği bir "gradyan" — yani düz renk, `Brush` kılığında.
  Ve KDoc'unun saydığı dört ekranın hiçbiri onu kullanmıyor; `AnaEkran.kt:153`
  `Gradyan.BaslikSonumu`'nu kullanıyor.
- **Niye gereksiz:** Beşi de sıfır kullanımda. `Olcu.GirdiKenarligi` ayrıca
  `Olcu.ButonKenarligi` ile **aynı değeri** (1.5.dp) taşıyor, yani var olsaydı
  bile ikinci bir isim olurdu.
- **Silinirse ne olur:** ~14 satır (KDoc'lar dahil). Davranış değişmez.
- **Risk:** Yok — `rg` ile app/src + res + manifest + Gradle'da arandı.
- **Gerekçesi var mı:** Yok. (Not: `docs/tarama-04.md §7`, "Sabitler, PuanSabitleri,
  NudgeSabitleri üyelerinin hepsi kullanılıyor" diyor — **doğru**, ama `ui/tema`
  tokenları o taramada sayılmamış.)
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-011 — `Int.kisaSaat()` K49'da yerine geçen fonksiyon yazıldığında silinmemiş

- **Tür:** ölü kod
- **Konum:** `ui/Bicim.kt:48-49`
- **Ne var:**

  ```kotlin
  /** Grafik ekseni için kısa süre: `480` → `8s`. */
  fun Int.kisaSaat(): String = "${this / 60}s"
  ```

  Hemen altında halefi duruyor ve KDoc'u niçin geldiğini yazıyor:

  ```kotlin
  /**
   * Grafik ekseni için süre — bir saatin ALTINI da gösterir.
   * [kisaSaat] tam bölme yaptığı için 8 dakikayı "0s" yazıyordu; …
   */
  fun Int.eksenSuresi(): String = …
  ```
- **Niye gereksiz:** `kisaSaat` **0 kullanımda** (`eksenSuresi` 2, `kisaBin` 2).
  K49 onu değiştirdi ama silmedi; şimdi kod tabanında, KDoc'u kendi hatasını
  anlatan, çağrılmayan bir fonksiyon duruyor. Yeni bir eksen yazan biri adına
  bakıp yanlışlıkla bunu seçebilir — hatanın geri gelme yolu açık.
- **Silinirse ne olur:** 2 satır. `eksenSuresi`'nin KDoc'undaki `[kisaSaat]`
  bağlantısı düz metne çevrilir.
- **Risk:** Yok.
- **Gerekçesi var mı:** **K49** halefini gerekçelendiriyor, selefin kalmasını değil.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-012 — `TemaOnizleme.kt`: 243 satırlık geliştirici galerisi, gezinme grafiğinde girişi yok, `src/main`'de

- **Tür:** ulaşılamayan kod
- **Konum:** `ui/tema/TemaOnizleme.kt` (243 satır)
- **Ne var:** Dosyanın kendi KDoc'u durumu doğru anlatıyor:

  > Gerçek ana ekran gelince MainActivity buradan oraya bağlanır;
  > bu dosya geliştirici aracı olarak kalır.

  `TemaOnizleme()` composable'ının **tek** çağıranı aynı dosyadaki
  `@Preview`'ı (`TemaOnizlemeOnizleme`). `Rota`'da karşılığı yok, `NavHost`'a
  kayıtlı değil, hiçbir ekrandan açılmıyor.
- **Niye gereksiz:** Amacı meşru (canlı stil kılavuzu) ama **yeri yanlış**:
  `src/main`'de olduğu için release kaynak ağacına giriyor. `AdimTaniAlicisi` /
  `SaglikTaniAlicisi` / `DemoVeriModulu` için kurulmuş desen tam olarak bunun
  için var — `app/src/debug/`. Ayrıca Android Studio'nun `@Preview` paneli bu
  dosyanın tamamı olmadan da çalışır.
- **Silinirse / taşınırsa ne olur:** `src/debug`'a taşınırsa 243 satır release
  kaynak ağacından çıkar (R8 zaten atıyor, yani **APK boyutu değişmez** — kazanç
  boyutta değil, "bu dosya nedir, ekranda nerede?" sorusunun ortadan kalkmasında).
- **Risk:** Yok — hiçbir yerden çağrılmıyor.
- **Gerekçesi var mı:** KDoc gerekçelendiriyor ama gerekçe "ana ekran gelene
  kadar" diyor; ana ekran geleli aylar oldu.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (ulaşılamazlık), yer değişimi **görüş**

---

### G5-013 — `res/values/colors.xml`'deki beş renkten üçü hiçbir yerden okunmuyor; dosyanın kendi yorumu bunu yanlış anlatıyor

- **Tür:** ölü kaynak + yorum
- **Konum:** `app/src/main/res/values/colors.xml`
- **Ne var:**

  ```xml
  <!--
    XML tarafı SADECE launcher ikonu ve açılış teması için gereken renkleri taşır.
    Uygulama içi her renk Compose tarafında: ui/tema/Renk.kt (CLAUDE.md §14).
    Buradaki değerler oradakilerle aynı olmak zorunda.
  -->
  <color name="yesil_ana">#FF00AE42</color>   ← 0 kullanım
  <color name="yesil_koyu">#FF124734</color>  ← 0 kullanım
  <color name="yesil_50">#FFEDF7EF</color>    ← themes.xml ×6
  <color name="beyaz">#FFFFFFFF</color>       ← 0 kullanım
  <color name="logo_zemini">#FFE7F7EA</color> ← ic_launcher.xml ×1
  ```
- **Niye gereksiz:** Yorum "SADECE … gereken renkleri taşır" diyor; gerçekte
  beşten **üçü** hiçbir şey için gerekmiyor. Bu renkler `Renk.kt` ile senkron
  tutulmak zorunda diye de yazılmış, yani bakım yükü var ve karşılığı yok.
  Ayrıca `CLAUDE.md §14` diye bir bölüm **yok** (CLAUDE.md §1–§12); kastedilen
  §7 "Composable içinde hardcoded renk/dp".
- **Silinirse ne olur:** 3 satır + yorumun düzeltilmesi.
- **Risk:** Yok (`@color/` ve `R.color.` ile tüm `res/`, manifest ve Kotlin arandı).
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-014 — "CLAUDE.md §14" iki yerde geçiyor; öyle bir bölüm yok

- **Tür:** yorum (yanlış referans)
- **Konum:** `res/values/colors.xml:4` · `ui/tema/Tema.kt:57`
- **Ne var:** `Tema.kt:57` — "ama ekran içinde hardcoded yazmak da §14 yasağı."
- **Niye gereksiz:** CLAUDE.md 12 bölüm. Okuyucu §14'ü arayıp bulamıyor;
  kural §7 "Yasaklar"da.
- **Silinirse ne olur:** İki karakter (`14` → `7`).
- **Risk:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-015 — `VeriYonetimiDeposu` KDoc tablosu "Room (12 tablo)" diyor; şema 15 tablo

- **Tür:** yorum (eskimiş sayı)
- **Konum:** `data/depo/VeriYonetimiDeposu.kt:22`
- **Ne var:** `| Room (12 tablo) | ✓ | ✓ |` — oysa `YesilizDb` **15 entity**
  taşıyor (sürüm 9; `@Entity` sayımı ve `entities = [...]` listesi doğruladı).
- **Niye gereksiz:** "Verilerimi sil" akışının **neyi sildiğini** anlatan tabloda
  yanlış sayı, en kötü yerdeki yanlış sayı: KVKK yüzeyi. `clearAllTables()` zaten
  hepsini siliyor, yani davranış doğru — yalnızca belge yanlış.
- **Silinirse ne olur:** Bir sayı düzelir. Daha iyisi: sayıyı hiç yazmamak
  ("Room (tüm tablolar)") — bir sonraki entity eklendiğinde yine eskimesin.
- **Risk:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-016 — Beş DAO/depo üyesi ve bir data class alanı hiç okunmuyor; biri var olan bir sorgunun `Flow` ikizi

- **Tür:** ölü kod
- **Konum ve ölçüm:**

  | Üye | Konum | Kullanım | Not |
  |---|---|---|---|
  | `GunlukOzetDao.sonGunlerAkisi(gun)` | `dao/Daolar.kt:67-68` | **0** | `sonGunler(gun)` (satır 64-65) ile **birebir aynı SQL** — yalnızca `Flow` sarmalı |
  | `ArkadasDao.sayiAkisi()` | `dao/Daolar.kt:403-404` | **0** | |
  | `BeyanDeposu.uykuAkisi(tarih)` | `depo/BeyanDeposu.kt:36` | **0** | Uyku beyanı `AnaViewModel`'de başka yoldan okunuyor |
  | `JetonDeposu.erisimAkisi` | `uzak/JetonDeposu.kt:35` | **0** | Kimliği akış olarak dinleyen kimse yok |
  | `WidgetEylemleri.widgetSayisi(baglam)` | `widget/WidgetEylemleri.kt:87-95` | **0** | KDoc'u "**teşhis ve testler için**" diyor — hiçbir test çağırmıyor (§4 "yalan söyleyen yorum") |
  | `ProfilAvatarSecenegi.ikonRenk` | `bilesen/ProfilAvatar.kt:29` | **0** | Alan tanımlı ve varsayılanı var, hiçbir çizim onu okumuyor |

- **Niye gereksiz:** Altısı da 0 kullanımda. `sonGunlerAkisi` özellikle dikkat
  çekiyor çünkü `docs/tarama-04.md` T4-008'de adı konmuş desenin aynısı: aynı
  sorunun iki DAO fonksiyonu, biri kullanılıyor, biri değil.
- **Silinirse ne olur:** ~20 satır, 6 dosya. Davranış değişmez.
- **Risk:** Düşük. Room DAO metotları kaynak üretimiyle bağlanıyor ama
  **çağrılmayan** bir `@Query` yalnızca üretilen sınıfta ölü bir metot bırakır.
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-017 — `NudgeDurum` data class'ı hiç örneklenmiyor

- **Tür:** ölü kod
- **Konum:** `nudge/NudgeDurum.kt:16-19`
- **Ne var:**

  ```kotlin
  data class NudgeDurum(
      val asama: NudgeAsama = NudgeAsama.ANA_KART,
      val esikSaniye: Long = NudgeSabitleri.ESIK.seconds,
  )
  ```
- **Niye gereksiz:** Aynı dosyadaki `NudgeAsama` enum'u kullanılıyor
  (`NudgeOverlay.kt:80,140,142,145`) ama `NudgeDurum`'un **tek geçtiği yer kendi
  tanımı**. Overlay durumu `remember { mutableStateOf(NudgeAsama.ANA_KART) }`
  ile, eşik ise `esikSaniye = ayar.esik.seconds` parametresiyle taşınıyor
  (`NudgeOverlayService.kt:262`). Dosyanın adı bile artık içeriğini yanlış
  anlatıyor: içinde `NudgeDurum` yok, `NudgeAsama` var.
- **Silinirse ne olur:** 4 satır. Dosya `NudgeAsama.kt` olur.
- **Risk:** Yok.
- **Gerekçesi var mı:** Yok. CLAUDE.md §6 ağacı `NudgeDurum.kt`'yi listeliyor —
  dosya kalırsa şema doğru, sınıf silinirse ad güncellenmeli.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-018 — `Rota.HESAP` sabiti: 0 kayıt, 0 gezinme

- **Tür:** ölü kod
- **Konum:** `ui/gezinme/Rota.kt:113`
- **Ne var:** `// --- hesap (akış dışı) ---` başlığının altında `const val HESAP = "hesap"`.
  Tüm 44 rota sabiti için `composable(Rota.X)` kaydı ve `navigate(Rota.X)` çağrısı
  sayıldı; **`HESAP` tek 0/0 olan**. `GIRIS`, `KAYIT`, `SIFRE` üçü de kayıtlı ve
  ulaşılabilir.
- **Silinirse ne olur:** 1 satır.
- **Risk:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-019 — `yedam_iki_figur.png` (770 KB) hiçbir yerden referanslanmıyor

- **Tür:** ölü varlık
- **Konum:** `app/src/main/res/drawable-nodpi/yedam_iki_figur.png` — 768×768 RGBA, **770 KB**
- **Ne var:** 13 `drawable-nodpi` PNG'sinin tamamı için Kotlin/XML referansı
  arandı. Onikisi kullanılıyor; bu dosyaya yalnızca `tools/agac_gorselleri_hazirla.py`
  (üreticisi) ve `docs/tasarim-referans.md` (tasarım kaydı) değiniyor.
  `ui/ekran/yedam/YedamOnayEkran.kt` illüstrasyon değil `Icons.Filled.Lock`
  kullanıyor.
- **Silinirse ne olur:** Depo 770 KB küçülür. Release APK **değişmez** (kaynak
  küçültme zaten atıyor, K34); debug APK küçülür.
- **Risk:** Yok — ama görsel teslim edilen tasarımın parçası. Silmek yerine
  `docs/tasarim/` altına taşımak da bir seçenek.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### G5-020 — `giris_guvenlik_banner.png` 2469×951 / 2,5 MB — ekranda ~360dp genişliğinde çiziliyor

- **Tür:** aşırı boyutlu varlık
- **Konum:** `app/src/main/res/drawable-nodpi/giris_guvenlik_banner.png` →
  `ui/ekran/hesap/GirisEkran.kt`
- **Ne var:** Depodaki 13 PNG'nin 4,7 MB'ının **2,5 MB'ı tek bu dosya**. Ağaç
  evreleri 320×318 (~150 KB) ölçeğinde tutulmuşken bu banner 2469×951.
  `drawable-nodpi` olduğu için Android **hiç ölçeklemeden** yüklüyor: bellekte
  2469 × 951 × 4 ≈ **9,4 MB** bitmap.
- **Niye gereksiz:** Giriş ekranının banner'ı en fazla ekran genişliği kadar
  çiziliyor (bu cihazda 720 px). Görüntünün %90'ı hiç görülmeyen çözünürlük.
- **Silinirse / küçültülürse ne olur:** 1080 px genişliğe indirmek dosyayı
  ~10 kat, bellek ayak izini ~5 kat düşürür. Görsel fark yok.
- **Risk:** Yok; yalnızca yeniden ölçekleme.
- **Gerekçesi var mı:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (boyut), "gerekmiyor" kısmı **görüş**

---

### G5-021 — `tools/` altında iki yetim betik

- **Tür:** ölü konfigürasyon/betik
- **Konum:**
  - `tools/kontrol_baslik.py` (1,6 KB) — hiçbir dokümanda, CI'da veya Gradle'da anılmıyor; **kendisi de var olmayan `kubragorev.md` §C'ye** referans veriyor (G5-001)
  - `tools/tasarim_ekran_kes.py` (4,8 KB) — hiçbir yerden anılmıyor; teslim edilen PNG'yi bir kez kesmek için yazılmış, çıktısı (`docs/tasarim/ekranlar/`) zaten depoda
- **Karşılaştırma:** Diğer beş betiğin hepsi CLAUDE.md/README/Gradle/asset
  tarafından adıyla anılıyor.
- **Silinirse ne olur:** 6,4 KB. Hiçbir üretim akışı etkilenmez.
- **Risk:** Düşük. `tasarim_ekran_kes.py` yeni bir teslim gelirse tekrar
  gerekebilir — silmek yerine kullanımını CLAUDE.md §1'in üreticiler tablosuna
  yazmak da çözüm.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (0 referans)

---

### G5-022 — `Olcu.GrafikEkseni` iki KDoc taşıyor; birincisi ölü

- **Tür:** yorum
- **Konum:** `ui/tema/Boyut.kt:89-97`
- **Ne var:**

  ```kotlin
  /** çubuk grafiğin sağındaki eksen etiketi sütunu */
  /**
   * Grafik eksen sütunu.
   * 28dp idi ve eksen sabit etiketler ("8s") taşıdığı sürece yetiyordu. …
   */
  val GrafikEkseni = 40.dp
  ```
- **Niye gereksiz:** İki ardışık KDoc bloğu; Kotlin yalnızca **ikincisini**
  bağlıyor, ilki derleyici için yorumdan ibaret. K49 yeni bloğu eklerken eskisi
  silinmemiş.
- **Silinirse ne olur:** 1 satır.
- **Risk:** Yok.
- **Bu görüş mü, ölçüm mü:** **ölçüm**

---

### Bölüm 1 toplamı

| Kalem | Dosya | Yaklaşık satır / boyut |
|---|---|---|
| Ölü fonksiyon/alan/sabit (G5-004, 010, 011, 016, 017, 018) | 13 | ~60 satır |
| Ulaşılamayan/yanlış yerdeki dosya (G5-012) | 1 | 243 satır |
| Çift/üçlü uygulama (G5-006, 007, 008, 009) | 9 | ~55 satır (birleştirmeyle ~30 net) |
| Ölü/aşırı boyutlu varlık (G5-019, 020) | 2 | 770 KB + ~2,3 MB |
| Yetim betik (G5-021) | 2 | 6,4 KB |
| Yalan/kırık yorum (G5-001, 002, 003, 013, 014, 015, 022) | 20+ | ~40 satır |
| Koşulsuz çizilen boş bölüm (G5-005) | 7 | ~100 satır |

**Kod tarafında toplam ≈ 500 satır ve ~3 MB varlık.**
Bunun ~250 satırı doğrudan silinebilir (0 kullanım), ~150 satırı birleştirme,
~100 satırı ürün kararı (G5-005).

---

## 2. Tartışılır fazlalık

Bu bölümdeki maddeler ölçülebilir ama **silinmeleri bir karar gerektiriyor.**
Her biri için "kalsın" argümanı da yazılı.

---

### G5-023 — Sahte/yedekli depo katmanının bugünkü maliyeti: 8 dosya, ~1.070 satır, 10 test

- **Tür:** fazladan soyutlama (ölçüm isteği — prompt §3.1 son madde)
- **Ölçüm:**

  | Dosya | Satır | Ne yapıyor |
  |---|---|---|
  | `data/depo/YedekliDepolar.kt` | 214 | debug sarmalayıcısı — ağ hatasında yerel sahteye düşer |
  | `data/depo/SunucuDepolar.kt` | 192 | release sarmalayıcısı — hatayı boş sonuca indirger |
  | `data/depo/YerelSahteLigDeposu.kt` | 172 | sahte lig sıralaması + gerçek arkadaş defteri |
  | `data/depo/YerelSahteHesapDeposu.kt` | 109 | çevrimdışı hesap kolu |
  | `src/debug/di/DepoBaglama.kt` | 39 | debug bağlaması |
  | `src/release/di/DepoBaglama.kt` | 35 | release bağlaması |
  | `src/debug/data/demo/DemoVeriModulu.kt` | 164 | "Örnek veri oluştur" tohumu |
  | `src/release/data/demo/DemoVeriModulu.kt` | 26 | boş uygulama |
  | **Toplam** | **951** | |
  | `src/test/.../YedekliDepolarTest.kt` | 121 | **10 test** |
  | **Genel toplam** | **1.072** | app main+debug+release toplamının **%3,2'si** |

- **Hangi kısmı hâlâ iş görüyor:**
  - `SunucuDepolar.kt` (192) — **yayınlanan sürümün gerçek yolu**. Fazlalık değil.
  - `YedekliDepolar.kt` (214) — demo günü sigortası; K18'in kendisi.
  - `YerelSahteHesapDeposu.kt` (109) — çevrimdışı giriş kolu; K16/K38 ile bağlı,
    K38'de şifre değişimini açıkça reddediyor, yani bakım görüyor.
  - `DemoVeriModulu` (debug, 164) — Profil › Geliştirici'den kullanılıyor.
- **Hangi kısmı geride kalmış:**
  - `YerelSahteLigDeposu.kt`'nin **sıralama** kısmı. Backend 5 Ağustos'tan beri
    canlı, `GET /lig` bu cihazda bugün 200 döndü ve gerçek veriyi getirdi
    (`/tmp/tarama-05/ss/06-lig.png`: "Fidan Ligi · 7 gün kaldı · 1. turan · 49").
    Sınıfın KDoc'u hâlâ gelecek zamanda konuşuyor:

    > Sıralama **bilinçli olarak sahte** (backend Faz 2). … Faz 2'de
    > `RetrofitLigDeposu` bu sınıfın yerine geçiyor; `LigViewModel` ve ekranlar
    > değişmiyor.

    Faz 2 oldu. KDoc'un "geçecek" dediği şey geçti; metin güncellenmemiş.
  - `onerilenler()` kolu (G5-005) — bu sınıftaki üç sabit isim.
- **"Kalsın" argümanı:** K18 gerekçesi hâlâ geçerli — demo günü backend'in ayakta
  olacağı garanti değil ve sahte kol **yalnızca debug'da** bağlı, yani yayınlanan
  sürüme sızmıyor (5 Ağu daraltması). 1.072 satır, sahip olunan riske göre ucuz.
- **Öneri (silme değil):** KDoc'lardaki "Faz 2'de … gelecek" dilini bugünkü
  duruma çevirmek; `onerilenler` kolunu G5-005 ile birlikte düşmek. Sınıfın
  kendisi kalmalı.
- **Bu görüş mü, ölçüm mü:** satır/dosya/test sayıları **ölçüm**, "geride
  kalmış" değerlendirmesi **görüş**

---

### G5-024 — `ProfilEkran.kt` 1.616 satır: projenin en büyük dosyası, en yakın rakibinin 1,6 katı

- **Tür:** fazladan yoğunluk (yapı)
- **Ölçüm:** 1.616 satır. İkinci: `AnaEkran.kt` 978. Üçüncü: `EbeveynEkran.kt` 749.
  Dosya bir ekran + **en az 10 alt sayfa** taşıyor (Profili Düzenle, Toplanan
  veriler, Geçmişi indir, Verilerimi sil, Hesabımı sil, Bildirim saatleri,
  Sunucu adresi, YEDAM hakkında, Yardım, Hakkında).
- **Niye tartışılır:** Cihazda Profil ekranı **5 bölüm / ~20 satır** gösteriyor
  (`/tmp/tarama-05/ss/09..11-profil*.png`). Bir ayar satırını değiştirmek için
  1.616 satırlık dosyayı açmak gerekiyor ve dosya içindeki alt sayfaların yarısı
  Profil'in kendi işiyle ilgisiz (YEDAM, Yardım, Hakkında birer içerik ekranı).
- **"Kalsın" argümanı:** **K11 bu deseni açıkça karara bağladı** — alt sayfa =
  ekran dosyası içinde private composable, ayrı ViewModel yok, "bir daha `gorev/`
  klasörü açılmasın". Dosyayı bölmek K11'i tersine çevirmek olur ve kazanç
  yalnızca okunabilirlik.
- **Öneri:** K11'i bozmadan, saf **içerik** alt sayfaları (YEDAM hakkında, Yardım,
  Hakkında — hiçbiri `ProfilViewModel`'e dokunmuyor) `ui/ekran/profil/icerik/`
  altında ayrı dosyalara alınabilir. Bu, K11'in "ayrı ViewModel açma" yasağını
  ihlal etmiyor.
- **Bu görüş mü, ölçüm mü:** satır sayısı **ölçüm**, bölünmesi **görüş**

---

### G5-025 — `Bicim.kt` altı biçimleyici tutuyor ama üç ekran kendi kopyasını yazmış — soyutlama var, kullanılmıyor

- **Tür:** fazladan soyutlama (ters yön)
- **Ölçüm:** `ui/Bicim.kt` altı fonksiyon: `binlik` (15 kullanım), `sureMetni` (13),
  `puanEtiketi` (4), `kalinIsle` (4), `eksenSuresi` (2), `kisaBin` (2),
  `kisaSaat` (**0** — G5-011). Buna karşılık G5-006 ve G5-007'de sayılan **beş**
  yerel kopya var.
- **Niye tartışılır:** Ortak biçimleyici dosyası **doğru** kurulmuş ve yoğun
  kullanılıyor; sorun soyutlamanın fazlalığı değil, **eksik uygulanması**. Yani
  bu madde "sil" değil "kullan" diyor — prompt §3.1'in tersinden bir bulgu.
- **"Kalsın" argümanı:** Yerel kopyaların ikisi (`OdakHesapla`, `YuruyusHesapla`)
  saf, emülatörsüz test edilebilir sınıflarda duruyor ve `Bicim.kt`
  `androidx.compose.ui.text` import ediyor. Ama bu import yalnızca `kalinIsle`
  için; biçimleyiciler saf Kotlin. İstenirse `kalinIsle` ayrı dosyaya alınabilir.
- **Bu görüş mü, ölçüm mü:** kullanım sayıları **ölçüm**, birleştirme **görüş**

---

### G5-026 — `debug.md` 75 dosyadan, `CLAUDE.md` 113 dosyadan referanslanıyor

- **Tür:** yorum (tekrar)
- **Ölçüm:**

  | Hedef | Referans veren dosya | Toplam satır |
  |---|---|---|
  | `CLAUDE.md` | 113 | 155 |
  | `debug.md` | 75 | 118 |
  | `docs/tasarim-referans.md` | 54 | 60 |
  | `docs/spec-*` | 33 | 38 |
  | `gorev2.md` (**yok**) | 16 | 23 |

- **Niye tartışılır:** Bu, projenin bilinçli izlenebilirlik geleneği: her düzeltme
  hangi bulgudan doğduğunu yazıyor. Ama `debug.md`'nin **tamamı kapatılmış**
  (CLAUDE.md §11: "hepsi düzeltildi… ✅ işaretli") ve 75 dosya kapanmış bir arşive
  işaret ediyor. Yeni gelen biri `debug.md K-7` gördüğünde bunun açık bir kusur
  mu, kapanmış bir kayıt mı olduğunu ayırt edemiyor.
- **"Kalsın" argümanı:** Referanslar tarihsel bağlamı taşıyor ve bu proje tam da
  o bağlam sayesinde aynı tuzağa iki kez düşmüyor (§9). Referansı silmek gerekçeyi
  siler.
- **Öneri:** Silme değil; `debug.md`/`DEBUG_LOG.md`/`docs/tarama-0*.md` dördünü
  `docs/arsiv/` altında toplayıp CLAUDE.md §2'de tek satırla "kapanmış tarama
  kayıtları" demek. Kod referansları olduğu gibi kalır.
- **Bu görüş mü, ölçüm mü:** sayılar **ölçüm**, öneri **görüş**

---

### G5-027 — `FaktorSeviyesi` etiketleri asimetrik: yalnızca "Düşük" olan "risk" kelimesini taşıyor

- **Tür:** UI metni / erken genelleştirme kalıntısı
- **Konum:** `core-risk/.../EtkileyenFaktorler.kt:13-32`
- **Ne var:** `DUSUK("Düşük risk")`, `ORTA("Orta")`, `YUKSEK("Yüksek")`,
  `BILINMIYOR("Veri yok")`. Cihazda risk detayı ekranı bunu üst üste gösteriyor
  (`/tmp/tarama-05/ss/16-riskdetay.png`):

  ```
  Gece kullanımı              Düşük risk
  Kesintisiz kullanım süresi  Düşük risk
  Sosyal medya oranı          Düşük risk
  Fiziksel hareket            Yüksek
  ```
- **Niye tartışılır:** Dört satırın üçü "… risk" der, dördüncüsü demez. Okuyan
  kişi için "Yüksek" neyin yükseği belli değil ve tam da §4'ün kaçınmak istediği
  yerde (yüksek risk satırında) dil zayıflıyor.
- **"Kalsın" argümanı:** `ORTA`/`YUKSEK` etiketlerinin kısa olması yer kısıtından
  olabilir; ayrıca `BILINMIYOR("Veri yok")` bilinçli olarak farklı bir cümle
  yapısında (K30/K39 ailesi).
- **Öneri:** Ya dördü de tek kelime (`Düşük` / `Orta` / `Yüksek` / `Veri yok`),
  ya da üçü de "… risk". İkisinin arası bugünkü hâli.
- **Bu görüş mü, ölçüm mü:** **görüş** (ekran görüntüsüyle desteklenmiş)

---

## 3. Kullanıcıya dokunan gürültü

Her madde için **alternatif** öneriliyor.

---

### G5-028 — Günlük hedef ekranı aynı sayıyı **üç kez, üç ayrı biçimde** gösteriyor

- **Tür:** UI metni / sürtünme
- **Konum:** `ui/ekran/hedef/HedefEkran.kt` — halka `:80` civarı, `AyarSeridi` `:109`, `HizliCip` satırı `:117-129`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/14-hedef-alt.png`
- **Ne var:** Aynı anda ekranda:

  | Yer | Metin |
  |---|---|
  | Halka merkezi | **4s 45dk** |
  | −/+ şeridi | **4 saat 45 dakika** |
  | Hızlı seçim (seçili çip) | **4s 45dk** |

  Aynı desen mola eşiği bloğunda tekrarlanıyor: şerit "**15 dakika**", çip "**15 dk**".
- **Niye gürültü:** Kullanıcı tek bir değeri ayarlıyor; ekran onu üç kez, **iki
  farklı yazım kuralıyla** söylüyor (`sureMetni` "4s 45dk" ve `uzunSure`
  "4 saat 45 dakika" — G5-006'nın ekrandaki karşılığı). Halka ile çip birebir
  aynı dizeyi, şerit üçüncü bir biçimi taşıyor.
- **Üçüncü çip fazlalık DEĞİL:** `HedefSinir.hizliSecim(mevcutHedef)` listeye
  **kayıtlı** hedefi ekliyor (`HedefDurum.kt:62-64`) ve bu teslim edilen
  tasarımın kendisi ("Tasarım 34'teki hızlı seçim çipleri: 3s · 4s · mevcut · 6s").
  −/+ ile oynadıktan sonra kayıtlı değere dönmeyi sağlıyor. Tek eksiği görsel:
  hiçbir şey değiştirilmemişken bu çip "seçili" görünüyor ve **kayıtlı değer**
  olduğu anlaşılmıyor — küçük bir etiket ("kayıtlı") ayrımı çözer.
- **Alternatif:** −/+ şeridi ile halkadan biri değeri yazmasın (şerit yalnızca
  kontrol olsun), ve iki biçim tek biçime insin — G5-006 birleştirilirse bu
  kendiliğinden düzelir.
- **Bu görüş mü, ölçüm mü:** üç tekrar ve iki farklı biçim **ölçüm**, düzen
  önerisi **görüş**

---

### G5-029 — "Değişiklikler yarından itibaren geçerli olur." şeridi mola eşiği için **yanlış**

- **Tür:** UI metni (yanlış iddia)
- **Konum:** `ui/ekran/hedef/HedefEkran.kt:185` (`BilgiSeridi`, `MolaEsigiBolumu`'nun **altında**) · metin `:331-338`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/14-hedef-alt.png` — şerit, mola eşiği
  bloğunun hemen altında, saat ikonuyla.
- **Ne var:** Ekranın en altındaki bilgi şeridi:

  > 🕐 Değişiklikler yarından itibaren geçerli olur.

- **Niye yanlış:** Şerit, **iki** ayarın altında duruyor ve yalnızca birincisi için
  doğru. Mola eşiği **anında** geçerli oluyor: `NudgeOverlayService.kt:128` eşiği
  **her poll turunda** yeniden okuyor (`NudgeAyari.dakikadan(tercihler.nudgeEsikDk.first())`),
  K53 de bunu açıkça kaydediyor ("Servis eşiği her turda okuyor, ayar değişince
  yeniden başlatma gerekmiyor"). Yani kullanıcı 15 dk'yı 45 dk'ya çekiyor, ekran
  "yarın" diyor, sistem hemen uyguluyor. Bu, K37'nin tersi yönde bir hata:
  yapılan işi yapılmadı göstermek.
- **Alternatif:** Şeridi `MolaEsigiBolumu`'nun **üstüne**, günlük hedef bloğunun
  içine taşımak; ya da metni ikiye ayırmak — hedef altında "Değişiklikler yarından
  itibaren geçerli olur.", mola bloğunda "Hemen geçerli olur."
- **Bu görüş mü, ölçüm mü:** **ölçüm** (kod yolu tek anlamlı)

---

### G5-030 — Lig ekranı "Son **0** düşer" yazıyor

- **Tür:** UI metni
- **Konum:** `ui/ekran/lig/LigEkran.kt` (özet şeridi) · veri `LigOzeti.dusen`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/06-lig.png`
- **Ne var:** Tek kişilik ligde şerit şunu gösteriyor:

  > Fidan Ligi · 7 gün kaldı · **İlk 1 yükselir** · **Son 0 düşer**

- **Niye gürültü:** "Son 0 kişi düşer" bir kural değil, kuralın yokluğu. Kullanıcıya
  hiçbir şey söylemiyor ve tek başına olduğu bir ligde "düşme" fikrini gündeme
  getirerek §4'ün ceza/korkutma çizgisine gereksizce yaklaşıyor.
- **Alternatif:** `dusen == 0` iken o çipi hiç çizmemek (aynısı `yukselen == 0`
  için de). Tek satırlık koşul.
- **Bu görüş mü, ölçüm mü:** **ölçüm** (ekranda görüldü), gizlenmesi **görüş**

---

### G5-031 — "Adım kaynağı: Telefon sayacı" — gerekçesi yalnızca kod yorumunda, ekranda değil

- **Tür:** UI metni (mühendislik kavramı)
- **Konum:** `ui/ekran/istatistik/IstatistikEkran.kt:191` · etiket üretimi
  `IstatistikViewModel.kt:240-252`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/05-istatistik-alt.png`
- **Ne var:** Adım grafiğinin altında, ortalanmış üçüncül metin:

  > Adım kaynağı: Telefon sayacı

  Niçin orada olduğu **yalnızca** kaynak yorumunda yazılı
  (`IstatistikEkran.kt:184-188`): "Adım sayısı Fit/Samsung Health'ten %10-20
  farklı çıkıyor ve kullanıcı bunu hata sanıyor… Kaynağı söylemek farkı
  açıklanabilir kılıyor."
- **Niye gürültü:** Gerekçe **iyi** ama kullanıcıya ulaşmıyor. Ekranda kalan şey
  bir iç kavramın adı ("Telefon sayacı" / "Health Connect") ve kullanıcının bununla
  yapabileceği hiçbir şey yok. Yani en pahalı yarısı (açıklama) kodda, en işe
  yaramaz yarısı (etiket) ekranda.
- **Alternatif:** Ya cümleyi tamamlamak — "Adımlar telefonun sensöründen okunuyor;
  Samsung Health gibi uygulamalarla küçük farklar olabilir." — ya da satırı
  kaldırıp aynı açıklamayı bir "ⓘ" dokunuşunun arkasına koymak.
- **Bu görüş mü, ölçüm mü:** **görüş**

---

### G5-032 — Ana ekran başlığında "**0 gün**" seri çipi

- **Tür:** UI metni
- **Konum:** `ui/ekran/ana/AnaEkran.kt` başlık şeridi
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/01-ana.png`
- **Ne var:** Başlıkta, kullanıcı adının yanında alev ikonlu bir çip: **🔥 0 gün**
- **Niye gürültü:** Sıfır serili bir kullanıcıya "serin 0 gün" demek bilgi değil.
  Çip, başlığın en değerli alanını işgal ediyor ve gösterdiği tek şey bir eksiklik.
  §4 "ceza puanı yok / suçluluk üretme" ilkesinin ruhuyla gerilimde: puan
  sisteminde ceza yok ama arayüz sıfırı sürekli hatırlatıyor.
- **Alternatif:** `seri == 0` iken çipi çizmemek; ilk seri gününde belirmesi ayrıca
  küçük bir ödül anı yaratır. (K40 zaten "gün ölçülmediyse seri ne artar ne kırılır"
  diyor, yani 0 çoğu zaman "henüz veri yok" demek — bunu "0 gün" diye söylemek yanlış.)
- **Bu görüş mü, ölçüm mü:** **görüş**

---

### G5-033 — İstatistik boş-gün açıklaması üç cümlede aynı şeyi söylüyor

- **Tür:** UI metni (uzunluk)
- **Konum:** `ui/ekran/istatistik/IstatistikEkran.kt`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/04-istatistik.png`
- **Ne var:**

  > 7 günün 1 tanesi ölçüldü. Ortalama yalnızca o günlerden hesaplandı;
  > ölçülemeyen günler grafikte boş görünüyor.

- **Niye gürültü:** Cümlenin **varlığı gerekli** (K30/K39 — ölçülmemişi ölçülmüş
  gibi göstermemek; bu raporun §6'sında da öyle listeleniyor). Tartışılan
  **uzunluk**: üçüncü cümle ("ölçülemeyen günler grafikte boş görünüyor")
  grafiğin kendisinin zaten gösterdiği şeyi anlatıyor, ikinci cümle birinciden
  çıkarılabilir.
- **Alternatif:** "Son 7 günün 1'i ölçüldü; ortalama yalnızca o günden." — aynı
  dürüstlük, yarı uzunluk.
- **Bu görüş mü, ölçüm mü:** **görüş**

---

### G5-034 — "Mola hatırlatıcı" ayarı iki ekrana bölünmüş; biri Gizlilik başlığı altında

- **Tür:** sürtünme
- **Konum:** açma/kapama `ui/ekran/profil/ProfilEkran.kt` (GİZLİLİK bölümü) ·
  eşik `ui/ekran/hedef/HedefEkran.kt` (Profil › Hedeflerim › Günlük hedef)
- **Ekran görüntüleri:** `/tmp/tarama-05/ss/10-profil-alt.png`, `14-hedef-alt.png`
- **Ne var:** Cihazda Profil › **GİZLİLİK** bölümü şu üç satırı taşıyor:
  "Yeşilay ile paylaşım", "Ligde görün", "**Mola hatırlatıcı**". Eşiği değiştirmek
  için ise Profil › HEDEFLERİM › Günlük hedef'e gitmek gerekiyor — üç dokunuş
  uzakta, ayrı bir bölümün altında. Hedef ekranı bunu kendisi de itiraf ediyor:

  > Mola hatırlatıcı şu anda kapalı. Profil › Gizlilik'teki "Mola hatırlatıcı"
  > anahtarını açtığında bu eşik geçerli olur.

- **Niye gürültü:** Tek bir özelliğin açma anahtarı ile eşiği iki ayrı ekranda,
  iki ayrı kategoride. Ayrıca mola hatırlatıcı bir **gizlilik** ayarı değil:
  cihazdan hiçbir veri çıkarmıyor, bir bildirim tercihi. Yanındaki iki satır
  (Yeşilay paylaşımı, ligde görün) gerçek rıza kapıları; aynı başlık altında
  durması ikisinin ağırlığını da bulanıklaştırıyor.
- **Alternatif:** Anahtarı da eşiğin yanına (Günlük hedef ekranındaki "MOLA
  HATIRLATICI" bölümüne) almak; Profil'deki satır ya kalkar ya oraya kısayol olur.
  Bu, K53'ün "kullanıcının sözü olsun" amacını bozmadan uyarı metnini de
  gereksizleştirir.
- **Bu görüş mü, ölçüm mü:** **görüş** (yerleşim), açıklamanın varlığı **ölçüm**

---

### G5-035 — "Kişilerden bul": devre dışı bir düğme + onu açıklayan bir paragraf

- **Tür:** sürtünme
- **Konum:** `ui/ekran/lig/ArkadasEkleEkran.kt:126-140`
- **Ekran görüntüsü:** `/tmp/tarama-05/ss/21-arkadas-alt.png`
- **Ne var:** Üç eylem kutusundan biri gri ve tıklanamaz ("Kişilerden bul"),
  altında bir paragraf niçin tıklanamadığını anlatıyor.
- **Niye gürültü:** Kullanıcıya **hiçbir zaman** kullanılamayacak bir düğme
  gösteriliyor ve bu düğmenin varlığı bir açıklama gerektiriyor. K14 rehberin
  okunmamasını doğru gerekçelendiriyor; tartışılan şey düğmenin **görünmesi**.
  Devre dışı bir kontrol, kullanıcının "acaba nasıl açılır?" diye aramasına yol
  açar — açılmasının bir yolu yok.
- **"Kalsın" argümanı:** Düğme teslim edilen tasarımda var (§8) ve mahremiyet
  duruşunu **görünür** kılıyor: "bunu yapabilirdik, yapmıyoruz."
- **Alternatif:** Düğmeyi kaldırıp açıklamayı tek satıra indirmek ("Rehberine
  erişmiyoruz; davet kodunu paylaşman yeterli."). Ya da tam tersi: düğmeyi
  bırakıp G5-005'in "Örnek kişiler" bölümünü kaldırmak — ikisini birden tutmak,
  aynı özür iki kez.
- **Bu görüş mü, ölçüm mü:** **görüş**

---

## 4. K kararlarına itiraz

Prompt bu bölümün kısa olmasını istiyor; **iki madde** var.

---

### G5-036 — K50 (nudge eşiğinin dala göre ayrışması) K53'ten sonra ödediği bedeli hak etmiyor

- **Kararın kendi gerekçesi (K50, 14 Ağu):** `main` gerçek kullanıcı değerini
  (45 dk) taşısın, `dev` cihazda doğrulanabilir kalsın (15 sn). Bedeli açıkça
  yazılmış: "`main` artık `dev`'e fast-forward edilemiyor", `TetikleyiciTest`
  ayrışmayı yakalamıyor, `git reset --hard dev` değeri sessizce yutar.
- **Niçin artık tutmuyor:** **K53 (fcd3f18)** eşiği kullanıcı ayarı yaptı
  (`Tercihler.nudgeEsikDk`, 15–90 dk, Profil › Hedeflerim). `NudgeSabitleri.ESIK`
  artık **yalnızca seçim yokken** geçerli olan varsayılan. Yani doğrulama için
  15 saniyeye ihtiyaç kalmadı: geliştirici ekrandan 15 dakikayı seçebiliyor.

  Dahası, K50'nin kendi kabul ettiği yan etkisi K53'ten sonra da duruyor ve artık
  gerekçesiz: `dev`'de `NudgeSabitleri.ESIK = 15 sn` iken
  `bildirimDeposu.molaHatirlatildi(ayar.esik.toMinutes())` **0** alıyor ve
  bildirim defterine "**0 dakikadır** aralıksız ekrandasın" yazıyor
  (`docs/tarama-04.md §8` bunu bilinen sonuç olarak kaydediyor).
- **Maliyeti:** İki dal arasında kalıcı bir merge engeli, testlerin görmediği bir
  ayrışma, ve `dev`'de yanlış metin üreten bir bildirim yolu.
- **Öneri:** İki dalda da 45 dk; doğrulama ekrandaki ayardan yapılsın. Bu, K53'ün
  "sınırlar bilinçli, alt sınır 15 dk" kararıyla da uyumlu — 15 saniye zaten
  kullanıcıya sunulmayan bir değer.
- **Not:** Bu bir itiraz, uygulama değil. `main` bugün 45 dk taşıyor; karar
  yalnızca `dev`'in 15 saniyesini ilgilendiriyor.

---

### G5-037 — K7'nin mock repository deseni vaadini tuttu; ama `LigDeposu.onerilenler()` deseni taşıyamadığı yere kadar götürmüş

- **Kararın kendi gerekçesi (K7):** Arayüz sabit tutulunca backend gelince
  yalnızca DI bağlaması değişir. **Bu gerçekleşti** ve iki kez sınandı — itiraz
  edilen şey karar değil, arayüzün **kapsamı**.
- **Niçin tutmuyor:** `LigDeposu` arayüzünde `suspend fun onerilenler(): List<OnerilenKisi>`
  var. Sunucu bunu **hiç uygulamıyor**: `RetrofitLigDeposu.kt:91` tek satır,
  `= emptyList()`. Yani arayüz, gerçek uygulamasının **hiçbir zaman**
  dolduramayacağı bir metot taşıyor ve o metot yalnızca sahte deponun üç sabit
  ismini UI'a taşımak için var (G5-005). Desenin amacı "backend gelince arayüz
  değişmesin"di; burada backend geldi ve arayüzün bir kolu boşta kaldı.
- **Maliyeti:** Arayüz metodu + 3 uygulama + `OnerilenKisi` + `LigDurum` alanı +
  2 sarmalayıcı delegasyonu + ekran bölümü ≈ 90–110 satır, ve release'de boş bir
  bölüm başlığı (G5-005).
- **Öneri:** `onerilenler()`'i `LigDeposu`'ndan düşürmek. K7 deseni bozulmuyor —
  aksine, arayüz gerçekten sunucunun karşılayabildiği yüzeye eşitleniyor.

---

## 5. Doküman önerileri

Düzenleme yapılmadı; öneri listesi.

### 5.1 Koda uymayan sayı ve yollar (ölçüm)

| Yer | Yazan | Ölçülen | Not |
|---|---|---|---|
| `CLAUDE.md §1` komut bloğu | "testler (toplam **293**)" · core-risk **22** · app **153** · connected **36** · backend **76** | **30 · 332 · 58 · ~116** | Dört sayının dördü de eski. §11 aynı sayıları **doğru** veriyor (537 toplam) — belge kendi içinde çelişiyor |
| `CLAUDE.md §1` üretilen dosyalar | `res/drawable-nodpi/agac_*.png` ← `tools/agac_gorselleri_ayikla.py` | `res/drawable-nodpi/`'de **hiç `agac_*.png` yok**; kullanılan set `downscaled_tree_phase_1..9.png` ve deposunda üreticisi **yok** | Betik gerçekten `agac_<ad>.png` yazıyor (`:107`), yani tablo betiği doğru anlatıyor ama **shipping set başka** |
| `CLAUDE.md §1` üretilen dosyalar | `DB.md` ← `./gradlew :app:assembleDebug` → `db_dokuman_uret.py` | `app/build.gradle.kts`'te böyle bir görev **yok**; yalnızca `room.schemaLocation` ayarı ve bir **yorum** var (`:298-299`) | `DB.md` elle koşturulan bir betikle üretiliyor; tablo bunu otomatikmiş gibi gösteriyor |
| `CLAUDE.md §11` (`app/data/yerel/` satırı) | "`YesilizDb` (sürüm **8**)" · "`GecisTest` **1→2 … 7→8**" · "sürüm **4'ten 8'e**" | sürüm **9** · `GecisTest` **1→9**'un tamamı (`gecis8den9a*` üç test) · kapsamlı test **4'ten 9'a** | §6 aynı belgede "şema sürümü 9" diyor — iç çelişki |
| `CLAUDE.md §2` dosya haritası | `tarama-01`, `debug.md`, `DEBUG_LOG.md`, `DEBUG_STATE.md` listeli | **`docs/tarama-04.md` listede yok** | Dördüncü tarama raporu haritada görünmüyor |

### 5.2 Birleştirme / tek doğruluk kaynağı

- **Tarama kayıtları dörde çıktı** (`docs/tarama-01.md`, `debug.md`, `DEBUG_LOG.md`
  + `DEBUG_STATE.md`, `docs/tarama-04.md`, ve bu rapor beşinci). Hepsi kapalı ya
  da kısmen kapalı. Öneri: `docs/arsiv/` altında toplamak, CLAUDE.md §2'de tek
  satır ("kapanmış tarama kayıtları — tarihsel"), ve **açık kalan** maddeleri
  (DEBUG_LOG'un "yayın öncesi riskler" bölümü) §12'nin **A** tablosuna taşımak.
  Bugün A12–A16 zaten oradan kopyalanmış durumda, yani iki yerde duruyorlar.
- **Test sayıları üç yerde**: §1 komut bloğu, §11 "Biten" tablosunun satırları,
  §11 sonundaki "Toplam 537". Öneri: sayı **yalnızca §11'in son paragrafında**
  yaşasın, §1 komut bloğu sayı yazmasın (komutlar zaten sayıyı basıyor).
- **`DESIGN.md §4` ile K2 çelişiyor** ve K2 bunu kendisi söylüyor ("kod kazanıyor,
  o bölüm tarihsel"). Çelişkiyi belgede bırakmak yerine `DESIGN.md §4`'ün başına
  bir uyarı satırı koymak (ya da bölümü çıkarmak) okuyucuyu K2'yi bulmak zorunda
  bırakmaz.
- **`docs/stitch-ekranlar.md` + `DESIGN.md §5` + `tools/stitch/`** üçü de "yeni
  ekran için kullanma" notuyla duruyor. §8 zaten Stitch'in yalnızca **yerleşim**
  referansı olduğunu söylüyor. Öneri: üçünü de arşive almak.

### 5.3 Kodda düzeltilecek referanslar

- `gorev1.md` / `gorev2.md` / `kubragorev.md` (G5-001) — 35 referans.
- `CLAUDE.md §14` (G5-014) — 2 referans, §7 olmalı.
- `YerelSahteLigDeposu` KDoc'undaki "Faz 2'de … gelecek" dili (G5-023).

---

## 6. Fazlalık DEĞİL — bakıldı ve elendi

Kapsamın kanıtı. Bunlar "gereksiz görünüyor ama gerekli" diye elenen şeyler.

| Şey | Niçin fazlalık değil |
|---|---|
| **KVKK / rıza / aydınlatma metinleri**, `sozlesmeler.json`, "Toplanan veriler" alt sayfası | Yasal zorunluluk; prompt §2 de kapsam dışı bırakıyor |
| **YEDAM'ın "buradan hiçbir veri gönderilmez" açıklaması** | K37'nin ta kendisi. Yalnızca **kopya sayısı** raporlandı (G5-009), varlığı değil |
| **"7 günün 1 tanesi ölçüldü", "Henüz yeterli veri yok", `VeriDurumu.YETERSIZ`** | K30/K39. Yalnızca uzunluğu tartışıldı (G5-033) |
| **Ödül kademelerinin "Planlanan" etiketi** | K28/A4. Cihazda doğrulandı, dokunulmadı |
| **`Bicim.kt`'in var olması** | Altı biçimleyicinin beşi yoğun kullanımda; sorun eksik kullanım (G5-025) |
| **`JetonKaynak` / `JetonYenileyici` arayüzleri** | Tek uygulamalı ama KDoc'ları "niçin arayüz"ü yazıyor: OkHttp `Authenticator` senkron, DataStore emülatörsüz kurulamıyor, saf karar mantığı test edilebilir kalmalı. `OturumCikar`/`NefesFazHesapla`/`Tetikleyici` ile aynı kurulmuş desen |
| **`AdimKaynagi` arayüzü** | Tek uygulamalı ama K33 gerekçesi kayıtlı ve `AdimDeposuTest` fiilen sahte kaynak takıyor |
| **`DemoVeri` arayüzü + debug/release ikizi** | Sahte veri release'e sızmasın diye; K18'in 5 Ağu daraltmasının uygulaması |
| **`Bant.etiket` ("AZ RİSKLİ")** | Ekranda kullanılmıyor (K10/§4 dili `kisaEtiket` veriyor) ama `Bant.etiketten()` referans Python'ın test vektörlerini okuyor — silinemez |
| **`BildirimTuru.EBEVEYN`** | KDoc'u niçin **korunduğunu** yazıyor: eski sürümün sahte kayıtlarını okuyabilmek (DBG-064). `LIG`/`DEGERLENDIRME`'de böyle bir not yok (G5-004) |
| **`AdimTaniAlicisi` / `SaglikTaniAlicisi`** | `rg` 0 çağrı gösteriyor ama `app/src/debug/AndroidManifest.xml`'de `<receiver>` olarak kayıtlı; `am broadcast` ile tetikleniyor (K32) |
| **`Donusturucu.kt`'nin 6 fonksiyonu** | `rg` 1 kullanım gösteriyor; Room `@TypeConverter` olarak **kaynak üretimiyle** çağrılıyor |
| **`Olcu` içindeki 35 ölçüden 30'u** | Tam olarak birer yerden çağrılıyor — bu tek-kullanım değil, **§7'nin kendisi** ("Composable içinde hardcoded dp YASAK"). Sayı yerine ad kullanmak bu projede zaten amaç |
| **`debug.md`/`CLAUDE.md` geri referansları** | İzlenebilirlik geleneği; yalnızca arşivleme önerildi (G5-026) |
| **`ekransiz` / `gecekapanisi` / `gozmolasi` ekranları** | Küçük ama hepsi `Rota`'da kayıtlı, Aktiviteler sekmesinden ulaşılabilir; cihazda görüldü |
| **`proguard-rules.pro`'nun boş olması** | K34'te gerekçelendirilmiş ve `dogrulaReleaseApk` ile denetleniyor |
| **Hedef ekranındaki "mevcut" hızlı seçim çipi** | İlk bakışta işlevsiz görünüyor ama `HedefSinir.hizliSecim` kayıtlı hedefi listeye ekliyor: −/+ ile oynadıktan sonra geri dönüş yolu. Teslim edilen tasarım 34'ün kendisi (G5-028) |
| **`HedefEkran.uzunSure()`** | `Bicim.sureMetni()` ile aynı işi yapmıyor — bilinçli **uzun** biçim ("4 saat 45 dakika"), G5-006'ya dahil edilmedi |

---

## 7. Test edilemedi / kapsam dışı

| Konu | Neden |
|---|---|
| **Nudge overlay metni ve sıklığı** (prompt §5 son madde) | Tetiklemek 15 dk kesintisiz tüketim kullanımı ve cihazda gerçek veri üretmek demekti; ayrıca `dev`/`fix` dalının eşiği ile `main`'inki farklı (K50) — bu turda ölçüm cihazın gerçek verisini kirletmeden yapılamazdı |
| **Ödül seçim akışı** | Hesap 52 puanda; en düşük maddi kademe 2.000 (K28 doğrulandı, kademeler kilitli) |
| **YEDAM onay ekranı** | Yalnızca `Bant.COK`'ta açılıyor; risk detayındaki öneri kartı görüldü ama ekranın kendisi açılmadı (açmak `YedamOnay` akışına girmek demekti) |
| **Onboarding ekranları** | Kurulu uygulamada onboarding tamamlanmış; yeniden görmek `pm clear` gerektirirdi (yasak) |
| **Ebeveyn paneli** | Eşleşme yok; sahte kod denemek K25'in reddettiği yol |
| **Release derlemesinde "Örnek kişiler" bölümünün boş hâli** (G5-005 madde 1) | Kod yolu kesin (`RetrofitLigDeposu.kt:91` → `emptyList()`, başlık koşulsuz) ama release APK üretilip kurulmadı — **ekran görüntüsüyle teyit edilmedi** |
| **`connectedDebugAndroidTest`** | Cihazdaki uygulama verisini değiştirme riski (`GecisTest` veritabanı kuruyor) |

---

## 8. Özet tablo

| ID | Tür | Tek satır | Ölçüm/görüş | Yaklaşık kazanç |
|---|---|---|---|---|
| **G5-001** | doküman | `gorev1/gorev2/kubragorev.md` depoda yok, 35 referans var | ölçüm | ~23 yorum satırı, 16 dosya |
| **G5-002** | yorum | `build.gradle.kts` "ViewModel'ler henüz Hilt'siz" — 27 `@HiltViewModel` var | ölçüm | 2 satır |
| **G5-003** | yorum | `RiskDeposu.dEfektif` KDoc "gecmisGun şimdilik 0" — artık dolu | ölçüm | 1 cümle |
| **G5-004** | ölü kod + UI | `ligSirasi`/`degerlendirmeYenilenebilir` 0 çağrı; boş durum metni lig bildirimi vaat ediyor | ölçüm | ~16 satır + metin |
| **G5-005** | ölü UI | "Örnek kişiler" başlığı koşulsuz; release'de boş bölüm | ölçüm/görüş | ~100 satır, 7 dosya |
| **G5-006** | çift uygulama | 3 `sureMetni`, 3 farklı çıktı | ölçüm | ~8 satır |
| **G5-007** | çift uygulama | 3 `mm:ss` biçimleyici, ikisi birebir aynı | ölçüm | ~8 satır |
| **G5-008** | çift uygulama | Bant kısa etiketi 2 kez, aynı `when` | ölçüm | 5 satır |
| **G5-009** | çift uygulama | YEDAM saatleri 4 kopya, arama kodu 3 kopya | ölçüm | ~30 → ~25 satır |
| **G5-010** | ölü kod | 5 tema tokenı 0 kullanım; `Gradyan.Baslik` tek renkli "gradyan" | ölçüm | ~14 satır |
| **G5-011** | ölü kod | `Int.kisaSaat()` K49'da halefi gelince silinmemiş | ölçüm | 2 satır |
| **G5-012** | ulaşılamaz | `TemaOnizleme.kt` 243 satır, `src/main`'de, girişi yok | ölçüm | 243 satır (taşıma) |
| **G5-013** | ölü kaynak | `colors.xml`'de 3/5 renk 0 kullanım; yorumu yanlış | ölçüm | 3 satır |
| **G5-014** | yorum | "CLAUDE.md §14" ×2 — öyle bir bölüm yok | ölçüm | 2 karakter |
| **G5-015** | yorum | `VeriYonetimiDeposu` "Room (12 tablo)" — 15 tablo | ölçüm | 1 sayı |
| **G5-016** | ölü kod | 5 DAO/depo üyesi + 1 alan, 0 kullanım (biri `sonGunler`'in `Flow` ikizi) | ölçüm | ~20 satır |
| **G5-017** | ölü kod | `NudgeDurum` data class hiç örneklenmiyor | ölçüm | 4 satır |
| **G5-018** | ölü kod | `Rota.HESAP` 0 kayıt / 0 gezinme | ölçüm | 1 satır |
| **G5-019** | ölü varlık | `yedam_iki_figur.png` 770 KB, 0 referans | ölçüm | 770 KB |
| **G5-020** | aşırı varlık | `giris_guvenlik_banner.png` 2469×951 / 2,5 MB | ölçüm | ~2,3 MB |
| **G5-021** | ölü betik | `kontrol_baslik.py`, `tasarim_ekran_kes.py` 0 referans | ölçüm | 6,4 KB |
| **G5-022** | yorum | `Olcu.GrafikEkseni` iki KDoc, birincisi ölü | ölçüm | 1 satır |
| **G5-023** | ölçüm | Sahte/yedekli katman: 8 dosya, 1.072 satır, 10 test | ölçüm | — (silme önerilmiyor) |
| **G5-024** | yapı | `ProfilEkran.kt` 1.616 satır | görüş | — |
| **G5-025** | soyutlama | `Bicim.kt` var, 5 yerel kopya yazılmış | görüş | — |
| **G5-026** | doküman | `debug.md` 75, `CLAUDE.md` 113 dosyadan referanslı | görüş | — |
| **G5-027** | UI metni | `FaktorSeviyesi` etiketleri asimetrik ("Düşük risk" / "Yüksek") | görüş | 2 dize |
| **G5-028** | UI metni | Hedef ekranı aynı sayıyı 3 kez, 2 farklı biçimde gösteriyor | ölçüm/görüş | — |
| **G5-029** | UI metni | "yarından itibaren geçerli" şeridi mola eşiği için **yanlış** | ölçüm | 1 satır taşıma |
| **G5-030** | UI metni | Lig "Son **0** düşer" | ölçüm/görüş | 1 koşul |
| **G5-031** | UI metni | "Adım kaynağı: Telefon sayacı" — gerekçesi yalnızca kodda | görüş | — |
| **G5-032** | UI metni | Başlıkta "🔥 0 gün" seri çipi | görüş | 1 koşul |
| **G5-033** | UI metni | İstatistik boş-gün açıklaması 3 cümle | görüş | ~1 cümle |
| **G5-034** | sürtünme | Mola hatırlatıcı anahtarı Gizlilik'te, eşiği Hedeflerim'de | görüş | — |
| **G5-035** | sürtünme | Devre dışı "Kişilerden bul" + onu açıklayan paragraf | görüş | — |
| **G5-036** | K itirazı | K50 (dal ayrımı) K53'ten sonra bedelini hak etmiyor | görüş | — |
| **G5-037** | K itirazı | `LigDeposu.onerilenler()` sunucuda hiç uygulanmıyor | ölçüm | ~100 satır (G5-005 ile aynı) |

**37 madde · 22'si ölçüm · 15'i görüş (ya da karma).**
Ölçülebilir kazanç: **≈ 500 kod satırı** ve **≈ 3 MB varlık**.

---

> **Bu rapor tespit amaçlıdır.** Hiçbir kaynak dosya değiştirilmedi, hiçbir düzeltme
> uygulanmadı, hiçbir git yazma komutu çalıştırılmadı. Cihazda hiçbir izin, saat,
> saat dilimi ya da uygulama verisi değiştirilmedi.
