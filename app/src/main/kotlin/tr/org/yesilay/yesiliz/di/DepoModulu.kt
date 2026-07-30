package tr.org.yesilay.yesiliz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tr.org.yesilay.yesiliz.data.depo.YedekliHesapDeposu
import tr.org.yesilay.yesiliz.data.depo.YedekliLigDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
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

    // Faz 2: artık `Yedekli*` bağlanıyor — sunucu varsa uzak, yoksa yerel.
    // Ekranlar ve ViewModel'ler değişmedi; §12 K7'nin vaat ettiği tam olarak
    // buydu.
    @Binds
    @Singleton
    abstract fun ligDeposu(yedekli: YedekliLigDeposu): LigDeposu

    @Binds
    @Singleton
    abstract fun hesapDeposu(yedekli: YedekliHesapDeposu): HesapDeposu
}
