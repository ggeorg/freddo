//
//  FdGlobalizationFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 21/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdGlobalizationFactory.h"
#import "DDLog.h"
#import "FdGlobalization.h"

@implementation FdGlobalizationFactory

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdGlobalization alloc] init:name withController:controller];
}
@end
