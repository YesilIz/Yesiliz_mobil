# DESIGN.md — Yeşilİz

> **Stitch** (stitch.withgoogle.com) için hazır tasarım dosyası.
> §3'teki BASE PROMPT'u kopyala, §5'ten ekran bloğunu ekle, Stitch'e yapıştır.
> Prompt'lar İngilizce (Stitch İngilizce daha iyi çalışıyor), arayüz metinleri Türkçe.

---

## 1. TASARIM TOKENLARI

Kaynak: Türkiye Yeşilay Cemiyeti Marka Kılavuzu 2025 (doğrulanmış).

### Renkler

```css
/* ANA */
--primary:        #00AE42;   /* Pantone 354 C  — ana marka yeşili */
--primary-dark:   #124734;   /* Pantone 3435 C — koyu yeşil */

/* DESTEK */
--green-50:       #edf7ef;   /* ekran zemini */
--green-200:      #c4dec9;   /* kart zemini, chip */
--green-500:      #70ab70;   /* orta ton, ikincil */

/* NÖTR */
--white:          #FFFFFF;
--black:          #000000;
--text-primary:   #124734;
--text-secondary: #5A6B60;
--divider:        #E3EDE6;
```

**Risk bandı renkleri — KIRMIZI YOK:**

```css
--risk-low:    #00AE42;   /* AZ RİSKLİ   */
--risk-mid:    #70ab70;   /* ORTA RİSKLİ */
--risk-high:   #124734;   /* ÇOK RİSKLİ  */
```

> Marka kılavuzu "karamsar değil pozitif renkler" diyor. Riski kırmızıyla
> göstermek damgalayıcı olur. Koyulaşan yeşil kullanılır.

**Hata / doğrulama durumu — burada da kırmızı yok:**

```css
--error:           #124734;   /* koyu yeşil — metin, kenarlık */
--error-container: #c4dec9;   /* destek yeşili — zemin */
```

Material 3'ün varsayılan `error` paleti (`#ba1a1a`, `#ffdad6`) **kullanılmaz**.
Bu palet şu an Stitch tasarım sisteminde hâlâ tanımlı (hiçbir ekran kullanmıyor) —
temizlenmesi bekleyen elle iş, bkz. `docs/stitch-ekranlar.md` §3.
"Bu alan boş bırakılamaz" gibi form hataları koyu yeşille ve destekleyici dille
gösterilir. Bu, §2'deki "kırmızı renk yok" kuralının form/hata durumlarına da
uygulanmış hâli — kural sadece risk bandı için değil.

**İstisnalar (30 Tem 2026 ek teslimi):** Dışarıdan gelen tasarım iki yerde
kırmızı kullanıyor ve kullanıcı bunun aynen uygulanmasına karar verdi —
kural bu iki somut noktayla sınırlı olacak şekilde gevşetildi (CLAUDE.md §12 K10):

```css
--kirmizi-istisna:        #C0392B;  /* risk analizi "Yüksek" etiketi/çubuğu;
                                        hesap/veri kalıcı silme onayı */
--kirmizi-istisna-zemin:  #FDECEB;  /* aynı iki bağlamda ikon/uyarı kutusu zemini */
```

Başka hiçbir yerde kullanılmaz — genel risk gösterimi ve hata durumları hâlâ
yukarıdaki `--risk-*` / `--error*` yeşil skalasında. Ekran spekleri:
`docs/tasarim-referans.md §2` ekran 25, 26, 30.

### Teslim edilen tasarımdan eklenen tokenlar

`docs/tasarim-referans.md §1`'de doğrulanmış, henüz buraya taşınmamış
tokenlar (29 Tem teslimi getirdi, palet çakışması yok — yalnızca ekleme):

