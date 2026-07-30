package tr.org.yesilay.yesiliz.data.demo

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Release derlemesinde demo tohumu **yoktur**.
 *
 * Bu dosya `src/release/` altında; `src/debug/`deki gerçek tohumlayıcı
 * release derlemesine hiç girmiyor. Gerçek kullanıcı sahte veri göremez —
 * koruma bir bayrakta değil, kaynak setinde.
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoVeriModulu {

    @Provides
    @Singleton
    fun demoVeri(): DemoVeri = object : DemoVeri {
        override val kullanilabilir = false
        override suspend fun tohumla(gun: Int) = Unit
    }
}
