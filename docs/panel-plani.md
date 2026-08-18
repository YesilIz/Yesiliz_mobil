# panel-plani.md — Yönetim Paneli ve YEDAM Paneli

> **Durum:** uygulanıyor · 18 Ağustos 2026 · dal `feature/panel`
> Bu belge kararları ve iş sırasını kaydeder. Uygulama sırasında ortaya çıkan
> yedi gerekçeli sapma **§1.1**'de (`U1`–`U7`).
> Kaynak: `CLAUDE.md` §4 (mutlak kurallar), §6 (mimari), §12 (kararlar),
> `docs/spec-veri.md` §13, `docs/dagitim.md`.

---

## 0. NİÇİN BU BELGE

Uygulamanın iki yeni insan kullanıcısı olacak:

| Kim | Ne yapar | Bugün nasıl yapıyor |
|---|---|---|
| **Yeşilay/proje yöneticisi** | hesap yönetimi, ödül operasyonu, içerik, izleme, puan düzeltmesi | hiçbir yolu yok — `psql` ile elle |
| **YEDAM danışmanı** | yüksek riskli ve **destek isteyen** kullanıcıyı karşılamak | hiçbir yolu yok — kullanıcı 115'i kendi arıyor |

İkincisi projenin en büyük yarım kalan vaadi. `CLAUDE.md` §3'ün çekirdek
döngüsünün 5. adımı "KÖPRÜ KUR: yüksek risk + izin → YEDAM yönlendirmesi"
diyor; kodda karşılığı `YedamOnayEkran.kt` ve o ekranın KDoc'u aynen şunu
yazıyor: *"Bu sürümün YEDAM'a veri ya da geri arama talebi gönderen bir
entegrasyonu yoktur."* Köprünün kullanıcı tarafı var, YEDAM tarafı yok.
Bu plan o tarafı kuruyor.

**Bu bir demo planı değil.** Kullanıcı kararı: gerçek kullanıma hazır sistem,
takvim baskısı gözetilmeden. Dolayısıyla KVKK rejimi, denetim izi, rol
yönetimi ve rıza sürümlemesi baştan planın içinde.

---

## 1. BU OTURUMDA ALINAN KARARLAR

| # | Konu | Karar | Niçin |
|---|---|---|---|
| P1 | Platform | **Yalnızca web, masaüstü öncelikli** | Panel işi tablo, arama, filtre, çoklu kayıt karşılaştırması. Danışman zaten bilgisayar başında ve kurum ağında. Ayrı APK dağıtımı, imzalama ve sürüm uyumu derdi yok; tarayıcıda güncelleme anında yayılıyor. Mobil, salt okunur bir görünüm olarak sonra eklenebilir |
| P2 | Kimlik alanı | **Personel hesapları son kullanıcıdan tamamen ayrı tabloda** (`personel`) | Aşağıda §2 |
| P3 | Giriş yönlendirmesi | **Tek personel giriş sayfası, role göre yönlendirme**: `admin` → `/yonetim`, `yedam_*` → `/yedam` | Kullanıcının istediği davranış bu; ama son kullanıcı girişiyle **aynı formda değil**, ayrı alan adında |
| P4 | Mobil uygulama personel hesabıyla açılmaz | Uygulamada personel girişi denenirse mevcut "E-posta ya da şifre hatalı" yanıtı döner (**U3**: özel mesaj yazılmıyor) | Rol yönlendirmesi kodu hiç yazılmıyor — iki kimlik alanı birbirini tanımıyor. Unutulan tek uç yüzünden adminin son kullanıcı akışına düşmesi mümkün değil |
| P5 | YEDAM danışmanının göreceği veri | **Tam risk detayı + sürükleyiciler** — ama yalnızca açık rıza kapısının arkasında, süreli ve her görüntüleme kullanıcıya görünür şekilde kaydedilerek | Kullanıcı kararı. §4'ün mahremiyet iddiasıyla gerilimi ve azaltma yolu §8'de |
| P6 | Admin puan müdahalesi | **Ekleme ve düşürme, gerekçe zorunlu, denetim kaydı zorunlu** — kullanıcıya "ceza" değil "düzeltme" olarak görünür, kazanılmış ödül kademesini geri almaz | Kullanıcı kararı. §4 "Ceza puanı yok" ile gerilimi ve azaltma yolu §9'da |
| P7 | Teknoloji | **FastAPI + Jinja2 + HTMX**, sunucu tarafında render, çerez oturumu | §3 |
| P8 | Dağıtım | **Tek kod tabanı / tek imaj, iki ayrı servis** — API ve panel farklı alan adlarında, farklı ağ kısıtlarıyla | §4 |
| P9 | Admin varsayılan olarak tekil kullanıcı davranış verisini **göremez** | Toplu istatistik serbest; tekil kullanıcının kategori/örüntü/skor verisi ancak KVKK talebi işlemi olarak, gerekçeyle ve denetim kaydıyla | §7'de gerekçesi. Kullanıcı bunu talep etmedi, ben sınır olarak koyuyorum — kaldırılması ayrı bir karar olmalı |

---

## 1.1 UYGULAMA SAPMALARI

Uygulamaya başlarken kod tabanında üç bulgu çıktı ve dört tasarım kararı
belgedekinden ayrıldı. Hepsi gerekçeli; belge bu satırlarla birlikte geçerli.

**Numaralandırma:** sapmalar `U`, §14'ün açık kararları `S`. İkisi ayrı diziler.

