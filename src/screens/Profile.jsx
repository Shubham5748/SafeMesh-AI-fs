import React from 'react';
import { User, Settings, Bell, Lock, HeartPulse, Shield } from 'lucide-react';

export default function Profile({ onOpenContacts }) {
  return (
    <div className="profile-screen screen-content">
      <header className="profile-header mb-6">
        <div className="profile-avatar-large">
          <User size={48} />
        </div>
        <h2 className="title mt-4">Shubham Chaubey</h2>
        <p className="subtitle">Premium Member</p>
      </header>

      <div className="medical-card glass-panel mb-6">
        <div className="medical-header">
          <HeartPulse className="text-red" />
          <h3>Medical Info</h3>
        </div>
        <div className="grid-2 mt-4">
          <div className="info-item">
            <span className="info-label">Blood Group</span>
            <span className="info-value">O+</span>
          </div>
          <div className="info-item">
            <span className="info-label">Allergies</span>
            <span className="info-value">Penicillin</span>
          </div>
        </div>
      </div>

      <div className="settings-list">
        <div onClick={onOpenContacts}>
          <SettingItem icon={Shield} label="Emergency Contacts" />
        </div>
        <SettingItem icon={Bell} label="Notifications" />
        <SettingItem icon={Lock} label="Privacy & Security" />
        <SettingItem icon={Settings} label="App Settings" />
      </div>
    </div>
  );
}

function SettingItem({ icon: Icon, label }) {
  return (
    <div className="glass-panel setting-item mb-3">
      <Icon size={20} className="text-blue" />
      <span>{label}</span>
      <div className="chevron">›</div>
    </div>
  );
}
