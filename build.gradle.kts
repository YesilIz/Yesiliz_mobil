// Kök build dosyası — burada modüllere ortak ayar YAZILMAZ.
// Her modül kendi build.gradle.kts'ini taşır (CLAUDE.md §3.5).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP kökte de bildirilmek ZORUNDA — yalnızca :app'te bildirilirse Hilt
    // Gradle eklentisi farklı bir sınıf yükleyiciye düşüp KSP'nin task
    // sınıfını bulamıyor ("KSP plugin was detected to be applied but its
    // task class could not be found"). google/dagger#3965.
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Ağ DTO'ları için (Faz 2 backend). KSP'yle aynı gerekçe: eklenti
    // kökte de bildirilmezse sürüm çözümlemesi modüle göre değişebiliyor.
    alias(libs.plugins.kotlin.serialization) apply false
}
