"""Quickened Studio Voice: Piper HTTP microservice.

POST /synthesize {"text": "...", "voice": "en_US-ryan-high"}
  -> audio/wav bytes (16-bit monoREF).

Env:
  PIPER_VOICE   default voice id (default: en_US-ryan-high)
  PIPER_DATA    voice storage dir (default: /srv/voices)
  API_KEY       optional shared secret; send as X-API-Key header
"""
import io
import os
import wave

from fastapi import FastAPI, Header, HTTPException, Response
from fastapi.responses import JSONResponse
from piper import PiperVoice

VOICE = os.environ.get("PIPER_VOICE", "en_US-ryan-high")
DATA_DIR = os.environ.get("PIPER_DATA", "/srv/voices")
API_KEY = os.environ.get("API_KEY", "")

app = FastAPI(title="Quickened Studio Voice")
_voice = None


def get_voice():
    global _voice
    if _voice is None:
        _voice = PiperVoice.load(os.path.join(DATA_DIR, VOICE + ".onnx"))
    return _voice


@app.get("/health")
def health():
    return {"ok": True, "voice": VOICE}


@app.get("/voices")
def voices():
    return {"default": VOICE}


@app.post("/synthesize")
def synthesize(body: dict, x_api_key: str = Header(default="")):
    if API_KEY and x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="bad key")
    text = (body.get("text") or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="empty text")
    if len(text) > 2000:
        raise HTTPException(status_code=400, detail="text too long")
    voice = get_voice()
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(voice.config.sample_rate)
        for chunk in voice.synthesize(text):
            w.writeframes(chunk.audio_int16_bytes)
    return Response(content=buf.getvalue(), media_type="audio/wav")


@app.get("/openapi-check")
def openapi_check():
    return JSONResponse({"routes": ["/health", "/voices", "/synthesize"]})
