# Görev dağılımı — 6 kişi

> **Durum:** 3 Ağustos 2026 · dal `fix/gercek-telefon-onkosullari` @ `5ceb314`
> **Takvim:** rapor **8 Ağustos 2026** · final **30 Eylül – 4 Ekim 2026, Şanlıurfa**
> **Ekip:** 6 kişi — 4 yazılım · 2 raporlama
>
> Bu belge açık işleri kişilere böler. Kaynak: `debug.md` §4 (açık işler) ve
> §5 (bu taramanın kapsamadıkları), `CLAUDE.md` §11 (durum).
> **Yeni iş uydurulmadı** — hepsi o iki belgede zaten yazılı.
>
> **Altı kişinin altısı da koda dokunuyor.** Rapor yazan iki kişinin işi §5'te;
> seçilen işler kendi yazacakları rapor bölümünün *kaynağı* olan kod, yani kod
> okumak rapordan çalınan zaman değil rapora hazırlık.

---

## Bölme mantığı

Eksen **modül sahipliği**, "herkes her yere dokunsun" değil. Sebebi somut:
`dev` ile `main` ayrıştığında 8 dosyada çakıştı ve hepsi aynı satırlara farklı
zamanlarda dokunulmasındandı. Repo zaten temiz sınırlarla ayrılmış (`CLAUDE.md` §6);
o sınırları takip edin.

| # | Slot | Kim | Sahiplendiği dosyalar |
|---|---|---|---|
| 1 | Backend + dağıtım | **`tchkechagia0`** | `backend/`, `di/AgModulu.kt`, `data/tercih/Tercihler.kt`, `res/xml/ag_guvenligi.xml` |
| 2 | Veri + risk motoru | **Yazılım mühendisi** | `core-risk/`, `app/data/`, `app/arkaplan/`, `app/di/`, `app/src/release/` |
| 3 | UI · ekranlar ve akışlar | 3. yazılımcı | `app/ui/ekran/`, `app/ui/bilesen/`, `app/ui/tema/` |
| 4 | UI · widget, nudge, cihaz doğrulama | 4. yazılımcı | `app/widget/`, `app/nudge/`, `app/src/androidTest/`, `debug.md` |
| R1 | Rapor · risk metodolojisi | Rapor 1 | `tests/`, `docs/spec-*.md` |
| R2 | Rapor · içerik ve metin | Rapor 2 | `app/src/main/assets/*.json`, ekran metinleri |

**UI iki kişiye niçin böyle bölündü.** İkisi de `app/ui/` içinde gezinirse aynı
dosyalara dokunurlar. Sınır **ekran içi** ile **ekran dışı** arasında: 3 numara
uygulamanın içindeki akışlarda, 4 numara uygulamanın dışına taşan yüzeylerde
(ana ekran widget'ı, sistem üstü nudge overlay'i) ve cihazda doğrulamada.
Kesişen tek nokta 4 numaranın tarama bulguları — kusuru **bulan** 4, ekran
kodunda **düzelten** 3.

| Kim | Niçin bu slot |
|---|---|
| 1 · sen | Dağıtım hesap, alan adı, sertifika ve secret işi; repo sahipliği ve merge yetkisi zaten burada. Başkasına verilirse her adımda geri dönmek gerekir |
| 2 · mühendis | Repodaki en zor teknik yer: `core-risk`'in saf fonksiyon garantisi, referans Python ile birebir eşleşme, Room migration'ları, adım birikim aritmetiği. Vektör kuralı (`CLAUDE.md` §7) disiplin isteyen tek yer |

---

## 1 · Backend + dağıtım

**Tek başına en kritik blokaj.** Şu an telefon sunucuya ancak USB kablosuyla
(`adb reverse tcp:8000 tcp:8000`) bağlanıyor. Demo günü jüri önünde kablo takmak
yok. Bu bitmezse lig, ebeveyn paneli ve özet senkronu **sahte veriyle** gösterilir
(`CLAUDE.md` §12 K18) — ve bu, puanlamada en ağır kriter olan "çalışan uçtan uca
döngü"nün (%25) altını oyar.

