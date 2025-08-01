//
//  BatteryInfo.m
//  @anwar1909_react-native-background-geolocation
//
//  Created by Suman Saurabh on 09/05/25.
//
#import "BatteryInfo.h"

@implementation BatteryInfo

+ (NSDictionary *)getBatteryLevelAndStatus {
    // Enable battery monitoring
    [UIDevice currentDevice].batteryMonitoringEnabled = YES;

    // Get battery level
    float batteryLevel = [UIDevice currentDevice].batteryLevel; // Battery level is between 0.0 and 1.0, or -1.0 if unavailable.

    // Get battery state
    UIDeviceBatteryState batteryState = [UIDevice currentDevice].batteryState;
    int batteryStatus = 0; // Default to not charging (0)

    // Determine status: 0 (not charging) or 1 (charging)
    if (batteryState == UIDeviceBatteryStateCharging || batteryState == UIDeviceBatteryStateFull) {
        batteryStatus = 1; // Charging
    }

    // Create a dictionary to return battery information
    NSDictionary *batteryInfo = @{
            @"BatteryLevel": @(batteryLevel * 100), // Convert to percentage
            @"BatteryStatus": @(batteryStatus)     // 0 or 1
        };

    return batteryInfo;
}

@end
