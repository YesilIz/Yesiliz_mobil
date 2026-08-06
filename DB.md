# DB.md — Yeşilİz Veritabanı

> **ÜRETİLEN DOSYA — elle düzenleme.**
> Kaynak: `app\schemas\tr.org.yesilay.yesiliz.data.yerel.YesilizDb\7.json`
> Yeniden üretmek için:
> ```bash
> ./gradlew :app:assembleDebug      # Room şemayı yeniden yazar
> python3 tools/db_dokuman_uret.py  # bu dosyayı günceller
> ```

**Şema sürümü:** 7 · **Tablo sayısı:** 15 · **Güncelleme:** 2026-08-06

---

## 0. Mahremiyet sözleşmesi

Bu şemada **yok ve olmayacak**: mesaj içeriği, fotoğraf, rehber,
arama geçmişi, konum/GPS. Yalnızca *hangi paket, ne kadar süre,
hangi kategori* tutuluyor (CLAUDE.md §4).

`kullanim_olay` tablosu **cihazdan çıkmaz.** Sunucuya (Faz 2)
yalnızca `gunluk_ozet` ve `risk_sonuc` özetleri gider.

---

## 1. Tablolar

| Tablo | Kolon | Birincil anahtar | İndeks | Amaç |
|---|---|---|---|---|
| `adim_sayac` | 3 | `tarih` | 0 |  |
| `aktivite_oturumu` | 4 | `id` | 1 |  |
| `anket_yanit` | 14 | `id` | 0 | SAS-SV yanıtları. Ham puan saklanır, bant değil (spec-anket.md §6.4). |
| `arkadas` | 3 | `isim` | 0 |  |
| `bildirim` | 6 | `id` | 1 |  |
| `farkindalik_testi` | 5 | `id` | 0 |  |
| `gorev_tamamlama` | 5 | `id` | 1 | Görev tamamlama geçmişi (spec-puan.md §7). |
| `gunluk_ozet` | 26 | `tarih` | 0 | Günlük özet — risk motorunun girdisi ve ekranların okuduğu satır. |
| `kategori_esleme` | 3 | `paketAdi` | 0 | Paket → kategori. `assets/kategori_tablosu.json`'dan seed edilir; kullanıcı override'ı aynı tabloda `kaynak='kullanici'` ile durur. |
| `kullanim_olay` | 6 | `id` | 2 | Ham kullanım olayları. **Cihazdan çıkmaz** (CLAUDE.md §4). En hacimli tablo; özet çıkarıldıktan sonra eskiler silinir. |
| `odul_secimi` | 3 | `kademeId` | 0 |  |
| `puan_gunluk` | 5 | `tarih` | 0 | Günlük puan ve seri. Ceza yok, sadece kazanç. |
| `risk_sonuc` | 12 | `tarih` | 0 | Günlük risk anlık görüntüsü. Trend ve histerezisin kaynağı. |
| `ruh_hali` | 2 | `tarih` | 0 |  |
| `uyku_beyani` | 2 | `tarih` | 0 |  |

---

## 2. Kolon dökümü

### `adim_sayac`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `toplam` | INTEGER | hayır |  |  |
| `sonSayac` | INTEGER | hayır |  |  |


### `aktivite_oturumu`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `baslangic` | INTEGER | hayır |  | epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir |
| `bitis` | INTEGER | hayır |  | epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |

- İndeks `index_aktivite_oturumu_tarih` → (tarih)

### `anket_yanit`

SAS-SV yanıtları. Ham puan saklanır, bant değil (spec-anket.md §6.4).

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `madde1` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde2` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde3` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde4` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde5` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde6` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde7` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde8` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde9` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `madde10` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `hamPuan` | REAL | hayır |  |  |
| `rS` | REAL | hayır |  |  |


### `arkadas`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `isim` | TEXT | hayır | ✔ |  |
| `durum` | TEXT | hayır |  |  |
| `eklenmeTarihi` | TEXT | hayır |  |  |


### `bildirim`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `tur` | TEXT | hayır |  |  |
| `baslik` | TEXT | hayır |  |  |
| `govde` | TEXT | hayır |  |  |
| `zaman` | INTEGER | hayır |  |  |
| `okundu` | INTEGER | hayır |  |  |