| # | Belgede yazan | Bunun yerine | Niçin |
|---|---|---|---|
| U1 | Sürükleyiciler `POST /ozet` gövdesine alan olarak ekleniyor (§11) | Ayrı uç **`POST /yedam/surukleyici`** | Yeşilay paylaşımı kapalı kullanıcıda `SenkronDeposu.senkronEt()` ilk satırda dönüyor, yani `/ozet` **hiç çağrılmıyor** — özellik tam da §8'in korumak istediği kullanıcıda sessizce ölü olurdu. Ayrıca iki rızanın fiziksel ayrımı, §8'in KVKK argümanının yaşadığı yer |
| U2 | CSRF jetonu tek kullanımlık (§2) | Oturuma bağlı HMAC jetonu | Tek kullanımlık jeton HTMX kısmi güncellemeleriyle ve iki açık sekmeyle çalışmıyor: ikinci sekme ve her paralel istek kırılıyor. Standart savunma zaten oturuma bağlı jeton + `SameSite=Strict` + `Origin` denetimi. Tek kullanımlık jetonun doğru yeri **yalnızca puan düzeltme formu**, orada idempotency anahtarı olarak |
| U3 | Mobilde personel girişinde 401 + "Bu hesap panel hesabı" (P4) | Mevcut "E-posta ya da şifre hatalı" yanıtı | Özel mesaj, API servisinin `personel` tablosuna bakmasını gerektiriyor — P2'nin reddettiği bağın ta kendisi. Ayrıca personel e-postasının varlığını sızdırırdı. Personel e-postası `kullanici` tablosunda yok; mevcut yanıt zaten doğru |
| U4 | — | `pyotp` eklenmiyor, TOTP `app/totp.py`'de elle (~35 satır) | Bağımlılık disiplini (K5'in grafik kütüphanesi gerekçesi). TOTP bir kripto *tasarımı* değil, HMAC üstünde tam belirtilmiş bir sayaç kesme algoritması ve **RFC 6238 Ek B resmî test vektörleri** veriyor — doğruluk `core-risk`'in referans Python ile eşleşmesi gibi kanıtlanıyor. QR kodu da üretilmiyor (`qrcode`+`pillow` ~10 personel için orantısız); kurulum ekranı base32 sırrı ve `otpauth://` bağlantısını kopyalanabilir metin olarak veriyor |
| U5 | Redis tabanlı hız sınırı (§12 G1) | F1'de veritabanı tabanlı **hesap kilidi**; Redis F6'da cihaz API'si için | Panel girişi tek süreç ve düşük hacim. `personel.basarisiz_deneme` + `kilit_bitis` (5 denemede 15 dk), paylaşımlı kurum NAT'ının arkasında IP tabanlı sınırdan **daha doğru** bir kontrol. A12'nin gerçek konusu cihaz API'sinin dağıtık sınırı, o F6'da kalıyor |
| U6 | — | Ağaç evresi ve ödül kademesi için `Tercihler`'de tek `en_yuksek_toplam_puan` anahtarı | §9 "ağaç kurumaz" ve K28 "kazanılmış kademe geri alınmaz" iki ayrı kural ama aynı garantiye ihtiyaç duyuyor. Evre saklanırsa kademe için ikinci bir anahtar gerekir ve ikisi ayrışabilir; **ham toplamı** saklayınca ikisi de aynı sayıdan türüyor |
| U7 | — | `SenkronIstek`'e `toplamPuan` alanı | §5.3'ün "mevcut toplam · delta · önizleme" formu bugünkü şemayla yazılamıyor: sunucu yalnızca `LigUyelik.haftalik_puan`'ı biliyor. Değer **bilgilendirici, otorite değil** — A13 aynen geçerli ve form bunu ekranda yazıyor |

### Kod tabanında çıkan üç bulgu

| Bulgu | Sonucu |
|---|---|
| `main.py:40-55` `ayarlar()`'ı **import anında** okuyor ve `ayarlar()` `lru_cache`'li | İki rolü tek pytest oturumunda kurmak imkânsız → "API alan adında panel yolu 404" testi yazılamaz. **Uygulama fabrikası (`uygulama_kur`) ilk commit olmak zorunda** |
| **Yaş hiçbir yere yazılmıyor** — `OnboardingDurum.yas` yalnızca ViewModel state'i, `Tercihler`'de anahtar yok, sunucuda kolon yok | §6.2'nin "yaş bandı" satırı ve 18 altı ebeveyn onayı kapısı bugün render edilemez. `Tercihler.beyanYas` + `yedam_riza.yas_bandi` F3'ün ön koşulu |
| Sunucu kullanıcının **toplam puanını bilmiyor** | U7 |

---

## 2. KİMLİK: PERSONEL AYRI BİR ALAN

Sunucuda `Kullanici.rol` kolonu **zaten var** (`backend/app/modeller.py:52`)
ve yorumu "`kullanici` | `uzman` — uzman paneli arayüzü Faz 3, rol şimdiden
var" diyor. Bu plan o kolonu **kullanmıyor** ve gerekçesi şu:

| Sebep | Açıklama |
|---|---|
| Hukuki sıfat farklı | Son kullanıcı **veri sahibi**, personel **veri işleyen**. KVKK'da iki farklı rejim, iki farklı aydınlatma metni, iki farklı saklama süresi. Aynı tabloda tutmak ilk denetimde sorun olur |
| Yüzey farklı | Personelin ekran süresi verisi, lig üyeliği, ödül kademesi, seri, ebeveyn bağlantısı **olmamalı**. Aynı tabloda olursa her uçta "bu kullanıcı personel mi" diye sormak gerekiyor; bir uçta unutulursa danışman ligde görünüyor |
| Hata sınıfı farklı | `rol` kolonuyla ayrım, **her uç için ayrı ayrı doğru yazılmak zorunda**. Ayrı tabloyla ayrım, yabancı anahtar seviyesinde fiziksel — yanlış yazmak mümkün değil. `CLAUDE.md` §6'nın `core-risk` gerekçesiyle aynı mantık: "derleyici bunu fiziksel olarak engeller" |
| Aynı kişi ikisi de olabilir | YEDAM danışmanı da uygulamayı kullanabilir. Ayrı tabloda iki hesap açar, hiçbir şey çakışmaz. Tek tabloda bu imkânsız |

**Sonuç:** `kullanici.rol` kolonu yerinde kalır (migration ile silmek şu an
kazanç getirmiyor) ama yeni kod ona bakmaz; yorumu bu belgeye işaret edecek
şekilde güncellenir.

### Roller

| Rol | Görebildiği | Yapabildiği |
|---|---|---|
| `admin` | her şey + personel listesi | personel davet/askıya alma, rol atama, özellik bayrakları, tüm operasyon |
| `operasyon` | gösterge, hesap yönetimi, ödül, içerik | puan düzeltmesi, ödül teslimatı, içerik yayını. Personel yönetimi **yok** |
| `yedam_yonetici` | başvuru havuzunun tamamı, danışman iş yükü | atama, başvuru kapatma, danışman ekleme talebi |
| `yedam_danisman` | **yalnızca kendisine atanmış** başvurular | not yazma, temas kaydı, kapatma önerisi |
| `gozlemci` | yalnızca toplu/anonim istatistik | hiçbir yazma. Yeşilay yönetimi ve raporlama ekibi için |

