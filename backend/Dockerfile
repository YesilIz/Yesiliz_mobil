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

EXPOSE 8000

CMD ["uvicorn", "app.main:uygulama", "--host", "0.0.0.0", "--port", "8000"]
