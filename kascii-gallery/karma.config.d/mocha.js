// The piece tests build every scene several times and each scene fills large palettes. A slow runner needs more than
// Mocha's default 2 s per test.
config.client = config.client || {}
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 120000 })
