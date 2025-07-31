module.exports = {
    dependencies: {
      '@anwar1909/react-native-background-geolocation': {
        platforms: {
          android: {
            // 1. Tambahkan kedua sub-modul ke include
            sourceDir:
              './node_modules/@anwar1909/react-native-background-geolocation/android/lib',
            packageImportPath:
              'import com.marianhello.bgloc.react.BackgroundGeolocationPackage;',
            packageInstance: 'new BackgroundGeolocationPackage()',
  
            // 2. Extra include untuk module "common"
            dependencyConfiguration: `
              implementation project(':anwar1909_react-native-background-geolocation')
              implementation project(':anwar1909_react-native-background-geolocation-common')
            `,
          },
        },
      },
    },
  };