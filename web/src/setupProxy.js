const proxy = require('http-proxy-middleware').createProxyMiddleware

module.exports = function (app) {
  app.use(
    '/api',
    proxy({
      target: 'http://localhost:8080',
      changeOrigin: true,
    })
  )
}
