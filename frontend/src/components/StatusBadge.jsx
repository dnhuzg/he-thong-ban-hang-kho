function StatusBadge({ status }) {
  const isOnline = status === 'online'

  return (
    <span className={`status-badge ${isOnline ? 'status-online' : 'status-offline'}`}>
      <span className="status-dot" aria-hidden="true" />
      {isOnline ? 'Đang hoạt động' : 'Chưa kết nối'}
    </span>
  )
}

export default StatusBadge
