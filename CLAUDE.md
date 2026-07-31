# CLAUDE.md — Yeşilİz

> **Proje bağlamı. Kod yazmadan önce oku.**
> Kararlar burada kayıtlı — tahmin etme, buradan bak.
> Araçtan bağımsızdır: Claude Code, Codex veya başka bir ajan bu dosyayla devralabilir.

---

## 1. HIZLI BAŞLANGIÇ

### Ortam

| Gereksinim | Değer |
|---|---|
| JDK | Temurin 17 · `JAVA_HOME=$(/usr/libexec/java_home -v 17)` |
| Android SDK | `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Emülatör | AVD adı `Yesiliz_Pixel7_API36` (Android 16 / API 36) |
| Python | 3.9+ · üreticiler için `pillow numpy scipy` |

`~/.zshrc` içinde `JAVA_HOME`, `ANDROID_HOME`, `PATH` ayarlı.

### Komutlar

```bash
# derle · kur · çalıştır
./gradlew :app:assembleDebug
./gradlew :app:installDebug
emulator -avd Yesiliz_Pixel7_API36 &

# testler  (toplam 230)
./gradlew :core-risk:test              # 22 · saf JVM, saniyeler
./gradlew :app:testDebugUnitTest       # 132 · saf JVM
./gradlew :app:connectedDebugAndroidTest   # 28 · emülatör gerektirir

# backend  (48 pytest)
cd backend && docker compose up -d && docker compose exec api pytest -q

# UsageStats izni — Ayarlar'dan verilir, adb ile de verilebilir.
# Verilmezse ilgili emülatör testleri Assume ile ATLANIR (kırmızıya dönmez).
adb shell appops set tr.org.yesilay.yesiliz android:get_usage_stats allow
```

### Üretilen dosyalar — elle düzenlenmez

| Dosya | Üreticisi |
|---|---|
| `tests/test_vectors.json` | `python3 tests/vektor_uret.py` |
| `app/src/main/assets/kategori_tablosu.json` | `python3 tools/kategori_tablosu_uret.py` |
| `DB.md` | `./gradlew :app:assembleDebug` → `python3 tools/db_dokuman_uret.py` |
| `app/src/main/res/drawable-nodpi/agac_*.png` | `python3 tools/agac_gorselleri_ayikla.py` (30 Tem 2026 6-evre seti; `agac_gorselleri_hazirla.py` yalnızca eski Stitch kaynaklı illüstrasyonlar için) |

`python3 tests/kontrol_stitch.py <klasör>` — üretilen Stitch ekranlarını DESIGN.md §7'ye göre denetler.

### Git çalışma düzeni

**`main`'e kesinlikle doğrudan push atılmaz.** Herkes kendi branch'inde
çalışır (`feature/<ad>` veya `fix/<ad>`, `dev`'den dallanır), işi bitince
o branch'e push edip Pull Request açar. **Merge işlemini yalnızca
`tchkechagia0` yapar.**

Bu, kod yazan ajanlar için de geçerli — ekip vibe coding ile çalışıyor,
yani bir ajan kullanıcı adına git komutu çalıştırabiliyor. **Hiçbir ajan
`main`'e (ya da `dev`'e) doğrudan commit/push/merge yapmamalı**; bir ajana
"şu PR'ı incele, sorun yoksa merge et" gibi bir yetki açıkça verilmedikçe,
varsayılan davranış kendi feature/fix branch'inde çalışıp push etmek,
merge kararını `tchkechagia0`'a bırakmaktır.

```
main   ──────────●──────────────●────────  (yalnızca release/demo noktaları, sadece tchkechagia0 merge eder)
                  \              /
dev    ───●───●────●───●────●──●─────────  (PR ile, entegrasyon)
           \       \        /
