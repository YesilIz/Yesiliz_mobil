# htmx — depoya alınmış (vendored)

| | |
|---|---|
| Sürüm | 2.0.4 |
| Kaynak | `https://unpkg.com/htmx.org@2.0.4/dist/htmx.min.js` |
| SRI | `sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+` |
| Boyut | 50.917 bayt |
| Lisans | BSD Zero Clause |

**Niçin CDN değil** (`docs/panel-plani.md §5`):

1. Panel VPN / IP kısıtı arkasında koşuyor; dışa çıkışı olmayabilir.
2. CDN script'i, sağlık verisine yakın bir oturumda üçüncü taraf kod
   yürütmek demek ve `script-src 'self'` CSP'sini gevşetmeyi zorunlu kılar.
3. Yeniden üretilebilir imaj: derleme ağa bağlı olmuyor.

**Yükseltirken:** dosyayı değiştir, yukarıdaki üç satırı güncelle ve SRI'yı
yeniden hesapla:

```bash
curl -sS -o htmx.min.js https://unpkg.com/htmx.org@<surum>/dist/htmx.min.js
echo "sha384-$(openssl dgst -sha384 -binary htmx.min.js | openssl base64 -A)"
```