- İndeks `index_bildirim_zaman` → (zaman)

### `farkindalik_testi`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `yanitlar` | TEXT | hayır |  |  |
| `hamPuan` | INTEGER | hayır |  |  |
| `seviye` | TEXT | hayır |  |  |


### `gorev_tamamlama`

Görev tamamlama geçmişi (spec-puan.md §7).

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `gorevId` | TEXT | hayır |  |  |
| `puan` | INTEGER | hayır |  | hedef motorda `INTEGER` (aralığı dar, BIGINT gereksiz) |
| `dogrulamaTuru` | TEXT | hayır |  |  |

- İndeks `index_gorev_tamamlama_tarih` → (tarih)

### `gunluk_ozet`

Günlük özet — risk motorunun girdisi ve ekranların okuduğu satır.

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `socialDk` | REAL | hayır |  |  |
| `entertainmentDk` | REAL | hayır |  |  |
| `gamesDk` | REAL | hayır |  |  |
| `communicationDk` | REAL | hayır |  |  |
| `productivityDk` | REAL | hayır |  |  |
| `educationDk` | REAL | hayır |  |  |
| `healthDk` | REAL | hayır |  |  |
| `otherDk` | REAL | hayır |  |  |
| `gece` | REAL | hayır |  |  |
| `oturum` | REAL | hayır |  |  |
| `acilis` | REAL | hayır |  |  |
| `sabah` | REAL | hayır |  |  |
| `uykuOncesi` | REAL | hayır |  |  |
| `yerDegis` | REAL | hayır |  |  |
| `adim` | REAL | hayır |  |  |
| `sporDk` | REAL | hayır |  |  |
| `bTuketim` | REAL | hayır |  |  |
| `bIslevsel` | REAL | hayır |  |  |
| `P` | REAL | hayır |  |  |
| `A` | REAL | hayır |  |  |
| `D` | REAL | hayır |  |  |
| `S` | REAL | hayır |  |  |
| `geceKullanimDk` | REAL | hayır |  |  |
| `acilisSayisi` | INTEGER | hayır |  | hedef motorda `INTEGER` (aralığı dar, BIGINT gereksiz) |
| `olayVarMi` | INTEGER | hayır |  |  |


### `kategori_esleme`

Paket → kategori. `assets/kategori_tablosu.json`'dan seed edilir; kullanıcı override'ı aynı tabloda `kaynak='kullanici'` ile durur.

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `paketAdi` | TEXT | hayır | ✔ |  |
| `kategori` | TEXT | hayır |  |  |
| `kaynak` | TEXT | hayır |  |  |


### `kullanim_olay`

Ham kullanım olayları. **Cihazdan çıkmaz** (CLAUDE.md §4). En hacimli tablo; özet çıkarıldıktan sonra eskiler silinir.

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `id` | INTEGER | hayır | ✔ |  |
| `paketAdi` | TEXT | hayır |  |  |
| `kategori` | TEXT | hayır |  |  |
| `baslangic` | INTEGER | hayır |  | epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir |
| `bitis` | INTEGER | hayır |  | epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |

- İndeks `index_kullanim_olay_tarih` → (tarih)
- İndeks `index_kullanim_olay_baslangic` → (baslangic)

### `odul_secimi`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `kademeId` | TEXT | hayır | ✔ |  |
| `secenekId` | TEXT | hayır |  |  |
| `tarih` | TEXT | hayır |  | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |


### `puan_gunluk`

Günlük puan ve seri. Ceza yok, sadece kazanç.

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `puanHam` | INTEGER | hayır |  | hedef motorda `INTEGER` (aralığı dar, BIGINT gereksiz) |
| `puanNet` | INTEGER | hayır |  | hedef motorda `INTEGER` (aralığı dar, BIGINT gereksiz) |
| `seriGun` | INTEGER | hayır |  | hedef motorda `SMALLINT` (aralığı dar, BIGINT gereksiz) |
| `carpani` | REAL | hayır |  |  |


### `risk_sonuc`

