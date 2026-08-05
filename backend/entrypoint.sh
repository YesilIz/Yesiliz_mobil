#!/bin/sh
# Her dağıtımda migration önce çalışır. `set -e`: migration başarısız
# olursa script burada durur, uvicorn hiç başlamaz — eski sürüm ayakta
# kalır, yarım güncellenmiş bir şema ile karışık kod çalışmaz
# (görev tanımı §4).
set -e

echo "Migration'lar uygulanıyor..."
alembic upgrade head

echo "Backend başlatılıyor..."
exec uvicorn app.main:uygulama --host 0.0.0.0 --port 8000