### Sunucu tarafı — önce bu

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 1.1 | Sağlayıcı seç ve hesap aç (Railway · Render · Fly.io · VPS). `Dockerfile` ve `docker-compose.yml` **zaten hazır** — sıfırdan yazılmayacak | `backend/Dockerfile`, `docker-compose.yml` | Uzak kapta servis ayakta, `GET /` cevap veriyor |
| 1.2 | PostgreSQL örneğini kaldır, bağlantıyı ayarlara bağla | `backend/app/ayarlar.py`, `veritabani.py` | Uygulama açılışta DB'ye bağlanıyor, log'da hata yok |
| 1.3 | Alembic migration'larını **dağıtılmış** DB'de koştur | `backend/alembic/` | `alembic current` en son revizyonu gösteriyor, tablolar var |
| 1.4 | JWT secret ve parola ayarlarını ortam değişkenine al — repoda varsayılan kalmasın | `backend/app/ayarlar.py`, `guvenlik.py` | Secret kod tabanında aranınca **bulunmuyor**, sağlayıcı panelinde tanımlı |
| 1.5 | Alan adı + HTTPS sertifikası | dağıtım | `curl https://<alan>/…` 200, sertifika geçerli |
| 1.6 | 53 pytest'i dağıtılmış ortama karşı koştur | `backend/tests/` | Hepsi geçiyor |
| 1.7 | *Sıra sonda:* panele veri verecek ucu aç — paneli yazan 4 numara değil, **ucu yazan bu kişi** | `backend/app/uclar/` | Uç 200 dönüyor ve kendi pytest'i var |

### İstemci tarafı — HTTPS bittikten sonra

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 1.8 | Varsayılan sunucu adresini gerçek alan adı yap. Şu an `VARSAYILAN_SUNUCU = "http://10.0.2.2:8000/"` ve bu **yalnızca emülatörde** anlamlı — temiz kurulmuş gerçek telefon hiçbir yere gitmiyor | `data/tercih/Tercihler.kt:302`, `di/AgModulu.kt` | Temiz kurulan telefon **hiçbir ayar yapılmadan** sunucuya ulaşıyor |
| 1.9 | Ağ güvenliği yapılandırmasını sık — HTTPS gelince şifresiz trafiğe gerek kalmıyor, `10.0.2.2` istisnası silinebilir | `res/xml/ag_guvenligi.xml:19` | Release yapılandırmasında `cleartextTrafficPermitted` hiç yok |
| 1.10 | Profil › Geliştirici'deki uyarı metnini güncelle — "10.0.2.2 yalnızca emülatörde" artık varsayılan durumu anlatmıyor | `ui/ekran/profil/ProfilEkran.kt:624` | Metin gerçek varsayılanı anlatıyor (3 numarayla ortak) |

**Bütünün bitti ölçütü:** temiz kurulmuş telefonda, **kablo takmadan**,
kayıt → giriş → `POST /ozet` → `GET /lig` dördü de 200 dönüyor ve lig ekranında
"çevrimdışı" rozeti **görünmüyor**.

**Çakışma riski:** ~0 — Android kodunda yalnızca 3 dosyaya dokunuyor, ikisi tek
satırlık. 1.8–1.10 tek PR'da gitsin.

---

## 2 · Veri + risk motoru

Bu slotun ekseni **ölçümün doğruluğu**. Zincirin çalıştığı biliniyor; ölçtüğü
sayının doğru olduğu bilinmiyor (`debug.md` §5).

### Ölçüm doğrulaması — en önemlisi 2.1

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 2.1 | **Adım sayısını gerçek bir günle doğrula.** Telefonu tam gün cepte taşı, bağımsız bir sayaçla (saat, başka uygulama) karşılaştır. Şu an bilinen tek şey **aritmetiğin** doğruluğu; `debug.md`'deki `1039` enjekte edilmiş bir tabandan üretildi, ölçüm değil | ölçüm · `data/saglik/AdimBirikim.kt` | Aynı gün için `gunluk_ozet.adim` ile bağımsız sayaç yan yana yazılı; sapma varsa **yönü ve büyüklüğü** belli |
| 2.2 | Sistematik eksik sayma çıkarsa kullanıcıya söyle ("telefon cebinde değilse adım sayılmaz"). Eksik sayma risk açısından güvenli taraf ama kullanıcı bunu bilmeli | `ui/ekran/saglik/` — **3 numarayla ortak** | Sağlık ekranında görünüyor; metni 3 numara yazıyor, sayıyı bu kişi veriyor |
| 2.3 | **Egzersiz süresini ikinci kaynağa bağla.** `sporDk` yalnızca Health Connect'ten geliyor ve test telefonunda hep 0. Uygulama kendi yürüyüş oturumunu zaten ölçüyor — tamamlanan yürüyüş `sporDk`'ya yazılabilir. Uydurma değil, kendi ölçtüğü veri | `arkaplan/YuruyusServisi.kt`, `data/saglik/AktiviteSaglayici.kt` | 15 dk yürüyüş sonrası `gunluk_ozet.sporDk > 0`. **HC ile toplanmıyor** — §12 K27'nin mantığı burada da geçerli, aynı hareket iki kez sayılmaz |

