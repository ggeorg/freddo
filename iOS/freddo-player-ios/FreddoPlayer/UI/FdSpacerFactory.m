//
//  FdSpacerFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/15/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSpacerFactory.h"

#import "FdSpacer.h"

@implementation FdSpacerFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdSpacer alloc] init:name options:options];
}

@end
