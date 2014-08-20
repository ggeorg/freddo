//
//  FdCalendarFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 23/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdCalendarFactory.h"
#import "DDLog.h"
#import "CalendarPlugin.h"

@implementation FdCalendarFactory

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[CalendarPlugin alloc] init:name withController:controller];
}

@end
