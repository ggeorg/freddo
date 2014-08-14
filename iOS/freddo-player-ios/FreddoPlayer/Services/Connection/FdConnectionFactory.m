//
//  FdConnectionFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 17/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdConnectionFactory.h"
#import "DDLog.h"
#import "FdConnection.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdConnectionFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdConnection alloc] init:name withController:controller];
}


@end
