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

# testler  (toplam 293)
./gradlew :core-risk:test              # 22 · saf JVM, saniyeler
./gradlew :app:testDebugUnitTest       # 153 · saf JVM
./gradlew :app:connectedDebugAndroidTest   # 36 · GERÇEK CİHAZ ya da emülatör
./gradlew :app:installDebug            # cihaza kur

# backend  (76 pytest)
cd backend && docker compose up -d && docker compose exec api pytest -q

# canlı ortam smoke testleri — dağıtılmış sunucuya karşı, `pytest -q` toplamıyor
YESILIZ_SMOKE_TABAN=https://yesilizmobil-production.up.railway.app \
  python -m pytest smoke -q

# başka bir adrese derlemek — İKİ build türü de bu adresi gömüyor
# (varsayılan: canlı Railway sunucusu, app/build.gradle.kts)
./gradlew :app:assembleRelease -Pyesiliz.sunucu=https://api.ornek.com

# YEREL backend'e bağlanmak (varsayılan artık canlı sunucu, §12 K29):
./gradlew :app:installDebug -Pyesiliz.sunucu=http://10.0.2.2:8000/   # emülatör
# Gerçek cihazdan: telefon Wi-Fi'da değilse LAN adresi çalışmaz. USB
# üzerinden tünel kur, sonra -Pyesiliz.sunucu=http://127.0.0.1:8000/ ile
# derle ya da Profil › Geliştirici › Sunucu adresi'ne aynı adresi yaz.
# Canlı sunucuya dönmek için aynı ekranda "Varsayılana dön".
adb reverse tcp:8000 tcp:8000

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
| `app/src/main/res/mipmap-*/ic_launcher_foreground.png` | `python3 tools/logo_uret.py` (§12 K44) |
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
| **debug.md** | Açık kusur listesi — **ikinci** gerçek cihaz taraması (1 Ağu 2026) | bug düzeltmeden önce |
| **`DEBUG_LOG.md`** | **Üçüncü tarama (11 Ağu 2026) — 75 bulgu (DBG-001…075), hepsi kapatıldı.** Kanıt/beklenen/gerçek/durum biçiminde, İngilizce | tarihsel — ama açık **yayın riskleri** son bölümünde |
| `DEBUG_STATE.md` | Aynı taramanın oturum durumu ve checkpoint'leri | tarihsel |
| `docs/tarama-01.md` | Birinci tarama (31 Tem 2026) — tamamı kapatıldı, **arşiv** | tarihsel |
| `docs/spec-risk.md` | Risk skoru + bantlama tam speki | core-risk'e dokunurken |
| `docs/spec-anket.md` | SAS-SV ölçeği, 10 madde, Likert | ankete dokunurken |
| `docs/spec-puan.md` | Görev · puan · lig · ödül | puan sistemine dokunurken |
| `docs/spec-veri.md` | Veri stratejisi, 18 yaş altı ebeveyn modu | veri politikasına dokunurken |
| `docs/referans/yesiliz_hesap.py` | Risk motorunun **referans** implementasyonu | **DOKUNULMAZ** — spek |
| **`docs/tasarim-referans.md`** | **Teslim edilen 31 ekranın (19 + 12 ek) spek'i + tokenlar** | **UI yazarken — ekranların kaynağı** |
| `docs/stitch-ekranlar.md` | DESIGN §5.X ↔ Stitch ekran ID eşlemesi | tarihsel — yeni ekran için kullanma |
| `docs/spike-usagestats-sonuc.md` | UsageStats ortam bulguları | veri okumaya başlarken |
| **`docs/dagitim.md`** | **Ortam değişkenleri, Railway, migration, /health, CI/CD, Android adres yönetimi** | **dağıtıma ya da sunucu adresine dokunurken** |
| `docs/gorev-dagilimi.md` | Altı kişilik ekibin modül sahipliği ve iş bölümü | kime hangi iş sorulacağını ararken |

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
| **Yapılmayan işi yapıldı gösterme** | Arka ucu olmayan bir eylemi başarılı ilan etmek — "ebeveyn paneline gönderildi", "YEDAM seninle iletişime geçecek", "ödülün hazır", uydurma çocuk adı/yaşı. Entegrasyon yoksa eylem ya hiç sunulmaz ya da yapamadığını söyler. §12 K37 |
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
| Ağ | Retrofit 3 + OkHttp + kotlinx-serialization | `data/uzak/` — yazıldı (31 Tem). Retrofit 3 kendi converter'ını getirmiyor, köprü ayrı bağımlılık. Sunucu adresi `BuildConfig.API_TABANI`'nden (§12 K29); çalışma zamanı ezmesi **yalnızca debug** (§12 K24) |
| Küçültme | R8 + kaynak küçültme, **yalnızca release** | §12 K34 — 24,2 MB / 4 DEX → 8,6 MB / 1 DEX. `proguard-rules.pro` boş: uygulama kodu yansıma kullanmıyor, kütüphaneler kendi kurallarını paketliyor |
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
├─ di/                    Hilt — VeriModulu · AgModulu
│                         (DepoBaglama kaynak setinde: src/debug · src/release)
├─ data/
│  ├─ kullanim/           UsageStatsManager sarmalayıcı
│  ├─ saglik/             Health Connect + donanım adım sayacı (K27)
│  ├─ yerel/              Room: entity/ dao/ YesilizDb.kt Donusturucu.kt Gecisler.kt
│  ├─ uzak/               Retrofit — YesilizApi · Dto · JetonDeposu
│  ├─ tercih/             DataStore
│  ├─ zaman/              GuncelGun · DegiskenSistemSaati — süreç yaşarken
│  │                      gece yarısını ve saat dilimi değişimini yakalayan
│  │                      TEK "bugün" kaynağı (§12 K35)
│  └─ depo/               Repository — UI buradan okur.
│                         Yerel: Kategori · Kullanim · Risk · Puan · Bildirim ·
│                         Bilgi · Beyan · GecmisVeri · DisaAktar · VeriYonetimi
│                         Uzak: RetrofitHesap · RetrofitLig · Senkron · Ebeveyn
│                         Sarmalayıcı: Yedekli*Deposu (debug) ·
│                         Sunucu*Deposu (release) — §12 K18
├─ domain/
│  ├─ model/              UI'ın gördüğü modeller
│  ├─ kategori/           Kategori · KategoriEsleyici · KategoriTablosu
│  └─ kullanim/           KullanimOlay · OruntuCikar · YerDegisHesapla
├─ ui/
│  ├─ tema/               Renk · Tipografi · Sekil · Boyut · Tema
│  ├─ TurkceMetin.kt      buyukHarf() / kucukHarf()
│  ├─ EkranAcikTut.kt     FLAG_KEEP_SCREEN_ON sarmalayıcı (nefes, göz molası —
│  │                      yürüyüş DEĞİL, o foreground service'te, §12 K21)
│  ├─ KaliciAkis.kt       "önce yaz, sonra ilerle" — kalıcılık bitmeden
│  │                      ekran değiştirmeyi engelleyen ortak ilke
│  ├─ bilesen/            AyarSatiri · BosDurum · Cip · IlerlemeCubugu · Kart
│  │  │                   KodKutusu · KullaniciAvatari · LikertSecici · OnayKutusu
│  │  │                   RiskGirisKarti · SallananYapraklar · UstBaslik ·
│  │  │                   YaprakZemini · YesilizButon
│  │  └─ veri/            CubukGrafik · CubukSecim · DaireselGosterge · YiginBar
│  ├─ gezinme/            NavHost, rotalar
│  └─ ekran/              onboarding ana istatistik nefes riskdetay
│                         lig odul ebeveyn profil hesap hedef kategori
│                         bilgi test bildirim anket yuruyus saglik yedam
│                         sozlesme aktivite odak gozmolasi gecekapanisi
│                         ekransiz
├─ nudge/                 Tetikleyici.kt · NudgeSabitleri.kt · NudgeDurum.kt ·
│                         NudgeOverlay.kt · NudgeOverlayService.kt · NudgeIzni.kt
├─ arkaplan/              GunlukOzetWorker · HatirlatmaWorker ·
│                         YuruyusServisi (İ-3) · IsPlanlayici
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
sunucuya bağladı. **Vaat edilen tam olarak gerçekleşti:** yalnızca DI
bağlaması değişti, hiçbir ViewModel veya Composable'a dokunulmadı.

Bağlanan şey ikisinden biri değil, **sarmalayıcı** — ve sarmalayıcı build
türüne göre değişiyor (`di/DepoBaglama.kt`, kaynak setinde):

| Kaynak seti | Bağlanan | Sunucuya ulaşılamazsa |
|---|---|---|
| `src/debug` | `Yedekli*Deposu` | Yerel sahteye düşer, ekran `cevrimdisi` notunu gösterir |
| `src/release` | `Sunucu*Deposu` | Boş sonuç + aynı not; sahte veri **hiç örneklenmez** |

İkisinde de iş hatası (401 yanlış şifre, 409 e-posta kayıtlı) olduğu gibi
geçiyor — yoksa yanlış şifreyle "giriş yaptın" demiş olurduk. Deseni bir
kez daha sınayan şey bu ayrım oldu: dosya sayısı iki, dokunulan ekran sıfır.

### Sunucu ile senkron

FastAPI backend (`backend/`) beş alanı karşılıyor — uç listesi
`data/uzak/YesilizApi.kt`'de:

| Alan | Uçlar |
|---|---|
| Sağlık | `GET saglik` — veritabanını da yokluyor; DB düşükse 503 (`DBG-046`) |
| Hesap | `POST auth/kayit` · `auth/giris` · `auth/yenile` · **`auth/cikis`** · `auth/sifre-sifirla` · **`PATCH auth/profil`** · **`DELETE auth/hesap`** |
| Özet | `POST ozet` · `GET ozet` · `POST heartbeat` |
| Lig | `GET lig` · `POST lig/katil` · **`POST lig/gorunurluk`** · `GET arkadas` · `GET arkadas/kod` · `POST arkadas/davet` |
| Ödül | `POST odul/sec` · `GET odul/secimlerim` (§12 K28) |
| Ebeveyn | `POST ebeveyn/kod` · `POST ebeveyn/eslestir` · **`DELETE ebeveyn/eslesme`** · `GET ebeveyn/ozet` |

İki kural pazarlık edilemez:

- **Sunucu skor hesaplamıyor** (§12 K17). Cihaz hesaplıyor, sunucu saklıyor ve
  ebeveyn/lig için okuyor. İkinci bir motor `core-risk`'in referans Python ile
  birebir doğrulanmışlığını bozardı.
- **Senkron rıza kapısına bağlı** (§12 K19). "Yeşilay ile paylaşım" kapalıysa
  ağ çağrısı hiç yapılmıyor; misafir kullanıcı da senkronize edilmiyor.
  **`heartbeat` de bu kapının arkasında** (11 Ağu, DBG-068): "son açılış"
  bilgisi eşleşmiş + rıza vermiş çocuk dışında hiç kimseden çıkmıyor.

Rıza kapısının **istisnası** `lig/gorunurluk` (DBG-049): "Ligde görün"
davranışsal veri değil, bir mahremiyet tercihinin kendisi. Paylaşım kapalıyken
de sunucuya gitmek zorunda — yoksa kullanıcı "görünmüyorum" derken sunucuda
görünmeye devam ediyordu. Ödül seçimi de (K28) aynı sınıfta.

Ham olay sunucuya **gitmiyor** — giden şey günlük özet + skor (§4).

Kimlik doğrulama: parola `argon2id` ile özetleniyor (`backend/app/guvenlik.py`),
istemciye JWT erişim + yenileme jetonu dönüyor. Jetonlar `JetonDeposu`'nda,
`Tercihler`'den **ayrı** bir DataStore'da (`name = "jetonlar"`) — "verilerimi sil"
akışı jetonları tek başına temizleyebilsin diye.

> **Yazıldı (1 Ağu 2026).** `VeriYonetimiDeposu` artık `Dispatchers.IO`'da
> koşuyor, jetonları temizliyor ve kategori tablosunu yeniden tohumluyor.
> "Hesabımı sil" ayrıca sunucuya gidiyor (`DELETE /auth/hesap`); sunucuya
> ulaşılamazsa kullanıcı sessizce dışarı atılmıyor, uyarı gösteriliyor.

### Veritabanı

Tam şema `DB.md`'de. **`fallbackToDestructiveMigration` kaldırıldı**
(31 Tem 2026): `data/yerel/Gecisler.kt` gerçek `Migration` nesneleri
taşıyor ve `GecisTest` emülatörde doğruluyor.

> **Entity değiştiren her PR bir `Migration` de yazmak zorunda.** Yazılmazsa
> uygulama açılışta çöküyor — sessizce kullanıcının geçmişini silmektense
> gürültülü başarısız olmak yeğ.

**Şu an 15 entity · 15 DAO, şema sürümü 9.** `GECIS_1_2` (`Gecisler.kt`)
`bildirim` ve `arkadas` tablolarını ekliyor; `GECIS_3_4` `adim_sayac`
tablosunu ekliyor (§12 K27); `GECIS_2_3` `gunluk_ozet`'e
`olayVarMi` kolonunu ekleyip var olan satırları `acilisSayisi > 0` ile
geriye dönük dolduruyor (§9 "satır var ≠ veri var"); `GECIS_4_5` `odul_secimi`
tablosunu ekliyor (§12 K28); `GECIS_5_6` `risk_sonuc`'a `veriDurumu`
kolonunu ekleyip `gunluk_ozet.olayVarMi = 0` olan günleri geriye dönük
`YETERSIZ`'e çekiyor (§12 K30); `GECIS_6_7` `aktivite_oturumu` tablosunu
ekliyor — uygulamanın kendi yürüyüş oturumları artık kalıcı (§12 K31);
`GECIS_7_8` tablo eklemiyor, **veri siliyor**: eski sürümün hiçbir ağ işlemi
yapmadan yazdığı yanıltıcı "ebeveyn paneline gönderildi" bildirimlerini
(`tur = 'ebeveyn'`) temizliyor, gerçek bildirimlere dokunmuyor (DBG-064);
`GECIS_8_9` `gunluk_ozet`'e `geceOlculduMu` (var olan satırlar `olayVarMi`
ile dolduruluyor) ve `adimKaynagi` kolonlarını, `puan_gunluk`'a
`hedefTutturuldu` + `jokerKullanildi` kolonlarını ekliyor, `ruh_hali`
birincil anahtarını `tarih`ten `(tarih, dilim)`'e taşıyor — var olan
satırlar `sabah` penceresine geçiyor (§12 K39, K40, K41).

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
| **XML yorumunda çift tire** | `processDebugMainManifest` `MergeFailureException: Error parsing AndroidManifest.xml` ile düşüyor, satır/sütun veriyor ama sebebi söylemiyor. Türkçe yorumlarda tire-tire ayraç olarak yazmak refleks; XML'de `--` yorum içinde **yasak** | Yorumda ayraç olarak `--` değil `;` ya da uzun tire. `python3 -c "import xml.dom.minidom;xml.dom.minidom.parse('app/src/main/AndroidManifest.xml')"` saniyede söylüyor |
| **Satır var ≠ veri var** | Gecelik worker telafi ederken boş güne de sıfır dolu `gunluk_ozet` satırı yazıyor. `ozet != null` diye bakan her kontrol o satırı "veri var" sanıyor: yeni kullanıcı hiçbir şey yapmadan 8 günlük seri + 400 puan kazanıyor, `alpha` 8 günlük geçmiş sanıp anketin ağırlığını yarıya indiriyor | `gunluk_ozet.olayVarMi`. Satır yazılmaya devam ediyor (telefonsuz gerçek gün de kayıt), ayıran şey bayrak. `ozet != null` yazan her yeni kod bunu sormalı |
| **Paket görünürlüğü: `<intent>` her paketi görmüyor** | `<queries>` içindeki MAIN/LAUNCHER filtresi **launcher aktivitesi olmayan** paketleri görmüyor. Health Connect bazı cihazlarda (Samsung/Android 12) yalnızca `SettingsActivity1` taşıyor; `getSdkStatus()` `SDK_AVAILABLE` dönmüyor ve uygulama kurulu olan HC için "kurulu değil" diyor | Bilinen paket ayrıca `<package android:name="…" />` ile bildirilmeli. `adb shell dumpsys package <paket> \| grep -A2 "queries via package name"` doğruluyor |
| **Health Connect ön plan şartı** | Okuma izni verilmiş, `getSdkStatus()` `SDK_AVAILABLE`, ekran "İzin verildi" diyor — ama okuma yine `0`. HC, uygulama ön planda değilken reddediyor: `RemoteException: … must be in foreground to read the following data types [Steps, StepsCadenceSeries]`. Adımı okuyan iki yer de ön planda değil (`GunlukOzetWorker` gece, `YuruyusServisi` ekran kapalıyken), yani adım **hiç** özete girmiyordu (`debug.md` K-0) | Manifest'te `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` — bildirilmezse HC'nin izin ekranında "Ek erişim" satırı bile çıkmıyor. **Ayrı** izin: `gerekliIzinler` (okuma) ile `istenenIzinler` (okuma + arka plan) ayrılmalı, yoksa arka planı reddeden kullanıcı ön plandaki okumayı da kaybeder. Cihaz desteği `HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND` ile sorulmalı |
| **`queryAndAggregateUsageStats` istenen aralığa kırpmıyor** | Çağrı, verilen aralıkla **kesişen** kovaları birleştirip döndürüyor. Android günlük kovaları yalnızca ~7-10 gün saklıyor; daha eski bir gün sorulduğunda sistem elindeki **haftalık/aylık** kovayı veriyor ve o kova tek güne yazılıyor. İstatistik ekranında "en eski iki gün 19 saat" bundandı; toplamda 24 saat tavanı da yoktu | `queryUsageStats(INTERVAL_DAILY, …)` + `data/kullanim/AgregaSuz`: 26 saatten uzun kova reddediliyor, kabul edilen kova **orta noktasının** düştüğü güne sayılıyor (bir kova iki güne birden girmiyor), paket ve toplam başına 1440 dk tavanı var. Güvenilir kova yoksa `null` — "ölçemedik", sıfır değil |
| **Ölçülemeyen değerin "iyi" tarafa yorumlanması** | Agrega yolunda saat bilgisi okunamıyor, `geceKullanimDk` zorunlu olarak 0 kalıyor. `GECE_TEMIZ` görevi bunu "gece temiz" sanıp ölçülemeyen her güne puan yazıyordu. Aynı sınıf: adım okunamayınca 0 dönmek → "hiç yürümemiş" | Ölçülüp ölçülmediği **ayrı bir bayrakla** taşınır (`GunlukCikarim.geceOlculduMu` → `gunluk_ozet.geceOlculduMu`). Puan/seri/risk hesabı bayrağa bakar; bilinmeyen değer ödüle çevrilmez |
| **`runCatching { … }.getOrDefault(bosDeger)`** | Hatayı yutup **meşru görünen** bir değer döndürmek kusuru sınırsız süre saklıyor. HC okuması reddediliyordu, çağıran `adim = 0` alıyordu, sıfır da geçerli bir gün ("hiç yürümemiş") olduğu için ne kullanıcı ne logcat bir şey söylüyordu | Boş değer dönmek doğru olabilir ama **sessiz** olmamalı: `getOrElse { Log.w(…, it); bosDeger }`. Kural: yutulan istisnanın en az bir izi kalsın |
| **Tek `sheetState` birden çok `ModalBottomSheet`'te** | Ekran tek bir `rememberModalBottomSheetState()` örneğini bütün alt sayfalarında paylaşıyor; biri kapanıp durum `Hidden`'a düştüğünde sıradaki sayfa aynı durumla açılıp **hiç çizilmiyor**. Boş `onDismissRequest` de aynı sınıf: sayfa gizleniyor ama bileşimde kalıp perde dokunuşlarını ve geri tuşunu yutuyor | `ui/bilesen/AltSayfa.kt` — her çağrı kendi durumunu alıyor, `skipPartiallyExpanded` ve iç `verticalScroll` tek yerden |
| **Askıya alan çağrı `MutableStateFlow.update` bloğunun içinde** | `update` lambdayı yarışma durumunda **yeniden çalıştırıyor**; içeride ağ çağrısı varsa ikinci kez istek gidiyor | Askıya alan işi `update`'ten önce yap, sonuca göre `update` çağır |
| **Sağlayıcının `DATABASE_URL`'i psycopg2 arıyor** | Railway/Render PostgreSQL eklendiğinde `postgresql://…` (bazen `postgres://…`) enjekte ediyor. SQLAlchemy 2.0 bu şemada psycopg **2**'yi arıyor, `requirements.txt`'de psycopg **3** var: kap açılışta `ModuleNotFoundError: psycopg2` ile ölüyor ve bu, sağlayıcı panelinde okunması en zor hatalardan biri | `ayarlar.py` şemayı `postgresql+psycopg://`'ye çeviriyor. Değişkenin adı da değiştirilemiyor — sağlayıcı yazıyor — o yüzden her ayar iki takma adla okunuyor (`YESILIZ_*` ve standart ad) |
| **Alembic URL'inde `%`** | `alembic.ini` ConfigParser ile okunuyor ve `%` orada yerleştirme işareti. Sağlayıcının ürettiği veritabanı parolasında bir `%` geçerse migration `InterpolationSyntaxError` ile düşüyor; yerelde hiç görülmüyor çünkü compose parolası `yesiliz` | `alembic/env.py` `set_main_option` öncesi `%` → `%%` |
| **Gradle `doFirst` içinde script değişkeni** | `doFirst { require(canliSunucu…) }` build script nesnesini yakalıyor; yapılandırma önbelleği bunu serileştiremiyor ve `assembleRelease` "cannot serialize Gradle script object references" ile düşüyor. Hata derlemeyle ilgili görünüyor, ilgisi yok | Değeri `configureEach` içinde yerel bir `val`'e al, lambda onu yakalasın |
| **Gradle `file()` modül dizinine göre çözüyor** | `app/build.gradle.kts` içinde `file("yesiliz.jks")` **`app/`** altında arıyor; depo köküne göre yazılmış bir yol `app/app/…` oluyor. Keystore bulunamayınca derleme düşmüyor — sessizce **imzasız** APK üretiyor ve bu ancak telefona kurmaya çalışınca anlaşılıyor | Kök göreli yollar `rootProject.file(...)` ile çözülür. İmzasız üretim bilinçli (keystore'u olmayan ekip üyesi de derleyebilsin) ama **uyarı basıyor** — çıktının adı da söylüyor: `app-release-unsigned.apk` |
| **`railway up` bulunduğu dizini yüklüyor** | Workflow'un çalışma dizini `backend/` olduğu için yüklenen ağacın kökü backend'in kendisi oluyordu; servisin **Root Directory** ayarı (`/backend`) bunun üstüne uygulanınca Railway `backend/backend` arıyor ve build düşüyordu. Hata dağıtımda çıkıyor, kodla ilgisi yok | Dağıtım adımı **depo kökünden** koşar; klasörü servisin Root Directory ayarı belirler, iki yerde birden verilmez |
| **Yenileme jetonu saklanıp hiç kullanılmaması** | Erişim jetonu dolunca (30 dk) her kimlikli çağrı 401 veriyor ve `Yedekli*Deposu` sessizce sahte veriye düşüyor — kullanıcı hâlâ "giriş yapmış" görünüyor | `okhttp3.Authenticator` (`JetonAuthenticator`). Yenileme çağrısı **ayrı** bir istemciden gitmeli, yoksa Dagger döngüsü + sonsuz 401 özyinelemesi |
| **`CancellationException` bir `Exception`'dır** | `catch (Exception)` ve `runCatching` onu da yakalıyor. Ekrandan çıkan kullanıcının iptal edilen coroutine'i "ağ hatası" sanılıp yerel yedeğe düşülüyor, `false` dönülüyor ya da yeniden deneme planlanıyor — yani **iptal, sessizce yan etkiye dönüşüyor** | İptal her zaman yeniden fırlatılır. Çevrimdışı yedek yalnızca `IOException`'a açılır; iş hatası (4xx) ve iptal olduğu gibi geçer. Zorunlu yerel temizlik `finally` + `NonCancellable` içine alınır |
| **Farklı sertifika = kurulum reddi** | `INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match`. Paket adı aynı olsa bile Android farklı anahtarla imzalanmış APK'yı üstüne kurmuyor. Ekipte üç ayrı sertifika dolaşıyordu: her makinenin kendi `~/.android/debug.keystore`'u ve release anahtarı. Telefondaki derlemeyi değiştirmek **veriyi silmeden** mümkün olmuyor | Cihazdaki veri değerliyse önce `adb exec-out run-as <paket> tar cf - databases files shared_prefs > yedek.tar` (yalnızca `debuggable` derlemede çalışır ve **arşiv** amaçlıdır; release'e geri yüklenemez). Yan yana kurulum için `applicationIdSuffix`. Parmak izleri `~/Desktop/yesiliz-debug-anahtari.txt`'de |
| **WorkManager periyodu takvim değil, süredir** | "Her gün 09:00" diye kurulan iş, ilk gecikme + sabit 24 saatlik aralıkla planlanıyor. Yaz saati geçişi ya da saat dilimi değişimi hatırlatmayı kalıcı olarak 08:00'e ya da 10:00'a kaydırıyor; `KEEP` politikası da uygulama yeniden açıldığında eski planı koruyor | Her koşumdan sonra **ve** açılışta bir sonraki yerel hedef yeniden hesaplanıp WorkManager 2.11'in açık `nextScheduleTimeOverride`'ı güncelleniyor (`IsPlanlayici`). Koşan işi iptal etmeden yerinde günceller |
| **Zaten `RESUMED` sahibe `LifecycleObserver` eklemek** | Android yeni gözlemciyi mevcut duruma **senkronlarken** `ON_RESUME` gönderiyor. Odak seansı bunu "kullanıcı telefona geri döndü" sayıp her seansı başlar başlamaz "bozuldu" işaretliyordu — özellik hiç kazanılamıyordu | Gerçek bir ayrılış (`ON_PAUSE`/`ON_STOP`) görülmeden gelen dönüş yok sayılır; küçük bir durum izleyici bunu tutar (`OdakDonusTakibi`, 3 test) |
| **`delay(1000)` = 1 saniye değildir** | Cihaz uykuya dalınca callback erteleniyor. Sayacı "her tur bir saniye" diye ilerleten odak ve yürüyüş, tam da tasarlandıkları durumda (ekran kapalı, telefon cepte) zaman kaybediyordu; 15 dakikalık aktivite gerçekte çok daha uzun sürüyor ya da hiç bitmiyordu | Geçen süre monotonik saatten yeniden kurulur (`SystemClock.elapsedRealtime`, `zaman/GecenSureSaati`) — uykuyu da sayan tek kaynak. Duraklatma yalnızca duraklatılan aralığı düşer |

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
>
> **Aktivite kolu da koştu (3 Ağu 2026):** aynı cihazda `gunluk_ozet.adim`
> ilk kez sıfırdan farklı bir değerle doldu ve `D < 1` çıktı. O güne kadar
> `D` hep `1,0`'dı ve `spec-risk.md`'nin sönümleme dalı hiç **koşturulamamıştı**
> — sebebi iki katmanlıydı: Health Connect ön plan şartı ve Health Connect'in
> bu cihazda tamamen boş olması (`debug.md` K-0, §12 K27).
>
> **Ama koşan şey boru hattı, ölçüm değil.** Kullanılan `1039` sayısı
> veritabanına elle enjekte edilmiş bir tabandan üretildi; o gün telefon
> çantadaydı ve gerçek adım sıfıra yakındı. **Adım sayısının doğruluğu hâlâ
> doğrulanmadı** — telefonu bir gün cepte taşıyıp bağımsız bir sayaçla
> karşılaştırmak gerekiyor (`debug.md` §5).

---

## 11. DURUM

### Biten

| Alan | İçerik | Doğrulama |
|---|---|---|
| Araç zinciri | JDK 17 · Android Studio · SDK 37 · AVD | emülatör açılıyor |
| `core-risk/` | 6 dosya — risk motoru tam, artık `VeriDurumu` (YETERSİZ/HESAPLANDI/ESKİ, §12 K30) | **30 test**, referansla birebir |
| `app/ui/tema/` | Renk · Tipografi · Sekil · Boyut · Tema + Inter + hilal ikonu | emülatörde görüldü |
| `app/domain/kategori/` | 3.106 paketlik tablo + eşleyici | **9 test** |
| `app/domain/kullanim/` | `OruntuCikar` — 6 örüntü + `YerDegisHesapla` | **22 test** |
| `app/data/yerel/` | 15 entity · 15 DAO · `YesilizDb` (sürüm 8) · `Gecisler.kt` — destructive fallback **yok** | **cihaz testleri** (`GecisTest` 1→2 … 7→8'in tamamını, ayrıca dolu veritabanını sürüm 4'ten 8'e taşıyan kapsamlı testi gerçek cihazda ve emülatörde koşuyor) |
| `app/data/kullanim/` | `OturumCikar` (saf) · `KullanimOkuyucu` · `KullanimIzni` | **13 birim + 7 emülatör testi** |
| `app/data/depo/` | `KategoriDeposu` · `KullanimDeposu` (sistem → kategori → Room → özet) | seed + override korunuyor |
| Dokümantasyon | `DB.md` · `docs/stitch-ekranlar.md` · spike raporu | üreticileri var |
| Tasarım | **Dışarıdan 31 ekranlık teslim** (29 Tem + 30 Tem ek 2026) | `docs/tasarim-referans.md` · palet DESIGN.md §1 ile birebir |
| `app/ui/bilesen/` | Kart · Cip · YesilizButon · IkonRozeti · AyarSatiri · LikertSecici · KullaniciAvatari · UstBaslik · IlerlemeCubugu + `veri/` CubukGrafik · DaireselGosterge · YiginBar | emülatörde görüldü |
| `app/ui/gezinme/` | NavHost · 4 sekmeli alt bar · onboarding alt grafiği · çift gezinme koruması | akış uçtan uca yürüdü |
| `app/ui/ekran/` | onboarding (7) · ana (+ görev detayı alt sayfası) · istatistik · riskdetay · lig · profil (+6 alt sayfa) · ödül (+4 alt sayfa — sertifika/geçmiş/detay + seçim) · ebeveyn · nefes | emülatörde ekran ekran doğrulandı |
| `app/di/` | Hilt — `VeriModulu`, `@HiltAndroidApp`, `HiltWorkerFactory` | uygulama açılıyor |
| `app/data/tercih/` | DataStore: hedef · onboarding bayrağı · kurulum tarihi · gizlilik | yeniden başlatmada korunuyor |
| `app/data/depo/RiskDeposu` | **hesap zinciri** — anket + `d` → skor → güvenlik → histerezis → trend → `risk_sonuc` | **8 emülatör testi** |
| `app/data/depo/PuanDeposu` + `domain/puan/` | görev · seri · çarpan · günlük tavan | **7 birim testi** |
| `app/arkaplan/` | `GunlukOzetWorker` (gecelik + 7 gün telafi) · `IsPlanlayici` | emülatörde 8 gün telafi etti |
| `app/data/saglik/` | Health Connect (adım + oturum bazlı egzersiz) **+** donanım adım sayacı yedeği; `AktiviteSaglayici` adımı birleştiriyor, spor dakikasını `domain/kullanim/AktiviteTekillestir` ile tekilleştiriyor (§12 K31) | **6 + 11 birim testi**; cihazda boru hattı uçtan uca koştu (`D < 1` ilk kez) — sayının kendisi gerçek bir günle **doğrulanmadı**, `debug.md` §5 |
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
| `backend/` | FastAPI + PostgreSQL + Alembic — hesap (JWT), özet senkronu, lig, ödül seçimi, ebeveyn eşleştirme; `docker compose up -d` ile ayağa kalkıyor | **117 pytest**; `backend/README.md` çalıştırma + mahremiyet sözleşmesi |
| `backend/` dağıtım katmanı | Ortam ayrımı + üretim açılış kapısı (`ayarlar.py`) · `/health` (DB kontrollü) · JSON log + `X-Istek-Id` + gizli değer maskesi (`gunluk.py`) · CORS · açılışta migration (`baslat.sh`) · `railway.json` · `.env.ornek` | **23 pytest**; yerel yığında `/health` 200 ve `alembic current` = head |
| `backend/smoke/` | Canlı ortama karşı uçtan uca akış — `/health` → kayıt → giriş → korumalı uç → `POST /ozet` → `GET /lig` → `DELETE /auth/hesap` temizliği | **canlı ortama karşı koştu** (CI, 5 Ağu); `pytest.ini` normal koşumdan ayırıyor |
| `.github/workflows/` | `backend.yml` (test → güvenlik kapısı → imaj → Railway → `/health` → smoke) · `android-ag-guvenligi.yml` (cleartext ve `10.0.2.2` nöbetçisi) | **`main`'de uçtan uca yeşil koştu** (5 Ağu): dağıtım + `/health` + smoke |
| **Canlı ortam** | Railway'de yayında — `https://yesilizmobil-production.up.railway.app` · PostgreSQL bağlı · Alembic açılışta koşuyor · `ENVIRONMENT=production` kapısı devrede | `GET /health` → 200, `{"status":"healthy","database":"connected"}` |
| Release imzalama | `app/build.gradle.kts` `signingConfigs` — keystore ve parolalar **depoda değil** (`local.properties` ya da ortam değişkeni). Üçünden biri eksikse derleme düşmüyor, imzasız üretip uyarı basıyor | V2 imza geçerli · dex'te `10.0.2.2` yok · gömülü adres canlı HTTPS |
| `app/data/uzak/` | `YesilizApi` (22 uç) · `Dto` · `JetonDeposu` (erişim/yenileme jetonu) · `AgModulu` (OkHttp interceptor) · `JetonAuthenticator` + `JetonYenileyici` — **401'de sessiz yenileme**, 5xx/429'da oturum korunuyor | uçtan uca emülatörde doğrulandı; birim testleri var |
| `app/data/depo/` uzak taraf | `RetrofitHesapDeposu` · `RetrofitLigDeposu` · `SenkronDeposu` (rıza kapılı, §12 K19) · `EbeveynDeposu` (kod sunucuda doğrulanıyor) | rıza kapalıyken `POST /ozet` hiç görülmüyor, açılınca 200 |
| `app/data/depo/YedekliDepolar` | Offline-first sarmalayıcı — ağ hatasında yerel sahteye düşer, iş hatasında (401/409) düşmez (§12 K18). **Yalnızca debug'da bağlı** | backend kapalıyken debug'da lig sekmesi yine dolu |
| `app/data/depo/SunucuDepolar` | Release'in bağladığı sunucu-yalnız depolar — sahte veri yok, hata indirgeniyor (boş sonuç + `cevrimdisi` + logcat) | release APK'da `10.0.2.2` ve şifresiz adres yok (dex denetlendi) |
| `app/data/depo/OdulDeposu` | Ödül seçimi (market kuponu / fidan bağışı, §12 K28) — yerel yazma birincil, sunucuya rıza kapısı olmadan (hesaplıysa) ikincil; uzak seçimler yerele alınır, bekleyen yerel seçimler yeniden gönderilir | **3 cihaz + 2 uzlaştırma testi**; tekrar seçim `false` dönüyor |

**Toplam 518 test** (315 Android birim + 56 cihaz/emülatör + 30 core-risk + 117
backend pytest). Sayılar **17 Ağustos 2026'da** ölçüldü: JVM tarafı
`:core-risk:test` + `:app:testDebugUnitTest` (345 test, 0 hata, 1 `Assume`
atlaması — `TetikleyiciTest`'in "devam et sönümü moladan uzun" senaryosu, `dev`
dalında iki sönüm de eşiğe eşitlendiği için atlanıyor, §12 K50), backend
`docker compose exec api pytest -q` (117 passed). Cihaz süiti
(`connectedDebugAndroidTest`) o gün **koşturulmadı**; en son 11 Ağustos'ta hem
gerçek telefonda (Samsung SM-A217F · API 31) hem emülatörde (API 36) 44 geçen +
4 `Assume` atlaması ile koştu (`KullanimOkuyucuTest`, UsageStats izni verilmeden
atlanıyor; `adb shell appops … allow` sonrası geçiyor). Zincir uçtan uca
çalışıyor:
`UsageStats → Room → core-risk → risk_sonuc → StateFlow → Composable`.
Ekranlardaki sabit veri **silindi**; veri yokken boş durum gösteriliyor.
Tıklayınca hiçbir şey yapmayan buton/satır kalmadı.

**Hâlâ bilinçli sahte olanlar:** önerilen kişiler listesi (rehber okunmuyor —
§12 K14) ve **debug derlemesinde, backend kapalıyken** lig sıralaması/lig adı
(§12 K18 sarmalayıcısı `YerelSahteLigDeposu`'na düşüyor). Release'de bu yol
**yok**: sahte depo hiç örneklenmiyor, boş lig + "sunucuya ulaşılamıyor" notu
görünüyor. Sunucu ayaktayken lig, hesap, özet senkronu ve ebeveyn eşleştirmesi
gerçek. Kullanıcı adı da gerçek: hesap ekranından ya da misafir modunda
sabitten geliyor.

> **Demo günü uyarısı:** demo debug APK'sıyla yapılıyorsa lig ekranındaki veri
> sunucu ayakta mı diye bakmadan yorumlanmamalı — yerel yedek devreye giriyor.
> Ekran bunu `cevrimdisi` notuyla söylüyor ve o not göründüğü sürece "backend
> çalışıyor" diye gösterilemez.

**Demo tohumu:** `app/src/debug/` kaynak setinde — release derlemesine
**giremiyor**. Profil → Geliştirici → "Örnek veri oluştur". Tohum yalnızca
`gunluk_ozet` *girdilerini* yazıyor; skoru ve bandı gerçek motor üretiyor.

### Açık işler

**31 Temmuz 2026 taramasının tamamı kapatıldı** (1 Ağustos 2026). Gerçek cihazda
(Samsung SM-A217F · Android 12) bulunan **9 kritik + 12 yüksek + 18 orta** kusurun
hepsi düzeltildi ve 5 kapsam kararının hepsi uygulandı; her biri aynı cihazda
doğrulandı. `debug.md` bu taramanın kaydı olarak duruyor, ✅ işaretli.

**Backend dağıtımı bitti (5 Ağustos 2026).** Sunucu Railway'de yayında
(`https://yesilizmobil-production.up.railway.app`), PostgreSQL bağlı,
migration'lar açılışta koşuyor, CI `main`'e her push'ta test → imaj →
dağıtım → `/health` → smoke zincirini uçtan uca yeşil koşuyor. Release
APK artık imzalı ve bu adresi gömülü taşıyor. Ayrıntı `docs/dagitim.md`.

**Üçüncü tarama kapatıldı (11 Ağustos 2026).** 75 bulgu (`DEBUG_LOG.md`,
DBG-001…075) — çapraz hesap mahremiyet sınırı, sahte ebeveyn paneli, ödül
ve YEDAM doğruluk iddiaları, saat dilimi/gece yarısı hataları, jeton
saklama, R8 küçültme. Hepsi düzeltildi ve iki cihazda doğrulandı. Aynı gün
`dev` ve `main`'e alındı, üretim dağıtımı yeşil koştu, imzalı release APK
sıfırlanmış gerçek telefona kuruldu.

| İş | Not |
|---|---|
| Release APK'nın **gerçek telefonda** akış doğrulaması | `gorev1.md`'nin 13 kriterinden 12'si sağlandı. APK 11 Ağu'da sıfırlanmış Samsung SM-A217F'e kuruldu, açıldı, çökmedi; `DEBUGGABLE` değil, gömülü adres canlı HTTPS. **Kalan tek şey akışın kendisi:** hiçbir manuel sunucu ayarı yapmadan kayıt → giriş → lig turu ve o turda "çevrimdışı" uyarısının **görünmemesi** |
| Yayın öncesi riskler | `DEBUG_LOG.md` son bölümü — paylaşımlı hız sınırı, istemci beyanlı lig puanı, Keystore'suz jeton saklama, e-posta sağlayıcısı, doğrulanabilir ebeveyn onayı. §12 A12–A16 (ebeveyn onayı A5/K26) |
| **`NudgeSabitleri.ESIK` dallara göre ayrışıyor** | `main` 45 dakika (gerçek değer), `dev` 15 saniye (cihazda doğrulama için). 14 Ağu 2026'da bilinçli olarak ayrıldı → §12 K50. **`dev`'i `main`'e alırken `--ff-only` çalışmaz**, gerçek merge gerekiyor; `TetikleyiciTest` eşiğe göreli olduğu için ayrışmayı yakalamaz |
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
| K2 | Ana ekran metaforu | **Ağaç**, 9 büyüme evresi | durumu tek bakışta, damgalamadan gösteriyor. **İki düzeltme (14 Ağu 2026):** bu satır 6 evre diyordu, kod (`AnaViewModel.agacEvresi`, `AnaEkran.agacGorseli`) 9 evre kullanıyor; ve evre **seriye değil TOPLAM PUANA** bağlı (`EVRE2_PUAN = 100` … `EVRE9_PUAN = 10.000`). Risk bandına bağlamak bilinçli olarak reddedildi — kötü bir günde ağacı kurutmak cezalandırma olurdu (`AnaViewModel` KDoc). `DESIGN.md §4` hâlâ ağacı risk bandına ve 5 aşamaya bağlıyor; **kod kazanıyor**, o bölüm tarihsel |
| K3 | MVP kesimi | §11'deki sıra | puanlamada en ağır kriter "çalışan uçtan uca döngü" (%25) |
| K4 | Font | **Inter** | Averta ticari lisans istiyor; lisans gelirse `Tipografi.kt` tek dosyadan değişir |
| K5 | Grafik kütüphanesi | Custom Compose Canvas | yalnızca 3 grafik türü var, hepsi tek renk skalasında, pan/zoom istenmiyor. Vico/MPAndroidChart kendi paletiyle gelir — "kırmızı yok" kuralına sürekli karşı durmak gerekirdi. 4 Canvas bileşeni yazmak daha az risk |
| K6 | İkon seti | Material Symbols + Stitch illüstrasyonları | işlevsel ikonlar hazır setten; ağaç/nefes/YEDAM gibi özgün görseller Stitch'ten |
| K7 | Lig/Ödül backend bağımlılığı | Mock repository deseni | backend o sırada §11'de en sondaydı; arayüz sabit tutulunca demo günü 4 nav sekmesi de çalışır. **Sonuç (31 Tem):** backend gelince yalnızca DI bağlaması değişti, tek bir ViewModel veya Composable'a dokunulmadı — desen vaadini tuttu. 5 Ağu'da bir kez daha sınandı: bağlama debug/release kaynak setine ayrıldı (`di/DepoBaglama.kt`), yine hiçbir ekran değişmedi |
| K8 | Kategori kaynağı | LMU veri seti + referans tablo + Türkiye | LMU (doi:10.5334/jopd.59) 3.091 uygulama veriyor ama Almanya örneklemi; referans tablo Snapchat/TikTok gibi kritik çakışmaları düzeltiyor |
| K9 | `sistem` kategorisi | Risk hesabına girmez | LMU'nun 3.091 uygulamasının 1.426'sı launcher/SystemUI. Tabloda olmasalardı `other` (0,5) sayılırlardı — telefonun kendi arayüzü kullanıcıya risk yazılamaz |
| K10 | Kırmızı renk istisnaları | 30 Tem ek teslimindeki iki kırmızı kullanım (risk analizi "Yüksek" etiketi/çubuğu; hesap/veri kalıcı silme onayı) **aynen uygulanır** | Kullanıcı, teslim edilen tasarımın bu iki noktada olduğu gibi kullanılmasına karar verdi. §4 MUTLAK KURALLAR ve DESIGN.md §1 bu istisnayla güncellendi — kural genel olarak geçerliliğini koruyor, yalnızca bu iki ekranla sınırlı |
| K11 | Görev detayı ayrı `gorev/` ekran klasörü **değil** | Alt sayfa içeriği `GorevDetayi` — `AnaEkran.kt` içinde private composable, `ProfilEkran`/`OdulEkran`'ın kendi alt sayfaları gibi | §6'daki mimari şema `gorev`'i ayrı bir `ui/ekran/` klasörü gibi listeliyordu ama bu hiç uygulanmadı — `ProfilEkran`/`OdulEkran` emsali (alt sayfa = yerel `remember` state + private composable, ayrı ViewModel yok) daha güçlü ve daha yeni. Şema buna göre düzeltildi (§6). Bir daha `gorev/` klasörü **açılmasın** |
| K12 | "Kendini Test Et" kısa farkındalık testi, **SAS-SV değil** | 5 soru, 3'lü seçim, kesme puanı yok, risk motoruna girmiyor. `domain/test/` + ayrı `farkindalik_testi` tablosu | SAS-SV'nin 29,5 kesme puanı yayınlanmış bir çalışmaya dayanıyor ve ölçeğin **birebir** korunmasına bağlı (`docs/spec-anket.md §6.1`). Kısaltmak geçersizleştirirdi; ayrı bir araç yazmak tek doğru yol. İkisini aynı tabloda tutmak da kalibre edilmemiş bir puanın kalibre edilmişle karışması riskini doğururdu — bu yüzden `anket_yanit` ve `farkindalik_testi` ayrı |
| K13 | Kilit ekranı widget'ı (tasarım 41) **uygulanmadı** | Aynı bilgi tasarım 40'taki ana ekran widget'ıyla veriliyor | iOS'un kilit ekranı widget'ının Android'de karşılığı yok; platformda genel kullanıma açık bir yetenek değil. Tasarım arşivde referans olarak duruyor |
| K14 | Rehber (kişiler) **hiç okunmuyor** | Tasarım 36'daki "Kişilerden bul" butonu bilinçli olarak devre dışı, sebebi ekranda yazılı | CLAUDE.md §4: rehber toplanmıyor. İzin istemek, toplamayacağımız bir veriye erişim istemek olurdu. Davet kodunu paylaşmak aynı işi görüyor |
| ~~K15~~ | ~~Aktif yürüyüş **foreground service'siz**~~ | **1 Ağu 2026'da TERSİNE ÇEVRİLDİ → K21.** Eski karar: ekran açık kaldığı sürece sayıyor, gerekçesi "arka planda takip ayrı izin/bildirim/pil bütçesi ister, MVP'nin kritik yolunda değil". Gerçek cihazda bu, özelliği tam da kullanılacağı anda çalışmaz kılıyordu (`debug.md` İ-3) |
| K17 | Sunucu **skor hesaplamıyor** | Risk çıkarımı cihazda kalıyor, sunucuya yalnızca sonuç gidiyor | İkinci bir motor uygulaması yazmak, `core-risk`'in referans Python ile birebir doğrulanmışlığını bozardı. Sunucu saklıyor ve ebeveyn/lig için okuyor, hesaplamıyor |
| K18 | Offline-first sarmalayıcı (`Yedekli*Deposu`) — **yalnızca debug** | Sunucuya ulaşılamazsa yerel mock; **yalnızca ağ hatasında**, iş hatasında değil. **Bağlama kaynak setinde** (`di/DepoBaglama.kt`): debug → `Yedekli*`, release → `Sunucu*` | Demo günü backend'in ayakta olacağı garanti değil (ağ, laptop, kap). Lig sekmesinin bağlantı hatasıyla boş kalması, özelliğin yokmuş gibi görünmesi demek. Ama yanlış şifrede yerele düşmek "giriş yaptın" demek olurdu — o yüzden 401/409 olduğu gibi geçiyor. **5 Ağu 2026 daraltması:** aynı davranış yayınlanan sürümde kullanıcıya uydurulmuş bir sıralamayı gerçekmiş gibi göstermek olurdu. Release'de `YerelSahte*` hiç örneklenmiyor; `Sunucu*Deposu` hatayı yutmuyor, indirgiyor — boş sonuç + `cevrimdisi` bayrağı + logcat izi |
| K19 | Senkron **rıza kapısına** bağlı | "Yeşilay ile paylaşım" kapalıysa ağ çağrısı bile yapılmıyor; misafir de senkronize edilmiyor | `docs/spec-veri.md` opt-in. Emülatörde doğrulandı: kapalıyken `POST /ozet` hiç görülmüyor, açılınca 200 dönüyor. **Genişletme (11 Ağu, DBG-068):** `POST heartbeat` bu kapının **dışındaydı** — paylaşımı kapatan kullanıcının "son açılış" zamanı yine sunucuya gidiyordu. Cihazın ne zaman kullanıldığı da davranışsal veri. Artık heartbeat kimlikli + misafir olmayan + eşleşmiş + rıza vermiş çocuk şartına bağlı. **Kapının dışında kalan iki şey bilinçli:** `lig/gorunurluk` ve ödül seçimi (K28) — bunlar davranış değil, kullanıcının kendi tercihinin sunucudaki karşılığı; rızaya bağlarsak "görünmek istemiyorum" tercihi hiç uygulanamaz |
| K20 | Görev doğrulaması **üç türlü**: `OTOMATIK` · `AKTIVITE` · `BEYAN` | İki değer varken aralarına sıkışan görevler yanlış tarafa düşüyordu. Ana ekranın `when` bloğu NEFES ve YURUYUS dışındaki her şeyi "Tamamlandı olarak işaretle" butonuna gönderiyordu — OTOMATIK ilan edilmiş `GECE_TEMIZ` ve `TUKETIM_HEDEFI` dahil. O iki görev **zaten** özetten puanlanıyor; elle işaretlenince aynı gün için çift sayılıyordu ve kayıt `dogrulamaTuru = "otomatik"` olarak yazıldığı için denetim izi de yanıltıcıydı (`debug.md` K-9). `OTOMATIK` görevler `PuanDeposu.gorevTamamla`'dan da yazılamıyor |
| K21 | Aktif yürüyüş **foreground service ile** (K15 tersine) | `arkaplan/YuruyusServisi`. Servis yalnızca oturum sürerken yaşıyor, hedef süre dolunca kendini durduruyor; bildirimi sessiz ve düşük öncelikli. Ekranı 15 dakika açık tutarak yürümek ne makul bir istek ne de uygulamanın kendi mesajıyla ("ekrandan uzaklaş") tutarlıydı — yürüyüş ekranından `EkranAcikTut` kaldırıldı (nefes ve göz molasında **duruyor**, orada kullanıcı ekrana bakıyor). **Tip düzeltmesi (11 Ağu, DBG-045):** `foregroundServiceType` `health` **değil** `specialUse`. `health` tipi, platformun sağlık/aktivite izinlerinden **en az birini** şart koşuyor; uygulama `ACTIVITY_RECOGNITION`'ı isteğe bağlı sayıp izin yokken yalnızca adım sayısını gizlediği için, izni reddetmiş API 36 cihazında `startForeground` `SecurityException` atıp **süreci çökertiyordu** ve Android servisi yeniden başlatıp döngüye giriyordu. 15 dakikalık sayaç için meşru bir izin ön koşulu yok; `specialUse` + açık gerekçe doğru tip. Play Console incelemesinde bu tip ayrı gerekçe istiyor |
| K22 | Dördüncü sekme **Aktiviteler**, Profil avatara taşındı | Profil bir ayar ekranı: nadiren ve belirli bir şey aramaya gidiliyor. Kalıcı bir sekmeyi işgal etmesi, en sık kullanılacak dördüncü alanı harcıyordu. Altı aktivite: nefes · yürüyüş · odak seansı · 20-20-20 göz molası · gece kapanışı · "ekransız ne yaptın?" (`debug.md` İ-1) |
| K23 | Hedef ve gece puanları **oransal** | İkili eşik, hedefinin bir dakika üstünde kalana 25 puanın tamamını kaybettiriyordu; 300 dakikadan 130'a inen biri hedefi 120 olduğu için hiçbir şey kazanmıyordu. Hedefin altında tam puan, aşıldıkça doğrusal iniş, iki katında sıfır. Ceza değil: en kötü ihtimalle terim 0 (`debug.md` İ-4) |
| K24 | Sunucu adresi çalışma zamanında okunuyor — **ama yalnızca debug'da** | `10.0.2.2` yalnızca emülatörde anlamlı; gerçek cihazda her çağrı zaman aşımına uğrayıp `Yedekli*Deposu` sahte veriye düşüyordu ve bu istisna değil **varsayılan** durumdu. Adres `Tercihler.sunucuTabani`'nden geliyor, Profil › Geliştirici'den değişiyor. Yerele düşüldüğü ekranda **söyleniyor** — K18 hâlâ geçerli ama artık görünür (`debug.md` O-14). **Daraltma (11 Ağu, DBG-056):** geliştirici denetimi release'de gizliydi ama DataStore'daki değer hâlâ yetkiliydi — debug'ın üstüne release kurmak ya da yedekten veri geri yüklemek, yayın derlemesinin kimlik bilgilerini ve özetlerini eski geliştirme sunucusuna göndermesine yol açabilirdi. Derleme zamanı HTTPS kapısı bir URL'i doğrularken istemci başka bir URL kullanıyordu. Artık release saklanan değeri **okumuyor da yazmıyor da**; trafik `BuildConfig.API_TABANI`'ne kilitli (`Tercihler.etkinSunucuTabani`, iki JVM testi). **İkinci daraltma (13 Ağu):** debug'ın *varsayılanı* da canlı sunucu oldu (K29) — geliştirici ezmesi hâlâ debug'a özel ama artık istisna, varsayılan değil. Eski varsayılandan kalan kayıtlı ezmeler ilk açılışta bir kez siliniyor (`Tercihler.eskiSunucuAdresiniTemizle`, sürüm işaretçili — değere bakmıyor çünkü `10.0.2.2` sabiti `src/main`'e giremiyor, K32 nöbetçisi düşürür); alt sayfada "Varsayılana dön" var. `YerelOturumSiniri` hesap geçişinde artık **ham** ezmeyi koruyor, etkin adresi değil — yoksa o günkü varsayılan kalıcı ezmeye dönüşüp derlemedeki adresi bir daha uygulanamaz kılıyordu |
| K25 | Eşleştirme **çevrimdışı yedeğe düşmüyor** (K18 istisnası) | `EbeveynDeposu.eslestir` ağ hatasında eşleşmeyi yerel olarak kaydedip başarı dönüyordu; gerçek telefonda sunucuya hiç ulaşılamadığı için bu **tek** davranıştı ve rastgele herhangi bir 6 haneli sayı ebeveyn panelini açıyordu. Çocuğun ekran verisine erişim veren bir akış, yanlış şifreyle giriş yaptırmakla aynı sınıfta (`debug.md` K-8) |
| K26 | 18 yaş altı **beyan usulü ebeveyn onayı** | Varsayılan yaş 17'ydi: varsayılan kullanıcı 18 altıydı ve hiçbir şey sorulmuyordu. Varsayılan 18 oldu; yaş < 18 ise onay ekranı geliyor ve onaylanmadan devam edilemiyor. Bunun **doğrulanabilir olmadığı** ekranda yazılı; gerçek doğrulama ebeveyn panelinin işi ve A5 hâlâ açık. Özellik kısıtı yok (`debug.md` Y-11) |
| K27 | Adım verisi **iki kaynaklı**: Health Connect + donanım sayacı | Health Connect adım **saymıyor**, saklıyor. Yazan bir uygulama yoksa okuma sonsuza kadar sıfır dönüyor — test telefonunda tam olarak bu oldu ("Health Connect'te hiç veriniz yok"; Samsung Health kurulu değil, Google Fit bağlı değil). Yani düzgün çalışan bir entegrasyon, kullanıcının **başka bir uygulama kurmasına** bağlıydı. `Sensor.TYPE_STEP_COUNTER` hiçbir şeye bağlı değil. `AktiviteSaglayici` ikisini birleştiriyor: **Health Connect önce** (saatten/bileklikten gelen veri telefonun cebe girmesinden bağımsız), sıfırsa sensör. **Toplama yok** — ikisi aynı ayakları ölçüyor, şişirilen her adım riski haksız yere aşağı çeker. Sayaç gün başına adım vermiyor (açılıştan beri kümülatif, yeniden başlatmada sıfırlanıyor), o yüzden `adim_sayac` tablosu farkları biriktiriyor; hesap saf (`AdimBirikim.kt`, 6 test). Egzersiz süresi yalnızca Health Connect'ten — adımdan egzersiz uydurmak ölçüm değil tahmin olurdu (`debug.md` K-0) |
| K28 | 5.000 ve 10.000 puanlık ödül kademelerinde **seçenek**: kupon veya fidan bağışı | Market kuponuyla ilgilenmeyen kullanıcı için kupon boşa giden bir ödüldü — "kazandım ama işime yaramadı". `OdulViewModel.Kademe` artık tek bir ödül yerine `secenekler` listesi taşıyor; kademe kazanılınca birden fazla seçenek varsa `OdulHali.SECIM_BEKLIYOR`, kullanıcı seçince `OdulDeposu` üzerinden **kalıcı** yazılıyor (CLAUDE.md §4 "yalnızca kazanç" — geri alma yok). 1. ve 2. kademede (rozet, fidan) tek seçenek olduğu için davranış değişmedi. Seçim, `Yedekli*Deposu` (K18) deseninden farklı çalışıyor: yerel yazma **birincil** (sunucu ulaşılamasa da ödül kaybolmasın), sunucuya senkron **rıza kapısına (K19) bağlı değil** — davranışsal veri taşımıyor, hesap açmakla aynı sınıfta bir kullanıcı eylemi. `docs/spec-puan.md §9.1` güncellendi. Uzak uzlaştırma sunucudaki mevcut seçimleri yeni telefona yazar ve ağda düşmüş yerel seçimleri sonraki akış açılışında yeniden gönderir; ilk seçim ilkesi iki tarafta da korunur (11 Ağu 2026, A7 kapandı). **Maddi ödüller şu an `PLANLANAN` (11 Ağu, DBG-066/067):** kupon ve fidan bağışının karşılanacağı bir tedarik/iş ortaklığı entegrasyonu **yok** (A4 hâlâ açık). Olmayan bir teslimatı "kazandın" diye yazmak, uygulamanın en somut vaadini uydurma yapardı — üstelik hem cihazda hem sunucuda kalıcı kayıt oluşturarak. Maddi kademeler görünüyor ama seçilemiyor, teslimatın hazır olmadığı ekranda yazılı, Android bu seçimi `POST`'lamıyor ve backend `503` dönüp **hiçbir satır yazmıyor**; eski kayıtlar okunmaya devam ediyor. Dijital rozet de "resmî sertifika" görünümünden çıkarıldı, uygulama içi başarı kartı oldu. A4 çözülünce açılacak tek şey bu kapı |
| K29 | Sunucu adresi **tek yapılandırma noktasından**; iki build türü de canlı sunucuyu gömüyor | `app/build.gradle.kts` → `BuildConfig.API_TABANI` = `canliSunucu` (HTTPS) — **13 Ağu 2026'dan beri debug'da da**. Eskiden debug varsayılanı emülatörün `10.0.2.2`'siydi; gerçek telefona kurulan debug APK'sında bu, her çağrının zaman aşımına uğrayıp sessizce yerel yedeğe düşmesi (K18) ve adresin elle yazılması demekti — yani **varsayılan durum bozuktu**. Yerel backend'e bağlanmak aynı düğmeden: `./gradlew :app:installDebug -Pyesiliz.sunucu=http://10.0.2.2:8000/`. `Tercihler.VARSAYILAN_SUNUCU` bunu okuyor, `AgModulu` ondan alıyor. Adres `-Pyesiliz.sunucu` ya da `YESILIZ_SUNUCU` ile eziliyor. Temiz kurulmuş telefon hiçbir ayar yapmadan sunucuya ulaşıyor. **Dört koruma birlikte:** release HTTPS olmayan adresle derlenmiyor, `ag_guvenligi.xml` şifresiz trafiği istisnasız reddediyor (10.0.2.2 istisnası silindi), `dogrulaReleaseApk` üretilen ikiliyi açıp denetliyor (K32), ve release DataStore ezmesini hiç okumuyor (K24 daraltması, DBG-056). CI hepsini denetliyor |
| K16 | Şifre **yerelde saklanmıyor** | `YerelSahteHesapDeposu` şifreyi doğrulayıp unutuyor; yalnızca ad ve e-posta DataStore'da | Yarım bir kimlik doğrulama yazmak, güvenli olduğu izlenimi verirdi. Parola sunucu işi. **Sonuç (31 Tem):** sunucu geldi, doğrulama `backend/app/guvenlik.py`'de; cihazda yalnızca JWT var (`JetonDeposu`), parola hâlâ hiçbir yere yazılmıyor. Yerel sahte depo sunucu kapalıyken yedek olarak duruyor (§12 K18) |
| K30 | Risk motoruna **`VeriDurumu`** (YETERSİZ/HESAPLANDI/ESKİ) eklendi | `gorev2.md` §8: "15 dakikadan az kullanım" kuralı `EtkileyenFaktorler`'deki alt faktörleri koruyordu (`FAKTOR_ASGARI_DK`, K-7/Y-3) ama günün **kendi bandını** korumuyordu — `RiskDeposu.gunuHesapla` `olayVarMi = false` olan (hiç ölçüm olmayan) bir günü bile sessizce hesaplayıp `R = 0 → AZ RİSKLİ` yazıyordu; 34 saniyeliğine açıp bırakan bir kullanıcı ilk günde "düşük risk" görüyordu. `RiskHesapla.hesapla` artık `gun.toplamDk < Sabitler.GUN_ASGARI_DK` iken `VeriDurumu.YETERSIZ` dönüyor; `RiskDetayViewModel` ve `AnaViewModel` bu durumda bandı **hiç göstermiyor**, `RiskDetayEkran` var olan "Henüz yeterli veri yok" boş durumuna düşüyor. `VeriDurumu.ESKI` ayrı bir eksen: `RiskDeposu` her günü kendi tarihi için taze hesapladığından hiç üretmiyor, `RiskDetayViewModel` son `risk_sonuc` satırının tarihini gösterim ANINDA bugünle karşılaştırıp türetiyor (`Sabitler.VERI_ESKI_ESIK_GUN = 3` gün) — kullanıcı birkaç gündür uygulamayı açmadıysa eski bant "güncelmiş" gibi durmuyor. `risk_sonuc.veriDurumu` kolonu `GECIS_5_6` ile eklendi, geriye dönük `gunluk_ozet.olayVarMi = 0` olan günler `YETERSIZ`'e çekildi. 10 yeni `core-risk` senaryo testi (34sn gece kullanımı, sınır değerin altı/tam/üstü, ESKİ) eklendi |
| K31 | Uygulamanın kendi yürüyüşü artık spor dakikasına giriyor; ortak aktivite modeli + tekilleştirme | `gorev2.md` §4-§6: `AktiviteSaglayici` egzersiz süresini **yalnızca** Health Connect'ten alıyordu — `YuruyusServisi`'nde tamamlanan bir yürüyüş hiçbir yere yazılmıyordu, aktif yürüyüş özelliği Görev tamamlama puanı veriyordu ama risk motorunun aktivite sönümlemesine (`D`, tavan %15) hiç girmiyordu. `domain/kullanim/AktiviteOturumu.kt` ortak modeli tanımlıyor (`baslangic`/`bitis`/`kaynak`/`disKimlik`), `AktiviteTekillestir.birlestirilmisSureDk` çakışan oturumları (aynı yürüyüş hem uygulamada hem saatte) tek sayıyor, çakışmayanları topluyor — çakışma oranı eşiği (`CAKISMA_ORANI_ESIGI = 0.5`) tek yerde. `aktivite_oturumu` tablosu (`GECIS_6_7`) uygulama yürüyüşlerini kalıcı tutuyor; `SaglikOkuyucu.gununEgzersizOturumlari` artık Health Connect'in `ExerciseSessionRecord`'larını tek tek okuyor (eskiden yalnızca toplam süre aggregate'i vardı — oturum bazlı çakışma hesabı bununla mümkün oldu), `Metadata.id` [AktiviteOturumu.disKimlik]'e giriyor ki aynı Health Connect kaydının iki kez senkronize edilmesi çift saymasın. `AktiviteSaglayici` ikisini `AktiviteTekillestir`'e veriyor. 11 yeni birim testi (`gorev2.md` §6'daki 8 senaryonun tamamı + sınır değer + boş liste + negatif süre reddi) |
| K32 | Adım teşhis alıcısı (`AdimTaniAlicisi`, debug-only) + release APK'yı otomatik denetleyen Gradle task'ı (`dogrulaReleaseApk`) eklendi | `gorev2.md` §2 donanım sayacının sapma nedenini (ham sayaç/gün başlangıcı/yeniden başlatma) ayırt edecek teşhis bilgisi istiyordu; hiçbiri hiçbir yerde loglanmıyordu. `SaglikTaniAlicisi`'yle (Health Connect) aynı kurulmuş desen: `app/src/debug`'da, `adb shell am broadcast -a tr.org.yesilay.yesiliz.ADIM_TANI` ile tetiklenip logcat'e yazıyor, production'a hiç girmiyor. §11 "release APK içeriğini kontrol eden otomatik bir test" isteği için `app/build.gradle.kts`'e `ReleaseApkAgGuvenligiDenetimi` task'ı eklendi — üretilen APK'nın **her dosyasını** açıp `10.0.2.2` arıyor (varsa düşer) ve canlı HTTPS adresinin gerçekten gömülü olduğunu doğruluyor (yoksa düşer); `android-ağ-güvenliği` CI'ının kaynak-düzeyi kontrolünden daha güçlü çünkü gerçekten üretilmiş ikiliye bakıyor. **Bağ iki yönlü olmalı (7 Ağu düzeltmesi):** task yalnızca `dependsOn("assembleRelease")` taşıyordu, yani `dogrulaReleaseApk` çağrılınca APK üretiliyordu ama `assembleRelease` tek başına koştuğunda denetim **hiç çalışmıyordu** — hiçbir CI workflow'u da task'ı adıyla çağırmadığı için nöbetçi pratikte hiç nöbet tutmuyordu. `tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(…) }` ile ters bağ kuruldu; `tasks.named` burada çalışmıyor çünkü AGP task'ı script değerlendirilirken henüz kaydetmemiş oluyor (aynı dosyadaki HTTPS kapısıyla aynı desen). `bundleRelease` bilerek dışarıda: AAB üretiyor, denetimin baktığı `outputs/apk/release` dizinine dokunmuyor. Denetimin geçtiği **ve** yasaklı desen bulunca derlemeyi gerçekten düşürdüğü 7 Ağu'da doğrulandı |
| K33 | `AdimKaynagi` arayüzü çıkarıldı, `AdimDeposu` çağrılar-arası senaryolarla test edildi + dolu veritabanı migration testi 3 tabloya genişledi | `gorev2.md` §3'ün 9 senaryosundan (gece yarısı gün değişimi, saat dilimi değişimi, izin kaldırma/verme, birkaç gün açılmama, aynı verinin tekrarı) `AdimBirikimTest`teki saf fonksiyon testleri yalnızca TEK ÇAĞRILIK durumları kapsıyordu — gün/saat dilimi değişimi gibi ÇAĞRILAR ARASI senaryolar `AdimDeposu`nun kendisini gerektiriyordu ama `AdimSayacOkuyucu` somut sınıf olduğu için (Android sensörüne bağlı) sahtelenemiyordu. `AdimKaynagi` arayüzü (`kullanilabilir`/`anlikSayac`) çıkarıldı, `AdimSayacOkuyucu` onu uyguluyor, Hilt bağlaması `VeriModulu.adimKaynagi`'de tek satır. `AdimDeposuTest` elle yazılan sahte DAO + sahte kaynak + değişebilir `Clock` ile 6 senaryoyu kapatıyor (mocking kütüphanesi kullanılmadı, projede zaten yok). Ayrıca `GecisTest`e `gorev2.md` §10'un istediği gibi üç günlük geçmişi dört tabloda (`gunluk_ozet`/`risk_sonuc`/`adim_sayac`/`aktivite_oturumu`) birden taşıyan, sürüm 4'ten 7'ye TÜM geçişlerden veri kaybı/çoğalma olmadan geçtiğini doğrulayan tek bir kapsamlı test eklendi. Yazıldığı makinede `aapt2` bir güvenlik politikasınca bloklandığı için bu değişiklikler önce yalnızca elle gözden geçirilmişti; **7 Ağu 2026'da başka bir makinede koşturuldu ve geçti** — `AdimDeposuTest` dahil 170 birim testi, `GecisTest`in 12 testi dahil 40 cihaz testi (emülatör, API 36) |
| K34 | Release'de **R8 + kaynak küçültme** açık; `proguard-rules.pro` bilinçli olarak boş | İmzalı APK 24,2 MB ve **dört DEX**'ti; `material-icons-extended`'in tamamı, yalnızca debug kollarında kalan kod ve erişilemez kütüphane grafiği her indirmede taşınıyordu. Küçültme sonrası 8,6 MB / **tek DEX** (%64,5 azalma) — indirme boyutu kadar sınıf yükleme ve soğuk açılış da ucuzladı. Kural dosyası boş çünkü **uygulama kodunda yansıma yok**; Room, Hilt, Retrofit, kotlinx-serialization ve WorkManager gerekli `keep` kurallarını kendi paketlerinde taşıyor. Bu bir varsayım ve varsayım **denetleniyor**: `dogrulaReleaseApk` (K32) `assembleRelease`'ten sonra otomatik koşuyor, ayrıca küçültülmüş APK API 36'da onboarding → sekmeler → soğuk yeniden başlatma turundan geçirildi. Kurallara ihtiyaç doğuran bir kütüphane eklenirse **kural dosyasına yazılır**, minify kapatılmaz |
| K35 | "Bugün" ve saat dilimi **tek kaynaktan, çalışma anında** — hem cihazda hem sunucuda | İki ayrı kusurun ortak kökü. **Cihazda (DBG-013/057):** Hilt `ZoneId.systemDefault()` ve `LocalDate.now()`'u singleton kuruluşunda **bir kez** çözüyordu. Android süreci günlerce yaşatabiliyor; kullanıcı gece yarısını geçince ya da seyahatte saat dilimi değişince uygulama eski günü kullanmaya devam ediyor, yeni görev/aktivite dünkü güne yazılıyordu. `data/zaman/GuncelGun` yerel gece yarısında ilerleyen tek bir "bugün" akışı veriyor, `DegiskenSistemSaati` her erişimde sistem dilimini yeniden okuyor; testler yine sabit `Clock` enjekte edebiliyor. **Sunucuda (DBG-012):** kap UTC'de koşuyor, `date.today()` Türkiye'de gece yarısı–03:00 arasında **dünü** veriyordu — haftalık lig grubu ve ebeveynin 7 günlük penceresi yanlış güne düşüyordu. Takvim kuralları artık `YESILIZ_SAAT_DILIMI` (varsayılan `Europe/Istanbul`, IANA doğrulamalı) üzerinden tek yardımcıdan geçiyor. **Ayrım:** an/zaman damgası UTC kalır, **takvim** kararı yerel dilimden sorulur |
| K36 | Sunucuda **jeton özeti** saklanıyor, jetonun kendisi değil | Yenileme jetonu (30 gün) ve şifre sıfırlama jetonu veritabanına **düz** yazılıyordu; veritabanı yedeğini okuyabilen biri onları doğrudan oynatabilirdi — yani DB içeriği hamiline yazılı kimlik bilgisiydi. Artık SHA-256 özeti yazılıp özetle aranıyor (`guvenlik.jeton_ozetle`), tüketim satır kilidiyle seri hale getirildi (iki eşzamanlı yenileme aynı jetonu harcayamıyor). Migration var olan satırları yerinde özetledi, istemcideki ham jetonlar geçersizleşmedi. Parola zaten `argon2id` idi (K16), bu onun jeton karşılığı |
| K37 | Bir işi yapmadan "yaptım" diyen her ekran/uç **fail-closed** çevrildi | Üçüncü taramanın (DBG-022/023/053/064/065/066/067) ortak kökü tek bir alışkanlıktı: bir özelliğin arka ucu yokken UI'ın başarı durumunu yazması. Sahte ebeveyn paneli (uydurma çocuk adı/yaşı/grafiği), gerçekte gönderilmeyen "ebeveyn paneline gönderildi", iletilmeyen YEDAM başvurusu, karşılanamayan maddi ödül, doğrulanamayan eşleştirme kodu. Hepsi aynı sınıf hata ve hepsi **kullanıcıya en pahalıya patlayan yerde**: mahremiyet, aile, destek, ödül. Kural §4'e madde olarak eklendi. Uygulaması: entegrasyonu olmayan eylem ya **hiç sunulmaz** ya da yapamadığını söyler; boş/başarısız yanıt asla varsayılan bir "meşru görünen" değere çevrilmez (§9 `runCatching{}.getOrDefault` tuzağının UI karşılığı). **Tekrarladı (13 Ağu):** profil düzenleme ekranı hiçbir ağ çağrısı yapmadan DataStore'a yazıp "güncellendi" diyordu — sunucudaki ad değişmiyor, girilen **yeni şifre hiçbir yere gitmiyordu** (kullanıcı eski şifresiyle giriş yapmaya devam ediyor). `PATCH /auth/profil` yazıldı (K38); alışkanlık, arka ucu olmayan bir alanı forma koymakla başlıyor |
| K38 | Profil düzenleme **sunucuda**: `PATCH /auth/profil`; e-posta alanı kapalı | Ekran ad, e-posta, şifre ve avatarı düzenletiyor ama hiçbiri sunucuya gitmiyordu (K37). Üç ayrı sonuç: sunucudaki ad ligde/ebeveyn panelinde eski kalıyordu, "yeni şifre" hiçbir yere yazılmadığı için kullanıcı şifresini değiştirdiğini sanıyordu, ve e-posta yalnızca cihazda değişince `YerelOturumSiniri`'nin e-posta bazlı veri sahibi anahtarı (`VERI_SAHIBI`) uyuşmaz hâle gelip **sonraki girişte kullanıcının bütün yerel geçmişini sildiriyordu**. Uç ad + avatar + şifreyi güncelliyor; şifre değişimi **mevcut şifreyi** şart koşuyor (erişim jetonu tek başına yetseydi telefonu eline geçiren biri hesabı devralırdı). **E-posta değiştirilemiyor** — doğrulama e-postası gönderemiyoruz (A15), yalnızca sunucuda değiştirmek de kullanıcıyı ulaşılamaz bir adrese kilitlerdi; alan salt okunur ve sebebi ekranda yazılı. Avatar `kullanici.avatar` kolonunda (`a83f19c2d507`) ve giriş yanıtında dönüyor: yeni telefonda simge geri geliyor. Çevrimdışı kolda (`YerelSahteHesapDeposu`) ad/avatar yazılıyor ama şifre değişimi açıkça reddediliyor — cihazda saklanan bir parola yok (K16) |

| K39 | Ölçüm olmayan yerde **ölçüm varmış gibi davranılmıyor** — üç kusurun ortak kökü | `AktiviteSaglayici.geçmişAdimTahmini` geçmiş günlerde adım 0 dönünce hafta içi ~5.500 / hafta sonu ~3.800 civarında bir sayı **üretiyordu**; `agregadanCikar` Android'in haftalık kovasını tek güne yazıyordu (istatistikteki "19 saat"); `olayVarMi` adım varlığıyla da doğru oluyordu. Üçü birlikte, kullanıcı telefonuna hiç dokunmadan gün başına ~60-125 puan ve seri kazanmasına yol açıyordu — üstelik uydurma adım risk motorunun `D` sönümlemesine girip bandı olduğundan **iyi** gösteriyordu. Tahmin fonksiyonu silindi; agrega yolu `AgregaSuz`'den geçiyor (§9); `olayVarMi` yeniden yalnızca gerçek kullanım ölçümüne bakıyor; ölçülemeyen gün `geceOlculduMu = false` ile işaretlenip puanlanmıyor. Cihazlarda kalmış uydurma satırlar tek seferlik bir bayrakla (`Tercihler.uydurmaAdimTemizligiGerekli`, sürüm işaretçili — K24 deseni) yeniden işleniyor. Kurulum **öncesi** günler ekran süresi geçmişine giriyor (UsageStats onları gerçekten ölçmüş, §10) ama puan/risk zincirine girmiyor: geriye dönük ödül dağıtmak `debug.md` K-4'ün ta kendisi |
| K40 | Seri (streak) **günlük hedefi tutturmakla** ilerliyor; haftada bir joker gün | Eski kural "önceki günde `puanNet > 0` mıydı" idi (`PuanGunlukDao.oncekiAktifGun`): nefes egzersizi yapıp 9 saat ekranda kalan kullanıcının serisi de büyüyordu, yani seri ödüllendirdiği davranıştan kopuktu. `PuanSabitleri.KORUMA_PENCERESI_GUN = 7` tanımlıydı ama **hiçbir yerde kullanılmıyordu** — KDoc "haftada bir kaçırma dondurur" diyor, kod bunu yapmıyordu. Yeni kural: gün ölçüldü **ve** ekran süresi ≤ günlük hedef → seri +1; hedef kaçırıldı ve son 7 günde joker kullanılmamış → seri donar (joker harcanır); joker yoksa → 0; gün hiç **ölçülmediyse** seri ne artar ne kırılır (izin vermeyen kullanıcıyı cezalandırmak §4'e aykırı). Karar gün bitiminde, gecelik worker'da veriliyor — gün içi geri alınabilir bir seri gösterilmiyor. `hedefTutturuldu` ve `jokerKullanildi` kolonları `puan_gunluk`'ta; ana ekran çipi joker harcanan günü "3 gün · joker" diye söylüyor |
| K41 | Ruh hali **günde üç pencere**: sabah / öğlen / akşam | Tek kayıt, "bugün nasıldın"ı sabahki tek bir ana indirgiyordu; ruh hali gün içinde değişen bir şey ve takibin değeri tam da o değişimde. `domain/gorev/RuhHaliPenceresi` sınırları saf fonksiyon olarak taşıyor (05:00–11:59 / 12:00–17:59 / 18:00–04:59); `ruh_hali` birincil anahtarı `(tarih, dilim)` oldu, `Gorev.RUH_HALI.gunlukLimit` 3. **Gece yarısı ile 05:00 arası hâlâ DÜNÜN akşam penceresi** — yoksa 00:30'da uygulamayı açan kullanıcı yeni günün akşam kaydını saatler önce yapmış olurdu. Aynı pencerede tekrar sürüklemek değeri günceller, ikinci kez puan vermez: günlük limit tek başına yetmiyordu (sabah penceresinde iki kez kaydırmak limitin altında kalıp ikinci puanı yazardı), pencerenin daha önce doldurulup doldurulmadığı ayrıca soruluyor |
| K42 | Lig **401'i ağ hatasından ayırıyor** | `GET /lig` zorunlu Bearer jeton istiyor (`backend/app/uclar/lig.py`). `SunucuLigDeposu` `CancellationException` dışında **her** istisnayı aynı boş sonuca indirgiyordu: misafir kullanıcı, hiç giriş yapmamış kullanıcı ve yenileme jetonu düşmüş kullanıcı, sunucu ayaktayken "Lig şu anda kullanılamıyor" görüyordu. Bir telefonda lig görünürken diğerinde görünmemesinin sebebi buydu — üstelik debug'da `Yedekli*` sahte veriye düştüğü için aynı hesap iki telefonda farklı davranıyordu. `LigOzeti.oturumYok` ayrımı eklendi; ekran "Lig için giriş yapman gerekiyor" + **Giriş yap** butonu gösteriyor. İki yan düzeltme: `ligeKatil()` artık `KatilimSonuc` dönüyor (eskiden `sessizce(...)` hatayı yutuyordu — kullanıcı butona basıyor, hiçbir şey olmuyordu, §12 K37) ve `ligAkisi()` bir tazeleme tetikleyicisi alıyor (tek atışlık `flow { }` idi; ilk denemede ağ yoksa sekme değiştirilene kadar çevrimdışı kalıyordu) |
| K43 | Elle **yenile** düğmesi — Ana Sayfa, İstatistik, Lig | Veri Room akışlarından geliyor, yani yazılır yazılmaz ekrana düşüyor; sorun verinin ne zaman **yazıldığıydı** — gecelik iş ya da uygulamanın yeniden açılması. `IsPlanlayici.elleTazele()` ayrı bir iş adı ve `REPLACE` politikası kullanıyor: açılış işi `KEEP` olduğu için kuyrukta iş varken düğmeye basmak hiçbir şey yapmazdı. `IstatistikViewModel`'deki `pencereyiCanliTazele` **kaldırıldı** — ViewModel'den depoya yazmak katman ihlaliydi (§6), worker'ın kurulum tarihi/`enErken` korumalarını atlıyordu ve "adım sıfırsa yeniden işle" koşulu adım gerçekten sıfır olan her günde sonsuza kadar tetikleniyordu |
| K44 | Launcher ikonu **yeni logo**; foreground üretimi betikle | `docs/tasarim/yenilogo.png` 1600×1600 kare ve **alfasız**; asıl marka işareti ortadaki yuvarlak diskin içinde. Dosya olduğu gibi foreground yapılsaydı yuvarlak maske altında disk kırpılırdı — adaptive icon 108dp tuvalin yalnızca orta 66dp'sini garanti ediyor. `tools/logo_uret.py` zeminleri atıp (maskenin işi) yalnızca işareti kırpıyor, saydamlaştırıyor ve 60dp güvenli alana ölçekleyip beş yoğunluğa yazıyor; zemin rengi adaptive icon'un `background` katmanında (`@color/logo_zemini`), yani maske daire de olsa squircle da olsa kenardan kenara doluyor. `monochrome` **vektör hilal olarak kalıyor**: Android 13+ temalı ikon tek renkli maske istiyor, fotoğrafik PNG orada okunmuyor |
| K45 | Karşılama akışına **hareket izni adımı**; ana ekranda eksik izin hatırlatıcısı | Adım izni yalnızca Profil › "Adım ve hareket izni" satırından isteniyordu; varsayılan kullanıcı hiç izin vermiyor, aktivite sönümlemesi (`D`) hep 1,0 kalıyor ve risk skoru aktiviteyi hiç görmüyordu. Onboarding'e `HareketIzinEkran` eklendi (adım 3/5, atlanabilir). **Health Connect bilinçli olarak dışarıda**: kurulu olmayabiliyor, kendi izin ekranını açıyor ve Play Store yönlendirmesi gerekebiliyor — karşılamanın ortasında kullanıcıyı uygulamadan çıkarmak akışı kırıyor. İzin verilmediyse ana ekranda `Kart` + `BosDurum` deseninde (yeni bileşen değil) bir hatırlatıcı çıkıyor; **kapatılabilir**, çünkü bilinçli reddedeni sürekli dürtmek §3'ün "zorlama yok" ilkesine aykırı |
| K46 | Bildirimler **silinebiliyor**: kaydırarak tek tek, onayla toplu | Defterde yalnızca 60 günlük otomatik temizlik vardı; kullanıcının kendi silme yolu yoktu. Tek tek silme `SwipeToDismissBox` ile (projedeki ilk kullanımı) ve **tek yön** — iki yönlü kaydırma listede yanlışlıkla çok kolay tetikleniyor ve buradaki tek eylem silme. Silinen kayıt `Snackbar` + "Geri al" ile geri yüklenebiliyor: geri alınamayan bir silme, yanlışlıkla kaydıran kullanıcıya kaydı kalıcı kaybettirirdi. Toplu temizleme geri alınamadığı için **önce onay soruyor**; onay diliyle birlikte kırmızı kullanılmıyor (§4) |
| K47 | Odak seansında **tekerlekli süre seçici** | Süre "Süreyi değiştir" butonuyla 15 → 25 → 45 → 15 diye dönüyordu: aradaki bir değer (30 dk) seçilemiyor, istenen süreye kaç dokunuşta ulaşılacağı belirsiz kalıyordu. Yeni ortak bileşen `ui/bilesen/TekerlekSecici` (`LazyColumn` + `rememberSnapFlingBehavior`) 5–120 dk arasını 5'er adımla veriyor; hızlı çipler (15/25/45) duruyor çünkü kullanıcıların çoğu bu üçünden birini seçiyor. Puan süreye bağlı **değil** (sabit `Gorev.ODAK` puanı), yani uzun süre seçmek bir kazanç yolu açmıyor. `HedefEkran` ve `ProfilEkran`'daki −/+ desenleri bu tura dahil edilmedi — kapsam şişmesin |

| K48 | **Yarım ölçülen gün** hedefe de seriye de sayılmıyor | `KullanimDeposu` bugünü `enErken = veriBaslangicAni` ile işliyor: kurulum/rıza anından öncesi hiç okunmuyor. Saat 14:00'te kuran misafirin o günkü ölçümü 8 dakika çıkıyor ve bu, TÜM GÜN için konmuş 285 dakikalık hedefle karşılaştırılınca hedef tam puanla tutturulmuş sayılıyordu — uygulama açılır açılmaz **27 puan ve 1 günlük seri** (gerçek cihazda görüldü). `PuanDeposu` artık `veriBaslangicAni`'yi günün başlangıcıyla karşılaştırıyor; yarım ölçülen gün hedef puanı ve gece puanı almıyor, seriyi ne artırıyor ne kırıyor. Ayrıca **süren gün** de seriye sayılmıyor (K40 "karar gün bitiminde"): akşam hedef aşılınca geri alınacak bir seri göstermek, kullanıcının bunu bozulma sanmasına yol açar. Aynı iddia bildirimde de vardı — `bugunuBildir` süren gün için "Ekran süresi hedefine ulaştın" yazıyordu; `kapananGunuBildir` oldu, dünü bildiriyor ve ölçülmemiş/yarım günü atlıyor (§12 K37) |
| K49 | Grafik ekseni **çubuklarla aynı ölçekten** türüyor | `CubukGrafik` çubukları verinin kendi maksimumuna göre ölçekliyor ama eksen etiketleri çağrıdan sabit geliyordu (`8s / 4s / 0`). 8 DAKİKALIK bir gün, tepesinde "8s" yazan bir eksenin altında tam boy çubuk olarak çiziliyordu — "istatistikteki saatler tutmuyor" şikâyeti buydu ve gerçek cihazda doğrulandı. `eksenEtiketleri: List<String>` yerine `eksenBicimi: (Float) -> String`: bileşen etiketleri kendi `enBuyuk` değerinden üretiyor, ikisi ayrışamıyor. `Int.eksenSuresi()` bir saatin altını da gösteriyor (`kisaSaat` tam bölme yaptığı için 8 dakika "0s" çıkıyordu); eksen sütunu 28 → 40dp, etiket sarmıyor |
| K50 | Nudge eşiği **dala göre**: `main` 45 dakika, `dev` 15 saniye | Değer 13 Ağu'da 15 saniyeye çekilmişti çünkü popup'ı gerçek cihazda doğrulamak için 45 dakika beklemek pratik değil — ama o hâliyle commit'lenince yayınlanabilir dal da demo değerini taşıyordu ve tek koruma bir KDoc uyarısıydı. Testler yakalamıyor (`TetikleyiciTest` eşiğe **göreli** yazılı, ikisinde de yeşil), yani unutulması an meselesiydi. Ayrım şu: `main` gerçek kullanıcı değerini taşır, geliştirme dalı doğrulanabilir kalır. Ayrışan tek şey `NudgeSabitleri.ESIK` satırı ve KDoc'u; türetilen sabitler (`POLL_ARALIGI`, `SONUM_*`) zaten `ESIK`'ten hesaplandığı için iki dalda da tutarlı, `NudgeOverlay.AnaKart` 60 sn üstünü dakikaya çevirdiği için metin de doğru. **Bedeli:** `main` artık `dev`'e fast-forward edilemiyor. `dev` bu satıra dokunmadığı sürece merge 45 dakikayı koruyor (merge tabanına göre değişen taraf yalnızca `main`), dokunursa çakışma çıkıyor — istenen davranış bu, karar insana kalıyor. `git reset --hard dev` değeri sessizce yutar; §11'deki satır bu yüzden duruyor. 15 saniye ayrıca `BildirimDeposu.molaHatirlatildi(ESIK.toMinutes())`'e **0** geçiriyor ("0 dakikadır aralıksız ekrandasın") — demo dalında kabul, yayında değil |
| K51 | Nudge ön plandaki uygulamayı **kendi, dışlamasız kaynağından** soruyor; `KullanimOkuyucu`'nun kendi paketi elemesi olduğu gibi duruyor | Servis "şu an ön planda ne var" sorusunu `oturumlar.maxByOrNull { it.bitis }` ile, yani penceredeki **en son biten oturumla** cevaplıyordu. Ama `KullanimOkuyucu` kendi paketimizi olay akışından eliyor — ve o eleme doğru (K9 ailesi: kullanıcı Yeşilİz'e baktığı için cezalandırılmaz). İkisi birleşince kullanıcı tüketim uygulamasından Yeşilİz'e geçtiğinde geçiş **hiç görünmüyor**: `OturumCikar` TikTok oturumunu kapatamıyor, `suAnTuketimdeMi` `true` takılıyor ve nudge kullanıcıyı **kendi uygulamasına, hatta kendi önerdiği nefes egzersizinin üstüne** kadar takip ediyordu (4. tarama T4-003; cihazda üç ayrı tetikleme). Çözüm eleme tarafında değil **nudge tarafında**: `KullanimOkuyucu.sonPencereHamOlaylari` elemesiz ham olayı veriyor (risk yolundaki `sonPencereOlaylari` aynen duruyor), yeni saf `OnPlanTespit` bu olayları yürüyüp ön plandaki paketi buluyor, kendi paketimiz `NudgeOverlayService.tuketimKategorisi`'nde açıkça dışarıda. Ön plan okunamıyorsa cevap `false` — **fail-closed** (§4, K37). İki yan sonuç: overlay dururken de ön plan sorulmaya devam ediyor, kullanıcı tüketimden çıkınca overlay **kapanıyor** (yoksa kullanıcıyı 60 saniye takip ediyordu); ve `Tetikleyici.acikBlokSuresi` bloğu artık `blokBas → blokBit` ölçüyor, `blokBas → simdi` değil — tüketim uygulamasından çıkıldıktan sonra blok duvar saatiyle büyüyordu, yani "45 dakikadır buradasın" iddiası o dakikaların bir kısmı başka uygulamada geçmişken kuruluyordu (§4 "yapılmayan işi yapıldı gösterme" ailesi). Hata `TetikleyiciTest`'in göremeyeceği yerdeydi: `Tetikleyici` saf fonksiyon olarak **doğruydu**, kusur onu besleyen sorudaydı — bu yüzden yeni saf fonksiyonun kendi testi var (`OnPlanTespitTest`, 8 senaryo) |

### Açık

| # | Konu | Ne zaman kapanır |
|---|---|---|
| A1 | Kategori ağırlıkları ve örüntü katsayıları | Faz 3 (ML), çoklu regresyon — pilot verisi gerekiyor |
| A2 | `S_REF = 300` kalibrasyonu | pilot verisi |
| A3 | Örüntü ölçüm tanımları (`oturum` `sabah` `uyku_oncesi` `yer_degis`) | pilot — P'nin %59'unu belirliyorlar, `docs/spec-risk.md §4.2` |
| A4 | Ödül finansmanı / sponsorluk | teknik değil |
| A5 | Ebeveyn modu kapsamı | MVP kritik yolunda değil |
| A6 | Bant inişi kademeli mi | pilot |
| ~~A7~~ | ~~Ödül seçiminin sunucuya yeniden gönderilmesi~~ | **11 Ağu 2026'da kapandı → K28.** Numara yeniden kullanılmaz |
| A12 | **Paylaşımlı hız sınırı** — giriş, kayıt, şifre sıfırlama ve 6 haneli ebeveyn eşleştirme kodu | Halka açılmadan önce. Şu an süreç içi koruma var, dağıtım geneli yok; Redis tabanlı sınırlayıcı + alarm gerekiyor |
| A13 | **Lig puanı istemci beyanlı** | Halka açık yarışmadan önce. Puan 0–2.000 aralığına sıkıştırılıyor ama kaynağı cihaz; gerçek rekabet sunucuda doğrulanabilir olay ya da başka bir güven sınırı ister |
| A14 | **Jetonlar Keystore ile şifrelenmiyor** | Yayın öncesi savunma derinliği. Uygulamaya özel DataStore'da ve Android yedeklemesi kapalı, ama donanım destekli saklamaya taşınmalı |
| A15 | **E-posta sağlayıcısı yok** | Şifre sıfırlama şu an üretimde dürüstçe 503 dönüyor (DBG-009). Sağlayıcı + izleme gerekiyor |
| A16 | **Health Connect `1.2.0-alpha01` ön sürüm** | Cihazda doğrulandı ama alpha. Yükseltme ancak ayrı bir bağımlılık/test incelemesiyle yapılır — son dakika sürüm artırımı olarak **değil** |