### Motor ve kalibrasyon

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 2.4 | `FAKTOR_ASGARI_DK = 15` eşiğini kalibre et — Y-3 için aceleyle konmuş geçici sayı (`debug.md` §4) | `core-risk/…/Sabitler.kt` | Seçilen değerin gerekçesi `docs/spec-risk.md`'de yazılı; `test_vectors.json`'da eşiğin iki yanını da tutan vektör var |
| 2.5 | Veri **dolu** bir sürüm-2 veritabanıyla gerçek yükseltme yolunu dene. `GecisTest` boş DB'yle geçiyor; dolu DB ile hiç yapılmadı | `data/yerel/Gecisler.kt`, `GecisTest.kt` | Dolu sürüm-2 DB → sürüm 4; satır sayısı ve içerik korunuyor, `olayVarMi` geriye dönük doğru dolmuş |
| 2.6 | **Release derlemesini sına.** Her şey `debug` üzerinde doğrulandı; release'in kendi ağ yapılandırması ve `DemoVeri` no-op'u hiç çalıştırılmadı | `app/src/release/` | `assembleRelease` kuruluyor ve açılıyor · Profil › Geliştirici'de "Örnek veri oluştur" **yok** · sunucuya HTTPS ile bağlanıyor |

**Kural:** `core-risk`'e dokunan her değişiklik `tests/test_vectors.json`'a vektör
eklemek zorunda, yoksa merge edilmez (`CLAUDE.md` §7). 2.4 doğrudan bu kuralın
kapsamında.

**Bağımlılık:** 2.6, 1.5 ve 1.9 (HTTPS + ağ yapılandırması) bitmeden tam
yapılamaz — release'in ağ tarafı ancak gerçek sunucu varken sınanır.

**Sıra:** 2.1 önce. `CLAUDE.md` §10 ve `debug.md` §5 "adım doğrulanmadı" diyor;
rapora **ölçülmüş** bir sayı koyabilmek için tek gereken bir gün telefonu cepte
taşımak. Diğer beş madde kod işi ve bekleyebilir.

---

## 3 · UI · ekranlar ve akışlar

İşin çoğu **yeni ekran yazmak değil** — yazılmış ama hiç görülmemiş akışları
gerçekten çalıştırıp görmek. Tasarımı eksik ekran kalmadı (`CLAUDE.md` §11).

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 3.1 | **YEDAM onayı ekranına ulaş.** Yalnızca `Bant.COK` iken açılıyor; iki taramada da bant `AZ` kaldı, ekran bir kez bile görülmedi. Örnek veriyle bandı yükseltip akışı yürü | `ui/ekran/yedam/`, `ui/ekran/riskdetay/` | Ekran cihazda görüldü, onay kutusu zorunluluğu çalışıyor, ekran görüntüsü rapora verildi |
| 3.2 | **Odak seansını cihazda gerçek 15 dakika koştur**, bozulma yolunu gör. Şu ana kadar yalnızca koddan ve birim testten doğrulandı | `ui/ekran/odak/` | 15 dk tam seans + ekran açılınca bozulma ikisi de cihazda görüldü |
| 3.3 | **15 dakikalık tam yürüyüş oturumu + pil etkisi.** Şu an yalnızca 20 saniyelik ekran-kapalı testi var (`debug.md` §5) | `ui/ekran/yuruyus/` | Sayaç 15 dk boyunca doğru ilerledi, servis kendini durdurdu, pil tüketimi ölçüldü ve yazıldı |
| 3.4 | Adım ölçümünün sınırını sağlık ekranında söyle — metni bu kişi yazar, sayıyı **2 numara** verir (2.2) | `ui/ekran/saglik/` | Metin ekranda; emir kipi ve korkutma yok (§4) |
| 3.5 | Profil › Geliştirici'deki sunucu adresi uyarısını güncelle — **1 numara** HTTPS'i bitirince (1.10) | `ui/ekran/profil/ProfilEkran.kt:624` | Metin gerçek varsayılanı anlatıyor |
| 3.6 | 4 numaranın taramasından çıkan ekran kusurlarını düzelt | `app/ui/` | Tarama listesindeki UI maddeleri kapalı |