feature/x   ●──●──●
fix/y             ●────●───
```

---

## 2. DOSYA HARİTASI

| Dosya | İçerik | Ne zaman |
|---|---|---|
| **CLAUDE.md** | Kurallar, mimari, kararlar | her oturum |
| **DESIGN.md** | Tasarım sistemi, tokenlar, 19 ekran speki | UI yazarken |
| **DB.md** | Şema, kolonlar, SQLite/PostgreSQL/MSSQL DDL | veritabanına dokunurken |
| **debug.md** | Açık kusur listesi (gerçek cihaz taraması + kullanım notları) + düzeltme sırası | bug düzeltmeden önce |
| `docs/spec-risk.md` | Risk skoru + bantlama tam speki | core-risk'e dokunurken |
| `docs/spec-anket.md` | SAS-SV ölçeği, 10 madde, Likert | ankete dokunurken |
| `docs/spec-puan.md` | Görev · puan · lig · ödül | puan sistemine dokunurken |
| `docs/spec-veri.md` | Veri stratejisi, 18 yaş altı ebeveyn modu | veri politikasına dokunurken |
| `docs/referans/yesiliz_hesap.py` | Risk motorunun **referans** implementasyonu | **DOKUNULMAZ** — spek |
| **`docs/tasarim-referans.md`** | **Teslim edilen 31 ekranın (19 + 12 ek) spek'i + tokenlar** | **UI yazarken — ekranların kaynağı** |
| `docs/stitch-ekranlar.md` | DESIGN §5.X ↔ Stitch ekran ID eşlemesi | tarihsel — yeni ekran için kullanma |
| `docs/spike-usagestats-sonuc.md` | UsageStats ortam bulguları | veri okumaya başlarken |

---

## 3. PROJE

**Yeşilİz** — telefon/ekran bağımlılığını erken tespit edip kullanıcıyı sağlıklı
kullanıma ve gerçek hayat aktivitelerine yönlendiren Android uygulaması.

| | |
|---|---|
| Bağlam | TEKNOFEST 2026 · "Bağımlılıklarla Mücadelede Teknolojik Uygulamalar" |
| Partner | Yeşilay |
| Rapor | 8 Ağustos 2026 |
| Final | 30 Eylül – 4 Ekim 2026, Şanlıurfa |
| Puanlama | işlevsellik %25 · teknik %20 · problem örtüşmesi %20 · UX %15 · özgünlük %10 · sunum %10 |

> **Çalışan uçtan uca döngü, yarım kalmış çok özellikten değerli.**

### Çekirdek döngü

| # | Adım | Ne yapar |
|---|---|---|
| 1 | İZLE | ekran + uygulama kullanımı, aktivite, uyku |
| 2 | DEĞERLENDİR | hibrit risk skoru (anket + davranış) |
| 3 | YÖNLENDİR | hafif bildirimsel nudge (3-5 sn), zorlama yok |
| 4 | ÖDÜLLENDİR | görev + lig + gerçek dünya ödülleri |
| 5 | KÖPRÜ KUR | yüksek risk + izin → YEDAM yönlendirmesi |

---

## 4. MUTLAK KURALLAR

| Yasak | Sebep |
|---|---|
| **Teşhis koyma** | "Bağımlısın" ASLA. Betimleyici dil: "son 7 günde gece kullanımın arttı" |
| **Uygulama kilitleme** | Direnç doğuruyor, kalıcı etkisi zayıf. Sadece nudge. |
| **İçerik okuma** | Mesaj, foto, rehber, arama geçmişi HİÇ toplanmaz. Sadece süre/sıklık/kategori. |
| **Ham veriyi sunucuya gönderme** | Çıkarım ve skorlama CİHAZDA. Sunucuya günlük özet + skor. |
| **Konum/GPS** | Gerekli değil, mahremiyet yükü ağır. |
| **Serbest metin chatbot** | İlk sürüm kapsamı dışı (`docs/spec-veri.md`) |
| **Ceza puanı** | Sadece kazanç. Ceza suçluluk üretir, uygulamayı sildirir. |
| **Aktiviteyi skordan düşme** | "Koş → istediğin kadar kaydır" tuzağı. Sönümleme tavanı %15. |
| **Emir kipi / korkutma** | Yeşilay marka kılavuzu yasaklıyor. |
| **Kırmızı renk** | Damgalayıcı. Risk bandı koyulaşan yeşille gösterilir. Hata durumları da yeşil. **İstisna (K10):** risk analizi ekranındaki "Yüksek" etiketi/çubuğu ve hesap/veri kalıcı silme onayı — teslim edilen tasarım burada kırmızı kullanıyor, kullanıcı onayıyla aynen uygulanıyor. Başka hiçbir yerde geçerli değil. |

**18 yaş altı:** ebeveyn onayı zorunlu, KVKK ayrı rejim.

---

## 5. TEKNOLOJİ

**Android-first, native Kotlin.** 

| Katman | Seçim | Not |
|---|---|---|
| Yapı | AGP 9.3.1 · Gradle 9.6.1 · Kotlin 2.4.10 · JDK 17 | compileSdk/targetSdk 37 · minSdk 26 |
| UI | Jetpack Compose + Material 3 | tek Activity, NavHost |
| Font | Inter 400/500/600/700 | `res/font/`, SIL OFL |
| Yerel DB | Room 2.8.4 + KSP 2.3.10 | ham olaylar cihazda kalır |
| Arka plan | WorkManager + Foreground Service | |
| Widget | Glance | |
| Grafik | **Custom Compose Canvas** | 3. parti chart lib YOK — hazır kütüphaneler kendi paletiyle gelir |
| İkon | `material-icons-extended` | özgün illüstrasyonlar Stitch'ten PNG/vektör |
| Ağ | Retrofit 3 + OkHttp + kotlinx-serialization | `data/uzak/` — yazıldı (31 Tem). Retrofit 3 kendi converter'ını getirmiyor, köprü ayrı bağımlılık |
| Backend | FastAPI (Python) | ASP.NET DEĞİL — ML aynı dilde |
| Sunucu DB | PostgreSQL | Redis MVP'de yok |
| ML (faz 3) | scikit-learn, XGBoost, SHAP | TensorFlow/PyTorch yok |


---

## 6. MİMARİ

### Modüller

```
yesiliz/
├─ app/          Android uygulaması
├─ core-risk/    SAF KOTLIN (JVM) — android.* import YASAK
├─ backend/      FastAPI + PostgreSQL — hesap, senkron, lig, ebeveyn
├─ docs/         spec'ler + referans Python
├─ tests/        doğrulama vektörleri + üreticiler
└─ tools/        veri/görsel/doküman üreticileri
```

**core-risk neden ayrı:** Android sınıfı sızarsa saf fonksiyon garantisi bozulur;
ayrı JVM modülünde derleyici bunu fiziksel olarak engeller. Testler emülatörsüz koşar.

### Paket düzeni

```
core-risk/…/risk/
├─ Sabitler.kt      TÜM sabitler. Kalibrasyonda SADECE burası değişir.
├─ Girdi.kt         GunlukGirdi · AnketGirdi · GecmisPencere · GunlukOzet
├─ Cikti.kt         RiskSonuc · Bant · Trend · GunSonuc
├─ RiskHesapla.kt   hesap sırası, saf fonksiyon
├─ Alpha.kt         alpha eğrisi + dEfektif
└─ Bantlama.kt      eşik → güvenlik → histerezis → trend + BantTakipci

app/…/yesiliz/
├─ MainActivity.kt · YesilizApp.kt
├─ di/                    Hilt — VeriModulu · AgModulu · DepoModulu
├─ data/
│  ├─ kullanim/           UsageStatsManager sarmalayıcı
│  ├─ saglik/             Health Connect
│  ├─ yerel/              Room: entity/ dao/ YesilizDb.kt Donusturucu.kt Gecisler.kt
│  ├─ uzak/               Retrofit — YesilizApi · Dto · JetonDeposu
│  ├─ tercih/             DataStore
│  └─ depo/               Repository — UI buradan okur.
│                         Yerel: Kategori · Kullanim · Risk · Puan · Bildirim ·
│                         Bilgi · Beyan · GecmisVeri · DisaAktar · VeriYonetimi
│                         Uzak: RetrofitHesap · RetrofitLig · Senkron · Ebeveyn
│                         Sarmalayıcı: YedekliLigDeposu · YedekliHesapDeposu
├─ domain/
│  ├─ model/              UI'ın gördüğü modeller
│  ├─ kategori/           Kategori · KategoriEsleyici · KategoriTablosu
│  └─ kullanim/           KullanimOlay · OruntuCikar · YerDegisHesapla
├─ ui/
│  ├─ tema/               Renk · Tipografi · Sekil · Boyut · Tema
│  ├─ TurkceMetin.kt      buyukHarf() / kucukHarf()
│  ├─ EkranAcikTut.kt     FLAG_KEEP_SCREEN_ON sarmalayıcı (nefes, yürüyüş)
│  ├─ bilesen/            AyarSatiri · BosDurum · Cip · IlerlemeCubugu · Kart
│  │  │                   KodKutusu · KullaniciAvatari · LikertSecici · OnayKutusu
│  │  │                   RiskGirisKarti · SallananYapraklar · UstBaslik ·
│  │  │                   YaprakZemini · YesilizButon
│  │  └─ veri/            CubukGrafik · CubukSecim · DaireselGosterge · YiginBar
│  ├─ gezinme/            NavHost, rotalar
│  └─ ekran/              onboarding ana istatistik nefes riskdetay
│                         lig odul ebeveyn profil hesap hedef kategori
│                         bilgi test bildirim anket yuruyus saglik yedam
├─ nudge/                 Tetikleyici.kt · NudgeSabitleri.kt · NudgeDurum.kt ·
│                         NudgeOverlay.kt · NudgeOverlayService.kt · NudgeIzni.kt
├─ arkaplan/              GunlukOzetWorker · HatirlatmaWorker · IsPlanlayici
└─ widget/                Glance — EkranSuresiWidget · GununSorusuWidget ·
                          WidgetVerisi (@EntryPoint) · WidgetEylemleri
