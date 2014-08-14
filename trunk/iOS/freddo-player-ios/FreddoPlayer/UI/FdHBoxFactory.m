//
//  FdHBoxFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/15/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdHBoxFactory.h"

#import "FdHBox.h"

@implementation FdHBoxFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdHBox alloc] init:name options:options];
}

@end
