# Tasarım Referansı — Yeşilİz Mobil Arayüz

> **Bu dosya UI'ın TEK kaynağıdır.** Ekran yazarken buraya bak.
> `stitch_markdown_design_system/` artık nihai kaynak DEĞİL — yalnızca burada
> karşılığı olmayan ekranlar için yerleşim referansı (§4).

| | |
|---|---|
| Kaynak | `Yeşilİz Mobil Arayüz-selection.png` (dışarıdan teslim, 29 Tem 2026, 19 ekran) + ek teslim (30 Tem 2026, 12 ekran/varyasyon) + ikinci ek teslim (30 Tem 2026, 8 ekran + 3 widget) |
| Kesilmiş ekranlar | `docs/tasarim/ekranlar/*.png` — 41 adet, 2× ölçek |
| Kapsam | 38 ekran + 3 widget tasarımı (biri uygulanmıyor, bkz. **41**) + modal/alt sayfalar |

---

## 1. TOKENLAR

Teslim edilen tasarım `DESIGN.md §1` paletini **birebir** kullanıyor —
piksel örneklemesi doğruladı. Yani `ui/tema/Renk.kt` değiştirilmiyor, yalnızca
tasarımın getirdiği yeni tokenlar **ekleniyor**.

### Doğrulanan (zaten var)

| Token | Değer | Tasarımda nerede |
|---|---|---|
| `YesilAna` | `#00AE42` | birincil buton, aktif adım çubuğu, vurgu |
| `YesilKoyu` | `#124734` | başlık metni, avatar, koyu kart |
| `Yesil50` | `#EDF7EF` | ekran zemini, alt sayfa zemini |
| `Yesil200` | `#C4DEC9` | pasif adım çubuğu, pasif ilerleme |
| `Ayrac` | `#E3EDE6` | liste ayraçları |
| `Beyaz` | `#FFFFFF` | kart yüzeyi |

### Eklenen

| Token | Değer | Kullanım |
|---|---|---|
| `YuzeyKirik` | `#FAFDFB` | kart üstü ikinci kademe yüzey |
| `YesilNane` | `#E1F6EA` | ikon rozeti zemini, bilgi şeridi |
| `MetinUcuncul` | `#7B8A82` | grafik ekseni, "5 gün kaldı", meta metin |
| `PuanKartiBas` | `#0B2B1D` | puan kartı gradyan başlangıcı (sol üst) |
| `PuanKartiSon` | `#3FB578` | puan kartı gradyan bitişi (sağ alt) |
| `RuhHaliBas` | `#EE9E5C` | ruh hali slider'ı sol uç (turuncu) |
| `RuhHaliOrta` | `#F6D667` | ruh hali slider'ı orta (sarı) |

**Kategori paleti** — yalnızca kategori dağılımı, adım grafiği ve lig
avatarlarında. Risk/durum göstermez, bu yüzden "kırmızı yok" kuralını
ihlal etmiyor (turuncu ≠ kırmızı, damgalama taşımıyor).

| Kategori | Renk |
|---|---|
| sosyal medya | `#3B8F42` |
| eğlence | `#00989A` |
| iletişim / adım grafiği | `#008BC7` |
| oyun | `#707ED4` |
| üretkenlik | `#DC8748` |
| diğer | `#AB78C1` |

### Yerleşim

- Ekran kenar boşluğu **20dp** (mevcut `Olcu.EkranKenari` doğru)
- Kart yarıçapı **20dp**, buton **16dp**, alt sayfa üst köşe **28dp** — mevcut
- Birincil buton **56dp** yükseklik, tam genişlik, altında metin bağlantı
- Yeşil başlık (`YesilAna` dolgu) → altındaki içerik **beyaz/`Yesil50`** yuvarlak
  köşeyle başlığın üstüne biner (üst köşe 20dp)

### Kırmızı istisnaları

30 Temmuz ek teslimi iki yerde kırmızı kullanıyor. CLAUDE.md §4 ve
`DESIGN.md §1`'deki "kırmızı yasak" kuralı bu iki somut istisnayla sınırlı
olacak şekilde güncellendi (bkz. CLAUDE.md §12 K10) — kuralın geri kalanı
(risk bandı genel gösterimi, hata durumları) **değişmedi**, hâlâ yeşil.

