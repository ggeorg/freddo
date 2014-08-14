//
//  FtvLocationFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 11/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdLocationFactory.h"
#import "DDLog.h"
#import "FdLocation.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdLocationFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdLocation alloc] init:name withController:controller];
}



@end
