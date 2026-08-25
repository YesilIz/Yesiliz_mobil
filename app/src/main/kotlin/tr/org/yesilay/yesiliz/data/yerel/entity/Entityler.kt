package tr.org.yesilay.yesiliz.data.yerel.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Room şeması. Tam döküm ve DDL'ler: `DB.md` (üretilen dosya).
 *
 * **MAHREMİYET SÖZLEŞMESİ (CLAUDE.md §4):** bu şemada mesaj içeriği, fotoğraf,
 * rehber, arama geçmişi, konum YOK ve olmayacak. Yalnızca *hangi paket, ne
 * kadar süre, hangi kategori*. [KullanimOlayEntity] cihazdan ÇIKMAZ; sunucuya
 * (Faz 2) yalnızca günlük özet ve skor gider.
 */

/**
 * Ham kullanım olayı — bir uygulamanın bir ön plan oturumu.
 *
 * En hacimli tablo: günde ~100-300 satır. Örüntü çıkarımı (gece oranı,
 * kesintisiz blok, açılış sayısı) yalnızca buradan yapılabilir; günlük özet
 * bunu sıkıştırır.
 *
 * `queryEvents()` verisi kısa saklandığı için bu tablo geriye dönük
 * doldurulamaz (spec-risk.md §4.5) — kurulumdan önceki günler için
 * yalnızca hacim vardır, örüntü yoktur.
 */
@Entity(
    tableName = "kullanim_olay",
    indices = [Index("tarih"), Index("baslangic")],
)
data class KullanimOlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paketAdi: String,
    /** [tr.org.yesilay.yesiliz.domain.kategori.Kategori.anahtar] değeri */
    val kategori: String,
    val baslangic: Instant,
    val bitis: Instant,
    /** Olayın ait olduğu yerel gün — gün sınırı sorgularını hızlandırır. */
    val tarih: LocalDate,
)

/**
 * Uygulamanın kendi kaydettiği aktivite oturumu (şu an yalnızca aktif
 * yürüyüş, `YuruyusServisi`) — Health Connect'ten canlı okunan egzersiz
 * kayıtlarıyla birlikte [tr.org.yesilay.yesiliz.domain.kullanim.AktiviteTekillestir]'e
 * giriyor (CLAUDE.md §12 K31). Health Connect'in kendi kayıtları burada
 * ayrıca SAKLANMIYOR — Health Connect zaten kendi kaynağı, tekrar
 * tutmak veri çatallanması yaratırdı; yalnızca uygulamanın kendi
 * ölçtüğü oturumlar burada.
 *
 * `tarih` gün sınırı sorgularını hızlandırıyor, `baslangic`/`bitis`
 * [tr.org.yesilay.yesiliz.domain.kullanim.AktiviteOturumu]'nun örtüşme
 * hesabı için gerekli.
 */
@Entity(tableName = "aktivite_oturumu", indices = [Index("tarih")])
data class AktiviteOturumuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baslangic: Instant,
    val bitis: Instant,
    val tarih: LocalDate,
)

/**
 * Günlük özet — risk motorunun girdisi ve ekranların okuduğu satır.
 *
 * Hem ham girdiler (kategori dakikaları, örüntü değerleri) hem de hesabın
 * ara ürünleri (P, A, D, S) saklanıyor. Ara ürünler tekrar hesaplanabilir
 * ama saklamak risk detayı ekranını (DESIGN.md §5.12) tek sorguya indiriyor.
 */
