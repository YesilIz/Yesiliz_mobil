# Yeşilİz — 5. Tarama Yapılacaklar Listesi

> **TAMAMLANDI — 18 Ağustos 2026.** Bütün kalemler `fix/tarama-05` dalında
> uygulandı (5 commit); sonuç `CLAUDE.md` §12 K55'te. Kapsam dışı bırakılanlar
> aşağıdaki tabloda, gerekçeleriyle duruyor. Bu dosya artık **tarihsel kayıt**.
>
> Kaynak: `docs/arsiv/tarama-05-gereksizlik.md` (37 bulgu, 17 Ağu 2026).
> Bu liste **kararları verilmiş** iş kalemleridir. Rapor tespit, bu dosya plan.
> Taban: `dev` @ `85d498c`. İş `fix/tarama-05` dalında yapılır, PR ile `dev`'e gider.

## Verilen kararlar (özet)

| Konu | Karar |
|---|---|
| "Örnek kişiler" bölümü + "Kişilerden bul" düğmesi | **Tamamen kaldır** — `LigDeposu.onerilenler()` dahil |
| `LIG` / `DEGERLENDIRME` bildirim türleri | **Sil**, boş durum metnini düzelt |
| `dev`'de nudge eşiği (15 sn) | **Kalsın** — K50 aynen duruyor, itiraz reddedildi |
| Kapanmış tarama kayıtları | **`docs/arsiv/`** altında toplansın (Stitch belgeleri dahil) |
| Süre biçimleyicileri | **Tek biçime in** (`Bicim.sureMetni`) — ebeveyn paneli görünümü değişecek |
| `gorev*.md` referansları | **K maddelerine çevir**, çıkmaz sokak kalmasın |
| Yetim betikler | `kontrol_baslik.py` **sil**, `tasarim_ekran_kes.py` **belgele** |
| Hedef ekranı | −/+ şeridi değeri yazmasın; kayıtlı hedef çipine "kayıtlı" etiketi |
| Faktör etiketleri | Hepsi **tek kelime** (Düşük / Orta / Yüksek / Veri yok) |
| Mola hatırlatıcı anahtarı | **Hedef ekranına taşı**, Gizlilik bölümünden çıkar |
| Yapısal + varlık işleri | Dördü de yapılacak (görseller, TemaOnizleme, YEDAM bileşeni, ProfilEkran) |
| Dokunulmayacak | G5-031 (adım kaynağı metni), G5-036 (K50), G5-023 (sahte depo katmanı) |

---

## Grup 1 — Yalan ve kırık yorumlar

> Davranış değişmez, tek başına commit'lenebilir, testler etkilenmez.

- [x] **G5-002** `app/build.gradle.kts:304-306` — "ViewModel'ler henüz Hilt'siz" yorumu silinir.
      Kodda 27 `@HiltViewModel`, 60 `hiltViewModel()`, 0 çıplak `viewModel()`.
- [x] **G5-003** `data/depo/RiskDeposu.kt:149-154` — `dEfektif` KDoc'undan ", şimdilik 0" atılır;
      `GecmisVeriDeposu`'nun alanı doldurduğu yazılır.
