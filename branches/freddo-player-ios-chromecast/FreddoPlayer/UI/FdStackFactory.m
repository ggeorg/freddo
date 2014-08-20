//
//  FdStackFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/16/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdStackFactory.h"

#import "FdStack.h"

@implementation FdStackFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdStack alloc] init:name options:options];
}

@end