İlke: **en az yetki**. Bir rolün göremediği veri, ekranda gri de görünmez —
uç 403 döner ve arayüzde o bölüm hiç çizilmez.

### Oturum

| Karar | Değer | Niçin |
|---|---|---|
| Mekanizma | **Sunucu tarafı oturum + HttpOnly çerez** | JWT tarayıcıda iptal edilemez. Personel hesabı askıya alındığında oturumun **o anda** düşmesi gerekiyor; sunucu tarafı oturum tablosu bunu tek `UPDATE` ile yapıyor |
| Çerez | `HttpOnly` · `Secure` · `SameSite=Strict` | XSS ile çalınamaz, siteler arası istek taşımaz |
| CSRF | Oturuma bağlı HMAC jetonu, her form ve HTMX isteğinde (**U2**) | `SameSite=Strict` tek başına yeterli sayılmıyor. Tek kullanımlık jeton HTMX ve çoklu sekmeyle çalışmıyor; tek kullanımlık olan yalnızca puan düzeltme formunun işlem anahtarı |
| İkinci faktör | **TOTP zorunlu** — `admin`, `operasyon`, `yedam_*` için | Danışman paneli sağlık verisine yakın bir yüzey. Tek parolayla korumak gerçek kullanımda savunulamaz |
| Oturum süresi | 8 saat mutlak, 30 dakika hareketsizlik | Vardiya uzunluğu |
| Eşzamanlı oturum | Kayıtlı, listelenebilir, tek tek düşürülebilir | Kaybolan dizüstü senaryosu |

---

## 3. TEKNOLOJİ

**FastAPI + Jinja2 + HTMX + elle yazılmış CSS.**

| Neyi | Niçin |
|---|---|
| Aynı dil, aynı repo | Backend zaten FastAPI ve ekipte Python var. Ayrı bir Node araç zinciri, ayrı derleme, ayrı dağıtım hattı ve ayrı bağımlılık denetimi getiriyor. Panelin işi form ve tablo — SSR'ın tam da güçlü olduğu yer |
| Tarayıcıda jeton yok | SPA olsaydı erişim jetonunu tarayıcıda tutmak gerekirdi; A14'ün (jeton saklama) web karşılığı. Çerez oturumuyla bu sorun hiç doğmuyor |
| HTMX | Tablo filtreleme, sayfalama, satır güncelleme için tam sayfa yenilemesi gerekmiyor; JSON API + istemci durumu de gerekmiyor. Arada duruyor |
| CSS elle | `DESIGN.md §1` paleti ve `Olcu`/`Yaricap` skalası zaten tanımlı — panel aynı palette kurulur. Hazır bir CSS çerçevesi kendi rengiyle gelir; `CLAUDE.md` §12 K5'in grafik kütüphanesi için verdiği kararla aynı gerekçe |
| Grafik | Panel tarafında da **hazır chart kütüphanesi yok** — SVG üretimi sunucuda, ya da küçük bir Canvas çizimi. K5 ile tutarlı |

**Ne zaman SPA'ya geçilir:** danışman ekranında canlı güncelleme (birden çok
danışmanın aynı havuzda çalışması) ya da karmaşık, kaydedilebilir filtre
durumu gerekirse. O gün geldiğinde uçlar zaten JSON dönebiliyor olacak —
Jinja şablonları uçların **üstüne** yazılıyor, yerine değil.

**Kırmızı renk:** `CLAUDE.md` §4 kırmızıyı damgalayıcı olduğu için yasaklıyor;
istisnası (K10) son kullanıcı arayüzündeki iki nokta. **Panel bu kuralın
kapsamı dışında** — kural kullanıcının kendi risk bandını gördüğü yüzey için
konmuştu. Panelde durum renkleri (uyarı/kritik) serbest; damgalanan bir
kullanıcı değil, bir iş kaydının durumu. Bu gerekçe `DESIGN.md`'ye not
düşülecek.

---

## 4. DAĞITIM: TEK İMAJ, İKİ SERVİS

```
                    ┌─────────────────────────────┐
  Android APK ───►  │  api.<alan>                 │  ← yalnızca API router'ları
                    │  YESILIZ_ROL=api            │
                    └──────────────┬──────────────┘
                                   │
                            aynı PostgreSQL
                                   │
                    ┌──────────────┴──────────────┐
  Tarayıcı   ───►   │  panel.<alan>               │  ← yalnızca panel router'ları
                    │  YESILIZ_ROL=panel          │
                    │  + IP kısıtı / VPN           │
                    └─────────────────────────────┘
```

| Karar | Niçin |
|---|---|
| **Aynı imaj, ortam değişkeniyle farklı router** | İki ayrı Python paketi, iki ayrı model kopyası ve iki ayrı migration zinciri bakımı bölerdi. `main.py` `ayarlar().rol`'e göre `include_router` yapıyor; şema, modeller ve migration'lar tek yerde kalıyor |
| **Panel uçları API alan adında hiç mount edilmiyor** | Yanlış yapılandırılmış bir yetki kontrolü bile internete açık olmuyor — yol 404 |
| **Panel servisine IP kısıtı / VPN** | Yeşilay kurum ağı ya da sağlayıcı seviyesinde allowlist. Kimlik doğrulamanın önünde ikinci bir kapı |
| **Migration'ları yalnızca API servisi koşturur** | `baslat.sh` iki serviste birden koşarsa aynı anda `alembic upgrade` çakışır. Panel servisi `YESILIZ_ROL=panel` iken migration atlar, sürüm uyuşmazlığında **açılmaz** |
| CORS | Panel SSR olduğu için tarayıcıdan çapraz istek yok; `izinli_kaynak_listesi` boş kalmaya devam ediyor (`main.py:50`) |

---

## 5. YÖNETİM PANELİ — YETENEKLER

### 5.1 Gösterge

