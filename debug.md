# debug.md — Yeşilİz açık kusur listesi

> **Cihaz:** Samsung SM-A217F (Galaxy A21s) · Android 12 / API 31 · 720×1600 @280dpi · 3 tuşlu gezinme
> **Tarih:** 31 Temmuz 2026 · **Derleme:** `fix/gercek-telefon-onkosullari` @ `a7e19e9`
>
> **İki kaynak birleştirildi:**
> - `[tarama]` — uygulama `pm clear` ile sıfırlanıp uçtan uca gezildi; tıklanabilir her öge
>   `uiautomator` ile listelenip tek tek denendi, sonuç ekran görüntüsü + Room veritabanı
>   (`run-as … cat databases/yesiliz.db`) ile doğrulandı.
> - `[kullanıcı]` — gerçek kullanımdan çıkan 20 maddelik not. **Hepsi kodda doğrulandı**,
>   yıkıcı olanlar cihazda tekrar üretildi.
> - `[ikisi]` — iki kaynakta da çıkan bulgu.
>
> Kullanıcı listesindeki 20 maddenin **tamamı** aşağıda karşılık buluyor.

Bu dosya CLAUDE.md §11'deki **gerçek telefonda uçtan uca doğrulama** işinin çıktısıdır ve
açık iş listesinin kaynağıdır.

> **✅ işareti = düzeltildi ve cihazda doğrulandı.** Bu dosya aynı zamanda ilerleme
> kaydı; hangi commit'te düzeltildiği `git log`'dan okunur.

---

## §0 · Önce iyi haber: zincir gerçekten çalışıyor

Emülatörde hiç görülemeyen şey ilk kez görüldü (CLAUDE.md §10):

```
UsageStats → Room → core-risk → risk_sonuc → StateFlow → Composable
```

`gunluk_ozet` gerçek veriyle doldu (45 açılış, TikTok 9 dk), `risk_sonuc` gerçek skor üretti
(`S=10,88 · rS=81,97 · alpha=0,49 · R=41,47 · adayBant=ORTA · gösterilenBant=AZ`), anket
10 maddeyle kaydedildi, puan/görev/ruh hali yazıldı, dışa aktarım gerçek dosya üretti.
**Motor sağlam.** Aşağıdakiler bu zincirin **etrafındaki** kusurlar.

---

## §1 · KRİTİK

### ✅ K-1 · Ebeveyn panelinde kullanıcı kilitleniyor (geri tuşu dahil) `[ikisi]`

**`ui/ekran/ebeveyn/EbeveynEkran.kt:98`**

Profil › Ebeveyn paneli açılınca rol seçimi alt sayfası geliyor. Kullanıcı bu sayfayı
kapatırsa (perdeye dokunma ya da aşağı sürükleme) ekran **tamamen boşalıyor** — yalnızca
başlık ve boş zemin kalıyor. Alt sayfa geri gelmiyor, **geri tuşu da çalışmıyor**,
uygulamayı kapatıp açmak da yetmiyor. Yalnızca `force-stop` kurtarıyor.

```kotlin
ModalBottomSheet(
    onDismissRequest = {},          // ← boş
```

M3 `ModalBottomSheet` kendini yine de gizliyor; `onDismissRequest` sadece bir geri çağrı.
`if (!durum.eslesmisMi)` hâlâ true olduğu için sayfa bileşimde kalıyor ama `sheetState`
artık `Hidden` → hiçbir şey çizilmiyor, buna karşın **görünmez perde dokunuşları ve geri
tuşunu yutmaya devam ediyor**.

---

### ✅ K-2 · "Verilerimi sil" ve "Hesabımı sil" uygulamayı ANINDA çökertiyor, hiçbir şey silmiyor `[kullanıcı]`

**`data/depo/VeriYonetimiDeposu.kt:24` · `ui/ekran/profil/ProfilViewModel.kt:120`**

Cihazda tekrar üretildi. Onay butonuna basıldığı anda uygulama kapanıyor, launcher'a
düşülüyor. Gerçek yığın izi:

```
FATAL EXCEPTION: main
Process: tr.org.yesilay.yesiliz
java.lang.IllegalStateException: Cannot access database on the main thread
        since it may potentially lock the UI for a long period of time.
    at androidx.room.RoomDatabase.assertNotMainThread(RoomDatabase.android.kt:592)
    at androidx.room.RoomDatabase.performClear(RoomDatabase.android.kt:528)
    at ...data.yerel.YesilizDb_Impl.clearAllTables(YesilizDb_Impl.kt:427)
    at ...data.depo.VeriYonetimiDeposu.tumVerileriSil(VeriYonetimiDeposu.kt:24)
    at ...ui.ekran.profil.ProfilViewModel$verileriSil$1.invokeSuspend(ProfilViewModel.kt:120)
    at ...ui.ekran.profil.ProfilEkranKt.ProfilEkran$lambda$34$0$0(ProfilEkran.kt:343)
```

**Sebep:** `Room.clearAllTables()` **bloklayan** (suspend olmayan) bir çağrı;
`viewModelScope.launch { }` varsayılan olarak `Dispatchers.Main.immediate` üzerinde
koşuyor. Room ana iş parçacığında veritabanına erişimi reddedip `IllegalStateException`
fırlatıyor.

**Silme, istisna atıldığı için hiç başlamıyor.** Çökme öncesi ve sonrası satır sayıları
birebir aynı:

| tablo | önce | sonra |
|---|---|---|
| `gunluk_ozet` | 8 | 8 |
| `puan_gunluk` | 8 | 8 |
| `risk_sonuc` | 8 | 8 |
| `kategori_esleme` | 3.106 | 3.106 |
| `anket_yanit` | 1 | 1 |
| `arkadas` | 1 | 1 |

Kullanıcının iki ayrı gözlemi ("beni uygulamadan atıyor ve verileri silmiyor",
"direkt uygulama çöküyor, gene silmiyor") tek ve aynı hatanın iki yüzü.

