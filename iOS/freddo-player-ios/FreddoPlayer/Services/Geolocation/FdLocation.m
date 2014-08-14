/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "FdLocation.h"
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

@implementation FdLocationData

@synthesize locationStatus, locationInfo, locationCallbacks;
- (FdLocationData*)init
{
    self = (FdLocationData*)[super init];
    if (self) {
        self.locationInfo = nil;
        self.locationCallbacks = nil;
    }
    return self;
}

@end

#pragma mark -
#pragma mark CDVLocation

@implementation FdLocation

@synthesize locationManager, locationData;

- (void)setup
{
    [super setup];
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self; // Tells the location manager to send updates to this object
    __locationStarted = NO;
    __highAccuracyEnabled = NO;
    self.locationData = nil;
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    NSDictionary *params = [event objectForKey:@"params"];
    if ([params isKindOfClass:[NSNull class]]) {
        params = nil;
    }
    BOOL enableHighAccuracy = YES;
    if (params != nil) {
        NSNumber *enableHighAccuracyNumber = [params objectForKey:@"enableHighAccuracy"];
        enableHighAccuracy = [enableHighAccuracyNumber boolValue];
    }
    if ([@"getCoordinates" isEqualToString:action]) {
        [self getLocation:event withAccuray:enableHighAccuracy];
        return YES;
    }
    
    return [super onEvent:event];
}

- (BOOL)isAuthorized
{
    BOOL authorizationStatusClassPropertyAvailable = [CLLocationManager respondsToSelector:@selector(authorizationStatus)]; // iOS 4.2+

    if (authorizationStatusClassPropertyAvailable) {
        NSUInteger authStatus = [CLLocationManager authorizationStatus];
        return (authStatus == kCLAuthorizationStatusAuthorized) || (authStatus == kCLAuthorizationStatusNotDetermined);
    }

    // by default, assume YES (for iOS < 4.2)
    return YES;
}

- (BOOL)isLocationServicesEnabled
{
    BOOL locationServicesEnabledInstancePropertyAvailable = [self.locationManager respondsToSelector:@selector(locationServicesEnabled)]; // iOS 3.x
    BOOL locationServicesEnabledClassPropertyAvailable = [CLLocationManager respondsToSelector:@selector(locationServicesEnabled)]; // iOS 4.x

    if (locationServicesEnabledClassPropertyAvailable) { // iOS 4.x
        return [CLLocationManager locationServicesEnabled];
    } else if (locationServicesEnabledInstancePropertyAvailable) { // iOS 2.x, iOS 3.x
        return [(id)self.locationManager locationServicesEnabled];
    } else {
        return NO;
    }
}

- (void)startLocation:(BOOL)enableHighAccuracy
{
    if (![self isLocationServicesEnabled]) {
        [self returnLocationError:PERMISSIONDENIED withMessage:@"Location services are not enabled."];
        return;
    }
    if (![self isAuthorized]) {
        NSString* message = nil;
        BOOL authStatusAvailable = [CLLocationManager respondsToSelector:@selector(authorizationStatus)]; // iOS 4.2+
        if (authStatusAvailable) {
            NSUInteger code = [CLLocationManager authorizationStatus];
            if (code == kCLAuthorizationStatusNotDetermined) {
                // could return POSITION_UNAVAILABLE but need to coordinate with other platforms
                message = @"User undecided on application's use of location services.";
            } else if (code == kCLAuthorizationStatusRestricted) {
                message = @"Application's use of location services is restricted.";
            }
        }
        // PERMISSIONDENIED is only PositionError that makes sense when authorization denied
        [self returnLocationError:PERMISSIONDENIED withMessage:message];

        return;
    }

    // Tell the location manager to start notifying us of location updates. We
    // first stop, and then start the updating to ensure we get at least one
    // update, even if our location did not change.
    [self.locationManager stopUpdatingLocation];
    [self.locationManager startUpdatingLocation];
    __locationStarted = YES;
    if (enableHighAccuracy) {
        __highAccuracyEnabled = YES;
        // Set to distance filter to "none" - which should be the minimum for best results.
        self.locationManager.distanceFilter = kCLDistanceFilterNone;
        // Set desired accuracy to Best.
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
    } else {
        __highAccuracyEnabled = NO;
        // TODO: Set distance filter to 10 meters? and desired accuracy to nearest ten meters? arbitrary.
        self.locationManager.distanceFilter = 10;
        self.locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters;
    }
}

- (void)_stopLocation
{
    if (__locationStarted) {
        if (![self isLocationServicesEnabled]) {
            return;
        }

        [self.locationManager stopUpdatingLocation];
        __locationStarted = NO;
        __highAccuracyEnabled = NO;
    }
}

- (void)locationManager:(CLLocationManager*)manager
    didUpdateToLocation:(CLLocation*)newLocation
           fromLocation:(CLLocation*)oldLocation
{
    FdLocationData* cData = self.locationData;

    cData.locationInfo = newLocation;
    if (self.locationData.locationCallbacks.count > 0) {
        for (NSDictionary* request in self.locationData.locationCallbacks) {
            [self returnLocationInfo:request];
        }
        [self.locationData.locationCallbacks removeAllObjects];
    }
    [self returnLocationInfo:nil];
}

