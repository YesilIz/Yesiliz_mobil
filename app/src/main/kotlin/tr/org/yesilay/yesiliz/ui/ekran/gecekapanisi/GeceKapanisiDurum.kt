package tr.org.yesilay.yesiliz.ui.ekran.gecekapanisi

/**
 * Gece kapanışı — yatmadan önceki kısa ritüel.
 *
 * Dört adım: telefonu şarja yatak dışına koy, yarına bir şey bırak,
 * bugünden bir iyi şey seç, ışığı kapat. Hepsi **beyan** ama tek tek
 * işaretleniyor: ritüelin kendisi maddeleri okumaktan geçiyor.
 *
 * `GECE_TEMIZ` görevine bağlanıyor: kapanışı yapan kullanıcının gecesi
 * zaten büyük olasılıkla temiz geçecek, ama puanlar ayrı — biri beyan,
 * öteki ölçüm.
 */
data class GeceKapanisiDurum(
    val isaretliler: Set<Int> = emptySet(),
    val tamamlandi: Boolean = false,
    val puanYazildi: Boolean = false,
) {
    val hepsiIsaretli: Boolean get() = isaretliler.size == ADIMLAR.size
}

/** Ritüel adımları — sabit, sunucudan gelmiyor. */
val ADIMLAR = listOf(
    "Telefonu yatağının uzağına, şarja koy" to
        "Kolun uzanabildiği yerdeyse gece yarısı bakmamak zor.",
    "Yarın için tek bir şey not et" to
        "Aklındaki işi bir yere bırakmak, onu düşünmeyi bırakmayı kolaylaştırıyor.",
    "Bugünden iyi geçen bir şey seç" to
        "Küçük olabilir. Amaç günü olumlu bir yerde kapatmak.",
    "Işığı kıs, ekranı bırak" to
        "Son yarım saat uykunun kalitesini en çok etkileyen aralık.",
)
