# Yeşilİz Backend

FastAPI + PostgreSQL. CLAUDE.md §5'teki Faz 2 sunucusu.

## Çalıştırma

```bash
cd backend
docker compose up -d          # PostgreSQL 16 + API (:8000) + panel (:8001)
curl localhost:8000/health    # {"status":"healthy","database":"connected","version":"0.1.0"}
curl localhost:8000/saglik    # Android uyumluluk ucu: {"durum":"ayakta"}
curl localhost:8001/health    # panel servisi
```

API dokümanı: <http://localhost:8000/docs>

Android emülatöründen sunucuya **`http://10.0.2.2:8000`** ile ulaşılıyor;
emülatörde `localhost` cihazın kendisi demek.

### Panel

`api` ve `panel` **aynı imajı** koşuyor; farkları `YESILIZ_ROL`
(`docs/panel-plani.md §4`, CLAUDE.md §12 K57). Panel uçları API alan adında
**hiç mount edilmiyor** — yanlış yazılmış bir yetki kontrolü bile internete
açık olmuyor, yol 404 dönüyor.

Panelde kayıt formu yok: hesap açmak bir yetki işlemi. İlk hesap komutla,
sonrakiler panelden `admin` rolüyle açılıyor.

```bash
docker compose exec panel python -m app.personel_ekle \
  --eposta admin@yesilay.org.tr --ad "Ad Soyad" --rol admin
```

Komut TOTP sırrını ve kurtarma kodlarını **bir kez** yazdırıyor; veritabanında
yalnızca şifreli sır (Fernet) ve kod özetleri duruyor. Sonra
<http://localhost:8001> → parola → doğrulayıcı kodu.

Roller: `admin` · `operasyon` · `yedam_yonetici` · `yedam_danisman` ·
`gozlemci` (`docs/panel-plani.md §2`).

## Test

```bash
docker compose exec api pytest -q
```

Testler SQLite bellek veritabanında koşuyor — saniyeler sürüyor ve
PostgreSQL kabına bağımlı değil. Şema aynı SQLAlchemy modellerinden
üretiliyor.

Canlı ortama karşı koşan smoke testleri `smoke/` altında ve `pytest.ini`
onları normal koşumdan ayırıyor:

```bash
YESILIZ_SMOKE_TABAN=https://<alan-adi> python -m pytest smoke -q
```

Dağıtım, ortam değişkenleri ve CI/CD: `docs/dagitim.md`. Örnek ortam
dosyası `.env.ornek`.

## Şema değişikliği

```bash
docker compose exec api alembic revision --autogenerate -m "aciklama"
docker compose exec api alembic upgrade head
```

## MAHREMİYET SÖZLEŞMESİ

Bu şemada **`kullanim_olay` yok ve olmayacak.** Hangi uygulamanın ne zaman
açıldığı, oturum başlangıç/bitişleri, uygulama adları sunucuya **hiç**
gelmiyor. Mesaj, fotoğraf, rehber, arama geçmişi, konum zaten hiçbir yerde
toplanmıyor (CLAUDE.md §4).

Sunucuya çıkan tek davranış verisi:

| Tablo | İçerik |
|---|---|
| `gunluk_ozet` | kategori dakikaları + örüntü değerleri + `S` |
| `risk_sonuc` | skor, bant, trend |

İkisi de **cihazda hesaplanıyor**; sunucu yalnızca saklıyor. Gönderim
kullanıcının "Yeşilay ile paylaşım" onayına bağlı ve varsayılan kapalı.

**Sunucu skor hesaplamıyor** (CLAUDE.md §12 K17). Risk motorunun tek bir
uygulaması var (`core-risk`) ve referans Python ile birebir doğrulanmış;
ikinci bir uygulama o doğrulamayı bozardı.

## Ebeveyn özeti

`/ebeveyn/ozet` ebeveyne **ham veri değil özet** döndürüyor
(`docs/spec-veri.md`): günlük toplam ekran süresi, bant, trend. Uygulama
adı, oturum dökümü, saat düzeyinde ayrıntı **yok**. Bu uç bilerek dar;
genişletmek veri politikasını değiştirmek demek.

## Yapılandırma

Ortam değişkenleri `YESILIZ_` önekli (`app/ayarlar.py`):

| Değişken | Varsayılan | Not |
|---|---|---|
| `YESILIZ_ORTAM` | `development` | `development` / `test` / `production`. `production`'da varsayılan JWT anahtarı veya SQLite ile başlamak **hata verir** |
| `YESILIZ_VERITABANI_URL` | SQLite | docker-compose PostgreSQL veriyor; production'da PostgreSQL zorunlu |
| `YESILIZ_JWT_ANAHTARI` | geliştirme anahtarı | **Üretimde mutlaka değiştir**, depoya girmemeli |
| `YESILIZ_IZINLI_KAYNAKLAR` | boş (hiçbir origin'e izin yok) | CORS — virgülle ayrılmış origin listesi. Android istemcisi Origin göndermez, bu yalnızca tarayıcı tabanlı istemcileri ilgilendirir |
| `YESILIZ_ERISIM_DAKIKA` | 30 | erişim jetonu ömrü |
| `YESILIZ_YENILEME_GUN` | 30 | yenileme jetonu ömrü |
| `YESILIZ_ESLESTIRME_KODU_DAKIKA` | 10 | tasarım 21'deki metinle birebir |
| `YESILIZ_SAAT_DILIMI` | `Europe/Istanbul` | haftalık lig ve ebeveyn özeti için IANA iş saat dilimi; anlık kayıtlar yine UTC |

## Panel ve mahremiyet

Panel tabloları (`personel`, `personel_oturum`, `denetim_kaydi`, …) yukarıdaki
sözleşmeyi **değiştirmiyor**: personel kimliği ve denetim izi taşıyorlar,
davranış verisi değil. Panel bir kullanıcının davranış verisini varsayılan
olarak göstermiyor (`docs/panel-plani.md` P9); tekil veri yalnızca KVKK
talebi işlemi olarak, gerekçeyle ve denetim kaydıyla açılıyor.

Her personel yazma eylemi `denetim_kaydi`'na **eylemle aynı transaction'da**
yazılıyor, yani kaydı olmayan bir eylem mümkün değil. Tablo salt eklenebilir:
PostgreSQL'de bir tetikleyici `UPDATE` ve `DELETE`'i reddediyor, `psql`'den
bile.

## Henüz yok

- **E-posta gönderimi** — sağlayıcı bağlanana kadar üretim ucu açıkça 503
  döndürüyor. Geliştirme/test jetonu yalnızca veritabanında tutulur; sırlar
  hiçbir ortamda günlüğe yazılmaz.
- **Google OAuth** — kapsam dışı bırakıldı
- **Panel ekranları** — kimlik, yetki ve denetim iskeleti hazır (F1);
  gösterge metrikleri ve hesap yönetimi F2'de, YEDAM başvuru akışı F3'te.
  Boş ekranlar bunu **yazıyor**, uydurma sayı göstermiyor (CLAUDE.md §4).
  `kullanici.rol` kolonu panelde **kullanılmıyor** — personel ayrı tabloda
  (K57)
- **HTTPS** — yerel geliştirmede şifresiz; dağıtımda TLS zorunlu
  (Android tarafında `ag_guvenligi.xml` şifresiz trafiği yalnızca
  emülatörün `10.0.2.2` adresi için açıyor)