Toplu ve anonim; tekil kullanıcı yok.

- kayıtlı hesap · son 7/30 günde senkron eden cihaz sayısı
- risk bandı dağılımı (yalnızca `veri_durumu = HESAPLANDI` satırlardan — `CLAUDE.md` §12 K30)
- ortalama günlük ekran süresi, trend
- lig katılımı, aktif grup sayısı
- açık YEDAM başvurusu sayısı ve en eski başvurunun yaşı
- `/health` durumu, çalışan sürüm, son migration revizyonu
- son 24 saatteki 5xx sayısı (`gunluk.py`'nin JSON log'undan)

### 5.2 Hesap yönetimi

| İşlem | Not |
|---|---|
| E-posta / kullanıcı no ile arama | Ad ile arama **yok** — ad benzersiz değil, yanlış hesaba müdahale riski |
| Hesap detayı | Kayıt tarihi, son senkron, lig üyeliği, ebeveyn bağı **var/yok**, ödül kademeleri. **Davranış verisi yok** (P9) |
| Askıya alma | Oturumlar düşer, API 403 döner, cihaz sebebi ekranda görür. Veri silinmez |
| Silme | KVKK silme talebi. `DELETE /auth/hesap` ile aynı yolu kullanır; yedeklerden silme prosedürü ayrı ve yazılı olmalı |
| Veri dışa aktarma | KVKK erişim hakkı. Kullanıcının sunucudaki tüm satırları JSON olarak. **Çift onay + gerekçe + denetim kaydı**; indirilen dosya kullanıcının kendisine gider, panele düşmez |
| Ebeveyn bağını kaldırma | Destek talebi senaryosu (`DELETE /ebeveyn/eslesme` sunucu tarafı karşılığı) |

### 5.3 Puan düzeltmesi

§9'da ayrıntılı. Panelden görünen: kullanıcı, mevcut toplam, delta, **serbest
metin gerekçe (zorunlu, en az 20 karakter)**, önizleme, onay.

### 5.4 Ödül operasyonu — A4'ün teknik ayağı

`CLAUDE.md` §12 K28: maddi ödüller şu an `PLANLANAN`, çünkü teslimatı
karşılayacak bir tedarik entegrasyonu yok ve olmayan bir teslimatı "kazandın"
demek §4'ü ihlal ediyor. **Panel bu kapının operasyon yeri:**

- ödül kademesi tanımı ve seçeneklerinin sunucudan yönetilmesi
- kupon stoğu: parti yükleme, kalan adet, son kullanma
- kademeyi hak eden kullanıcı listesi, seçimleri, teslimat durumu (`bekliyor` → `gönderildi` → `teslim edildi`)
- **stok bittiğinde kademe otomatik kapanır** — cihaz "seçilemez" gösterir. K28'in fail-closed davranışı korunuyor

**Karar (18 Ağu):** `MADDI_ODUL_TESLIMATI_ETKIN` sabiti stoğa bağlanıyor —
panelden parti yüklenince kademe **otomatik açılıyor**, stok bitince kapanıyor.
A4'ün teknik ayağı böylece çözülüyor; finansman ayağı hâlâ Yeşilay'da.

A4'ün finansman ayağı teknik değil; panel yalnızca finansman geldiğinde
açılacak kapıyı hazır tutuyor.

### 5.5 İçerik yönetimi

Şu an `bilgi_bankasi.json` **APK'ya gömülü** (`app/src/main/assets/`) — 13
yazı, çevrimdışı çalışsın diye. Panelden yönetilmesi isteniyorsa:

| Karar | Sonuç |
|---|---|
| Kaynak | **(b) seçildi (18 Ağu):** sunucudan servis + cihazda önbellek; gömülü set ilk açılış ve çevrimdışı yedeği olarak kalıyor |

**Seçilen (b).** Çevrimdışı garantisi bozulmuyor (gömülü set yedek), ama yazı
düzeltmesi için APK yayını beklemek gerekmiyor. Bedeli: sürümleme, önbellek
tazeleme ve "hangi sürümü gördü" sorusu.

Aynı soru "Günün Sorusu" widget'ı ve bildirim metinleri için de geçerli.

### 5.6 Duyuru

Şu an uygulamada **push altyapısı yok** (FCM entegre değil). Duyuru istenirse
bu ayrı bir iş kalemi: FCM projesi, jeton kaydı, izin akışı, `CLAUDE.md` §4'ün
"emir kipi / korkutma yok" kuralına uyan metin denetimi. Faz 5'te.

**Karar (18 Ağu): FCM yok.** Duyuru **uygulama içi bildirim defterine**
(`BildirimDeposu`) senkronla iner, kullanıcı uygulamayı açtığında görür.

### 5.7 Geliştirici seçenekleri

| Yapılabilir | Yapılamaz |
|---|---|
| Özellik bayrakları (ödül kademesi açık/kapalı, YEDAM köprüsü açık/kapalı, senkron zorunlu sürüm) | **Risk motoru sabitleri.** `core-risk/Sabitler.kt` cihazda ve saf; `CLAUDE.md` §12 K17 sunucunun skor hesaplamasını yasaklıyor. Sunucudan sabit dağıtmak referans Python ile birebir doğrulanmışlığı bozar. Panel bu sabitleri **yalnızca gösterir** |
| Asgari istemci sürümü ("bu sürümü güncelle") | Cihaz tarafı iş mantığı |
| Bakım modu — API 503, cihaz açıklayıcı ekran gösterir | |
| Test hesabı üretimi (yalnızca `staging` ortamında) | Üretimde sahte veri üretimi. `CLAUDE.md` §11 demo tohumunun `src/debug`'da tutulmasıyla aynı gerekçe |

### 5.8 Denetim kaydı görüntüleyici

Her personel eylemi yazılır ve **silinemez**. Kim, ne zaman, hangi kullanıcıya,
hangi eylem, hangi gerekçe, hangi IP. Filtreleme: personel, kullanıcı, eylem
türü, tarih aralığı.

---

## 6. YEDAM PANELİ — AKIŞ

### 6.1 Uçtan uca

