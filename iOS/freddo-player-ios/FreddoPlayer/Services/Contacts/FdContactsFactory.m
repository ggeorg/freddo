//
//  FtvContactsFactory.m
//  FreddoTV Player
//
//  Created by Alejandro Garin on 14/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdContactsFactory.h"
#import "FdContacts.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdContactsFactory

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, name);
    
    return [[FdContacts alloc] init:name withController:controller];
}

@end
