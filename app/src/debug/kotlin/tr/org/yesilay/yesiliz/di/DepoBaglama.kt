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
 * Arayüz → uygulama bağlamaları — **DEBUG**.
 *
 * `Yedekli*Deposu` bağlanıyor: sunucuya ulaşılamazsa `YerelSahte*` devreye
 * giriyor ve ekran bunu `cevrimdisi` bayrağıyla söylüyor (CLAUDE.md §12
 * K18). Gerekçe demo güvencesi — sunum salonunda ağ/laptop/kap üçü de
 * sorun çıkarabilir ve lig sekmesinin boş kalması özelliğin yokmuş gibi
 * görünmesi demek.
 *
 * Release eşi `src/release/.../di/DepoBaglama.kt`: orada sahte veri
 * sınıfları **hiç örneklenmiyor**.
 *
 * Mock repository deseni (CLAUDE.md §12 K7) hâlâ geçerli: bağlama
 * değişiyor, hiçbir ViewModel ya da Composable değişmiyor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DepoBaglama {

    @Binds
    @Singleton
    abstract fun ligDeposu(yedekli: YedekliLigDeposu): LigDeposu

    @Binds
    @Singleton
    abstract fun hesapDeposu(yedekli: YedekliHesapDeposu): HesapDeposu
}
