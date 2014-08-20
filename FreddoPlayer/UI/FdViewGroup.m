//
//  FtvViewGroup.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdViewGroup.h"

#import "FdDisplay.h"

#import "WeView.h"
#import "WeViewLayout.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdViewGroup()

- (void)addView:(NSString *)name options:(NSDictionary *)options;
- (void)removeView:(NSString *)name;

@end

@implementation FdViewGroup

#pragma mark - Actions

- (void)addView:(NSString *)name options:(NSDictionary *)options
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    FdView *fdView = [FdDisplay viewByName:name];
    if(fdView != nil) {
        WeView *container = (WeView *)self.view;
        [container addSubviewToDefaultLayout:fdView.view];
    }
}

- (void)removeView:(NSString *)name
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    FdView *ftvView = [FdDisplay viewByName:name];
    [ftvView.view removeFromSuperview];
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if([action isEqualToString:@"add"]) {
            NSArray *params = (NSArray *)[event objectForKey:@"params"];
            if (params != nil && [params count] >= 2) {
                NSString *targetId = (NSString *)[params objectAtIndex:0];
                NSDictionary *options = (NSDictionary *)[params objectAtIndex:1];
                if (targetId != nil && options != nil) {
                    [self addView:targetId options:options];
                    return YES;
                }
            }
        } else if([action isEqualToString:@"remove"]) {
            NSString *targetId = (NSString *)[event objectForKey:@"params"];
            if (targetId != nil) {
                [self removeView:targetId];
                return YES;
            }
        }
    }
    
    return [super onEvent:event];
}

#pragma mark - Setters

- (void)set:(NSDictionary *)options
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [super set:options];
    
    for(id key in options) {
        DDLogVerbose(@"SET %@", key);
        
        NSString *property = (NSString *)key;
        
        WeView *container = (WeView *)self.view;
        
        if ([property isEqualToString:@"topMargin"]) {
            NSNumber *topMargin = [self getNumberProperty:options property:property];
            if (topMargin != nil) {
                [[container defaultLayout] setTopMargin:[topMargin floatValue]];
            }
        } else if ([property isEqualToString:@"hAlign"]) {
            NSNumber *hAlign = [self getNumberProperty:options property:property];
            if (hAlign != nil) {
                switch ([hAlign intValue]) {
                    case 1:
                        // left
                        [[container defaultLayout] setHAlign:H_ALIGN_LEFT];
                        break;
                    case 2:
                        // center
                        [[container defaultLayout] setHAlign:H_ALIGN_CENTER];
                        break;
                    case 3:
                        // right
                        [[container defaultLayout] setHAlign:H_ALIGN_RIGHT];
                        break;
                    default:
                        // center
                        [[container defaultLayout] setHAlign:H_ALIGN_CENTER];
                        break;
                }
            }
        } else if ([property isEqualToString:@"vAlign"]) {
            NSNumber *vAlign = [self getNumberProperty:options property:property];
            if (vAlign) {
                switch ([vAlign intValue]) {
                    case 1:
                        // top
                        [[container defaultLayout] setVAlign:V_ALIGN_TOP];
                        break;
                    case 2:
                        // middle
                        [[container defaultLayout] setVAlign:V_ALIGN_CENTER];
                        break;
                    case 3:
                        // bottom
                        [[container defaultLayout] setVAlign:V_ALIGN_BOTTOM];
                        break;
                    default:
                        // none
                        [[container defaultLayout] setVAlign:V_ALIGN_CENTER];
                        break;
                }
            }
        }
    }
}


@end
