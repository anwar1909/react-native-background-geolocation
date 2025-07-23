const path = require('path');

module.exports = {
  dependencies: {
    '@anwar1909/react-native-background-geolocation': {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: path.join(__dirname, 'android', 'lib'),
          packageImportPath: 'import com.anwar1909.bgloc.react.BackgroundGeolocationPackage;',
          packageInstance: 'new BackgroundGeolocationPackage()',
        },
      },
    },
  },
};