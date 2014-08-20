//
//  FtvSliderFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 17/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSliderFactory.h"

#import "FdSlider.h"

@implementation FdSliderFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdSlider alloc] init:name options:options];
}

@end
