const path = require('path');

module.exports = {
  dependencies: {
    '@anwar1909/react-native-background-geolocation': {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: path.join(__dirname, 'android', 'lib'),
          libraryName: 'anwar1909_react-native-background-geolocation',
          packageImportPath: 'import com.marianhello.bgloc.react.BackgroundGeolocationPackage;',
          dependencies: [
            {
              name: 'anwar1909_react-native-background-geolocation-common',
              sourceDir: path.join(__dirname, 'android', 'common'),
            }
          ]
        },
      },
    },
  },
};