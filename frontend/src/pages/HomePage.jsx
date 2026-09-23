import { useCallback, useEffect, useState } from 'react'
import StatusBadge from '../components/StatusBadge'
import { getHealth } from '../services/api'

function HomePage() {
  const [health, setHealth] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const checkHealth = useCallback(async () => {
    setIsLoading(true)
    setError('')

    try {
      setHealth(await getHealth())
    } catch (requestError) {
      setHealth(null)
      setError(requestError.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    checkHealth()
  }, [checkHealth])

  const status = health ? 'online' : 'offline'

  return (
    <section className="home-page">
      <div className="hero-copy">
        <StatusBadge status={status} />
        <h1>Sẵn sàng xây dựng hệ thống.</h1>
        <p>
          Đây là trang kiểm tra khung sườn ban đầu của hệ thống quản lý bán hàng
          và kho.
        </p>
      </div>

      <div className="health-panel">
        <div>
          <p className="panel-label">Backend health check</p>
          <h2>{isLoading ? 'Đang kiểm tra...' : health ? 'Kết nối thành công' : 'Chưa kết nối backend'}</h2>
          {error && <p className="error-message">{error}</p>}
          {health && <p className="success-message">API trả về trạng thái: {health.status}</p>}
        </div>
        <button type="button" onClick={checkHealth} disabled={isLoading}>
          {isLoading ? 'Đang kiểm tra' : 'Kiểm tra lại'}
        </button>
      </div>
    </section>
  )
}

export default HomePage