**Kurallar:** Composable içinde sabit renk/dp yasak, hepsi `ui/tema`'dan
(`CLAUDE.md` §7). Kırmızı yasağının iki istisnası var, **üçüncüsü açılmaz**
(§12 K10). Her ekran klasörü üç dosya: `<Ad>Ekran.kt` · `<Ad>ViewModel.kt` ·
`<Ad>Durum.kt` — `gorev/` klasörü bir daha açılmaz (§12 K11).

---

## 4 · UI · widget, nudge, cihaz doğrulama

Aynı UI işi ama **uygulamanın dışına taşan yüzeyler** ve cihazda kanıt üretme.
`debug.md` §5'in yarısı burada — iki turdur kapsanamayan maddeler.

| # | İş | Nerede | Bitti sayılma ölçütü |
|---|---|---|---|
| 4.1 | **Widget'ları ana ekrana gerçekten yerleştir.** İki turdur kapsanmıyor: sağlayıcı olarak kayıtlı oldukları `dumpsys appwidget` ile doğrulandı ama hiç çizildikleri görülmedi | `widget/EkranSuresiWidget.kt`, `GununSorusuWidget.kt` | İkisi de ana ekranda çizili, veri gerçek, "Günün sorusu" dokunulunca cevabı kaydediyor · ekran görüntüsü var |
| 4.2 | **Nudge overlay'i çizdir.** Servis ayağa kalkıyor ama tetikleyici eşiği hiç oluşmadı, overlay bir kez bile ekrana gelmedi. Bu çekirdek döngünün "YÖNLENDİR" adımı (`CLAUDE.md` §3) — demoda gösterilmesi gereken şey | `nudge/NudgeOverlay.kt`, `nudge/Tetikleyici.kt` | Overlay gerçek cihazda çizildi, 3–5 sn sonra kendini kapattı, ekran görüntüsü/video var |
| 4.3 | **Üçüncü gerçek cihaz taraması.** `pm clear` ile sıfırla, uçtan uca gez, her ekranı `uiautomator` ile ölç, Room'u oku. İlk iki tarama 39 + 9 kusur buldu; üçüncüsü de bulacak | yeni `debug.md` turu | Bulgular sınıflandırılmış (K/Y/O), UI olanlar 3 numaraya, veri olanlar 2 numaraya açıldı |
| 4.4 | **Ebeveyn eşleştirmesini iki cihazla** dene — tek telefon olduğu için hiç yapılamadı | `ui/ekran/ebeveyn/`, `data/depo/EbeveynDeposu.kt` | Kod ikinci cihazda girildi, panel gerçek veri gösterdi; **yanlış kod reddedildi** (§12 K25) |
| 4.5 | Kapsanmayan yollara test yaz, test sayılarını güncel tut | `app/src/test/`, `app/src/androidTest/` | `CLAUDE.md` §11'deki toplam güncel |
| 4.6 | **Demo senaryosu:** hangi ekranlar, hangi sırayla, hangi veriyle. Sunucunun ayakta olduğunu **gösteren** bir adım şart — sahte veri sessizce devreye giriyor (§12 K18, K24) | `docs/` | Senaryo yazılı ve baştan sona bir kez prova edildi |
| 4.7 | Raporlama ekibine ekran görüntüsü, doğrulanmış sayı ve mimari şeması ver | — | R1 ve R2 istediklerini aldı |

**Güncel test sayıları (3 Ağu 2026):** 153 Android birim + 32 cihaz + 22 core-risk
+ 53 backend pytest = **260**.

