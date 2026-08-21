# panel-yapilacaklar.md — Panel işinin kalan kalemleri

> **Türetildi:** `docs/panel-plani.md` (572 satır, 16 bölüm) ile `feature/panel`
> dalının kod durumu karşılaştırılarak · 21 Ağustos 2026
> **F0–F4 kapandı** (21 Ağustos 2026). Sıradaki faz **F5 (içerik ve duyuru)**.
> Numaralar plan belgesinin bölümlerine bağlı — bir kalemin *niçini* için
> oradaki bölüme bak, burada yalnızca *ne* ve *nerede* var.

---

## 0. BİTEN — tekrar açılmasın

| Faz | Commit | İçerik |
|---|---|---|
| **F0 · Sözleşme** | `1f79708` | Rıza metni `docs/yedam-riza-metni-v1.md` (139 satır) · `CLAUDE.md` §12'ye K56/K57 · yedi sapma (`U1`–`U7`) · S2/S3/S5 kapatıldı |
| **F1/1 · Fabrika** | `2c56a6b` | `uygulama_kur()` · `YESILIZ_ROL` · panel uçları API alan adında mount edilmiyor (`test_servis_rolu.py`) |
| **F1/2 · TOTP** | `cc32aec` | `app/totp.py` elle (U4) · RFC 6238 Ek B vektörleri · sır Fernet ile şifreli |
| **F1/3 · Kimlik** | `7023aeb` | `personel` · `personel_oturum` · `personel_kurtarma_kodu` · `denetim_kaydi` · `ozellik_bayragi` tabloları (`b4c8e1d27f39`) |
| **F1/4 · Oturum** | `6504368` | İki adımlı giriş · HMAC CSRF (U2) · hesap kilidi (U5) · güvenlik başlıkları |
| **F1/5 · Dağıtım** | `67a554b` | `docker-compose` iki servis · CI `deploy-panel` · `surum_kapisi.py` · panel smoke |
| **F2/1 · Gösterge** | `9aa9602` | `panel/gosterge.py` · sunucuda üretilen SVG (`panel/grafik.py`) |

---

## 1. F2 · YÖNETİM v1 — ✅ KAPANDI (21 Ağu 2026)

**Bitiş ölçütü (plan §13):** bir destek talebi baştan sona panelden çözülüyor,
`psql` açılmıyor.

**Kapanış commit'leri:** `2fb89b7` (F2/2 arama·detay·silme kaskadı) ·
`ef1ac18` (F2/3 askıya alma + Android M8) · `b3bf990` (F2/4 KVKK) ·
`0e7c292` (F2/5 denetim görüntüleyici) · `41100ca` (F2/6 personel) ·
F2/7 (gösterge + belge).

Bir destek talebi baştan sona panelden çözülüyor; `psql` gerekmiyor.

### 1.1 Gösterge eksikleri (§5.1)

`Gosterge` dataclass'ında olan: hesap sayısı, aktif 7/30, bant dağılımı,
ortalama ekran süresi, lig katılımı, ebeveyn eşleşmesi, bandı olmayan.

