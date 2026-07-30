# -*- coding: utf-8 -*-
"""
YEŞİLİZ — DB.md ÜRETİCİ

`DB.md` dosyasını Room'un dışa aktardığı şema JSON'undan üretir.

    python3 tools/db_dokuman_uret.py

**Neden üretiliyor, elle yazılmıyor:** elle tutulan şema dokümanı er geç
koddan sapar ve yalan söylemeye başlar. Bu script tek doğruluk kaynağı
olarak `app/schemas/.../<sürüm>.json` dosyasını alıyor — o dosyayı da
Room'un kendisi üretiyor. Şema değişti mi:

    ./gradlew :app:assembleDebug     # yeni <sürüm>.json çıkar
    python3 tools/db_dokuman_uret.py # DB.md güncellenir

Üretilenler:
  · tablo/kolon dökümü, birincil anahtarlar, indeksler
  · SQLite DDL (Room'un gerçekten çalıştırdığı)
  · PostgreSQL DDL  (CLAUDE.md §5'teki backend hedefi)
  · MSSQL DDL       (taşıma ihtimaline karşı)
  · tip eşleme tablosu
"""

import glob
import json
import os
import re
import sys
from datetime import date

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
SEMA_DIZINI = os.path.join(KOK, "app", "schemas")
HEDEF = os.path.join(KOK, "DB.md")

# Room'un SQLite "affinity" değerleri → diğer motorlar.
# SQLite tipsize yakındır; hedef motorlarda gerçek tip seçmek zorundayız.
#
# INTEGER'ın karşılığı neden BIGINT: SQLite'ın tek tamsayı tipi var ve o zaten
# 64 bit. Şema JSON'unda `Int` ile `Long` ayrımı görünmüyor — ikisi de "INTEGER".
# Bu yüzden varsayılan kayıpsız olanı: BIGINT. Aralığı bilinen kolonlar
# aşağıdaki KOLON_TIPI ile daraltılıyor.
TIP_ESLEME = {
    "INTEGER": ("BIGINT", "BIGINT"),
    "TEXT": ("TEXT", "NVARCHAR(MAX)"),
    "REAL": ("DOUBLE PRECISION", "FLOAT"),
    "BLOB": ("BYTEA", "VARBINARY(MAX)"),
}

# Aralığı bilinen kolonlar — varsayılan BIGINT'i daraltır.
# Şema JSON'u bu bilgiyi taşımıyor; Kotlin tarafındaki gerçek aralık burada.
# Otomatik üretilen birincil anahtarlar buraya girmez, onlar BIGSERIAL kalır.
KOLON_TIPI = {}
for _i in range(1, 11):                       # SAS-SV maddeleri: Likert 1–5
    KOLON_TIPI["madde%d" % _i] = ("SMALLINT", "SMALLINT")
KOLON_TIPI["seriGun"] = ("SMALLINT", "SMALLINT")        # gün serisi
KOLON_TIPI["acilisSayisi"] = ("INTEGER", "INT")         # günlük açılış adedi
KOLON_TIPI["puan"] = ("INTEGER", "INT")                 # görev puanı
KOLON_TIPI["puanHam"] = ("INTEGER", "INT")
KOLON_TIPI["puanNet"] = ("INTEGER", "INT")

# Kotlin tarafında anlamı olan ama SQLite'ta TEXT/INTEGER görünen alanlar.
# Taşırken doğru tipi seçebilmek için not düşülüyor.
ANLAM_NOTU = {
    "tarih": "ISO tarih metni (`2026-07-28`) — hedef motorda `DATE` yapılabilir",
    "baslangic": "epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir",
    "bitis": "epoch milisaniye — hedef motorda `TIMESTAMPTZ` / `DATETIME2` yapılabilir",
    "surukleyiciler": "virgülle ayrılmış liste — hedef motorda `TEXT[]` / ayrı tablo olabilir",
}

TABLO_ACIKLAMA = {
    "kullanim_olay": "Ham kullanım olayları. **Cihazdan çıkmaz** (CLAUDE.md §4). "
                     "En hacimli tablo; özet çıkarıldıktan sonra eskiler silinir.",
    "gunluk_ozet": "Günlük özet — risk motorunun girdisi ve ekranların okuduğu satır.",
    "risk_sonuc": "Günlük risk anlık görüntüsü. Trend ve histerezisin kaynağı.",
    "anket_yanit": "SAS-SV yanıtları. Ham puan saklanır, bant değil (spec-anket.md §6.4).",
    "kategori_esleme": "Paket → kategori. `assets/kategori_tablosu.json`'dan seed edilir; "
                       "kullanıcı override'ı aynı tabloda `kaynak='kullanici'` ile durur.",
    "gorev_tamamlama": "Görev tamamlama geçmişi (spec-puan.md §7).",
    "puan_gunluk": "Günlük puan ve seri. Ceza yok, sadece kazanç.",
}


