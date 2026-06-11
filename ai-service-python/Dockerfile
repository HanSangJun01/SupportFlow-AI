FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml .
RUN pip install --no-cache-dir "fastapi[standard]==0.136.3" "pydantic==2.13.4"

COPY app ./app

EXPOSE 8000

CMD ["fastapi", "run", "app/main.py", "--host", "0.0.0.0", "--port", "8000"]