| Token | Değer | Kullanım |
|---|---|---|
| `KirmiziIstisna` | `#C0392B` | risk analizi ekranında (30) yalnızca "Yüksek" etiketi/çubuğu; hesap/veri kalıcı silme onayında (25, 26) ikon + buton |
| `KirmiziIstisnaZemin` | `#FDECEB` | aynı iki bağlamda ikon rozeti zemini ve uyarı kutusu zemini |

**Yalnızca bu iki bağlamda kullanılır.** Başka hiçbir yerde (genel risk
bandı, hata mesajı, uyarı ikonu) kırmızıya dönülmez.

---

## 2. EKRAN EKRAN

Dosya adları `docs/tasarim/ekranlar/` altındaki kesitlere karşılık gelir.

### 01 · Karşılama — `onboarding/karsilama`
Adım göstergesi (4 çubuk, 1 aktif) + "Adım 1 / 4" · ağaç illüstrasyonu
(`agac_evre1_seyrek.png`) · H1 "Ekranla ilişkini yeniden kur." ·
gövde metni · `Başla` · metin bağlantı "Nasıl çalışır?"

### 02 · İzin — `onboarding/izin`
Adım 2/4 · daire içinde çubuk grafik ikonu (`Yesil50` zemin) ·
"Ekran kullanımını görebilmemiz için" · açıklama ·
kilit ikonlu bilgi şeridi "Verilerin telefonunda kalır." (`YesilNane` zemin,
16dp yarıçap) · `İzin ver` · "Şimdi değil"

### 03 · Profil — `onboarding/profil`
Adım 3/4 · "Seni biraz tanıyalım." · **Yaşın**: `Yesil50` zemin üstünde büyük
sayı, sağda `−` (beyaz) ve `+` (`YesilAna`) kare butonlar ·
**Cinsiyetin**: 3'lü segment (Kadın · Erkek · Belirtmek istemiyorum),
seçili olan `YesilAna` dolgu · yardımcı metin · `Devam`

### 04 · Anket — `onboarding/anket`
Adım 4/4 (hepsi dolu) · "Aşağıdaki ifadelere ne kadar katılıyorsun?" ·
her madde bir kart: ifade metni + **6 daire** (1–6 Likert), seçili daire
`YesilAna` dolgu beyaz rakam, seçilmemişler `Yesil50` zemin ·
altta iki uç etiketi "Kesinlikle katılmıyorum" / "Kesinlikle katılıyorum" ·
`Devam`. SAS-SV 10 madde → `docs/spec-anket.md`.

### 05 · İlk sonuç — `onboarding/ilksonuc`
Üstte çip "Başlangıç değerlendirmen hazır" · yarı yapraklı ağaç ·
**betimleyici** başlık "Telefon kullanımın şu an dikkat edilmesi gereken bir
düzeyde." · "Bu bir teşhis değil…" · 3 satırlık kart (ay/saat/şimşek ikonları:
Gece kullanımı · Kesintisiz süre · Fiziksel hareket) · `Planımı gör`

> Bant adı ekranda yazılmıyor, cümleye gömülüyor — CLAUDE.md §4 teşhis yasağı.

### 06 · Hedef önerisi — `onboarding/hedef`
"Küçük adımlarla başlıyoruz." · açıklama (mevcut ortalama → ilk hafta hedefi) ·
**dairesel gösterge**: kalın halka, ortada `285 dk` + "günlük hedefin" ·
4 haftalık yol çubuğu (285 · 240 · 180 · 120) · `Hadi başlayalım` +
çerçeveli ikincil buton `Kendi hedefini belirle`

### 07 · Hedef özel — `onboarding/hedefozel`
"Kendi hedefini belirle." · dev sayı `270 dk` + "yeni günlük hedefin" ·
slider (60 dk – 300 dk), tutamak beyaz dolgu yeşil kenarlık ·
**Hızlı seçim** 3 çip: −15 / −30 / −45 dk · `Hedefi kaydet` · "Vazgeç"

