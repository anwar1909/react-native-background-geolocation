require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "anwar1909_react-native-background-geolocation"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "11.0"

  s.source       = { :path => "ios" }
  s.source_files  = "ios/**/*.{h,m}"
  s.exclude_files = "ios/common/BackgroundGeolocationTests/*.{h,m}"

  s.dependency 'React'
end
