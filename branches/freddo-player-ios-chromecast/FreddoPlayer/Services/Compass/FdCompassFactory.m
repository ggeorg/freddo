//
//  FtvCompassFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 11/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdCompassFactory.h"
#import "DDLog.h"
#import "FdCompass.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdCompassFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdCompass alloc] init:name withController:controller];
}

@end
