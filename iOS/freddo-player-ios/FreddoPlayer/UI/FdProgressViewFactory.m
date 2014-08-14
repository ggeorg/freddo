//
//  FtvProgressViewFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdProgressViewFactory.h"

#import "FdProgressView.h"

@implementation FdProgressViewFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdProgressView alloc] init:name options:options];
}

@end