```css
--surface-cracked:   #FAFDFB;  /* kart üstü ikinci kademe yüzey (YuzeyKirik) */
--green-mint:        #E1F6EA;  /* ikon rozeti zemini, bilgi şeridi (YesilNane) */
--text-tertiary:     #7B8A82;  /* grafik ekseni, meta metin (MetinUcuncul) */
--points-card-start: #0B2B1D;  /* puan kartı gradyan başlangıcı (PuanKartiBas) */
--points-card-end:   #3FB578;  /* puan kartı gradyan bitişi (PuanKartiSon) */
--mood-start:        #EE9E5C;  /* ruh hali slider'ı sol uç (RuhHaliBas) */
--mood-mid:          #F6D667;  /* ruh hali slider'ı orta (RuhHaliOrta) */
```

Kategori paleti (sosyal medya/eğlence/iletişim/oyun/üretkenlik/diğer) için
`docs/tasarim-referans.md §1` "Kategori paleti" tablosuna bakılır — risk/durum
göstermediği için "kırmızı yok" kuralını ilgilendirmez.

### Canlı Stitch tasarım sistemi

Bu dosyadaki tokenlardan üretilmiş, Stitch'te **canlı** olan sistem:

| Alan | Değer |
|---|---|
| Proje | `projects/17621803030342705738` — "Markdown Design System" |
| Tasarım sistemi | `assets/0b7f4c630ae54da29f9c5a74103ff73f` — "Hopeful Green", sürüm 1 |
| Ekran eşlemesi | `docs/stitch-ekranlar.md` |

Yeni ekran üretirken `generate_screen_from_text` çağrısına **mutlaka** bu
`designSystem` asset id'si verilmeli — verilmezse Stitch varsayılan sistemi kullanır
ve tutarlılık bozulur.

> **KAYNAK ÖNCELİĞİ.** Stitch'in tasarım sistemi Material 3 türevi olduğu için
> ek/farklı tonlar üretiyor (`surface` `#f2fcf4`, `primary` `#006e27`; bizim
> `#00AE42` orada `primary-container`). **Compose `ui/tema/` dosyaları bu bölümdeki
> DESIGN.md tokenlarından yazılır** — Yeşilay marka kılavuzundan doğrulanmış olanlar
> bunlar. Stitch'in türev tonları yalnızca Stitch içi tutarlılık içindir, Kotlin'e
> kopyalanmaz.

### Tipografi

**Inter** — karar kilitlendi (CLAUDE.md §12 K4). Kurumsal font Averta ticari lisans
gerektiriyor; lisans temin edilirse değişiklik `ui/tema/Tipografi.kt` tek dosyasından
yapılır, başka hiçbir yer etkilenmez.

```
Display   32sp  Bold        ana ekran skoru, büyük sayılar
H1        24sp  Semibold    ekran başlığı
H2        20sp  Semibold    bölüm başlığı
Body      16sp  Regular     gövde metni
Caption   14sp  Regular     yardımcı metin
Label     12sp  Medium      etiket, chip
```

### Boşluk ve şekil

```
Spacing skalası:  4 / 8 / 16 / 24 / 32 / 48
Köşe yarıçapı:    kart 20dp · buton 16dp · chip 12dp · bottom sheet 28dp
Gölge:            çok yumuşak, y+2 blur 12, %6 opaklık
Ekran kenar boşluğu: 20dp
```

### Bileşenler

| Bileşen | Spek |
|---|---|
| Primary buton | Dolgu `#00AE42`, beyaz metin, 56dp yükseklik, radius 16 |
| Secondary buton | Kenarlık `#00AE42` 1.5dp, yeşil metin, şeffaf zemin |
| Kart | Zemin `#FFFFFF`, radius 20, yumuşak gölge, iç boşluk 20 |
| Görev kartı | Sol ikon + başlık + puan rozeti sağda, tamamlanınca yeşil tik |
| İlerleme çubuğu | Yükseklik 8dp, radius tam, dolu `#00AE42`, boş `#c4dec9` |
| Chip | Zemin `#edf7ef`, metin `#124734`, radius 12 |
| Alt navigasyon | 4 sekme: Ana Sayfa · İstatistik · Lig · Profil |

---

## 2. GENEL İLKELER

**Marka arketipi: Bakıcı (Caregiver) + Masum (Innocent)**

**Genç kitle tonu:** Samimi, enerjik, cesaretlendirici. "Kendin için seç."

