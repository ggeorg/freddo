//
//  FdVBoxFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/15/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdVBoxFactory.h"

#import "FdVBox.h"

@implementation FdVBoxFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdVBox alloc] init:name options:options];
}

@end