@Entity(tableName = "gunluk_ozet")
data class GunlukOzetEntity(
    @PrimaryKey val tarih: LocalDate,

    // --- kategori dakikaları (core-risk'in kategoriDakika girdisi) ---
    val socialDk: Double = 0.0,
    val entertainmentDk: Double = 0.0,
    val gamesDk: Double = 0.0,
    val communicationDk: Double = 0.0,
    val productivityDk: Double = 0.0,
    val educationDk: Double = 0.0,
    val healthDk: Double = 0.0,
    val otherDk: Double = 0.0,

    // --- örüntü değerleri, 0..1 (spec-risk.md §4.2) ---
    val gece: Double = 0.0,
    val oturum: Double = 0.0,
    val acilis: Double = 0.0,
    val sabah: Double = 0.0,
    val uykuOncesi: Double = 0.0,
    val yerDegis: Double = 0.0,

    // --- aktivite ---
    val adim: Double = 0.0,
    val sporDk: Double = 0.0,

    // --- hesabın ara ürünleri ---
    val bTuketim: Double = 0.0,
    val bIslevsel: Double = 0.0,
    val P: Double = 1.0,
    val A: Double = 0.0,
    val D: Double = 1.0,
    val S: Double = 0.0,

    // --- ham sayaçlar (güvenlik tabanı ve istatistik ekranı için) ---
    val geceKullanimDk: Double = 0.0,
    val acilisSayisi: Int = 0,

    /**
     * O gün **gerçekten ölçülmüş kullanım olayı** var mıydı.
     *
     * Satırın varlığı veri olduğu anlamına gelmiyor: gecelik worker son 7
     * günü telafi ederken hiç oturum bulamadığı günlere de sıfır dolu bir
     * satır yazıyor. Bu satırlar "veri var" sayıldığı için yeni kullanıcı
     * hiçbir şey yapmadan 8 günlük seri ve ~400 puan kazanıyor, üstelik
     * `alpha` eğrisi 8 günlük geçmiş sanıp anketin ağırlığını yarıya
     * indiriyordu (`debug.md` K-4, K-5).
     *
     * Satır yine yazılıyor — telefonsuz geçen gerçek bir gün de kayıt;
     * ayıran şey bu bayrak.
     */
    val olayVarMi: Boolean = false,

    /**
     * Gece kullanımı gerçekten ölçülebildi mi.
     *
     * Olay bazlı yolda her oturumun saati bilindiği için `true`. Olay geçmişi
     * tükenmiş eski günlerde yalnızca paket başına toplam süre okunabiliyor
     * (saat bilgisi yok) ve bu bayrak `false` oluyor. Ayrım olmasa
     * `geceKullanimDk = 0` "gece hiç kullanılmadı" sanılır ve `GECE_TEMIZ`
     * görevi ölçülemeyen her güne puan yazardı.
     */
    val geceOlculduMu: Boolean = true,

    /**
     * [adim] hangi kaynaktan geldi — `SAGLIK` | `SENSOR` | `YOK`.
     *
     * Kullanıcı Fit/Samsung Health ile uygulamanın sayısı arasındaki farkı
     * hata sanıyor; fark gerçek (o uygulamalar ham sayacı kendi hareket
     * sınıflandırıcılarından geçirip eliyor). Sayının nereden geldiğini
     * söyleyebilmek için saklanıyor.
     */
    val adimKaynagi: String = "YOK",
)

/**
 * Günlük risk sonucu anlık görüntüsü.
 *
 * Trend (§5.5) ve histerezis (§5.4) çok günlü; kaynakları bu tablo.
 * [gosterilenBant] histerezisten geçmiş olan — kullanıcının gerçekten
 * gördüğü bant. [adayBant] eşik + güvenlik tabanından çıkan ham aday.
 */
@Entity(tableName = "risk_sonuc")
data class RiskSonucEntity(
    @PrimaryKey val tarih: LocalDate,
    val S: Double,
    val rB: Double,
    val rS: Double?,
    val alpha: Double,
    val R: Double,
    val adayBant: String,
    val gosterilenBant: String,
    val guvenlikKurali: String?,
    val trend: String,
    /** En çok katkı yapan 3 kategori, virgülle. §4.6 md.5: hep dolu olmalı. */
    val surukleyiciler: List<String>,
    /**
     * [tr.org.yesilay.yesiliz.risk.VeriDurumu] adı — `YETERSIZ` iken
     * [gosterilenBant] hesaplanmış olsa da UI'da GÖSTERİLMEMELİ
     * (CLAUDE.md §12 K30, `RiskDeposu.gunuHesapla`).
     */
    val veriDurumu: String = "HESAPLANDI",
)

/**
 * SAS-SV yanıtı.
 *
 * **Ham puan saklanır, bant değil** (spec-anket.md §6.4). Kesme puanı 29,5
 * kalibrasyonla değişirse geçmiş yanıtlar yeniden puanlanabilsin diye
 * madde madde de tutuluyor.
 */
@Entity(tableName = "anket_yanit")
data class AnketYanitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarih: LocalDate,
    val madde1: Int, val madde2: Int, val madde3: Int, val madde4: Int,
    val madde5: Int, val madde6: Int, val madde7: Int, val madde8: Int,
    val madde9: Int, val madde10: Int,
    val hamPuan: Double,
    val rS: Double,
)

/**
 * Paket → kategori eşlemesi.
 *
 * İlk açılışta `assets/kategori_tablosu.json` buraya seed edilir
 * ([kaynak] = `tablo`). Kullanıcı bir uygulamanın kategorisini değiştirirse
 * aynı satır [kaynak] = `kullanici` ile güncellenir — böylece tablo yeniden
 * seed edilse bile kullanıcı seçimi korunur.
 */
@Entity(tableName = "kategori_esleme")
data class KategoriEslemeEntity(
    @PrimaryKey val paketAdi: String,
    val kategori: String,
    /** `tablo` | `kullanici` */
    val kaynak: String,
) {
    companion object {
        const val KAYNAK_TABLO = "tablo"
        const val KAYNAK_KULLANICI = "kullanici"
    }
}

