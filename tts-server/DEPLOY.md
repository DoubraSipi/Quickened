# Quickened Studio Voice — deploy guide
#
# What this is: a small Piper HTTP service that renders top-quality voice
# audio for the app. The app caches every render, so each moment needs
# internet ONCE, then plays offline forever.
#
# 1) Create account: fly.io (or Railway/Render — same Dockerfile works).
# 2) Install flyctl, then:  fly launch --no-deploy --name quickened-voice
# 3) Set a secret so only your app can use it:
#      fly secrets set API_KEY=<long-random-string>
# 4) Deploy:  fly deploy
#    First boot downloads the voice model automatically (piper-tts handles it).
# 5) Note your URL:  https://quickened-voice.fly.dev
# 6) In the app: Settings → Cloud backup section → Studio voice URL +
#    Studio key → Save. Then pick the ✦ Studio voice in Voice list.
#
# Cost note: smallest instance renders a verse in ~1-3s. Scale to zero
# between uses if your host supports it.
