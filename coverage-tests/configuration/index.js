const overrideTests = require('./override-tests')
const initializeSdk = require('./initialize')
const testFrameworkTemplate = require('./template')

module.exports = {
  name: 'eyes_selenium_java',
  initializeSdk: initializeSdk,
  overrideTests,
  testFrameworkTemplate: testFrameworkTemplate,
  ext: '.java',
  outPath: './src/test/java/coverage/generic',
}
