//
//  BatteryInfo.h
//  Pods
//
//  Created by Suman Saurabh on 09/05/25.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface BatteryInfo : NSObject

// Method to get battery level and status
+ (NSDictionary *)getBatteryLevelAndStatus;

@end