Günlük risk anlık görüntüsü. Trend ve histerezisin kaynağı.

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `S` | REAL | hayır |  |  |
| `rB` | REAL | hayır |  |  |
| `rS` | REAL | evet |  |  |
| `alpha` | REAL | hayır |  |  |
| `R` | REAL | hayır |  |  |
| `adayBant` | TEXT | hayır |  |  |
| `gosterilenBant` | TEXT | hayır |  |  |
| `guvenlikKurali` | TEXT | evet |  |  |
| `trend` | TEXT | hayır |  |  |
| `surukleyiciler` | TEXT | hayır |  | virgülle ayrılmış liste — hedef motorda `TEXT[]` / ayrı tablo olabilir |
| `veriDurumu` | TEXT | hayır |  |  |


### `ruh_hali`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `deger` | REAL | hayır |  |  |


### `uyku_beyani`

| Kolon | SQLite | Null? | PK | Not |
|---|---|---|---|---|
| `tarih` | TEXT | hayır | ✔ | ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir |
| `aralik` | TEXT | hayır |  |  |


---

## 3. SQLite DDL — Room'un gerçekten çalıştırdığı

Bu bölüm Room tarafından üretildi, elle yazılmadı.

```sql
CREATE TABLE IF NOT EXISTS `adim_sayac` (`tarih` TEXT NOT NULL, `toplam` INTEGER NOT NULL, `sonSayac` INTEGER NOT NULL, PRIMARY KEY(`tarih`))
CREATE TABLE IF NOT EXISTS `aktivite_oturumu` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `baslangic` INTEGER NOT NULL, `bitis` INTEGER NOT NULL, `tarih` TEXT NOT NULL)
CREATE INDEX IF NOT EXISTS `index_aktivite_oturumu_tarih` ON `aktivite_oturumu` (`tarih`)
CREATE TABLE IF NOT EXISTS `anket_yanit` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tarih` TEXT NOT NULL, `madde1` INTEGER NOT NULL, `madde2` INTEGER NOT NULL, `madde3` INTEGER NOT NULL, `madde4` INTEGER NOT NULL, `madde5` INTEGER NOT NULL, `madde6` INTEGER NOT NULL, `madde7` INTEGER NOT NULL, `madde8` INTEGER NOT NULL, `madde9` INTEGER NOT NULL, `madde10` INTEGER NOT NULL, `hamPuan` REAL NOT NULL, `rS` REAL NOT NULL)
CREATE TABLE IF NOT EXISTS `arkadas` (`isim` TEXT NOT NULL, `durum` TEXT NOT NULL, `eklenmeTarihi` TEXT NOT NULL, PRIMARY KEY(`isim`))
CREATE TABLE IF NOT EXISTS `bildirim` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tur` TEXT NOT NULL, `baslik` TEXT NOT NULL, `govde` TEXT NOT NULL, `zaman` INTEGER NOT NULL, `okundu` INTEGER NOT NULL)
CREATE INDEX IF NOT EXISTS `index_bildirim_zaman` ON `bildirim` (`zaman`)
CREATE TABLE IF NOT EXISTS `farkindalik_testi` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tarih` TEXT NOT NULL, `yanitlar` TEXT NOT NULL, `hamPuan` INTEGER NOT NULL, `seviye` TEXT NOT NULL)
CREATE TABLE IF NOT EXISTS `gorev_tamamlama` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tarih` TEXT NOT NULL, `gorevId` TEXT NOT NULL, `puan` INTEGER NOT NULL, `dogrulamaTuru` TEXT NOT NULL)
CREATE INDEX IF NOT EXISTS `index_gorev_tamamlama_tarih` ON `gorev_tamamlama` (`tarih`)
CREATE TABLE IF NOT EXISTS `gunluk_ozet` (`tarih` TEXT NOT NULL, `socialDk` REAL NOT NULL, `entertainmentDk` REAL NOT NULL, `gamesDk` REAL NOT NULL, `communicationDk` REAL NOT NULL, `productivityDk` REAL NOT NULL, `educationDk` REAL NOT NULL, `healthDk` REAL NOT NULL, `otherDk` REAL NOT NULL, `gece` REAL NOT NULL, `oturum` REAL NOT NULL, `acilis` REAL NOT NULL, `sabah` REAL NOT NULL, `uykuOncesi` REAL NOT NULL, `yerDegis` REAL NOT NULL, `adim` REAL NOT NULL, `sporDk` REAL NOT NULL, `bTuketim` REAL NOT NULL, `bIslevsel` REAL NOT NULL, `P` REAL NOT NULL, `A` REAL NOT NULL, `D` REAL NOT NULL, `S` REAL NOT NULL, `geceKullanimDk` REAL NOT NULL, `acilisSayisi` INTEGER NOT NULL, `olayVarMi` INTEGER NOT NULL, PRIMARY KEY(`tarih`))
CREATE TABLE IF NOT EXISTS `kategori_esleme` (`paketAdi` TEXT NOT NULL, `kategori` TEXT NOT NULL, `kaynak` TEXT NOT NULL, PRIMARY KEY(`paketAdi`))
CREATE TABLE IF NOT EXISTS `kullanim_olay` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `paketAdi` TEXT NOT NULL, `kategori` TEXT NOT NULL, `baslangic` INTEGER NOT NULL, `bitis` INTEGER NOT NULL, `tarih` TEXT NOT NULL)
CREATE INDEX IF NOT EXISTS `index_kullanim_olay_tarih` ON `kullanim_olay` (`tarih`)
CREATE INDEX IF NOT EXISTS `index_kullanim_olay_baslangic` ON `kullanim_olay` (`baslangic`)
CREATE TABLE IF NOT EXISTS `odul_secimi` (`kademeId` TEXT NOT NULL, `secenekId` TEXT NOT NULL, `tarih` TEXT NOT NULL, PRIMARY KEY(`kademeId`))
CREATE TABLE IF NOT EXISTS `puan_gunluk` (`tarih` TEXT NOT NULL, `puanHam` INTEGER NOT NULL, `puanNet` INTEGER NOT NULL, `seriGun` INTEGER NOT NULL, `carpani` REAL NOT NULL, PRIMARY KEY(`tarih`))
CREATE TABLE IF NOT EXISTS `risk_sonuc` (`tarih` TEXT NOT NULL, `S` REAL NOT NULL, `rB` REAL NOT NULL, `rS` REAL, `alpha` REAL NOT NULL, `R` REAL NOT NULL, `adayBant` TEXT NOT NULL, `gosterilenBant` TEXT NOT NULL, `guvenlikKurali` TEXT, `trend` TEXT NOT NULL, `surukleyiciler` TEXT NOT NULL, `veriDurumu` TEXT NOT NULL, PRIMARY KEY(`tarih`))
CREATE TABLE IF NOT EXISTS `ruh_hali` (`tarih` TEXT NOT NULL, `deger` REAL NOT NULL, PRIMARY KEY(`tarih`))
CREATE TABLE IF NOT EXISTS `uyku_beyani` (`tarih` TEXT NOT NULL, `aralik` TEXT NOT NULL, PRIMARY KEY(`tarih`))
```