```

Her ekran klasörü **üç dosya**: `<Ad>Ekran.kt` · `<Ad>ViewModel.kt` · `<Ad>Durum.kt`.
İstisna: `nefes/` dört dosya — `NefesFazHesapla.kt` saf faz/tempo hesabını
ViewModel'den ayırıp emülatörsüz test edilebilir kılıyor (`OturumCikar`/
`Tetikleyici` ile aynı desen). Ekran mantığı gerçekten saf bir alt-hesaba
ayrıştırılabiliyorsa bu istisna kabul edilir; kural genel olarak geçerli.

> Paket adları Türkçe olabilir ama Kotlin anahtar kelimesi olamaz —
> `is in as for object val var when` yasak. Bu yüzden "iş" değil `arkaplan`.

### Veri akışı — tek yön

```
UsageStatsManager / Health Connect
   → data/kullanim · data/saglik
   → Room  (ham olaylar, CİHAZDAN ÇIKMAZ)
   → data/depo  (günlük özet)
   → core-risk  (saf fonksiyon)
   → domain/kullanim  (use case)
   → ViewModel  (StateFlow)
   → Composable
```

**Kural:** Composable asla `data/`'ya erişmez. ViewModel asla Context tutmaz.

### Mock repository deseni — desen tuttu

`domain/`'de `LigDeposu` / `HesapDeposu` **arayüzleri** var. MVP'de
`YerelSahteLigDeposu` / `YerelSahteHesapDeposu` sahte veriyle uyguluyordu;
backend gelince `RetrofitLigDeposu` / `RetrofitHesapDeposu` aynı arayüzü
sunucuya bağladı. **Vaat edilen tam olarak gerçekleşti:** yalnızca
`di/DepoModulu.kt` değişti, hiçbir ViewModel veya Composable'a dokunulmadı.

Bugün bağlanan şey ikisinden biri değil, **sarmalayıcı**: `YedekliLigDeposu`
ve `YedekliHesapDeposu` önce Retrofit'i deniyor, **ağ hatasında** yerel sahteye
düşüyor (§12 K18). İş hatası (401 yanlış şifre, 409 e-posta kayıtlı) olduğu
gibi geçiyor — yoksa yanlış şifreyle "giriş yaptın" demiş olurduk.

### Sunucu ile senkron

FastAPI backend (`backend/`) dört alanı karşılıyor — uç listesi
`data/uzak/YesilizApi.kt`'de:

| Alan | Uçlar |
|---|---|
| Hesap | `POST auth/kayit` · `auth/giris` · `auth/yenile` · `auth/sifre-sifirla` |
| Özet | `POST ozet` · `GET ozet` · `POST heartbeat` |
| Lig | `GET lig` · `POST lig/katil` · `GET arkadas` · `GET arkadas/kod` · `POST arkadas/davet` |
| Ebeveyn | `POST ebeveyn/kod` · `POST ebeveyn/eslestir` · `GET ebeveyn/ozet` |

İki kural pazarlık edilemez:

- **Sunucu skor hesaplamıyor** (§12 K17). Cihaz hesaplıyor, sunucu saklıyor ve
  ebeveyn/lig için okuyor. İkinci bir motor `core-risk`'in referans Python ile
  birebir doğrulanmışlığını bozardı.
- **Senkron rıza kapısına bağlı** (§12 K19). "Yeşilay ile paylaşım" kapalıysa
  ağ çağrısı hiç yapılmıyor; misafir kullanıcı da senkronize edilmiyor.

Ham olay sunucuya **gitmiyor** — giden şey günlük özet + skor (§4).

Kimlik doğrulama: parola `argon2id` ile özetleniyor (`backend/app/guvenlik.py`),
istemciye JWT erişim + yenileme jetonu dönüyor. Jetonlar `JetonDeposu`'nda,
`Tercihler`'den **ayrı** bir DataStore'da (`name = "jetonlar"`) — "verilerimi sil"
akışı jetonları tek başına temizleyebilsin diye.

> **Bu temizleme henüz yazılmadı.** `VeriYonetimiDeposu` yalnızca Room'u ve
> `Tercihler`'i siliyor, `JetonDeposu`'na hiç dokunmuyor; üstelik silme akışının
> kendisi ana iş parçacığında `clearAllTables()` çağırdığı için çöküyor.
> Ayrıntı: `debug.md` K-2.

### Veritabanı

Tam şema `DB.md`'de. **`fallbackToDestructiveMigration` kaldırıldı**
(31 Tem 2026): `data/yerel/Gecisler.kt` gerçek `Migration` nesneleri
taşıyor ve `GecisTest` emülatörde doğruluyor.

> **Entity değiştiren her PR bir `Migration` de yazmak zorunda.** Yazılmazsa
> uygulama açılışta çöküyor — sessizce kullanıcının geçmişini silmektense
> gürültülü başarısız olmak yeğ.

**Şu an 12 entity · 12 DAO, şema sürümü 2.** `GECIS_1_2` (`Gecisler.kt`)
`bildirim` ve `arkadas` tablolarını ekliyor.

Bir zamanlar planlanan `LigUyelikEntity` ve `EbeveynBaglantiEntity` **yazılmadı
ve yazılmayacak.** Lig üyeliği ve ebeveyn bağlantısı doğası gereği sunucu
durumu — haftalık grup ataması ve eşleştirme kodunun geçerliliği cihazda
doğrulanamaz. Cihaz tarafında yalnızca `arkadas` tablosu var; gerisi
`RetrofitLigDeposu` / `EbeveynDeposu` üzerinden sunucudan geliyor, sunucuya
ulaşılamazsa `YerelSahteLigDeposu` devreye giriyor.

**Room'a girmeyenler → DataStore.** Tekil tercih değerleri tablo istemez:

| Değer | Niçin saklanır |
|---|---|
| bildirim saatleri | nudge zamanlaması |
| leaderboard opt-out | lig ekranında görünme tercihi |
| Yeşilay paylaşım onayı | veri paylaşım rızası |
| ilk kurulum tarihi | alpha eğrisinin `d` girdisi |
| son anket tarihi | alpha eğrisinin `g` girdisi |

---

## 7. KOD KONVANSİYONLARI

- Yorum ve değişken adları Türkçe olabilir; API/model isimleri İngilizce
- Sabitler tek yerde: `core-risk/Sabitler.kt`
- Risk motoru **saf fonksiyon** — girdi → çıktı, yan etki yok
- Kotlin, referans Python ile **birebir aynı sonucu** vermeli
- Her risk hesabı `surukleyiciler` döndürmeli (açıklanabilirlik)

### Yeni dosya nereye?

| Ne yazıyorsun | Nereye |
|---|---|
| Risk/bant hesabı | `core-risk/` — başka hiçbir yere |
| Sabit, eşik, katsayı | `core-risk/Sabitler.kt` |
| Android API'sine dokunan kod | `app/data/` |
| Ekran | `app/ui/ekran/<ad>/` — üç dosya birden |
| Çok yerde kullanılan görsel | `app/ui/bilesen/` |
| Renk, ölçü, radius | `app/ui/tema/` — asla ekran içinde |
| Arka plan işi | `app/arkaplan/` |

### İsimlendirme

| Tür | Kalıp |
|---|---|
| Ekran | `<Ad>Ekran.kt` `<Ad>ViewModel.kt` `<Ad>Durum.kt` |
| Bileşen | isim, "Composable" eki yok → `GorevKarti` |
| Room | `<Ad>Entity.kt` `<Ad>Dao.kt` |
| Worker | `<Ad>Worker.kt` |
| Test | `<SınıfAdı>Test.kt` |

### Yasaklar

- `core-risk` içinde `android.*` import
- `uppercase()` / `lowercase()` çıplak çağrı → §9
- Composable içinde hardcoded renk/dp
- Risk hesabını ViewModel içinde yapmak
- Room entity'sini doğrudan UI'a vermek
- Magic number

### Test kuralı

`core-risk`'te yazılan her fonksiyonun `tests/test_vectors.json`'da karşılığı
olmalı. Vektörü olmayan hesap kodu merge edilmez.

---

## 8. UI İŞ AKIŞI

**Tasarım dışarıdan geliyor.** 29 Temmuz 2026'da 19 ekranlık teslim alındı:
`Yeşilİz Mobil Arayüz-selection.png`. 30 Temmuz 2026'da aynı dosyanın
güncellenmiş sürümüyle **12 ekran/varyasyonluk ek teslim** geldi (ebeveyn
eşleştirme akışı, veri şeffaflığı/hesap silme ekranları, YEDAM hakkında,
Yardım, Hakkında ve — sıradaki iş listesindeki #2'yi karşılayan — Risk
analizi ekranı). Ekran ekran spek ve tokenlar **`docs/tasarim-referans.md`**'de,
kesilmiş ekranlar (31 adet) `docs/tasarim/ekranlar/*.png`'de.

| # | Adım |
|---|---|
| 1 | Ekran yazılacağı zaman `docs/tasarim-referans.md §2`'deki ilgili madde ve `docs/tasarim/ekranlar/<ad>.png` okunur |
| 2 | Compose kodu §7 konvansiyonlarına göre elle yazılır — token, ölçü ve metin tasarımdan; yapı `ui/tema` + `ui/bilesen`'den |
| 3 | Teslimde olmayan ekranlar (`nudge`, görev detay, YEDAM onayı, widget, aktif yürüyüş) için `stitch_markdown_design_system/` yalnızca **yerleşim** referansı — tonlar ve tipografi taşınmaz. Eşleme `docs/tasarim-referans.md §4`'te |
| 4 | İllüstrasyonlar `tools/agac_gorselleri_ayikla.py` (ağaç evreleri) veya `tools/agac_gorselleri_hazirla.py` (diğer Stitch kaynaklı illüstrasyonlar) ile temizlenip `res/drawable-nodpi/`'ye konur |
| 5 | Widget ve nudge overlay gerçek implementasyonu Glance/Compose ile yazılır, taslak sadece hizalama referansı |

**`DESIGN.md` hâlâ tasarım sisteminin (palet, skala, ilke) kaynağı;
`docs/tasarim-referans.md` ekranların kaynağı.** İkisi çelişirse teslim edilen
tasarım kazanır — palet zaten birebir uyuşuyor, teslim yalnızca token *ekledi*.

> `DESIGN.md §5` Stitch prompt'ları ve `docs/stitch-ekranlar.md` **tarihsel
> kayıt**. Yeni ekran üretmek için kullanılmaz.

---

## 9. BİLİNEN TUZAKLAR

Hepsi bir kez yaşandı. Tekrarlama.

| Tuzak | Ne olur | Çözüm |
|---|---|---|
| **AGP 9 + kotlin.android** | `Failed to apply plugin` — build çöker | AGP 9'da Kotlin desteği içeride. `app` yalnızca `com.android.application` + `kotlin.plugin.compose` uygular. `core-risk` saf JVM olduğu için `kotlin.jvm` kullanmaya devam eder. |
| **compileSdk 36** | Bağımlılık çözümlemesi reddeder | AndroidX 2026 sürümleri (core-ktx 1.19, lifecycle 2.11) **37** şart koşuyor |
| **Çıplak `uppercase()`** | "TIPOGRAFI", "RISK BANDI" — bozuk Türkçe | `Locale.ROOT` `i→I` veriyor, Türkçede `İ` olmalı. `ui/TurkceMetin.kt` → `buyukHarf()` |
| **Test girdisi bildirilmemiş** | Veriyi bozarsın, test yeşil kalır | `systemProperty` yetmez; dosya ayrıca `inputs.file()` ile bildirilmeli yoksa Gradle testi UP-TO-DATE sayıp atlar |
| **Histerezis sayacı sıfırlama** | "Daha çok iyileştiğin için baştan başla" cezası | Sayaç yalnızca YÖN değişince sıfırlanır. Regresyon vektörü: `histerezis.TUZAK_ayni_yon_sayac_sifirlanmaz` |
| **Uyku = en uzun boşluk** | Telefonsuz geçen iş günü uyku sanılır | Boşluğun orta noktası yerel 22:00–08:00 arasına düşmeli |
| **Stitch "transparent background"** | Damalı desen resmin İÇİNE çizilir | `tools/agac_gorselleri_hazirla.py` temizliyor |
| **MSSQL `NVARCHAR(MAX)` PK** | İndekslenemez, tablo oluşmaz | PK/indeks kolonları `NVARCHAR(450)` |
| **Hilt kökte, KSP yalnızca `:app`'te** | `KSP plugin was detected to be applied but its task class could not be found` | KSP de kökte `apply false` ile bildirilmeli — ikisi aynı sınıf yükleyicide olmalı (google/dagger#3965) |
| **Hilt 2.59 + Kotlin 2.4** | `Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0` | Hilt **2.60.1**. Kotlin sürümü yükselirse Hilt de yükselmeli — işlemci `kotlin-metadata-jvm` paketliyor |
| **Gün çözünürlüklü `ORDER BY`, eşitlik bozucusuz** | `LocalDate` kolonuna göre `... DESC LIMIT 1` aynı güne düşen iki satırda **eskisini** veriyor; SQLite eşitlikte sıra garanti etmiyor, pratikte `rowid` sırası geliyor. Anket aynı gün yenilenince risk hiç değişmiyordu | Tarihe göre sıralayan her sorgu `id`'yi de eşitlik bozucu olarak almalı: `ORDER BY tarih DESC, id DESC` |
| **Health Connect gerekçe bildirimi yok** | İzin diyaloğu **sessizce** izinsiz dönüyor, hata vermiyor. Emülatörde HC kurulu olmadığı için hiç görülmüyor | Manifest'te iki bildirim birden: Android 13 ve altı için `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` intent-filter'ı, 14+ için `VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS` `activity-alias`'ı |
| **`enableEdgeToEdge` + sabit yükseklikli alt çubuk** | `NavigationBar`'a çıplak `height(72.dp)` verilince kendi sistem çubuğu payı bu 72dp'nin **içinden** düşülüyor. 3 tuşlu gezinmede (`navigation_mode=0`, 48dp pay) sekmelere 24dp kalıyor: ikon+etiket eziliyor, dokunma hedefi tıklanamayacak kadar küçülüyor. Emülatörde jest gezinmesi varsayılan olduğu için (24dp) fark edilmiyor | Yükseklik = tasarım yüksekliği **+** `WindowInsets.navigationBars` alt payı (`AltGezinme.kt`) |
| **Alt sistem çubuğu payının hiç verilmemesi** | Ekranların dibindeki buton/satır sistem gezinme çubuğunun altında kalıyor. Üst tarafta pay vardı (`statusBarsPadding`, `windowInsetsTopHeight`), alt taraf hiçbir ekranda yoktu | Pay ekranın **kendi zemininin içine** konur (`background(...).navigationBarsPadding()`) — zemin rengi çubuğun arkasına uzanmaya devam etsin. Ortak iskelet `YesilBaslikliEkran` bunu 17 ekran için tek yerden veriyor; `YesilizGezinme` `consumeWindowInsets(bosluk)` ile alt çubuklu sekme ekranlarında payı tüketiyor ki iki kez eklenmesin. `ModalBottomSheet` M3 1.3'ten beri kenardan kenara — `contentWindowInsets` açıkça verilmezse alt sayfanın son butonu çubuğun altında kalıyor |
| **Yenileme jetonu saklanıp hiç kullanılmaması** | Erişim jetonu dolunca (30 dk) her kimlikli çağrı 401 veriyor ve `Yedekli*Deposu` sessizce sahte veriye düşüyor — kullanıcı hâlâ "giriş yapmış" görünüyor | `okhttp3.Authenticator` (`JetonAuthenticator`). Yenileme çağrısı **ayrı** bir istemciden gitmeli, yoksa Dagger döngüsü + sonsuz 401 özyinelemesi |

---

## 10. PLATFORM KISITLARI — PAZARLIK EDİLEMEZ

| Yetenek | Android | iOS |
|---|---|---|
| Uygulama kimliği | ✅ gerçek paket adı | ❌ opak token |
| Kategori | ✅ otomatik (tablo) | ⚠️ kullanıcı beyanı |
| Kurulumda geçmiş | ✅ haftalar (hacim) | ❌ sıfır |
| Veri çözünürlüğü | ✅ dakika | ⚠️ kuantize |
| Ham veriyi sunucuya | ✅ | ❌ Apple izin vermiyor |
| Müdahale ekranı | ✅ overlay izni | ✅ `ShieldConfiguration` |
| Dağıtım engeli | standart | ⚠️ Family Controls entitlement başvurusu |

**KARAR: prototip Android üzerinden gösterilir.** iOS analiz katmanı kısıtlı;
müdahale katmanı iki platformda da çalışır. iOS kategori picker tasarımı
prototip kapsamı dışı: kategori başına ayrı picker, örüntü denetimiyle desteklenir.

> **Emülatör uyarısı:** taze emülatörde 0 üçüncü parti uygulama var, `dumpsys
> usagestats` yalnızca açılıştan beri veri gösteriyor. Yani `d_efektif = 0`,
> `alpha = 1.0` — alpha eğrisinin hiçbir kolu test edilemez, risk motoru hep
> `S≈0` üretir. **Döngü doğrulaması gerçek telefon ister.**
> Ayrıntı: `docs/spike-usagestats-sonuc.md`
>
> **Yapıldı (31 Tem 2026):** Samsung SM-A217F / Android 12 üzerinde zincirin
> tamamı ilk kez gerçek veriyle koştu — `gunluk_ozet` doldu, `risk_sonuc`
> gerçek skor üretti (`S=10,88 · rS=81,97 · R=41,47`). Emülatörde görünmeyen
> kusurlar da ilk kez buradan çıktı: `debug.md`.

---

## 11. DURUM

### Biten

| Alan | İçerik | Doğrulama |
|---|---|---|
| Araç zinciri | JDK 17 · Android Studio · SDK 37 · AVD | emülatör açılıyor |
| `core-risk/` | 6 dosya — risk motoru tam | **22 test**, referansla birebir |
| `app/ui/tema/` | Renk · Tipografi · Sekil · Boyut · Tema + Inter + hilal ikonu | emülatörde görüldü |
| `app/domain/kategori/` | 3.106 paketlik tablo + eşleyici | **9 test** |
| `app/domain/kullanim/` | `OruntuCikar` — 6 örüntü + `YerDegisHesapla` | **22 test** |
| `app/data/yerel/` | 12 entity · 12 DAO · `YesilizDb` (sürüm 2) · `Gecisler.kt` — destructive fallback **yok** | **8 + 1 emülatör testi** (`GecisTest` gerçek 1→2 geçişini koşuyor) |
| `app/data/kullanim/` | `OturumCikar` (saf) · `KullanimOkuyucu` · `KullanimIzni` | **13 birim + 7 emülatör testi** |
| `app/data/depo/` | `KategoriDeposu` · `KullanimDeposu` (sistem → kategori → Room → özet) | seed + override korunuyor |
| Dokümantasyon | `DB.md` · `docs/stitch-ekranlar.md` · spike raporu | üreticileri var |
| Tasarım | **Dışarıdan 31 ekranlık teslim** (29 Tem + 30 Tem ek 2026) | `docs/tasarim-referans.md` · palet DESIGN.md §1 ile birebir |
| `app/ui/bilesen/` | Kart · Cip · YesilizButon · IkonRozeti · AyarSatiri · LikertSecici · KullaniciAvatari · UstBaslik · IlerlemeCubugu + `veri/` CubukGrafik · DaireselGosterge · YiginBar | emülatörde görüldü |
| `app/ui/gezinme/` | NavHost · 4 sekmeli alt bar · onboarding alt grafiği · çift gezinme koruması | akış uçtan uca yürüdü |
| `app/ui/ekran/` | onboarding (7) · ana (+ görev detayı alt sayfası) · istatistik · riskdetay · lig · profil (+6 alt sayfa) · ödül (+3 alt sayfa) · ebeveyn · nefes | emülatörde ekran ekran doğrulandı |
| `app/di/` | Hilt — `VeriModulu`, `@HiltAndroidApp`, `HiltWorkerFactory` | uygulama açılıyor |
| `app/data/tercih/` | DataStore: hedef · onboarding bayrağı · kurulum tarihi · gizlilik | yeniden başlatmada korunuyor |
| `app/data/depo/RiskDeposu` | **hesap zinciri** — anket + `d` → skor → güvenlik → histerezis → trend → `risk_sonuc` | **8 emülatör testi** |
| `app/data/depo/PuanDeposu` + `domain/puan/` | görev · seri · çarpan · günlük tavan | **7 birim testi** |
| `app/arkaplan/` | `GunlukOzetWorker` (gecelik + 7 gün telafi) · `IsPlanlayici` | emülatörde 8 gün telafi etti |
| `app/data/saglik/` | Health Connect — adım + egzersiz; yoksa `D = 1.0` | HC'siz cihazda sessiz |
| `app/nudge/` | Tetikleyici (tüketim bloğu, saf) + `NudgeOverlayService` (canlı izleme, `WindowManager` overlay, blur API 31+) + `NudgeOverlay` UI + `NudgeIzni` | **8 birim testi**; Profil'de "Mola hatırlatıcı" anahtarı |
| `ui/ekran/ana/AnaEkran` görev detayı | Alt sayfa (`GorevDetayi`) — tüm 7 görev için ortak; puan/süre/doğrulama/limit çipleri, göreve göre eylem | `AnaViewModel.gorevTiklandi` artık gerçekten çağrılıyor |
| `ui/ekran/nefes/` | Kutu nefesi tam ekran — `NefesFazHesapla` (saf faz/tempo hesabı) + `NefesViewModel` (sayaç, 4/5/6 sn tempo) + `NefesEkran` (animasyonlu daire) | **8 birim testi**; tamamlanınca `Gorev.NEFES` puanı veriyor |
| `app/data/depo/GecmisVeriDeposu` | Kurulumda geçmiş veri — `queryAndAggregateUsageStats` ile izin onaylandıktan sonra bir kez `Tercihler.gecmisGunSayisi`'ni gerçek sayıyla günceller | **4 birim testi**; gerçek sayı yalnızca gerçek cihazda doğrulanabilir (§10) |
| `domain/test/` + `ui/ekran/test/` | Kısa farkındalık testi (5 soru) — **SAS-SV değil** (§12 K12) | **9 birim testi** |
| `domain/bilgi/` + `ui/ekran/bilgi/` | Bilgi Bankası — `assets/bilgi_bankasi.json` (13 yazı, çevrimdışı), liste + yazı ekranı | **6 birim testi** |
| `domain/bildirim/` + `data/depo/BildirimDeposu` + `ui/ekran/bildirim/` | Uygulama içi bildirim defteri; ana ekran başlığında okunmamış noktalı zil | **8 birim testi** |
| `ui/ekran/hedef/` | Günlük hedef (ekran 34) — halka, −/+, hızlı seçim; taban 120 dk | Profil › Hedeflerim |
| `ui/ekran/kategori/` | Uygulama kategorileri + Kategorize et + kategori seç (31, 32, 33) | `KategoriDeposu.kullaniciSecti`'ye bağlı |
| `ui/ekran/anket/` | SAS-SV yeniden değerlendirme + 4 hafta hatırlatması | madde metni `SasSv`'den paylaşılıyor |
| `ui/ekran/yuruyus/` | Aktif yürüyüş — `YuruyusHesapla` (saf) + canlı sayaç | **7 birim testi** |
| `ui/ekran/saglik/` | Health Connect izin akışı (tasarımı yok, ekran 02 uyarlandı) | HC yoksa Play Store yönlendirmesi |
| `ui/ekran/yedam/` | YEDAM onayı (rıza/köprü) — yalnızca `Bant.COK`, onay kutusu zorunlu | risk detayındaki öneri kartından |
| `ui/ekran/lig/` + `domain/lig/` + `YerelSahteLigDeposu` | Lig boş durumu (37) + Arkadaş ekle (36); `LigDeposu` arayüzü artık gerçekten var | **6 birim testi** (`DavetKodu`) |
| `ui/ekran/hesap/` + `domain/hesap/` | Giriş · Kayıt · Şifremi unuttum (14, 15, 38); şifre hiçbir yere yazılmıyor | **7 birim testi** |
| `app/widget/` | Glance: `EkranSuresiWidget` (40) + etkileşimli `GununSorusuWidget` (39) | manifest receiver'ları; emülatörde eklendi |
| `arkaplan/HatirlatmaWorker` | Günlük hatırlatma (Profil › Bildirim saatleri), varsayılan kapalı | `BildirimSaatleri` saf çözümleme |
| `backend/` | FastAPI + PostgreSQL + Alembic — hesap (JWT), özet senkronu, lig, ebeveyn eşleştirme; `docker compose up -d` ile ayağa kalkıyor | **48 pytest**; `backend/README.md` çalıştırma + mahremiyet sözleşmesi |
| `app/data/uzak/` | `YesilizApi` (16 uç) · `Dto` · `JetonDeposu` (erişim/yenileme jetonu) · `AgModulu` (OkHttp interceptor) · `JetonAuthenticator` + `JetonYenileyici` — **401'de sessiz yenileme** | uçtan uca emülatörde doğrulandı; **7 birim testi** |
| `app/data/depo/` uzak taraf | `RetrofitHesapDeposu` · `RetrofitLigDeposu` · `SenkronDeposu` (rıza kapılı, §12 K19) · `EbeveynDeposu` (kod sunucuda doğrulanıyor) | rıza kapalıyken `POST /ozet` hiç görülmüyor, açılınca 200 |
| `app/data/depo/YedekliDepolar` | Offline-first sarmalayıcı — ağ hatasında yerel sahteye düşer, iş hatasında (401/409) düşmez (§12 K18) | backend kapalıyken lig sekmesi yine dolu |

**Toplam 230 test** (154 Android birim + 28 emülatör + 48 backend pytest). Zincir uçtan uca çalışıyor:
`UsageStats → Room → core-risk → risk_sonuc → StateFlow → Composable`.
Ekranlardaki sabit veri **silindi**; veri yokken boş durum gösteriliyor.
Tıklayınca hiçbir şey yapmayan buton/satır kalmadı.

**Hâlâ bilinçli sahte olanlar:** önerilen kişiler listesi (rehber okunmuyor —
§12 K14) ve **backend kapalıyken** lig sıralaması/lig adı (§12 K18 sarmalayıcısı
`YerelSahteLigDeposu`'na düşüyor). Sunucu ayaktayken lig, hesap, özet senkronu
ve ebeveyn eşleştirmesi gerçek. Kullanıcı adı da gerçek: hesap ekranından ya da
misafir modunda sabitten geliyor.

> **Demo günü uyarısı:** lig ekranındaki veri sunucu ayakta mı diye bakmadan
> yorumlanmamalı — sahte veri sessizce devreye giriyor, bu bilinçli bir tasarım
> kararı (§12 K18) ama demo sırasında "backend çalışıyor" diye gösterilemez.

**Demo tohumu:** `app/src/debug/` kaynak setinde — release derlemesine
**giremiyor**. Profil → Geliştirici → "Örnek veri oluştur". Tohum yalnızca
`gunluk_ozet` *girdilerini* yazıyor; skoru ve bandı gerçek motor üretiyor.

### Açık işler

**Bulgular ve düzeltme sırası `debug.md`'de.** 31 Temmuz 2026'da gerçek cihazda
(Samsung SM-A217F · Android 12) uçtan uca tarama yapıldı ve kullanıcının kendi
kullanım notlarıyla birleştirildi: zincir çalışıyor, ama **9 kritik + 12 yüksek +
18 orta** kusur ve 5 kapsam kararı açık. En ağırı, "Verilerimi/Hesabımı sil"in
uygulamayı çökertip hiçbir şey silmemesi (`debug.md` K-2). Yapılacak iş listesi
orada tutuluyor — burada tekrarlanmıyor.

Taramanın dışında kalan iki iş:

| İş | Not |
|---|---|
| Backend dağıtımı | `AgModulu.EMULATOR_TABANI = http://10.0.2.2:8000/` yalnızca emülatörden erişilir; gerçek telefonda **her** çağrı zaman aşımına uğrayıp sahte veriye düşüyor (`debug.md` O-14). Dağıtım + HTTPS alan adı gerekiyor — `ag_guvenligi.xml` şifresiz trafiği yalnızca `10.0.2.2` için açıyor |
| Uzman/Yeşilay paneli | Web tarafı — hiç başlanmadı, MVP kritik yolunda değil |
| *Faz 3* | ML katmanı (scikit-learn · XGBoost · SHAP), A1–A3 kalibrasyonu — pilot verisi bekliyor |

**Tasarımı eksik ekran kalmadı.** 30 Tem ikinci ek teslimi (8 ekran +
3 widget, `docs/tasarim-referans.md §2 / 31–41`) son boşlukları kapattı;
tasarımı hiç gelmeyen üç ekran (Aktif yürüyüş, YEDAM onayı, Health Connect
izni) §8 adım 3'teki kabul edilmiş yöntemle — mevcut ekranların yerleşimi
+ gerçek tokenlar — uygulandı. `docs/tasarim-referans.md §4` tablosunda
yalnızca Health Connect izni bilgi amaçlı duruyor.

### Kapanan açık iş — Stitch tasarım sistemi

Stitch'teki `Hopeful Green` sisteminde kırmızı `error` tokenı ve silinmiş
özel tokenlar vardı. **Artık önemsiz:** tasarım dışarıdan geliyor (§8),
Stitch'te yeni ekran üretilmiyor. Düzeltilmiş dosya `tools/stitch/` altında
duruyor, ihtiyaç olursa oradan alınır.

### Teslimden bilinçli sapmalar

| Tasarımda | Kodda | Niçin |
|---|---|---|
| "Leaderboard'da görün" | "Ligde görün" | Uygulamanın geri kalanı Türkçe ve sekmenin adı "Lig"; tek İngilizce terim tutarsız |
| Lig avatar renkleri sabit | İsimden deterministik türetiliyor (`avatarRengi`) | Sunucudan renk gelmiyor; aynı isim her cihazda aynı rengi alsın |
| Lig içerik zemini açık mint | Beyaz | Sıralama satırları `Yesil50` dolgulu; zemin de aynı tonda olunca satırlar kayboluyordu |

---

## 12. KARARLAR

### Kapanmış

| # | Konu | Karar | Niçin |
|---|---|---|---|
| K1 | Lig seviyeleri | Fidan → Filiz → Ağaç → Koru → Orman | ağaç metaforunu izliyor; fidan bağışı ödülü de aynı dilde |
| K2 | Ana ekran metaforu | **Ağaç**, 6 büyüme evresi (30 Tem 2026'da 5→6, A7'yi kapatan tutarlı set geldi) | durumu tek bakışta, damgalamadan gösteriyor. Not: yeni set yoğunluk artışı değil mevsimsel/meyve teması ile ilerliyor (çiçek → narenciye → pembe çiçek → sarı → erik → elma), seri gün eşiklerine (`AnaViewModel.agacEvresi`) sırayla eşleniyor |
| K3 | MVP kesimi | §11'deki sıra | puanlamada en ağır kriter "çalışan uçtan uca döngü" (%25) |
| K4 | Font | **Inter** | Averta ticari lisans istiyor; lisans gelirse `Tipografi.kt` tek dosyadan değişir |
| K5 | Grafik kütüphanesi | Custom Compose Canvas | yalnızca 3 grafik türü var, hepsi tek renk skalasında, pan/zoom istenmiyor. Vico/MPAndroidChart kendi paletiyle gelir — "kırmızı yok" kuralına sürekli karşı durmak gerekirdi. 4 Canvas bileşeni yazmak daha az risk |
| K6 | İkon seti | Material Symbols + Stitch illüstrasyonları | işlevsel ikonlar hazır setten; ağaç/nefes/YEDAM gibi özgün görseller Stitch'ten |
| K7 | Lig/Ödül backend bağımlılığı | Mock repository deseni | backend o sırada §11'de en sondaydı; arayüz sabit tutulunca demo günü 4 nav sekmesi de çalışır. **Sonuç (31 Tem):** backend gelince yalnızca `di/DepoModulu.kt` değişti, tek bir ViewModel veya Composable'a dokunulmadı — desen vaadini tuttu |
| K8 | Kategori kaynağı | LMU veri seti + referans tablo + Türkiye | LMU (doi:10.5334/jopd.59) 3.091 uygulama veriyor ama Almanya örneklemi; referans tablo Snapchat/TikTok gibi kritik çakışmaları düzeltiyor |
| K9 | `sistem` kategorisi | Risk hesabına girmez | LMU'nun 3.091 uygulamasının 1.426'sı launcher/SystemUI. Tabloda olmasalardı `other` (0,5) sayılırlardı — telefonun kendi arayüzü kullanıcıya risk yazılamaz |
| K10 | Kırmızı renk istisnaları | 30 Tem ek teslimindeki iki kırmızı kullanım (risk analizi "Yüksek" etiketi/çubuğu; hesap/veri kalıcı silme onayı) **aynen uygulanır** | Kullanıcı, teslim edilen tasarımın bu iki noktada olduğu gibi kullanılmasına karar verdi. §4 MUTLAK KURALLAR ve DESIGN.md §1 bu istisnayla güncellendi — kural genel olarak geçerliliğini koruyor, yalnızca bu iki ekranla sınırlı |
| K11 | Görev detayı ayrı `gorev/` ekran klasörü **değil** | Alt sayfa içeriği `GorevDetayi` — `AnaEkran.kt` içinde private composable, `ProfilEkran`/`OdulEkran`'ın kendi alt sayfaları gibi | §6'daki mimari şema `gorev`'i ayrı bir `ui/ekran/` klasörü gibi listeliyordu ama bu hiç uygulanmadı — `ProfilEkran`/`OdulEkran` emsali (alt sayfa = yerel `remember` state + private composable, ayrı ViewModel yok) daha güçlü ve daha yeni. Şema buna göre düzeltildi (§6). Bir daha `gorev/` klasörü **açılmasın** |
| K12 | "Kendini Test Et" kısa farkındalık testi, **SAS-SV değil** | 5 soru, 3'lü seçim, kesme puanı yok, risk motoruna girmiyor. `domain/test/` + ayrı `farkindalik_testi` tablosu | SAS-SV'nin 29,5 kesme puanı yayınlanmış bir çalışmaya dayanıyor ve ölçeğin **birebir** korunmasına bağlı (`docs/spec-anket.md §6.1`). Kısaltmak geçersizleştirirdi; ayrı bir araç yazmak tek doğru yol. İkisini aynı tabloda tutmak da kalibre edilmemiş bir puanın kalibre edilmişle karışması riskini doğururdu — bu yüzden `anket_yanit` ve `farkindalik_testi` ayrı |
| K13 | Kilit ekranı widget'ı (tasarım 41) **uygulanmadı** | Aynı bilgi tasarım 40'taki ana ekran widget'ıyla veriliyor | iOS'un kilit ekranı widget'ının Android'de karşılığı yok; platformda genel kullanıma açık bir yetenek değil. Tasarım arşivde referans olarak duruyor |
| K14 | Rehber (kişiler) **hiç okunmuyor** | Tasarım 36'daki "Kişilerden bul" butonu bilinçli olarak devre dışı, sebebi ekranda yazılı | CLAUDE.md §4: rehber toplanmıyor. İzin istemek, toplamayacağımız bir veriye erişim istemek olurdu. Davet kodunu paylaşmak aynı işi görüyor |
| K15 | Aktif yürüyüş **foreground service'siz** | Ekran açık kaldığı sürece sayıyor (`EkranAcikTut`), ekranda tek satırla belirtiliyor | Arka planda adım takibi ayrı izin, ayrı bildirim kanalı ve ayrı pil bütçesi demek; MVP'nin kritik yolunda değil. Kullanıcının telefonu cebine koyup sayacın durduğunu sonradan öğrenmemesi için davranış açıkça yazılı |
| K17 | Sunucu **skor hesaplamıyor** | Risk çıkarımı cihazda kalıyor, sunucuya yalnızca sonuç gidiyor | İkinci bir motor uygulaması yazmak, `core-risk`'in referans Python ile birebir doğrulanmışlığını bozardı. Sunucu saklıyor ve ebeveyn/lig için okuyor, hesaplamıyor |
| K18 | Offline-first sarmalayıcı (`Yedekli*Deposu`) | Sunucuya ulaşılamazsa yerel mock; **yalnızca ağ hatasında**, iş hatasında değil | Demo günü backend'in ayakta olacağı garanti değil (ağ, laptop, kap). Lig sekmesinin bağlantı hatasıyla boş kalması, özelliğin yokmuş gibi görünmesi demek. Ama yanlış şifrede yerele düşmek "giriş yaptın" demek olurdu — o yüzden 401/409 olduğu gibi geçiyor |
| K19 | Senkron **rıza kapısına** bağlı | "Yeşilay ile paylaşım" kapalıysa ağ çağrısı bile yapılmıyor; misafir de senkronize edilmiyor | `docs/spec-veri.md` opt-in. Emülatörde doğrulandı: kapalıyken `POST /ozet` hiç görülmüyor, açılınca 200 dönüyor |
| K16 | Şifre **yerelde saklanmıyor** | `YerelSahteHesapDeposu` şifreyi doğrulayıp unutuyor; yalnızca ad ve e-posta DataStore'da | Yarım bir kimlik doğrulama yazmak, güvenli olduğu izlenimi verirdi. Parola sunucu işi. **Sonuç (31 Tem):** sunucu geldi, doğrulama `backend/app/guvenlik.py`'de; cihazda yalnızca JWT var (`JetonDeposu`), parola hâlâ hiçbir yere yazılmıyor. Yerel sahte depo sunucu kapalıyken yedek olarak duruyor (§12 K18) |

### Açık

| # | Konu | Ne zaman kapanır |
|---|---|---|
| A1 | Kategori ağırlıkları ve örüntü katsayıları | Faz 3 (ML), çoklu regresyon — pilot verisi gerekiyor |
| A2 | `S_REF = 300` kalibrasyonu | pilot verisi |
| A3 | Örüntü ölçüm tanımları (`oturum` `sabah` `uyku_oncesi` `yer_degis`) | pilot — P'nin %59'unu belirliyorlar, `docs/spec-risk.md §4.2` |
| A4 | Ödül finansmanı / sponsorluk | teknik değil |
| A5 | Ebeveyn modu kapsamı | MVP kritik yolunda değil |
| A6 | Bant inişi kademeli mi | pilot |
