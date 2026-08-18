"""
Panel altyapısı — yalnızca `YESILIZ_ROL=panel` servisinde yükleniyor.

Router'lar bu pakette değil `app/uclar/panel_*.py`'de: depodaki bütün
router'lar orada duruyor ve panel bu konvansiyondan ayrılmıyor. Burada
oturum, CSRF, güvenlik başlıkları, şablon yardımcısı, denetim kaydı ve
grafik üretimi var.
"""
