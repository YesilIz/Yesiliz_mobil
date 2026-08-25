# yedam-riza-metni-v1.md — YEDAM Desteği Açık Rıza Metni

> **Sürüm: `v1`.** Bu değer koda `yedam_riza.metin_surumu` alanına **birebir**
> giriyor. Metnin kapsamı değişirse sürüm `v2` olur ve daha önce rıza vermiş
> herkesten **yeniden** rıza istenir — eski rıza yeni kapsamı karşılamaz.
>
> **Bu metin bilgilendirme amaçlıdır ve hukuki incelemeden geçmemiştir.**
> Nihai KVKK açık rıza metni Yeşilay tarafından yayımlanacaktır.
> (`Dökümanlar/Yesiliz - Gizlilik Politikası.txt` ile aynı çekince.)

---

## 1. EKRANDA GÖSTERİLECEK METİN

Aşağıdaki bölüm uygulamada **birebir** gösterilir. Kısaltılamaz, özetlenemez;
madde atlanırsa rıza o maddeyi kapsamaz.

---

### YEDAM desteği istiyorum

YEDAM (Yeşilay Danışmanlık Merkezi) ücretsiz ve gizli danışmanlık veriyor.
Buradan destek istersen, bir danışman seninle iletişime geçebilsin diye bazı
bilgilerin paylaşılması gerekiyor. Ne paylaşıldığını tek tek aşağıda yazdık.

**Paylaşılacak bilgiler**

- Adın ve seçtiğin iletişim bilgin (telefon ya da e-posta)
- Son 30 günün risk bandın ve eğilimi
- Skorunu en çok yükselten kategoriler (örneğin "sosyal medya", "oyun")
- Daha önce yaptıysan, farkındalık anketi sonuçların
- Yazmak istersen, kendi eklediğin not

**Paylaşılmayacak bilgiler**

- Hangi uygulamayı ne zaman açtığın
- Mesajların, fotoğrafların, rehberin, arama ve tarayıcı geçmişin
- Konumun
- Şifren

Bunlar zaten hiçbir zaman toplanmıyor ve telefonundan çıkmıyor.

**Kim görecek**

Yalnızca başvurunun atandığı YEDAM danışmanı. Danışmanlar kullanıcı arayamaz;
yalnızca kendilerine atanan başvuruyu açabilirler.

**Ne kadar süre**

90 gün. Süre dolduğunda risk ve anket bilgilerin sunucudan **silinir**,
danışmanın erişimi kapanır.

**Her bakış sana bildirilir**

Danışman bilgilerine her baktığında uygulamada bir kayıt görürsün:
"14 Ağustos 10:32'de bakıldı." Bunu Profil › Gizlilik'ten her zaman
görebilirsin.

**İstediğin an geri alabilirsin**

Profil › Gizlilik › "YEDAM rızamı geri çek". Geri çektiğin anda danışmanın
erişimi kesilir ve risk ile anket bilgilerin sunucudan silinir.

Geri çektiğinde **silinmeyenler**: başvurunun kaydı, görüşme kayıtları,
danışmanın kendi notları ve sana kimin baktığını gösteren kayıt. Bunlar bir
danışmanlık ilişkisinin kaydı; silinmeleri geçmişte ne olduğunu senin de
görememen demek olurdu.

**Bu bir teşhis değil**

Yeşilİz bir sağlık hizmeti değildir, tanı koymaz. Danışman kendi
değerlendirmesini yapar.

☐ Yukarıdakileri okudum, YEDAM danışmanının bu bilgileri görmesine izin
veriyorum.

---

### 18 yaşından küçüksen

Uygulamaya bir ebeveyn ya da yasal vasi hesabı bağlıysa, YEDAM başvurunun
açılabilmesi için onların da onayı isteniyor. Onay gelene kadar başvurun
bekliyor sayılır ve **hiçbir danışman görmez**.

Bağlı bir ebeveyn hesabı yoksa kendi rızan yeterli — desteğe erişimin bir
eşleştirme yapmadığın için engellenmemeli.

> **Beyan usulü.** Yaşını uygulamaya sen giriyorsun ve bunu doğrulayamıyoruz.
> `CLAUDE.md §12 K26` ile aynı çekince.

---

## 2. KAPSAM — KODA GİREN HÂLİ

`yedam_riza.kapsam` alanı bu listeyi taşıyor. Ekrandaki metinle **aynı şeyi**
söylüyor; ekranda insan diliyle, burada makine diliyle.

| Kapsam anahtarı | Karşılığı | Tablo |
|---|---|---|
| `kimlik` | ad + iletişim tercihi ve değeri | `yedam_basvuru` |
| `risk` | 90 günlük bant, trend, `veri_durumu` | `risk_sonuc` (var olan) |
| `surukleyici` | en çok katkı yapan 3 kategori | `risk_surukleyici` (yeni) |
| `anket` | SAS-SV yanıt geçmişi | `anket_yanit` (yeni) |
| `davranis` | kategori dakikaları, örüntü değerleri | `gunluk_ozet` (var olan) |
| `not` | kullanıcının kendi yazdığı not | `yedam_basvuru` |

> **`davranis` ayrı bir rızaya bağlı.** `gunluk_ozet` sunucuya yalnızca
> "Yeşilay ile paylaşım" açıkken gidiyor (`CLAUDE.md §12 K19`). YEDAM rızası
> o kapıyı **açmıyor**: paylaşım kapalıysa danışman bu bölümü boş görür ve
> ekranda "Kullanıcı davranış özetini paylaşmıyor" yazar. İki rıza bağımsız.

---

## 3. SAKLAMA VE SİLME

| Veri | Rıza sürerken | Geri çekme / süre dolması |
|---|---|---|
| `risk_surukleyici` | danışman görür | **silinir** |
| `anket_yanit` | danışman görür | **silinir** |
| `gunluk_ozet` · `risk_sonuc` | danışman görür (paylaşım rızası varsa) | kalır — bunlar YEDAM'a değil, kullanıcının kendi hesabına ait |
| `yedam_basvuru` | açık | kalır, durumu `riza_geri_cekildi` olur |
| `yedam_temas` | danışman görür | kalır |
| `danisman_notu` | danışman görür | kalır — klinik kayıt, ayrı rejim (`docs/panel-plani.md §14 S4`) |
| `erisim_kaydi` | kullanıcı görür | kalır — silinmesi şeffaflığın kendisini yok ederdi |

Hesabın tamamen silinmesi (`DELETE /auth/hesap`) bu ayrımı **tanımaz**:
KVKK silme hakkı her şeyi siler, danışman notu dahil.

---

## 4. SÜRÜM GEÇMİŞİ

| Sürüm | Tarih | Değişiklik |
|---|---|---|
| `v1` | — | İlk metin |

> Yeni sürüm yazılırken: eski sürümü **silme**, tabloya satır ekle. Hangi
> kullanıcının hangi metne rıza verdiği `yedam_riza.metin_surumu`'nda duruyor
> ve o metnin okunabilir olması gerekiyor.
