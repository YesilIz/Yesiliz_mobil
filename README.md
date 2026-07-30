# Yeşilİz

Telefon/ekran bağımlılığını erken tespit edip kullanıcıyı sağlıklı kullanıma
yönlendiren Android uygulaması. TEKNOFEST 2026 · Yeşilay iş birliğiyle.

> **Bu dosya bir harita.** Hangi dosya nerede, ne işe yarar.
> Kurallar ve mimari kararlar için → **[CLAUDE.md](CLAUDE.md)**

---

## Ne nerede — kısa yol

| Ne arıyorsun | Nereye bak |
|---|---|
| Risk skoru nasıl hesaplanıyor | `core-risk/` |
| Telefondan veri nasıl okunuyor | `app/.../data/kullanim/` |
| Veritabanı tabloları | `app/.../data/yerel/` · şema dökümü **[DB.md](DB.md)** |
| Renkler, yazı tipi, ölçüler | `app/.../ui/tema/` |
| Ekran tasarımları | **[DESIGN.md](DESIGN.md)** |
| Bir uygulama hangi kategoride | `app/src/main/assets/kategori_tablosu.json` |
| Proje kuralları, yasaklar, kararlar | **[CLAUDE.md](CLAUDE.md)** |

---

## Klasör yapısı

```
YEŞİLAY/
│
├── CLAUDE.md      ← kurallar, mimari, kararlar, yapılacaklar   [ÖNCE BUNU OKU]
├── DESIGN.md      ← tasarım sistemi + 19 ekranın speki
├── DB.md          ← veritabanı şeması (otomatik üretilir)
├── README.md      ← bu dosya
│
├── app/           ANDROID UYGULAMASI
├── core-risk/     RİSK MOTORU (Android'siz, saf Kotlin)
├── docs/          spesifikasyonlar + referans Python
├── tests/         doğrulama vektörleri
└── tools/         yardımcı scriptler
```

---

## `core-risk/` — risk motoru

Ekran süresi + anket puanı → **0-100 risk skoru** ve AZ/ORTA/ÇOK bandı.

**Android'i hiç tanımıyor.** Ayrı bir modül olması bilinçli: motorun saf
fonksiyon kalması garanti altında ve testleri emülatörsüz saniyeler içinde koşuyor.

```
core-risk/src/main/kotlin/tr/org/yesilay/yesiliz/risk/
├── Sabitler.kt        TÜM sayılar burada. Kalibrasyonda sadece burası değişir.
├── Girdi.kt           motora ne veriliyor
├── Cikti.kt           motordan ne çıkıyor (skor, bant, sürükleyiciler)
├── RiskHesapla.kt     asıl hesap — 7 adım
├── Alpha.kt           anket ne kadar ağırlıklı? (veri biriktikçe azalır)
└── Bantlama.kt        skor → bant. Eşik + güvenlik tabanı + histerezis + trend

core-risk/src/test/.../
├── VektorTest.kt      Python referansıyla birebir aynı mı? (54 vektör)
└── SenaryoTest.kt     çok günlü davranış anlamlı mı?
```

---

## `app/` — Android uygulaması

Kaynak kökü: `app/src/main/kotlin/tr/org/yesilay/yesiliz/`

### `data/` — dış dünyayla temas

```
data/
├── kullanim/          TELEFONDAN VERİ OKUMA
│   ├── OturumCikar.kt      olay akışı → "Instagram 10:00-10:30 açıktı"  [saf, testli]
│   ├── KullanimOkuyucu.kt  UsageStatsManager sarmalayıcısı
│   └── KullanimIzni.kt     izin verildi mi, ayar ekranı nerede
│
├── yerel/             VERİTABANI (Room)
│   ├── entity/Entityler.kt  7 tablo tanımı
│   ├── dao/Daolar.kt        sorgular
│   ├── YesilizDb.kt         veritabanının kendisi
│   └── Donusturucu.kt       tarih/saat ↔ veritabanı çevirisi
│
└── depo/              ZİNCİRİ BİRLEŞTİREN KATMAN
    ├── KategoriDeposu.kt   3.106 paketlik tabloyu veritabanına yükler
    └── KullanimDeposu.kt   oku → kategorile → kaydet → özetle
```

### `domain/` — iş mantığı

