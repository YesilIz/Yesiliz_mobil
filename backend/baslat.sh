#!/bin/sh
# Kap giriş noktası — üretim.
#
# Sıra pazarlık edilemez: **önce migration, sonra sunucu.** `set -e` ile
# `alembic upgrade head` düşerse uvicorn hiç başlamıyor, sağlayıcının
# sağlık kontrolü zaman aşımına uğruyor ve yeni sürüm devreye ALINMIYOR.
# Böylece uygulama kodu ile veritabanı şeması farklı sürümlerde kalamıyor.
#
# Yerel geliştirmede kullanılmıyor: docker-compose kendi `command`'ını
# veriyor (--reload ile).
set -e

echo "migration: alembic upgrade head"
alembic upgrade head

# Railway/Render dinlenecek portu `PORT` ile enjekte ediyor; yerelde 8000.
exec uvicorn app.main:uygulama --host 0.0.0.0 --port "${PORT:-8000}"
