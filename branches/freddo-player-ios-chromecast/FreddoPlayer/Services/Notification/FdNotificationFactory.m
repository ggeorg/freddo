//
//  FdNotificationFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 22/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdNotificationFactory.h"
#import "DDLog.h"
#import "FdNotification.h"

@implementation FdNotificationFactory

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdNotification alloc] init:name withController:controller];
}
@end