### 08 · Ana Sayfa — `ana` *(sekme 1)*
Yeşil gradyan başlık: avatar + "Günaydın / Turan" + streak çipi "6 gün" ·
ağaç illüstrasyonu (gradyan beyaza doğru söner) ·
**puan kartı** (koyu→açık yeşil gradyan, `3.240 puan`, "Ödüllerini görmek için
dokun", sağda `›`) → Ödüller ekranı ·
3'lü istatistik şeridi (Ekran `4s 20dk` · Hedef `285 dk` · Adım `3.240`) ·
**ruh hali kartı**: "Bugün nasıl hissediyorsun?" + turuncu→yeşil gradyan slider,
uçlar "Zorlanıyorum" / "Harika" ·
2'li kart: Bilgi Bankası · Kendini Test Et ·
"Bugünün görevleri" listesi

### 08b · Ana Sayfa — ebeveyn modu açık *(varyasyon)* — `ana`
08 ile birebir aynı, yalnızca 3'lü istatistik şeridi 4'e çıkıyor: 4. çip
"Aktif Ebeveyn" (iki kişi ikonu). Ebeveyn eşleştirmesi tamamlanmış
kullanıcıda gösterilir (bkz. 20-22).

### 09 · İstatistik — `istatistik` *(sekme 2)*
Yeşil başlık "İstatistik" · Gün/Hafta/Ay segmenti (seçili beyaz kart) ·
**risk analizi kartı**: koyu yeşil gradyan zemin (`PuanKartiBas`→
`PuanKartiSon`, puan kartıyla aynı gradyan), uyarı üçgeni ikonu +
"Risk analizi: Orta düzey" + "Sonuçları ve önerilen eylemleri gör" + sağda
`›` → **30 · Risk analizi ve öneriler**'e gider ·
**ekran süresi kartı**: ORTALAMA etiketi, `4s 12dk`, tarih aralığı,
7 çubuk (bugün `YesilAna`, diğerleri `Yesil200`), kesikli ızgara + sağ eksen,
altında karşılaştırma satırı ok ikonuyla ·
**kategori dağılımı kartı**: tek yığın bar (kategori paleti) + lejant +
"Detayları göster" ·
**adım kartı**: aynı düzen, çubuklar `#008BC7`, en iyi gün `YesilAna` ·
"Ekran ve hareket" birleşik grafik

### 10 · Lig — `lig` *(sekme 3)*
Yeşil başlık "Lig" · beyaz daire içinde ağaç ikonu · "Filiz Ligi" ·
"5 gün kaldı" · iki çip: "↑ İlk 7 yükselir" / "↓ Son 7 düşer" ·
sıralama satırları: sıra no · renkli baş harf avatarı · isim · puan.
Kullanıcının kendi satırı `YesilAna` kenarlıkla ayrışır.

### 11 · Ödüller — `odul`
Geri oklu yeşil başlık · puan kartı (gradyan, "Mevcut Puanın", "Sonraki ödüle
1.760 puan") · **dikey basamak**: aşağıdan yukarı, her basamak daire ikon +
puan eşiği + ad. Tamamlananlar `✓ Tamamlandı`, aktif olan halkalı + ilerleme
çubuğu + "1.760 puan kaldı", kilitli olan kesikli çember ve soluk metin.

### 12 · Profil — `profil` *(sekme 4)*
Yeşil başlık · büyük `YesilKoyu` avatar + isim + lig çipi ·
bölümler (`HEDEFLERİM` · `VERİLERİM` · `GİZLİLİK`), her biri bir kart içinde
ikon + etiket + `›` satırlar; gizlilik satırları switch:
"Yeşilay ile paylaşım" (kapalı), "Leaderboard'da görün" (açık)

### 13 · Ebeveyn paneli — `ebeveyn`
Geri oklu başlık · çocuk avatarı + "14 yaşında" çipi · haftalık ekran süresi
grafiği · durum kartı "Durum: ORTA / Geçen haftaya göre iyileşiyor" ·
bildirimler listesi · kilit ikonlu gizlilik notu.
> Ebeveyne **ham veri değil özet** gösterilir (`docs/spec-veri.md`).

### 14 · Giriş — `hesap/giris`
Logo + "Yeşilİz" · "Hoş geldin." · e-posta / şifre alanları (`Yesil50` zemin,
göz ikonu) · "Şifremi unuttum" · `Giriş yap` · "veya" ayracı ·
`Google ile devam et` · `Misafir olarak devam et` · alt bağlantı "Kayıt ol"

### 15 · Kayıt — `hesap/kayit`
"Hesap oluştur." · Ad Soyad / E-posta / Şifre / Şifre tekrar ·
onay kutusu (Kullanım Koşulları + Gizlilik Politikası) · `Hesap oluştur` ·
`Google ile devam et` · alt bağlantı "Giriş yap"

### 16 · Nefes — `nefes`
`Yesil50` tam ekran · kapat (×) + "4:12 kaldı" · "KUTU NEFESI /
Sakinleşme seansı" · merkezde nabız gibi büyüyüp küçülen daire
(iki soluk halka + dolu yeşil daire), içinde faz metni "Nefes al" ·
altında yönerge · 4 nokta faz göstergesi · duraklat/oynat/tekrarla butonları

### 17 · Puan geçmişi *(alt sayfa — Ödüller üstünde)*
"Puan geçmişi" + kapat · "Bu hafta toplam +310 puan" özet şeridi ·
ikon + başlık + zaman + `+puan` satırları

### 18 · Ödül detayı — devam eden *(alt sayfa)*
Koyu daire ikon · ödül adı · açıklama · `3.240 / 5.000 puan` + `%65` +
ilerleme çubuğu + "Kazanmana 1.760 puan kaldı" · `Puan kazanmaya devam et`

### 19 · Ödül detayı — kazanılmış *(alt sayfa)*
Gradyan daire ikon · `✓ Tamamlandı` çipi · ödül adı · açıklama ·
Bağış tarihi / Sertifika no satırları · `Sertifikayı görüntüle`

### 20 · Ebeveyn paneli — ilk kurulum, cihaz rolü *(alt sayfa)*
Koyu yeşil başlık "Ebeveyn paneli" + geri ikonu · beyaz alt sayfa, üstte
sürükleme çubuğu + kapat (×) · `YesilKoyu` daire içinde kişi ikonu ·
"Ebeveyn paneline hoş geldin" · açıklama · 2 seçim kartı: "Bu cihaz ebeveyn
cihazı" (seçili, `YesilAna` kenarlık + dolu radio) / "Bu cihaz çocuk cihazı"
(pasif) · `Devam et`

### 21 · Ebeveyn paneli — ilk kurulum, eşleştirme kodu *(alt sayfa)*
Aynı alt sayfa çerçevesi · "Cihazları eşleştir" + açıklama · 6 haneli
üretilen kod (`Yesil50` zeminli tekli kutular) · "Kod 10 dakika geçerlidir" ·
"veya" ayracı · "Çocuğunun cihazındaki kodu gir" — 6 boş kutu, aktif olan
`YesilAna` kenarlık · kilit ikonlu gizlilik notu ("Kodlar yalnızca bu iki
cihaz arasında paylaşılır ve başka kimseyle görülmez.") · `Eşleştir`

### 22 · Ebeveyn paneli — eşleştirme başarılı *(alt sayfa)*
Yeşil gradyan daire içinde beyaz onay ikonu · "Eşleştirme başarılı" çip ·
"Elif'in cihazıyla eşleştirildi" · açıklama · Eşleşen cihaz / Rol satırları ·
`Ebeveyn paneline git`

### 23 · Toplanan veriler *(alt sayfa — Profil › Verilerim üstünde)*
"Toplanan veriler" + kapat · açıklama · 5 satır (ikon + başlık + alt
açıklama): Uygulama kullanım süresi · Adım sayısı · Anket ve kendini test et
cevapları · Günlük giriş serisi ve puanlar · Ad ve yaş grubu · altta kilit
ikonlu not "Uygulama içeriklerine (mesaj, arama, fotoğraf) asla erişilmez."

### 24 · Geçmişi indir *(alt sayfa)*
"Geçmişi indir" + kapat · açıklama · seçili kart "PDF raporu — Okunabilir
özet, grafiklerle" (`YesilAna` onay ikonu) · bilgi şeridi "Dosya hazırlanınca
kayıtlı e-posta adresine gönderilecek." · `İndirmeyi başlat`

### 25 · Verilerimi kalıcı sil — onay *(alt sayfa)*
`KirmiziIstisnaZemin` daire içinde çöp kutusu ikonu (`KirmiziIstisna`) ·
"Verilerini kalıcı olarak sil?" · açıklama · `KirmiziIstisnaZemin` kutu
içinde 3 madde (ekran süresi/hareket geçmişi, puan/rozet/ödül ilerlemesi,
ebeveyn eşleştirmesi) · onay kutusu "Verilerimin kalıcı olarak silineceğini
anlıyorum." · `KirmiziIstisna` dolgu buton "Verilerimi kalıcı olarak sil" ·
metin bağlantı "Vazgeç"

> Bu ekran CLAUDE.md §4'teki genel "kırmızı yasak" kuralının istisnalarından
> biri — bkz. §1 "Kırmızı istisnaları".

### 26 · Hesabı kalıcı sil — onay *(alt sayfa)*
25 ile aynı çerçeve, daha ağır uyarı: 4 maddelik kayıp listesi ("bu hesapla
bir daha giriş yapılamaz" dahil) · onay kutusu yerine **metin girişi** —
"SİL" yazılması zorunlu · `KirmiziIstisna` dolgu buton "Hesabımı kalıcı
olarak sil"

### 27 · YEDAM hakkında *(alt sayfa)*
Koyu yeşil daire kulaklık ikonu · "YEDAM hakkında" · açıklama (Yeşilay
Danışmanlık Merkezi) · `YesilNane` zeminli satır "115 — YEDAM Hattı, 7/24
ücretsiz ve gizli" · 2 gizlilik notu (görüşmeler gizli, uzman psikolog) ·
`Ara` butonu "115'i ara"

> **"YEDAM onayı" (köprü/yönlendirme rıza ekranı) ile karıştırılmaz** — bu
> yalnızca bilgilendirme; risk yüksekken kullanıcıya yönlendirme teklif eden
> onay akışı hâlâ teslimde yok (bkz. §4).

### 28 · Yardım *(alt sayfa)*
"Yardım" + kapat · açıklama · 4 SSS satırı (`›` ile): puan hesaplama,
ebeveyn paneli eşleştirmesini kaldırma, hedef değiştirme, veri paylaşımı

### 29 · Hakkında *(alt sayfa)*
`YesilKoyu` kare logo + "Yeşilİz" + sürüm numarası · açıklama cümlesi ·
3 satır: Kullanım şartları / Gizlilik politikası / Açık kaynak lisansları ·
altta telif metni

### 30 · Risk analizi ve öneriler — `riskdetay`
**CLAUDE.md §11 sıradaki iş #2'nin tasarımı.** Geri oklu yeşil başlık "Risk
analizi" · dairesel gösterge (turuncu→açık yeşil yay, ortada bant adı "Orta"
+ "risk düzeyi") · betimleyici cümle + "Geçen aya göre iyileşiyor" ·
**"ETKİLEYEN FAKTÖRLER"**: 4 yatay çubuk (Gece kullanımı: `KirmiziIstisna`
dolgu + "Yüksek" çipi; Kesintisiz kullanım süresi ve Sosyal medya oranı:
turuncu + "Orta" çipi; Fiziksel hareket: `YesilAna` + "Düşük risk" çipi) ·
**"ÖNERİLEN EYLEMLER"**: aksiyon kartları (ikon + başlık + açıklama + sağda
`Aç`/`Ayarla` çipi buton)

> Yalnızca "Gece kullanımı: Yüksek" çubuğu kırmızı — bkz. §1 "Kırmızı
> istisnaları". Diğer üç çubuk turuncu/yeşil, mevcut kategori paletiyle
> tutarlı.

### 31 · Uygulama kategorileri — `kategori`
Geri oklu yeşil başlık "Uygulama kategorileri" · üstte **risk analizi kartı**
(09'daki kartın aynısı: `PuanKartiBas`→`PuanKartiSon` gradyan, uyarı üçgeni,
"Risk analizi: Orta düzey", sağda `›`) → **30**'a gider · açıklama paragrafı ·
**akordiyon kategori grupları**: `⌄`/`›` + kategori adı büyük harf + sağda
toplam süre; açık grupta uygulama satırları (renkli yuvarlak kare baş harf
avatarı, uygulama adı, sağda süre + "bugün") · altta ince **yığın bar**
(kategori paleti) + "Bugünkü ekran süresinin %65'i sosyal medya ve videoda
geçti" · birincil buton `Ebeveyn paneline gönder` · altında metin bağlantı
`Kategorize et` → **32**

> Buton yalnızca ebeveyn eşleştirmesi tamamlanmış kullanıcıda görünür ve
> ebeveyne **kategori özeti** gider, uygulama adı değil (`docs/spec-veri.md`).

### 32 · Kategorize et — `kategori/duzenle`
Geri oklu yeşil başlık "Kategorize et" · "Henüz kategorisi olmayan **10
uygulama** var. Birine dokun, kategorisini seç." · 3 sütunlu ızgara:
yuvarlatılmış kare baş harf avatarı (uygulamadan türetilen ton) + altında
uygulama adı · altta birincil buton `Kaydet`

### 33 · Kategori seç *(alt sayfa — 32 üstünde)*
Zemin karartılır · beyaz alt sayfa, sürükleme çubuğu · başlıkta uygulama
avatarı + adı + "Bu uygulama için bir kategori seç" + sağda `×` · 6 seçim
satırı (ikon rozeti + kategori adı + sağda radio): seçili olan `YesilAna`
kenarlık + `Yesil50` dolgu + dolu onay ikonu, tablodan gelen tahminin altında
küçük yeşil "Önerilen" etiketi · birincil buton `Kategoriyi kaydet`

### 34 · Günlük hedef — `hedef` *(Profil › Hedeflerim)*
Geri oklu yeşil başlık "Günlük hedef" · **kalın dairesel gösterge**, ortada
`4s 45dk` + "günlük ekran hedefi" · altında "Bugün 4s 20dk kullandın · %75" ·
`Yesil50` zeminli ayar satırı: sol `−` (beyaz daire), ortada "4 saat 45 dakika",
sağ `+` (`YesilAna` daire) · **HIZLI SEÇİM** 4 çip (3s · 4s · seçili olan ·
6s), seçili çip `YesilAna` kenarlık + yeşil metin · saat ikonlu bilgi şeridi
(`Yesil50`) · birincil buton `Hedefi kaydet`

> Bilgi şeridindeki "Hedefini aştığında ebeveynine bilgilendirme gönderilir"
> cümlesi yalnızca ebeveyn eşleşmişse; değilse yalnızca "Değişiklikler
> yarından itibaren geçerli olur" kalır.

### 35 · Bildirimler — `bildirim`
Geri oklu yeşil başlık "Bildirimler" + sağda metin eylem "Tümünü oku" ·
**BUGÜN / DÜN / BU HAFTA** bölüm başlıkları (büyük harf, `MetinUcuncul`) ·
bildirim satırları: ikon rozeti + başlık + gövde + göreli zaman.
**Okunmamış** satır `Yesil50` dolgulu ve sağ üstte `YesilAna` nokta taşır;
okunmuş satır beyaz + `Ayrac` kenarlıklı.

### 36 · Arkadaş ekle — `lig/arkadas`
Geri oklu yeşil başlık "Arkadaş ekle" · koyu→açık yeşil gradyan kart:
ortada iki kişi ikonu, "Birlikte daha güçlüsünüz", açıklama ·
`Yesil50` zeminli **davet kodu** satırı ("Davet kodun" + büyük kod + sağda
`Kopyala` butonu) · 3'lü eylem ızgarası (Mesajla paylaş · Bağlantı paylaş ·
Kişilerden bul) · **ÖNERİLEN KİŞİLER** listesi: avatar + isim + alt metin +
sağda `Ekle` çerçeveli buton; davet gönderilmişse pasif "Davet gönderildi" çipi

> "Kişilerden bul" **bilinçli olarak devre dışı** — rehber verisi toplanmıyor
> (CLAUDE.md §4). Ekranda tek satırla belirtilir.

### 37 · Lig — boş durum *(varyasyon — 10)*
Yeşil başlık "Lig" · ortada boş durum illüstrasyonu (tek başına duran fidan) ·
"Henüz kimseyle yarışmıyorsun" · açıklama · birincil `Lige katıl` ·
ikincil `Arkadaş davet et` → **36**

### 38 · Şifremi unuttum — `hesap/sifre`
Geri oklu beyaz ekran · `Yesil50` daire içinde kilit ikonu · "Şifreni mi
unuttun?" · açıklama · "E-posta" etiketi + `Yesil50` zeminli alan ·
birincil `Sıfırlama bağlantısı gönder` · altta "Şifreni hatırladın mı?
**Giriş yap**"

### 39 · Widget — Bugünün sorusu *(ana ekran, orta boy)*
Beyaz kart, 20dp yarıçap · başlıkta `YesilAna` yuvarlatılmış kare içinde
yaprak logosu + "Bugünün sorusu" · soru metni "Bugün kaç saat uyudun?" ·
2×2 seçenek ızgarası, seçilmemişler `Yesil50` zemin / `MetinIkincil` metin,
seçili olan `YesilAna` dolgu / beyaz kalın metin

> **Etkileşimli widget** — seçim Glance `actionRunCallback` ile doğrudan
> kaydedilir, uygulama açılmaz. Görev kataloğundaki "Uyku beyanı" (3 puan,
> günlük limit 1) tamamlanır.

### 40 · Widget — Ekran süresi *(ana ekran, küçük kare)*
Beyaz kart · üstte kalın halka (`YesilAna` yay, `Yesil50` zemin), ortasında
telefon ikonu · altında `1s 42dk` (kalın, `YesilKoyu`) + "ekran süresi"
(`MetinIkincil`). Halka oranı günlük hedefe göre. Dokununca uygulama açılır.

### 41 · Widget — kilit ekranı *(UYGULANMIYOR)*
iOS kilit ekranı widget'ı: saat altında küçük halka + `1s 42dk`.
**Android'de karşılığı yok** — kilit ekranı widget'ı platformda genel
kullanıma açık bir yetenek değil. Tasarım referans olarak arşivde durur;
aynı bilgi **40**'taki ana ekran widget'ıyla veriliyor.

---

## 3. GEZİNME

```
Onboarding (01→02→03→04→05→06[→07])
        ↓
Alt sekmeler:  Ana Sayfa · İstatistik · Lig · Profil
        ↓
Ana Sayfa → Ödüller → [17][18][19] alt sayfaları
Ana Sayfa → Görev detayı (alt sayfa) → Nefes / Yürüyüş
Ana Sayfa → Bilgi Bankası (liste → yazı) · Kendini Test Et (quiz → sonuç)
Ana Sayfa → Bildirimler [35]  ← başlıktaki zil ikonu
Ana Sayfa / İstatistik → Risk detayı [30] → YEDAM onayı (önerilen eylem)
Lig [10] ⇄ boş durum [37] → Arkadaş ekle [36]
Profil → Ebeveyn paneli [13] → ilk kurulum [20]→[21]→[22] (yalnızca
        eşleştirilmemiş kullanıcıda) → Ebeveyn paneli [13]
Profil → Hedeflerim → Günlük hedef [34] / Uygulama kategorileri [31] →
        Kategorize et [32] → Kategori seç [33] / Değerlendirmeyi yenile [04]
Profil → Verilerim → Toplanan veriler [23] / Geçmişi indir [24] /
        Verilerimi sil [25] / Hesabı sil [26] / Health Connect izni
Profil → Yardım [28] / Hakkında [29] / YEDAM hakkında [27]
Hesap (14/15/38) — akış dışı, uygulamayı ilk açışta
```

Ana ekran widget'ları [39][40] uygulamadan bağımsız; [39] uygulamayı
açmadan doğrudan cevap kaydeder.

Alt gezinme çubuğu **yalnızca** 4 sekmede görünür; onboarding, nefes,
alt sayfalar ve ödül akışı tam ekran.

---

## 4. TESLİMDE OLMAYAN EKRANLAR

Bunların yerleşimi `stitch_markdown_design_system/` altından alınır, ama
**§1 tokenlarına ve §2'deki dile uyarlanır** — Stitch'in kendi tonları,
gölgeleri ve tipografisi taşınmaz.

| Ekran | Stitch klasörü | Uyarlama notu |
|---|---|---|
| Aktif yürüyüş | `aktif_y_r_y_d_zeltilmi/` | Sayaç + ilerleme; ana sayfanın gradyan diliyle |
| YEDAM onayı *(rıza/köprü akışı — 27 "YEDAM hakkında" ile karıştırılmaz)* | `yedam_onay/` | `yedam_iki_figur.png`; paylaşılacak bilgiler listesi aynen; "Bu onayı istedi" alt notu kalır |
| Health Connect izni | — | Tasarımı yok; **02 · İzin** ekranının yerleşimi birebir uyarlanır |

> **Widget artık teslimde** (30 Tem ikinci ek teslimi, ekran **39–41**) — bu
> tablodan çıkarıldı, `stitch/widget_tasar_m/` yerleşimi kullanılmıyor.

> **Risk detayı artık teslimde** (30 Tem ek teslimi, ekran **30**) — bu
> tablodan çıkarıldı, `stitch/risk_detay/` yerleşimi kullanılmıyor.

> **Mola hatırlatıcı (nudge) ve Görev detayı uygulandı** (30 Tem) — ikisi
> de bu tablodan çıkarıldı. `stitch/mola_hat_rlat_c/` ve `stitch/g_rev_detay/`
> yerleşimleri gerçek tokenlarla uygulanıp `nudge/` ve `AnaEkran`'ın görev
> detayı alt sayfasına dönüştü; resmi tasarım teslimi beklenmiyor.

---

## 5. UYGULAMA NOTLARI

- **Grafikler custom Canvas** (CLAUDE.md §12 K5). Tasarımdaki kesikli ızgara
  çizgileri, yuvarlatılmış çubuk uçları ve sağdaki eksen etiketleri
  `ui/bilesen/veri/` içinde yazılır.
- **Metin dili betimleyici.** Ekran 05 ve 13'te bant adı geçse bile cümle
  "şu an dikkat edilmesi gereken bir düzeyde" gibi kurulur, "bağımlısın" asla.
- **Ceza yok.** Puan geçmişinde (17) yalnızca `+` değerler var; tasarım da
  bunu doğruluyor.
- **Büyük harf** gereken yerlerde (`ORTALAMA`, `HEDEFLERİM`, `KUTU NEFESI`)
  `ui/TurkceMetin.kt → buyukHarf()` kullanılır, çıplak `uppercase()` değil.
- Ağaç illüstrasyonları `res/drawable-nodpi/agac_evre*.png` — tasarımda
  yer tutucu olarak duruyorlar, kod bunları gerçek dosyalarla doldurur.
- **Kırmızı yalnızca 25, 26 ve 30'daki tek çubukta** (`KirmiziIstisna` /
  `KirmiziIstisnaZemin`, bkz. §1). Başka hiçbir ekranda kırmızıya
  başvurulmaz.
- **Yeni paylaşılan bileşen adayı:** 6 haneli eşleştirme kodu kutuları
  (20, 21) — `ui/bilesen/KodKutusu` olarak tek yerde yazılıp hem kod
  üretme hem kod girme ekranında kullanılabilir.