/** Görev tamamlama geçmişi (spec-puan.md §7). */
@Entity(tableName = "gorev_tamamlama", indices = [Index("tarih")])
data class GorevTamamlamaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarih: LocalDate,
    val gorevId: String,
    val puan: Int,
    /** `otomatik` (sensörle doğrulandı) | `beyan` (kullanıcı işaretledi) */
    val dogrulamaTuru: String,
)

/** Günlük puan ve seri (spec-puan.md §7.3). Ceza yok, sadece kazanç. */
@Entity(tableName = "puan_gunluk")
data class PuanGunlukEntity(
    @PrimaryKey val tarih: LocalDate,
    val puanHam: Int,
    val puanNet: Int,
    val seriGun: Int,
    val carpani: Double,

    /**
     * O gün günlük ekran süresi hedefi tutturuldu mu — serinin **tek** girdisi.
     *
     * Eskiden seri "önceki günde puan aldın mı" ile ilerliyordu; nefes
     * egzersizi yapıp 9 saat ekranda kalan kullanıcının serisi de büyüyordu.
     * Seri artık davranışın kendisini ödüllendiriyor.
     */
    val hedefTutturuldu: Boolean = false,

    /**
     * Bu gün hedefi kaçırdı ama seri **joker hakkıyla** korundu mu.
     *
     * Joker penceresi 7 gün: son 7 günde joker kullanılmışsa hak yok, seri
     * sıfırlanır. Tek kötü günün 30 günlük seriyi silmesi kullanıcıyı
     * uygulamadan koparıyor; bedelsiz olması da seriyi anlamsızlaştırıyor.
     */
    val jokerKullanildi: Boolean = false,
)

/**
 * Ruh hali beyanı — ana ekrandaki slider.
 *
 * **Risk skoruna girmez** (`core-risk` bunu bilmiyor); kullanıcının kendi
 * anlatısı ve "Ruh hali kaydı" görevinin dayanağı.
 *
 * Gün **üç pencereye** bölünüyor (sabah/öğlen/akşam, [RuhHaliPenceresi]) ve
 * her pencere kendi satırını tutuyor: tek satır varken günün hangi saatinde
 * nasıl hissedildiği kayboluyordu, oysa ruh hali gün içinde değişen şey.
 * Aynı pencerede tekrar sürüklenince değer güncellenir, yeni satır açılmaz.
 */
@Entity(tableName = "ruh_hali", primaryKeys = ["tarih", "dilim"])
data class RuhHaliEntity(
    val tarih: LocalDate,
    /** `sabah` | `oglen` | `aksam` — [RuhHaliPenceresi.anahtar] */
    val dilim: String,
    /** 0f (zorlanıyorum) – 1f (harika) */
    val deger: Float,
)

/**
 * Uyku beyanı — "Bugünün sorusu" widget'ından (tasarım 39) veya görev
 * listesinden gelir.
 *
 * Saat değil **aralık** saklanıyor: widget dört seçenek sunuyor ve kullanıcı
 * zaten tahmin ediyor; sahte hassasiyet üretmenin anlamı yok.
 */
@Entity(tableName = "uyku_beyani")
data class UykuBeyaniEntity(
    @PrimaryKey val tarih: LocalDate,
    /** [tr.org.yesilay.yesiliz.domain.gorev.UykuAraligi.anahtar] değeri */
    val aralik: String,
)

/**
 * Kısa farkındalık testi sonucu (5 soru).
 *
 * **SAS-SV DEĞİL.** Ölçek dışıdır, kesme puanı yoktur, risk motoruna
 * girmez — bu yüzden [AnketYanitEntity]'den ayrı tabloda duruyor. İkisini
 * aynı tabloda tutmak, kalibre edilmemiş bir puanın kalibre edilmiş bir
 * ölçekle karışması riskini doğururdu (`docs/spec-anket.md §6.1`).
 */
@Entity(tableName = "farkindalik_testi")
data class FarkindalikTestiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarih: LocalDate,
    /** 5 yanıt, her biri 0–2 */
    val yanitlar: List<String>,
    val hamPuan: Int,
    /** [tr.org.yesilay.yesiliz.domain.test.TestSeviyesi] adı */
    val seviye: String,
)

/**
 * Uygulama içi bildirim geçmişi (tasarım 35).
 *
 * Sistem bildirimlerinin kaydı **değil** — uygulamanın kullanıcıya söylediği
 * şeylerin defteri. Sistem bildirimi gönderilse de gönderilmese de buraya
 * satır düşer, böylece bildirim izni kapalı kullanıcı da olan biteni görür.
 */
