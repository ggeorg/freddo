//
//  FtvTextViewFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdTextViewFactory.h"

#import "FdTextView.h"

@implementation FdTextViewFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdTextView alloc] init:name options:options];
}

@end