- [x] **Y1** `/health` durumu, çalışan sürüm, son migration revizyonu — `surum_kapisi.py` zaten head'i biliyor, göstergeye bağlanacak
- [ ] **Y2** *(F6'ya ertelendi)* Son 24 saatteki 5xx sayısı — `gunluk.py`'nin JSON log'undan. *Not: log kalıcı değilse ayrı bir sayaç gerekiyor; kalemi açmadan önce karar ver*
- [x] **Y3** Açık YEDAM başvurusu sayısı + en eskisinin yaşı — ✅ `F4/4` + en eski başvurunun yaşı — **F3'e bağımlı**, tablo yok
- [x] **Y4** Ekran süresi trendi (§5.1 "ortalama günlük ekran süresi, **trend**") — şu an yalnızca ortalama var

### 1.2 Hesap yönetimi (§5.2) — hiçbiri yazılmadı

- [x] **Y5** `GET /yonetim/kullanici?ara=` — e-posta / kullanıcı no ile arama. **Ad ile arama yazılmayacak** (§5.2: ad benzersiz değil)
- [x] **Y6** `GET /yonetim/kullanici/{id}` — kayıt tarihi, son senkron, lig üyeliği, ebeveyn bağı **var/yok**, ödül kademeleri. **Davranış verisi yok** (P9)
- [x] **Y7** `POST /yonetim/kullanici/{id}/askiya-al` — oturumlar düşer, API 403 döner, veri silinmez. *Cihaz tarafı karşılığı M8*
- [x] **Y8** `DELETE /yonetim/kullanici/{id}` — `DELETE /auth/hesap` ile aynı yolu kullanır
- [x] **Y9** `POST /yonetim/kullanici/{id}/veri-disa-aktar` — KVKK erişim hakkı. Çift onay + gerekçe + denetim kaydı; dosya **kullanıcının kendisine** gider, panele düşmez
- [x] **Y10** Ebeveyn bağını kaldırma — `DELETE /ebeveyn/eslesme`'nin sunucu tarafı karşılığı
- [x] **Y11** Her biri için `denetim_kaydi` yazımı — `panel/denetim.py` var, uçlara bağlanacak

### 1.3 Denetim kaydı görüntüleyici (§5.8)

- [x] **Y12** `GET /yonetim/denetim` — filtre: personel, kullanıcı, eylem türü, tarih aralığı. Yalnızca okuma; kayıt **silinemez**

### 1.4 Personel yönetimi (§11)

- [x] **Y13** `GET /yonetim/personel` + `POST /yonetim/personel` — davet, askıya alma, rol atama. Yalnızca `admin`. Şu an tek yol `python -m app.personel_ekle`
- [x] **Y14** Eşzamanlı oturum listesi + tek tek düşürme (§2 "kaybolan dizüstü senaryosu") — `personel_oturum` tablosu hazır, arayüzü yok

### 1.5 Rol kapısı (§2 "Roller")

- [x] **Y15** Beş rolün (`admin` · `operasyon` · `yedam_yonetici` · `yedam_danisman` · `gozlemci`) yetki matrisi tek yerden uygulanmalı. İlke: **görünmeyen bölüm hiç çizilmez**, uç 403 döner. `PersonelRolu` enum'u var, kapı yok
- [x] **Y16** Rol başına en az bir "yetkisiz erişim 403 alır" testi

---

## 2. F3 · YEDAM UÇTAN UCA — ✅ KAPANDI (21 Ağu 2026)

**Bitiş ölçütü (plan §13):** gerçek bir telefonda rıza verilip başvuru
açılıyor, danışman görüyor, kullanıcı "bakıldı" kaydını görüyor, geri
çekince erişim kesiliyor.

**Kapanış commit'leri:** `76b8d97` (F3/1 bayrak·kapı·yaş) · `de5656c`
(F3/2 tablolar) · `e108e3a` (F3/3 cihaz uçları) · `3a90cb2` (F3/4 danışman
paneli) · `4bdb2fe` (F3/5 rıza bakımı) · `d0383ba` (F3/6 Android M1–M6).

Gerçek bir cihazda uçtan uca doğrulama **yapılmadı** — emülatör/telefon
turu kalan tek kalem (aşağıda).

### 2.1 Ön koşullar — plan §1.1'in "kod tabanında çıkan bulgu"ları

- [x] **D1** `Tercihler.beyanYas` — **yaş bugün hiçbir yere yazılmıyor**, `OnboardingDurum.yas` yalnızca ViewModel state'i. §6.2'nin "yaş bandı" satırı ve 18 altı kapısı bu olmadan render edilemez
- [x] **D2** `app/yedam_kapisi.py` — S2 kararının tek fonksiyonu (`eslesmisse` / `her_zaman` / `hicbir_zaman`), `ozellik_bayragi` ile değiştirilebilir. **Dosya yok**
- [x] **D3** `OzellikBayragi` modeli var ama **hiçbir yerde okunmuyor** — okuma yardımcısı + önbellek yazılacak

### 2.2 Tablolar (§7) — on bir tablonun hiçbiri yok

- [x] **D4** `yedam_riza` · `yedam_basvuru` · `yedam_temas` · `danisman_notu` · `erisim_kaydi` · `risk_surukleyici` · `anket_yanit` — tek Alembic revizyonu
- [x] **D5** **Veri minimizasyonu işi:** rıza süresi dolduğunda / geri çekildiğinde `risk_surukleyici` ve `anket_yanit` **satırları silinir**, erişim kapatılmakla yetinilmez (§7). Süre dolumu için zamanlanmış iş gerekiyor — bugün böyle bir iş yok

### 2.3 Cihaz uçları (§11, API servisi)

- [x] **D6** `POST /yedam/basvuru` — rıza + iletişim tercihi + not
- [x] **D7** `DELETE /yedam/riza` — geri çekme, erişim **aynı saniyede** kesilir
- [x] **D8** `GET /yedam/basvurum` — durum takibi
- [x] **D9** `GET /yedam/erisim-kaydim` — şeffaflık
- [x] **D10** `POST /yedam/gecmis` — rıza anındaki tek seferlik yükleme (rıza başına bir kez)
- [x] **D11** `POST /yedam/surukleyici` — **U1: `/ozet`'ten AYRI uç.** İki rıza kapısı bağımsız: Yeşilay paylaşımı kapalı kullanıcı YEDAM desteği isteyebilir
- [x] **D12** `POST /ebeveyn/yedam-onay` — 18 altı, eşleşmiş ebeveynin onayı (mevcut router genişler)

### 2.4 Panel uçları (§11, panel servisi)

- [x] **D13** `GET /yedam/havuz` — **iskelet var, doldurulacak**
- [x] **D14** `GET /yedam/basvuru/{id}` — **`erisim_kaydi` YAZAR**; §6.2'nin yedi bölümü
- [x] **D15** `POST /yedam/basvuru/{id}/ata`
- [x] **D16** `POST /yedam/basvuru/{id}/temas` — `arandi` / `ulasilamadi` / `gorusuldu`
- [x] **D17** `POST /yedam/basvuru/{id}/not` — klinik not, kullanıcıya gösterilmiyor
- [x] **D18** `POST /yedam/basvuru/{id}/kapat`
- [x] **D19** Durum makinesi: `yeni` → `atandi` → `temas_kuruldu` → `kapandi`; ek durumlar `ulasilamadi` (3 denemeden sonra) · `riza_geri_cekildi` (otomatik) · `suresi_doldu` (90 gün)

### 2.5 Danışman ekranının yasakları (§6.2) — teste bağlanmalı

- [x] **D20** Uygulama adı / oturum dökümü **sunucuda hiç yok** — regresyon testi
- [x] **D21** Teşhis etiketi yok (`CLAUDE.md` §4 panelde de geçerli)
- [x] **D22** Danışman **arama yapamaz** — yalnızca kendisine atanmış başvuruyu açar. 403 testi

---

## 3. F4 · PUAN VE ÖDÜL — ✅ KAPANDI (21 Ağu 2026)

**Bitiş ölçütü:** panelden verilen puan telefonda görünüyor; bir kupon
teslimatı uçtan uca işaretleniyor.

**Kapanış commit'leri:** `4ea2297` (F4/1 sunucu) · `fc61840` (F4/2 cihaz,
Room 10) · `e838a7c` (F4/3 ödül stoğu).

