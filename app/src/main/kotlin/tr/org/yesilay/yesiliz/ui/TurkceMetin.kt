package tr.org.yesilay.yesiliz.ui

import java.util.Locale

/** Türkçe yerel ayarı — `i ↔ İ` ve `ı ↔ I` eşlemesi için. */
private val TR: Locale = Locale.forLanguageTag("tr")

/**
 * Türkçe büyük harfe çevirir.
 *
 * **Neden gerekli:** Kotlin'in `uppercase()` fonksiyonu `Locale.ROOT` kullanır,
 * yani `"i" → "I"`. Türkçede doğrusu `"i" → "İ"`. Varsayılanla çevrilirse
 * arayüzde "TIPOGRAFI", "RISK BANDI", "ILERLEME" gibi bozuk metinler çıkar —
 * bu hata prototipte bir kez yapıldı ve emülatör ekran görüntüsünde yakalandı.
 *
 * Aynı tuzak `lowercase()` için de geçerli: `"I".lowercase()` ROOT'ta `"i"`
 * verir, Türkçede `"ı"` olmalı.
 *
 * ```
 * "Tipografi".uppercase()     // TIPOGRAFI  ← YANLIŞ
 * "Tipografi".buyukHarf()     // TİPOGRAFİ  ← doğru
 * ```
 */
fun String.buyukHarf(): String = uppercase(TR)

/** Türkçe küçük harfe çevirir. `"IŞIK".kucukHarf()` → `"ışık"`. */
fun String.kucukHarf(): String = lowercase(TR)
