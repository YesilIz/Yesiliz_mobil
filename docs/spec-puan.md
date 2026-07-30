# spec-puan.md — Görev/Puan, Leaderboard, Ödül

> Kaynak: CLAUDE.md, taşındı 2026-07-24

---

## 7. GÖREV VE PUAN SİSTEMİ

> **Temel kural: durumu değil, çabayı ödüllendir.**
> Puan sistemi risk skorundan **tamamen bağımsız**. Risk düşük olmalı, puan yüksek olmalı.

### 7.1 Görev kataloğu

| Görev | Puan | Doğrulama | Günlük limit |
|---|---|---|---|
| Nefes egzersizi (2 dk) | 5 | Uygulama içi tamamlama | 2 kez |
| Dijital mola (15 dk ekransız) | 10 | Ekran kapalı doğrulaması | 2 kez |
| Doğrulanmış spor/yürüyüş | 3 / dk | Spor başlat + ivmeölçer | 30 dk (90 puan) |
| Adım hedefi | adım / 200 | Sağlık platformu | tavan 40 |
| Günlük tüketim hedefi | 25 | Otomatik | 1 kez |
| Gece temiz (00–06 < 15 dk) | 10 | Otomatik | 1 kez |
| Ruh hali kaydı | 3 | Tek dokunuş | 1 kez |
| Uyku beyanı | 3 | Tek dokunuş | 1 kez |

**Teorik max:** 201 · **Uygulanan tavan:** **150**

### 7.2 Kişisel tüketim hedefi

```kotlin
val hedef = maxOf(120.0, baslangicTuketim * (1 - 0.05 * hafta))
```

- **Taban 120 dk** = TBM önerisi. Hedef bunun altına inmez.
- İlk hafta yalnızca ölçüm, hedef yok. (Android'de geçmiş veriden kurulabilir → 1 hafta kazanç. iOS'te beklemek zorunlu → o hafta hacimden bağımsız görevler sun.)
- **Emniyet valfi:** 3 hafta üst üste kaçırma varsa hafta sayacı ilerlemez.

Ağır kullanıcı 285'i tutturunca da, hafif kullanıcı 171'i tutturunca da **aynı 25 puan**.

### 7.3 Risk bandına göre görev ataması

Günde 3-4 görev önerilir.

| Bant | Görevler | Ton |
|---|---|---|
| AZ | Nefes, dijital mola, ruh hali, adım hedefi | Koruyucu |
| ORTA | Yürüyüş (10-15 dk), adım, tüketim hedefi, gece temiz | Aktifleştirici |
| ÇOK | Uzun spor (20-30 dk), gece temiz, tüketim hedefi, günlük takip | Destekleyici |

Yüksek bantta daha çok puan kazanılabilir — bilinçli. Kasıtlı kötüleşme kendini baltalar (tüketim hedefi zorlaşır, tavan 150 kaçışı sınırlar).

### 7.4 Seri

```kotlin
val carpan = minOf(2.0, 1 + 0.1 * seriGun)
```

**Kayıp koruması:** Haftada bir gün kaçırma seriyi sıfırlamaz, dondurur.

### 7.5 Günlük puan

```
Π_ham = 3 × spor_dk + min(40, adım/200) + görev puanları
      + 25 (hedef tutturulduysa) + 10 (gece temizse)

Π = min(150, Π_ham × seri_çarpanı)
```

---

## 8. LEADERBOARD

| Öğe | Kural |
|---|---|
| Grup büyüklüğü | ~30 kişi |
| Gruplandırma | **Başlangıç risk bandı** + mevcut lig seviyesi |
| Döngü | Pazartesi 00:00 – Pazar 23:59 |
| Sıralama | Haftalık toplam puan |
| Yükselme | İlk 7 |
| Düşme | Son 7 |

**Lig seviyeleri:** Fidan → Filiz → Ağaç → Koru → Orman

**Kritik:** Gruplandırma başlangıç risk bandına göre. Yüksek riskliler kendi aralarında yarışır, düşük risklilerle aynı listede ezilmezler.

**Lig bonusu:** 1. → 100 · 2. → 60 · 3. → 40 · 4-7. → 20 puan

**Opt-out zorunlu özellik.** Bazı kullanıcı için sosyal karşılaştırma stres kaynağı.

Ek: okul/sınıf ligi (Yeşilay okul programlarıyla entegrasyon), arkadaş ligi (davet kodu).

---

## 9. ÖDÜL SİSTEMİ

### 9.1 Kademeler

| Kademe | Puan | Ödül | Ek koşul |
|---|---|---|---|
| 1 | 500 | Dijital rozet | — |
| 2 | 2.000 | **Fidan bağışı** (kullanıcı adına, sertifikalı) | 14 aktif gün |
| 3 | 5.000 | Yeşilay Market kuponu | 30 aktif gün |
| 4 | 10.000 | Spor / kitap kuponu | 45 aktif gün |

**Aktif gün:** en az bir görev tamamlanmış + kullanım verisi kaydedilmiş gün.
Aktif kullanıcı ~6 haftada ilk maddi ödüle ulaşır (~120 puan/gün).

### 9.2 Anti-gaming

| Risk | Önlem |
|---|---|
| Sahte spor kaydı | İvmeölçer tutarlılık kontrolü |
| Tek günde patlama | Günlük tavan 150 |
| Adım manipülasyonu | Artış > %200 → işaretle, dondur |
| Silip yeniden kurma | Başlangıç hedefi sunucuda |
| Çoklu hesap | Telefon doğrulama, cihaz parmak izi |
| Kupon suistimali | Manuel onay (uzman paneli) |
