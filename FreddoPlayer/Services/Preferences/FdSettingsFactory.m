//
//  FdSettingsFactory.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 5/11/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSettingsFactory.h"
#import "FdSettings.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdSettingsFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdSettings alloc] init:name withController:controller];
}

@end