K56'nın "Room 10, GECIS_9_10" kararı artık gerçekten kodda. **A4'ün teknik
ayağı çözüldü:** kapı stoğun kendisi. Finansman ayağı Yeşilay'da.

- [x] **P1** Sunucu: `puan_duzeltme` tablosu + `POST /yonetim/kullanici/{id}/puan-duzeltme`. Gerekçe **en az 20 karakter**, denetim kaydı zorunlu
- [x] **P2** Tek kullanımlık işlem anahtarı — **U2'nin istisnası**: CSRF genelde oturuma bağlı HMAC, puan düzeltme formu idempotency anahtarı taşır
- [x] **P3** `GET /puan/duzeltmeler` (cihaz ucu)
- [x] **P4** Cihaz: `puan_duzeltme` Room tablosu, **birincil anahtar sunucu id'si** (aynı delta iki kez uygulanamaz), `GECIS_9_10`, Room 10
- [x] **P5** Cihaz: `Tercihler.enYuksekToplamPuan` (**U6**) — ağaç evresi ve ödül kademesi `maxOf(guncel, enYuksek)` okur; bakiye güncel toplamı gösterir
- [x] **P6** `SenkronIstek.toplamPuan` alanı (**U7**) — §5.3'ün önizleme formu bugünkü şemayla yazılamıyor. Değer **bilgilendirici, otorite değil**; form bunu ekranda yazar (A13 aynen geçerli)
- [x] **P7** Bildirim dili: *"Puanında bir düzeltme yapıldı: −120. Gerekçe: …"* — "ceza" kelimesi geçmez, emir kipi yok
- [x] **P8** `odul_stok` + `odul_teslimat` tabloları; `GET/POST /yonetim/odul/stok`, `/teslimat`
- [x] **P9** **K28 kapısının stoğa bağlanması** (§5.4 kararı): parti yüklenince kademe otomatik açılır, stok bitince kapanır. `MADDI_ODUL_TESLIMATI_ETKIN` sabiti → `ozellik_bayragi`. A4'ün teknik ayağı burada kapanıyor
- [x] **P10** Teslimat durumu `bekliyor` → `gönderildi` → `teslim edildi`