@Entity(tableName = "bildirim", indices = [Index("zaman")])
data class BildirimEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [tr.org.yesilay.yesiliz.domain.bildirim.BildirimTuru] adı */
    val tur: String,
    val baslik: String,
    val govde: String,
    val zaman: Instant,
    val okundu: Boolean = false,
)

/**
 * Arkadaş / davet kaydı (tasarım 36, 37).
 *
 * MVP'de tamamen yerel: backend yok, gerçek bir sosyal grafik yok. Lig
 * sekmesinin boş durum ile dolu durum arasında geçmesini sağlıyor ve
 * Faz 2'de sunucudan gelecek listenin şeklini şimdiden sabitliyor.
 * **Rehber okunmuyor** (CLAUDE.md §4) — isimler ya davet kodundan ya da
 * örnek öneri listesinden geliyor.
 */
@Entity(tableName = "arkadas")
data class ArkadasEntity(
    @PrimaryKey val isim: String,
    /** `davet_gonderildi` | `eklendi` */
    val durum: String,
    val eklenmeTarihi: LocalDate,
) {
    companion object {
        const val DURUM_DAVET = "davet_gonderildi"
        const val DURUM_EKLENDI = "eklendi"
    }
}

/**
 * Donanım adım sayacının günlük birikimi (`debug.md` K-0 ortam tarafı).
 *
 * Health Connect bir **depo**; adım saymıyor, saklıyor. Yazan bir uygulama
 * (Samsung Health, Google Fit) yoksa okuma sonsuza kadar sıfır dönüyor —
 * test telefonunda tam olarak bu oldu. `Sensor.TYPE_STEP_COUNTER` üçüncü
 * parti hiçbir şeye bağlı değil, ama gün başına adım da vermiyor: açılıştan
 * beri biriken tek bir sayı veriyor. Bu tablo o sayının **farkını** güne
 * yazıyor; hesabın kendisi saf ve test edilebilir ([AdimDurumu]).
 *
 * Ham konum ya da hareket izi tutulmuyor — yalnızca iki tam sayı
 * (CLAUDE.md §4).
 */
@Entity(tableName = "adim_sayac")
data class AdimSayacEntity(
    @PrimaryKey val tarih: LocalDate,

    /** O gün biriktirilen adım. */
    val toplam: Long = 0,

    /** Sayacın son örnekteki ham değeri — bir sonraki farkın tabanı. */
    val sonSayac: Long = 0,
)

/**
 * Ödül seçimi — bazı kademelerde tek bir ödül yerine birden çok seçenek
 * var (örn. "Market kuponu" veya "Fidan bağışı"); kademe kazanılınca
 * kullanıcı biri arasında seçim yapıyor.
 *
 * Cihaz tek kullanıcılı olduğu için birincil anahtar yalnızca [kademeId] —
 * sunucudaki `(kullanici_id, kademe_id)` tekil kısıtının cihaz tarafı
 * karşılığı. Seçim kalıcı: [OdulSecimiDao.kaydet] `Insert` kullanıyor,
 * `Upsert` değil — bir kademe için ikinci yazma denemesi (rozet sürücüsü
 * hatalıysa bile) var olan seçimi sessizce değiştirmesin diye.
 */
@Entity(tableName = "odul_secimi")
data class OdulSecimiEntity(
    @PrimaryKey val kademeId: String,
    val secenekId: String,
    val tarih: LocalDate,
)


/**
 * Panelden yapılmış puan düzeltmesi (`CLAUDE.md §12 K56`).
 *
 * **Ayrı tablo olmasının sebebi:** `PuanDeposu.gunuYenidenHesaplaIc` her
 * koşumda `puan_gunluk` satırını `@Upsert` ile eziyor. Düzeltmeyi oraya
 * yazmak, bir sonraki gecelik işte kaybolması demekti.
 *
 * **Birincil anahtar sunucu id'si.** Aynı düzeltmenin ikinci kez
 * indirilmesi no-op oluyor; bir deltayı iki kez uygulamak geri dönülemez
 * bir kullanıcı zararı ve koruma tek bir bayrağa bırakılamaz.
 */
@Entity(tableName = "puan_duzeltme")
data class PuanDuzeltmeEntity(
    /** Sunucudaki `puan_duzeltme.id` — tekrar koruması bu. */
    @PrimaryKey val id: Int,
    /** Pozitif ekler, negatif düşürür. */
    val delta: Int,
    /** Kullanıcıya **gösteriliyor**; gerekçesiz düzeltme "ceza" olurdu. */
    val gerekce: String,
    val zaman: Instant,
    /** Bildirim defterine yazıldı mı? Aynı düzeltme iki kez bildirilmesin. */
    val bildirildi: Boolean = false,
)
