// `java.util.Properties` açıkça import ediliyor: Kotlin DSL'de `java`
// Gradle'ın kendi uzantısına bağlanıyor ve paket adını gölgeliyor.
import java.util.Properties
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

// AGP 9'da Kotlin desteği eklentinin İÇİNDE — `org.jetbrains.kotlin.android`
// artık uygulanmıyor, uygulanırsa çakışıyor. Kotlin ayarları `android { }`
// bloğundaki `kotlin { }` alt bloğundan yapılır.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Ağ DTO'ları @Serializable — Retrofit'in kotlinx converter'ı bunu
    // şart koşuyor. Eklenti AGP 9 ile çakışmıyor (§9'daki tuzak
    // `kotlin.android` eklentisiyle ilgili, bu ondan bağımsız).
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Canlı backend adresi — release derlemesine gömülüyor.
 *
 * Öncelik: `-Pyesiliz.sunucu=…` › `YESILIZ_SUNUCU` ortam değişkeni ›
 * buradaki varsayılan. Alan adı değişirse **tek yer** burası; adres
 * uygulama koduna dağınık şekilde yazılmıyor (`BuildConfig.API_TABANI`).
 *
 * Sondaki eğik çizgi Retrofit için şart, elle yazılmasına bırakılmıyor.
 */
val canliSunucu: String = (
    (findProperty("yesiliz.sunucu") as String?)
        ?: System.getenv("YESILIZ_SUNUCU")
        ?: "https://yesilizmobil-production.up.railway.app"
    ).trim().let { if (it.endsWith("/")) it else "$it/" }

/**
 * İmza bilgileri — depoya **girmiyor**.
 *
 * Öncelik: ortam değişkeni › `local.properties`. İkisi de yoksa `null`
 * dönüyor ve release imzasız üretiliyor (aşağıda). Bu bilerek böyle:
 * keystore'u olmayan bir ekip arkadaşının `assembleRelease`'i "dosya
 * bulunamadı" ile düşmesin, sadece kurulamayan bir APK üretsin.
 */
fun imzaDegeri(ortamAdi: String, ozellikAdi: String): String? =
    System.getenv(ortamAdi)
        ?: rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { dosya ->
                Properties()
                    .apply { dosya.inputStream().use { load(it) } }
                    .getProperty(ozellikAdi)
            }

// Yol **depo köküne** göre çözülüyor. `file()` modül dizinine (`app/`)
// göre çözerdi ve `app/app/…` arayıp sessizce imzasız APK üretirdi.
val imzaDeposu: File? =
    imzaDegeri("YESILIZ_IMZA_DEPOSU", "yesiliz.imza.deposu")
        ?.let { rootProject.file(it) }
        ?.takeIf { it.exists() }

val imzaParolasi: String? = imzaDegeri("YESILIZ_IMZA_PAROLASI", "yesiliz.imza.parolasi")
val imzaTakmaAdi: String? = imzaDegeri("YESILIZ_IMZA_TAKMA_ADI", "yesiliz.imza.takmaAdi")

