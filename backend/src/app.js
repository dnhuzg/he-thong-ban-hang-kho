const express = require('express')
const cors = require('cors')
const healthRoutes = require('./routes/health.routes')
const authRoutes = require('./routes/auth.routes')
const { notFound, errorHandler } = require('./middleware/error.middleware')

const app = express()

app.use(cors())
app.use(express.json({ limit: '10kb' }))

app.get('/', (req, res) => {
  res.json({
    message: 'Sales and inventory backend is running',
  })
})

app.use('/api', healthRoutes)
app.use('/api/auth', authRoutes)
app.use(notFound)
app.use(errorHandler)

module.exports = app
