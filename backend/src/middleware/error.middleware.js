const notFound = (req, res) => {
  res.status(404).json({
    message: 'Route not found',
  })
}

const errorHandler = (error, req, res, next) => {
  if (error.status && error.code) {
    return res.status(error.status).json({ message: error.message, code: error.code })
  }

  if (error.type === 'entity.parse.failed') {
    return res.status(400).json({ message: 'Dữ liệu JSON không hợp lệ.', code: 'INVALID_JSON' })
  }

  if (error.type === 'entity.too.large') {
    return res.status(413).json({ message: 'Dữ liệu gửi lên quá lớn.', code: 'PAYLOAD_TOO_LARGE' })
  }

  console.error(error)
  return res.status(500).json({ message: 'Đã xảy ra lỗi hệ thống.', code: 'INTERNAL_ERROR' })
}

module.exports = { notFound, errorHandler }