```
CİHAZ                              SUNUCU                        PANEL
─────                              ──────                        ─────
Bant = COK
  │
  ├─ risk detayında YEDAM kartı
  │
  ├─ "Destek istiyorum"
  │
  ├─ AÇIK RIZA EKRANI ──────────►  yedam_riza satırı
  │   · ne paylaşılıyor (madde madde)
  │   · kim görecek (YEDAM danışmanı)
  │   · ne kadar süre (90 gün)
  │   · nasıl geri alınır
  │   · metin sürümü kaydediliyor
  │
  ├─ iletişim tercihi + not ─────►  yedam_basvuru (durum: yeni)
  │
  └─ son 30 günün sürükleyicileri ►  risk_surukleyici (tek seferlik yükleme)
                                          │
                                          ▼
                                   yedam_yonetici havuzda görür
                                          │  atar
                                          ▼
                                   yedam_danisman açar ──► erisim_kaydi YAZILIR
                                          │
                                          ├─ temas kaydı (arandı / ulaşılamadı)
                                          ├─ danışman notu
                                          └─ kapatma
                                          │
CİHAZ ◄───────────────────────────────────┤
  · "Başvurun alındı / danışmana iletildi"
  · "Verilerine 14 Ağu 10:32'de bakıldı"
  · [Rızamı geri çek] ────────────► erişim ANINDA kesilir
```

### 6.2 Danışmanın gördüğü ekran

| Bölüm | İçerik |
|---|---|
| Başlık şeridi | Rıza durumu, rızanın bitiş tarihi, "bu ekranı açtığınız kullanıcıya bildirilir" uyarısı |
| Kimlik | Ad, iletişim tercihi ve değeri, yaş bandı (18 altı ise ebeveyn onayı durumu — `CLAUDE.md` §12 K26) |
| Risk | 90 günlük bant çizgisi, güncel bant, trend, `veri_durumu` |
| Sürükleyiciler | Skoru en çok yükselten faktörler, `core-risk`'in ürettiği açıklanabilirlik çıktısı |
| Davranış | Kategori dakikaları, örüntü değerleri (gece, oturum, açılış, sabah, uyku öncesi, yer değiş) |
| Anket | SAS-SV geçmişi — **şu an sunucuda yok**, yeni alan (§10) |
| Temas | Görüşme kayıtları, danışman notları |

**Ekranda olmayacaklar:**
- uygulama adları ve oturum dökümü — sunucuda **hiç yok**, `modeller.py`'nin mahremiyet sözleşmesi bunu garanti ediyor ve o sözleşme değişmiyor
- teşhis etiketi. `CLAUDE.md` §4 "Teşhis koyma" panelde de geçerli; danışman "bağımlı" ibaresini uygulamadan görmez, kendi klinik değerlendirmesini yapar
- kullanıcı arama. Danışman **arayamaz**; yalnızca kendisine atanmış başvuruyu açar. Rıza vermemiş bir kullanıcıya ulaşmanın hiçbir yolu yok

### 6.3 Başvuru durumları

`yeni` → `atandi` → `temas_kuruldu` → `kapandi`

Ek durumlar: `ulasilamadi` (3 denemeden sonra), `riza_geri_cekildi`
(otomatik, danışman erişimi kesilir), `suresi_doldu` (90 gün).

---

## 7. VERİ MODELİ — YENİ TABLOLAR

Hepsi sunucuda. Alembic revizyonlarıyla gelir.

| Tablo | Alanlar (özet) | Not |
|---|---|---|
| `personel` | id · eposta · ad · sifre_ozet (argon2id) · rol · durum · totp_sir · son_giris · olusturma | `kullanici`dan bağımsız |
| `personel_oturum` | id · personel_id · jeton_ozet · ip · tarayici · olusturma · son_erisim · iptal | SHA-256 özet, `CLAUDE.md` §12 K36 deseni |
| `denetim_kaydi` | id · personel_id · eylem · hedef_tur · hedef_id · gerekce · ip · zaman · detay_json | **Silinemez.** Yalnızca `INSERT` yetkisi |
| `yedam_riza` | id · kullanici_id · metin_surumu · kapsam · verilme · son_gecerlilik · geri_cekme | Metin sürümü değişirse yeniden rıza |
| `yedam_basvuru` | id · kullanici_id · riza_id · durum · iletisim_turu · iletisim_degeri · kullanici_notu · atanan_personel_id · olusturma · kapanis · kapanis_nedeni | |
| `yedam_temas` | id · basvuru_id · personel_id · tur (`arandi`/`ulasilamadi`/`gorusuldu`) · zaman | |
| `danisman_notu` | id · basvuru_id · personel_id · metin · zaman | Klinik not. Kullanıcıya **gösterilmiyor**; saklama süresi ve erişimi ayrı politika |
| `erisim_kaydi` | id · personel_id · kullanici_id · kapsam · zaman | Kullanıcıya gösterilen şeffaflık kaydı |
| `risk_surukleyici` | kullanici_id · tarih · faktor · katki | Yalnızca rıza varken dolar |
| `anket_yanit` | kullanici_id · tarih · madde_puanlari · toplam | Sunucuda yok, YEDAM paneli için gerekiyor. Rıza kapılı |
| `puan_duzeltme` | id · kullanici_id · delta · gerekce · personel_id · zaman · cihaz_uyguladi | §9 |
| `odul_stok` | id · kademe_id · secenek_id · toplam · kalan · son_kullanma | |
| `odul_teslimat` | id · odul_secimi_id · durum · kod · gonderim_zamani · personel_id | |
| `ozellik_bayragi` | anahtar · deger · aciklama · guncelleyen_id · guncelleme | |
| `duyuru` | id · baslik · metin · hedef · yayin_zamani · personel_id | |

**Veri minimizasyonu kuralı:** `risk_surukleyici` ve `anket_yanit` yalnızca
geçerli rızası olan kullanıcılar için dolu durur. Rıza süresi dolduğunda ya da
geri çekildiğinde **satırlar silinir**, sadece erişim kapatılmaz. Cihazda
zaten duruyorlar; sunucuda tutmanın gerekçesi rızaydı, gerekçe bitince veri de
bitiyor.

---

## 8. §4 GERİLİMİ: YEDAM'A TAM RİSK DETAYI