**Kritik ilke:** *"Yapma!" değil, "Böyle yapabilirsin."*

**Yeşilay marka sesi — her ekranda geçerli:**

| Yap | Yapma |
|---|---|
| Umut ver | Korkutma |
| Çözüm göster | Soruna takılma |
| Yargılamadan konuş | Emir kipiyle konuşma |
| Sade ve gerçek kal | Abartılı olma |

**Görsel yasaklar:**
- Kırmızı renk yok
- Uyarı üçgeni, ünlem ikonu yok
- Ağır gölge, gradient, 3D efekt yok
- Kalabalık ekran yok — geniş beyaz alan esas

**Marka kılavuzu kuralları (renk/logo):**
- Beyaz alan geniş kullanılmalı — sadelik ve netlik esas
- Yazı ağırlıklı ekranlarda beyaz zemin; görsel yüzeyli ekranlarda beyaz yazı
- Farklı renklerin üst üste bindirilmesinden kaçın
- Logo **yalnızca yeşil ve beyaz**, asla siyah
- Uygulama ikonunda **sadece hilal** amblemi

**Metin dönüşümleri:**

```
❌ "6 saat telefondaydın!"        ✅ "Bugün 6 saat ekrandaydın. Yarın 20 dakika azaltmayı dener misin?"
❌ "Bağımlılık riskin yüksek"     ✅ "Son 7 günde gece kullanımın arttı."
❌ "Hedefi kaçırdın"              ✅ "Bugün olmadı, yarın yeni bir gün."
```

---

## 3. BASE PROMPT

Her ekran prompt'unun başına bunu koy:

```
Mobile app UI screen, Android, portrait.
App: "Yeşilİz" — a Turkish digital wellbeing app that helps young people reduce
screen time and move toward healthier habits. Partner brand: Yeşilay (Turkish
health NGO). Supportive, hopeful, never judgmental.

Design system:
- Primary green #00AE42, dark green #124734, light background #edf7ef,
  card background #FFFFFF, accent #c4dec9, muted green #70ab70
- Typography: Inter (or Averta). Display 32 Bold, H1 24 Semibold,
  Body 16 Regular, Caption 14 Regular
- Corner radius: cards 20dp, buttons 16dp, chips 12dp
- Very soft shadows, generous white space, 20dp screen padding
- Rounded, friendly, calm. Flat illustration style.

Strict rules:
- NO red color anywhere. NO warning icons or alarm styling.
- Never alarming or scolding. Encouraging tone only.
- All UI text in Turkish.

Screen:
```

---

## 4. AĞAÇ METAFORU — SPEK

> **TARİHSEL — kod bu bölümü izlemiyor.** Aşağıdaki tablo ağacı **risk bandına**
> ve beş duruma bağlıyor; uygulama ağacı **TOPLAM PUANA** ve **dokuz evreye**
> bağlıyor (`AnaViewModel.agacEvresi`, `EVRE2_PUAN = 100` … `EVRE9_PUAN = 10.000`).
> Risk bandına bağlamak bilinçli olarak reddedildi: kötü bir günde ağacı
> kurutmak cezalandırma olurdu (CLAUDE.md §4 "ceza puanı yok", §12 K2).
> **Çelişkide kod kazanıyor.** Bölümün geri kalanı — ağacın hiçbir zaman ölü
> görünmemesi, yumuşak geçişler, palet — hâlâ geçerli.

Ana ekranın merkezindeki görsel. Kullanıcının durumu tek bakışta görünür.

| Durum | Görsel tanım |
|---|---|
| **ÇOK riskli** | İnce dallar, az yaprak, soluk yeşil tonlar. **Asla ölü/kuru değil** — birkaç canlı yeşil yaprak her zaman var. Toprak nemli, kök sağlam. |
| **ORTA** | Dalların yarısı yeşermiş, orta yoğunlukta yaprak, taze tonlar belirmeye başlamış |
| **AZ riskli** | Dolgun yeşil taç, birkaç küçük beyaz çiçek, canlı ve dengeli |
| **İyileşiyor** | Dal uçlarında yeni tomurcuk, hafif parlama animasyonu |
| **Kötüleşiyor** | Birkaç yaprak yavaşça soluyor — **yumuşak geçiş, dramatik değil** |

