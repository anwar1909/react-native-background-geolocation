module.exports = {
  dependency: {
    '@anwar1909/react-native-background-geolocation': {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: 'android/lib',
        },
      },
    },
    '@anwar1909/react-native-background-geolocation-common': {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: 'android/common',
        },
      },
    },
  },
};
