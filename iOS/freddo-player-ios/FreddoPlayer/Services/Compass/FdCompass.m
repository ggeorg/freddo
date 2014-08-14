#import "FdCompass.h"
#import "NSArray+Comparisons.h"

#pragma mark Constants

#define kPGLocationErrorDomain @"kPGLocationErrorDomain"
#define kPGLocationDesiredAccuracyKey @"desiredAccuracy"
#define kPGLocationForcePromptKey @"forcePrompt"
#define kPGLocationDistanceFilterKey @"distanceFilter"
#define kPGLocationFrequencyKey @"frequency"

#pragma mark -
#pragma mark Categories

@interface CLHeading (JSONMethods)

- (NSString*)JSONRepresentation;

@end

#pragma mark -
#pragma mark CDVHeadingData

@implementation CDVHeadingData

@synthesize headingStatus, headingInfo, headingTimestamp;

- (CDVHeadingData*)init
{
    self = (CDVHeadingData*)[super init];
    if (self) {
        self.headingStatus = HEADINGSTOPPED;
        self.headingInfo = nil;
        self.headingTimestamp = nil;
    }
    return self;
}

@end

#pragma mark -
#pragma mark CDVLocation

@implementation FdCompass

@synthesize locationManager, headingData;

- (void)setup
{
    [super setup];
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self; // Tells the location manager to send updates to this object
    __locationStarted = NO;
    __highAccuracyEnabled = NO;
    self.headingData = nil;
}

- (BOOL)hasHeadingSupport
{
    BOOL headingInstancePropertyAvailable = [self.locationManager respondsToSelector:@selector(headingAvailable)]; // iOS 3.x
    BOOL headingClassPropertyAvailable = [CLLocationManager respondsToSelector:@selector(headingAvailable)]; // iOS 4.x

    if (headingInstancePropertyAvailable) { // iOS 3.x
        return [(id)self.locationManager headingAvailable];
    } else if (headingClassPropertyAvailable) { // iOS 4.x
        return [CLLocationManager headingAvailable];
    } else { // iOS 2.x
        return NO;
    }
}

- (Boolean)start
{
    [self watchHeadingFilter];
    return YES;
}

- (void)stop
{
    [self stopHeading];
}

/*
- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    if ([@"start" isEqualToString:action]) {
        [self watchHeadingFilter];
        return YES;
    } else if ([@"stop" isEqualToString:action]) {
        [self stopHeading];
        return YES;
    }
    return [super onEvent:event];
}
 */

// called to get the current heading
// Will call location manager to startUpdatingHeading if necessary

- (void)getHeading
{
    if ([self hasHeadingSupport] == NO) {
        [self fireObjectEvent:@"onerror" value:[NSDictionary dictionaryWithObject:@"Not heading supported" forKey:@"error"]];
    } else {
        // heading retrieval does is not affected by disabling locationServices and authorization of app for location services
        if (!self.headingData) {
            self.headingData = [[CDVHeadingData alloc] init];
        }
        CDVHeadingData* hData = self.headingData;

        if ((hData.headingStatus != HEADINGRUNNING) && (hData.headingStatus != HEADINGERROR)) {
            // Tell the location manager to start notifying us of heading updates
            [self startHeadingWithFilter];
        }
    }
}

// called to request heading updates when heading changes by a certain amount (filter)
- (void)watchHeadingFilter
{
    CDVHeadingData* hData = self.headingData;

    if ([self hasHeadingSupport] == NO) {
        [self fireObjectEvent:@"onerror" value:[NSDictionary dictionaryWithObject:@"Not heading supported" forKey:@"error"]];
    } else {
        if (!hData) {
            self.headingData = [[CDVHeadingData alloc] init];
            hData = self.headingData;
        }
        if (hData.headingStatus != HEADINGRUNNING) {
            // Tell the location manager to start notifying us of heading updates
            [self startHeadingWithFilter];
        }
    }
}

- (void)returnHeadingInfo
{
    CDVHeadingData* hData = self.headingData;

    self.headingData.headingTimestamp = [NSDate date];

    if (hData && (hData.headingStatus == HEADINGERROR)) {
        [self fireObjectEvent:@"onerror" value:[NSDictionary dictionaryWithObject:@"Heading error" forKey:@"error"]];
    } else if (hData && (hData.headingStatus == HEADINGRUNNING) && hData.headingInfo) {
        // if there is heading info, return it
        CLHeading* hInfo = hData.headingInfo;
        NSMutableDictionary* returnInfo = [NSMutableDictionary dictionaryWithCapacity:4];
        NSNumber* timestamp = [NSNumber numberWithDouble:([hInfo.timestamp timeIntervalSince1970] * 1000)];
        [returnInfo setObject:timestamp forKey:@"timestamp"];
        [returnInfo setObject:[NSNumber numberWithDouble:hInfo.magneticHeading] forKey:@"magneticHeading"];
        id trueHeading = __locationStarted ? (id)[NSNumber numberWithDouble : hInfo.trueHeading] : (id)[NSNull null];
        [returnInfo setObject:trueHeading forKey:@"trueHeading"];
        [returnInfo setObject:[NSNumber numberWithDouble:hInfo.headingAccuracy] forKey:@"headingAccuracy"];
        [super fireObjectEvent:@"onstatus" value:returnInfo];
    }
}

- (void)stopHeading
{
    if (self.headingData && (self.headingData.headingStatus != HEADINGSTOPPED)) {
        [self.locationManager stopUpdatingHeading];
        NSLog(@"heading STOPPED");
        self.headingData = nil;
    }
}

// helper method to check the orientation and start updating headings
- (void)startHeadingWithFilter
{
    // FYI UIDeviceOrientation and CLDeviceOrientation enums are currently the same
    self.locationManager.headingOrientation = self.controller.interfaceOrientation;
    self.locationManager.headingFilter = 0.2;
    [self.locationManager startUpdatingHeading];
    self.headingData.headingStatus = HEADINGSTARTING;
}

- (BOOL)locationManagerShouldDisplayHeadingCalibration:(CLLocationManager*)manager
{
    return YES;
}

- (void)locationManager:(CLLocationManager*)manager
       didUpdateHeading:(CLHeading*)heading
{
    CDVHeadingData* hData = self.headingData;

    // normally we would clear the delegate to stop getting these notifications, but
    // we are sharing a CLLocationManager to get location data as well, so we do a nil check here
    // ideally heading and location should use their own CLLocationManager instances
    if (hData == nil) {
        return;
    }

    // save the data for next call into getHeadingData
    hData.headingInfo = heading;

    if (hData.headingStatus == HEADINGSTARTING) {
        hData.headingStatus = HEADINGRUNNING; // so returnHeading info will work
    }
    
    [self returnHeadingInfo];
    hData.headingStatus = HEADINGRUNNING;  // to clear any error
}

- (void)locationManager:(CLLocationManager*)manager didFailWithError:(NSError*)error
{
    NSLog(@"locationManager::didFailWithError %@", [error localizedFailureReason]);

    // Compass Error
    if ([error code] == kCLErrorHeadingFailure) {
        CDVHeadingData* hData = self.headingData;
        if (hData) {
            if (hData.headingStatus == HEADINGSTARTING) {
                // TODO: heading error during startup - report error
            }
            hData.headingStatus = HEADINGERROR;
        }
    }

    [self.locationManager stopUpdatingLocation];
    __locationStarted = NO;
}

- (void)dealloc
{
    self.locationManager.delegate = nil;
}

- (void)onReset
{
    [self.locationManager stopUpdatingHeading];
    self.headingData = nil;
}

@end
