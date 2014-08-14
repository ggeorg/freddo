//
//  FdDeviceBrowserFactory.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 19/11/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdPresenceFactory.h"
#import "FdPresence.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdPresenceFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    return [[FdPresence alloc] init:name withController:controller];
}

@end


