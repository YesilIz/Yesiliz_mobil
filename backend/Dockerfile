# Yeşilİz API
#
# python:3.12 — makinedeki Python 3.9 ile geliştirmek yerine kap içinde
# sabit sürüm kullanılıyor; SQLAlchemy 2.0 ve modern FastAPI 3.9'da da
# çalışıyor ama sürüm farkı er geç "bende çalışıyordu" hatası üretir.
FROM python:3.12-slim

WORKDIR /uygulama

# Python'un kap içindeki iki alışkanlığı: .pyc yazmasın, stdout'u
# tamponlamasın. İkincisi loglama için şart — tamponlanmış stdout,
# sağlayıcı panelinde logların dakikalarca geç görünmesi demek.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Bağımlılıklar ayrı katmanda: kaynak değişince yeniden kurulmasın.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
RUN chmod +x baslat.sh

# Yerelde 8000; üretimde sağlayıcı `PORT`'u enjekte ediyor ve baslat.sh
# onu okuyor. EXPOSE yalnızca belge değeri taşıyor.
EXPOSE 8000

# Migration → sunucu. Migration düşerse sunucu hiç başlamıyor (baslat.sh).
CMD ["./baslat.sh"]
