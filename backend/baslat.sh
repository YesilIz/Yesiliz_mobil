#!/bin/sh
# Kap giriş noktası — üretim. Aynı imaj iki servis koşuyor.
#
# **API rolü:** sıra pazarlık edilemez, önce migration sonra sunucu. `set -e`
# ile `alembic upgrade head` düşerse uvicorn hiç başlamıyor, sağlayıcının
# sağlık kontrolü zaman aşımına uğruyor ve yeni sürüm devreye ALINMIYOR.
# Böylece uygulama kodu ile veritabanı şeması farklı sürümlerde kalamıyor.
#
# **Panel rolü:** migration KOŞTURMUYOR. İki servis aynı anda `alembic
# upgrade` çağırırsa çakışırlar; migration'ın tek sahibi API. Panel yalnızca
# şemanın head'de olduğunu doğruluyor ve doğrulayamazsa aynı şekilde
# başlamıyor (`app/surum_kapisi.py`, docs/panel-plani.md §4).
#
# Rol shell'de değil **Python'da** çözülüyor: `YESILIZ_ROL`/`SERVER_ROLE`
# takma adları tek yerde (`ayarlar.py`) ele alınsın ve üretim yapılandırma
# kapısı migration'dan önce ateşlensin diye.
#
# Yerel geliştirmede kullanılmıyor: docker-compose kendi `command`'ını
# veriyor (--reload ile).
set -e

rol=$(python -c "from app.ayarlar import ayarlar; print(ayarlar().rol.value)")

if [ "$rol" = "panel" ]; then
  echo "rol=panel · migration atlanıyor, şema sürümü doğrulanıyor"
  python -m app.surum_kapisi
else
  echo "rol=$rol · migration: alembic upgrade head"
  alembic upgrade head
fi

# Railway/Render dinlenecek portu `PORT` ile enjekte ediyor; yerelde 8000.
exec uvicorn app.main:uygulama --host 0.0.0.0 --port "${PORT:-8000}"
