# AiTvRemote

This repository contains backend and web client starter code for the AiTvRemote project — a networked TV remote server and a simple React web UI.

What I added in this commit:

- backend/: Express + Socket.IO server that manages device registrations and relays remote commands.
- client/: Vite + React web UI that lists devices and sends remote commands.

How to run locally:

1. Start the backend:
   - cd backend
   - npm install
   - npm run start

2. Start the client:
   - cd client
   - npm install
   - npm run dev

The server listens on port 3001 by default, and the web client expects the API at http://localhost:3001.

Next steps I can take for you:
- Implement protocol adapters (Samsung Tizen, LG webOS, Cast, HDMI-CEC, IR blaster).
- Add persistent device storage and pairing flows.
- Open a PR with these changes and a CI workflow to build/tests.

If you want the files adapted to an Android Kotlin app (repository language is Kotlin) I can also add an Android module instead of the web client.
