#import <UIKit/UIKit.h>
#import "FdService.h"

@interface FdAccelerometer : FdService <UIAccelerometerDelegate>
{
    double x;
    double y;
    double z;
    NSTimeInterval timestamp;
}

@property (readonly, assign) BOOL isRunning;

@end
