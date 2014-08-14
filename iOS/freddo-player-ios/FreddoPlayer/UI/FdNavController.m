//
//  FdNavController.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/16/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdNavController.h"

#import "FdDisplay.h"

#import "WeView.h"

@implementation FdNavController

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[WeView alloc] init] options:options];
}

- (void)setup:(NSDictionary*)options
{
    [super setup:options];
    
    WeView *_view = (WeView *)self.view;
    [_view useStackDefaultLayout];
}

- (void)addView:(NSString *)name options:(NSDictionary *)options
{
    [super addView:name options:options];
    
    FdView *fdView = [FdDisplay viewByName:name];
    if(fdView != nil) {
        CATransition* transition = [CATransition animation];
        transition.type = kCATransitionPush;
        transition.subtype = kCATransitionFromRight;
        [fdView.view.layer addAnimation:transition forKey:@"push-transition"];
    }
}

- (void)removeView:(NSString *)name
{
    FdView *fdView = [FdDisplay viewByName:name];
    if(fdView != nil) {
        CATransition* transition = [CATransition animation];
        transition.type = kCATransitionPush;
        transition.subtype = kCATransitionFromLeft;
        //transition.
        [fdView.view.layer addAnimation:transition forKey:@"push-transition"];
    }
    
    [super removeView:name];
}

@end
