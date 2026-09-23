function AppLayout({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Workspace nhóm</p>
          <p className="brand-name">Quản lý bán hàng & kho</p>
        </div>
        <span className="environment-label">Môi trường phát triển</span>
      </header>
      <main className="app-main">{children}</main>
      <footer className="app-footer">Khung sườn hệ thống · React + Express + MySQL</footer>
    </div>
  )
}

export default AppLayout
