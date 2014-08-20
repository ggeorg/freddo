//
//  FdVBox.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/15/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdVBox.h"

#import "WeView.h"

@implementation FdVBox

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[WeView alloc] init] options:options];
}

- (void)setup:(NSDictionary*)options
{
    [super setup:options];
    
    WeView *_view = (WeView *)self.view;
    [_view useVerticalDefaultLayout];
}

@end