- (void)getLocation:(NSDictionary *)request withAccuray:(BOOL)enableHighAccuracy
{
    if ([self isLocationServicesEnabled] == NO) {
        [self sendErrorResponse:@"Location services are disabled." request:request];
    } else {
        if (!self.locationData) {
            self.locationData = [[FdLocationData alloc] init];
        }
        FdLocationData* lData = self.locationData;
        if (!lData.locationCallbacks) {
            lData.locationCallbacks = [NSMutableArray arrayWithCapacity:1];
        }

        if (!__locationStarted || (__highAccuracyEnabled != enableHighAccuracy)) {
            // add the callbackId into the array so we can call back when get data
            if (request != nil) {
                [lData.locationCallbacks addObject:request];
            }
            // Tell the location manager to start notifying us of heading updates
            [self startLocation:enableHighAccuracy];
        } else {
            [self returnLocationInfo:request];
        }
    }
}

- (Boolean)start
{
    BOOL enableHighAccuracy = YES;
    
    if (!self.locationData) {
        self.locationData = [[FdLocationData alloc] init];
    }
    
    if ([self isLocationServicesEnabled] == NO) {
        NSMutableDictionary* posError = [NSMutableDictionary dictionaryWithCapacity:2];
        [posError setObject:[NSNumber numberWithInt:PERMISSIONDENIED] forKey:@"code"];
        [posError setObject:@"Location services are disabled." forKey:@"message"];
        [self fireObjectEvent:@"onerror" value:posError];
    } else {
        if (!__locationStarted || (__highAccuracyEnabled != enableHighAccuracy)) {
            // Tell the location manager to start notifying us of location updates
            [self startLocation:enableHighAccuracy];
        }
    }
    return YES;
}

- (void)stop
{
    [self _stopLocation];
}


- (void)returnLocationInfo:(NSDictionary *)request
{
    FdLocationData* lData = self.locationData;

    if (lData && !lData.locationInfo) {
        if (request == nil) {
            NSMutableDictionary* posError = [NSMutableDictionary dictionaryWithCapacity:2];
            [posError setObject:[NSNumber numberWithInt:POSITIONUNAVAILABLE] forKey:@"code"];
            [posError setObject:@"Position unavailable" forKey:@"message"];
            [self fireObjectEvent:@"onerror" value:posError];
        } else {
            [super sendErrorResponse:@"Position unavailable" request:request];
        }
    } else if (lData && lData.locationInfo) {
        CLLocation* lInfo = lData.locationInfo;
        NSMutableDictionary* returnInfo = [NSMutableDictionary dictionaryWithCapacity:8];
        NSNumber* timestamp = [NSNumber numberWithDouble:([lInfo.timestamp timeIntervalSince1970] * 1000)];
        [returnInfo setObject:timestamp forKey:@"timestamp"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.speed] forKey:@"velocity"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.verticalAccuracy] forKey:@"altitudeAccuracy"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.horizontalAccuracy] forKey:@"accuracy"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.course] forKey:@"heading"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.altitude] forKey:@"altitude"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.coordinate.latitude] forKey:@"latitude"];
        [returnInfo setObject:[NSNumber numberWithDouble:lInfo.coordinate.longitude] forKey:@"longitude"];

        // if callbackId is nil, then we publish an 'onstatus' event.
        // if not, we have to return the coordinates to the correct recipient
        if (request == nil) {
            [self fireObjectEvent:@"onstatus" value:returnInfo];
        } else {
            [self sendObjectResponse:returnInfo request:request];
        }
    }
}

- (void)returnLocationError:(NSUInteger)errorCode withMessage:(NSString*)message
{
    NSMutableDictionary* posError = [NSMutableDictionary dictionaryWithCapacity:2];

    [posError setObject:[NSNumber numberWithInt:errorCode] forKey:@"code"];
    [posError setObject:message ? message:@"" forKey:@"message"];
    [self fireObjectEvent:@"onerror" value:posError];
    [self.locationData.locationCallbacks removeAllObjects];
}

- (void)locationManager:(CLLocationManager*)manager didFailWithError:(NSError*)error
{
    NSLog(@"locationManager::didFailWithError %@", [error localizedFailureReason]);

    FdLocationData* lData = self.locationData;
    if (lData && __locationStarted) {
        // TODO: probably have to once over the various error codes and return one of:
        // PositionError.PERMISSION_DENIED = 1;
        // PositionError.POSITION_UNAVAILABLE = 2;
        // PositionError.TIMEOUT = 3;
        NSUInteger positionError = POSITIONUNAVAILABLE;
        if (error.code == kCLErrorDenied) {
            positionError = PERMISSIONDENIED;
        }
        [self returnLocationError:positionError withMessage:[error localizedDescription]];
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
    [self _stopLocation];
    [self.locationManager stopUpdatingHeading];
}

@end
