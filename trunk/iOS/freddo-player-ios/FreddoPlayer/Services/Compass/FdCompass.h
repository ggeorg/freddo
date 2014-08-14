#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import "FdService.h"

enum CDVHeadingStatus {
    HEADINGSTOPPED = 0,
    HEADINGSTARTING,
    HEADINGRUNNING,
    HEADINGERROR
};
typedef NSUInteger CDVHeadingStatus;

// simple object to keep track of heading information
@interface CDVHeadingData : NSObject {}

@property (nonatomic, assign) CDVHeadingStatus headingStatus;
@property (nonatomic, strong) CLHeading* headingInfo;
@property (nonatomic, strong) NSDate* headingTimestamp;

@end

@interface FdCompass : FdService <CLLocationManagerDelegate>{
    @private BOOL __locationStarted;
    @private BOOL __highAccuracyEnabled;
    CDVHeadingData* headingData;
}

@property (nonatomic, strong) CLLocationManager* locationManager;
@property (strong) CDVHeadingData* headingData;

- (BOOL)hasHeadingSupport;

- (void)locationManager:(CLLocationManager*)manager
       didFailWithError:(NSError*)error;

- (void)getHeading;
- (void)returnHeadingInfo;
- (void)watchHeadingFilter;
- (void)stopHeading;
- (void)startHeadingWithFilter;
- (void)locationManager:(CLLocationManager*)manager
       didUpdateHeading:(CLHeading*)heading;

- (BOOL)locationManagerShouldDisplayHeadingCalibration:(CLLocationManager*)manager;

@end
