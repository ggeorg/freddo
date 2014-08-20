//
//  FdDeviceFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 22/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdDeviceFactory.h"
#import "DDLog.h"
#import "FdDevice.h"

@implementation FdDeviceFactory

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdDevice alloc] init:name withController:controller];
}
@end