**"Hesabımı sil" de aynı çökmeyi veriyor:** `ProfilEkran.kt:362` ile `:343` **aynı**
`viewModel.verileriSil()`'i çağırıyor — iki akış arasında hiçbir fark yok.
(Otomasyonla `SİL` onay metnindeki `İ` karakteri gönderilemediği için o butonun yığın
izi ayrıca alınamadı; kod yolu birebir aynı ve kullanıcı çökmeyi ilk elden bildirdi.)

**Düzeltilse bile eksik kalacak üç şey** — aynı turda ele alınmalı:

- **JWT temizlenmiyor.** `JetonDeposu` ayrı bir DataStore'da (`name = "jetonlar"`,
  `data/uzak/JetonDeposu.kt:16`) ve `VeriYonetimiDeposu` ona **hiç dokunmuyor**. Kullanıcı
  "hesabım silindi" dedikten sonra oturum açık kalır. CLAUDE.md §6 tam tersini iddia
  ediyor (*"…'verilerimi sil' akışı jetonları tek başına temizleyebilsin diye"*) — **belge
  yanlış**, kod öyle yapmıyor.
- `HesapDeposu.cikisYap()` çağrılmıyor (Y-3'teki ölü kodun aynısı).
- `clearAllTables()` `kategori_esleme`'deki 3.106 seed satırını da silecek;
  `kategoriDeposu.seedEt()` yalnızca `AcilisViewModel.init`'te koşuyor → aynı oturumda tüm
  uygulamalar "kategorisiz" kalır. Silmeden sonra yeniden seed gerekiyor.
- Silme çalışır hâle gelince **K-4 devreye girecek**: `GunlukOzetWorker` telafisi 7 günlük
  sahte özet + puan + risk satırını hemen geri yazar. K-4 düzeltilmeden silme akışı
  düzelmiş sayılmaz.

---

### ✅ K-3 · "Kategorize et" tamamen çalışmıyor — kaydet butonu ekrana sığmıyor `[kullanıcı]`

**`ui/ekran/kategori/KategorizeEtEkran.kt:196-211`**

Cihazda tekrar üretildi. Uygulama seçiliyor, kategori kartına dokunuluyor, **hiçbir şey
olmuyor**: alt sayfa açık kalıyor, `kategori_esleme` tablosuna `kaynak='kullanici'` satırı
**hiç yazılmıyor**, hata da yok.

**Sebep:** kartlar yalnızca yerel durumu değiştiriyor —

```kotlin
SecenekKarti(..., onTikla = { secilen = kategori })   // sadece işaretliyor
...
BirincilButon(metin = "Kategoriyi kaydet", onTikla = { onKaydet(secilen) })  // asıl kayıt
```

Asıl kaydı yapan **"Kategoriyi kaydet"** butonu 8 kategori kartının **altında**. Alt sayfa
Y-1 yüzünden yarım açılıyor, içerik `Column`'unda `verticalScroll` **yok**, sonuna kadar
sürüklense bile buton ekran dışında kalıyor. Erişilebilirlik ağacında hiç görünmüyor;
sayfa içinde kaydırma denemeleri de sonuç vermiyor.

Yani ekran, uygulamanın kullanıcıya vaat ettiği tek düzeltme mekanizmasını (kategori
ağırlıkları risk skorunun girdisi — `docs/spec-risk.md`) **bu cihazda %100 kullanılamaz**
hâlde. Y-1 düzeltilince kendiliğinden çözülür, ama içerik ayrıca kaydırılabilir olmalı.

---

### ✅ K-4 · Yeni kullanıcı hiçbir şey yapmadan 8 günlük seri ve ~400 puan kazanıyor `[tarama]`

**`data/depo/KullanimDeposu.kt` (`gunuIsle`) · `data/depo/PuanDeposu.kt`
(`gunuYenidenHesapla`) · `arkaplan/GunlukOzetWorker.kt` (`TELAFI_GUN = 7`)**

Temiz kurulumdan ~10 dakika sonra:

```
puan_gunluk                          gunluk_ozet (aynı günler)
2026-07-24 | ham 35 | seri 1 | ×1.1   → tüm alanlar 0.0
...                                     ...
2026-07-31 | ham 35 | seri 8 | ×1.8   → gerçek veri
```

Ana ekran: **"8 gün" serisi · "404 puan"**. Kullanıcı tek bir görev yapmadı.

1. `GunlukOzetWorker` son 7 günü telafi ediyor.
2. `KullanimDeposu.gunuIsle` o günlerde hiç oturum bulamasa da **her koşulda** bir
   `gunluk_ozet` satırı yazıyor — sıfır dolu.
3. `PuanDeposu.gunuYenidenHesapla` satırı görüyor:
   `hedefTutturuldu = ozet != null && 0.0 <= hedefDk` → **true**;
   `geceTemiz = ozet != null && 0.0 < eşik` → **true**. Her boş güne tam bonus + seri.

Kodun kendi yorumu tam bunu engellemeyi amaçlıyor (*"Özet yoksa hedef 'tutturuldu'
sayılmıyor…"*) — ama sıfır dolu satır "özet var" sayıldığı için koruma devreye girmiyor.

Etkisi ödül sisteminin ötesinde: İstatistik bu sahte günleri grafiğe çiziyor ("Geçen
haftaya göre 2 dk daha fazla" — karşılaştırılan hafta uydurma) ve `risk_sonuc`'a 7 gün
sahte `AZ` bandı giriyor.

---

### ✅ K-5 · Sahte geçmiş alpha eğrisini kirletiyor — risk skoru olması gerekenden düşük `[tarama]`

**`data/depo/RiskDeposu.kt:150` (`dEfektif`)**

```kotlin
canliGun = ozetDao.gunSayisiEKadar(tarih)   // satır sayıyor, veri değil
```

K-4'ün yazdığı 7 boş satır yüzünden uygulama **ilk gününde** `d = 8` sanıyor. Ölçülen
sonuç: `alpha = 0,49` (olması gereken `1,0`). Anket puanı 81,97 iken gösterilen risk
`R = 41,47`, bant `ORTA` yerine `AZ`.

Sahte geçmiş yalnızca puanı şişirmiyor, **projenin çekirdek çıktısını da olduğundan düşük
gösteriyor**. `docs/spec-risk.md §4.5`'e göre `d` "kaç gün gerçek veri biriktiği" olmalı,
satır sayısı değil. Y-7'nin de ana sebeplerinden biri bu.

---

### ✅ K-6 · Health Connect kurulu olduğu hâlde "kurulu değil" deniyor — adım sayacı gerçek cihazda tamamen ölü `[ikisi]`

**`app/src/main/AndroidManifest.xml:57` (`<queries>`)**

Cihazda kurulu: `com.google.android.apps.healthdata` **v2026.07.09.00**. Uygulama Profil ›
Adım ve hareket izni ekranında **"Health Connect kurulu değil"** diyor ve Play Store'a
yönlendiriyor (Play Store'da buton "Kaldır" yazıyor — yani kurulu).

**Sebep:** Android 11+ paket görünürlüğü. Manifest'teki `<queries>` yalnızca MAIN/LAUNCHER
intent'ini bildiriyor. Health Connect'in bu cihazda **launcher aktivitesi yok**
(`cmd package query-activities -a MAIN -c LAUNCHER` sonucunda çıkmıyor; yalnızca
`SettingsActivity1` var). Uygulama paketi hiç göremiyor → `getSdkStatus()` `SDK_AVAILABLE`
dönmüyor. Birleştirilmiş manifest'te `com.google.android.apps.healthdata` için `<package>`
bildirimi yok; HC istemcisi 1.2.0-alpha01 kendiliğinden eklemiyor.

**Sonuç:** `gunluk_ozet.adim = 0`, `sporDk = 0`, `D = 1,0` — aktivite sönümlemesi hiç
uygulanmıyor, `docs/spec-risk.md`'deki bu dal gerçek cihazda hiç test edilemiyor.

**Ek kusur:** `SaglikOkuyucu.kullanilabilir()` düz `Boolean` döndürüyor, bu yüzden
`SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` ("güncellemen gerek") ile `SDK_UNAVAILABLE`
("kurulu değil") ayırt edilemiyor.

---

### ✅ K-7 · Veri yokken "Fiziksel hareket: Yüksek" + tam kırmızı çubuk `[tarama]`

**`core-risk/.../EtkileyenFaktorler.kt:41`**

```kotlin
val hareketOran = 1.0 - RiskHesapla.araUrunler(gun).A
```

K-6 yüzünden adım verisi hiç gelmiyor → `A = 0,0` → `hareketOran = 1,0` → `YUKSEK`.
Risk analizi ekranı üstte "**Düşük** risk düzeyi / son 7 günlük kullanım örüntün sağlıklı
bir düzeyde görünüyor" derken hemen altında **Fiziksel hareket → Yüksek** ve ucundan ucuna
kırmızı çubuk gösteriyor. Uygulamayı yeni kuran **her** kullanıcı bunu görüyor.

Veri yokluğu maksimum risk olarak sunuluyor — uygulamanın başka her yerinde uygulanan
"veri yoksa `BosDurum` göster, uydurma" ilkesinin (`ui/bilesen/BosDurum.kt`) tersi.

> Kırmızının kendisi kural ihlali değil (§12 K10 bu ekranda kırmızıya izin veriyor).
> Sorun **rengin değil, iddianın** yanlış olması.

---

### ✅ K-8 · Ebeveyn paneli rastgele 6 haneli sayıyla açılıyor `[kullanıcı]`

**`data/depo/EbeveynDeposu.kt:48-67`**

```kotlin
} catch (_: Exception) {
    // Çevrimdışı: eşleşmeyi yerel olarak işaretle.
    tercihler.ebeveynEslesmeyiKaydet(rol)
    null            // ← "başarılı"
}
```

`eslestir()` HTTP olmayan her istisnada eşleşmeyi **yerel olarak kaydedip başarı
dönüyor**. Gerçek telefonda sunucuya hiç ulaşılamadığı için (O-14) çalışan tek yol bu →
**6 haneli herhangi bir sayı eşleştirmeyi açıyor**; tek kontrol `kod.length != 6`.

Kod yorumu bunu demo sürekliliği için bilinçli yaptığını söylüyor, ama gerçek cihazda
**tek davranış** hâline geldiği için artık bilinçli bir yedek değil, varsayılan.
Çocuğun ekran verisine erişim veren bir akışta bu bir güvenlik sorunu.

---

### ✅ K-9 · Otomatik görevler elle işaretlenebiliyor ve çift puan veriyor `[kullanıcı]`

**`ui/ekran/ana/AnaEkran.kt:655-661` · `domain/gorev/Gorev.kt` · `data/depo/PuanDeposu.kt`**

```kotlin
when (gorev) {
    Gorev.NEFES   -> BirincilButon("Nefes egzersizine başla", …)
    Gorev.YURUYUS -> BirincilButon("Yürüyüşü başlat", …)
    else          -> BirincilButon("Tamamlandı olarak işaretle", onTikla = onTamamla)
}
```

`else` dalı, `DogrulamaTuru.OTOMATIK` ilan edilmiş iki görevi de kapsıyor:

| Görev | Puan | İlan edilen doğrulama | Gerçekte |
|---|---|---|---|
| `TUKETIM_HEDEFI` — "Günlük hedefi tuttur" | +25 | OTOMATIK | elle işaretlenebiliyor |
| `GECE_TEMIZ` — "Telefonsuz gece" | +10 | OTOMATIK | elle işaretlenebiliyor |

Üstelik `PuanHesapla.gunlukPuan` **aynı davranışı özetten zaten ödüllendiriyor**
(`hedefTutturuldu`, `geceTemiz`) → aynı gün için **çift sayım**. `PuanDeposu.gorevTamamla`
kaydı `dogrulamaTuru = gorev.dogrulama.anahtar` ile yazdığı için elle basılmış görev
`gorev_tamamlama` tablosuna **`"otomatik"`** olarak giriyor — denetim izi de yanıltıcı.

Kullanıcının isteği (gerçek kullanıma göre / oransal puan) İ-4'te.

---

## §2 · YÜKSEK

### ✅ Y-1 · Alt sayfalar yarım açılıyor; birincil eylem sistem çubuğunun altında kalıyor `[tarama]`

**`rememberModalBottomSheetState()` — 5 dosyanın hepsinde `skipPartiallyExpanded` yok:**
`ana/AnaEkran.kt:108` · `odul/OdulEkran.kt:90` · `profil/ProfilEkran.kt:128` ·
`kategori/KategorizeEtEkran.kt:70` · `ebeveyn/EbeveynEkran.kt:88`

Sayfalar yarı açık başlıyor. İçerik sığmayınca son ögeler ekran dışında kalıyor
(erişilebilirlik ağacında sınırlar `[0,0][0,0]`). Kullanıcıya sürüklemesi gerektiğini
söyleyen hiçbir işaret yok; buton yarı yarıya gezinme çubuğunun **arkasında** çiziliyor.

`contentWindowInsets = { WindowInsets.navigationBars }` her sayfada doğru verilmiş; sorun
inset değil, **açılış yüksekliği** ve içeriğin kaydırılamaması.

| Alt sayfa | Erişilemeyen öge |
|---|---|
| **Kategori seç** | `Kategoriyi kaydet` — sürüklense bile erişilemiyor → **K-3** |
| **Verilerimi sil** | Onay kutusu **ve** silme butonu |
| Hesabımı sil | `SİL` yazma alanı ve onay butonu |
| Ödül detayı | `Puan kazanmaya devam et` — dokunuş sistem çubuğuna gidiyor |
| Geçmişi indir | `Dosyaları oluştur ve paylaş` |
| Toplanan veriler | Son iki madde |
| **YEDAM hakkında** | `115'i ara` — yardım hattını arama butonu |
| Yardım | "Verilerim kimlerle paylaşılıyor?" |
| Hakkında | "Açık kaynak lisansları" |
| Ebeveyn rol seçimi | "Bu cihaz çocuk cihazı" seçeneği |

Çoğunda sürükleyince erişilebiliyor; kategori seçiminde **hiç** erişilemiyor.

---

### ✅ Y-2 · Bildirimler başlığında "Tümünü oku" başlığın ve geri okunun üstüne biniyor `[tarama]`

**`ui/ekran/bildirim/BildirimEkran.kt:62` · `ui/bilesen/YesilizButon.kt:124`**

Başlık çubuğunda "Bildirimler" ile "Tümünü oku" **üst üste** çiziliyor, ikisi de okunmuyor.
Dahası "Tümünü oku"nun dokunma alanı çubuğun tamamını (685 px) kaplıyor: **geri okuna
basınca bildirimlerin hepsi okundu işaretleniyor** ve ekrandan çıkılmıyor.

`sagEylem` olarak `MetinBaglanti` kullanılmış; o bileşen `TextButton` + `fillMaxWidth()`.
Kendi KDoc'u zaten uyarıyor: *"tek başına duran bir eylem için tasarlandı ve `fillMaxWidth`
dayatıyor"*. Doğrusu `SatirBaglanti`.

---

### ✅ Y-3 · Hesaptan çıkış yapılamıyor `[ikisi]`

`HesapDeposu.cikisYap()` yazılmış ve üç deponun (`Retrofit`, `YerelSahte`, `Yedekli`)
hepsinde uygulanmış, ama **hiçbir ekrandan çağrılmıyor**. Profil menüsünün tamamı tarandı:
HEDEFLERİM · VERİLERİM · GİZLİLİK · Ebeveyn paneli · DESTEK · GELİŞTİRİCİ — "Çıkış yap"
satırı yok. Giriş yapan kullanıcı hesabını değiştiremiyor; tek yol "Hesabımı sil" (ki o da
K-2 yüzünden çöküyor).

---

### ✅ Y-4 · Ebeveyn eşleşmesini kaldırma / ebeveyn hesabından çıkma yok `[kullanıcı]`

`EbeveynViewModel.eslesmeyiKaldir()` **yazılmış ama hiçbir ekrandan çağrılmıyor** —
Y-3'teki `cikisYap()` ile birebir aynı ölü kod deseni. Ebeveyn panelinde bağlantıyı kesme,
hesabı kapatma veya çıkma seçeneği hiç yok.

İroni: Yardım ekranındaki dört sorudan biri tam olarak *"Ebeveyn paneli eşleştirmesini
nasıl kaldırırım?"* — hem sorunun cevabı yok (Y-6) hem de özelliğin kendisi yok.

---

### ✅ Y-5 · "Ligde görün" tamamen ölü bir tercih `[kullanıcı]`

`Tercihler.ligdeGorun` (`Tercihler.kt:82`) yazılıyor ve **yalnızca kendi anahtarını
çizmek için** okunuyor (`ProfilViewModel` → `ProfilDurum` → `ProfilEkran`). Lig kodu,
`SenkronDeposu`, `RetrofitLigDeposu` ve backend'in hiçbiri tüketmiyor.

Kullanıcı anahtarı kapatsa da ligde görünmeye devam ediyor. Bu bir gizlilik tercihi
olarak sunulduğu için yalnızca "çalışmayan buton" değil, **tutulmayan bir söz**.

---

### ✅ Y-6 · Kabul edilmesi zorunlu tutulan sözleşmeler ve Yardım cevapları hiçbir yerde yok `[ikisi]`

Aynı desen üç yerde: chevron'lu, tıklanabilir görünen satır — **`onTikla` yok**.

| Yer | Satırlar | Kaynak |
|---|---|---|
| Profil › Yardım | 4 SSS sorusu — **hiçbirinin cevabı yazılmamış** | `ProfilEkran.kt:717-741` |
| Profil › Hakkında | "Kullanım şartları", "Gizlilik politikası", "Açık kaynak lisansları" | `ProfilEkran.kt:797-816` |
| Kayıt ekranı | "Kullanım Koşulları'nı ve Gizlilik Politikası'nı okudum" — düz metin, bağlantı değil | `KayitEkran.kt` |

Kayıt ekranı bu onayı **zorunlu** tutuyor (onaylanmadan kayıt olmuyor), ama kullanıcının
metni okuyabileceği hiçbir yol yok. 18 yaş altı KVKK rejimi (CLAUDE.md §4) düşünülünce
ayrıca riskli.

---

### ✅ Y-7 · Anket yenilenince gösterilen risk seviyesi değişmiyor `[kullanıcı]`

Kullanıcı ölçümü: ham puan **60** ve ham puan **10** ile iki ayrı değerlendirme, **ikisinde
de "orta"**. Yazma yolu doğru çalışıyor (`AnketYenileViewModel` → `anket_yanit` →
`riskDeposu.gunuHesapla` → `isPlanlayici.simdiTazele`, `f0fa8ca` düzeltmesi tuttu), sorun
**gösterilen bandın** hareket etmemesi. Üç sebep üst üste biniyor:

1. **Histerezis** — `Sabitler.GUN_ASAGI = 7`: bandın inmesi 7 ardışık gün istiyor
   (`GUN_YUKARI = 3`). Tasarım gereği asimetrik ("uyarmakta hızlı, rahatlamakta yavaş");
   §12 A6 hâlâ açık bir karar.
2. **K-5** — `alpha = 0,49` olmamalıydı; ilk gün `1,0` olmalı. `R = alpha·rS + (1-alpha)·rB`
   olduğu için anketin ağırlığı yarıya inmiş durumda.
3. **`PENCERE_GUN = 7`** kayan ortalaması, K-4'ün ürettiği 6 sahte sıfır günün üstünden
   geçiyor.

Ham 60 → `rS = 100`, ham 10 → `rS = 0` olmasına rağmen ekranın aynı kalması bunlardan.
**Asıl UX kusuru:** kullanıcıya hiçbir açıklama gösterilmiyor. "Yeni değerlendirmen
kaydedildi; gösterilen seviye birkaç gün içinde güncellenecek" gibi tek bir cümle yok —
kullanıcı anketi doldurmanın hiçbir işe yaramadığı sonucuna varıyor.

---

### ✅ Y-8 · Nefes seansı bitince hiçbir şey olmuyor `[tarama]`

2 dakikalık seans `0:00 kaldı`'ya ulaşıp **öylece duruyor**: faz dairesi hâlâ "Tut" yazıyor,
buton "Oynat"a dönüyor. Tebrik yok, "+5 puan" bildirimi yok, otomatik dönüş yok.
Puan **aslında yazılıyor** (`gorev_tamamlama: nefes | 5 | otomatik`, günlük puan 63 → 72),
ama kullanıcı bunu ancak geri gidip ana ekrana bakarsa anlıyor.

Kontroller tek tek doğrulandı ve **hepsi çalışıyor**: 4s/5s/6s tempo, duraklat, sürdür,
baştan başlat, kapat.

---

### ✅ Y-9 · "Mola hatırlatıcı" anahtarı açılınca nudge servisi başlamıyor `[tarama]`

**`MainActivity.kt:66` (`onResume`)** — servis yalnızca `onResume` içinde başlatılıyor;
kullanıcı anahtarı uygulama önplandayken açtığı için `onResume` tekrar çalışmıyor.

Ölçüm: anahtar açık + `SYSTEM_ALERT_WINDOW: allow` → çalışan servis **0**. Ana ekrana çıkıp
dönünce → `ServiceRecord{… .nudge.NudgeOverlayService}`. Yani kullanıcı anahtarı açıp
uygulamayı kullanmaya devam ederse nudge (§3 çekirdek döngünün "YÖNLENDİR" adımı) o oturum
boyunca hiç çalışmıyor.

---

### ✅ Y-10 · Arkadaş "Ekle" dokunuşu kaydediliyor ama ekranda hiçbir iz bırakmıyor `[tarama]`

Lig › Arkadaş ekle › Önerilen kişiler › `Ekle`: veritabanına yazılıyor
(`arkadas: Ece Kılıç | davet_gonderildi`), ama çip hâlâ "Ekle" diyor, kişi listeden
çıkmıyor, "Davet gönderildi" durumu yok. Kullanıcı işe yaramadı sanıp tekrar dokunuyor.

---

### ✅ Y-11 · 18 yaş altı için ebeveyn onayı hiç istenmiyor `[tarama]`

CLAUDE.md §4: *"18 yaş altı: ebeveyn onayı zorunlu, KVKK ayrı rejim."*
Onboarding 3/4'te yaş **15** girildi, "Devam" → doğrudan ankete geçti. Onay sorulmadı,
uyarı çıkmadı, akış hiçbir yerde ayrışmadı. Varsayılan yaş **17** — yani varsayılan
kullanıcı zaten 18 altı.

---

### ✅ Y-12 · Ağ çağrılarında yükleniyor göstergesi yok — ekran 5 saniye ölü kalıyor `[tarama]`

`AgModulu.ZAMAN_ASIMI_SN = 5`. Buton `etkin = !durum.islemde` ile pasifleşiyor ama hiçbir
görsel gösterge yok. En belirgin hâli: kayıt formunda tüm alanlar boşken "Hesap oluştur" →
**"Adını yazman gerekiyor."** hatası **5+ saniye sonra** çıkıyor, çünkü alan doğrulaması
depo katmanında ve `YedekliHesapDeposu` önce ağı deniyor. Tamamen istemci tarafı olması
gereken bir kontrol, ağ gecikmesinin arkasında.

---

## §3 · ORTA

| # | Bulgu | Kaynak |
|---|---|---|
| **✅ O-1** | **Onboarding içerik alanı kaydırılamıyor.** `OnboardingIskelet.kt:59` — `Column(Modifier.weight(1f))`, `verticalScroll` yok. İlk sonuç ekranında sürükleyiciler kartının üçüncü satırı ("Fiziksel hareket") kırpılıyor. Küçük ekran / büyük yazı tipi ölçeğinde diğer adımlar da etkilenir | `[tarama]` |
| **✅ O-2** | **İlk değerlendirme metni sabit.** `IlkSonucEkran.kt:71` — anket "Şimdi değil" ile tamamen atlansa da *"Telefon kullanımın şu an dikkat edilmesi gereken bir düzeyde."* diyor. Hiç veri yokken kullanıcının kullanımı hakkında iddia | `[tarama]` |
| **✅ O-3** | **"Nasıl çalışır?" hiçbir şey açıklamıyor.** `OnboardingAkisi.kt:44-45` — `onBasla` ve `onNasilCalisir` **birebir aynı** hedefe (`Rota.IZIN`) gidiyor; nasıl çalıştığını anlatan ekran yok | `[tarama]` |
| **✅ O-4** | **"Bu bir tanı değildir" tıklanabilir ama hiçbir şey yapmıyor.** `TestEkran.kt:293`, `AnketYenileEkran.kt:180` — `MetinBaglanti(… onTikla = {})`, yani dalga efekti veren ölü `TextButton`. Sorumluluk reddi metni; düz `Text` olmalı | `[tarama]` |
| **✅ O-5** | **Bildirim saati: 5. çip dikey eziliyor + serbest saat seçilemiyor.** `BildirimSaatleri.kt:24` `SABAH_SECENEKLERI = listOf(7,8,9,10,11)` — beş çip satıra sığmıyor, "11:00" tek karakter genişliğinde dikey sütuna (`1 1: 0 0`) sıkışıyor. Kullanıcı ayrıca istediği saati giremiyor (→ İ-5) | `[ikisi]` |
| **✅ O-6** | **"Uygulama kategorileri" başlığı "Uygulama" olarak kırpılıyor.** Ortalanmış tek satır; üç nokta da konmuyor. Erişilebilirlik ağacında tam metin var, ekranda yok | `[tarama]` |
| **✅ O-7** | **Risk detayı öneri kartlarında iki satırlık başlık açıklamaya yapışıyor.** "Hatırlatma saatlerini ayarla", "Günlük hedefini gözden geçir", "İki dakikalık nefes molası" — üçünde de metinler birbirine giriyor | `[tarama]` |
| **✅ O-8** | **"Hatırlatma saatlerini ayarla → Aç" hedefe götürmüyor.** Profil ekranının tepesine bırakıyor, bildirim saatleri alt sayfasını açmıyor. Diğer iki eylem doğru: "Ayarla" → Günlük hedef, "Başlat" → Nefes | `[tarama]` |
| **✅ O-9** | **Cinsiyet seçicide üçüncü seçenek iki satıra düşüyor, seçili pil hizasız.** "Belirtmek istemiyorum" kapsayıcıyı yükseltiyor ama seçili pil tek satır yüksekliğinde kalıyor | `[tarama]` |
| **✅ O-10** | **Ana ekranda yan yana iki kart farklı yükseklikte.** "Bilgi Bankası" / "Kendini Test Et" — sağdaki kısa, başlığı iki satıra kırılınca alt metniyle çakışıyor. `IntrinsicSize` ile eşitlenmeli | `[tarama]` |
| **✅ O-11** | **Giriş ve şifre ekranlarında eskimiş metin.** Backend 31 Tem'de bağlandı ama ekranlar hâlâ *"hesaplar yalnızca bu cihazda tutuluyor"*, *"Hesap sistemi henüz devrede değil"*, *"…hesap sistemi devreye girdiğinde gönderilecek"* diyor | `[tarama]` |
| **✅ O-12** | **İstatistik "Gün" sekmesinde tek gün için "ORTALAMA" yazıyor.** "TOPLAM" ya da "BUGÜN" olmalı; Hafta/Ay sekmelerinde etiket doğru | `[tarama]` |
| **✅ O-13** | **Önerilen kişiler sahte ve "3 ortak arkadaş" diyor.** Rehber hiç okunmuyor (§12 K14, ekranda da yazılı) ve sunucuda sosyal grafik yok. Uydurma isim tolere edilebilir ama **"3 ortak arkadaş"** aynı ekranda verilen mahremiyet sözüyle doğrudan çelişiyor | `[tarama]` |
| **✅ O-14** | **Backend gerçek telefondan erişilemiyor; lig her zaman sahte.** `AgModulu.EMULATOR_TABANI = http://10.0.2.2:8000/` yalnızca emülatör içindir. Gerçek telefonda **her** çağrı 5 sn sonra başarısız olup `YerelSahteLigDeposu`'na düşüyor (§12 K18 gereği doğru davranış) — ama ekranda bunun çevrimdışı yedek olduğunu söyleyen **hiçbir işaret yok**. Gerçek cihazda bu istisna değil, **varsayılan durum**. Ebeveyn ekranındaki `cevrimdisiMi` göstergesinin benzeri lig için de gerekli. K-8'in de kök nedeni | `[tarama]` |
| **✅ O-15** | **Seri ("8 gün") çipi tıklanmıyor.** `AnaEkran.kt:237` `Cip(...)` — `onTikla` verilmemiş. `Cip` bu parametreyi destekliyor (`Cip.kt:37`) | `[kullanıcı]` |
| **✅ O-16** | **Ligdeki "İlk 7 yükselir" / "Son 7 düşer" tıklanmıyor.** `LigEkran.kt:170-180`, aynı sebep. İkisi de hap biçimli buton gibi duruyor, biri vurgulu biri soluk — filtre sanılıyor | `[kullanıcı]` |
| **✅ O-17** | **Sol üstteki avatar profile götürmüyor.** `AnaEkran.kt:220` `KullaniciAvatari` süsleme; tıklama yok. (İ-1'de Profil sekmesi kalkarsa bu bir eksiklik değil, **zorunluluk** olur) | `[kullanıcı]` |
| **✅ O-18** | **"Yeşilay ile paylaşım" hiçbir geri bildirim vermiyor.** Tercih aslında çalışıyor (`SenkronDeposu.kt:57` ağ çağrısını kapatıyor, §12 K19 doğrulandı), ama O-14 yüzünden açıp kapatmanın **gözlenebilir hiçbir sonucu yok**. Son senkron zamanı / "sunucuya ulaşılamıyor" durumu gösterilmiyor | `[kullanıcı]` |

---

## §4 · İSTEK ve KAPSAM KARARLARI

Bunlar kusur değil; ürün kararı gerektiriyor. Karar verilmeden kod yazılmamalı.

### ✅ İ-1 · Dördüncü sekme "Aktiviteler" olsun, Profil avatara taşınsın

Kullanıcının isteği: *"şuanki profil sekmesi yerinde yürüyüş, nefes egzersizi ve birkaç
şeyin olduğu bir sekme olsun; nefes ve yürüyüş haricinde uygulama konseptine uygun şeyler
ekle."*

Önerilen yapı — O-17'yi de kendiliğinden çözüyor:

```
Ana Sayfa · İstatistik · Lig · Aktiviteler
Profil → Ana Sayfa başlığındaki avatardan   (O-17 zaten bunu istiyor)
```

Aktivite seti — mevcut ikisi + dört öneri. Hepsi §4 mutlak kurallarına uygun (kilitleme
yok, ceza yok, içerik okuma yok, serbest metin yok) ve mevcut desenleri yeniden kullanıyor:

| Aktivite | Doğrulama | Yeniden kullanılan desen |
|---|---|---|
| Nefes egzersizi *(var)* | otomatik | `NefesFazHesapla` |
| Aktif yürüyüş *(var)* | otomatik | `YuruyusHesapla` |
| **Odak seansı** — telefonu bırak, 15/25/45 dk | otomatik (ekran kapalı süresi) | `DIJITAL_MOLA`'nın gerçekten doğrulanmış hâli; `nudge/Tetikleyici`'deki tüketim bloğu mantığı |
| **20-20-20 göz molası** — 20 sn uzağa bak | otomatik | `NefesFazHesapla` ile aynı saf faz hesabı |
| **Gece kapanışı** — yatmadan önce kısa ritüel | yarı otomatik | `GECE_TEMIZ` görevine bağlanır; `HatirlatmaWorker` akşam saatini zaten biliyor |
| **Ekransız ne yaptın?** — kart seçimi (metin girişi yok) | beyan | `ruh_hali` deseni; §4'teki serbest metin yasağına takılmıyor |

Yeni görevler `Gorev` enum'una eklenir, puan/limit `docs/spec-puan.md`'ye işlenir; her
aktivite `ui/ekran/<ad>/` altında kendi üçlü dosyasını alır (§6 kuralı).

### ✅ İ-2 · Nefes seansının süresi seçilebilsin

Bugünkü kontrol seansın uzunluğunu değil **temposunu** (4/5/6 sn nefes) değiştiriyor;
kullanıcı bunu süre sanmış — etiketleme de buna davetiye çıkarıyor. İki ayrı kontrol
gerekiyor: tempo (var) + **seans süresi** (1/2/5 dk, yeni). `Gorev.NEFES.aciklama`
"2 dakikalık kutu nefesi" olduğu için puan tarafı da gözden geçirilmeli.

### ✅ İ-3 · Yürüyüş arka planda da saysın

Bugünkü davranış: `viewModelScope` + `EkranAcikTut`; ekran kapanınca ya da uygulamadan
çıkınca **sayaç duruyor**. **§12 K15'i tersine çevirir** (*"foreground service MVP'nin
kritik yolunda değil"*). Foreground service + bildirim kanalı + `ACTIVITY_RECOGNITION`
izni demek. Karar kaydı gerekiyor.

### ✅ İ-4 · Hedef görevleri oransal puanlansın

K-9 elle işaretlemeyi kaldırdıktan sonraki soru: *"hedefinin %80'ini tutturdun, 25 puanın
20'sini alıyorsun"* gibi oransal ödül. `PuanHesapla.gunlukPuan` bugün ikili
(tuttu / tutmadı). `docs/spec-puan.md` değişikliği gerektirir; §4'teki "yalnızca kazanç"
kuralına aykırı değil çünkü hâlâ ceza yok.

### ✅ İ-5 · Bildirim saati serbest seçilsin

`SABAH_SECENEKLERI` / `AKSAM_SECENEKLERI` sabit listeleri yerine `TimePicker`.
O-5'teki taşan çip sorununu da kapatır.

---

## §5 · Doğrulanan ve sorunsuz çalışanlar

Kayıt altına alınıyor ki bir dahaki taramada tekrar edilmesin.

| Alan | Sonuç |
|---|---|
| Kayıt · giriş · misafir akışı | Çalışıyor; şifre hiçbir yere yazılmıyor (§12 K16) |
| Alan doğrulamaları | Doğru mesajlar üretiliyor (gecikme sorunu Y-12) |
| UsageStats izni | "İzin ver" sistem ekranını açıyor; dönüşte `onResume` ile algılanıyor, adım otomatik atlanıyor |
| Onboarding yaş sayacı | 10–70 arasında doğru sınırlanıyor |
| SAS-SV anketi | 10 madde · 5 sayfa · Likert 1-6; eksik yanıtla "Devam" pasif; `anket_yanit` doğru yazıldı (`hamPuan 49 · rS 81,97`) |
| Anket → risk yazma yolu | `gunuHesapla` + `simdiTazele` doğru çağrılıyor (`f0fa8ca` tuttu). Gösterilen bandın oynamaması ayrı konu → Y-7 |
| Ruh hali kaydırıcısı | `ruh_hali` yazıldı, `ruh_hali` görevi otomatik tamamlandı (+3) |
| Görev detayı alt sayfası | Açılıyor, çipler doğru (yanlış eylem sunması ayrı konu → K-9) |
| Nefes kontrolleri | Tempo 4s/5s/6s · duraklat · sürdür · baştan başlat · kapat — hepsi doğru |
| Ödüller ekranı | 4 basamak, ilerleme %82 doğru, dört detay sayfası da açılıyor |
| İstatistik Gün/Hafta/Ay | Üçü de doğru aralık ve veri gösteriyor; boş günler boş çiziliyor |
| Risk analizi | Bant, açıklama, 4 faktör, 3 öneri; iki eylem doğru hedefe gidiyor |
| Bilgi Bankası | Kategori süzgeci ve yazı ekranı çalışıyor, çevrimdışı |
| Kendini test et | 5 soru, eksikken buton pasif + açıklama, sonuç ekranı ve öneri kartları çalışıyor |
| Günlük hedef | 2–6 saat arasında sınırlı, hızlı seçim ve kaydetme çalışıyor |
| Uygulama kategorileri **listesi** | **Gerçek veri**: Eğlence 9 dk (TikTok), Sağlık 1 dk, Diğer 1 dk. Yalnızca *listeleme*; kategori *atama* çalışmıyor → K-3 |
| Geçmişi indir | Gerçek dosya üretiyor, Android paylaşım sayfası açılıyor |
| "Hesabımı sil" onay kapısı | `SİL` yazma zorunluluğu doğru kurgulanmış — ama onaylandığında akış çöküyor (K-2) |
| Davet kodu kopyala | "Kopyalandı" geri bildirimi veriyor |
| "Kişilerden bul" | Bilinçli olarak devre dışı, sebebi ekranda yazılı (§12 K14) — doğru |
| Alt gezinme çubuğu payı | 3 tuşlu gezinmede sekmeler tam boyutta ve tıklanabilir (`a7e19e9` düzeltmesi tuttu) |
| Kararlılık | K-2 dışında tarama boyunca çökme ve ANR yok |

---

## §6 · Önerilen düzeltme sırası

| Sıra | Maddeler | Gerekçe |
|---|---|---|
| 1 | **K-2** | Uygulama çöküyor ve yıkıcı bir işlem sessizce başarısız oluyor. `withContext(Dispatchers.IO)` tek satır; jeton temizleme + yeniden seed aynı turda |
| 2 | **K-1** | Kullanıcı uygulamada kilitleniyor; tek satır |
| 3 | **Y-1 → K-3** | `skipPartiallyExpanded = true` + içerik kaydırma. Kategorize et, veri silme onayı ve YEDAM hattı birlikte açılıyor |
| 4 | **K-4 · K-5** | Sahte geçmiş; puanı, risk skorunu, Y-7'yi **ve** silme akışının sonucunu birlikte bozuyor. Ortak kök: boş özet satırı yazılması |
| 5 | **K-9 · K-8** | Puan bütünlüğü + sahte ebeveyn eşleşmesi — ikisi de "uygulama yalan söylüyor" sınıfı |
| 6 | **K-6 · K-7** | Manifest'e tek `<package>` satırı; K-7 ardından kendiliğinden düzelir, kalanı "veri yok" durumu |
| 7 | **Y-3 · Y-4 · Y-5** | Ölü kod üçlüsü: çıkış · ebeveyn kaldırma · ligde görün. Üçü de yazılmış ama bağlanmamış |
| 8 | **Y-6 · Y-7 açıklaması** | Sözleşme ve SSS içeriği + "seviye birkaç gün içinde güncellenir" cümlesi. İçerik işi, kod az |
| 9 | **Y-8 · Y-9 · Y-10 · Y-12** | Geri bildirim eksikleri — hepsi "işlem oldu ama kullanıcı görmedi" |
| 10 | **Y-11** | KVKK yükümlülüğü; kapsam kararı gerekiyor (§12 A5 hâlâ açık) |
| 11 | **O-1 … O-18** | Yerleşim ve metin; toplu geçilebilir |
| 12 | **İ-1 … İ-5** | Kapsam kararı sonrası |

---

## §7 · Bu taramanın kapsamadıkları

- **Widget'lar** (`EkranSuresiWidget`, `GununSorusuWidget`) — ana ekrana eklenmedi.
- **Nudge overlay'in kendisi** — servis Y-9 sonrası ayağa kalktı ama tetikleyici eşiği
  tarama süresince oluşmadı; overlay hiç çizilmedi.
- **Aktif yürüyüş ekranı** — açılmadı; İ-3'teki davranış koddan okundu.
- **YEDAM onayı ekranı** — yalnızca `Bant.COK` iken erişilebiliyor; cihazda bant `AZ`.
- **Ebeveyn eşleştirmenin ötesi** — K-1 yüzünden rol seçiminin ötesine geçilemedi;
  K-8 koddan doğrulandı.
- **Backend'li senkron** — sunucu erişilemez (O-14), `POST /ozet` hiç denenemedi.
- **"Hesabımı sil" butonunun kendi yığın izi** — `SİL` onay metnindeki `İ` karakteri
  `adb input text` ile gönderilemedi; kod yolu "Verilerimi sil" ile birebir aynı.
- **Örnek veri oluştur** (debug tohumu) — gerçek veriyi bozmamak için çalıştırılmadı.
