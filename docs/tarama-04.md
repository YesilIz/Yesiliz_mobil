# Yeşilİz — 4. Tarama (salt tespit)

> **Bu rapor yalnızca tespittir.** Hiçbir kaynak dosya değiştirilmedi, hiçbir
> düzeltme uygulanmadı, hiçbir git yazma komutu çalıştırılmadı.
> Karar kullanıcıya aittir.

---

## 1. Kapsam ve ortam

| | |
|---|---|
| Tarih | 17 Ağustos 2026, 09:30–… (Europe/Istanbul) |
| Cihaz | Samsung **SM-A217F** · Android **12** (API 31) · seri `RF8NA115KCD` |
| İkinci hedef | `emulator-5554` bağlıydı ama **kullanılmadı** (CLAUDE.md §10: emülatörde `d_efektif = 0`) |
| Dal | **`dev`** · HEAD `c0e63dd` "Nudge eşiğinin dal ayrımını belgele (K50)" |
| Çalışma ağacı | Temiz (yalnızca izlenmeyen `Dökümanlar/`, `debug aahtarları/`, `yesiliz-cihaz-yedek/`) |
| Derleme türü | **debug** (`DEBUGGABLE`, `versionName=0.1.0`, `versionCode=1`) — K18 uyarısı geçerli |
| `NudgeSabitleri.ESIK` | **15 saniye** (dal `dev` → K50'ye uygun; `main` 45 dk taşır) |
| Uygulama kurulumu | Cihazda 2026-08-14 15:11'den beri kurulu; `installDebug` **imza çakışması olmadan** üstüne kuruldu, **veri korundu** |
| Backend | **Yerel** `docker compose` (`http://127.0.0.1:8000`) · `/health` → `{"status":"healthy","database":"connected"}`. Canlı Railway'e **hiçbir yazma isteği gönderilmedi**. |

### İzin durumu (test öncesi tespit)

| İzin | Durum | Not |
|---|---|---|
| `android:get_usage_stats` | **allow** | Zaten verilmişti, tarafımdan değiştirilmedi |
| `SYSTEM_ALERT_WINDOW` | **allow** | Nudge overlay için mevcut |
| `android.permission.ACTIVITY_RECOGNITION` | **granted=true** | Adım sensörü okunabiliyor |

Hiçbir izin bu tarama sırasında **değiştirilmedi**.

### Cihazın veri durumu (bulguların yorumlanması için kritik)

Cihaz fiilen **boşta duran bir test telefonu**. `dumpsys usagestats` bugünün
tamamı için yalnızca şunları gösteriyor: `tr.org.yesilay.yesiliz` 3 dk 35 sn,
`com.sec.android.app.launcher` 41 sn. Başka hiçbir uygulama kullanılmamış.

`gunluk_ozet` bunu doğruluyor (cihazdan `run-as` ile alınan kopya):

| tarih | toplam dk | gece dk | geceÖlçüldü | olayVarMi | adım | kaynak |
|---|---|---|---|---|---|---|
| 2026-07-15 … 2026-08-09 | 0,0 | 0,0 | 0 | **0** | 0 | YOK |
| 2026-08-10 | 2,1 | 0,0 | 1 | 1 | 0 | YOK |
| 2026-08-11 | 3,7 | 2,3 | 1 | 1 | 0 | YOK |
| 2026-08-12 | 0,0 | 0,0 | 0 | **0** | 0 | YOK |
| 2026-08-13 | 10,9 | 0,0 | 1 | 1 | 0 | YOK |
| 2026-08-14 | 6,7 | 0,0 | 1 | 1 | 157 | SENSOR |
| 2026-08-15 | 0,0 | 0,0 | 0 | **0** | 0 | YOK |
| 2026-08-16 | 0,0 | 0,0 | 0 | **0** | 0 | YOK |
| 2026-08-17 (bugün) | 0,0 | 0,0 | 0 | **0** | 234 | SENSOR |

**Bu, ölçüm hatası değil** — telefon gerçekten kullanılmamış, ve uygulama kendi
paketini bilinçli olarak dışlıyor (`KullanimOkuyucu:85,116,149`), launcher da
`sistem` kategorisinde (K9). Yani `olayVarMi = 0` doğru.

**Sonucu:** hiçbir günde `toplamDk ≥ 15` (`Sabitler.GUN_ASGARI_DK`) yok, bu yüzden
`risk_sonuc`'un **dört satırının da** `veriDurumu = YETERSIZ` olması beklenen
davranış (K30). Ama bu aynı zamanda **risk bandı, trend, histerezis ve güvenlik
kuralı kollarının bu turda cihazda çalıştırılamadığı** anlamına geliyor — bkz. §6
"Test edilemedi".

---

## 2. Kritik bulgular

### T4-001 — E-posta `lowercase()` çıplak çağrılıyor; Türkçe cihazda `I` → `ı` olup hesap yanlış adrese açılıyor ve kullanıcı kendi hesabına giremiyor

- **Önem:** **Kritik** (hesap kilitlenmesi + hesabın kullanıcıya ait olmayan bir adrese açılması)
- **Tür:** kod (+ davranışsal sonucu backend'de doğrulandı)
- **Konum:**
  - `app/src/main/kotlin/tr/org/yesilay/yesiliz/data/depo/RetrofitHesapDeposu.kt:44` (`girisYap`)
  - `…/RetrofitHesapDeposu.kt:48` (`kayitOl`)
  - `…/RetrofitHesapDeposu.kt:52` (`sifreSifirla`)
- **Tekrar üretim:**
  1. Cihaz dili Türkçe (bu cihazda `persist.sys.locale = tr-TR` — doğrulandı).
  2. Kayıt ekranına e-posta olarak **büyük `I` içeren** bir adres yaz: `ALI@t4tarama.com` (ya da `Irmak@…`, `Isil@…` — Türkçe adlarda çok yaygın).
  3. İstemci `eposta.trim().lowercase()` uyguluyor. **Locale verilmediği için cihazın varsayılan yereli (tr-TR) kullanılıyor** ve `I` → `ı` (noktasız ı) oluyor: gönderilen dizge `alı@t4tarama.com`.
  4. Kullanıcı daha sonra gerçek adresini doğru biçimde (`ali@t4tarama.com`) yazıp giriş yapmayı dener.
- **Beklenen:** E-posta normalleştirmesi yerelden bağımsız olmalı (`Locale.ROOT`), `ALI@…` → `ali@…`. Kullanıcı kendi adresiyle giriş yapabilmeli.
- **Gözlenen:** Yerel backend'e karşı **birebir üretildi**:

  ```
  POST /auth/kayit  {"eposta":"alı@t4tarama.com", …}   → HTTP 201  (hesap açıldı)
  POST /auth/giris  {"eposta":"ali@t4tarama.com", …}   → HTTP 401  {"detail":"E-posta ya da şifre hatalı"}
  POST /auth/giris  {"eposta":"alı@t4tarama.com", …}   → HTTP 200
  ```

  JVM tarafında dönüşümün kendisi de doğrulandı (`Locale.setDefault(tr-TR)`):

  ```
  "ALI@X.COM".toLowerCase()      -> alı@x.com     ← istemcinin yaptığı
  "ALI@X.COM".toLowerCase(ROOT)  -> ali@x.com     ← doğrusu
  "Isik@Mail.com".toLowerCase()  -> ısik@mail.com
  ```
- **Kanıt:** `/tmp/tarama-04/k.json` · `/tmp/tarama-04/g1.json` (401) · `/tmp/tarama-04/g2.json` (200) · `/tmp/tarama-04/Cas.java` çıktısı · `adb shell getprop persist.sys.locale` → `tr-TR`
- **Neden şüpheli:** Üç ayrı sonucu var ve hiçbiri kullanıcıya doğru söylenmiyor:
  1. **Hesap, kullanıcının adresi olmayan bir adrese açılıyor.** `alı@…` ile `ali@…` farklı e-posta adresleridir. A15 (e-posta sağlayıcısı) çözüldüğünde doğrulama/sıfırlama postası **başka bir adrese** gider.
  2. **Giriş 401 dönüyor ve ekran bunu "şifre hatalı" diye gösteriyor** (`RetrofitHesapDeposu` 401'i şifre alanına bağlıyor, dosya başı KDoc). Kullanıcı şifresini defalarca sıfırlamaya çalışır; sıfırlama ucu da **aynı bozuk dönüşümü** uyguladığı için (satır 52) sorun hiç görünmez.
  3. Aynı dosyadaki `YerelOturumSiniri` **doğru** olanı yapıyor (`Locale.ROOT`, satır 49 ve 94) — yani proje bu tuzağı biliyor, tek bir dosyada uygulanmamış.
- **CLAUDE.md karşılığı:** **§9 "Çıplak `uppercase()`" tuzağının tekrarı** (tuzak `uppercase` üzerinden yazılmış ama kök sebep aynı: locale'siz kasa dönüşümü). Ayrıca **§7 Yasaklar** listesinde açıkça `uppercase()`/`lowercase()` çıplak çağrısı yasak. Gerekçelendirilmiş bir karar (K__) **yok**.
- **Güven:** **kesin** — hem dönüşüm hem uçtan uca 401 üretildi.

> **Not:** `sifreSifirla` şu an üretimde zaten 503 dönüyor (A15), yani (1) maddesinin
> bugünkü etkisi ertelenmiş durumda. Kayıt ve giriş etkisi bugün canlı.

---

### T4-002 — Ekran süresi widget'ı "satır var = veri var" diyor; ölçüm olmayan günde `%0 · 0dk` gösteriyor

> ✅ **düzeltildi** (`fix/tarama-04`, `fb9d721`)

- **Önem:** **Kritik** (ölçülmemiş veriyi ölçülmüş gibi sunmak — §4 "yapılmayan işi yapıldı gösterme" sınıfı; ayrıca §9'un adı konmuş tuzağının tekrarı)
- **Tür:** kod
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/widget/WidgetVerisi.kt:81`
  → tükettiği yer `…/widget/EkranSuresiWidget.kt:67, 79, 92`
- **Tekrar üretim:**
  1. Uygulamayı kur, ekran süresi widget'ını ana ekrana ekle.
  2. Bir gün boyunca telefonu (izlenen uygulamalar bakımından) hiç kullanma — ya da izni verme.
  3. Gecelik `GunlukOzetWorker` o gün için `gunluk_ozet`'e **sıfır dolu bir satır** yazar (`olayVarMi = 0`). Bu tasarım gereği (telefonsuz geçen gerçek gün de kayıt).
  4. Widget'a bak.
- **Beklenen:** Widget "Veri yok" ve halka içinde `–` göstermeli — dosyanın kendi KDoc'u da bunu söylüyor:
  > "Özet yoksa sıfır göstermek yerine 'veri yok' — widget'ta uydurma sayı, ekrandakinden daha yanıltıcı olur (kullanıcı ona bakıp uygulamayı hiç açmıyor)." (`WidgetVerisi.kt:72-74`)
- **Gözlenen:** Kod `veriVarMi = ozet != null` soruyor, `ozet.olayVarMi` sormuyor. Boş gün için satır **var**, dolayısıyla `veriVarMi = true` → widget `%0` + `0dk` gösteriyor, yani "bugün ekranda 0 dakika geçirdin, hedefinin %0'ındasın" **iddiasında bulunuyor**.
- **Kanıt:** Cihazdan `run-as` ile alınan veritabanı, bugün (2026-08-17) dahil dört günde tam bu durumda:

  ```
  tarih        toplamDk  olayVarMi
  2026-08-15   0.0       0          ← satır VAR, ölçüm YOK
  2026-08-16   0.0       0
  2026-08-17   0.0       0          ← bugün
  ```

  Yani `ozetDao.getir(bugun) != null` **doğru**, `olayVarMi` **yanlış**.
  Aynı anda uygulamanın kendi ana ekranı **doğru** davranıyor ve "Ölçmeye yeni
  başladık / Bugünün özeti hazırlanıyor" boş durumunu gösteriyor
  (`/tmp/tarama-04/02-acilis.png`) — çünkü `AnaViewModel.kt:207` doğru soruyu
  soruyor: `veriVarMi = ara.ozet?.olayVarMi == true`, ve satır 203'teki yorum
  bunu açıkça gerekçelendiriyor ("`ozet != null` DEĞİL: gecelik worker ölçüm…").
  **İki yüzey aynı gün için birbirine zıt şey söylüyor.**
- **Neden şüpheli:** §9'daki "Satır var ≠ veri var" tuzağı tam olarak bu; K39
  "ölçülemeyen değerin iyi tarafa yorumlanması" da bu. Ekran süresi widget'ında
  `0dk` **iyi** taraftır: kullanıcı hedefini tutturmuş gibi görünür. Widget,
  kullanıcının uygulamayı hiç açmadan baktığı yüzey olduğu için (KDoc'un kendi
  gerekçesi) yanlış bilginin en uzun süre fark edilmeden kaldığı yer.
- **CLAUDE.md karşılığı:** **§9 "Satır var ≠ veri var" tuzağının tekrarı** ·
  K39 sınıfı. `AnaViewModel`/`IstatistikViewModel`/`PuanDeposu` düzeltilirken
  widget atlanmış. Gerekçelendirilmiş bir karar **yok**.
- **Güven:** **kesin** (kod yolu tek anlamlı, cihazdaki veritabanı koşulu bugün
  fiilen sağlıyor). Widget'ın ekrandaki **görüntüsü** doğrulanamadı: cihazda
  yerleştirilmiş bir örneği yok ve One UI'da widget yerleştirme adb ile
  güvenilir biçimde sürülemiyor — bkz. §6.

### T4-003 — Nudge, kullanıcı Yeşilİz'in **kendi içindeyken** tetikleniyor ve süresiz tekrar ediyor; kendi nefes egzersizini bölüyor

> ✅ **düzeltildi** (`fix/tarama-04`, `d34b49f`)

- **Önem:** **Kritik** (uygulamanın çekirdek müdahale mekanizması yanlış anda ve yanlış iddiayla çalışıyor; kullanıcıyı **kendi uygulamasını kullandığı için** dürtüyor)
- **Tür:** kod + davranış
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/nudge/NudgeOverlayService.kt:124-126`
  (`suAnTuketimdeMi` hesabı) · etkileşimi `…/NudgeOverlayService.kt:142` (`onPlanTuketimUzat`)
  · dolaylı kök `…/data/kullanim/KullanimOkuyucu.kt:85,116,149` (kendi paketini dışlama)
- **Tekrar üretim (cihazda birebir yapıldı):**
  1. Bir tüketim uygulaması aç (TikTok = `com.zhiliaoapp.musically`, `kategori_esleme`'de `entertainment`).
  2. Eşik dolsun, nudge çıksın (`dev` dalında 15 sn).
  3. Nudge'ta **"5 dk mola ver" → "Evet"**'e bas — akış seni **Yeşilİz'in nefes ekranına** götürür. (Ya da tüketim uygulamasından doğrudan Yeşilİz'e geç.)
  4. Nefes ekranında bekle.
- **Beklenen:** Kullanıcı tüketim kategorisinden çıktı; üstelik uygulamanın **kendisinin yönlendirdiği** ekranda. Nudge tetiklenmemeli (`Tetikleyici.degerlendir`'in ilk satırı zaten bunu amaçlıyor: `if (!suAnTuketimdeMi) return GOSTERME`).
- **Gözlenen:** Nudge **tekrar tekrar tetiklendi**. Yeşilİz ön plandayken:

  ```
  09:57:40  poll: oturum=1 tuketim=1 sonuc=GOSTER   ← odak: tr.org.yesilay.yesiliz (İstatistik)
  09:59:02  poll: oturum=1 tuketim=1 sonuc=GOSTER
  10:00:10  poll: oturum=1 tuketim=1 sonuc=GOSTER   ← odak: MainActivity, NEFES EGZERSİZİ sürüyor
  ```

  Ekran görüntüleri: `/tmp/tarama-04/07-nudge-yesiliz-uzerinde.png` — overlay
  **Yeşilİz'in kendi İstatistik ekranının üstünde**, "15 saniyedir buradasın /
  Kısa bir mola iyi gelebilir." yazıyor. `/tmp/tarama-04/10-nefes-90sn.png` —
  10:00:10'daki tetiklemenin ardından kullanıcı hâlâ nefes seansında.
- **Kanıt:** `/tmp/tarama-04/logcat-genel.txt` satır 11106, 12896, 15055 ·
  `07-nudge-yesiliz-uzerinde.png` · `10-nefes-90sn.png` · odak doğrulaması
  `dumpsys window | grep mCurrentFocus` → `tr.org.yesilay.yesiliz`
- **Neden şüpheli — kök sebep:** `suAnTuketimdeMi`, "şu an hangi uygulama ön planda"
  sorusunu **`oturumlar.maxByOrNull { it.bitis }`** ile, yani penceredeki *en son biten
  oturumla* cevaplıyor. Ama `KullanimOkuyucu` **kendi paketini olay akışından
  eliyor** (`kendiPaketAdi` filtreleri). Yani kullanıcı tüketim uygulamasından
  **doğrudan Yeşilİz'e geçtiğinde Yeşilİz hiç oturum üretmiyor**; penceredeki en son
  oturum hâlâ TikTok olarak kalıyor ve `suAnTuketimdeMi` **`true` takılıyor**.

  Üstüne `onPlanTuketimUzat` (satır 142) her poll'de TikTok oturumunun bitişini
  `simdi`'ye uzattığı için `acikBlokSuresi` **sınırsız büyüyor**. Sonuç: sönüm
  penceresi (`SONUM_DEVAM`) her dolduğunda nudge yeniden çıkıyor ve kullanıcı
  Yeşilİz'de kaldığı sürece bu **hiç bitmiyor**.

  Log'daki `oturum=1` bunu doğruluyor: 15 dakikalık pencerede tek bir oturum var
  (TikTok). Yeşilİz sayılsaydı en az iki olurdu.
- **`main` dalındaki hâli (K50):** Eşik 45 dakika, `SONUM_DEVAM` 10 dakika. Yani
  45 dakika TikTok kaydıran bir kullanıcı Yeşilİz'e geçtiğinde **"45 dakikadır
  buradasın" yazan bir nudge alıyor** — Yeşilİz'de olduğu hâlde — ve Yeşilİz'de
  kaldığı sürece **her 10 dakikada bir** tekrar alıyor. Süre iddiası da yanlış:
  sayaç Yeşilİz'de geçen süreyi TikTok bloğuna ekliyor. Bu, eşik değerinden
  bağımsız bir mantık hatası; `dev`'in 15 saniyesi yalnızca **görünür kılıyor**.
- **CLAUDE.md karşılığı:** Gerekçelendirilmiş bir karar **yok**. §3 "hafif
  bildirimsel nudge, zorlama yok" ilkesiyle ve §4 "emir kipi / rahatsız etme"
  çizgisiyle çelişiyor; ayrıca kullanıcıya davranışı hakkında **yanlış bir süre
  iddiası** sunuyor (§4 "yapılmayan işi yapıldı gösterme" ile aynı aile).
  `TetikleyiciTest` bunu yakalayamaz: `Tetikleyici` saf fonksiyon olarak
  **doğru** — hata `suAnTuketimdeMi`'yi **besleyen** serviste.
- **Güven:** **kesin** (üç ayrı tetikleme, ekran görüntüsü + odak doğrulaması + log).

---

## 3. Orta

### T4-004 — Nudge kartındaki bulanık hilal filigranı metni okunmaz hâle getiriyor

- **Önem:** Orta (uygulamanın **tek** müdahale yüzeyinde okunabilirlik)
- **Tür:** kod (UI)
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/nudge/NudgeOverlay.kt:125-133`
- **Tekrar üretim:** Nudge'ı tetikle (T4-003 adım 1-2), karta bak.
- **Beklenen:** Filigran arka planda kalmalı; "sen de YEŞİLAYCISIN" logosu ve
  "Kısa bir mola iyi gelebilir." satırı net okunmalı.
- **Gözlenen:** `R.drawable.hilal` kartın **içine**, `blur(12.dp)` + **`alpha(0.9f)`**
  ile çiziliyor. 0,9 alfa bir filigran için neredeyse opak: koyu yeşil hilal
  logonun ve ikincil metnin üstüne biniyor. `/tmp/tarama-04/04-nudge-45sn.png` ve
  `/tmp/tarama-04/07-nudge-yesiliz-uzerinde.png` — "Kısa bir mola iyi gelebilir."
  satırının orta kısmı hilalin koyu gövdesiyle çakışıyor.
- **Kanıt:** `/tmp/tarama-04/04-nudge-45sn.png` · `/tmp/tarama-04/07-nudge-yesiliz-uzerinde.png`
- **Neden şüpheli:** Kart zemini `Beyaz.copy(alpha = 0.95f/0.85f)` gradyanı, yani
  zaten yarı saydam; filigranın 0,9 alfası bunun üstünde baskın çıkıyor.
  `docs/tasarim-referans.md`'de nudge teslim edilmiş bir ekran **değil** (§8 adım 3,
  yerleşim referansı), yani karşılaştırılacak bir tasarım yok.
- **CLAUDE.md karşılığı:** yok
- **Güven:** **kasıtlı olabilir, teyit gerek** — filigran bilinçli bir tercih;
  tartışılan şey yalnızca alfa değeri. Tasarım sahibinin kararı.

### T4-005 — `PATCH /auth/profil` gövdede `avatar` yoksa kayıtlı avatarı **siliyor**

> ✅ **düzeltildi** (`fix/tarama-04`, `723c9ee`)

- **Önem:** Orta (veri kaybı — küçük ama sessiz)
- **Tür:** backend
- **Konum:** `backend/app/semalar.py:58` (`avatar: str = Field(default="", max_length=32)`)
  → `backend/app/uclar/hesap.py:191` (`kilitli.avatar = istek.avatar`, koşulsuz atama)
- **Tekrar üretim (yerel backend'e karşı yapıldı):**
  1. `PATCH /auth/profil` `{"ad":"Yeni Ad","avatar":"a83f19c2d507"}` → `200`, avatar yazıldı.
  2. `PATCH /auth/profil` `{"ad":"Yeni Ad","mevcutSifre":…,"yeniSifre":…}` (avatar **yok**) → `200`.
  3. `POST /auth/giris` ile kontrol et.
- **Beklenen:** `PATCH` kısmi güncellemedir; gövdede olmayan alan **değişmemeli**.
- **Gözlenen:** Avatar boşaldı.

  ```
  adım 1 yanıtı : {"ad":"Yeni Ad","eposta":"t4-a@…","avatar":"a83f19c2d507"}
  adım 2 yanıtı : {"ad":"Yeni Ad","eposta":"t4-a@…","avatar":""}
  giriş yanıtı  : ad= Yeni Ad  avatar= ''
  psql          : SELECT ad, avatar FROM kullanici …  →  Yeni Ad | (boş)
  ```
- **Kanıt:** `/tmp/tarama-04/p1.json` · `/tmp/tarama-04/q4.json` · `/tmp/tarama-04/v.json` · psql çıktısı
- **Neden şüpheli:** Pydantic'te `default=""` verilmesi, "alan gönderilmedi" ile
  "alan boş gönderildi" arasındaki farkı yok ediyor. Uç `PATCH` olarak
  adlandırılmış ama `PUT` gibi davranıyor.
- **Bugünkü etkisi sınırlı:** Android istemcisi avatarı **her zaman** gönderiyor
  (`RetrofitHesapDeposu.kt:110-115`, form `durum.avatarId` ile ilkleniyor —
  `ProfilEkran.kt:1423`). Yani uygulamadan şifre değiştirmek avatarı silmiyor;
  kusur **gizli** kalıyor ve başka bir istemci (ya da alanı koşullu gönderen
  bir gelecek sürüm) yazdığı anda ortaya çıkıyor.
- **CLAUDE.md karşılığı:** yok (K38 ucu tanımlıyor, kısmi güncelleme semantiğini konuşmuyor)
- **Güven:** **kesin** (sunucu davranışı üretildi) — *tasarım tercihi mi* sorusu açık:
  uç bilinçli olarak "tam değiştir" olabilir. Teyit gerek.

---

### T4-010 — Nefes egzersizi uygulama arka plandayken de tamamlanıyor ve puan yazıyor

> ✅ **düzeltildi** (`fix/tarama-04`, `550aefa`)

- **Önem:** Orta (hak edilmemiş puan; odak seansında bu yol **kapatılmışken** burada açık)
- **Tür:** kod + davranış
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/ui/ekran/nefes/NefesViewModel.kt:122`
  (`viewModelScope` içinde `delay(1_000)` döngüsü, ayrılış denetimi yok)
  · karşılaştır: `…/ui/ekran/odak/OdakViewModel.kt` + `OdakDonusTakibi`
- **Tekrar üretim (cihazda birebir yapıldı):**
  1. Aktiviteler → **Nefes egzersizi** (2 dakikalık kutu nefesi, +5p). Seans otomatik başlıyor.
  2. **Hemen** HOME tuşuna bas — uygulama arka plana düşsün, launcher ön planda kalsın.
  3. 135 saniye bekle (seans 2 dakika).
  4. `gorev_tamamlama` tablosuna bak.
- **Beklenen:** Kullanıcı egzersizi yapmadı; ya seans duraklamalı ya da puan yazılmamalı.
  Odak seansında proje tam olarak bunu yapıyor: ayrılış tespit edilip seans "bozuldu"
  işaretleniyor (`OdakDonusTakibi`, CLAUDE.md §9 "Zaten `RESUMED` sahibe
  `LifecycleObserver` eklemek" tuzağının çözümü).
- **Gözlenen:** Seans arka planda tamamlandı ve **ikinci bir `nefes:5` satırı yazıldı**:

  ```
  ÖNCE : nefes:5,ruh_hali:3
  SONRA: nefes:5,ruh_hali:3,nefes:5
  odak (135 sn boyunca): com.sec.android.app.launcher/…LauncherActivity
  ```
- **Kanıt:** yukarıdaki `run-as` + `sqlite3` çıktısı ·
  `dumpsys window | grep mCurrentFocus` seans başında ve sonunda launcher gösteriyor
- **Neden şüpheli:** `Gorev.NEFES.gunlukLimit = 2` olduğu için bu yol günde **10 puan**
  değerinde; tek başına lig sıralamasını bozacak büyüklükte değil ama puan sistemi
  "yapılan işin karşılığı" iddiasını taşıyor (`docs/spec-puan.md`). Uygulamanın kendi
  ölçüsüne göre yapılmamış bir aktiviteye puan yazmak §4'ün "yapılmayan işi yapıldı
  gösterme" maddesiyle aynı aile. K21'in gerekçesi de nefes için "orada kullanıcı
  **ekrana bakıyor**" varsayımını kuruyor — bu varsayım denetlenmiyor.
- **CLAUDE.md karşılığı:** K21 (varsayımı kuruyor, doğrulamıyor) · odak tarafındaki
  çözümle **tutarsızlık**. Gerekçelendirilmiş bir karar yok.
- **Güven:** **kesin** (cihazda üretildi, odak doğrulamasıyla birlikte)

---

## 4. Düşük

### T4-006 — `AnaViewModel` `Context` tutuyor (§6 "ViewModel asla Context tutmaz")

- **Önem:** Düşük (kod kalitesi/konvansiyon; `@ApplicationContext` olduğu için sızıntı riski yok)
- **Tür:** kod
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/ui/ekran/ana/AnaViewModel.kt:67`
  (`@ApplicationContext private val baglam: Context`) · kullanımı satır 309
  (`ContextCompat.checkSelfPermission`)
- **Beklenen:** §6'nın kuralı: "Composable asla `data/`'ya erişmez. **ViewModel asla Context tutmaz.**"
- **Gözlenen:** `AnaViewModel` kurucu enjeksiyonla `Context` tutuyor.
- **Neden şüpheli:** Projedeki **diğer bütün** ViewModel'ler bu kuralı özellikle
  uyguluyor ve gerekçesini KDoc'a yazmış: `ProfilViewModel.kt:152,177`,
  `SaglikIzinViewModel.kt:91`, `YuruyusViewModel.kt:21,58` hepsi "Context gerekiyor,
  o yüzden bu iş Composable'ın" diyor. `AnaViewModel` tek istisna ve gerekçesi yazılı değil.
  İzin kontrolü (K45 hatırlatıcısı) Composable'dan parametre olarak da geçirilebilirdi —
  `OnboardingViewModel.izinKontrolEt(baglam: Context)` zaten o deseni kullanıyor (tutmuyor, alıyor).
- **CLAUDE.md karşılığı:** **§6 ve §7 kuralının ihlali**; gerekçelendirilmiş karar yok
- **Güven:** **kesin** (kural açık, kod açık) — düzeltilip düzeltilmeyeceği kapsam kararı

---

### T4-007 — Davet kodu ve lig kod alanında çıplak `uppercase()` (§7 yasağı)

- **Önem:** Düşük (bugün **ulaşılamaz**, ama T4-001'in aynı kök sebebi)
- **Tür:** kod
- **Konum:**
  - `app/src/main/kotlin/tr/org/yesilay/yesiliz/domain/lig/DavetKodu.kt:35`
  - `…/ui/ekran/lig/LigViewModel.kt:176, 186, 231`
  - `…/data/depo/RetrofitLigDeposu.kt:80`
  - `…/data/depo/YerelSahteLigDeposu.kt:89`
- **Beklenen:** `uppercase(Locale.ROOT)` — §7 çıplak çağrıyı yasaklıyor, §9 tuzağı adıyla anıyor.
- **Gözlenen:** Locale verilmemiş. Türkçe cihazda `"i".uppercase()` → `"İ"` (doğrulandı).
- **Neden bugün zarar vermiyor:** `DavetKodu.ALFABE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"`
  — `I` ve `O` bilinçli olarak dışarıda (satır 23-24). Geçerli bir kodun küçük harfli
  hâli **hiç `i` içermiyor**, `YSZ` öneki de içermiyor. Yani dönüşüm bugün hiçbir geçerli
  kodu bozmuyor.
- **Neden yine de raporlanıyor:** Koruma **tesadüfi ve yazılı değil** — alfabe bir gün
  `I` içerecek şekilde genişletilirse kusur sessizce ortaya çıkar, ve bu tam olarak
  T4-001'in yaşadığı şey. Ayrıca `LigViewModel.kt:176` **kullanıcının yazdığı her karakteri**
  büyütüyor: Türkçe klavyeden `i` yazan kullanıcı alanda `İ` görüyor.
- **CLAUDE.md karşılığı:** §7 Yasaklar · §9 tuzağı
- **Güven:** **kesin** (kod ve locale davranışı doğrulandı); etkisi bugün yok

---

### T4-008 — Seri hesabında "önceki gün" katı sorgulanıyor; `oncekiGun` yardımcısı kullanılmıyor

- **Önem:** Düşük (yalnızca 30 günden uzun kesintide)
- **Tür:** kod
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/data/depo/PuanDeposu.kt:130`
  (`puanDao.getir(tarih.minusDays(1))`) — karşısında `…/dao/Daolar.kt:284` (`oncekiGun`)
  ve `PuanDeposu.kt:193` (`seriGunu`, **`oncekiGun` kullanıyor**)
- **Beklenen:** K40: "Gün hiç **ölçülmediyse** seri ne artar ne kırılır."
- **Gözlenen:** Bir önceki takvim gününde `puan_gunluk` satırı **yoksa** `onceki` `null`
  olur, `oncekiSeri = 0` alınır ve seri kırılır. `oncekiGun` DAO'su tam bu durum için
  yazılmış ve KDoc'u da bunu söylüyor ("…yazılmadıysa en son bilinen seri neydi") ama
  hesap yolunda **çağrılmıyor**; yalnızca gösterim yolunda (`seriGunu`) çağrılıyor.
  Yani gösterilen seri ile hesaplanan seri farklı kurallar kullanıyor.
- **Neden etkisi dar:** `GunlukOzetWorker.TELAFI_GUN = 30`, yani son 30 gün için satır
  garanti. Boşluk ancak **30 günden uzun** bir kesintiden sonra oluşur — orada serinin
  sıfırlanması zaten savunulabilir.
- **CLAUDE.md karşılığı:** K40 ile kısmi gerilim; gerekçelendirilmiş karar yok
- **Güven:** **muhtemel** — cihazda üretilmedi (30+ günlük takvim kaydırması gerekirdi),
  kod okumasından çıkarıldı

---

### T4-009 — Ana ekrandaki ağaç "evre 1"de bile tam boy yetişkin ağaç

- **Önem:** Düşük (metafor/tasarım)
- **Tür:** kod (görsel varlık)
- **Konum:** `app/src/main/res/drawable-nodpi/downscaled_tree_phase_1.png`
  → `…/ui/ekran/ana/AnaEkran.kt:462` · eşikler `AnaViewModel.kt:396-403`
- **Gözlenen:** Hesap **4 puandayken** (evre 1) ana ekranda gövdesi ve dalları tam
  gelişmiş, yalnızca yaprakları seyrek bir ağaç görünüyor (`/tmp/tarama-04/02-acilis.png`).
  Evre 9 ile evre 1 arasındaki fark **yaprak yoğunluğu**; boyut ve gövde aynı.
- **Neden şüpheli:** K1 lig adlandırması (Fidan → Filiz → Ağaç → Koru → Orman) ve K2'nin
  "9 büyüme evresi" dili bir **fidandan ormana** büyüme anlatıyor. Görsel set bunu
  karşılamıyor: kullanıcı ilk günden itibaren yetişkin bir ağaç görüyor, dolayısıyla
  "büyüttüğü" hissi zayıflıyor.
- **CLAUDE.md karşılığı:** K2 (evre sayısı ve puan eşikleri kayıtlı; **görsellerin
  neye benzediği** kayıtlı değil)
- **Güven:** **kasıtlı olabilir, teyit gerek** — teslim edilen 6/9 evrelik illüstrasyon
  setinin doğası bu olabilir; tasarım sahibinin kararı

### T4-011 — Doküman sapması: test sayıları CLAUDE.md §11 ile uyuşmuyor

- **Önem:** Düşük (dokümantasyon)
- **Tür:** kod/doküman
- **Konum:** `CLAUDE.md §11` ("Toplam 484 test … 107 backend pytest")
- **Gözlenen (bu makinede, bugün koştu):**

  | Süit | CLAUDE.md | Ölçülen |
  |---|---|---|
  | `:core-risk:test` + `:app:testDebugUnitTest` | 291 + 30 = 321 | **321** ✅ |
  | `backend` pytest | 107 | **113** |

  Backend: `113 passed in 4.90s`. JVM: `BUILD SUCCESSFUL`, XML raporlarında toplam 321.
  Cihaz/emülatör süiti (`connectedDebugAndroidTest`) bu turda **koşturulmadı** (bkz. §6).
- **Neden şüpheli:** Sayı yukarı gitmiş, yani testler eklenmiş ama §11 güncellenmemiş.
  Zararsız; yalnızca "toplam 484" rakamına dayanan bir okuyucu yanılır.
- **CLAUDE.md karşılığı:** yok
- **Güven:** **kesin**

---

## 5. Regresyon şüphesi

Bu turdaki en değerli iki bulgu, **daha önce adı konmuş** tuzakların başka
dosyalarda tekrarı:

| Bulgu | Tekrarlayan tuzak | Nerede kapatılmıştı, nerede geri geldi |
|---|---|---|
| **T4-002** | **§9 "Satır var ≠ veri var"** | `AnaViewModel` (`veriVarMi = ozet?.olayVarMi == true`), `IstatistikViewModel`, `PuanDeposu`, `GunlukOzetWorker` düzeltildi — **`widget/WidgetVerisi.kt:81` atlandı** ve orada hâlâ `ozet != null`. Aynı gün, aynı veriyle iki yüzey zıt şey söylüyor. |
| **T4-001** | **§9 "Çıplak `uppercase()`" / §7 yasağı** | `YerelOturumSiniri` `Locale.ROOT` kullanıyor (doğru), `TurkceMetin.kt` bu iş için var — **`RetrofitHesapDeposu.kt:44,48,52` çıplak `lowercase()` kullanıyor**. Sonucu hesap kilitlenmesi. |
| T4-007 | Aynı tuzak, ikinci yer | `DavetKodu`/`LigViewModel`/`RetrofitLigDeposu` çıplak `uppercase()`. Bugün alfabe sayesinde zararsız. |

**Not:** T4-003 (nudge) bir regresyon **değil** — daha önce hiç raporlanmamış,
yeni bir mantık hatası.

---

## 6. Test edilemedi — ve nedeni

| Konu | Neden koşturulamadı |
|---|---|
| **Risk bandı, trend, histerezis, güvenlik kuralı** (senaryo 12'nin "yüksek risk" tarafı) | Cihaz fiilen kullanılmayan bir test telefonu: en yüksek günlük ölçüm **10,9 dk**, `Sabitler.GUN_ASGARI_DK = 15`. Dört `risk_sonuc` satırının hepsi `YETERSIZ`. **K30'un doğru çalıştığı doğrulandı** (bant hiç gösterilmiyor, giriş kartı `IstatistikEkran.kt:94` `durum.riskBandi?.let` ile gizleniyor) ama bandın **gösterildiği** kollar hiç çalışmadı. Gerçek bir kullanım günü gerekiyor. |
| **`VeriDurumu.ESKI`** (3 günden eski veri "güncel" görünmesin) | Aynı sebep — `HESAPLANDI` bir satır hiç oluşmadı, `RiskDetayViewModel.etkinVeriDurumu` bu kola hiç giremedi. |
| **RiskDetayEkran'ın "Henüz yeterli veri yok" boş durumu** | Ekrana giden **tek** giriş noktası (`IstatistikEkran` risk kartı) `YETERSIZ` durumunda gizlendiği için ekran gezinmeyle **ulaşılamıyor**. Tasarım gereği doğru; ama boş durumun kendisi gözlenemedi. |
| **Senaryo 15 — kayıt → giriş → lig turu (manuel ayar yapmadan)** | **Bilinçli olarak yapılmadı, iki sebeple:** (1) Saf hâli canlı Railway'e `POST /auth/kayit` demek; prompt §0 canlıya yazmayı yasaklıyor. (2) Yerel backend'e çevirip yeni hesap açmak `YerelOturumSiniri.hesabaGec` ile **cihazdaki bütün yerel geçmişi silerdi** (`gunluk_ozet`, `puan_gunluk`, `risk_sonuc`) — prompt §1 bu veriyi taramanın en değerli girdisi sayıyor. **Kısmî kanıt var:** mevcut hesapla canlı sunucuya karşı `GET /lig` **çevrimdışı uyarısı olmadan** 200 döndü ve süresi **dolmuş** erişim jetonu sessizce yenilendi (aşağıda). |
| **Senaryo 8 — aktif yürüyüş, ekran kapalıyken 15 dk** | 15 dakikalık gerçek zamanlı bir tur ve telefonun cepte taşınması gerekiyor; adb ile sürülemez. Yürüyüş ekranı açıldı ve **"Sayaç arka planda da ilerliyor"** ifadesi ile `foregroundServiceType` yapılandırması görüldü, ama monotonik saat doğrulaması (K21/§9 `delay` tuzağı) **yapılmadı**. |
| **Senaryo 8'in `ACTIVITY_RECOGNITION` reddi kolu** | Cihazda izin **verilmiş** durumda (`granted=true`). Reddedip test etmek izni kalıcı olarak değiştirmek demekti; prompt izin durumunu bozmamayı ima ediyor ve reddedilmiş izin sonraki ölçümleri kirletirdi. |
| **Senaryo 14 — saat dilimi değişimi** | Cihaz saat dilimini değiştirmek gerekiyordu; geri almayı unutma riski ve süren ölçümleri (bugünkü `gunluk_ozet`) bozma riski nedeniyle **yapılmadı**. Gece yarısı dönüşü de doğal olarak gözlenemedi (tarama 09:30–10:30 arası koştu). |
| **Senaryo 5 — ruh hali 23:50 / 00:30 penceresi** | Saat manipülasyonu gerektiriyor (yukarıdaki sebep). **Sabah penceresi tam olarak doğrulandı** (aşağıda), gece yarısı sınırı doğrulanmadı. |
| **T4-002'nin ekrandaki görüntüsü** | Widget sağlayıcısı kayıtlı ama cihazda **yerleştirilmiş örneği yok**; One UI'da widget yerleştirme adb ile güvenilir sürülemiyor. Kod yolu ve veritabanı koşulu kesin, **görsel** teyit elle widget eklemeyi gerektiriyor. |
| **`:app:connectedDebugAndroidTest` (56 cihaz testi)** | Koşturulmadı — cihazdaki uygulama verisini değiştirme riski taşıyor (`GecisTest` veritabanı kuruyor). JVM (321) ve backend (113) süitleri koştu. |
| **Ödül seçiminin UI'dan gönderilmesi** | Hesap 44 puanda; en düşük maddi kademe 2.000. Kademe **kilitli** görünüyor (K28 doğrulandı) ama "seç" akışı puan yetmediği için hiç açılmıyor. Sunucu tarafı `503` + satır yazmama **doğrudan curl ile** doğrulandı. |
| **YEDAM köprü ekranı (`YedamOnayEkran`)** | Yalnızca `Bant.COK`'ta açılıyor; risk bandı hiç oluşmadığı için ulaşılamadı. Metni **statik olarak** okundu ve dürüst bulundu (aşağıda). |
| **A12 hız sınırı** | Bilinen açık madde; yerel tek süreçli backend'de anlamlı ölçüm yapılamaz, canlıya yük bindirmek yasak. |

---

## 7. Doğrulanan kararlar — regresyon YOK

Bunlar bilinçli olarak **kırılganlık listesi** (prompt §4.5) üzerinden sınandı ve
**çalışıyor** bulundu. Bulgu değil; kapsamın kanıtı.

| # | Karar | Nasıl doğrulandı |
|---|---|---|
| K41 | Ruh hali üç pencere, aynı pencerede ikinci kayıt puan vermez | Cihazda: 1. kayıt → `ruh_hali` satırı `2026-08-17/sabah`, `gorev_tamamlama` `ruh_hali:3`, puan **41→44**. 2. kayıt (aynı pencere) → satır **artmadı**, görev **yazılmadı**, puan **44'te kaldı**. Saat 10:12, etiket doğru: "Sabah". |
| K46 | Bildirim silme + geri al + toplu silme onayı | Kaydırma → `bildirim` satır sayısı **7→6**, "Bildirim silindi / Geri al" Snackbar'ı çıktı, Geri al → **6→7**. Toplu silme onay soruyor ("Bu işlem geri alınamıyor"), kırmızı kullanılmıyor. Tek yönlü kaydırma. |
| K47 | Odak süre tekerleği 5–120 dk, 5'er adım | Tekerlek üst uca **120 dk**, alt uca **5 dk** kaydırıldı; ara değerler 5'er artıyor. Hızlı çipler 15/25/45 duruyor. Ekran metni "süre dolarsa 20 puan" — puan **süreye bağlı değil**. |
| K49 | Grafik ekseni veriden türüyor | Ölçüm 2 dk iken eksen **`2dk / 1dk / 0`**; ölçüm 8 dk'ya çıkınca **`8dk / 4dk / 0`**. Sabit `8s/4s/0` yok. Bir saatin altı doğru gösteriliyor. |
| K30 | Yetersiz ölçümlü günde bant gösterilmiyor | Dört `risk_sonuc` satırının tamamı `veriDurumu = YETERSIZ`; ana ekranda bant **yok**, "Ölçmeye yeni başladık" boş durumu var; İstatistik'te risk giriş kartı **hiç çizilmiyor**. |
| K25 | Ebeveyn eşleştirmesi çevrimdışı yedeğe düşmüyor | Ağ kapalı + rastgele kod `483927` → **"Sunucuya ulaşılamadı. Eşleştirmeyi bağlantın olduğunda tekrar dene."** Panel **açılmadı**. Kod üretimi de sahte kod uydurmadı. Logcat: `POST /ebeveyn/eslestir` → `UnknownHostException`. |
| K28 | Maddi ödüller seçilemiyor, sunucu 503 + satır yazmıyor | UI: kademeler "**Planlanan** market kuponu" diye görünüyor, alt sayfa "sponsorluk ve teslimat altyapısı henüz etkin değil" diyor. Sunucu: `POST /odul/sec` → **503**, `GET /odul/secimlerim` → **`[]`**. |
| K37 | YEDAM sahte başarı iddiası yok | `YedamOnayEkran.kt:103-105`: "**Bu ekrandan YEDAM'a hiçbir veri veya iletişim talebi gönderilmez.** 115'e dokunduğunda yalnızca telefon çeviricisi açılır; aramayı sen başlatırsın." Bilgi alt sayfası da yalnızca gerçek 115 hattını veriyor. |
| K38 | Profil: e-posta salt okunur, şifre mevcut şifre istiyor | UI: e-posta alanı dolu ve "**E-posta adresi şu an değiştirilemiyor.**" yazıyor; "Yeni Şifre" doldurulunca **"Mevcut Şifren"** alanı beliriyor. Sunucu: mevcut şifresiz → **400**, yanlış şifreyle → **401**, doğru şifreyle → **200** ve eski şifre artık **401**, yeni şifre **200**. `eposta` alanı şemada **hiç yok**. |
| K36 | Yenileme jetonu özet olarak saklanıyor, tek kullanımlık | DB'deki değer istemcideki ham jetonun **SHA-256'sı** ile birebir eşleşti. Aynı jetonla **6 eşzamanlı** `POST /auth/yenile` → tam **1 tanesi 200**, 5 tanesi 401; sonradan tekrar denemek de 401. |
| K42 | Lig 401'i ağ hatasından ayırıyor (sunucu tarafı) | `GET /lig` jetonsuz **401**, bozuk jetonla **401**, geçerli jetonla **200**. |
| K18 | Çevrimdışı yedek dürüstçe söyleniyor | Ağ kapatıldı → Lig "**Lig şu anda kullanılamıyor / Sunucuya ulaşılamıyor**" (`LigEkran.kt:344` `cevrimdisi` kolu). Uydurma sıralama **gösterilmedi** (yerel sahte depo bu hesap için boş). Ağ açılınca gerçek veri geri geldi. |
| K44 | Launcher ikonu maskede kırpılmıyor | Squircle maskesi altında telefon+hilal işareti **tam görünüyor** (`/tmp/tarama-04/44-ikon-kirpik.png`). `roundIcon` da adaptive ikona işaret ediyor; foreground PNG'leri beş yoğunlukta 108dp tuvalde. Etiket `uygulama_adi = 'Yeşilİz'`, `İ` = `U+0130` (doğru). |
| K19 | Çapraz hesap sınırı | A hesabı `POST /ozet` ile veri yazdı; **B hesabının** jetonuyla `GET /ozet?bas=…&bit=…` → **`[]`**. B'nin `GET /ebeveyn/ozet` isteği → **404 "Eşleşmiş çocuk cihazı yok"**. |
| — | Jeton yenileme gerçek cihazda | Uygulama açılırken erişim jetonunun süresi **dolmuştu** (`exp` 09:18, saat 09:54). Lig sekmesi açılınca `JetonAuthenticator` sessizce yeniledi: yeni jeton `iat 09:54:10 / exp 10:24:10`, ekran **çevrimdışı uyarısı göstermedi**. |
| — | Mimari sınırlar | `core-risk` içinde **hiç** `android.*` importu yok. `ORDER BY tarih` kullanan sorgularda eşitlik bozucu eksikliği **yok** (`gunluk_ozet`/`risk_sonuc`/`puan_gunluk`'ta `tarih` birincil anahtar; `anket_yanit` zaten `tarih, id`). `MutableStateFlow.update` bloğu içinde askıya alan çağrı **yok**. Paylaşılan `sheetState` **yok** (hepsi `AltSayfa` üzerinden). `CancellationException` 13 dosyada **sistematik olarak yeniden fırlatılıyor**. `TODO/FIXME/HACK` **yok**. `Sabitler`, `PuanSabitleri`, `NudgeSabitleri` üyelerinin **hepsi** kullanılıyor (`KORUMA_PENCERESI_GUN` dahil — K40 ile bağlandı). |

---

## 8. Bilinen kararlar / açık maddeler — tartışılmadı

- **K50** — `dev` dalında `NudgeSabitleri.ESIK = 15 sn`. Bildirim defterindeki
  "**0 dakikadır** aralıksız ekrandasın" metni bunun bilinen sonucu; `main`'de 45 dk.
- **A4** — Ödül finansmanı yok; maddi kademeler `PLANLANAN` (K28 fail-closed, doğrulandı).
- **A12** — Paylaşımlı hız sınırı yok; yerelde ölçülemedi.
- **A13** — Lig puanı istemci beyanlı; `POST /ozet` ile gönderdiğim değer sorgusuz kabul edildi.
- **A14** — Jetonlar Keystore ile şifrelenmiyor; `files/datastore/jetonlar.preferences_pb`
  içinden `run-as` ile **düz metin** okunabildi (debug derlemesinde, beklenen).
- **A15** — E-posta sağlayıcısı yok. **T4-001'i ağırlaştırıyor:** yanlış adrese açılmış
  bir hesabın kurtarma yolu da yok.
- **A16** — Health Connect `1.2.0-alpha01`; cihazda HC verisi yok, adım `SENSOR`'dan geliyor
  (`gunluk_ozet.adimKaynagi = SENSOR`, K27'nin donanım sayacı kolu çalışıyor).
- **A1/A2/A3/A5/A6** — pilot verisi bekliyor; bu taramanın konusu değil.

---

## 9. Cihaz/ortam değişiklikleri — hepsi geri alındı

| Değişiklik | Durum |
|---|---|
| `adb shell svc power stayon usb` (ekran uykuya dalmasın) | **Geri alındı** (`svc power stayon false`). `screen_off_timeout` **değiştirilmedi** (300000 olarak kaldı). |
| `svc wifi disable` / `svc data disable` (çevrimdışı testi) | **Geri alındı**, ağ doğrulandı (`Active default network: 180`). |
| İzinler | **Hiçbiri değiştirilmedi.** |
| Saat dilimi / sistem saati | **Değiştirilmedi.** |
| Uygulama verisi | `pm clear`/`uninstall` **yapılmadı**; `installDebug` üstüne kuruldu, veri korundu. |
| Test sırasında oluşan veri | Nefes ×2 (+10p), ruh hali (+3p), TikTok ~1 dk kullanım, 7 nudge bildirimi. **Cihazdaki gerçek veriye eklendi** — bunlar tarama artefaktıdır, sonraki yorumda dikkate alınmalı. |
| Yerel backend | `docker compose` **ayakta bırakıldı**; içinde `t4tarama.com` alan adlı üç test hesabı var. Canlıya **hiçbir yazma isteği gitmedi** (yalnızca uygulamanın kendi normal `GET /lig` trafiği). |

---

## 10. Özet tablo

| ID | Önem | Tür | Başlık | Güven |
|---|---|---|---|---|
| **T4-001** | **Kritik** | kod | E-posta `lowercase()` çıplak; tr-TR'de `I`→`ı`, hesap yanlış adrese açılıyor ve giriş 401 | kesin |
| **T4-002** | **Kritik** | kod | Widget `ozet != null` diyor; ölçülmemiş günde `%0 · 0dk` gösteriyor (§9 tuzağı tekrarı) | kesin |
| **T4-003** | **Kritik** | kod+davranış | Nudge, Yeşilİz'in kendi içindeyken tetikleniyor; kendi nefes egzersizini bölüyor | kesin |
| **T4-004** | Orta | kod | Nudge kartındaki hilal filigranı (`alpha 0.9`) metni okunmaz kılıyor | kasıtlı olabilir |
| **T4-005** | Orta | backend | `PATCH /auth/profil` gövdede `avatar` yoksa kayıtlı avatarı siliyor | kesin (tasarım mı? teyit) |
| **T4-010** | Orta | kod+davranış | Nefes egzersizi arka planda tamamlanıp puan yazıyor (odakta bu yol kapalı) | kesin |
| **T4-006** | Düşük | kod | `AnaViewModel` `Context` tutuyor (§6 ihlali) | kesin |
| **T4-007** | Düşük | kod | Davet kodu/lig alanında çıplak `uppercase()` (bugün alfabe sayesinde zararsız) | kesin |
| **T4-008** | Düşük | kod | Seri hesabı `oncekiGun` yerine katı `getir(tarih-1)` kullanıyor (30+ gün kesintide kırılır) | muhtemel |
| **T4-009** | Düşük | kod | Ağaç evre 1 zaten tam boy yetişkin ağaç; "fidandan ormana" metaforunu karşılamıyor | kasıtlı olabilir |
| **T4-011** | Düşük | doküman | CLAUDE.md §11 test sayıları güncel değil (backend 107 → **113**) | kesin |

**Kritik 3 · Orta 3 · Düşük 5 — toplam 11 bulgu.**
İkisi (**T4-001, T4-002**) daha önce adı konmuş §9 tuzaklarının başka dosyalarda tekrarı.

---

> **Bu rapor tespit amaçlıdır.** Hiçbir kaynak dosya değiştirilmedi, hiçbir düzeltme
> uygulanmadı, hiçbir git yazma komutu çalıştırılmadı. Değiştirilen tek dosya bu rapordur.

---

## 11. Ek bulgular (düzeltme oturumu · 17 Ağu 2026)

Aşağıdakiler **T4-003 · T4-002 · T4-010 · T4-005** düzeltme oturumunda
görüldü. Kapsam o dört bulguyla sınırlı olduğu için hiçbiri düzeltilmedi;
karar kullanıcıya ait.

### E-1 — T4-001 ve T4-007 büyük olasılıkla **yanlış pozitif**: Kotlin'in `lowercase()`/`uppercase()` zaten `Locale.ROOT`

- **Önem:** T4-001 raporda **Kritik** olarak listeleniyor; bu doğruysa öyle değil
- **Kanıt:** Kotlin stdlib 2.x JVM gerçeklemesi (`kotlin-stdlib` kaynak jar'ı,
  `jvmMain/kotlin/text/StringsJVM.kt`):

  ```kotlin
  147: public actual inline fun String.uppercase(): String = (this as java.lang.String).toUpperCase(Locale.ROOT)
  168: public actual inline fun String.lowercase(): String = (this as java.lang.String).toLowerCase(Locale.ROOT)
  ```

  Yani `RetrofitHesapDeposu.kt:44,48,52`'deki argümansız `lowercase()` çağrıları
  **cihaz yereline bakmıyor**; `"ALI@X.COM".lowercase()` Türkçe cihazda da
  `ali@x.com` veriyor. Raporun T4-001 kanıtı olan `Cas.java` çıktısı
  **Java'nın** `String.toLowerCase()`'i (yerele bağlı) ile üretilmiş; Kotlin'in
  aynı adlı ama farklı davranan fonksiyonu değil. Backend'e karşı üretilen
  401 de bu yüzden istemcinin davranışını göstermiyor — istek elle
  `alı@…` yazılarak gönderilmiş.
- **Aynısı T4-007 için de geçerli:** `DavetKodu`/`LigViewModel`'deki argümansız
  `uppercase()` de `Locale.ROOT`; `"i".uppercase()` Türkçe cihazda `"İ"` değil
  **`"I"`** veriyor. T4-007'nin "bugün zararsız ama tesadüfen" gerekçesi de
  düşüyor: koruma tesadüfi değil, stdlib sözleşmesi.
- **Bunun tersi bir tuzak gerçekten var:** CLAUDE.md §9'un "Çıplak
  `uppercase()`" maddesi **görüntülenen metin** için yazılmış — orada
  `Locale.ROOT` yanlış, çünkü Türkçe `i → İ` istiyor; `TurkceMetin.buyukHarf()`
  tam olarak bunun için var. Kimlik/kod normalleştirmesinde ise `Locale.ROOT`
  **doğru** olandır. §7'nin çıplak çağrı yasağı bu iki durumu ayırmıyor;
  ayırması yararlı olabilir.
- **Öneri:** T4-001 ve T4-007 kapatılmadan önce cihazda **Kotlin** ile
  doğrulanmalı (uygulamanın kayıt ekranından `ALI@…` yazıp giden isteği
  logcat'ten okumak yeter). Doğrulanmadan yapılacak bir "düzeltme" davranışı
  değiştirmez, yalnızca gürültü ekler.
- **Güven:** stdlib kaynağı **kesin**; uçtan uca cihaz doğrulaması yapılmadı
  (kapsam dışı).

### E-2 — Göz molası (`GozMolasiViewModel`) T4-010'un **birebir aynı kusurunu** taşıyor

- **Önem:** Orta (hak edilmemiş puan, T4-010 ile aynı sınıf)
- **Konum:** `app/src/main/kotlin/tr/org/yesilay/yesiliz/ui/ekran/gozmolasi/GozMolasiViewModel.kt:34-43`
- **Gözlenen:** 20 saniyelik sayaç `viewModelScope` içinde, ayrılış denetimi
  **yok**; `basla()` çağrıldıktan sonra uygulama arka plana düşse de seans
  tamamlanıp `Gorev.GOZ_MOLASI` (+3p) yazılıyor. Nefesteki yol (T4-010)
  kapatıldı, buradaki açık kaldı.
- **Neden bu turda düzeltilmedi:** Kullanıcı kararı — prompt kapsamı dört
  bulgu; aynı duraklat/devam mekanizması (`ui/EkranDonusTakibi` +
  `NefesSayac` deseni) hazır, ayrı bir tur olarak uygulanabilir.
- **Güven:** **kesin** (kod okuması; T4-010 ile aynı yapı, cihazda ayrıca
  üretilmedi)

### E-3 — Gece kapanışı **temiz** (bulgu değil)

`GeceKapanisiViewModel` sayaç tutmuyor; puan kullanıcının kendi dokunuşuyla
(`tamamla()`, `hepsiIsaretli` şartıyla) yazılıyor. T4-010/E-2 sınıfına
girmiyor.

### E-4 — Nefes süresi hâlâ `delay(1_000)` biriktirmesiyle ölçülüyor

- **Önem:** Düşük
- **Konum:** `ui/ekran/nefes/NefesViewModel.kt` (`sayaciBaslat`)
- CLAUDE.md §9 "`delay(1000)` = 1 saniye değildir" maddesi burada da geçerli;
  `zaman/GecenSureSaati` zaten var ve odak/yürüyüş onu kullanıyor.
  **Bilinçli olarak bu turda dokunulmadı** (kullanıcı kararı): nefes ekranı ön
  planda ve `EkranAcikTut` ekranı açık tuttuğu için erteleme riski odak/yürüyüş
  kadar ağır değil, ayrıca duraklatma eklendikten sonra monotonik saatle
  "yalnızca ön planda geçen süre" ölçmek duraklatılan aralıkların düşülmesini
  gerektiriyor — kendi turunu hak eden ayrı bir tasarım.

### E-5 — Nudge overlay'i kullanıcıyı takip ediyordu (T4-003 kapsamında düzeltildi)

Raporun `07-nudge-yesiliz-uzerinde.png` kanıtının bir bileşeni tekrar
tetiklenme değildi: TikTok'ta **doğru biçimde** tetiklenen overlay, kullanıcı
Yeşilİz'e geçtiğinde otomatik kapanma süresi (60 sn) dolana kadar ekranda
kalıyordu. Düzeltme oturumunda cihazda görüldü ve T4-003 ile birlikte
kapatıldı: overlay dururken de ön plan sorulmaya devam ediyor, kullanıcı
tüketim kategorisinden çıktığı anda overlay kapanıyor
(`NudgeOverlayService.izlemeDongusu`).