**Kurallar:**
- Ağaç hiçbir zaman tamamen çıplak veya kuru görünmez
- Her durumda büyüme yönü hissedilmeli
- Arka plan: yumuşak gradient `#edf7ef` → beyaz
- Stil: düz (flat) illüstrasyon, ince çizgi, organik hatlar

**Lig seviyeleri metaforu izliyor:**
`Fidan → Filiz → Ağaç → Koru → Orman`

- Fidan bağışı ödülü metaforu tamamlıyor: sanal ağacın gerçek karşılığı

**Alternatifler** (karar bekliyor): gündoğumu/gökyüzü · nehir/su · takımyıldız

**Stitch prompt (ağaç için ayrı üretim):**

```
Flat vector illustration of a stylized tree, minimal and calm, organic soft
shapes, thin outlines. Palette: greens #00AE42, #70ab70, #c4dec9 on soft
#edf7ef background. Five variants of the same tree showing growth stages:
(1) sparse leaves but still alive with a few bright green leaves,
(2) half-leafed, (3) full green canopy, (4) canopy with small white blossoms,
(5) new buds appearing on branch tips.
Hopeful, never dead or wilted. No red, no brown decay. Children's-book warmth
but modern and clean.
```

---

## 5. EKRAN PROMPT'LARI

Her biri BASE PROMPT'tan sonra gelir.

### 5.1 Karşılama

```
Onboarding welcome screen. Centered flat illustration of a small green sapling
growing from soil. Below it, headline in Turkish: "Ekranla ilişkini
yeniden kur." Subtext: "Yeşilİz, telefon kullanımını anlamana ve daha
sağlıklı bir dengeye ulaşmana yardım eder." At the bottom, a full-width
primary green button "Başla" and a text link "Nasıl çalışır?".
Lots of white space. Calm, inviting.
```

### 5.2 İzin açıklaması

```
Permission explanation screen, one permission at a time (step 2 of 4).
Top: thin progress indicator showing 4 steps.
Centered simple line icon (bar chart) inside a soft #edf7ef circle.
Headline: "Ekran kullanımını görebilmemiz için". Body text explaining why:
"Hangi uygulamada ne kadar zaman geçirdiğini görebilmemiz gerekiyor.
İçeriklerine asla bakmayız — sadece süre ve kategori." 
Below, a small reassurance card with a lock icon: "Verilerin telefonunda kalır."
Bottom: primary button "İzin ver", secondary text button "Şimdi değil".
```

### 5.3 Yaş ve cinsiyet

```
Simple profile setup screen. Headline: "Seni biraz tanıyalım".
Two form fields: a number stepper for "Yaşın" and a segmented control with
three options for "Cinsiyetin": Kadın / Erkek / Belirtmek istemiyorum.
Small caption below: "Bu bilgiler önerileri sana uygun hale getirmek için."
Bottom: primary button "Devam".
Very minimal, generous spacing.
```

### 5.4 Anket (SAS-SV)

```
Questionnaire screen showing 2 questions per page (page 2 of 5).
Top: thin horizontal progress bar 40% filled in green, with label "2 / 5".
Each question is a white card with the statement text in Turkish, and below it
a 6-point Likert selector rendered as 6 rounded pill buttons in a row,
labeled 1 to 6, with small text at the ends: "Kesinlikle katılmıyorum" (left)
and "Kesinlikle katılıyorum" (right). Selected pill fills with #00AE42.
Example question text: "Akıllı telefon olmadan yapamam."
Bottom: primary button "Devam", disabled until both answered.
Calm, uncluttered, no imagery.
```

### 5.5 İlk sonuç

```
Assessment result screen. NO diagnosis language, NO red, NO alarm.
Center: a medium flat illustration of a half-leafed tree.
Above it, a soft chip with muted green background reading "Başlangıç
değerlendirmen hazır".
Headline: "Telefon kullanımın şu an dikkat edilmesi gereken bir düzeyde."
Body: "Bu bir teşhis değil. Önümüzdeki günlerde gerçek kullanımını ölçüp
sana özel bir plan çıkaracağız."
Below, a white card listing 3 focus areas with small icons:
"Gece kullanımı", "Kesintisiz süre", "Fiziksel hareket".
Bottom: primary button "Planımı gör".
```