## 4. Tip eşleme — taşırken

| SQLite (Room) | PostgreSQL | MSSQL | Not |
|---|---|---|---|
| `INTEGER` | `BIGINT` | `BIGINT` | varsayılan — otomatik PK'da `BIGSERIAL` / `IDENTITY(1,1)` |
| `INTEGER` (aralığı dar) | `SMALLINT` / `INTEGER` | `SMALLINT` / `INT` | aşağıdaki liste |
| `TEXT` | `TEXT` | `NVARCHAR(MAX)` | Türkçe karakter için MSSQL'de **NVARCHAR şart** |
| `TEXT` (PK/indeks) | `TEXT` | `NVARCHAR(450)` | MSSQL'de `NVARCHAR(MAX)` indekslenemez |
| `REAL` | `DOUBLE PRECISION` | `FLOAT` | risk skorları burada |
| `BLOB` | `BYTEA` | `VARBINARY(MAX)` | şu an kullanılmıyor |

### Neden varsayılan `BIGINT`

SQLite'ın tek tamsayı tipi var ve o zaten 64 bit. Room'un şema JSON'unda
Kotlin'deki `Int` ile `Long` ayrımı **görünmüyor** — ikisi de `INTEGER`
yazıyor. Yani üretici, `madde1` (1–5) ile `baslangic` (epoch ms,
~1.785.000.000.000) arasındaki farkı şema dosyasından okuyamıyor.

