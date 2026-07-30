# spec-risk.md — Risk Motoru ve Bantlama

> Kaynak: CLAUDE.md, taşındı 2026-07-24

---

## 4. RİSK MOTORU — TAM SPEK

### 4.1 Kategori ağırlıkları

```kotlin
val KATEGORI_AGIRLIK = mapOf(
    "social"        to 1.0,   // Instagram, X, Snapchat
    "entertainment" to 1.0,   // TikTok, YouTube, Netflix
    "games"         to 0.8,
    "communication" to 0.5,   // WhatsApp, Telegram
    "productivity"  to 0.3,   // CapCut, Notion, Office
    "education"     to 0.2,
    "health"        to 0.1,
    "other"         to 0.5    // tabloda bulunamayan
)
val TUKETIM = setOf("social", "entertainment", "games")
```

> **GEÇİCİ değerler.** Literatür tahmini. Veri gelince çoklu regresyonun
> standartlaştırılmış beta katsayılarıyla değişecek: `w_c = max(0, β_c) / max|β_j|`

### 4.2 Örüntü katsayıları

```kotlin
val ORUNTU_KATSAYI = mapOf(
    "gece"        to 0.6,   // 00:00-06:00 kullanım oranı
    "oturum"      to 0.4,   // 30dk+ kesintisiz TÜKETİM blokları
    "acilis"      to 0.3,   // saat başına açılış (6/saat = 1.0)
    "sabah"       to 0.2,   // uyanınca hemen telefona sarılma
    "uyku_oncesi" to 0.3,   // yatmadan önceki 30 dk
    "yer_degis"   to 0.4    // ekran artarken aktivite düşüyor mu
)
```

Hepsi 0–1 normalize.

#### Ölçüm tanımları — üçü GEÇİCİ

Yukarıdaki tablo katsayıları veriyor ama **nasıl ölçüleceğini söylemiyordu.**
Tanımlar `app/domain/kullanim/OruntuCikar.kt` içinde yapıldı:

| Örüntü | Kaynak | Tanım |
|---|---|---|
| `gece` | ✅ spec | 00:00–06:00 arası dakika / toplam dakika |
| `acilis` | ✅ spec | (açılış sayısı / uyanık saat) / 6, tavan 1,0 |
| `oturum` | ⚠️ **geçici** | 30dk+ kesintisiz tüketim bloklarındaki dakika / toplam tüketim dakikası. Blok toleransı 2 dk (uygulama değiştirmek tıkınmayı kesmez) |
| `sabah` | ⚠️ **geçici** | uyanış sonrası ilk oturumun süresi / 30 dk, tavan 1,0 |
| `uyku_oncesi` | ⚠️ **geçici** | uykudan önceki 30 dk içindeki kullanım / 30 |
| `yer_degis` | ⚠️ **geçici** | çok günlü: son 7 günün iki yarısı arasında ekran artışı **ve** adım düşüşü |

**Uyku penceresi** doğrudan ölçülmüyor, kullanımdan çıkarsanıyor: olaylar
arasındaki en uzun boşluk ≥ 4 saat **ve** orta noktası yerel saatle 22:00–08:00
arasına düşüyorsa uyku sayılıyor. Gece koşulu şart — telefonu hiç açmadan
geçen bir iş günü de 8 saatlik boşluk üretir ve uyku sanılırdı
(bu hata yazıldı, testte yakalandı, düzeltildi).

> ⚠️ işaretli dördü **onay bekliyor.** Katsayı toplamları 1,3 — örüntü
> çarpanı P'nin 2,2'lik azami aralığının %59'u. Yanlış ölçülürse skor
> sistematik olarak kayar. Pilot veriyle doğrulanmalı (§4.8 ile birlikte).
>
> `sabah` için not: "uyanıştan ilk kullanıma kadar geçen gecikme" ölçülemiyor —
> uyanma anını zaten ilk telefon açılışından çıkarsadığımız için gecikme tanım
> gereği sıfır olurdu. Health Connect uyku verisi geldiğinde (§17 md.10)
> gerçek uyanma saati bilinecek ve tanım gecikmeye çevrilebilecek.