### 5.6 Hedef tanıtımı

```
Goal introduction screen. Center: large circular progress ring, empty,
with "285 dk" in the middle in Display size, and caption "günlük hedefin".
Headline above: "Küçük adımlarla başlıyoruz."
Body: "Şu anki ortalaman 300 dakika. İlk hafta hedefin sadece 15 dakika daha
az. Her hafta biraz daha ineceğiz."
Below: a simple horizontal timeline showing weeks 1, 4, 8, 12 with decreasing
numbers 285 / 240 / 180 / 120.
Bottom: primary button "Hadi başlayalım".
```

### 5.7 ANA EKRAN ⭐

```
Home screen — the most important screen.
Top bar: greeting "Merhaba, Turan" on the left, small streak chip on the right
showing a flame icon and "6 gün".

Hero section: large centered flat tree illustration (half-leafed, hopeful),
on a soft gradient from #edf7ef to white. Below the tree, a small pill chip:
"ORTA RİSKLİ" in muted green — subtle, not alarming.

Below hero, a white summary card with three columns:
- "Ekran" 4s 20dk
- "Hedef" 285 dk
- "Adım" 3.240
Each with a tiny icon above.

Then section header "Bugünün görevleri" and three task cards stacked:
1. Walk icon — "15 dakika yürüyüş" — badge "45 puan"
2. Breathing icon — "Nefes egzersizi" — badge "5 puan" — with green check (done)
3. Moon icon — "Gece 00:00'dan sonra telefonu bırak" — badge "10 puan"

Bottom navigation bar with 4 tabs: Ana Sayfa, İstatistik, Lig, Profil.
Airy layout, scrollable.
```

### 5.8 Görev detayı

```
Task detail bottom sheet, rounded top corners 28dp, over dimmed home screen.
Top: small drag handle. Large walk icon in a soft green circle.
Title: "15 dakika yürüyüş". Points badge "45 puan".
Body: "Kısa bir yürüyüş ekrandan uzaklaşmanın en kolay yolu. Telefonunu
cebinde tut, biz saymaya devam edeceğiz."
A row of small info chips: "15 dk", "Doğrulanmış", "Bugün 1 kez".
Bottom: full-width primary button "Yürüyüşü başlat".
```

### 5.9 Spor başlat (aktif seans)

```
Active workout session screen, full screen, calm focus mode.
Background: soft green gradient #edf7ef to #c4dec9.
Center: very large timer "08:24" in Display size, dark green.
Below it: caption "Yürüyüş sürüyor".
Around the timer: a thin circular progress ring, 55% filled.
Below: two live stats side by side — "1.240 adım" and "24 puan".
Bottom: a secondary outlined button "Duraklat" and a text button "Bitir".
No distractions, no navigation bar.
```

### 5.10 Nefes egzersizi

```
Breathing exercise screen, meditative and minimal.
Full-screen soft gradient background from #edf7ef to white.
Center: a large soft circle in #c4dec9 with a smaller #00AE42 circle inside,
suggesting an expanding/contracting breathing animation.
Text inside the circle: "Nefes al".
Below the circle: small caption "4 saniye içeri · 4 saniye tut · 6 saniye dışarı".
Above: a subtle counter "3 / 6 tur".
Bottom: text button "Bitir".
No other UI elements. Very quiet.
```

### 5.11 İstatistik

```
Statistics screen. Top: segmented control with three options
"Gün / Hafta / Ay", "Hafta" selected.

First card: a bar chart of daily screen time across 7 days (Pzt–Paz),
bars in #00AE42, one bar highlighted darker. Below chart, caption
"Günlük ortalama: 4s 12dk · geçen haftaya göre 18 dk daha az" with a small
down arrow in green.

Second card: category breakdown as a horizontal stacked bar plus a legend
with colored dots and Turkish labels: Sosyal medya, Eğlence, Oyun, İletişim,
Üretim, Eğitim. Use shades of green only.

Third card: "Ekran ve hareket" — a simple dual line chart showing screen time
and step count over the week.

Clean data visualization, no red, no alarming colors.
```

