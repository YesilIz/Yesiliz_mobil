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
        ?: "https://yesiliz-api.up.railway.app"
    ).trim().let { if (it.endsWith("/")) it else "$it/" }

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
        doFirst {
            require(adres.startsWith("https://")) {
                "Release derlemesi HTTPS adresi istiyor, gelen: $adres\n" +
                    "Adresi -Pyesiliz.sunucu=https://… ile ya da YESILIZ_SUNUCU " +
                    "ortam değişkeniyle verin (docs/dagitim.md)."
            }
        }
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
    debugImplementation(libs.okhttp.logging)

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
