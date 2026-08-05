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

// Pending pairing requests: id -> { pin, createdAt, confirmed?, token?, deviceId? }
const pendingPairs = new Map();
const PAIR_TTL_MS = 1000 * 60 * 10; // 10 minutes

// Mapping of pairing tokens -> device id
const pairingTokens = new Map();

// Device registers via WebSocket or HTTP; WebSocket is preferred for command relay
io.on('connection', (socket) => {
  console.log('socket connected', socket.id);

  socket.on('register-device', (payload) => {
    // Expect payload: { id, name, meta, pin? }
    const id = payload.id || socket.id;
    const pin = payload.pin || payload.pairingPin;

    // If a pending pair exists for this id, and a pin was provided, validate it
    if (pendingPairs.has(id)) {
      const entry = pendingPairs.get(id);
      if (!pin) {
        socket.emit('register:error', { error: 'pairing pin required for this id' });
        return;
      }
      if (entry.pin !== pin) {
        socket.emit('register:error', { error: 'invalid pairing pin' });
        return;
      }

      // Pin matches: finalize pairing
      const token = uuidv4();
      pairingTokens.set(token, id);
      entry.confirmed = true;
      entry.token = token;
      entry.deviceId = id;
      pendingPairs.set(id, entry);

      // Register device
      devices.set(id, {
        id,
        name: payload.name || `TV-${id.substring(0,6)}`,
        socketId: socket.id,
        lastSeen: Date.now(),
        meta: payload.meta || {},
      });

      console.log('device paired & registered', id, 'token=', token);

      // Notify the device of success and token
      socket.emit('pair:confirmed', { token });
      io.emit('devices:update', Array.from(devices.values()));
      return;
    }

    // No pending pair: accept registration without pairing (legacy or manual register)
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
// Supports pairing by providing { id, name, meta, pin }
app.post('/api/devices', (req, res) => {
  const { id, name, meta, pin } = req.body;
  if (!id) return res.status(400).json({ error: 'id required' });

  if (pendingPairs.has(id)) {
    const entry = pendingPairs.get(id);
    if (!pin) return res.status(400).json({ error: 'pairing pin required' });
    if (entry.pin !== pin) return res.status(403).json({ error: 'invalid pairing pin' });

    // finalize pairing and generate token
    const token = uuidv4();
    pairingTokens.set(token, id);
    entry.confirmed = true;
    entry.token = token;
    entry.deviceId = id;
    pendingPairs.set(id, entry);

    devices.set(id, { id, name: name || `TV-${id.substring(0,6)}`, socketId: null, lastSeen: Date.now(), meta: meta || {} });
    io.emit('devices:update', Array.from(devices.values()));
    return res.status(201).json({ ok: true, token });
  }

  devices.set(id, { id, name, socketId: null, lastSeen: Date.now(), meta: meta || {} });
  io.emit('devices:update', Array.from(devices.values()));
  res.status(201).json({ ok: true });
});

// Pairing endpoints
app.post('/api/pair/request', (req, res) => {
  // Create a pairing id and PIN for a new pairing flow.
  const id = uuidv4();
  const pin = Math.floor(100000 + Math.random() * 900000).toString();
  pendingPairs.set(id, { pin, createdAt: Date.now(), confirmed: false });
  console.log('pair requested', id, pin);
  res.json({ id, pin });
});

app.get('/api/pair/status/:id', (req, res) => {
  const id = req.params.id;
  const pair = pendingPairs.get(id);
  const deviceRegistered = devices.has(id);
  const confirmed = pair ? !!pair.confirmed : false;
  res.json({ confirmed, deviceRegistered, token: pair && pair.confirmed ? pair.token : null });
});

// HTTP: send command to device by id (will be emitted over socket.io if connected)
// Requires header 'X-Pair-Token' containing the pairing token for the device
app.post('/api/devices/:id/command', (req, res) => {
  const id = req.params.id;
  const { command, params } = req.body;
  const token = req.headers['x-pair-token'];

  if (!token) return res.status(401).json({ error: 'pairing token required' });
  const mapped = pairingTokens.get(token);
  if (!mapped || mapped !== id) return res.status(403).json({ error: 'invalid pairing token' });

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