```
domain/
├── kategori/
│   ├── Kategori.kt          9 kategori (8 risk + "sistem")
│   ├── KategoriEsleyici.kt  paket adı → kategori
│   └── KategoriTablosu.kt   JSON tabloyu okur
│
└── kullanim/
    ├── KullanimOlay.kt      bir uygulamanın bir açılışı
    └── OruntuCikar.kt       ÖRÜNTÜ ÇIKARIMI — gece oranı, kesintisiz blok,
                             açılış sayısı, uyku, sabah alışkanlığı  [saf, testli]
```

### `ui/` — arayüz

```
ui/
├── tema/
│   ├── Renk.kt          DESIGN.md'deki renkler   ← renk değiştirmek istersen buradan
│   ├── Tipografi.kt     yazı tipi (Inter) ve boyutlar
│   ├── Sekil.kt         köşe yuvarlaklıkları
│   ├── Boyut.kt         boşluklar, buton yüksekliği
│   ├── Tema.kt          hepsini birleştirir
│   └── TemaOnizleme.kt  canlı stil galerisi (şu an uygulamada görünen tek ekran)
│
└── TurkceMetin.kt       buyukHarf() — Türkçe i/İ sorunu için, ÖNEMLİ
```

### `res/` — kaynaklar

```
res/
├── font/               Inter yazı tipi (4 ağırlık)
├── drawable-nodpi/     ağaç illüstrasyonları (5 evre) + YEDAM görseli
├── drawable/           uygulama ikonu (hilal)
└── values/             renkler, metinler, tema (yalnızca açılış ekranı için)
```

### Testler

| Klasör | Ne yapar | Emülatör? |
|---|---|---|
| `app/src/test/` | saf mantık testleri | hayır |
| `app/src/androidTest/` | veritabanı, gerçek sistem API'leri | **evet** |

---

## `docs/` — spesifikasyonlar

| Dosya | İçerik |
|---|---|
| `spec-risk.md` | Risk skoru formülleri, bantlama kuralları |
| `spec-anket.md` | SAS-SV anketi — 10 madde, puanlama |
| `spec-puan.md` | Görev, puan, lig, ödül sistemi |
| `spec-veri.md` | Veri politikası, 18 yaş altı ebeveyn modu |
| `referans/yesiliz_hesap.py` | **Risk motorunun referans hâli** — Kotlin bununla karşılaştırılıyor. Dokunma. |
| `stitch-ekranlar.md` | Hangi Stitch ekranı DESIGN.md'nin hangi bölümü |
| `spike-usagestats-sonuc.md` | UsageStats API'si hakkında bulgular |

---

## `tests/` ve `tools/` — scriptler

```
tests/
├── vektor_uret.py       Python referanstan doğrulama vektörü üretir
├── test_vectors.json    ← ÜRETİLEN dosya, elle düzenleme
└── kontrol_stitch.py    Stitch ekranlarını kurallara göre denetler

tools/
├── kategori_tablosu_uret.py   3.106 paketlik kategori tablosunu üretir
├── db_dokuman_uret.py         DB.md'yi Room şemasından üretir
├── agac_gorselleri_hazirla.py ağaç PNG'lerini temizler
└── stitch/                    Stitch tasarım sistemi yedeği
```

**Üretilen dosyalar elle düzenlenmez** — üreticisini çalıştır:

| Dosya | Komut |
|---|---|
| `tests/test_vectors.json` | `python3 tests/vektor_uret.py` |
| `app/.../kategori_tablosu.json` | `python3 tools/kategori_tablosu_uret.py` |
| `DB.md` | `./gradlew :app:assembleDebug` sonra `python3 tools/db_dokuman_uret.py` |

---

## Çalıştırma

```bash
# derle ve emülatöre kur
./gradlew :app:installDebug

# testler (toplam 71)
./gradlew :core-risk:test                  # 20 · hızlı
./gradlew :app:testDebugUnitTest           # 44 · hızlı
./gradlew :app:connectedDebugAndroidTest   # 15 · emülatör açık olmalı

# emülatörü başlat
emulator -avd Yesiliz_Pixel7_API36 &
```

Ayrıntılı komutlar ve ortam kurulumu → [CLAUDE.md §1](CLAUDE.md)

---

## Durum

**Çalışan:** risk motoru · kategori tablosu · örüntü çıkarımı · veritabanı ·
telefondan veri okuma · tema

**Eksik:** ekranlar. Uygulama açılıyor ama içinde şu an sadece bir stil galerisi var.

Sıradaki işler → [CLAUDE.md §11](CLAUDE.md)
