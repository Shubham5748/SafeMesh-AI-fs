import React, { useState, useEffect } from 'react';
import { Activity, MapPin, Wifi, Bluetooth, Users, ShieldAlert, X } from 'lucide-react';

export default function Home({ onSosTrigger, onOpenMap }) {
  return (
    <div className="home-screen screen-content">
      <header className="header">
        <div>
          <h2 className="greeting">Hello, Shubham</h2>
          <p className="status text-green">Safe & Protected</p>
        </div>
        <div className="avatar">S</div>
      </header>

      <div className="status-grid">
        <div onClick={onOpenMap} style={{ cursor: 'pointer' }}>
          <StatusCard icon={MapPin} label="GPS (Tap for Map)" value="Active" status="good" />
        </div>
        <StatusCard icon={Wifi} label="Network" value="Connected" status="good" />
        <StatusCard icon={Bluetooth} label="BLE Mesh" value="3 Nodes" status="good" />
        <StatusCard icon={Activity} label="AI Guard" value="Monitoring" status="good" />
      </div>

      <div className="sos-container">
        <div className="pulse-ring ring-1"></div>
        <div className="pulse-ring ring-2"></div>
        <button className="sos-button" onClick={onSosTrigger}>
          <ShieldAlert size={48} />
          <span>SOS</span>
        </button>
      </div>

      <section className="quick-info">
        <h3 className="section-title">Nearby Relays</h3>
        <div className="glass-panel relay-card">
          <div className="relay-info">
            <Users size={20} className="text-blue" />
            <span>3 SafeMesh devices nearby</span>
          </div>
          <span className="signal-strength">Strong</span>
        </div>
      </section>
    </div>
  );
}

function StatusCard({ icon: Icon, label, value, status }) {
  return (
    <div className="glass-panel status-card">
      <Icon size={20} className={status === 'good' ? 'text-green' : 'text-red'} />
      <div className="status-details">
        <span className="status-label">{label}</span>
        <span className="status-value">{value}</span>
      </div>
    </div>
  );
}
