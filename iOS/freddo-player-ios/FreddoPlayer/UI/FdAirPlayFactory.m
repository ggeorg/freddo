//
//  FdAirPlayFactory.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 24/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdAirPlayFactory.h"
#import "FdAirPlay.h"

@implementation FdAirPlayFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdAirPlay alloc] init:name options:options];
}

@end