- [x] **G5-014** `res/values/colors.xml:4` ve `ui/tema/Tema.kt:57` — "CLAUDE.md §14" → **§7**.
- [x] **G5-015** `data/depo/VeriYonetimiDeposu.kt:22` — "Room (12 tablo)" → **"Room (tüm tablolar)"**
      (sayı yazılmasın; bir sonraki entity'de yine eskimesin).
- [x] **G5-022** `ui/tema/Boyut.kt:89` — `Olcu.GrafikEkseni`'nin ölü birinci KDoc bloğu silinir.
- [x] **G5-013a** `res/values/colors.xml` başındaki yorum gerçeğe çekilir (kalan iki renk sayılır).
- [x] **G5-023-KDoc** `YerelSahteLigDeposu` KDoc'undaki "Faz 2'de … gelecek" gelecek zamanı bugüne
      çevrilir (backend 5 Ağu'dan beri canlı). **Sınıfın kendisi kalıyor** — K18 hâlâ geçerli.
- [x] **G5-001** `gorev1.md` / `gorev2.md` / `kubragorev.md` → **35 referans**, 16 kaynak dosyası +
      `CLAUDE.md` (5 satır) + `app/build.gradle.kts:85` + `tools/kontrol_baslik.py:4`.
      Karşılığı K30/K31/K32/K33 olanlar oraya yönlendirilir; olmayanlarda gerekçe yorumun içine yazılır.
      Dosyalar: `RiskHesapla.kt:68` · `Sabitler.kt:148,162` · `AktiviteTekillestir.kt:8,26,37` ·
      `SaglikOkuyucu.kt:134,163` · `Gecisler.kt:150` · `VeriModulu.kt:106` ·
      `GecisTest.kt:179,196,228,356` · `build.gradle.kts:85`.

---

## Grup 2 — Ölü kod ve kaynak silme

> Hepsi 0 kullanım (rapor `rg` ile ölçtü). Silmeden önce her biri için tek bir doğrulama araması yapılır.

- [x] **G5-010** `ui/tema/` beş ölü token: `Bosluk.Xxl` · `Olcu.GirdiKenarligi` · `Olcu.GorevSeridi` ·
      `Golge.BasiliOlcek` · `Gradyan.Baslik` (aynı rengi iki kez alan sahte gradyan). ~14 satır.
- [x] **G5-011** `ui/Bicim.kt:48-49` — `Int.kisaSaat()` silinir; `eksenSuresi` KDoc'undaki
      `[kisaSaat]` bağlantısı düz metne çevrilir (K49 halefini zaten gerekçelendiriyor).
- [x] **G5-016** Altı ölü üye:
      `GunlukOzetDao.sonGunlerAkisi` (`sonGunler` ile birebir aynı SQL) · `ArkadasDao.sayiAkisi` ·
      `BeyanDeposu.uykuAkisi` · `JetonDeposu.erisimAkisi` · `WidgetEylemleri.widgetSayisi`
      (KDoc'u "testler için" diyor, hiçbir test çağırmıyor) · `ProfilAvatarSecenegi.ikonRenk`.
- [x] **G5-017** `nudge/NudgeDurum.kt` — `NudgeDurum` data class'ı silinir, dosya **`NudgeAsama.kt`**
      olur. `CLAUDE.md §6` ağacındaki dosya adı güncellenir.
- [x] **G5-018** `ui/gezinme/Rota.kt:113` — `Rota.HESAP` (0 kayıt / 0 gezinme) silinir.
- [x] **G5-013b** `res/values/colors.xml` — `yesil_ana`, `yesil_koyu`, `beyaz` silinir
      (`yesil_50` themes.xml'de ×6, `logo_zemini` ic_launcher.xml'de ×1 kalır).
- [x] **G5-004** `BildirimDeposu.ligSirasi` + `degerlendirmeYenilenebilir` fonksiyonları ve
      `BildirimTuru.LIG` + `DEGERLENDIRME` enum değerleri silinir.
      `BildirimEkran.kt:122-124` boş durum metninin lig kolu kaldırılır →
      *"Görev tamamladıkça ve hedefini tutturdukça buraya düşecek."*
      **Ön koşul:** cihaz DB'sinde `tur IN ('lig','degerlendirme')` satırı var mı diye bakılır
      (`BildirimTuru.anahtardan()` eski satırları okuyor). Varsa `EBEVEYN` deseni uygulanır.
- [x] **G5-021** `tools/kontrol_baslik.py` silinir. `tools/tasarim_ekran_kes.py` **kalır** ve
      `CLAUDE.md §1` üreticiler tablosuna satır olarak eklenir.

---

## Grup 3 — Çift/üçlü uygulamaları birleştir

- [x] **G5-006** Üç `sureMetni` → tek `Bicim.sureMetni()`.
      Silinecek: `BildirimDeposu.kt:124`, `EbeveynViewModel.kt:171`.
      **Kullanıcıya görünen değişiklik:** ebeveyn paneli `0s 45dk` → `45 dk`, bildirim `45dk` → `45 dk`.
      `HedefEkran.uzunSure()` bu maddeye **dahil değil** (bilinçli uzun biçim) — ama G5-028'de ele alınıyor.
- [x] **G5-007** Üç `mm:ss` biçimleyici → `Bicim.kt`'e tek fonksiyon.
      `OdakHesapla.kt:75-76` · `YuruyusHesapla.kt:50-51` (ikisi birebir aynı ifade) ·
      `NefesEkran.kt:348-352` (aynı hesap + `" kaldı"`). Testler tek yerde birleşir.
- [x] **G5-008** `RiskDetayViewModel.kt:124-128` private `bantKisaEtiket` silinir;
      var olan public `Bant.kisaEtiket()` çağrılır.
      *Ek not:* `kisaEtiket()`'in evi `ui/bilesen/RiskGirisKarti.kt` — daha uygun bir yere
      (`ui/tema/` ya da `ui/BantMetin.kt`) taşınması bu turda opsiyonel.
- [x] **G5-009** YEDAM 115 bilgisi tek kaynağa iner:
      `domain/yedam/Yedam.kt` (numara + çalışma saatleri sabitleri) + `ui/bilesen/YedamKarti.kt`
      (metin + `ACTION_DIAL` eylemi). Besleyeceği ekranlar: `ProfilEkran.kt:1327+`,
      `YedamOnayEkran.kt:109+`, `EbeveynEkran.kt:726+`.
      **Yalnızca metin + eylem paylaşılır, yerleşim paylaşılmaz** (üç ekranın düzeni farklı).
      `assets/bilgi_bankasi.json:196` içindeki metin ayrı kalır (içerik yazısı), ama saat
      değişirse orası da güncellenecek diye üretici tabloya not düşülür.

---

## Grup 4 — Ürün kararı ve UI

- [x] **G5-005 + G5-035 + G5-037** "Örnek kişiler" **tamamen kaldırılır**. Dokunulan yüzey:
      - `ui/ekran/lig/ArkadasEkleEkran.kt:126-160` — "Kişilerden bul" devre dışı düğmesi,
        açıklama paragrafı, "ÖRNEK KİŞİLER" başlığı, ikinci açıklama, `OnerilenSatiri`
      - `domain/…/LigDeposu.onerilenler()` arayüz metodu
      - `RetrofitLigDeposu.kt:91` (`= emptyList()`), `YerelSahteLigDeposu.kt:157-170` (üç sabit isim),
        `YedekliLigDeposu` / `SunucuLigDeposu` delegasyonları
      - `OnerilenKisi` data class'ı, `LigDurum.onerilenler` alanı, `LigViewModel.davetGonder` kullanımı
      **Ekranda kalan:** davet kodu üret / kopyala / paylaş + kod girme.
      Rehber yasağının **gerekçesi** tek satır olarak kalır (K14 duruşu kaybolmasın).
      ~110 satır, 7 dosya. K7'ye not: arayüz sunucunun karşılayabildiği yüzeye eşitleniyor.
- [x] **G5-029** `HedefEkran.kt:185` — "Değişiklikler yarından itibaren geçerli olur." şeridi
      **mola eşiği için yanlış** (K53: servis eşiği her poll turunda okuyor).
      Şerit günlük hedef bloğunun içine alınır; mola bloğuna gerekiyorsa "Hemen geçerli olur." konur.
- [x] **G5-028** Hedef ekranı üç tekrar → iki: **−/+ şeridi değeri yazmaz**, yalnızca kontrol olur;
      değeri halka söyler. Kayıtlı hedef çipine **"kayıtlı"** ayırt edici etiketi eklenir
      (çip fazlalık değil — `HedefSinir.hizliSecim` geri dönüş yolu, tasarım 34).
      G5-006 birleşince biçim çatışması da kendiliğinden biter.
- [x] **G5-034** Mola hatırlatıcı **açma anahtarı** `ProfilEkran` GİZLİLİK bölümünden alınıp
      `HedefEkran`'ın "MOLA HATIRLATICI" bölümüne konur. Profil'deki satır kalkar ya da kısayola döner.
      "Şu anda kapalı, Profil › Gizlilik'ten aç" uyarı metni gereksizleşir → silinir.
      *Gerekçe:* mola hatırlatıcı bir rıza kapısı değil, bildirim tercihi; yanındaki iki satırın
      (Yeşilay paylaşımı, ligde görün) ağırlığını bulandırıyordu.
- [x] **G5-030** `LigEkran` özet şeridi: `dusen == 0` ve `yukselen == 0` iken çip çizilmesin.
- [x] **G5-032** `AnaEkran` başlık şeridi: `seri == 0` iken 🔥 çipi çizilmesin
      (K40'a göre 0 çoğu zaman "henüz veri yok" demek).
- [x] **G5-033** `IstatistikEkran` boş-gün açıklaması tek cümleye iner:
      *"Son 7 günün 1'i ölçüldü; ortalama yalnızca o günden."* — dürüstlük korunur (K30/K39),
      grafiğin zaten gösterdiği üçüncü cümle düşer.
- [x] **G5-027** `core-risk/EtkileyenFaktorler.kt:13-32` — `DUSUK("Düşük risk")` → `DUSUK("Düşük")`.
      Dördü de tek kelime: Düşük / Orta / Yüksek / Veri yok.
      **`core-risk` dokunuşu:** test vektörleri etiket okuyor mu diye `tests/test_vectors.json`
      kontrol edilir; okuyorsa vektör üreticisiyle birlikte güncellenir.

---

## Grup 5 — Yapısal ve varlık

- [x] **G5-012** `ui/tema/TemaOnizleme.kt` (243 satır) → **`app/src/debug/`**.
      `AdimTaniAlicisi` / `SaglikTaniAlicisi` / `DemoVeriModulu` ile aynı kurulmuş desen.
      APK boyutu değişmez (R8 zaten atıyor); kazanç release kaynak ağacının temizliği.
- [x] **G5-019** `res/drawable-nodpi/yedam_iki_figur.png` (770 KB, 0 referans) →
      `docs/tasarim/` altına taşınır (silinmez — teslim edilen tasarımın parçası).
- [x] **G5-020** `res/drawable-nodpi/giris_guvenlik_banner.png` 2469×951 / 2,5 MB →
      **1080 px genişliğe** indirilir. `drawable-nodpi` olduğu için bellekte
      9,4 MB bitmap yükleniyor; ~1,8 MB'a iner. Görsel fark yok (ekranda ~360dp çiziliyor).
- [x] **G5-024** `ProfilEkran.kt` (1.616 satır) — **K11 bozulmadan** yalnızca saf içerik alt
      sayfaları ayrılır: YEDAM hakkında, Yardım, Hakkında. Üçü de `ProfilViewModel`'e dokunmuyor,
      ayrı ViewModel açılmıyor. Hedef: `ui/ekran/profil/icerik/`.
      Veri taşıyan alt sayfalar (Profili Düzenle, Verilerimi sil, Sunucu adresi, Bildirim saatleri…)
      **yerinde kalır** — K11 onları açıkça ekran dosyasında tutuyor.

---

## Grup 6 — Doküman

- [x] **§5.1a** `CLAUDE.md §1` test sayıları siliniyor (komutlar zaten sayıyı basıyor).
      Sayı **yalnızca §11'in son paragrafında** yaşasın. Bugün §1 "293" diyor, §11 "537" —
      belge kendi içinde çelişiyor.
- [x] **§5.1b** `CLAUDE.md §11` şema satırı: "sürüm 8" → **9**, "GecisTest 1→2 … 7→8" → **1→9**,
      "sürüm 4'ten 8'e" → **4'ten 9'a**. (§6 aynı belgede zaten 9 diyor.)
- [x] **§5.1c** `CLAUDE.md §1` üretilen dosyalar tablosu:
      - `agac_*.png` satırı gerçeğe çekilir — `drawable-nodpi`'de `agac_*.png` **yok**,
        shipping set `downscaled_tree_phase_1..9.png` ve üreticisi depoda değil. Ya set
        yeniden üretilir ya da tablo bunu açıkça yazar.
      - `DB.md` satırı: `assembleDebug` böyle bir görev **çalıştırmıyor** (yalnızca
        `room.schemaLocation` ayarı + yorum var). "Elle: `python3 tools/db_dokuman_uret.py`" yazılır.
      - `tasarim_ekran_kes.py` satırı eklenir (G5-021).
- [x] **§5.2a** `docs/arsiv/` açılır; içine: `debug.md` · `DEBUG_LOG.md` · `DEBUG_STATE.md` ·
      `docs/tarama-01.md` · `docs/tarama-04.md` · `docs/arsiv/tarama-05-gereksizlik.md` ·
      `docs/stitch-ekranlar.md`. `CLAUDE.md §2` haritasında tek satır: "kapanmış tarama kayıtları".
      **Kod içindeki `debug.md K-7` gibi referanslar metin olarak aynen kalır** (yol yazmıyorlar).
- [x] **§5.2b** `DEBUG_LOG.md`'nin "yayın öncesi riskler" bölümü ile `CLAUDE.md §12`'nin
      A12–A16 satırları **aynı şeyi iki yerde** söylüyor. Tek kaynak §12'nin A tablosu olur.
- [x] **§5.2c** `DESIGN.md §4`'ün başına uyarı satırı: ağaç evreleri risk bandına değil
      **toplam puana** bağlı, 5 değil **9** evre — kod kazanıyor (K2), bu bölüm tarihsel.

---

## Kapsam dışı — bilinçli olarak yapılmayacak

| ID | Neden |
|---|---|
| **G5-031** "Adım kaynağı: Telefon sayacı" | Kullanıcı kararı: satır olduğu gibi kalıyor |
| **G5-036** K50 dal ayrımı (dev = 15 sn) | Kullanıcı kararı: **K50 duruyor**. `dev` → `main` merge'ünde `--ff-only` çalışmaz, gerçek merge gerekir (CLAUDE.md §11'deki uyarı geçerli) |
| **G5-023** Sahte/yedekli depo katmanı (1.072 satır) | K18 gerekçesi geçerli; yalnızca KDoc dili düzeltiliyor (Grup 1) ve `onerilenler` kolu G5-005 ile düşüyor |
| **G5-025** `Bicim.kt` soyutlaması | Sorun eksik kullanımdı — G5-006/007/011 ile zaten kapanıyor |
| **G5-026** Referans geleneği | Arşivleme (§5.2a) ile kapsandı; referanslar silinmiyor |

---

## Uygulama sırası ve doğrulama

1. **Grup 1 + Grup 6** — yorum/doküman, davranış değişmez, derleme yeter.
2. **Grup 2** — ölü kod. Her silmeden önce `rg` ile tek doğrulama araması; `G5-004` için
   ayrıca cihaz DB kontrolü.
3. **Grup 3** — birleştirme. `:app:testDebugUnitTest` + `:core-risk:test`; `G5-007`'de
   `OdakHesaplaTest` / `YuruyusHesapla` testleri birleşiyor.
4. **Grup 5** — taşıma/varlık. `G5-012` sonrası `assembleRelease` + `dogrulaReleaseApk` (K32).
5. **Grup 4** — kullanıcıya dokunan değişiklikler **en son**, cihazda ekran ekran doğrulanır:
   Arkadaş ekle · Hedef · Lig · Ana · İstatistik · Risk detayı · Profil.
6. `G5-027` `core-risk`'e dokunduğu için `tests/test_vectors.json` karşılığı kontrol edilir (§7 test kuralı).

**Dal:** `fix/tarama-05`, `dev`'den. Merge kararı `tchkechagia0`'da (CLAUDE.md §1).
Grup başına bir commit önerilir — Grup 4 kendi içinde madde başına ayrılabilir.
