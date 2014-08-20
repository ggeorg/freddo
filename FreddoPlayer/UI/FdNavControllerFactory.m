//
//  FdNavControllerFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/16/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdNavControllerFactory.h"

#import "FdNavController.h"

@implementation FdNavControllerFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdNavController alloc] init:name options:options];
}

@end
