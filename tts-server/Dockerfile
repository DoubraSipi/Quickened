FROM python:3.11-slim

ENV PIPER_VOICE=${PIPER_VOICE:-en_US-ryan-high}
ENV PORT=${PORT:-8000}

RUN apt-get update && apt-get install -y --no-install-recommends \
    espeak-ng espeak-ng-data curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir fastapi uvicorn piper-tts

WORKDIR /srv
COPY server.py /srv/server.py

EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost:8000/health || exit 1
CMD ["sh", "-c", "uvicorn server:app --host 0.0.0.0 --port ${PORT}"]
