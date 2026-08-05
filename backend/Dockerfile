# Yeşilİz API
#
# python:3.12 — makinedeki Python 3.9 ile geliştirmek yerine kap içinde
# sabit sürüm kullanılıyor; SQLAlchemy 2.0 ve modern FastAPI 3.9'da da
# çalışıyor ama sürüm farkı er geç "bende çalışıyordu" hatası üretir.
FROM python:3.12-slim

WORKDIR /uygulama

# Bağımlılıklar ayrı katmanda: kaynak değişince yeniden kurulmasın.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
RUN chmod +x entrypoint.sh

EXPOSE 8000

# Migration önce, backend sonra — entrypoint.sh migration başarısız olursa
# uvicorn'u hiç başlatmıyor (görev tanımı §4). docker-compose.yml kendi
# `command`'ıyla bunu eziyor (canlı yeniden yükleme için); bu CMD yalnızca
# Render gibi doğrudan imajı çalıştıran ortamlarda devreye giriyor.
CMD ["./entrypoint.sh"]