def kolon_notu(kolon, affinity):
    """§2 dökümündeki 'Not' hücresi — anlam notu + varsa tip daraltması."""
    parcalar = []
    if ANLAM_NOTU.get(kolon):
        parcalar.append(ANLAM_NOTU[kolon])
    if affinity == "INTEGER" and kolon in KOLON_TIPI:
        parcalar.append("hedef motorda `%s` (aralığı dar, BIGINT gereksiz)"
                        % KOLON_TIPI[kolon][0])
    return " · ".join(parcalar)


def sema_dosyasi():
    adaylar = glob.glob(os.path.join(SEMA_DIZINI, "*", "*.json"))
    if not adaylar:
        sys.exit(
            "Şema bulunamadı: %s\n"
            "Önce `./gradlew :app:assembleDebug` çalıştır." % SEMA_DIZINI
        )
    return max(adaylar, key=lambda p: int(os.path.basename(p).split(".")[0]))


def ddl_temizle(sql, tablo):
    return sql.replace("${TABLE_NAME}", tablo)


def cevir(entity, hedef):
    """SQLite entity tanımını PostgreSQL (0) veya MSSQL (1) DDL'ine çevirir."""
    tablo = entity["tableName"]
    pk = entity.get("primaryKey", {})
    pk_kolonlar = pk.get("columnNames", [])
    otomatik = pk.get("autoGenerate", False)

    # MSSQL'de NVARCHAR(MAX) indekslenemez ve birincil anahtar olamaz.
    # PK'ya veya indekse giren metin kolonları sabit uzunluğa çekilmeli.
    # 450 = SQL Server'ın indeks anahtarı için izin verdiği azami nvarchar.
    indeksli = set(pk_kolonlar)
    for ix in entity.get("indices", []):
        indeksli.update(ix["columnNames"])

    satirlar = []
    for alan in entity["fields"]:
        kolon = alan["columnName"]
        pg, ms = TIP_ESLEME.get(alan["affinity"], ("TEXT", "NVARCHAR(MAX)"))
        if alan["affinity"] == "INTEGER" and kolon in KOLON_TIPI:
            pg, ms = KOLON_TIPI[kolon]
        if alan["affinity"] == "TEXT" and kolon in indeksli:
            ms = "NVARCHAR(450)"
        tip = pg if hedef == 0 else ms

        if otomatik and kolon in pk_kolonlar:
            tip = "BIGSERIAL" if hedef == 0 else "BIGINT IDENTITY(1,1)"

        parca = '  "%s" %s' % (kolon, tip) if hedef == 0 else "  [%s] %s" % (kolon, tip)
        if alan.get("notNull"):
            parca += " NOT NULL"
        if alan.get("defaultValue") not in (None, "undefined"):
            parca += " DEFAULT %s" % alan["defaultValue"]
        satirlar.append(parca)

    if pk_kolonlar:
        if hedef == 0:
            kolonlar = ", ".join('"%s"' % k for k in pk_kolonlar)
        else:
            kolonlar = ", ".join("[%s]" % k for k in pk_kolonlar)
        satirlar.append("  PRIMARY KEY (%s)" % kolonlar)

    ad = '"%s"' % tablo if hedef == 0 else "[%s]" % tablo
    ddl = "CREATE TABLE %s (\n%s\n);" % (ad, ",\n".join(satirlar))

    for ix in entity.get("indices", []):
        if hedef == 0:
            kolonlar = ", ".join('"%s"' % k for k in ix["columnNames"])
            ddl += '\nCREATE %sINDEX "%s" ON "%s" (%s);' % (
                "UNIQUE " if ix.get("unique") else "", ix["name"], tablo, kolonlar)
        else:
            kolonlar = ", ".join("[%s]" % k for k in ix["columnNames"])
            ddl += "\nCREATE %sINDEX [%s] ON [%s] (%s);" % (
                "UNIQUE " if ix.get("unique") else "", ix["name"], tablo, kolonlar)
    return ddl