### 5.12 Risk detayı

```
Risk explanation screen — transparency about how the score works.
Top: a large circular gauge showing "52" out of 100, arc filled in muted
green #70ab70, with the label "ORTA RİSKLİ" beneath.
Caption under it: "Bu bir teşhis değil, bir yön göstergesi."

Section header: "Bu seviyeyi en çok etkileyenler"
Three rows, each with an icon, a label, and a thin horizontal bar showing
relative contribution:
- "Sosyal medya süresi" — long bar
- "Gece kullanımı" — medium bar
- "Kesintisiz kullanım" — short bar

Below: a white card titled "Trend" showing a small sparkline going slightly
down and text "Son 7 günde iyileşiyorsun."

Bottom: secondary button "Nasıl hesaplanıyor?".
```

### 5.13 Leaderboard

```
Weekly league screen.
Top: league badge — a stylized tree emblem in a circle with the name
"Filiz Ligi" and caption "5 gün kaldı".
Below: three chips showing promotion zone info: "İlk 7 yükselir" /
"Son 7 düşer".

Ranked list of ~10 users. Each row: rank number, small circular avatar,
display name, and weekly points on the right.
Top 7 rows have a subtle #edf7ef background tint (promotion zone).
The current user's row is highlighted with a #00AE42 left border and
slightly stronger background.
Bottom 7 rows are plain white.

Bottom navigation visible. Friendly and competitive but not aggressive.
```

### 5.14 Ödüller

```
Rewards screen. Top: a card showing total points "3.240 puan" in Display size
with a small tree icon, and a caption "14 aktif gün".

Below, a vertical stepper of 4 reward tiers connected by a line.
Completed tiers have a filled green circle with a check; upcoming ones
have an outlined circle.
1. 500 — "Dijital rozet" (completed)
2. 2.000 — "Adına fidan bağışı" (completed)
3. 5.000 — "Yeşilay Market kuponu" (in progress, with a thin progress bar
   showing 3.240 / 5.000 and text "1.760 puan kaldı")
4. 10.000 — "Spor / kitap kuponu" (locked, muted)

Each tier row is a white card with an icon on the left.
Warm, motivating, not transactional.
```

### 5.15 Profil ve ayarlar

```
Profile and settings screen.
Top: avatar circle, name "Turan", and a chip "Filiz Ligi".
Below, grouped settings list in white cards with dividers:

Group "Hedeflerim": Günlük hedef, Uygulama kategorileri, Bildirim saatleri
Group "Verilerim": Toplanan veriler, Geçmişi indir, Verilerimi sil
Group "Gizlilik": Yeşilay ile paylaşım (toggle, off), Leaderboard'da görün
(toggle, on)
Group "Destek": YEDAM hakkında, Yardım, Hakkında

Each row: leading line icon, label, trailing chevron or toggle.
At the bottom, a plain text button "Çıkış yap" in muted color.
Calm, no bright accents except toggles.
```

### 5.16 Nudge overlay ⭐

```
A small notification card overlay appearing over a blurred app background
(as if the user is inside a social media app). NOT full screen — a floating
card at the top of the screen, about 25% of screen height.

Card content: small tree leaf icon on the left, text on the right:
Title "45 dakikadır buradasın" and body "Kısa bir mola iyi gelebilir."
Two compact buttons at the bottom of the card:
primary "5 dk mola ver" and text button "Devam et".

Card style: white, radius 20, soft shadow, thin #c4dec9 border.
Non-blocking, gentle, easy to dismiss. Definitely not an alert dialog.
No red, no warning icon.
```

### 5.17 Widget

```
Android home screen widget, 4x2 size, shown on a phone home screen.
White rounded card, radius 20, thin green border.
Left side: small tree icon and text "Yeşilİz" plus today's points "68 puan".
Right side: two compact action buttons stacked:
"Spor başlat" (primary green) and "Uyku gir" (outlined).
Bottom row: a thin progress bar showing daily goal progress with caption
"Hedefe 42 dk".
Compact, glanceable, clean.
```

