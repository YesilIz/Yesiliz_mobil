# stitch-ekranlar.md — Üretilen Stitch Ekranları

> DESIGN.md §5.X ↔ Stitch ekran eşlemesi ve §7 kontrol listesi sonuçları.
> Yeni ekran üretilince veya mevcut ekran yeniden üretilince **bu dosya güncellenir** —
> aksi hâlde her oturum `list_screens` ile aynı keşfi baştan yapmak zorunda kalır.

**Son güncelleme:** 2026-07-28

---

## 1. PROJE KİMLİKLERİ

| Alan | Değer |
|---|---|
| Stitch projesi | `projects/17621803030342705738` — "Markdown Design System" |
| Tasarım sistemi | `assets/0b7f4c630ae54da29f9c5a74103ff73f` — "Hopeful Green", sürüm 1 |
| Cihaz tipi | MOBILE |
| Görünürlük | PUBLIC |

**Yeni ekran üretirken** `generate_screen_from_text` çağrısına `designSystem` olarak
`assets/0b7f4c630ae54da29f9c5a74103ff73f` verilmeli. Verilmezse Stitch varsayılan
sistemi kullanır ve 19 ekranla tutarlılık bozulur.

---

## 2. EKRAN EŞLEMESİ

DESIGN.md §5'teki **19 ekranın tamamı** üretildi.

| DESIGN.md | Stitch başlığı | Screen ID |
|---|---|---|
| §5.1 Karşılama | Hoş Geldiniz | `a55ceb4d05ee4b00b77a7647eeb86bfa` |
| §5.2 İzin açıklaması | İzin Açıklaması (Adım 2) | `0f49154c8c484b19a016ebda53930790` |
| §5.3 Yaş ve cinsiyet | Profil Kurulumu | `36a18bbf70ac4f1a8deee4fc98a1de17` |
| §5.4 Anket (SAS-SV) | Değerlendirme Anketi | `a7f9e5706d2844c0843dc8af0770c38e` |
| §5.5 İlk sonuç | İlk Sonuçlar (Güncellenmiş) | `ae06d317720048a9bc9aad3945e964c6` |
| §5.6 Hedef tanıtımı | Hedef Belirleme | `12269428088d4445b8ec42d6e1deefea` |
| **§5.7 Ana ekran ⭐** | Ana Ekran (Güncellenmiş Ağaç) | `6fc94aab2a184a52a40814cc92624295` |
| §5.8 Görev detayı | Görev Detayı | `0a4f21b5f0574adbb1403e51b8def430` |
| §5.9 Spor başlat | Aktif Yürüyüş (Düzeltilmiş) | `04b0a62c637b4830b6ba33dbd3cae9f7` |
| §5.10 Nefes egzersizi | Nefes Egzersizi | `842e8650f2954de39ada2b23dbdc9ddc` |
| §5.11 İstatistik | İstatistikler | `f8189198e0354e3682b1b4721483b1ad` |
| §5.12 Risk detayı | Risk Detayı | `d644fa0155f64da0b98dd41995d80518` |
| §5.13 Leaderboard | Lig Tablosu | `84418a1b89654cbcb2c6afe948eec8d2` |
| §5.14 Ödüller | Ödüller | `fb0dd883338d4ec995a2ced5d98c5a63` |
| §5.15 Profil ve ayarlar | Profil ve Ayarlar | `9604a16b0c5741139bbd0230468d7750` |
| **§5.16 Nudge overlay ⭐** | Mola Hatırlatıcı | `bb179cfbfd9d4bb5a40687c8e13e77bc` |
| §5.17 Widget | Widget Tasarımı | `f985002e91f443a495a99832ec4d01a4` |
| §5.18 Ebeveyn paneli | Ebeveyn Paneli | `436dba303fb1478cad9a6d7f6a147394` |
| §5.19 YEDAM onayı | YEDAM Onayı | `3ae5180014a44746806249ff9351aa96` |

### İllüstrasyonlar (§4 ağaç metaforu)

| Evre | Screen ID |
|---|---|
| 1 — Sparse (seyrek, ama canlı) | `81c58ff62e714e1da361bd16de014473` |
| 2 — Half (yarı yeşermiş) | `7c18830fb12843209698287b1d1024d7` |
| 3 — Full (dolgun taç) | `921f7fd77dea40f5803d39ff287c0219` |
| 4 — Blossom (beyaz çiçekli) | `125fb5015ac2409db895ed64c0491940` |
| 5 — Budding (tomurcuklu) | `b8595a010ea244449dd33ce68bf39e2d` |
| YEDAM — iki figür | `04f9ba709fa540fc9abaf0a1de9f2785` |

Beş evre de **şeffaf zeminli** üretildi ve aynı gövde/dal yapısını paylaşıyor —
`AgacGorseli` bileşeninde evreler arası geçiş bu sayede tutarlı olacak.

> Bunlar Android'e `res/drawable/` altına Vector Asset olarak alınacak (CLAUDE.md §5).

### Yardımcı sayfalar (ekran değil)

`4340d9390eab4bdf8e7875d0de108425` "Yeşilİz Screen Plan" ve
`13474683328662881274` "DESIGN.md" — projeye referans olarak yüklenmiş markdown
belgeleri. Ekran değiller, koda dönüşmezler.