`CLAUDE.md` §4 mutlak kuralı: *"Ham veriyi sunucuya gönderme — çıkarım ve
skorlama CİHAZDA. Sunucuya günlük özet + skor."*

Kullanıcı kararı danışmanın tam risk detayını ve sürükleyicileri görmesi
yönünde. **Kural teknik olarak ihlal edilmiyor:**

| İddia | Bu planla durumu |
|---|---|
| Ham olay (hangi uygulama, ne zaman) sunucuya gitmiyor | **Değişmiyor.** `kullanim_olay` tablosu sunucuda yok ve olmayacak |
| Skorlama cihazda | **Değişmiyor.** Sürükleyiciler cihazda hesaplanmış çıktı; sunucu hesaplamıyor, saklıyor (K17) |
| Sunucuya günlük özet + skor | Kategori dakikaları ve örüntüler **zaten sunucuda** (`GunlukOzet`). Eklenen şey sürükleyici sıralaması ve anket yanıtları |

Yine de bu, sunucuda tutulan kişisel verinin derinliğini artırıyor. Beş koruma
birlikte:

1. **Rıza kapısı.** Sürükleyici ve anket verisi rıza olmadan **hiç gönderilmiyor**. Rıza vermeyen kullanıcının sunucusundaki veri bugünküyle birebir aynı
2. **Süre.** Rıza 90 gün. Dolduğunda veri silinir, danışman erişimi kapanır
3. **Geri çekme.** Uygulamadan tek dokunuş; erişim aynı saniyede kesilir, satırlar silinir
4. **Erişim kaydı kullanıcıya görünür.** "Danışman X, 14 Ağustos 10:32'de risk detayına baktı." Bu KVKK şeffaflık yükümlülüğünün karşılığı ve aynı zamanda projenin en güçlü özgünlük argümanlarından biri — panelin kendisi denetleniyor
5. **Arama yok.** Danışman yalnızca kendisine atanmış başvuruyu açabiliyor. Rıza vermemiş bir kullanıcıya erişim yüzeyi hiç yok

**Sunumda kullanılacak cümle:** *"Ham kullanım olayları hâlâ cihazdan hiç
çıkmıyor. Danışmana giden şey, kullanıcının açık rızasıyla, süreli olarak ve
her bakışı kullanıcıya bildirilerek paylaşılan bir risk özeti."*

---

## 9. §4 GERİLİMİ: PUAN DÜŞÜRME

`CLAUDE.md` §4 mutlak kuralı: *"Ceza puanı — Sadece kazanç. Ceza suçluluk
üretir, uygulamayı sildirir."*

Kullanıcı kararı düşürmenin de mümkün olması yönünde. Kuralın **amacı**
korunacak şekilde kurulacak:

| Sınır | Uygulama |
|---|---|
| Otomatik ceza yok | Hiçbir kural, hiçbir worker, hiçbir davranış puan düşürmüyor. Yalnızca bir insanın, gerekçeli, tekil işlemi |
| Dil | Kullanıcı "ceza" görmüyor. Bildirim: *"Puanında bir düzeltme yapıldı: −120. Gerekçe: ..."*. Emir kipi ve suçlayıcı dil yok (§4 "Emir kipi / korkutma") |
| Kazanılmış ödül geri alınmıyor | K28 "yalnızca kazanç, geri alma yok" korunuyor. Düzeltme puan bakiyesini değiştirir; hak edilmiş kademe ve seçim **kalır** |
| Ağaç kurumaz | `CLAUDE.md` §12 K2: ağaç evresi toplam puana bağlı. Düzeltme sonrası evre düşecekse **düşürülmez** — evre için ayrı bir "en yüksek ulaşılan" değeri tutulur. Kötü bir sonuçla ağacın geri gitmesi tam da K2'nin reddettiği şey |
| Gerekçe zorunlu | En az 20 karakter, denetim kaydına yazılıyor, kullanıcıya gösteriliyor |
| Meşru kullanımlar | hatalı kazanç düzeltmesi (hile/istismar), yanlış işlemin geri alınması, telafi/etkinlik puanı verilmesi |

**Cihaza nasıl ulaşır:** puan cihazda hesaplanıyor ve `puan_gunluk` tablosunda.
Sunucudan gelen düzeltme cihazda **ayrı** bir `puan_duzeltme` tablosuna yazılır
ve toplam gösterilirken eklenir; günlük puan hesabına karışmaz. Böylece
gecelik worker'ın yeniden hesabı düzeltmeyi silmiyor. Room şema sürümü 10,
`GECIS_9_10`.

> **Not:** Bu kural §4'ün mutlak listesinde yazılı. Karar uygulanırsa
> `CLAUDE.md` §4 satırı ve §12'ye yeni bir karar numarası (K56) yazılmalı —
> yoksa bir sonraki ajan kuralı okuyup kodu ona aykırı bulur.

---

## 10. ANDROID TARAFI DEĞİŞİKLİKLERİ

Panel işi yalnızca sunucu işi değil; cihazda da beş yeni yüzey var.

| # | İş | Nerede | Not |
|---|---|---|---|
| M1 | YEDAM rıza ekranı gerçek hâle gelir | `ui/ekran/yedam/` | Şu an yalnızca 115'i çeviricide açıyor. Madde madde rıza metni, sürüm numarası, kabul kutusu. `POST /yedam/basvuru` |
| M2 | Sürükleyici gönderimi | `data/depo/SenkronDeposu` | Yalnızca geçerli rıza varken. `POST /ozet` gövdesine yeni alan |
| M3 | Rıza anında geçmiş yükleme | yeni | Cihazda 30 günlük `risk_sonuc` zaten var; rıza verildiği an tek seferlik yüklenir. Yoksa danışman ilk gün hiçbir şey görmüyor |
| M4 | Başvuru durumu ekranı | `ui/ekran/yedam/` | "Alındı" → "Danışmana iletildi" → "Görüşüldü". Durum sunucudan; uydurma durum yok (§4, K37) |
| M5 | "Verilerine kim baktı" ekranı | Profil › Gizlilik | `GET /yedam/erisim-kaydim`. KVKK şeffaflığı |
| M6 | Rızayı geri çekme | Profil › Gizlilik | Tek dokunuş + onay. `DELETE /yedam/riza` |
| M7 | Puan düzeltmesini çekme ve gösterme | `data/depo/PuanDeposu` · Room 10 | §9 |
| M8 | Askıya alınmış hesap ekranı | `ui/ekran/hesap/` | 403 yanıtı sessizce çevrimdışı sayılmamalı (K18/K42 ailesi) |
| M9 | Bakım modu ekranı | ağ katmanı | Sunucu 503 + bakım gövdesi dönerse açıklayıcı ekran |

