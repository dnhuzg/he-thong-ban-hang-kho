import { logout } from '../api/authService';

function AppLayout({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Workspace nhóm</p>
          <p className="brand-name">Quản lý bán hàng & kho</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span className="environment-label">Môi trường phát triển</span>
          <button 
            onClick={logout} 
            style={{
              padding: '6px 12px',
              backgroundColor: '#ef4444',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            Đăng xuất
          </button>
        </div>
      </header>
      <main className="app-main">{children}</main>
      <footer className="app-footer">Khung sườn hệ thống · React + Express + MySQL</footer>
    </div>
  );
}

export default AppLayout;