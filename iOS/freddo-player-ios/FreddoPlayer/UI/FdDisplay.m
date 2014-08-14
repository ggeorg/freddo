//
//  FtvDisplay.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdDisplay.h"

#import "WeView.h"
#import "WeViewGridLayout.h"

#import "FdButtonFactory.h"
#import "FdHBoxFactory.h"
#import "FdNavControllerFactory.h"
#import "FdProgressViewFactory.h"
#import "FdSliderFactory.h"
#import "FdSpacerFactory.h"
#import "FdStackFactory.h"
#import "FdSwitchFactory.h"
#import "FdTextFieldFactory.h"
#import "FdTextViewFactory.h"
#import "FdVBoxFactory.h"
#import "FdAirPlayFactory.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdDisplay()

@property (readonly,retain) NSDictionary *factories;

- (void)registerController:(NSString*)targetId typeName:(NSString*)typeName option:(NSDictionary*)options;

@end

@implementation FdDisplay

@synthesize factories, controller;

static NSDictionary *views;

+ (FdView *)viewByName:(NSString*)name
{
    return [views objectForKey:name];
}

- (id)init:name withController:(UIViewController *)_controller
{
    WeView *_view = [[WeView alloc] init];
    [[[_view useVerticalDefaultLayout] setVAlign:V_ALIGN_TOP] setVSpacing:5] ;
    
    _view.opaque = NO;
    _view.backgroundColor = [UIColor clearColor];

    _view.frame = CGRectInset([_controller.view bounds], 0, 0);
    _view.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    _view.hidden = YES;
    
    [_controller.view addSubview:_view];
    [_controller.view bringSubviewToFront:_view];

    if (self = [super init:name withView:_view options:nil]) {
        factories = [[NSMutableDictionary alloc] init];
        views = [[NSMutableDictionary alloc] init];
        controller = _controller;
        
        [factories setValue:[[FdButtonFactory alloc] init] forKey:@"freddotv.ui.Button"];
        [factories setValue:[[FdHBoxFactory alloc] init] forKey:@"freddotv.ui.HBox"];
        [factories setValue:[[FdNavControllerFactory alloc] init] forKey:@"freddotv.ui.NavController"];
        [factories setValue:[[FdProgressViewFactory alloc] init] forKey:@"freddotv.ui.ProgressView"];
        [factories setValue:[[FdSliderFactory alloc] init] forKey:@"freddotv.ui.Slider"];
        [factories setValue:[[FdSpacerFactory alloc] init] forKey:@"freddotv.ui.Spacer"];
        [factories setValue:[[FdStackFactory alloc] init] forKey:@"freddotv.ui.Stack"];
        [factories setValue:[[FdSwitchFactory alloc] init] forKey:@"freddotv.ui.Switch"];
        [factories setValue:[[FdTextFieldFactory alloc] init] forKey:@"freddotv.ui.TextField"];
        [factories setValue:[[FdTextViewFactory alloc] init] forKey:@"freddotv.ui.TextView"];
        [factories setValue:[[FdVBoxFactory alloc] init] forKey:@"freddotv.ui.VBox"];
        [factories setValue:[[FdAirPlayFactory alloc] init] forKey:@"freddotv.ui.AirPlay"];
    }
    return self;
}

- (void)setup:(NSDictionary*)options
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    [super setup:options];
}

#pragma mark - Events

- (Boolean)onEvent:(NSDictionary*)event
{    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if([action isEqualToString:@"create"]) {
            NSArray *params = (NSArray *)[event objectForKey:@"params"];
            if (params != nil && [params count] >= 3) {
                NSString *targetId = (NSString *)[params objectAtIndex:0];
                NSString *typeName = (NSString *)[params objectAtIndex:1];
                NSDictionary *options = (NSDictionary *)[params objectAtIndex:2];
                if (targetId != nil && typeName != nil) {
                    [self registerController:targetId typeName:typeName option:options];
                    return YES;
                }
            }
        }
    }
    
    return [super onEvent:event];
}

- (void)registerController:(NSString*)targetId typeName:(NSString*)typeName option:(NSDictionary*)options
{
    if([views objectForKey:targetId] == nil) {
        FdViewFactory *factory = (FdViewFactory *)[factories objectForKey:typeName];
        if (factory != nil) {
            FdView *view = [factory createView:targetId withOptions:options];
            [views setValue:view forKey:targetId];
        }
    }
}

- (void)setVisibility:(Boolean)visibility
{
    [super setVisibility:visibility];
}

@end
