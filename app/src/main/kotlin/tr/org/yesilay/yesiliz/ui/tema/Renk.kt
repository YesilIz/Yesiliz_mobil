package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.ui.graphics.Color

/**
 * DESIGN.md §1 renk tablosundan BİREBİR.
 * Kaynak: Türkiye Yeşilay Cemiyeti Marka Kılavuzu 2025.
 *
 * Renk değeri BURADAN BAŞKA hiçbir yerde yazılmaz (CLAUDE.md §7).
 * Stitch'in "Hopeful Green" sistemi Material 3 türevi ek tonlar üretiyor;
 * onlar Stitch içi tutarlılık içindir, buraya kopyalanmaz — DESIGN.md kaynak.
 */

// --- ANA ---
/** Pantone 354 C — ana marka yeşili */
val YesilAna = Color(0xFF00AE42)

/** Pantone 3435 C — koyu yeşil */
val YesilKoyu = Color(0xFF124734)

// --- DESTEK ---
/** ekran zemini */
val Yesil50 = Color(0xFFEDF7EF)

/** kart zemini, chip */
val Yesil200 = Color(0xFFC4DEC9)

/** orta ton, ikincil */
val Yesil500 = Color(0xFF70AB70)

// --- NÖTR ---
val Beyaz = Color(0xFFFFFFFF)
val MetinBirincil = Color(0xFF124734)
val MetinIkincil = Color(0xFF5A6B60)
val Ayrac = Color(0xFFE3EDE6)

// --- TESLİM EDİLEN TASARIMIN EKLEDİKLERİ ---
// docs/tasarim-referans.md §1. Değerler PNG'den piksel örneklemesiyle alındı;
// yukarıdaki altı token tasarımda BİREBİR çıktı, aşağıdakiler yeni.

/** kart üstü ikinci kademe yüzey — beyazdan bir tık kırık */
val YuzeyKirik = Color(0xFFFAFDFB)

/** ikon rozeti zemini, bilgi şeridi — Yesil50'den bir tık daha canlı */
val YesilNane = Color(0xFFE1F6EA)

/** grafik ekseni, "5 gün kaldı", meta metin — MetinIkincil'den açık */
val MetinUcuncul = Color(0xFF7B8A82)

// --- RİSK BANDI — KIRMIZI YOK ---
// Marka kılavuzu "karamsar değil pozitif renkler" diyor; riski kırmızıyla
// göstermek damgalayıcı olur. Koyulaşan yeşil kullanılır (CLAUDE.md §4).
/** AZ RİSKLİ */
val RiskAz = Color(0xFF00AE42)

/** ORTA RİSKLİ */
val RiskOrta = Color(0xFF70AB70)

/** ÇOK RİSKLİ */
val RiskCok = Color(0xFF124734)

// --- HATA / DOĞRULAMA — burada da kırmızı yok ---
// Material 3'ün varsayılan error paleti (#ba1a1a, #ffdad6) KULLANILMAZ.
// "Bu alan boş bırakılamaz" gibi form hataları koyu yeşille gösterilir.
val Hata = Color(0xFF124734)
val HataZemin = Color(0xFFC4DEC9)

// --- KIRMIZI İSTİSNASI (CLAUDE.md §12 K10) ---
// 30 Tem 2026 ek teslimi iki yerde kırmızı kullanıyor: risk analizi
// ekranındaki "Yüksek" etiketi/çubuğu ve hesap/veri kalıcı silme onayı.
// Kullanıcı bunun aynen uygulanmasına karar verdi — YALNIZCA bu iki
// bağlamda kullanılır, başka hiçbir yerde (genel risk/hata hâlâ yeşil).
val KirmiziIstisna = Color(0xFFC0392B)
val KirmiziIstisnaZemin = Color(0xFFFDECEB)

// --- GRADYANLAR ---
// Compose'un Brush'ı burada kurulamaz (boyuta bağlı); uç renkler burada,
// fırça `ui/bilesen/` içinde. Değerler tasarımdan örneklendi.

/** puan kartı — sol üst koyu, sağ alt açık */
val PuanKartiBas = Color(0xFF0B2B1D)
val PuanKartiSon = Color(0xFF3FB578)

/** ruh hali slider'ı: zorlanıyorum → harika. Turuncu, KIRMIZI DEĞİL. */
val RuhHaliBas = Color(0xFFEE9E5C)
val RuhHaliOrta = Color(0xFFF6D667)
val RuhHaliSon = YesilAna

// --- KATEGORİ PALETİ ---
// SADECE kategori dağılımı, adım grafiği ve lig avatarları.
// Risk/durum GÖSTERMEZ — bu yüzden "risk kırmızıyla gösterilmez" kuralıyla
// çelişmiyor. Bir kategori rengi asla bant rengi olarak kullanılamaz.
val KategoriSosyal = Color(0xFF3B8F42)
val KategoriEglence = Color(0xFF00989A)
val KategoriIletisim = Color(0xFF008BC7)
val KategoriOyun = Color(0xFF707ED4)
val KategoriUretkenlik = Color(0xFFDC8748)
val KategoriDiger = Color(0xFFAB78C1)

/** Adım grafiği kategori paletinden ödünç alıyor — ekran süresinden ayrışsın. */
val AdimMavi = KategoriIletisim
