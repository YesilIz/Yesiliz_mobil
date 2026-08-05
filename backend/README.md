# Yeşilİz Backend

FastAPI + PostgreSQL. CLAUDE.md §5'teki Faz 2 sunucusu.

## Çalıştırma

```bash
cd backend
docker compose up -d          # PostgreSQL 16 + API (localhost:8000)
curl localhost:8000/saglik    # {"durum":"ayakta"}
```

API dokümanı: <http://localhost:8000/docs>

Android emülatöründen sunucuya **`http://10.0.2.2:8000`** ile ulaşılıyor;
emülatörde `localhost` cihazın kendisi demek.

## Test

```bash
docker compose exec api pytest -q     # 82 test
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

## Henüz yok

- **E-posta gönderimi** — şifre sıfırlama jetonu üretiliyor ve günlüğe
  yazılıyor; SMTP bağlanınca `uclar/hesap.py`'nin son satırı değişecek
- **Google OAuth** — kapsam dışı bırakıldı
- **Uzman paneli arayüzü** — `kullanici.rol` alanı hazır, arayüz Faz 3
- **HTTPS** — yerel geliştirmede şifresiz; dağıtımda TLS zorunlu
  (Android tarafında `ag_guvenligi.xml` şifresiz trafiği yalnızca
  emülatörün `10.0.2.2` adresi için açıyor)