def uret():
    yol = sema_dosyasi()
    with open(yol, encoding="utf-8") as f:
        sema = json.load(f)

    db = sema["database"]
    surum = db["version"]
    entities = sorted(db["entities"], key=lambda e: e["tableName"])

    y = []
    ek = y.append

    ek("# DB.md — Yeşilİz Veritabanı")
    ek("")
    ek("> **ÜRETİLEN DOSYA — elle düzenleme.**")
    ek("> Kaynak: `%s`" % os.path.relpath(yol, KOK))
    ek("> Yeniden üretmek için:")
    ek("> ```bash")
    ek("> ./gradlew :app:assembleDebug      # Room şemayı yeniden yazar")
    ek("> python3 tools/db_dokuman_uret.py  # bu dosyayı günceller")
    ek("> ```")
    ek("")
    ek("**Şema sürümü:** %d · **Tablo sayısı:** %d · **Güncelleme:** %s"
       % (surum, len(entities), date.today().isoformat()))
    ek("")
    ek("---")
    ek("")

    # --- mahremiyet ---
    ek("## 0. Mahremiyet sözleşmesi")
    ek("")
    ek("Bu şemada **yok ve olmayacak**: mesaj içeriği, fotoğraf, rehber,")
    ek("arama geçmişi, konum/GPS. Yalnızca *hangi paket, ne kadar süre,")
    ek("hangi kategori* tutuluyor (CLAUDE.md §4).")
    ek("")
    ek("`kullanim_olay` tablosu **cihazdan çıkmaz.** Sunucuya (Faz 2)")
    ek("yalnızca `gunluk_ozet` ve `risk_sonuc` özetleri gider.")
    ek("")
    ek("---")
    ek("")

    # --- özet tablo ---
    ek("## 1. Tablolar")
    ek("")
    ek("| Tablo | Kolon | Birincil anahtar | İndeks | Amaç |")
    ek("|---|---|---|---|---|")
    for e in entities:
        pk = ", ".join(e.get("primaryKey", {}).get("columnNames", [])) or "—"
        ix = len(e.get("indices", []))
        ek("| `%s` | %d | `%s` | %d | %s |" % (
            e["tableName"], len(e["fields"]), pk, ix,
            TABLO_ACIKLAMA.get(e["tableName"], "")))
    ek("")
    ek("---")
    ek("")

    # --- kolon dökümü ---
    ek("## 2. Kolon dökümü")
    ek("")
    for e in entities:
        tablo = e["tableName"]
        ek("### `%s`" % tablo)
        ek("")
        if TABLO_ACIKLAMA.get(tablo):
            ek(TABLO_ACIKLAMA[tablo])
            ek("")
        pk_kolonlar = set(e.get("primaryKey", {}).get("columnNames", []))
        ek("| Kolon | SQLite | Null? | PK | Not |")
        ek("|---|---|---|---|---|")
        for alan in e["fields"]:
            kolon = alan["columnName"]
            ek("| `%s` | %s | %s | %s | %s |" % (
                kolon,
                alan["affinity"],
                "hayır" if alan.get("notNull") else "evet",
                "✔" if kolon in pk_kolonlar else "",
                kolon_notu(kolon, alan["affinity"])))
        ek("")
        for ix in e.get("indices", []):
            ek("- İndeks `%s` → (%s)%s" % (
                ix["name"], ", ".join(ix["columnNames"]),
                " **UNIQUE**" if ix.get("unique") else ""))
        ek("")

    ek("---")
    ek("")

    # --- DDL'ler ---
    ek("## 3. SQLite DDL — Room'un gerçekten çalıştırdığı")
    ek("")
    ek("Bu bölüm Room tarafından üretildi, elle yazılmadı.")
    ek("")
    ek("```sql")
    for e in entities:
        ek(ddl_temizle(e["createSql"], e["tableName"]))
        for ix in e.get("indices", []):
            ek(ddl_temizle(ix["createSql"], e["tableName"]))
    ek("```")
    ek("")

    ek("## 4. Tip eşleme — taşırken")
    ek("")
    ek("| SQLite (Room) | PostgreSQL | MSSQL | Not |")
    ek("|---|---|---|---|")
    ek("| `INTEGER` | `BIGINT` | `BIGINT` | varsayılan — otomatik PK'da `BIGSERIAL` / `IDENTITY(1,1)` |")
    ek("| `INTEGER` (aralığı dar) | `SMALLINT` / `INTEGER` | `SMALLINT` / `INT` | aşağıdaki liste |")
    ek("| `TEXT` | `TEXT` | `NVARCHAR(MAX)` | Türkçe karakter için MSSQL'de **NVARCHAR şart** |")
    ek("| `TEXT` (PK/indeks) | `TEXT` | `NVARCHAR(450)` | MSSQL'de `NVARCHAR(MAX)` indekslenemez |")
    ek("| `REAL` | `DOUBLE PRECISION` | `FLOAT` | risk skorları burada |")
    ek("| `BLOB` | `BYTEA` | `VARBINARY(MAX)` | şu an kullanılmıyor |")
    ek("")
    ek("### Neden varsayılan `BIGINT`")
    ek("")
    ek("SQLite'ın tek tamsayı tipi var ve o zaten 64 bit. Room'un şema JSON'unda")
    ek("Kotlin'deki `Int` ile `Long` ayrımı **görünmüyor** — ikisi de `INTEGER`")
    ek("yazıyor. Yani üretici, `madde1` (1–5) ile `baslangic` (epoch ms,")
    ek("~1.785.000.000.000) arasındaki farkı şema dosyasından okuyamıyor.")
    ek("")
    ek("`INTEGER`'ın tavanı 2.147.483.647. Epoch milisaniye bu tavanı 1970'te")
    ek("aştı; epoch **saniye** bile 2038'de aşacak. Yanlış tarafa düşmenin")
    ek("bedeli sessiz veri kaybı olduğu için varsayılan geniş tip.")
    ek("")
    ek("### Daraltılmış kolonlar")
    ek("")
    ek("Aralığı Kotlin tarafından bilinen kolonlar üreticide elle daraltıldı")
    ek("(`tools/db_dokuman_uret.py` → `KOLON_TIPI`):")
    ek("")
    ek("| Kolon | Gerçek aralık | PostgreSQL | MSSQL |")
    ek("|---|---|---|---|")
    ek("| `madde1`…`madde10` | Likert 1–5 | `SMALLINT` | `SMALLINT` |")
    ek("| `seriGun` | gün serisi | `SMALLINT` | `SMALLINT` |")
    ek("| `acilisSayisi` | günlük açılış adedi | `INTEGER` | `INT` |")
    ek("| `puan` · `puanHam` · `puanNet` | görev/gün puanı | `INTEGER` | `INT` |")
    ek("")
    ek("`BIGINT` kalanlar ve nedenleri:")
    ek("")
    ek("- `baslangic` · `bitis` — epoch milisaniye, `INTEGER`'a **sığmaz**")
    ek("- `id` (`BIGSERIAL` / `IDENTITY`) — Kotlin tarafı `Long`. Ayrıca")
    ex = ("  `kullanim_olay` özet alındıktan sonra satır siliyor; sequence geri "
          "sayılmadığı için tavanı toplam satır değil toplam INSERT belirliyor.")
    ek(ex)
    ek("")
    ek("**Dikkat edilecek üç nokta:**")
    ek("")
    ek("1. **Tarihler metin.** `tarih` kolonları ISO metin (`2026-07-28`).")
    ek("   SQLite'ın tarih tipi olmadığı için böyle. Taşırken `DATE`'e")
    ek("   çevrilebilir, sıralama davranışı aynı kalır (ISO leksikografik sıralanır).")
    ek("2. **Zaman damgaları epoch milisaniye.** `baslangic` / `bitis` `INTEGER`.")
    ek("   Taşırken `TIMESTAMPTZ` / `DATETIME2` yapılabilir.")
    ek("3. **`surukleyiciler` virgüllü liste.** İlişkisel motorda ayrı tabloya")
    ek("   veya `TEXT[]`'e çevrilmesi daha doğru olur.")
    ek("")

    for i, (ad, baslik) in enumerate([("PostgreSQL", "5"), ("MSSQL", "6")]):
        ek("## %s. %s DDL" % (baslik, ad))
        ek("")
        if ad == "PostgreSQL":
            ek("CLAUDE.md §5'teki backend hedefi. Faz 2'de bu şema kullanılacak.")
        else:
            ek("Taşıma ihtimaline karşı. Türkçe karakterler için `NVARCHAR` kullanıldı.")
        ek("")
        ek("```sql")
        for e in entities:
            ek(cevir(e, i))
            ek("")
        ek("```")
        ek("")

    ek("---")
    ek("")
    ek("## 7. Değişiklik kaydı")
    ek("")
    ek("| Sürüm | Tarih | Değişiklik |")
    ek("|---|---|---|")
    ek("| 1 | 2026-07-28 | İlk şema — 7 tablo (CLAUDE.md §11) |")
    ek("")
    ek("> Şema değiştirdiğinde `YesilizDb.version`'ı artır, bu tabloya satır ekle,")
    ek("> sonra üretici scripti çalıştır. Prototip boyunca migration yazılmıyor")
    ek("> (`fallbackToDestructiveMigration`) — **yayına çıkmadan önce mutlaka**")
    ek("> gerçek migration'lar yazılmalı, yoksa her güncellemede kullanıcı geçmişi silinir.")
    ek("")

    with open(HEDEF, "w", encoding="utf-8") as f:
        f.write("\n".join(y))

    print("Yazıldı: %s" % os.path.relpath(HEDEF, KOK))
    print("  şema sürümü %d · %d tablo · %d kolon"
          % (surum, len(entities), sum(len(e["fields"]) for e in entities)))


if __name__ == "__main__":
    uret()
