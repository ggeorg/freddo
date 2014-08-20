#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import "FdService.h"

enum FdLocationStatus {
    PERMISSIONDENIED = 1,
    POSITIONUNAVAILABLE,
    TIMEOUT
};
typedef NSUInteger CDVLocationStatus;

// simple object to keep track of location information
@interface FdLocationData : NSObject {
    CDVLocationStatus locationStatus;
    NSMutableArray* locationCallbacks;
    CLLocation* locationInfo;
}

@property (nonatomic, assign) CDVLocationStatus locationStatus;
@property (nonatomic, strong) CLLocation* locationInfo;
@property (nonatomic, strong) NSMutableArray* locationCallbacks;

@end

@interface FdLocation : FdService <CLLocationManagerDelegate>{
    @private BOOL __locationStarted;
    @private BOOL __highAccuracyEnabled;
    FdLocationData* locationData;
}

@property (nonatomic, strong) CLLocationManager* locationManager;
@property (nonatomic, strong) FdLocationData* locationData;

- (void)returnLocationInfo:(NSDictionary *)request;
- (void)returnLocationError:(NSUInteger)errorCode withMessage:(NSString*)message;
- (void)startLocation:(BOOL)enableHighAccuracy;

- (void)locationManager:(CLLocationManager*)manager
    didUpdateToLocation:(CLLocation*)newLocation
           fromLocation:(CLLocation*)oldLocation;

- (void)locationManager:(CLLocationManager*)manager
       didFailWithError:(NSError*)error;

- (BOOL)isLocationServicesEnabled;
@end
