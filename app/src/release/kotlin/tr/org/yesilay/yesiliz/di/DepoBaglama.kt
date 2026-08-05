package tr.org.yesilay.yesiliz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tr.org.yesilay.yesiliz.data.depo.SunucuHesapDeposu
import tr.org.yesilay.yesiliz.data.depo.SunucuLigDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import javax.inject.Singleton

/**
 * Arayüz → uygulama bağlamaları — **RELEASE**.
 *
 * `Sunucu*Deposu` bağlanıyor: veri yalnızca backend'den geliyor.
 * `YerelSahte*` sınıfları burada hiç örneklenmiyor — sunucuya
 * ulaşılamadığında kullanıcı boş lig + "sunucuya ulaşılamıyor" notu
 * görüyor, uydurulmuş bir sıralama değil.
 *
 * Debug eşi `src/debug/.../di/DepoBaglama.kt`: orada demo güvencesi için
 * yerel yedek açık (CLAUDE.md §12 K18).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DepoBaglama {

    @Binds
    @Singleton
    abstract fun ligDeposu(sunucu: SunucuLigDeposu): LigDeposu

    @Binds
    @Singleton
    abstract fun hesapDeposu(sunucu: SunucuHesapDeposu): HesapDeposu
}
