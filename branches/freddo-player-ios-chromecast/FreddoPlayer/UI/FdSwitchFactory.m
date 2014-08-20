//
//  FtvSwitchFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 20/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSwitchFactory.h"

#import "FdSwitch.h"

@implementation FdSwitchFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdSwitch alloc] init:name options:options];
}

@end