**Dikkat:** M2–M3 `SenkronDeposu`'nun rıza kapısıyla (K19) karışmamalı.
Yeşilay paylaşım rızası ve YEDAM rızası **iki ayrı rıza**. Yeşilay paylaşımı
kapalı olan bir kullanıcı YEDAM desteği isteyebilir; o durumda sürükleyiciler
gider, günlük özet **gitmez**. İki kapı bağımsız değerlendirilmeli.

---

## 11. YENİ API UÇLARI

### Panel servisi (`YESILIZ_ROL=panel`)

```
POST   /oturum/giris            e-posta + parola → TOTP adımı
POST   /oturum/totp             kod → çerez
POST   /oturum/cikis
GET    /yonetim/gosterge
GET    /yonetim/kullanici?ara=
GET    /yonetim/kullanici/{id}
POST   /yonetim/kullanici/{id}/askiya-al
DELETE /yonetim/kullanici/{id}
POST   /yonetim/kullanici/{id}/veri-disa-aktar
POST   /yonetim/kullanici/{id}/puan-duzeltme
GET    /yonetim/odul/stok            POST /yonetim/odul/stok
GET    /yonetim/odul/teslimat        POST /yonetim/odul/teslimat/{id}
GET    /yonetim/icerik               POST /yonetim/icerik
GET    /yonetim/bayrak               POST /yonetim/bayrak
GET    /yonetim/duyuru               POST /yonetim/duyuru
GET    /yonetim/denetim
GET    /yonetim/personel             POST /yonetim/personel
GET    /yedam/havuz
GET    /yedam/basvuru/{id}           ← erisim_kaydi YAZAR
POST   /yedam/basvuru/{id}/ata
POST   /yedam/basvuru/{id}/temas
POST   /yedam/basvuru/{id}/not
POST   /yedam/basvuru/{id}/kapat
```

### API servisi (cihaz, mevcut `YesilizApi.kt` genişler)

```
POST   /yedam/basvuru           rıza + iletişim tercihi + not
DELETE /yedam/riza              geri çekme
GET    /yedam/basvurum          durum takibi
GET    /yedam/erisim-kaydim     şeffaflık
POST   /yedam/gecmis            rıza anındaki tek seferlik yükleme (rıza başına bir kez)
POST   /yedam/surukleyici       günlük sürükleyici gönderimi — U1, /ozet'ten AYRI uç
POST   /ebeveyn/yedam-onay      18 altı: eşleşmiş ebeveynin onayı (mevcut router)
GET    /puan/duzeltmeler        sunucudan gelen düzeltmeler
GET    /icerik/bilgi-bankasi    §5.5 (b) — sunucudan servis, cihazda önbellek
GET    /duyuru                  §5.6 — bildirim defterine iner
POST   /ozet                    + toplamPuan alanı (U7). surukleyiciler BURADA DEĞİL (U1)
```

> **U1 hatırlatması:** `POST /ozet` "Yeşilay ile paylaşım" rızasına bağlı
> (K19); `POST /yedam/surukleyici` YEDAM rızasına. İki kapı bağımsız —
> paylaşımı kapalı bir kullanıcı YEDAM desteği isteyebilir ve o durumda
> sürükleyicileri gider, günlük özeti gitmez.

---

## 12. GÜVENLİK VE KVKK

| # | Madde | Durum |
|---|---|---|
| G1 | **A12 artık zorunlu.** Paylaşımlı hız sınırı | `CLAUDE.md` A12 açık. **U5:** panel girişi ve TOTP için F1'de veritabanı tabanlı hesap kilidi (5 denemede 15 dk) — kurum NAT'ı arkasında IP sınırından daha doğru. Redis F6'da, cihaz API'sinin dağıtık sınırı için |
| G2 | Personel parola politikası + TOTP zorunluluğu + kurtarma kodları | yeni |
| G3 | IP allowlist / VPN, panel servisi önünde | yeni |
| G4 | Rıza metni **sürümlü**; metin değişirse yeniden rıza | yeni |
| G5 | Aydınlatma metni: son kullanıcı için güncellenir (YEDAM paylaşımı eklendi), personel için yeni | `Dökümanlar/Yesiliz - Gizlilik Politikası.txt` güncellenmeli |
| G6 | Veri işleyen sözleşmesi (Yeşilay ↔ geliştirici ekip) | hukuki, teknik değil — ama sistemin gerçek kullanıma geçme ön koşulu |
| G7 | Saklama süreleri yazılı olmalı: başvuru, danışman notu, denetim kaydı, erişim kaydı | yeni |
| G8 | Silme talebi **yedeklerde de** karşılanmalı; prosedür yazılı | `docs/dagitim.md`'ye eklenecek |
| G9 | Yedek + geri yükleme tatbikatı | Şu ana kadar hiç denenmedi. Panelin yazdığı veri (danışman notu) geri getirilemezse bu gerçek bir kayıp |
| G10 | Sızma testi / güvenlik gözden geçirmesi, yayına almadan önce | yeni |
| G11 | 18 yaş altı: danışman erişimi ebeveyn onayına da bağlı mı? | **Açık karar** — §14 |

---

## 13. FAZLAR

Takvim yok; **sıra** var. Her faz kendi başına yayına alınabilir.