### 5.18 Ebeveyn paneli

```
Parent dashboard screen (for users under 18).
Top: child's name "Elif" with a small avatar and a chip "14 yaşında".
First card: a simplified summary — "Bu hafta ortalama 3s 40dk ekran",
with a small weekly bar chart in green.
Second card: "Durum" showing a small half-leafed tree icon and the label
"ORTA" with caption "Geçen haftaya göre iyileşiyor".
Third card: "Bildirimler" listing two rows with timestamps:
"Uygulama 2 gündür açılmadı" and "Haftalık hedefe ulaştı".

Important: NO detailed app-by-app breakdown, NO message content, NO location.
A small footnote card: "Elif'in gizliliğini korumak için yalnızca özet
bilgiler paylaşılır."
Respectful, trust-building tone.
```

### 5.19 YEDAM yönlendirme onayı

```
Consent screen for sharing data with a support service. Serious but warm,
absolutely not alarming.
Center: soft illustration of two hands or two abstract figures facing each
other (referencing the YEDAM logo concept), in green tones.
Headline: "Yanında biri olsun ister misin?"
Body: "Son haftalarda kullanımın yüksek seyrediyor. İstersen YEDAM
uzmanlarıyla iletişime geçebilir, ücretsiz destek alabilirsin."
Below, a white card listing exactly what would be shared, with checkmarks:
"Risk seviyesi", "Haftalık kullanım özeti", "İletişim bilgin".
And a line with a lock icon: "Mesajların, uygulamaların ve içeriklerin
asla paylaşılmaz."
Bottom: primary button "Evet, iletişime geçilsin" and text button
"Şimdi değil". A small caption: "Bu onayı istediğin an geri alabilirsin."
```

---

## 6. STITCH KULLANIM NOTLARI

**Sıra önerisi:** Önce 5.7 (Ana ekran) ve 4 (ağaç) üret — tasarım dilini bunlar belirler. Sonra diğerleri.

**Tutarlılık için:** Stitch her üretimde biraz farklı yorumlayabiliyor. Ana ekranı beğendiğinde onu referans olarak tut ve sonraki prompt'lara şunu ekle:

```
Match the visual style of the previously generated home screen:
same card radius, same green tones, same illustration style.
```

**Türkçe metin sorunu:** Stitch bazen Türkçe karakterleri bozabiliyor (ı, ğ, ş, ç, ö, ü). Üretilen ekranlarda metinleri kontrol et; gerekirse Figma'ya aktarıp elle düzelt.

**Üretilemeyecekler:** Widget (5.17) ve nudge overlay (5.16) sistem bileşenleri — Stitch bunları yaklaşık üretir, gerçek implementasyon Compose/Glance ile yapılır.

---

## 7. KONTROL LİSTESİ

Her ekran üretildikten sonra:

| Madde | Nasıl bakılır |
|---|---|
| Hiç kırmızı yok | otomatik — `tests/kontrol_stitch.py` |
| Uyarı/ünlem ikonu yok | otomatik |
| Teşhis dili yok ("bağımlı", "riskli grup" gibi) | otomatik |
| Emir kipi yok ("azalt!", "bırak!") | otomatik |
| Türkçe karakterler doğru | otomatik (mojibake tespiti) |
| Ağaç ölü görünmüyor | **insan gözü** |
| Beyaz alan yeterli, ekran kalabalık değil | **insan gözü** |
| Renkler token listesinden | **insan gözü** |

```bash
python3 tests/kontrol_stitch.py <indirilen-html-klasoru>
```

**Ekran bazlı sonuçlar `docs/stitch-ekranlar.md` §3'te.**
2026-07-28 taraması: otomatik maddelerde 19/19 temiz, mojibake yok.
Açık madde: tasarım sisteminde Material varsayılan kırmızı `error` tokenları
tanımlı (hiçbir ekran kullanmıyor ama temizlenmeli) — ayrıntı orada.
