import AppLayout from '../layouts/AppLayout';

function NotFound() {
  return (
    <AppLayout>
      <div style={{ textAlign: 'center', padding: '60px 20px' }}>
        <h1 style={{ fontSize: '72px', margin: 0, color: '#ef4444' }}>404</h1>
        <h2>Không tìm thấy trang</h2>
        <p style={{ color: '#6b7280', maxWidth: '480px', margin: '0 auto 24px' }}>
          Đường dẫn bạn truy cập không tồn tại hoặc đã bị đổi địa chỉ.
        </p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button
            onClick={() => window.history.back()}
            style={{ padding: '10px 20px', border: '1px solid #d1d5db', borderRadius: '6px', backgroundColor: '#fff', cursor: 'pointer' }}
          >
            Quay lại trang trước
          </button>
          <a
            href="/"
            style={{ padding: '10px 20px', backgroundColor: '#2563eb', color: '#fff', textDecoration: 'none', borderRadius: '6px', fontWeight: 'bold' }}
          >
            Về trang chủ
          </a>
        </div>
      </div>
    </AppLayout>
  );
}

export default NotFound;