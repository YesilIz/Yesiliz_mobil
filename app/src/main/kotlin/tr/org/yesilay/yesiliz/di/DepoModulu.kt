package tr.org.yesilay.yesiliz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tr.org.yesilay.yesiliz.data.depo.YerelSahteLigDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import javax.inject.Singleton

/**
 * Arayüz → uygulama bağlamaları.
 *
 * Mock repository deseni (CLAUDE.md §12 K7) burada somutlaşıyor: Faz 2'de
 * backend gelince **yalnızca bu dosya** değişiyor —
 * `YerelSahteLigDeposu` yerine `RetrofitLigDeposu` bağlanıyor, ViewModel ve
 * Composable'lara dokunulmuyor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DepoModulu {

    @Binds
    @Singleton
    abstract fun ligDeposu(yerel: YerelSahteLigDeposu): LigDeposu
}
