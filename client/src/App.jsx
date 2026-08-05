import React, { useEffect, useState } from 'react';
import { io } from 'socket.io-client';

const API = import.meta.env.VITE_API || 'http://localhost:3001';
const socket = io(API);

export default function App() {
  const [devices, setDevices] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    async function fetchDevices() {
      try {
        const r = await fetch(`${API}/api/devices`);
        setDevices(await r.json());
      } catch (e) {
        console.error('failed to fetch devices', e);
      }
    }
    fetchDevices();

    socket.on('connect', () => console.log('ws connected'));
    socket.on('devices:update', (list) => setDevices(list));

    return () => {
      socket.off('devices:update');
    };
  }, []);

  const sendCommand = async (cmd) => {
    if (!selected) return alert('Select a device first');
    await fetch(`${API}/api/devices/${selected}/command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: cmd }),
    });
  };

  return (
    <div className="app">
      <aside className="sidebar">
        <h3>Devices</h3>
        <ul>
          {devices.map(d => (
            <li key={d.id} className={selected === d.id ? 'selected' : ''} onClick={() => setSelected(d.id)}>
              <div className="name">{d.name}</div>
              <div className="meta">{d.socketId ? 'online' : 'offline'}</div>
            </li>
          ))}
        </ul>
      </aside>

      <main className="remote">
        <h2>Remote {selected ? `(to ${devices.find(d=>d.id===selected)?.name})` : ''}</h2>
        <div className="buttons-grid">
          {['power','up','down','left','right','ok','back','home','volume_up','volume_down','mute'].map(b => (
            <button key={b} className="btn" onClick={() => sendCommand(b)}>{b.replace('_',' ')}</button>
          ))}
        </div>
      </main>
    </div>
  );
}
