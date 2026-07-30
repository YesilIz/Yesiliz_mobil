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
import java.time.LocalDate
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

    private companion object {
        val ONBOARDING_BITTI = booleanPreferencesKey("onboarding_bitti")
        val GUNLUK_HEDEF = intPreferencesKey("gunluk_hedef_dk")
        val ILK_KURULUM = longPreferencesKey("ilk_kurulum_gunu")
        val GECMIS_GUN = intPreferencesKey("gecmis_gun_sayisi")
        val GECMIS_GUN_HESAPLANDI = booleanPreferencesKey("gecmis_gun_hesaplandi")
        val SON_ANKET = longPreferencesKey("son_anket_gunu")
        val YESILAY_PAYLASIM = booleanPreferencesKey("yesilay_paylasimi")
        val LIGDE_GORUN = booleanPreferencesKey("ligde_gorun")
        val EBEVEYN_ESLESMIS = booleanPreferencesKey("ebeveyn_eslesmis")
        val EBEVEYN_ROL = stringPreferencesKey("ebeveyn_cihaz_rolu")
        val MOLA_HATIRLATICI = booleanPreferencesKey("mola_hatirlatici_acik")

        /**
         * `"acik|sabahSaati|aksamSaati"` — ör. `"true|9|21"`.
         *
         * Üç değer için üç ayrı anahtar açmak yerine tek satır: birlikte
         * okunuyorlar, birlikte yazılıyorlar ve ayrı ayrı hiçbir yerde
         * kullanılmıyorlar.
         */
        val BILDIRIM_SAATLERI = stringPreferencesKey("bildirim_saatleri")

        /** Hedef henüz belirlenmediyse — onboarding'i atlayan kullanıcı. */
        const val VARSAYILAN_HEDEF = 240
    }
}
