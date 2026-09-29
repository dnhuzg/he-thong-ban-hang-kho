const assert = require('node:assert/strict')
const { randomBytes } = require('node:crypto')
const test = require('node:test')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const { createAuthService } = require('../src/services/auth.service')

async function createFixture() {
  const passwordHash = await bcrypt.hash('correct-test-password', 4)
  const secret = randomBytes(48).toString('hex')
  const state = {
    attempts: 0,
    lockedUntil: null,
    isLocked: false,
  }
  const database = {
    async getConnection() {
      return {
        async beginTransaction() {},
        async commit() {},
        async rollback() {},
        release() {},
        async execute(sql, values) {
          if (sql.includes('FROM users')) {
            if (values[0] !== 'demo-user') return [[], []]
            return [[{
              id: 27,
              username: 'demo-user',
              email: 'demo@example.test',
              password_hash: passwordHash,
              is_active: 1,
              failed_login_attempts: state.attempts,
              locked_until: state.lockedUntil,
              is_locked: state.isLocked ? 1 : 0,
            }], []]
          }

          if (sql.includes('DATE_ADD')) {
            state.attempts = values[0]
            state.isLocked = values[1] >= 5
            state.lockedUntil = state.isLocked ? 'future' : null
          } else if (sql.includes('last_login_at')) {
            state.attempts = 0
            state.isLocked = false
            state.lockedUntil = null
          } else if (sql.includes('locked_until = NULL')) {
            state.attempts = 0
            state.isLocked = false
            state.lockedUntil = null
          } else if (sql.includes('FROM roles')) {
            return [[{ code: 'STOCK_VIEW', name: 'Xem tồn kho' }], []]
          } else if (sql.includes('FROM permissions')) {
            return [[{ code: 'stock.read' }], []]
          }

          return [[], []]
        },
      }
    },
  }

  return {
    state,
    secret,
    authService: createAuthService({ database, getSecret: () => secret }),
  }
}

test('login returns a signed token, public user, roles, and permissions', async () => {
  const { authService, secret } = await createFixture()

  const result = await authService.login('demo-user', 'correct-test-password')
  const claims = jwt.verify(result.token, secret, {
    issuer: 'he-thong-ban-hang-kho',
    audience: 'he-thong-ban-hang-kho',
  })

  assert.equal(claims.sub, '27')
  assert.deepEqual(result.user.roles, [{ code: 'STOCK_VIEW', name: 'Xem tồn kho' }])
  assert.deepEqual(result.user.permissions, ['stock.read'])
  assert.equal('password_hash' in result.user, false)
})

test('unknown account and wrong password return the same public error', async () => {
  const { authService } = await createFixture()
  const outcomes = []

  for (const [identifier, password] of [
    ['missing-user', 'bad-password'],
    ['demo-user', 'bad-password'],
  ]) {
    await assert.rejects(authService.login(identifier, password), (error) => {
      outcomes.push({ status: error.status, code: error.code, message: error.message })
      return error.status === 401
    })
  }

  assert.deepEqual(outcomes[0], outcomes[1])
})

test('fifth consecutive failure locks subsequent login attempts', async () => {
  const { authService, state } = await createFixture()

  for (let attempt = 1; attempt <= 5; attempt += 1) {
    await assert.rejects(authService.login('demo-user', 'bad-password'), { status: 401 })
  }

  assert.equal(state.attempts, 5)
  assert.equal(state.isLocked, true)
  await assert.rejects(authService.login('demo-user', 'correct-test-password'), { status: 401 })
})