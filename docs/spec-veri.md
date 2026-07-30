# spec-veri.md — Veri Stratejisi ve Ebeveyn Modu

> Kaynak: CLAUDE.md, taşındı 2026-07-24

---

## 12. VERİ STRATEJİSİ

### 12.1 Pasif girdiler

Ekran/uygulama süresi + kategori · kesintisiz bloklar · açılış sıklığı · gece kullanımı · adım/hareket (Health Connect) · telefon dokunulmama boşluğu → uyku tahmini

### 12.2 Aktif girdiler

**Widget veya bildirim panelinden** — uygulamayı AÇTIRMA:
- "Kaç saat uyudun?" (sabah, tek dokunuş)
- Ruh hali (emoji veya 1-5, günde 1)
- Spor başlat/bitir
- Antrenman sonrası his

**Uyku algoritması:** Telefon uzun süre dokunulmamış + gece saati → uyku varsayımı. Ama kullanıcı kitap okumuş olabilir. Tahmin **kesin veri işaretlenmez**, sabah doğrulanır.

### 12.3 Anket–davranış çapraz kontrolü

| Anket | Geçmiş veri | Anlam |
|---|---|---|
| Düşük | Düşük tüketim | Tutarlı, güven yüksek |
| Yüksek | Yüksek tüketim | Tutarlı, güven yüksek |
| **Düşük** | **Yüksek tüketim** | Farkındalık eksikliği — güven skorunu düşür, 2 haftada tekrar sor |
| **Yüksek** | **Düşük tüketim** | Kaygı odaklı olabilir; ya da yeni telefon |

### 12.4 Kalibrasyon yol haritası

**Faz 1 (şu an):** Kural tabanlı. ML YOK — eğitim verisi yok. Açıklanabilir, denetlenebilir, danışman onaylayabilir.

**Faz 2:** Veri biriktikçe istatistiksel türetme.

| Analiz | Çıktı |
|---|---|
| Betimsel | Yaygınlık |
| Cronbach α | Ölçüm güvenirliği |
| Korelasyon | Hangi değişken ilişkili |
| **Çoklu regresyon** | **Kategori ağırlıkları** |
| **ROC / Youden** | **S_REF ve bant sınırları** |

Örneklem: korelasyon ~85, regresyon ~120, ML için 300+.

**Faz 3:** Denetimli öğrenme. y = SAS-SV, X = öznitelikler. Lojistik/Ridge → XGBoost. Stratified 5-fold CV. SHAP.

> **Gerçekçi beklenti: %75-85 AUC iyi sonuçtur.** %95+ iddia eden çalışmalarda sentetik veri veya etiket sızıntısı var.

### 12.5 Chatbot neden yok

1. Yeşilay/YEDAM külliyatı geniş ama **ekran/teknoloji bağımlılığına özgü, danışmanlık amaçlı, yapılandırılmış dilim** RAG'i anlamlı kılacak derinlikte değil. Mevcut içerik farkındalık/tanıtım niteliğinde.
2. Kırılgan + reşit olmayan kullanıcıda **serbest metin etkileşiminin etik/güvenlik yükü** — kriz protokolü, kırmızı çizgiler, insan devri gerektirir.

LLM dar rolde kullanılabilir: rapor metni üretimi, nudge varyasyonu (önceden onaylanmış havuzdan). Kullanıcı serbest metin girmiyor.

---

## 13. EBEVEYN MODU (18 YAŞ ALTI)

- Kurulum ebeveyn onayıyla
- **Çocuk uygulamayı silerse ebeveyne bildirim** → **sunucu heartbeat** (X saat sinyal yoksa tetiklenir). Doğrudan silme tespiti iki platformda da güvenilir değil.
- Ebeveyne gösterilecek veri kapsamı **karar bekliyor** (§15)