| Faz | İçerik | Bittiği nasıl anlaşılır |
|---|---|---|
| **F0 · Sözleşme** | Rıza metinleri, aydınlatma metinleri, roller, saklama süreleri, KVKK rejimi. **Kod yok** | Metinler yazılı ve onaylanmış; §14'teki açık kararlar kapanmış |
| **F1 · İskelet** | `personel` + oturum + TOTP + `denetim_kaydi` + boş panel kabuğu + iki servisli dağıtım | Panel alan adından `admin` girip boş bir gösterge görüyor; API alan adında panel yolları 404 |
| **F2 · Yönetim v1** | Gösterge, hesap arama/detay/askıya alma/silme, veri dışa aktarma, denetim görüntüleyici | Bir destek talebi baştan sona panelden çözülüyor, `psql` açılmıyor |
| **F3 · YEDAM uçtan uca** | Cihaz rızası → başvuru → havuz → atama → detay → temas → kapanış; erişim kaydı; geri çekme | Gerçek bir telefonda rıza verilip başvuru açılıyor, danışman görüyor, kullanıcı "bakıldı" kaydını görüyor, geri çekince erişim kesiliyor |
| **F4 · Puan ve ödül** | Puan düzeltmesi (sunucu + cihaz + Room 10), ödül stoğu ve teslimat, K28 kapısının açılması | Panelden verilen puan telefonda görünüyor; bir kupon teslimatı uçtan uca işaretleniyor |
| **F5 · İçerik ve duyuru** | Bilgi bankası sunucudan, günün sorusu, duyuru, FCM | Bir yazı düzeltmesi APK yayını olmadan telefona iniyor |
| **F6 · Sertleştirme** | Redis hız sınırı (A12), IP kısıtı, sızma testi, yedek/geri yükleme tatbikatı, izleme ve alarm | Testlerin raporu var; tatbikat yapılmış |

**F0 gerçekten önce.** Rıza metninin kapsamı `yedam_riza.kapsam` alanının
içeriğini, o da hangi verinin yükleneceğini belirliyor. Metni sonra yazmak,
yazılmış kodu geri almak demek.

---

## 14. AÇIK KARARLAR

Plan bunlar olmadan da ilerler ama bir noktada tıkanır.

| # | Soru | Kim karar verir |
|---|---|---|
| S1 | **Danışman hesaplarını kim açar?** Yeşilay mı, ekip mi? Gerçek YEDAM personeli mi kullanacak, yoksa sistem Yeşilay'a teslim mi edilecek? | Yeşilay ile |
| ~~S2~~ | ~~18 yaş altı kullanıcıda YEDAM rızası kimden alınır?~~ **Kapandı (18 Ağu):** varsayılan *eşleşmiş ebeveyn varsa onayı da gerekli*, eşleşme yoksa çocuğun rızası yeterli. Kural `app/yedam_kapisi.py`'de tek fonksiyonda ve `ozellik_bayragi` ile değiştirilebilir (`eslesmisse`/`her_zaman`/`hicbir_zaman`). Yeşilay kararı gelince tek satır değişir | ~~Yeşilay + hukuk~~ |
| ~~S3~~ | ~~Rıza süresi 90 gün mü?~~ **Kapandı (18 Ağu):** 90 gün, uzatma yolu yok — süre dolunca yeniden rıza. `docs/yedam-riza-metni-v1.md` bunu yazıyor | ~~Yeşilay~~ |
| S4 | Danışman notlarının saklama süresi ve erişimi. Klinik kayıt sayılıyorsa ayrı rejim | Yeşilay + hukuk |
| ~~S5~~ | ~~Bilgi bankası sunucudan mı servis edilsin~~ **Kapandı (18 Ağu):** evet — sunucudan servis + cihazda önbellek; gömülü set çevrimdışı yedek kalıyor | ~~ekip~~ |
| S6 | Panel alan adı ve barındırma: aynı Railway hesabı mı, Yeşilay altyapısı mı | Yeşilay — **kod hazır:** CI `deploy-panel` job'u `vars.RAILWAY_PANEL_SERVICE` ve `vars.PANEL_TABAN` bekliyor |
| S7 | Danışman kullanıcıyla uygulama içinden iletişim kurabilmeli mi? §4 chatbot yasağı bunu kapsamıyor (insan danışman) ama yeni bir yüzey | ekip + Yeşilay |
| S8 | `gozlemci` rolüne gerçekten ihtiyaç var mı, yoksa raporlama için dışa aktarma yeter mi | ekip |

> Kapanan kararlar üstü çizili bırakılıyor, numaraları **yeniden
> kullanılmıyor** (`CLAUDE.md` §12 A7 deseni).

---

## 15. RİSKLER

| Risk | Etki | Azaltma |
|---|---|---|
| Sunucudaki kişisel veri derinleşiyor | Projenin en güçlü mahremiyet iddiası zayıflar | §8'in beş koruması. Rıza vermeyen kullanıcının verisi bugünküyle **birebir aynı** kalıyor |
| Puan düşürme §4 ile çelişiyor | Bir sonraki ajan/geliştirici kuralı okuyup kodu aykırı bulur | §9'un sınırları + `CLAUDE.md` §4 ve §12 K56 güncellenmeli |
| A13 (istemci beyanlı lig puanı) büyüyor | Panel puanı düzeltebiliyor ama kaynağı hâlâ cihaz | Gerçek çözüm sunucu tarafı doğrulama; panel bunu **çözmüyor**, yalnızca düzeltme imkânı veriyor |
| Panel, ekibin dördüncü kod tabanı olur | Android + backend + panel + doküman; 6 kişilik ekipte bakım yükü | Aynı repo, aynı dil, aynı imaj (§4). Ayrı bir Node yığını **bilinçle** reddedildi |
| YEDAM entegrasyonu Yeşilay onayına bağlı | F3 tıkanabilir | F1–F2 bağımsız ilerliyor; F3'ün teknik kısmı Yeşilay olmadan da geliştirilip test hesaplarıyla doğrulanabilir |
| Sunucuya yazılan danışman notu geri getirilemez | Gerçek veri kaybı | G9 tatbikatı F6'da değil, **F3 ile birlikte** yapılmalı |

---

## 16. BELGE BAKIMI

Bu belge `CLAUDE.md` §2'nin dosya haritasına eklenmelidir:

```
| `docs/panel-plani.md` | Yönetim + YEDAM paneli planı, roller, rıza rejimi | panele dokunurken |
```

Kararlar uygulandıkça `CLAUDE.md` §12'ye taşınır ve buradaki ilgili satır
"uygulandı → K__" diye işaretlenir. Bu belge planı tutar; **kararların kalıcı
yeri `CLAUDE.md`**.
