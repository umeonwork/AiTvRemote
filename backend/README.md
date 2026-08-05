# Backend

Minimal Express + Socket.IO server used by the AiTvRemote example.

Run locally:

1. cd backend
2. npm install
3. npm run start

The server exposes:
- GET /api/devices — list known devices
- POST /api/devices — register a device (optional)
- POST /api/devices/:id/command — send a command to a device

Devices should connect via WebSocket and emit `register-device` with { id, name, meta }.
