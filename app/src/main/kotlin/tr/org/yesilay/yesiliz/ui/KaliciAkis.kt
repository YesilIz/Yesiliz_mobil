package tr.org.yesilay.yesiliz.ui

/**
 * Kalıcı yazmayı tamamlamadan sonucu gösterme/gezinme.
 *
 * Gezinme ViewModel'in sahibi olan back-stack girdisini silebilir ve
 * `viewModelScope` işini iptal edebilir. Bu sıra, "başarı" ekranının
 * henüz kaydedilmemiş bir işlemi tamamlanmış gibi göstermesini önler.
 */
internal suspend fun kalicilastirVeIlerle(
    kalicilastir: suspend () -> Unit,
    ilerle: () -> Unit,
) {
    kalicilastir()
    ilerle()
}