android {
    namespace = "tr.org.yesilay.yesiliz"
    // compileSdk 37: AndroidX 2026 sürümleri (core-ktx 1.19, lifecycle 2.11)
    // 37'ye derlemeyi ŞART koşuyor. AGP 9.3.1 ile birlikte geldi.
    compileSdk = 37

    defaultConfig {
        applicationId = "tr.org.yesilay.yesiliz"
        // minSdk 26: Health Connect istemcisi 26 istiyor, Glance 23,
        // UsageStatsManager 21. En kısıtlayıcı olan belirledi.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    // İmzasız APK telefona KURULAMIYOR — Android her paketten kriptografik
    // imza şart koşuyor. Debug'da Android Studio kendi anahtarını üretiyor,
    // release'de üretmiyor; bu blok olmadan `assembleRelease` çıktısı
    // `app-release-unsigned.apk` oluyor ve gerçek cihaz doğrulaması
    // (`gorev1.md` son iki kriter) hiç yapılamıyor.
    //
    // Keystore ve parolalar depoda değil: `local.properties` (gitignore'da)
    // ya da ortam değişkeni. Değerler yoksa blok hiç tanımlanmıyor.
    signingConfigs {
        if (imzaDeposu != null && imzaParolasi != null && imzaTakmaAdi != null) {
            create("release") {
                storeFile = imzaDeposu
                storePassword = imzaParolasi
                keyAlias = imzaTakmaAdi
                keyPassword = imzaParolasi
            }
        }
    }

    // Sunucu adresi build türüne göre: debug yerel/test sunucusu, release
    // gerçek HTTPS adresi. Tek yapılandırma noktası — `Tercihler` ve
    // `AgModulu` buradan okuyor, koda ikinci bir adres yazılmıyor.
    buildTypes {
        debug {
            // Emülatörün geliştirme makinesine baktığı adres
            // (`backend/docker-compose.yml`). Gerçek cihazda Profil ›
            // Geliştirici'den LAN adresiyle değiştiriliyor.
            buildConfigField("String", "API_TABANI", "\"http://10.0.2.2:8000/\"")
        }
        release {
            buildConfigField("String", "API_TABANI", "\"$canliSunucu\"")
            // Bulunamazsa imzasız kalıyor; aşağıdaki `assembleRelease`
            // kontrolü bunu derleme sonunda söylüyor.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // AGP 8'den beri varsayılan kapalı; `BuildConfig.API_TABANI`
        // üretilsin diye açık.
        buildConfig = true
    }

    // CLAUDE.md §3.5: kaynaklar java/ değil kotlin/ altında.
    // debug/release ayrı: demo tohumu YALNIZCA debug'da derlenir, release'de
    // yerine no-op geçer (data/demo/DemoVeri.kt).
    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["debug"].kotlin.srcDir("src/debug/kotlin")
    sourceSets["release"].kotlin.srcDir("src/release/kotlin")
    sourceSets["androidTest"].kotlin.srcDir("src/androidTest/kotlin")

    // MigrationTestHelper eski şema JSON'larını assets'ten okuyor —
    // `room.schemaLocation` çıktısı testlere de verilmeli.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// Release çıktısı şifresiz adresle üretilemez.
//
// Kontrol yapılandırma anında değil, yalnızca release derlenirken koşuyor:
// aksi hâlde `-Pyesiliz.sunucu` vermeyen her `assembleDebug` de düşerdi.
// Yayınlanan APK'ya http:// adres gömülmesi, ağ güvenliği yapılandırması
// zaten şifresiz trafiği reddettiği için sessiz bir "sunucuya hiç
// ulaşamıyor" hatasına dönüşürdü.
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        // Adres yerel değişkene alınıyor: `doFirst` bloğu doğrudan
        // `canliSunucu`'ya baksaydı build script nesnesini yakalardı ve
        // yapılandırma önbelleği bunu serileştiremiyor
        // ("cannot serialize Gradle script object references").
        val adres = canliSunucu
        val imzali = imzaDeposu != null && imzaParolasi != null && imzaTakmaAdi != null
        doFirst {
            require(adres.startsWith("https://")) {
                "Release derlemesi HTTPS adresi istiyor, gelen: $adres\n" +
                    "Adresi -Pyesiliz.sunucu=https://… ile ya da YESILIZ_SUNUCU " +
                    "ortam değişkeniyle verin (docs/dagitim.md)."
            }
            // Uyarı, hata değil: keystore'u olmayan biri de derleyebilsin.
            // Ama sessiz kalmıyor — imzasız APK telefona kurulmuyor ve
            // sebebi çıktı adından ("-unsigned") anlaşılmıyor.
            if (!imzali) {
                logger.warn(
                    "UYARI: imza bilgisi bulunamadı, release APK İMZASIZ " +
                        "üretiliyor ve telefona KURULAMAZ. Keystore ve " +
                        "parolalar local.properties'ten okunuyor " +
                        "(docs/dagitim.md §10).",
                )
            }
        }
    }

// gorev2.md §11 "Release APK içeriğini kontrol eden otomatik bir test
// eklenmeli ve yasaklı debug adreslerinin bulunmadığı doğrulanmalıdır."
//
// `android-ağ-güvenliği` CI'ı (`.github/workflows/android-ag-guvenligi.yml`)
// KAYNAK düzeyinde bakıyor — `.kt` dosyalarında "10.0.2.2" string sabiti
// arıyor. Bu görev daha güçlü bir garanti: gerçekten üretilmiş **APK
// dosyasının içine** (dex + kaynaklar) bakıyor, üçüncü parti bir
// bağımlılığın ya da üretilen kodun sızdırdığı bir durumu da yakalar.
// `assembleRelease` sonrası `dogrulaReleaseApk` ile tetiklenir.
abstract class ReleaseApkAgGuvenligiDenetimi : DefaultTask() {

    @get:InputDirectory
    abstract val apkDizini: DirectoryProperty

    /** Bulunursa derleme düşer — emülatör adresi ya da benzeri. */
    @get:Input
    abstract val yasakliDesenler: ListProperty<String>

    /** Bulunmazsa derleme düşer — gerçek HTTPS adresinin gömüldüğünün kanıtı. */
    @get:Input
    abstract val zorunluDesen: Property<String>

    @TaskAction
    fun denetle() {
        val apkler = apkDizini.get().asFile.listFiles { f -> f.extension == "apk" }.orEmpty()
        if (apkler.isEmpty()) {
            throw org.gradle.api.GradleException(
                "Denetlenecek release APK bulunamadı: ${apkDizini.get().asFile}",
            )
        }

        val yasakli = yasakliDesenler.get()
        val zorunlu = zorunluDesen.get()

        apkler.forEach { apk ->
            var zorunluBulundu = false
            ZipFile(apk).use { zip ->
                zip.entries().asSequence().filterNot { it.isDirectory }.forEach { girdi ->
                    val icerik = String(zip.getInputStream(girdi).readBytes(), Charsets.ISO_8859_1)
                    yasakli.forEach { desen ->
                        if (icerik.contains(desen)) {
                            throw org.gradle.api.GradleException(
                                "Release APK'da yasaklı desen bulundu: '$desen' → " +
                                    "${apk.name}!${girdi.name}. Bu adres yalnızca " +
                                    "emülatörde/debug'da anlamlı, app/src/debug altına " +
                                    "taşınmalı (CLAUDE.md §9).",
                            )
                        }
                    }
                    if (!zorunluBulundu && icerik.contains(zorunlu)) zorunluBulundu = true
                }
            }
            if (!zorunluBulundu) {
                throw org.gradle.api.GradleException(
                    "Release APK'da beklenen canlı sunucu adresi ('$zorunlu') " +
                        "bulunamadı → ${apk.name}. BuildConfig.API_TABANI yanlış " +
                        "gömülmüş olabilir.",
                )
            }
        }
        logger.lifecycle(
            "Release APK ağ güvenliği denetimi geçti: ${apkler.size} APK, " +
                "yasaklı desen yok, canlı adres gömülü.",
        )
    }
}

// Config-cache güvenli: `canliSunucu` yerel değere alınıp task'a Provider
// olarak veriliyor, task action'ı build script nesnesine dokunmuyor
// (aynı gerekçe, satır ~160'taki `assembleRelease` doFirst bloğu).
tasks.register<ReleaseApkAgGuvenligiDenetimi>("dogrulaReleaseApk") {
    group = "verification"
    description = "Release APK'da emülatör adresi/şifresiz HTTP yok, canlı HTTPS adresi gömülü mü denetler (gorev2.md §11)."
    dependsOn("assembleRelease")
    apkDizini.set(layout.buildDirectory.dir("outputs/apk/release"))
    yasakliDesenler.set(listOf("10.0.2.2"))
    zorunluDesen.set(canliSunucu)
}

// Room şemasını JSON olarak dışa aktar.
// DB.md bu dosyadan ÜRETİLİYOR (tools/db_dokuman_uret.py) — şema değişince
// doküman da değişsin, elle yazılan şema dokümanı er geç yalan söyler.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core-risk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Tek Activity + NavHost (CLAUDE.md §6). ViewModel'ler henüz Hilt'siz —
    // `viewModel()` fabrikası yetiyor, DI sırası arka plan işleriyle gelecek.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // assets/kategori_tablosu.json çözümlemesi
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Ana ekran widget'ları (tasarım 39, 40). Glance kendi Composable
    // ağacını kullanıyor — `androidx.compose.*` bileşenleri widget içinde
    // çalışmıyor, o yüzden widget'lar ui/bilesen'i paylaşmıyor. Renkler
    // yine de ui/tema'dan okunuyor (CLAUDE.md §7).
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // DI — CLAUDE.md §5. `hilt-work` @HiltWorker için; WorkManager Faz 4'te
    // gelecek ama fabrika Application'da baştan kuruluyor ki sonradan
    // manifest/Application değişikliği gerekmesin.
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.work.runtime.ktx)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.datastore.preferences)

    // Faz 2 backend'i (backend/). Sunucu erişilemezse depolar yerel mock'a
    // düşüyor (`Yedekli*Deposu`) — demo günü backend kapalıyken de çalışır.
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    // `implementation`, `debugImplementation` değil: AgModulu (src/main) her
    // iki derleme türünde de HttpLoggingInterceptor'ı kullanabilmeli.
    // Release'de gerçek loglama BuildConfig.DEBUG kontrolüyle kapalı kalıyor
    // — sınıf sınıf yolunda olsa da hiçbir şey loglamıyor.
    implementation(libs.okhttp.logging)

    // Adım + egzersiz süresi → aktivite sönümlemesi (%15 tavan, spec-risk §4.4).
    // Cihazda Health Connect yoksa okuyucu sessizce sıfır döndürüyor.
    implementation(libs.health.connect.client)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(project(":core-risk"))

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
}

tasks.withType<Test>().configureEach {
    // Birim testler assets'i ve ortak vektör dosyasını doğrudan okur —
    // emülatör/Robolectric gerekmesin diye yollar sistem özelliğiyle geçiliyor.
    val kategoriTablosu = file("src/main/assets/kategori_tablosu.json")
    val vektorDosyasi = rootProject.file("tests/test_vectors.json")

    // GİRDİ OLARAK BİLDİRMEK ŞART. Yalnızca systemProperty verilirse Gradle
    // bu dosyaları görmez, değiştiklerinde testi UP-TO-DATE sayıp atlar —
    // yani tabloyu bozup testi yeşil görürsün. Bir kez böyle yakalandı.
    inputs.file(kategoriTablosu).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(vektorDosyasi).withPathSensitivity(PathSensitivity.RELATIVE)

    systemProperty("kategoriTablosu", kategoriTablosu.absolutePath)
    systemProperty("vektorDosyasi", vektorDosyasi.absolutePath)
    testLogging { events("passed", "failed") }
}