---

## 4. F5 · İÇERİK VE DUYURU — açık

**Bitiş ölçütü:** bir yazı düzeltmesi APK yayını olmadan telefona iniyor.

- [ ] **I1** `GET /icerik/bilgi-bankasi` — S5 kararı (b): sunucudan servis + cihazda önbellek. Gömülü `bilgi_bankasi.json` **çevrimdışı yedek olarak kalır**
- [ ] **I2** Sürümleme + önbellek tazeleme + "hangi sürümü gördü" (§5.5'in kabul edilmiş bedeli)
- [ ] **I3** `GET/POST /yonetim/icerik` — panel tarafı düzenleme
- [ ] **I4** "Günün Sorusu" ve bildirim metinleri aynı yoldan
- [ ] **I5** `duyuru` tablosu + `GET/POST /yonetim/duyuru` + cihazda `GET /duyuru`. **FCM YOK** (§5.6 kararı): duyuru senkronla `BildirimDeposu`'na iner, kullanıcı uygulamayı açınca görür
- [ ] **I6** Duyuru metni denetimi — §4 "emir kipi / korkutma yok"
- [ ] **I7** `GET/POST /yonetim/bayrak` — özellik bayrağı arayüzü. **Risk motoru sabitleri yalnızca gösterilir, değiştirilemez** (K17)
- [ ] **I8** Asgari istemci sürümü + bakım modu (§5.7) → cihaz tarafı M9

---

## 5. F6 · SERTLEŞTİRME — açık

- [ ] **S1** Redis hız sınırı — **A12'nin gerçek konusu: cihaz API'si.** Panel girişi F1'de hesap kilidiyle çözüldü (U5)
- [ ] **S2** IP allowlist / VPN, panel servisi önünde (G3)
- [ ] **S3** Sızma testi / güvenlik gözden geçirmesi, yayına almadan önce (G10)
- [ ] **S4** **Yedek + geri yükleme tatbikatı (G9) — F6'da değil, F3 İLE BİRLİKTE.** Danışman notu geri getirilemezse gerçek veri kaybı (§15)
- [ ] **S5** İzleme ve alarm
- [ ] **S6** Personel parola politikası + kurtarma kodu akışının belgelenmesi (G2 — kod var, politika yazılı değil)

---

## 6. ANDROID — M1…M9 (§10), hiçbiri başlamadı

Bugün `ui/ekran/yedam/` altında **tek dosya** var (`YedamOnayEkran.kt`) ve
`YesilizApi.kt`'de tek bir `yedam` / `duzeltme` / `icerik` ucu yok.
M8 F2 turunda yazıldı; kalan dokuz kalem F3–F5'e bağlı.

- [x] **M1** YEDAM rıza ekranı gerçek hâle gelir — madde madde metin, sürüm numarası, kabul kutusu. Şu an yalnızca 115'i çeviricide açıyor
- [x] **M2** Sürükleyici gönderimi — **U1: `POST /yedam/surukleyici`**, `/ozet` gövdesine değil
- [x] **M3** Rıza anında geçmiş yükleme — 30 günlük `risk_sonuc` tek seferlik. Yoksa danışman ilk gün hiçbir şey görmüyor
- [x] **M4** Başvuru durumu ekranı — durum sunucudan, **uydurma durum yok** (K37)
- [x] **M5** "Verilerine kim baktı" ekranı — Profil › Gizlilik
- [x] **M6** Rızayı geri çekme — tek dokunuş + onay
- [x] **M7** Puan düzeltmesini çekme ve gösterme — F4/P4 ile aynı iş
- [x] **M8** Askıya alınmış hesap ekranı — ✅ `ef1ac18`. 403 + `hesap_askida` bir OkHttp interceptor'ında tek yerde yakalanıyor; ekran gezinmenin üstünde
- [ ] **M9** Bakım modu ekranı — 503 + bakım gövdesi
- [x] **M10** `Tercihler.beyanYas` (= D1) — onboarding'de sorulan yaş kalıcı hâle gelir

---

## 7. DOKÜMAN VE HUKUK — kod dışı, ama yayın ön koşulu

- [ ] **G5** `Dökümanlar/Yesiliz - Gizlilik Politikası.txt` YEDAM paylaşımıyla güncellenmeli; **personel için ayrı aydınlatma metni** yazılmalı (bugün yok)
- [ ] **G6** Veri işleyen sözleşmesi (Yeşilay ↔ ekip) — hukuki, gerçek kullanıma geçme ön koşulu
- [ ] **G7** Saklama süreleri yazılı olmalı: başvuru, danışman notu, denetim kaydı, erişim kaydı
- [ ] **G8** Silme talebinin **yedeklerde de** karşılanması — prosedür `docs/dagitim.md`'ye. *Bugün o dosyada yedek/geri yükleme bölümü hiç yok*
- [ ] **B1** `DESIGN.md`'ye panel notu — §3'ün kararı: **kırmızı yasağı paneli kapsamıyor** (damgalanan kullanıcı değil, iş kaydının durumu). Bugün `DESIGN.md`'de panel geçmiyor
- [ ] **B2** `CLAUDE.md` §11 test sayıları güncellenmeli — panel testleri (`test_panel_*.py`, `test_totp.py`, `test_servis_rolu.py`, `test_personel_modeli.py`, `test_sir_sifreleme.py`, `test_surum_kapisi.py`) sayıma girmemiş

---

## 8. AÇIK KARARLAR — kod bekliyor değil, cevap bekliyor

Plan §14'ün sekiz maddesinden üçü (S2, S3, S5) kapandı. Kalanlar:

| # | Soru | Kim | Neyi tıkıyor |
|---|---|---|---|
| ~~S1~~ | **Kapandı (21 Ağu):** Yeşilay kendi açacak → panelde personel yönetimi yazıldı (`41100ca`) | — | — |
| S4 | Danışman notu **bayrakla kapalı geldi** (`yedam.danisman_notu`, varsayılan `hayir`); uç 503 dönüyor ve satır yazmıyor. Saklama süresi kararı hâlâ hukuktan bekleniyor — geldiğinde tek bayrak değişiyor | Yeşilay + hukuk | G7 |
| S6 | Panel alan adı ve barındırma | Yeşilay | **Kod hazır:** CI `vars.RAILWAY_PANEL_SERVICE` ve `vars.PANEL_TABAN` bekliyor |
| ~~S7~~ | **Kapandı (21 Ağu): hayır** — iletişim telefon/e-posta ile. F3'ün kapsamı daraldı | — | — |
| S8 | `gozlemci` rolü gerçekten gerekli mi, dışa aktarma yeter mi? | ekip | Y15'in matrisi |

---

## 9. SIRA ÖNERİSİ

1. ~~**Y5–Y12**~~ ✅ **bitti** (21 Ağu) — Y13/Y14 de S1 kararıyla birlikte kapandı
2. ~~**D1/D2/D3**~~ ✅ **bitti** (21 Ağu)
3. ~~**D4–D19 + M1–M6**~~ ✅ **bitti** (21 Ağu). **G9 tatbikatı hâlâ yapılmadı** —
   §15 onu F3 ile birlikte istiyordu; danışman notu bayrakla kapalı olduğu için
   şu an geri getirilemez veri yok, ama bayrak açılmadan önce tatbikat şart
4. **F4** — K56 kararı zaten yazılı, kodu yok; ödül kapısı A4'ün teknik ayağını kapatıyor
5. **F5**, sonra **F6**

> Y13/Y14 (personel yönetimi) 1. adıma da 2. adıma da alınabilir; bugün tek
> yol `python -m app.personel_ekle` ve bu, S1 cevaplanana kadar yeterli.


---

## 10. F3 SONRASI — GERÇEK CİHAZ DOĞRULAMASI

Plan §13'ün F3 bitiş ölçütü: *"gerçek bir telefonda rıza verilip başvuru
açılıyor, danışman görüyor, kullanıcı 'bakıldı' kaydını görüyor, geri
çekince erişim kesiliyor."*

Kod tarafı tamam ve testlerle örtülü, ama **bu tur hiç koşulmadı**:

- [ ] **V1** Telefonda rıza ver → başvuru aç → panelde havuzda gör
- [ ] **V2** Panelden ata → detayı aç → telefonda "bakıldı" kaydı görünsün
- [ ] **V3** Temas kaydı ekle → telefonda görüşme kaydı görünsün
- [ ] **V4** Telefondan rızayı geri çek → panelde veri bölümleri boşalsın
- [ ] **V5** 18 altı senaryosu: eşleşmiş ebeveynle başvuru → onay bekliyor →
      ebeveyn onaylayınca havuza düşsün
- [ ] **V6** Migration'ları **gerçek PostgreSQL'de** koştur — zincir SQLite'ta
      uçtan uca koşturulamıyor (`c84f32a1e907` PostgreSQL'e özgü SQL kullanıyor,
      mevcut bir migration)
- [ ] **G9** Yedek + geri yükleme tatbikatı — plan §15 bunu F3 ile birlikte
      istiyor; danışman notu açılmadan önce şart
- [ ] **V7** Puan düzeltmesi telefonda: panelden −120 → bildirim defterinde
      "düzeltme" görünsün, bakiye düşsün, **ağaç evresi ve ödül kademesi geri
      gitmesin** (U6'nın asıl sınandığı yer)
- [ ] **V8** Ödül stoğu: panelden parti yükle → telefonda kademe açılsın →
      seç → panelde teslimat "bekliyor" görünsün
