# Dağıtım — backend ve Android

> Görev 1'in dokümanı. Kaynak: `gorev1.md`, `CLAUDE.md` §5 ve §12 K18/K24.
>
> **Buraya gerçek parola ya da secret yazılmaz.** Değişkenlerin adları var,
> değerleri yok; değerler sağlayıcı panelinde ve `.env`'de (gitignore'da).

---

## 1. Ortam değişkenleri

Her ayar iki adla okunuyor: `YESILIZ_` önekli Türkçe ad ve standart ad.
Önekli olan kazanıyor. Standart adların desteklenmesi zorunlu, çünkü
sağlayıcı bazılarını (`DATABASE_URL`, `PORT`) kendisi enjekte ediyor ve
adları değiştirilemiyor.

| Standart ad | Önekli ad | Zorunlu mu | Ne işe yarıyor |
|---|---|---|---|
| `ENVIRONMENT` | `YESILIZ_ORTAM` | üretimde **evet** | `development` · `test` · `production` |
| `DATABASE_URL` | `YESILIZ_VERITABANI_URL` | üretimde **evet** | PostgreSQL adresi. Railway otomatik enjekte ediyor |
| `JWT_SECRET` | `YESILIZ_JWT_ANAHTARI` | üretimde **evet** | Jeton imza anahtarı, ≥ 32 karakter |
| `ALLOWED_ORIGINS` | `YESILIZ_IZINLI_KAYNAKLAR` | hayır | Virgülle ayrılmış CORS listesi (web paneli için) |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | `YESILIZ_ERISIM_DAKIKA` | hayır | Erişim jetonu ömrü, varsayılan 30 |
| `PORT` | — | hayır | Sağlayıcı enjekte ediyor; yerelde 8000 |

Örnek dosya: `backend/.env.ornek`. Anahtar üretmek için:

```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Üretim açılış kapısı

`ENVIRONMENT=production` iken `backend/app/ayarlar.py` açılışta doğruluyor
ve şu üç durumda **uygulama başlamıyor**:

- JWT anahtarı depodaki varsayılan değerde,
- JWT anahtarı 32 karakterden kısa,
- `DATABASE_URL` bir SQLite adresi.

Uyarı basıp devam etmiyor: uyarı sağlayıcı log akışında kaybolur ve sunucu
haftalarca varsayılan anahtarla çalışır. Hata mesajı anahtarın kendisini
**yazmıyor** — hata mesajı da bir log.

---

## 2. Railway'e dağıtım

Backend `backend/` klasörünün tamamı; `Dockerfile` ve `railway.json` orada.

1. Railway'de yeni proje → **Deploy from GitHub repo** → `Yesiliz_mobil`.
2. Servis ayarlarında **Root Directory** = `backend`.
3. Aynı projeye **New → Database → PostgreSQL** ekleyin.
4. **API servisinin** (Postgres'in değil) Variables sekmesi:
   ```
   ENVIRONMENT  = production
   JWT_SECRET   = <token_urlsafe(32) çıktısı>
   DATABASE_URL = ${{Postgres.DATABASE_URL}}
   PORT         = 8000
   ```
   `DATABASE_URL` **otomatik enjekte olmuyor** — Railway'in referans
   değişkeni süslü parantezlerle birebir böyle yazılmalı. Veritabanı
   servisinin kendi değişken listesindeki `DATABASE_URL` başka bir
   şeydir, oraya dokunulmaz.
5. Servis → Settings → Networking → **Generate Domain**, hedef port
   **8000**. Railway `https://<ad>.up.railway.app` veriyor ve sertifikayı
   kendisi yönetiyor. Hedef port `baslat.sh`'ın dinlediği portla aynı
   olmalı; `PORT` açıkça verildiği için ikisi de 8000.
6. `railway.json` sağlık kontrolünü `/health`'e bağlıyor; ilk dağıtım
   yeşile dönünce hazır.

> **Dağıtılan dal `main`.** Railway'in GitHub entegrasyonu varsayılan dalı
> izliyor ve CI'daki dağıtım işi de yalnızca `main`'de koşuyor
> (`backend.yml`). `dev`'e atılan bir değişiklik canlıya çıkmaz. 5 Ağu
> 2026'da tam olarak bu yaşandı: ayarların hepsi doğruydu ama `/health`
> 404 dönüyordu, çünkü `main` 18 commit geride ve o uç orada yoktu.
> Teşhis: eski `/saglik` 200, yeni `/health` 404 → sorun sunucuda değil,
> dağıtılan sürümde.

