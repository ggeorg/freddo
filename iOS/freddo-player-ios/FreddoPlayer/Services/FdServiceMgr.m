//
//  FtvServiceMgr.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdServiceMgr.h"

#import "FdVideoFactory.h"
#import "FdAccelerometerFactory.h"
#import "FdCompassFactory.h"
#import "FdLocationFactory.h"
#import "FdContactsFactory.h"
#import "FdConnectionFactory.h"
#import "FdGlobalizationFactory.h"
#import "FdDeviceFactory.h"
#import "FdNotificationFactory.h"
#import "FdCalendarFactory.h"
#import "FdSettingsFactory.h"
#import "FdAppViewFactory.h"
#import "FdPresenceFactory.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdServiceMgr()

@property (readonly,retain) NSDictionary *factories;

- (Boolean)registerService:(NSString *)typeName from:(NSString *)from;
- (void)unregisterService:(NSString *)typeName from:(NSString *)from;

@end

@implementation FdServiceMgr

@synthesize factories;

static NSMutableDictionary *services;

+ (FdService *) serviceByName:(NSString*)name
{
    return [services objectForKey:name];
}

- (id) init:(NSString *)name withController:(FdViewController*)controller
{
    if (self = [super init:name withController:controller]) {
        factories = [[NSMutableDictionary alloc] init];
        services = [[NSMutableDictionary alloc] init];
        
        [factories setValue:[[FdVideoFactory alloc] init] forKey:@"dtalk.service.Video"];
        [factories setValue:[[FdAccelerometerFactory alloc] init] forKey:@"dtalk.service.Accelerometer"];
        [factories setValue:[[FdCompassFactory alloc] init] forKey:@"dtalk.service.Compass"];
        [factories setValue:[[FdLocationFactory alloc] init] forKey:@"dtalk.service.Geolocation"];
        [factories setValue:[[FdContactsFactory alloc] init] forKey:@"dtalk.service.Contacts"];
        [factories setValue:[[FdConnectionFactory alloc] init] forKey:@"dtalk.service.Connection"];
        [factories setValue:[[FdGlobalizationFactory alloc] init] forKey:@"dtalk.service.Globalization"];
        [factories setValue:[[FdDeviceFactory alloc] init] forKey:@"dtalk.service.Device"];
        [factories setValue:[[FdNotificationFactory alloc] init] forKey:@"dtalk.service.Notification"];
        [factories setValue:[[FdCalendarFactory alloc] init] forKey:@"dtalk.service.Calendar"];
        [factories setValue:[[FdSettingsFactory alloc] init] forKey:@"dtalk.service.Settings"];
        [factories setValue:[[FdAppViewFactory alloc] init] forKey:@"dtalk.service.AppView"];
        [factories setValue:[[FdPresenceFactory alloc] init] forKey:@"dtalk.service.Presence"];
    }
    return self;
}

#pragma mark -
#pragma mark - Events

- (Boolean)onEvent:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *from = (NSString*)[event objectForKey:@"from"];
    if (from == nil)  // anonymous connection
        from = @"";
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if ([@"start" isEqualToString:action]) {
        NSString *typeName = (NSString *)[event objectForKey:@"params"];
        if (typeName != nil) {
            [self sendBooleanResponse:[self registerService:typeName from:from] request:event];
            return YES;
        }
        // TODO fire error message instead.
        [self sendBooleanResponse:FALSE request:event];
    } else if ([@"stop" isEqualToString:action]) {
        NSString *typeName = (NSString *)[event objectForKey:@"params"];
        if (typeName != nil) {
            [self unregisterService:typeName from:from];
            return YES;
        }
    }
    
    return [super onEvent:event];
}

- (Boolean)registerService:(NSString*)typeName from:(NSString*)from
{
    DDLogVerbose(@"%@[%p]: %@%@ from=%@", THIS_FILE, self, THIS_METHOD, typeName, from);
    
    FdService *service = [services objectForKey:typeName];
    if (service != nil) {
        DDLogVerbose(@"Found service: %@", typeName);
        [service.refCnt setObject:from forKey:from];
        return TRUE;
    }
    
    DDLogVerbose(@"Create service: %@", typeName);
    FdServiceFactory *factory = (FdServiceFactory *)[factories objectForKey:typeName];
    if (factory != nil) {
        FdService *service = [factory createService:typeName withController:self.controller];
        [service.refCnt setObject:from forKey:from];
        [services setValue:service forKey:typeName];
        return [service start];
    } else {
        DDLogWarn(@"Service factory not found: %@", typeName);
    }
    
    return FALSE;
}

- (void)unregisterService:(NSString*)typeName from:(NSString*)from
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, typeName);
    
    FdService *service = [services objectForKey:typeName];
    if(service != nil) {
        [service.refCnt removeObjectForKey:from];
        if ([service.refCnt count] == 0) {
            [services removeObjectForKey:typeName];
            [service stop];
        }
    }
}

@end
