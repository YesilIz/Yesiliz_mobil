// core-risk — SAF KOTLIN (JVM). Android bağımlılığı YASAK (CLAUDE.md §3.5).
// Bu modül bilerek `kotlin-jvm` kullanıyor: android.* import'u derleyici
// seviyesinde imkânsız hâle gelsin diye. Testler emülatörsüz, saniyeler içinde koşar.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    // tests/test_vectors.json okunacak (CLAUDE.md §7 test kuralı)
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnit()
    // Vektör dosyası kökte (tests/), test çalışma dizini core-risk/ —
    // yolu göreli yazmak yerine sistem özelliğiyle geçiriyoruz.
    val vektorDosyasi = rootProject.file("tests/test_vectors.json")

    // GİRDİ OLARAK BİLDİRMEK ŞART — yoksa `vektor_uret.py` vektörleri
    // yeniden ürettiğinde Gradle testi UP-TO-DATE sayıp atlar ve
    // uyumsuzluk fark edilmez.
    inputs.file(vektorDosyasi).withPathSensitivity(PathSensitivity.RELATIVE)

    systemProperty("vektorDosyasi", vektorDosyasi.absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
    }
}