Ayrıca `hidden: true` işaretli 10 kadar eski/deneme ekranı var (`228bece…`,
`291ecb37…`, `34684722…`, `bba56fe4…`, `a335b537…` ve 512×763'lük taslaklar).
**Bunlar geçersiz** — yukarıdaki tablodakiler güncel sürümlerdir.

---

## 3. DESIGN.md §7 KONTROL LİSTESİ — SONUÇLAR

Otomatik maddeler `tests/kontrol_stitch.py` ile 2026-07-28'de 19 ekranın tamamı
üzerinde koşturuldu. **İhlal içeren ekran: 0 / 19.**

| § 7 maddesi | Yöntem | Sonuç |
|---|---|---|
| Hiç kırmızı yok | otomatik | ✅ 19/19 — *aşağıdaki nota bak* |
| Uyarı/ünlem ikonu yok | otomatik | ✅ 19/19 |
| Teşhis dili yok | otomatik | ✅ 19/19 |
| Emir kipi yok | otomatik | ✅ 19/19 |
| Türkçe karakterler doğru | otomatik | ✅ 19/19 — mojibake yok |
| Ağaç ölü görünmüyor | **insan gözü** | ⬜ yapılmadı |
| Beyaz alan yeterli, kalabalık değil | **insan gözü** | ⬜ yapılmadı |
| Renkler token listesinden | **insan gözü** | ⬜ yapılmadı |

### Kırmızı hakkında — açık madde

`#ba1a1a` (error), `#ffdad6` (error-container) ve `#93000a` (on-error-container)
**19 ekranın tamamının** Tailwind tema bloğunda **tanımlı**. Ama:

> **Hiçbir ekran bu tokenları fiilen kullanmıyor** — `text-error`, `bg-error-container`,
> `var(--error)` gibi tek bir kullanım yok (denetleyici 0 kullanım buldu).
> Yani **şu an hiçbir ekranda kırmızı görünmüyor.**

Yine de temizlenmeli: token tanımlı olduğu sürece sonraki bir üretim veya
`edit_screens` çağrısı onu çekebilir — özellikle bir form hatası veya boş durum
eklenirse. DESIGN.md §1'e yeşil `--error` / `--error-container` değerleri yazıldı;
geriye Stitch tarafındaki tasarım sistemini güncellemek kaldı.

**Engel 1 — MCP yazamıyor.** `mcp__stitch__update_design_system` çağrısı başarıyla
dönüyor ama sonuç bir *session* kaynağı (`projects/…/sessions/…`) ve **asset'e
işlenmiyor**. 2026-07-28'de iki kez denendi, ikisinde de tasarım sistemi
değişmeden kaldı. Bu iş **Stitch arayüzünden** yapılmak zorunda.

**Engel 2 — elle düzenleme özel tokenları siliyor.** 2026-07-28'de arayüzden
düzenleme yapıldı, tasarım sistemi sürüm 1 → 3'e çıktı. Sonuç:

- `error` **hâlâ `#ba1a1a`** — kırmızı gitmedi
- Buna karşılık **7 özel token silindi**:
  `risk-low`, `risk-mid`, `risk-high`, `surface-white`, `surface-muted`,
  `text-secondary`, `divider`

Bu ikincisi daha ciddi: `risk-*` üçlüsü "kırmızı yok, koyulaşan yeşil" kuralının
**taşıyıcısı**. Mevcut 19 ekran etkilenmedi (HTML'leri render edilmiş hâlde
saklanıyor ve `text-risk-high`, `text-text-secondary` gibi sınıfları içeriyor),
ama **bundan sonra üretilecek her ekran bu renkleri bulamayacak.**

### Geri yüklenmesi gereken tokenlar

Stitch arayüzünde tasarım sistemine şunlar **geri eklenmeli**, `error` üçlüsü de
düzeltilmeli:

| Token | Değer | Neden |
|---|---|---|
| `risk-low` | `#00AE42` | AZ RİSKLİ bandı |
| `risk-mid` | `#70ab70` | ORTA RİSKLİ bandı |
| `risk-high` | `#124734` | ÇOK RİSKLİ bandı |
| `surface-white` | `#FFFFFF` | kart zemini |
| `surface-muted` | `#c4dec9` | ilerleme çubuğu boş kısmı, chip |
| `text-secondary` | `#5A6B60` | yardımcı metin |
| `divider` | `#E3EDE6` | ayraç |
| `error` | `#124734` | Material kırmızısı yerine koyu yeşil |
| `error-container` | `#c4dec9` | |
| `on-error-container` | `#124734` | |

Doğru kaynak her zaman **DESIGN.md §1**. Compose tarafı (`app/ui/tema/Renk.kt`)
zaten oradan yazıldı ve bu karmaşadan **etkilenmiyor** — sorun yalnızca Stitch'te
yeni ekran üretimini ilgilendiriyor.

Düzeltme sonrası: `python3 tests/kontrol_stitch.py <klasor>` tekrar koşturulup
bu bölüm güncellenmeli.

### Denetleyiciyi çalıştırma

```bash
# 1. list_screens çıktısındaki htmlCode.downloadUrl adreslerini bir klasöre indir
# 2. tara
python3 tests/kontrol_stitch.py /yol/ekranlar
```

Yeni ekran üretildiğinde veya mevcut ekran `edit_screens` ile değiştirildiğinde
tekrar koşturulmalı. Bulgu varsa çıkış kodu 1.

---

## 4. STITCH İŞ AKIŞI HATIRLATMALARI

CLAUDE.md §8'in özeti — ekran yazarken:

1. Stitch taslağı **kod değil, referanstır.** Yerleşim, boşluk oranı, hangi bileşenin
   nerede durduğu oradan okunur; Compose kodu CLAUDE.md §7'e göre elle yazılır.
2. Ana ekran (§5.7) tasarım dilini belirledi — yeni prompt'lara
   *"Match the visual style of the previously generated home screen"* eklenmeli.
3. Widget (§5.17) ve nudge overlay (§5.16) sistem bileşenleri; Stitch bunları
   yaklaşık üretti. Gerçek implementasyon Glance/Compose ile yazılacak, taslak
   sadece hizalama/boyut referansı.