`postgresql://` şeması psycopg3'e otomatik çevriliyor (`ayarlar.py`) —
çevrilmezse kap açılışta `ModuleNotFoundError: psycopg2` ile ölürdü.

### Migration

Elle çalıştırmaya gerek yok: `backend/baslat.sh` kap açılırken

```bash
alembic upgrade head
```

koşuyor ve `set -e` yüzünden düşerse uvicorn hiç başlamıyor. Sağlık
kontrolü de düşüyor, yani **migration başarısızsa yeni sürüm devreye
alınmıyor**. Uygulama kodu ile şema farklı sürümlerde kalamıyor.

Durumu görmek için:

```bash
railway run alembic current    # ya da sağlayıcı kabuğundan
```

---

## 3. Sağlık kontrolü

```http
GET https://<alan-adi>/health
```

```json
{ "status": "healthy", "database": "connected", "version": "0.1.0" }
```

Veritabanına ulaşılamazsa `503` ve `"database": "unreachable"`. Uç
kimlik doğrulaması olmadan açık ve **hiçbir gizli bilgi döndürmüyor**;
bağlantı hatasının gerekçesi bilerek loglanmıyor, çünkü SQLAlchemy
istisnaları URL'i parolasıyla taşıyabiliyor.

`/saglik` ayrı duruyor ve kalacak: kurulu APK'lar onu çağırıyor
(`data/uzak/YesilizApi.kt`). Yeni iş `/health`'e gitmeli.

---

## 4. Testler

```bash
# birim (76) — SQLite bellek DB, ağ istemiyor
cd backend && python -m pytest -q

# yerel yığın
docker compose up -d --build
curl -s localhost:8000/health

# canlı ortam smoke testleri
YESILIZ_SMOKE_TABAN=https://<alan-adi> python -m pytest smoke -q
```

Smoke testleri (`backend/smoke/test_duman.py`) `gorev1.md §9`'un adım
listesini izliyor: `/health` → benzersiz test kullanıcısı → giriş →
korumalı uç (jetonsuz 401 dâhil) → `POST /ozet` → `GET /lig` →
`DELETE /auth/hesap` ile temizlik. Temizlik `finally` içinde, test düşse
bile koşuyor; silinen hesapla girişin 401 döndüğü ayrıca doğrulanıyor.

`pytest.ini` `testpaths = tests` diyor, yani normal `pytest -q` smoke'u
toplamıyor. Şifresiz adrese karşı denemek için `YESILIZ_SMOKE_HTTP_TAMAM=1`.

---

## 5. Android — adres nereden yönetiliyor

**Tek yer:** `app/build.gradle.kts` içindeki `canliSunucu`.

```bash
# varsayılanı ezmek
./gradlew :app:assembleRelease -Pyesiliz.sunucu=https://api.ornek.com
# ya da
YESILIZ_SUNUCU=https://api.ornek.com ./gradlew :app:assembleRelease
```

Oradan `BuildConfig.API_TABANI` üretiliyor:

| Build türü | Adres |
|---|---|
| debug | `http://10.0.2.2:8000/` — emülatör; gerçek cihazda Profil › Geliştirici'den LAN adresi |
| release | `canliSunucu` (HTTPS) |

`Tercihler.VARSAYILAN_SUNUCU` bunu okuyor, `AgModulu` da ondan alıyor.
Kullanıcı adresi Profil › Geliştirici'den değiştirdiğinde DataStore'daki
değer öne geçiyor — o ekran yalnızca debug'da görünüyor.

Üç koruma birlikte çalışıyor:

- `assembleRelease`/`bundleRelease` HTTPS olmayan adresle **düşüyor**
  (`app/build.gradle.kts`).
- `app/src/main/res/xml/ag_guvenligi.xml` şifresiz trafiği istisnasız
  reddediyor; debug kaynak seti kendi gevşek yapılandırmasını getiriyor.
- `.github/workflows/android-ag-guvenligi.yml` ikisinin de geri
  alınmadığını her PR'da denetliyor.

### Release APK doğrulaması

Sürüm çıkarken bir kez:

```bash
./gradlew :app:assembleRelease
cd /tmp && rm -rf apk && mkdir apk && cd apk
unzip -qo <yol>/app-release-unsigned.apk "*.dex"
cat *.dex | strings -a | grep "10\.0\.2\.2"          # çıktı olmamalı
cat *.dex | strings -a | grep -oE "http://[a-z0-9.:/-]+" | sort -u
```

İkinci komutun beklenen çıktısı yalnızca kütüphane sabitleri
(`http://localhost/`, `schemas.android.com`, `w3.org/2000/svg`).

---

## 6. Sahte veri: debug'da var, release'de yok

`YedekliLigDeposu`/`YedekliHesapDeposu` ağ hatasında `YerelSahte*`
depolara düşüyor (CLAUDE.md §12 K18). Bu davranış **yalnızca debug**:

| Kaynak seti | Bağlanan | Sunucuya ulaşılamazsa |
|---|---|---|
| `src/debug/.../di/DepoBaglama.kt` | `Yedekli*Deposu` | Yerel örnek sıralama + "örnek veri" notu |
| `src/release/.../di/DepoBaglama.kt` | `Sunucu*Deposu` | Boş lig + "sunucuya ulaşılamıyor" notu; girişte alan hatası |

Yayınlanan sürümde `YerelSahte*` hiç örneklenmiyor. Hata yutulmuyor,
indirgeniyor: boş sonuç + `cevrimdisi` bayrağı + logcat izi
(`data/depo/SunucuDepolar.kt`).

---

## 7. CI/CD

`.github/workflows/backend.yml`: test → üretim güvenlik kapısı → Docker
imajı → (yalnızca `main`) Railway dağıtımı → `/health` bekleme → smoke.
Herhangi bir adım düşerse sonraki koşmuyor.

Depo ayarları (Settings › Secrets and variables › Actions):

| Tür | Ad | Değer |
|---|---|---|
| secret | `RAILWAY_TOKEN` | Railway proje jetonu |
| variable | `RAILWAY_SERVICE` | Railway'deki servis adı |
| variable | `CANLI_TABAN` | `https://<alan-adi>` |

Dağıtım işi `environment: production` kullanıyor; jeton yalnızca oradan
okunabiliyor.

> Railway'in kendi GitHub entegrasyonu da otomatik dağıtım yapabilir ama
> testlerden **bağımsız** çalışır — push eder etmez dağıtır. Kapıyı
> koruyan şey CI'dan `railway up` çağırmak; entegrasyonun otomatik
> dağıtımı açıksa kapatın, yoksa iki hat birbiriyle yarışır.

---

## 8. Loglar

Her istek tek bir JSON satırı: `istek_id`, `yol`, `metot`, `durum`,
`sure_ms`, hata varsa `hata_turu`. Yanıt `X-Istek-Id` başlığı taşıyor;
gelen istekte varsa korunuyor, böylece bir istek baştan sona izlenebiliyor.

**Loglanmayanlar:** başlıklar (yani `Authorization`/JWT), gövde (yani
parolalar ve günlük özet verisi), sorgu dizesi. Buna ek olarak hata
metinlerinde JWT anahtarı ve veritabanı parolası aranıp `***` ile
değiştiriliyor (`backend/app/gunluk.py`).

---

## 9. Sık karşılaşılanlar

| Belirti | Bakılacak yer |
|---|---|
| Kap açılışta `ModuleNotFoundError: psycopg2` | `DATABASE_URL` şeması; `ayarlar.py` çevirisi devrede mi |
| Açılışta `YapilandirmaHatasi` | Mesaj hangi maddeyi söylüyor — anahtar mı, SQLite mi |
| `/health` 503 | Veritabanı servisi ayakta mı, `DATABASE_URL` doğru projeye mi bakıyor |
| Migration `InterpolationSyntaxError` | Parolada `%` var; `alembic/env.py` iki katına çıkarıyor, güncel mi |
| Dağıtım yeşil ama uygulama bağlanmıyor | `BuildConfig.API_TABANI` doğru alan adı mı (`-Pyesiliz.sunucu`) |
| Telefonda "Sunucuya ulaşılamıyor" | Debug APK'da Profil › Geliştirici › Sunucu adresi; release'de alan adı ve sertifika |
| Smoke testi 422 veriyor | Test e-postası `example.com` olmalı; `.test`/`.invalid` reddediliyor |
| Lig ekranında "örnek veri" notu | Debug APK; sunucuya ulaşılamıyor demektir (release'de bu not çıkmaz) |
