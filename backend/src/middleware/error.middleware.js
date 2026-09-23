const notFound = (req, res) => {
  res.status(404).json({
    message: 'Route not found',
  })
}

const errorHandler = (error, req, res, next) => {
  console.error(error)
  res.status(500).json({
    message: 'Internal server error',
  })
}

module.exports = { notFound, errorHandler }