### 4.3 Referans sabitleri

```kotlin
const val S_REF = 300.0          // davranış skoru referansı
const val SASSV_KESME = 29.5     // Türkçe ergen formu
const val D_MAX = 0.15           // aktivite sönümleme tavanı
const val BANT_AZ_UST = 40.0
const val BANT_ORTA_UST = 65.0
const val PENCERE_GUN = 7
```

> **S_REF = 300, 120 DEĞİL.** TBM'in 120 dakikası *hedeftir*, *eşik değil*.
> 120'yi eşik alırsak günümüzde herkes "çok riskli" çıkar (ortalama kullanım 4-5 saat)
> ve ölçüm ayırt ediciliğini kaybeder. 120 dakika **hedef sisteminde** kullanılır (§7.2).

### 4.4 Hesap sırası

```
1. Yükü İKİYE AYIR
   B_tuketim  = Σ (t_c × w_c)  for c in TUKETIM
   B_islevsel = Σ (t_c × w_c)  for c not in TUKETIM

2. Örüntü çarpanı
   P = 1 + Σ (β_i × p_i)

3. Aktivite sönümlemesi
   A = 0.4 × min(1, adim/8000) + 0.6 × min(1, spor_dk/30)
   D = 1 - min(0.15, 0.15 × A)

4. Ham skor — çarpan ve sönümleme SADECE tüketime
   S = B_islevsel + B_tuketim × P × D

5. Davranış skoru
   R_b = 100 × (1 - exp(-0.693 × S / 300))     // S=300 → tam 50

6. Anket skoru (kesme 29.5 → tam 50)
   X <= 29.5:  R_s = 50 × (X-10) / 19.5
   X >  29.5:  R_s = 50 + 50 × (X-29.5) / 30.5

7. Birleştir
   R = alpha × R_s + (1-alpha) × R_b
```

### 4.5 alpha (anket ağırlığı)

```kotlin
val alpha0 = when {
    d <= 0  -> 1.00
    d < 7   -> 1.00 - 0.5 * (d / 7.0)          // 1.00 → 0.50
    d < 28  -> 0.50 - 0.2 * ((d - 7) / 21.0)   // 0.50 → 0.30
    else    -> 0.30
}
val alpha = alpha0 * maxOf(0.0, 1.0 - g / 60.0)
```

`d` = davranış verisi gün sayısı · `g` = anketten bu yana geçen gün

**Android geçmiş verisi — efektif gün:**

```
d_efektif = Σ (gün kalite ağırlığı)
  canlı gün (tam veri)       → 1.0
  geçmiş gün (sadece hacim)  → 0.6
```

`queryUsageStats()` geriye dönük **hacim** verir; `queryEvents()` kısa saklandığı için **örüntü** vermez. Geçmiş yarım veridir, tam veri gibi sayılmaz. iOS'te geçmiş sıfır → `d = 0`.

### 4.6 KRİTİK TASARIM KARARLARI — bozma

1. **Yük ikiye bölünür.** Örüntü cezası ve sönümleme sadece tüketim yüküne. Yoksa sosyal medyadan gelen kompulsiflik cezası ders saatlerini de cezalandırır.

2. **`oturum` metriği sadece tüketim kategorilerinde.** 2 saatlik CapCut kurgusu = derin çalışma. 2 saatlik TikTok = tıkınma. Aynı metrikle ölçülemez.

3. **İki skor 50'de buluşur.** SAS-SV 29.5 → 50, S_REF 300 → 50. Ağırlıklı ortalama bu sayede anlamlı.

4. **Karar 7 günlük kayan ortalamayla.** Tek gün gürültüdür.

5. **Her hesap `drivers` döndürmeli** (en çok katkı yapan 3 etken). Nudge metni bunu kullanıyor.

### 4.7 Doğrulama profilleri

Kotlin portu bu değerleri üretmeli:

| Profil | Ham ekran | S | R_b | Bant |
|---|---|---|---|---|
| Hafif (2,5 sa, üretim ağırlıklı) | 150 dk | 112,0 | 22,8 | AZ |
| Üretim ağırlıklı (5,5 sa), iyi örüntü | 330 dk | 172,5 | 32,9 | AZ |
| Ortalama (4 sa, dengeli) | 240 dk | 242,1 | 42,8 | ORTA |
| Üretim ağırlıklı, kötü örüntü | 330 dk | 337,3 | 54,1 | ORTA |
| Ağır tüketici (6,5 sa, gece) | 390 dk | 768,8 | 83,1 | ÇOK |

**Girdiler `tests/test_vectors.json` → `vektorler.dogrulama_profilleri` altında**
(kategori dağılımı, örüntü değerleri, adım, spor dakikası). Beşinde de anket yok
(`anket_puani = null`), dolayısıyla `alpha = 0` ve `R = R_b`.

> **2026-07-28 düzeltmesi.** Bu tablonun önceki hâli (115 / 177 / 226 / 330 / 765 ve
> 23,3 / 33,5 / 40,9 / 53,3 / 82,9) **girdileri kayıtlı olmayan** sayılardı ve iki satırı
> kendi formülüyle bile tutmuyordu: S=177 → 33,56 (33,5 yazıyordu), S=226 → 40,67
> (40,9 yazıyordu). §14 portun bu tabloyu 1e-4 toleransla üretmesini şart koştuğu için
> bu satırlar sahte test hatası üretirdi. Tablo artık `tests/vektor_uret.py`'nin
> referans Python'dan hesapladığı, **girdisi kayıtlı** değerlerden geliyor. Bant sütunu
> (AZ/AZ/ORTA/ORTA/ÇOK) değişmedi — profillerin niteliksel iddiası korundu.

**Yeniden üretim:** `python3 tests/vektor_uret.py` — referans Python veya sabitler
değişirse bu tabloyu script çıktısındaki değerlerle güncelle.

### 4.8 Bilinen sorun

`R_b` tepede doyuyor. Bantlama için sorun değil ama **trend takibinde kör nokta** — kullanıcı çok yüksekten inse bile skor az düşer. Pilotta gerçek dağılıma bakıp `S_REF` veya eğim (0.693) ayarlanmalı.

---

## 5. BANTLAMA

### 5.1 Eşikler

```
0 ───── 40 ───── 50 ───── 65 ───── 100
   AZ     │  ORTA   │      ÇOK
       net altı  sınır civarı  net üstü
```

### 5.2 Dört mekanizma — sıra önemli

```
1. eşikten aday bant
2. güvenlik tabanı uygula (aday yükselebilir)
3. histerezis (aday kabul edilir mi?)
4. trend (bandı değiştirmez, MESAJI değiştirir)
```

### 5.3 Güvenlik tabanı

5 gün üst üste:

| Koşul | Taban |
|---|---|
| Tüketim > 480 dk | ÇOK |
| Tüketim > 360 dk | ORTA |
| Gece kullanımı > 90 dk | ORTA |

Kronik kullanıcıyı yakalamak için gerekli — bu kişilerde değişim sinyali yoktur.

### 5.4 Histerezis

```kotlin
const val GUN_YUKARI = 3
const val GUN_ASAGI  = 7
```

Asimetrik: uyarmakta hızlı, rahatlamakta yavaş.

> **BİLİNEN TUZAK:** Sayaç sadece YÖN değişirse sıfırlanmalı. Kullanıcı iyileşmeye
> devam edip daha da düşük banda aday olursa (ORTA→AZ), sayaç sıfırlanmamalı.
> Aksi hâlde "daha çok iyileştiğin için baştan başla" cezası doğar.
> **Bu hata bir kez yapıldı, `docs/referans/yesiliz_hesap.py`'de düzeltildi.**
> Regresyon vektörü: `tests/test_vectors.json` → `histerezis.TUZAK_ayni_yon_sayac_sifirlanmaz`

### 5.5 Trend

Son 7 gün ortalaması vs önceki 7 gün. Fark > +5 kötüleşiyor, < −5 iyileşiyor.

Mesajı değiştirir: "ÇOK + iyileşiyor" → "doğru yoldasın" · "ÇOK + kötüleşiyor" → "durum ağırlaşıyor"
