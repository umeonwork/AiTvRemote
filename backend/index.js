// Minimal Express + Socket.io server for TV remote commands
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });

// In-memory store for devices (id, name, lastSeen, socketId)
const devices = new Map();

// Pending pairing requests: id -> { pin, createdAt }
const pendingPairs = new Map();
const PAIR_TTL_MS = 1000 * 60 * 10; // 10 minutes

// Device registers via WebSocket or HTTP; WebSocket is preferred for command relay
io.on('connection', (socket) => {
  console.log('socket connected', socket.id);

  socket.on('register-device', (payload) => {
    const id = payload.id || socket.id;
    devices.set(id, {
      id,
      name: payload.name || `TV-${id.substring(0,6)}`,
      socketId: socket.id,
      lastSeen: Date.now(),
      meta: payload.meta || {},
    });
    console.log('device registered', id);
    io.emit('devices:update', Array.from(devices.values()));
  });

  socket.on('device-heartbeat', (payload) => {
    const entry = devices.get(payload.id);
    if (entry) {
      entry.lastSeen = Date.now();
      devices.set(payload.id, entry);
    }
  });

  socket.on('disconnect', () => {
    for (const [id, d] of devices.entries()) {
      if (d.socketId === socket.id) {
        devices.delete(id);
        console.log('device disconnected', id);
      }
    }
    io.emit('devices:update', Array.from(devices.values()));
  });
});

// HTTP: list devices
app.get('/api/devices', (req, res) => {
  res.json(Array.from(devices.values()));
});

// HTTP: register a device (optional, for devices that cannot open a socket)
app.post('/api/devices', (req, res) => {
  const { id, name, meta } = req.body;
  devices.set(id, { id, name, socketId: null, lastSeen: Date.now(), meta: meta || {} });
  io.emit('devices:update', Array.from(devices.values()));
  res.status(201).json({ ok: true });
});

// Pairing endpoints
app.post('/api/pair/request', (req, res) => {
  // Create a pairing id and PIN for a new pairing flow.
  const id = uuidv4();
  const pin = Math.floor(100000 + Math.random() * 900000).toString();
  pendingPairs.set(id, { pin, createdAt: Date.now() });
  console.log('pair requested', id, pin);
  res.json({ id, pin });
});

app.get('/api/pair/status/:id', (req, res) => {
  const id = req.params.id;
  const pair = pendingPairs.get(id);
  const deviceRegistered = devices.has(id);
  const confirmed = deviceRegistered; // device registration confirms pairing
  res.json({ confirmed, deviceRegistered, pair: pair ? { pin: pair.pin, createdAt: pair.createdAt } : null });
});

// HTTP: send command to device by id (will be emitted over socket.io if connected)
app.post('/api/devices/:id/command', (req, res) => {
  const id = req.params.id;
  const { command, params } = req.body;
  const device = devices.get(id);
  if (!device) return res.status(404).json({ error: 'device not found' });

  if (device.socketId) {
    io.to(device.socketId).emit('remote:command', { command, params });
    return res.json({ ok: true, sent: true });
  } else {
    // If device isn't connected, queue or return error — here we return not connected
    return res.status(409).json({ error: 'device not connected' });
  }
});

// Example server-side command handler (bridge to actual TV protocol)
function sendToTvBridge(device, command, params) {
  // TODO: implement protocol-specific send:
  // - For Samsung: websocket pairing / REST
  // - For LG: webOS websocket
  // - For HDMI-CEC: through a local CEC adapter
  // - For IR blaster: forward to a hardware module
  //
  // This function is intentionally left as a small hook to implement per-device logic.
}

// Cleanup old pending pairs
setInterval(() => {
  const now = Date.now();
  for (const [id, p] of pendingPairs.entries()) {
    if (now - p.createdAt > PAIR_TTL_MS) {
      pendingPairs.delete(id);
    }
  }
}, 1000 * 60);

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => console.log(`server listening on ${PORT}`));