`INTEGER`'ın tavanı 2.147.483.647. Epoch milisaniye bu tavanı 1970'te
aştı; epoch **saniye** bile 2038'de aşacak. Yanlış tarafa düşmenin
bedeli sessiz veri kaybı olduğu için varsayılan geniş tip.

### Daraltılmış kolonlar

Aralığı Kotlin tarafından bilinen kolonlar üreticide elle daraltıldı
(`tools/db_dokuman_uret.py` → `KOLON_TIPI`):

| Kolon | Gerçek aralık | PostgreSQL | MSSQL |
|---|---|---|---|
| `madde1`…`madde10` | Likert 1–5 | `SMALLINT` | `SMALLINT` |
| `seriGun` | gün serisi | `SMALLINT` | `SMALLINT` |
| `acilisSayisi` | günlük açılış adedi | `INTEGER` | `INT` |
| `puan` · `puanHam` · `puanNet` | görev/gün puanı | `INTEGER` | `INT` |

`BIGINT` kalanlar ve nedenleri:

- `baslangic` · `bitis` — epoch milisaniye, `INTEGER`'a **sığmaz**
- `id` (`BIGSERIAL` / `IDENTITY`) — Kotlin tarafı `Long`. Ayrıca
  `kullanim_olay` özet alındıktan sonra satır siliyor; sequence geri sayılmadığı için tavanı toplam satır değil toplam INSERT belirliyor.

**Dikkat edilecek üç nokta:**

1. **Tarihler metin.** `tarih` kolonları ISO metin (`2026-07-28`).
   SQLite'ın tarih tipi olmadığı için böyle. Taşırken `DATE`'e
   çevrilebilir, sıralama davranışı aynı kalır (ISO leksikografik sıralanır).
2. **Zaman damgaları epoch milisaniye.** `baslangic` / `bitis` `INTEGER`.
   Taşırken `TIMESTAMPTZ` / `DATETIME2` yapılabilir.
3. **`surukleyiciler` virgüllü liste.** İlişkisel motorda ayrı tabloya
   veya `TEXT[]`'e çevrilmesi daha doğru olur.

## 5. PostgreSQL DDL

CLAUDE.md §5'teki backend hedefi. Faz 2'de bu şema kullanılacak.

