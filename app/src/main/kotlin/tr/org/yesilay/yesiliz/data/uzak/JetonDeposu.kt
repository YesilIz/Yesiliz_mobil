package tr.org.yesilay.yesiliz.data.uzak

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.jetonDeposu: DataStore<Preferences> by preferencesDataStore(name = "jetonlar")

/**
 * Erişim ve yenileme jetonları.
 *
 * `Tercihler`'den **ayrı** bir DataStore: "verilerimi sil" akışı
 * (`VeriYonetimiDeposu`) tercihleri sıfırlıyor; oturum jetonlarının aynı
 * dosyada olması, veri silme ile çıkış yapmayı istemeden birbirine
 * bağlardı. Ayrıca jetonlar hesap ekranından bağımsız olarak da
 * temizlenebiliyor.
 *
 * **Şifre burada da yok** — yalnızca sunucunun verdiği jetonlar.
 */
@Singleton
class JetonDeposu @Inject constructor(
    @ApplicationContext private val baglam: Context,
) {
    private val depo get() = baglam.jetonDeposu

    suspend fun erisim(): String? = depo.data.first()[ERISIM]

    suspend fun yenileme(): String? = depo.data.first()[YENILEME]

    /**
     * Interceptor'ın senkron okuması.
     *
     * OkHttp interceptor'ı askıya alınabilir değil; `runBlocking` burada
     * kaçınılmaz. DataStore okuması bellekten geldiği için pratikte
     * bloklamıyor — ilk okuma dışında disk erişimi yok.
     */
    fun erisimSenkron(): String? = runBlocking { erisim() }

    suspend fun kaydet(erisim: String, yenileme: String) {
        depo.edit { p ->
            p[ERISIM] = erisim
            p[YENILEME] = yenileme
        }
    }

    suspend fun temizle() {
        depo.edit { it.clear() }
    }

    private companion object {
        val ERISIM = stringPreferencesKey("erisim")
        val YENILEME = stringPreferencesKey("yenileme")
    }
}
