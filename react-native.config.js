const path = require('path');

module.exports = {
  dependencies: {
    '@anwar1909/react-native-background-geolocation': {
      root: __dirname,
    },
  },
  project: {
    android: {
      sourceDir: path.join(__dirname, 'android', 'lib'),
    },
  },
};