```sql
CREATE TABLE "adim_sayac" (
  "tarih" TEXT NOT NULL,
  "toplam" BIGINT NOT NULL,
  "sonSayac" BIGINT NOT NULL,
  PRIMARY KEY ("tarih")
);

CREATE TABLE "aktivite_oturumu" (
  "id" BIGSERIAL NOT NULL,
  "baslangic" BIGINT NOT NULL,
  "bitis" BIGINT NOT NULL,
  "tarih" TEXT NOT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX "index_aktivite_oturumu_tarih" ON "aktivite_oturumu" ("tarih");

CREATE TABLE "anket_yanit" (
  "id" BIGSERIAL NOT NULL,
  "tarih" TEXT NOT NULL,
  "madde1" SMALLINT NOT NULL,
  "madde2" SMALLINT NOT NULL,
  "madde3" SMALLINT NOT NULL,
  "madde4" SMALLINT NOT NULL,
  "madde5" SMALLINT NOT NULL,
  "madde6" SMALLINT NOT NULL,
  "madde7" SMALLINT NOT NULL,
  "madde8" SMALLINT NOT NULL,
  "madde9" SMALLINT NOT NULL,
  "madde10" SMALLINT NOT NULL,
  "hamPuan" DOUBLE PRECISION NOT NULL,
  "rS" DOUBLE PRECISION NOT NULL,
  PRIMARY KEY ("id")
);

CREATE TABLE "arkadas" (
  "isim" TEXT NOT NULL,
  "durum" TEXT NOT NULL,
  "eklenmeTarihi" TEXT NOT NULL,
  PRIMARY KEY ("isim")
);

CREATE TABLE "bildirim" (
  "id" BIGSERIAL NOT NULL,
  "tur" TEXT NOT NULL,
  "baslik" TEXT NOT NULL,
  "govde" TEXT NOT NULL,
  "zaman" BIGINT NOT NULL,
  "okundu" BIGINT NOT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX "index_bildirim_zaman" ON "bildirim" ("zaman");

CREATE TABLE "farkindalik_testi" (
  "id" BIGSERIAL NOT NULL,
  "tarih" TEXT NOT NULL,
  "yanitlar" TEXT NOT NULL,
  "hamPuan" BIGINT NOT NULL,
  "seviye" TEXT NOT NULL,
  PRIMARY KEY ("id")
);

CREATE TABLE "gorev_tamamlama" (
  "id" BIGSERIAL NOT NULL,
  "tarih" TEXT NOT NULL,
  "gorevId" TEXT NOT NULL,
  "puan" INTEGER NOT NULL,
  "dogrulamaTuru" TEXT NOT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX "index_gorev_tamamlama_tarih" ON "gorev_tamamlama" ("tarih");

CREATE TABLE "gunluk_ozet" (
  "tarih" TEXT NOT NULL,
  "socialDk" DOUBLE PRECISION NOT NULL,
  "entertainmentDk" DOUBLE PRECISION NOT NULL,
  "gamesDk" DOUBLE PRECISION NOT NULL,
  "communicationDk" DOUBLE PRECISION NOT NULL,
  "productivityDk" DOUBLE PRECISION NOT NULL,
  "educationDk" DOUBLE PRECISION NOT NULL,
  "healthDk" DOUBLE PRECISION NOT NULL,
  "otherDk" DOUBLE PRECISION NOT NULL,
  "gece" DOUBLE PRECISION NOT NULL,
  "oturum" DOUBLE PRECISION NOT NULL,
  "acilis" DOUBLE PRECISION NOT NULL,
  "sabah" DOUBLE PRECISION NOT NULL,
  "uykuOncesi" DOUBLE PRECISION NOT NULL,
  "yerDegis" DOUBLE PRECISION NOT NULL,
  "adim" DOUBLE PRECISION NOT NULL,
  "sporDk" DOUBLE PRECISION NOT NULL,
  "bTuketim" DOUBLE PRECISION NOT NULL,
  "bIslevsel" DOUBLE PRECISION NOT NULL,
  "P" DOUBLE PRECISION NOT NULL,
  "A" DOUBLE PRECISION NOT NULL,
  "D" DOUBLE PRECISION NOT NULL,
  "S" DOUBLE PRECISION NOT NULL,
  "geceKullanimDk" DOUBLE PRECISION NOT NULL,
  "acilisSayisi" INTEGER NOT NULL,
  "olayVarMi" BIGINT NOT NULL,
  PRIMARY KEY ("tarih")
);

CREATE TABLE "kategori_esleme" (
  "paketAdi" TEXT NOT NULL,
  "kategori" TEXT NOT NULL,
  "kaynak" TEXT NOT NULL,
  PRIMARY KEY ("paketAdi")
);

CREATE TABLE "kullanim_olay" (
  "id" BIGSERIAL NOT NULL,
  "paketAdi" TEXT NOT NULL,
  "kategori" TEXT NOT NULL,
  "baslangic" BIGINT NOT NULL,
  "bitis" BIGINT NOT NULL,
  "tarih" TEXT NOT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX "index_kullanim_olay_tarih" ON "kullanim_olay" ("tarih");
CREATE INDEX "index_kullanim_olay_baslangic" ON "kullanim_olay" ("baslangic");

CREATE TABLE "odul_secimi" (
  "kademeId" TEXT NOT NULL,
  "secenekId" TEXT NOT NULL,
  "tarih" TEXT NOT NULL,
  PRIMARY KEY ("kademeId")
);

CREATE TABLE "puan_gunluk" (
  "tarih" TEXT NOT NULL,
  "puanHam" INTEGER NOT NULL,
  "puanNet" INTEGER NOT NULL,
  "seriGun" SMALLINT NOT NULL,
  "carpani" DOUBLE PRECISION NOT NULL,
  PRIMARY KEY ("tarih")
);

CREATE TABLE "risk_sonuc" (
  "tarih" TEXT NOT NULL,
  "S" DOUBLE PRECISION NOT NULL,
  "rB" DOUBLE PRECISION NOT NULL,
  "rS" DOUBLE PRECISION,
  "alpha" DOUBLE PRECISION NOT NULL,
  "R" DOUBLE PRECISION NOT NULL,
  "adayBant" TEXT NOT NULL,
  "gosterilenBant" TEXT NOT NULL,
  "guvenlikKurali" TEXT,
  "trend" TEXT NOT NULL,
  "surukleyiciler" TEXT NOT NULL,
  "veriDurumu" TEXT NOT NULL,
  PRIMARY KEY ("tarih")
);

CREATE TABLE "ruh_hali" (
  "tarih" TEXT NOT NULL,
  "deger" DOUBLE PRECISION NOT NULL,
  PRIMARY KEY ("tarih")
);

CREATE TABLE "uyku_beyani" (
  "tarih" TEXT NOT NULL,
  "aralik" TEXT NOT NULL,
  PRIMARY KEY ("tarih")
);

```

