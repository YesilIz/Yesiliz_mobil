package tr.org.yesilay.yesiliz.data.tercih

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tr.org.yesilay.yesiliz.BuildConfig
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import java.time.LocalDate
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tercihDeposu: DataStore<Preferences> by preferencesDataStore(name = "tercihler")

/**
 * Tekil tercih değerleri — CLAUDE.md §6 "Room'a girmeyenler → DataStore".
 *
 * Buradaki her değer **tek satırlık**; tablo açmanın anlamı yok. Kritik
 * olanlar risk motorunun girdileri:
 *
 * | Değer | Motorda karşılığı |
 * |---|---|
 * | [ilkKurulumTarihi] | alpha eğrisinin `d` girdisi (`spec-risk.md §4.5`) |
 * | [gecmisGunSayisi] | `GecmisPencere.gecmisGun` — 0,6 ağırlıkla sayılan yarım veri |
 * | [sonAnketTarihi] | `AnketGirdi.kacGunOnce` — anket 60 günde tamamen söner |
 */
@Singleton
class Tercihler @Inject constructor(
    @ApplicationContext private val baglam: Context,
) {
    private val depo get() = baglam.tercihDeposu

    // --- onboarding ---

    val onboardingBitti: Flow<Boolean> = depo.data.map { it[ONBOARDING_BITTI] ?: false }

    val gunlukHedefDk: Flow<Int> = depo.data.map { it[GUNLUK_HEDEF] ?: VARSAYILAN_HEDEF }

    // --- alpha eğrisinin girdileri ---

    /** Uygulamanın ilk açıldığı gün; `d` bu tarihten bugüne geçen gün sayısı. */
    val ilkKurulumTarihi: Flow<LocalDate?> = depo.data.map { p ->
        p[ILK_KURULUM]?.let { LocalDate.ofEpochDay(it) }
    }

    /**
     * Kurulumda `queryUsageStats` ile geriye dönük alınabilen gün sayısı.
     *
     * Yarım veri: hacim var, örüntü yok (`queryEvents` kısa saklanıyor).
     * Bu yüzden `Alpha.dEfektif` bunu 0,6 ile çarpıyor.
     */
    val gecmisGunSayisi: Flow<Int> = depo.data.map { it[GECMIS_GUN] ?: 0 }

    val sonAnketTarihi: Flow<LocalDate?> = depo.data.map { p ->
        p[SON_ANKET]?.let { LocalDate.ofEpochDay(it) }
    }

    /**
     * [gecmisGunuKaydet] daha önce çağrıldı mı?
     *
     * [kurulumuIsaretle] ile ayrı tutuluyor çünkü o ilk açılışta izin
     * verilmeden önce çalışabiliyor (`gecmisGun = 0` yazar). Gerçek sayı
     * ancak izin onaylandıktan sonra hesaplanabiliyor — bu bayrak o
     * hesaplamanın tam olarak bir kez, izin ne zaman verilirse verilsin
     * yapılmasını garanti ediyor.
     */
    val gecmisGunHesaplandiMi: Flow<Boolean> = depo.data.map { it[GECMIS_GUN_HESAPLANDI] ?: false }

    // --- gizlilik ---

    /** Varsayılan **kapalı** — rıza opt-in olmalı (`docs/spec-veri.md`). */
    val yesilayPaylasimi: Flow<Boolean> = depo.data.map { it[YESILAY_PAYLASIM] ?: false }

    val ligdeGorun: Flow<Boolean> = depo.data.map { it[LIGDE_GORUN] ?: true }

    /** Kullanıcı bir lige katıldı mı — false ise Lig sekmesi boş durum (37). */
    val ligeKatildiMi: Flow<Boolean> = depo.data.map { it[LIGE_KATILDI] ?: false }

    /**
     * Cihaz başına bir kez üretilen davet kodu (tasarım 36).
     *
     * Sunucu olmadığı için kod yerelde üretiliyor; Faz 2'de backend'den
     * gelecek. Bir kez üretilip saklanması şart — kod her açılışta değişirse
     * paylaşılan davet geçersizleşir.
     */
    val davetKodu: Flow<String?> = depo.data.map { it[DAVET_KODU] }

    // --- hesap ---
    // Şifre BURADA YOK ve olmayacak: yerelde saklamak, güvenli olmayan bir
    // kimlik doğrulamayı güvenliymiş gibi göstermek olur. Sunucu Faz 2'de.

    val oturum: Flow<Oturum> = depo.data.map { p ->
        Oturum(
            girisYapildiMi = p[HESAP_GIRIS] ?: false,
            misafirMi = p[HESAP_MISAFIR] ?: false,
            ad = p[HESAP_AD] ?: "",
            eposta = p[HESAP_EPOSTA] ?: "",
            avatarId = p[HESAP_AVATAR] ?: "",
        )
    }

    // --- ebeveyn paneli eşleştirmesi ---
    // MVP'de gerçek `EbeveynBaglantiEntity` yok (CLAUDE.md §6 — Faz 2'ye
    // ertelendi, backend gerektiriyor). Eşleştirme durumu bu yüzden Room'a
    // değil buraya yazılıyor; Faz 2'de backend gelince bu iki alan
    // `EbeveynDeposu` arayüzünün arkasına taşınır, ekran/ViewModel değişmez.

    /** Eşleştirme tamamlandı mı (cihaz rolü fark etmeksizin). */
    val ebeveynEslesmisMi: Flow<Boolean> = depo.data.map { it[EBEVEYN_ESLESMIS] ?: false }

    /** `"ebeveyn"` veya `"cocuk"` — eşleşme yoksa `null`. */
    val ebeveynCihazRolu: Flow<String?> = depo.data.map { it[EBEVEYN_ROL] }

    // --- nudge ---

    /** Varsayılan **kapalı** — overlay izni gerektirdiği için opt-in. */
    val molaHatirlaticiAcikMi: Flow<Boolean> = depo.data.map { it[MOLA_HATIRLATICI] ?: false }

    // --- günlük hatırlatma saatleri ---

    /** Varsayılan **kapalı** — istenmeyen bildirim göndermiyoruz. */
    val bildirimSaatleri: Flow<BildirimSaatleri> = depo.data.map { p ->
        p[BILDIRIM_SAATLERI]?.let(BildirimSaatleri::coz) ?: BildirimSaatleri()
    }

    suspend fun bildirimSaatleriniKaydet(saatler: BildirimSaatleri) {
        depo.edit { it[BILDIRIM_SAATLERI] = saatler.metin() }
    }

    // --- yazma ---

    suspend fun onboardingiBitir(hedefDk: Int, anketTarihi: LocalDate?) {
        depo.edit { p ->
            p[ONBOARDING_BITTI] = true
            p[GUNLUK_HEDEF] = hedefDk
            anketTarihi?.let { p[SON_ANKET] = it.toEpochDay() }
        }
    }

    suspend fun hedefiKaydet(dakika: Int) {
        depo.edit { it[GUNLUK_HEDEF] = dakika }
    }

    /**
     * Anket yeniden çözüldüğünde tarihi tazeler.
     *
     * [onboardingiBitir] ile ayrı: o ilk kurulumda bir kez çalışıyor ve
     * hedefi de yazıyor. Bu ise 4 haftada bir tekrarlanan değerlendirmede
     * yalnızca tarihi güncelliyor — `AnketGirdi.kacGunOnce` sıfırlanıyor,
     * alpha eğrisi tazeleniyor (`docs/spec-risk.md §4.5`).
     */
    suspend fun anketTarihiniKaydet(tarih: LocalDate) {
        depo.edit { it[SON_ANKET] = tarih.toEpochDay() }
    }

    /**
     * İlk açılışta bir kez yazılır; **var olan değer ezilmez**.
     * Ezilse alpha eğrisi her açılışta `d = 0`'a döner ve anket ağırlığı
     * hiç düşmezdi.
     */
    suspend fun kurulumuIsaretle(bugun: LocalDate, gecmisGun: Int) {
        depo.edit { p ->
            if (p[ILK_KURULUM] == null) {
                p[ILK_KURULUM] = bugun.toEpochDay()
                p[GECMIS_GUN] = gecmisGun
            }
        }
    }

    /**
     * Gerçek `queryUsageStats` geriye dönük gün sayısı — izin onaylandıktan
     * sonra bir kez yazılır (`GecmisVeriDeposu.hesaplaGerekirse`).
     *
     * [kurulumuIsaretle]'den ayrı bayrakla korunuyor: o çağrı izin daha
     * verilmeden çalışabildiği için `gecmisGun = 0` yazmış olabilir; bu
     * fonksiyon o değeri gerçek sayıyla üzerine yazıyor ama yalnızca bir kez.
     */
    suspend fun gecmisGunuKaydet(gun: Int) {
        depo.edit { p ->
            if (p[GECMIS_GUN_HESAPLANDI] != true) {
                p[GECMIS_GUN] = gun
                p[GECMIS_GUN_HESAPLANDI] = true
            }
        }
    }

    suspend fun yesilayPaylasimiAyarla(acik: Boolean) {
        depo.edit { it[YESILAY_PAYLASIM] = acik }
    }

    suspend fun ligdeGorunAyarla(acik: Boolean) {
        depo.edit { it[LIGDE_GORUN] = acik }
    }

    suspend fun oturumuKaydet(
        ad: String,
        eposta: String,
        misafir: Boolean,
        avatarId: String = "",
    ) {
        depo.edit { p ->
            p[HESAP_GIRIS] = !misafir
            p[HESAP_MISAFIR] = misafir
            p[HESAP_AD] = ad
            p[HESAP_EPOSTA] = eposta
            p[HESAP_AVATAR] = avatarId
        }
    }

    /**
     * Profil güncellemesinin **cihaz** tarafı — e-postaya dokunmuyor.
     *
     * E-posta burada da değiştirilebiliyordu; `YerelOturumSiniri`'nin veri
     * sahibi anahtarı e-posta olduğu için (`VERI_SAHIBI`) bu, sonraki
     * girişte sahip uyuşmazlığı yaratıp kullanıcının bütün yerel geçmişini
     * sildiriyordu. E-posta yalnızca giriş/kayıt akışında yazılıyor.
     */
    suspend fun profilGuncelle(ad: String, avatarId: String) {
        depo.edit { p ->
            p[HESAP_AD] = ad
            p[HESAP_AVATAR] = avatarId
        }
    }

    suspend fun oturumuSil() {
        depo.edit { p ->
            p.remove(HESAP_GIRIS)
            p.remove(HESAP_MISAFIR)
            p.remove(HESAP_AD)
            p.remove(HESAP_EPOSTA)
            p.remove(HESAP_AVATAR)
        }
    }

    /**
     * Room'daki cihaz verisinin sahibi. Oturum kapanınca bilinçli olarak
     * silinmez: aynı hesap geri gelirse geçmiş korunur; farklı hesap
     * gelirse [tr.org.yesilay.yesiliz.data.depo.YerelOturumSiniri]
     * eski kullanıcının verisini temizler.
     */
    val veriSahibi: Flow<String?> = depo.data.map { it[VERI_SAHIBI] }

    /** Bu sahibin UsageStats olaylarının okunabileceği en erken an. */
    val veriBaslangicAni: Flow<Instant?> = depo.data.map { p ->
        p[VERI_BASLANGIC_ANI]?.let(Instant::ofEpochMilli)
    }

    suspend fun veriSahibiniKaydet(sahip: String) {
        depo.edit { it[VERI_SAHIBI] = sahip }
    }

    suspend fun veriBaslangiciniKaydet(an: Instant) {
        depo.edit { it[VERI_BASLANGIC_ANI] = an.toEpochMilli() }
    }

    suspend fun ligeKatildiIsaretle() {
        depo.edit { it[LIGE_KATILDI] = true }
    }

    /** Var olan kodu **ezmiyor** — paylaşılmış bir kod geçersizleşmesin. */
    suspend fun davetKodunuKaydet(kod: String) {
        depo.edit { p -> if (p[DAVET_KODU] == null) p[DAVET_KODU] = kod }
    }

    suspend fun ebeveynEslesmeyiKaydet(rol: String) {
        depo.edit { p ->
            p[EBEVEYN_ESLESMIS] = true
            p[EBEVEYN_ROL] = rol
        }
    }

    suspend fun ebeveynEslesmeyiKaldir() {
        depo.edit { p ->
            p.remove(EBEVEYN_ESLESMIS)
            p.remove(EBEVEYN_ROL)
        }
    }

    suspend fun molaHatirlaticiAyarla(acik: Boolean) {
        depo.edit { it[MOLA_HATIRLATICI] = acik }
    }

    /**
     * Profil → "Verilerimi/Hesabımı kalıcı sil" onayından çağrılır. Yalnızca
     * DataStore'daki tercihleri sıfırlar — Room tablolarının silinmesi
     * (`YesilizDb.clearAllTables()`) ayrı, çağıran taraf (`VeriYonetimiDeposu`)
     * ikisini birlikte yürütür.
     */
    /**
     * Sunucu adresi — varsayılanı [VARSAYILAN_SUNUCU], yani derlemeye gömülen
     * canlı adres. Debug de release de aynı sunucuya bakıyor: temiz kurulmuş
     * bir telefon hiçbir ayar yapmadan çalışıyor.
     *
     * Kayıtlı değer **hata ayıklama** için duruyor: yerel backend'e (docker
     * compose) bağlanmak isteyen geliştirici Profil › Geliştirici'den adres
     * yazabiliyor (`debug.md` O-14). Yayın derlemesi bu değeri okumuyor
     * (§12 K24).
     */
    val sunucuTabani: Flow<String> = depo.data.map { p ->
        etkinSunucuTabani(
            debugMi = BuildConfig.DEBUG,
            saklanan = p[SUNUCU_TABANI],
            varsayilan = VARSAYILAN_SUNUCU,
        )
    }

    /**
     * Kayıtlı geliştirici ezmesi — **ham** değer, varsayılana düşmüyor.
     *
     * "Ezme var mı yok mu" sorusunu ayırt etmesi gereken tek yer
     * `YerelOturumSiniri.yereliTemizle`: orada etkin adres okunup geri
     * yazılsaydı, ezme olmadığı hâlde o günkü varsayılan kalıcı bir ezmeye
     * dönüşür ve derlemedeki adres bir daha değişemezdi.
     */
    val saklananSunucuTabani: Flow<String?> = depo.data.map { it[SUNUCU_TABANI] }

    suspend fun sunucuTabaniniKaydet(adres: String) {
        // Geliştirici adresi yalnızca debug aracıdır. Eski/test bir
        // kurulumdan kalmış değer yayın istemcisinin token veya özetlerini
        // başka bir sunucuya yönlendirememeli.
        if (!BuildConfig.DEBUG) return
        depo.edit { p ->
            // Sondaki eğik çizgi Retrofit ve OkHttp için şart; kullanıcıya
            // yazdırmak yerine burada tamamlanıyor.
            p[SUNUCU_TABANI] = adres.trim().let { if (it.endsWith("/")) it else "$it/" }
            p[SUNUCU_TABANI_SURUM] = SUNUCU_TABANI_GECERLI_SURUM
        }
    }

    /**
     * "Varsayılana dön" — kayıtlı ezmeyi siler, adres [VARSAYILAN_SUNUCU]'ya
     * (derlemeye gömülü canlı sunucu) döner.
     */
    suspend fun sunucuTabaniniSifirla() {
        if (!BuildConfig.DEBUG) return
        depo.edit { p ->
            p.remove(SUNUCU_TABANI)
            p[SUNUCU_TABANI_SURUM] = SUNUCU_TABANI_GECERLI_SURUM
        }
    }

    /**
     * Tek seferlik temizlik — açılışta çağrılır.
     *
     * Debug varsayılanı emülatör adresiyken cihazlara elle yazılmış (ya da
     * `YerelOturumSiniri` tarafından geri yazılmış) adresler DataStore'da
     * kaldı ve varsayılanı ezmeye devam ediyor. Değere **bakmıyor**: sürüm
     * işaretçisi yoksa kayıtlı ezme siliniyor, işaretçi yazılıyor. Yerel
     * backend'e bağlanan geliştirici adresi bir kez yeniden yazar.
     *
     * (Değere bakan bir kural eski adresi string sabiti olarak taşımak
     * zorunda kalırdı; `10.0.2.2` sabiti `src/main`'e giremiyor — §12 K32
     * nöbetçisi derlemeyi düşürür.)
     */
    suspend fun eskiSunucuAdresiniTemizle() {
        if (!sunucuAdresiTemizlenmeli(depo.data.first()[SUNUCU_TABANI_SURUM])) return
        depo.edit { p ->
            p.remove(SUNUCU_TABANI)
            p[SUNUCU_TABANI_SURUM] = SUNUCU_TABANI_GECERLI_SURUM
        }
    }

    /**
     * Uydurma adım verisi temizliği yapılmalı mı — tek seferlik.
     *
     * Bir ara sürüm, geçmiş günlerde adım 0 dönünce hafta içi ~5.500 /
     * hafta sonu ~3.800 civarında **üretilmiş** bir sayı yazıyordu ve bu
     * sayı risk sönümlemesine, puana ve seriye giriyordu. Fonksiyon
     * kaldırıldı ama cihazlardaki satırlar duruyor; gecelik worker o
     * satırları atlıyordu çünkü "adım var" görünüyorlardı.
     *
     * Bayrak görüldüğünde worker telafi penceresinin tamamını **koşulsuz**
     * yeniden işliyor, sonra bayrağı düşürüyor. Sürüm işaretçili desen
     * [eskiSunucuAdresiniTemizle] ile aynı.
     */
    val uydurmaAdimTemizligiGerekli: Flow<Boolean> =
        depo.data.map { (it[ADIM_TEMIZLIK_SURUM] ?: 0) < ADIM_TEMIZLIK_GECERLI_SURUM }

    suspend fun uydurmaAdimTemizligiBitti() {
        depo.edit { it[ADIM_TEMIZLIK_SURUM] = ADIM_TEMIZLIK_GECERLI_SURUM }
    }

    suspend fun sifirla() {
        depo.edit { it.clear() }
    }

    /** Tek seferlik okuma — worker ve depo katmanı için (Flow toplamaz). */
    suspend fun anlik(): Anlik = Anlik(
        ilkKurulumTarihi = ilkKurulumTarihi.first(),
        gecmisGunSayisi = gecmisGunSayisi.first(),
        sonAnketTarihi = sonAnketTarihi.first(),
        gunlukHedefDk = gunlukHedefDk.first(),
    )

    data class Anlik(
        val ilkKurulumTarihi: LocalDate?,
        val gecmisGunSayisi: Int,
        val sonAnketTarihi: LocalDate?,
        val gunlukHedefDk: Int,
    )

    companion object {
        /**
         * Varsayılan sunucu adresi — **derlemeden** geliyor.
         *
         * İki build türünde de canlı HTTPS adresi; `-Pyesiliz.sunucu` ya da
         * `YESILIZ_SUNUCU` ile eziliyor. Tek yapılandırma noktası
         * `app/build.gradle.kts`; adres uygulama koduna ikinci bir yere
         * yazılmıyor. `AgModulu` yer tutucu taban olarak da bunu kullanıyor.
         */
        val VARSAYILAN_SUNUCU: String = BuildConfig.API_TABANI

        /**
         * Kayıtlı sunucu adresinin şema sürümü — [eskiSunucuAdresiniTemizle].
         *
         * Varsayılan adres değiştiğinde bu sayı artırılırsa cihazlardaki eski
         * ezmeler bir kez daha temizlenir.
         */
        const val SUNUCU_TABANI_GECERLI_SURUM = 1

        /**
         * Adım temizliği sürümü — [uydurmaAdimTemizligiGerekli].
         *
         * Geçmiş günlerin adımını bozan yeni bir kusur çıkarsa bu sayı
         * artırılır ve telafi penceresi bir kez daha baştan işlenir.
         */
        const val ADIM_TEMIZLIK_GECERLI_SURUM = 1

        // Anahtarlar dışarıya kapalı; yalnızca sabit paylaşılıyor.
        private val ONBOARDING_BITTI = booleanPreferencesKey("onboarding_bitti")
        private val GUNLUK_HEDEF = intPreferencesKey("gunluk_hedef_dk")
        private val ILK_KURULUM = longPreferencesKey("ilk_kurulum_gunu")
        private val GECMIS_GUN = intPreferencesKey("gecmis_gun_sayisi")
        private val GECMIS_GUN_HESAPLANDI = booleanPreferencesKey("gecmis_gun_hesaplandi")
        private val SON_ANKET = longPreferencesKey("son_anket_gunu")
        private val YESILAY_PAYLASIM = booleanPreferencesKey("yesilay_paylasimi")
        private val SUNUCU_TABANI = stringPreferencesKey("sunucu_tabani")
        private val SUNUCU_TABANI_SURUM = intPreferencesKey("sunucu_tabani_surum")
        private val ADIM_TEMIZLIK_SURUM = intPreferencesKey("adim_temizlik_surum")
        private val LIGDE_GORUN = booleanPreferencesKey("ligde_gorun")
        private val LIGE_KATILDI = booleanPreferencesKey("lige_katildi")
        private val HESAP_GIRIS = booleanPreferencesKey("hesap_giris")
        private val HESAP_MISAFIR = booleanPreferencesKey("hesap_misafir")
        private val HESAP_AD = stringPreferencesKey("hesap_ad")
        private val HESAP_EPOSTA = stringPreferencesKey("hesap_eposta")
        private val HESAP_AVATAR = stringPreferencesKey("hesap_avatar")
        private val VERI_SAHIBI = stringPreferencesKey("yerel_veri_sahibi")
        private val VERI_BASLANGIC_ANI = longPreferencesKey("yerel_veri_baslangic_ani")
        private val DAVET_KODU = stringPreferencesKey("davet_kodu")
        private val EBEVEYN_ESLESMIS = booleanPreferencesKey("ebeveyn_eslesmis")
        private val EBEVEYN_ROL = stringPreferencesKey("ebeveyn_cihaz_rolu")
        private val MOLA_HATIRLATICI = booleanPreferencesKey("mola_hatirlatici_acik")

        /**
         * `"acik|sabahSaati|aksamSaati"` — ör. `"true|9|21"`.
         *
         * Üç değer için üç ayrı anahtar açmak yerine tek satır: birlikte
         * okunuyorlar, birlikte yazılıyorlar ve ayrı ayrı hiçbir yerde
         * kullanılmıyorlar.
         */
        private val BILDIRIM_SAATLERI = stringPreferencesKey("bildirim_saatleri")

        /** Hedef henüz belirlenmediyse — onboarding'i atlayan kullanıcı. */
        const val VARSAYILAN_HEDEF = 240
    }
}

/**
 * Kayıtlı geliştirici adresi tek seferlik temizlenmeli mi?
 *
 * Saf karar: işaretçi yoksa (ya da eskiyse) evet. Değere bakmıyor —
 * gerekçesi [Tercihler.eskiSunucuAdresiniTemizle]'de.
 */
internal fun sunucuAdresiTemizlenmeli(saklananSurum: Int?): Boolean =
    (saklananSurum ?: 0) < Tercihler.SUNUCU_TABANI_GECERLI_SURUM

/** Yayın istemcisi kalıcı bir debug sunucu geçersiz kılmasını asla kullanmaz. */
internal fun etkinSunucuTabani(
    debugMi: Boolean,
    saklanan: String?,
    varsayilan: String,
): String = if (debugMi) {
    saklanan?.takeIf { it.isNotBlank() } ?: varsayilan
} else {
    varsayilan
}
