//
//  FdAppViewFactory.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 18/11/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdAppViewFactory.h"
#import "FdAppView.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdAppViewFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdAppView alloc] init:name withController:controller];
}

@end