## 6. MSSQL DDL

Taşıma ihtimaline karşı. Türkçe karakterler için `NVARCHAR` kullanıldı.

```sql
CREATE TABLE [adim_sayac] (
  [tarih] NVARCHAR(450) NOT NULL,
  [toplam] BIGINT NOT NULL,
  [sonSayac] BIGINT NOT NULL,
  PRIMARY KEY ([tarih])
);

CREATE TABLE [aktivite_oturumu] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [baslangic] BIGINT NOT NULL,
  [bitis] BIGINT NOT NULL,
  [tarih] NVARCHAR(450) NOT NULL,
  PRIMARY KEY ([id])
);
CREATE INDEX [index_aktivite_oturumu_tarih] ON [aktivite_oturumu] ([tarih]);

CREATE TABLE [anket_yanit] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [tarih] NVARCHAR(MAX) NOT NULL,
  [madde1] SMALLINT NOT NULL,
  [madde2] SMALLINT NOT NULL,
  [madde3] SMALLINT NOT NULL,
  [madde4] SMALLINT NOT NULL,
  [madde5] SMALLINT NOT NULL,
  [madde6] SMALLINT NOT NULL,
  [madde7] SMALLINT NOT NULL,
  [madde8] SMALLINT NOT NULL,
  [madde9] SMALLINT NOT NULL,
  [madde10] SMALLINT NOT NULL,
  [hamPuan] FLOAT NOT NULL,
  [rS] FLOAT NOT NULL,
  PRIMARY KEY ([id])
);

CREATE TABLE [arkadas] (
  [isim] NVARCHAR(450) NOT NULL,
  [durum] NVARCHAR(MAX) NOT NULL,
  [eklenmeTarihi] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([isim])
);

CREATE TABLE [bildirim] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [tur] NVARCHAR(MAX) NOT NULL,
  [baslik] NVARCHAR(MAX) NOT NULL,
  [govde] NVARCHAR(MAX) NOT NULL,
  [zaman] BIGINT NOT NULL,
  [okundu] BIGINT NOT NULL,
  PRIMARY KEY ([id])
);
CREATE INDEX [index_bildirim_zaman] ON [bildirim] ([zaman]);

CREATE TABLE [farkindalik_testi] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [tarih] NVARCHAR(MAX) NOT NULL,
  [yanitlar] NVARCHAR(MAX) NOT NULL,
  [hamPuan] BIGINT NOT NULL,
  [seviye] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([id])
);

CREATE TABLE [gorev_tamamlama] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [tarih] NVARCHAR(450) NOT NULL,
  [gorevId] NVARCHAR(MAX) NOT NULL,
  [puan] INT NOT NULL,
  [dogrulamaTuru] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([id])
);
CREATE INDEX [index_gorev_tamamlama_tarih] ON [gorev_tamamlama] ([tarih]);

CREATE TABLE [gunluk_ozet] (
  [tarih] NVARCHAR(450) NOT NULL,
  [socialDk] FLOAT NOT NULL,
  [entertainmentDk] FLOAT NOT NULL,
  [gamesDk] FLOAT NOT NULL,
  [communicationDk] FLOAT NOT NULL,
  [productivityDk] FLOAT NOT NULL,
  [educationDk] FLOAT NOT NULL,
  [healthDk] FLOAT NOT NULL,
  [otherDk] FLOAT NOT NULL,
  [gece] FLOAT NOT NULL,
  [oturum] FLOAT NOT NULL,
  [acilis] FLOAT NOT NULL,
  [sabah] FLOAT NOT NULL,
  [uykuOncesi] FLOAT NOT NULL,
  [yerDegis] FLOAT NOT NULL,
  [adim] FLOAT NOT NULL,
  [sporDk] FLOAT NOT NULL,
  [bTuketim] FLOAT NOT NULL,
  [bIslevsel] FLOAT NOT NULL,
  [P] FLOAT NOT NULL,
  [A] FLOAT NOT NULL,
  [D] FLOAT NOT NULL,
  [S] FLOAT NOT NULL,
  [geceKullanimDk] FLOAT NOT NULL,
  [acilisSayisi] INT NOT NULL,
  [olayVarMi] BIGINT NOT NULL,
  PRIMARY KEY ([tarih])
);

CREATE TABLE [kategori_esleme] (
  [paketAdi] NVARCHAR(450) NOT NULL,
  [kategori] NVARCHAR(MAX) NOT NULL,
  [kaynak] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([paketAdi])
);

CREATE TABLE [kullanim_olay] (
  [id] BIGINT IDENTITY(1,1) NOT NULL,
  [paketAdi] NVARCHAR(MAX) NOT NULL,
  [kategori] NVARCHAR(MAX) NOT NULL,
  [baslangic] BIGINT NOT NULL,
  [bitis] BIGINT NOT NULL,
  [tarih] NVARCHAR(450) NOT NULL,
  PRIMARY KEY ([id])
);
CREATE INDEX [index_kullanim_olay_tarih] ON [kullanim_olay] ([tarih]);
CREATE INDEX [index_kullanim_olay_baslangic] ON [kullanim_olay] ([baslangic]);

CREATE TABLE [odul_secimi] (
  [kademeId] NVARCHAR(450) NOT NULL,
  [secenekId] NVARCHAR(MAX) NOT NULL,
  [tarih] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([kademeId])
);

CREATE TABLE [puan_gunluk] (
  [tarih] NVARCHAR(450) NOT NULL,
  [puanHam] INT NOT NULL,
  [puanNet] INT NOT NULL,
  [seriGun] SMALLINT NOT NULL,
  [carpani] FLOAT NOT NULL,
  PRIMARY KEY ([tarih])
);

CREATE TABLE [risk_sonuc] (
  [tarih] NVARCHAR(450) NOT NULL,
  [S] FLOAT NOT NULL,
  [rB] FLOAT NOT NULL,
  [rS] FLOAT,
  [alpha] FLOAT NOT NULL,
  [R] FLOAT NOT NULL,
  [adayBant] NVARCHAR(MAX) NOT NULL,
  [gosterilenBant] NVARCHAR(MAX) NOT NULL,
  [guvenlikKurali] NVARCHAR(MAX),
  [trend] NVARCHAR(MAX) NOT NULL,
  [surukleyiciler] NVARCHAR(MAX) NOT NULL,
  [veriDurumu] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([tarih])
);

CREATE TABLE [ruh_hali] (
  [tarih] NVARCHAR(450) NOT NULL,
  [deger] FLOAT NOT NULL,
  PRIMARY KEY ([tarih])
);

CREATE TABLE [uyku_beyani] (
  [tarih] NVARCHAR(450) NOT NULL,
  [aralik] NVARCHAR(MAX) NOT NULL,
  PRIMARY KEY ([tarih])
);

```

---

## 7. Değişiklik kaydı

| Sürüm | Tarih | Değişiklik |
|---|---|---|
| 1 | 2026-07-28 | İlk şema — 7 tablo (CLAUDE.md §11) |

> Şema değiştirdiğinde `YesilizDb.version`'ı artır, bu tabloya satır ekle,
> sonra üretici scripti çalıştır. Prototip boyunca migration yazılmıyor
> (`fallbackToDestructiveMigration`) — **yayına çıkmadan önce mutlaka**
> gerçek migration'lar yazılmalı, yoksa her güncellemede kullanıcı geçmişi silinir.
