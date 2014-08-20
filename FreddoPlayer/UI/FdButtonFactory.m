//
//  FtvButtonFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdButtonFactory.h"

#import "FdButton.h"

@implementation FdButtonFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdButton alloc] init:name options:options];
}

@end
