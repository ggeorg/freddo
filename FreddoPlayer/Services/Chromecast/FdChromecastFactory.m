//
//  FdChromecastFactory.m
//  Freddo-Player
//
//  Created by George Georgopoulos on 3/31/14.
//  Copyright (c) 2014 ArkaSoft LLC. All rights reserved.
//

#import "FdChromecastFactory.h"

#import "FdChromecast.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdChromecastFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdChromecast alloc] init:name withController:controller];
}

@end