**İkinci cihaz gerekiyor.** 4.4 tek telefonla yapılamaz; ekipten birinin Android
telefonu yeterli (`CLAUDE.md` §1'deki APK yolu). İmza tuzağına dikkat — aşağıda.

### Uzman/Yeşilay web paneli — şimdilik kimseye atanmadı

Repoda hiç başlanmamış tek komple ürün (`CLAUDE.md` §11) ama **MVP kritik
yolunda değil**. Dört slotun dördü de dolu olduğu için finale kadar açık
bırakılıyor; alınacaksa 1 numaranın dağıtımı bittikten sonra ve önce
`docs/spec-panel.md` yazılarak alınır.

---

## 5 · Rapor yazan 2 kişi — kod teması

Rapor yazmak tek başına kalırsa iki kişi projenin nasıl çalıştığını hiç
öğrenmeden bitiriyor. Aşağıdaki işler bunu engelliyor ve **rapordan zaman
çalmıyor**: her biri o kişinin zaten yazacağı bölümün kaynağı.

İkisi de bir yazılımcıya eşleniyor; takıldıklarında soracakları kişi belli olsun.

### R1 · Risk metodolojisi — 2 numaraya eşlik eder

Raporun en teknik bölümünü (hibrit risk skoru, `docs/spec-risk.md`) bu kişi
yazacak. Kaynağı okumadan yazmasın.

| Ne yapacak | Nerede |
|---|---|
| `./gradlew :core-risk:test` koştursun — **emülatör istemiyor, saniyeler sürüyor**, 22 test. Kurulumun en kolay girişi | — |
| `docs/referans/yesiliz_hesap.py` ve `docs/spec-risk.md`'yi okuyup rapor metodoloji bölümünü buradan yazsın | `docs/` |
| **Somut teslim:** `tests/vektor_uret.py`'ye yeni bir senaryo ekleyip `test_vectors.json`'ı üretsin, Kotlin'in referans Python ile aynı sonucu verdiğini `:core-risk:test` ile göstersin | `tests/` |

Python yetiyor, Android bilgisi gerekmiyor. Öğrendiği şey projenin en özgün
kısmı — raporda "özgünlük" (%10) başlığını da bu kişi yazacak.

### R2 · İçerik ve metin — 3 numaraya eşlik eder

Raporun problem örtüşmesi (%20) ve UX (%15) bölümleri bu kişide.

| Ne yapacak | Nerede |
|---|---|
| **Somut teslim:** Bilgi Bankası'na yazı eklemek. 14 yazı var, yapısı belli (`id`, `baslik`, `ozet`, `kategori`, `okumaDk`, `govde`). Gerçek bir PR ama **derleme riski sıfır** — saf JSON | `assets/bilgi_bankasi.json` |
| Aydınlatma/rıza metinlerini gözden geçirmek — raporun KVKK bölümüyle aynı metin | `assets/sozlesmeler.json` |
| **Metin denetimi:** `CLAUDE.md` §4'ün "teşhis koyma · emir kipi · korkutma yok" kuralına aykırı kalmış cümleleri bulup düzeltmek. Editoryal iş ama kodun içinde, küçük ve kapsamlı PR'lar | `ui/ekran/` |

**Kural:** metin değiştiren PR ekran görüntüsü ekler — hem doğrulama hem rapor
malzemesi aynı anda üretilmiş olur.

---

## Sıralama

Rapor **8 Ağustos**, yani 5 gün var.

1. O tarihe kadar tek gerçek hedef **1 numara** — backend dağıtımı (1.1–1.6).
   Bitmeden "çalışan uçtan uca döngü" iddiası eksik kalıyor.
2. **2.1** (adım doğrulaması) bir gün telefonu cepte taşımaktan ibaret ve rapora
   **ölçülmüş** bir sayı kazandırıyor. Bugün başlasın.
3. **4.1 ve 4.2** (widget + nudge overlay) rapora görsel üretir; ikisi de
   "gösterilmemiş ama var" durumunda ve bu en kolay kapanan boşluk.
4. R1 ve R2 ilk gün kurulumu bitirsin, ilk küçük PR'ı 8 Ağustos'tan **önce**
   merge olsun — akışı rapor telaşının ortasında öğrenmesinler.
5. **Faz 3 (ML) rapora yetişmez** — pilot verisi gerekiyor. Rapora "planlanan"
   diye girsin, "yapıldı" diye değil.

---

## Çalışma düzeni

### İlk gün — altı kişinin altısı

1. Repoyu klonla, JDK 17 + Android Studio + SDK 37 (`CLAUDE.md` §1),
   `./gradlew :app:assembleDebug` → `installDebug`. **Uygulamayı kendi
   telefonunda çalıştırmadan iş almasın.**
2. `./gradlew :core-risk:test` koştur — emülatörsüz, saniyeler, 22 test yeşil.
   Herkesin gördüğü ilk "çalışıyor" anı bu olsun.
3. **İlk PR küçük olsun** — tek dosya, tercihen bir yazım hatası ya da bir
   yorum. Dal → push → PR → merge akışı gerçek işten *önce* öğrenilsin.

### Dal kuralı

`CLAUDE.md` §1'deki kural geçerli:

- Herkes kendi `feature/<ad>` ya da `fix/<ad>` dalında çalışır, PR açar.
- **Merge'ü yalnızca `tchkechagia0` yapar.** `main`'e doğrudan push yok.
- **Günde en az bir kez `main`'den kendi dalına merge alın.** `dev`/`main`
  faciası tam olarak bunun yapılmamasıydı — 8 dosyada çakışma çıktı.
- `CLAUDE.md` ve `debug.md`'ye herkes dokunacak; çakışmanın çıkacağı tek yer
  orası. **Kural:** doküman değişikliği kendi koduyla **aynı PR'da** gitsin,
  ayrı "doküman commit'i" atılmasın.

### Öğrenme çaprazı

Modül sahipliği duruyor — herkesin her yere dokunması `dev`/`main` faciasını
tekrarlar. Öğrenme **review** üzerinden oluyor: **her PR'ı komşu modülün sahibi
inceler.** Çakışma üretmeden herkes başkasının kodunu okumuş oluyor.

| PR sahibi | İnceleyen |
|---|---|
| 1 · backend | 2 · veri |
| 2 · veri | 1 · backend |
| 3 · UI ekranlar | 4 · UI yüzeyler |
| 4 · UI yüzeyler | 3 · UI ekranlar |
| R1 · vektör | 2 · veri |
| R2 · metin | 3 · UI ekranlar |

---

## Uygulamayı ekibe dağıtma

### Altı kişi de → GitHub'dan clone

Repo **private** (`YesilIz/Yesiliz_mobil`) — önce **altısına birden** collaborator
daveti gönderin; rapor yazan iki kişi de PR açacak (§5). Kurulum gereksinimleri
`CLAUDE.md` §1'de hazır: JDK 17 (Temurin), Android Studio, SDK 37, AVD
`Yesiliz_Pixel7_API36`.

> R2'nin işi saf JSON ve metin, R1'inki Python — ikisi de Android Studio açmadan
> iş görebilir. Ama `assembleDebug` ilk gün bir kez koşsun (§ Çalışma düzeni);
> uygulamayı hiç çalıştırmamış birinin yazdığı ekran metni tutmaz.

### Jüri / danışman → kod değil APK

Kod indirip derlemelerine gerek yok:

```bash
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk   (~30 MB)
```

WhatsApp/Drive ile gönderilir, telefona kurulur.

### ⚠ İmza tuzağı — ilk gün patlar

`app/build.gradle.kts`'te özel bir `signingConfig` **yok**. Debug APK her
bilgisayarın **kendi debug anahtarıyla** imzalanıyor. A kişisinin derlediği APK
telefonda kuruluyken B kişisinin APK'sını kurmaya çalışırsanız:

```
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

Önce silmek zorunda kalırsınız, o da **tüm uygulama verisini uçurur** (izinler,
veritabanı, onboarding — hepsi).

İki çözümden biri seçilmeli:

- **Tek derleyici:** APK'yı hep aynı kişi üretir ve dağıtır.
- **Ortak debug anahtarı:** repoya paylaşılan bir debug keystore + `signingConfig`
  eklenir. Debug için güvenli, **release için asla**. ~10 dakikalık iş.

### Bir de bunu bilin

`connectedDebugAndroidTest` (cihaz testleri) koşumu bitince uygulamayı
**kaldırıyor** — veri de gidiyor. Cihaz testi çalıştıran kişi sonrasında
`./gradlew :app:installDebug` ile geri kursun, ya da
`-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` ile koşsun.

---

## Şu an neyin gerçek, neyin sahte olduğu

Demo ve rapor için kritik ayrım (`CLAUDE.md` §11):

| Veri | Durum |
|---|---|
| Ekran süresi, uygulama kullanımı, açılış sayısı | **Gerçek** (UsageStats) |
| Risk skoru, bant, sürükleyiciler | **Gerçek** (`core-risk` cihazda hesaplıyor) |
| Adım sayısı | **Gerçek** (donanım sayacı, §12 K27) — ama sayının doğruluğu henüz doğrulanmadı |
| Egzersiz dakikası | Health Connect'ten; test telefonunda **hep 0** (HC boş) |
| Puan, görev, seri | **Gerçek** |
| Lig sıralaması, lig adı | **Sunucu erişilemezse sahte** (§12 K18) — demo öncesi 1 numara bunu kapatmalı |
| "Önerilen kişiler" listesi | **Her zaman sahte** — rehber okunmuyor, bilinçli (§12 K14) |
