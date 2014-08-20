//
//  FtvView.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdView.h"

#import "UIView+WeView.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdView()
- (void)handleEvent:(NSDictionary*)event;
@end

@implementation FdView

@synthesize name;
@synthesize nameInReply;
@synthesize view;

+ (UIColor *)mkColor:(NSArray *)rgba
{
    return [UIColor colorWithRed:([(NSNumber *)[rgba objectAtIndex:0] doubleValue] )
                           green:([(NSNumber *)[rgba objectAtIndex:1] doubleValue] )
                            blue:([(NSNumber *)[rgba objectAtIndex:2] doubleValue] )
                           alpha:([(NSNumber *)[rgba objectAtIndex:3] doubleValue] )];
}

- (id) init:(NSString *)_name withView:(UIView *)_view options:(NSDictionary*)options
{
    if (self = [super init]) {
        name = _name;
        nameInReply = [NSString stringWithFormat:@"$%@", _name];
        view = _view;
        [self setup:options];
    }
    return self;
}

- (void)setup:(NSDictionary*)options
{
    [[NSNotificationCenter defaultCenter] addObserverForName:name
                                                      object:nil
                                                       queue:[NSOperationQueue mainQueue]
                                                  usingBlock:^(NSNotification *note) {
                                                      [self handleEvent:[note userInfo]];
                                                  }];
    
    [self set:options];
}

#pragma mark - Event handler

- (void)handleEvent:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if (![self onEvent:event]) {
            // Log event was not handled
            NSLog(@"Unhandled event: %@", event);
        }
    }
}

- (Boolean)onEvent:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if([action isEqualToString:@"set"]) {
            NSDictionary *params = (NSDictionary *)[event objectForKey:@"params"];
            if (params != nil) {
                [self set:params];
                return YES;
            }
        } else if ([action isEqualToString:@"on"]) {
            NSDictionary *params = (NSDictionary *)[event objectForKey:@"params"];
            if (params != nil) {
                [self on:params];
                return YES;
            }

        }
    }
    
    return NO;
}

#pragma mark - Setters

- (void)set:(NSDictionary *)options
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    for(id key in options) {
        DDLogVerbose(@"SET %@", key);
        
        NSString *property = (NSString *)key;
        
        if ([property isEqualToString:@"hCellAlign"]) {
            NSNumber *hCellAlign = [self getNumberProperty:options property:property];
            switch ([hCellAlign intValue]) {
                case 1:
                    // left
                    break;
                case 2:
                    // center
                    break;
                case 3:
                    // right
                    break;
                default:
                    // none
                    break;
            }
        } else if ([property isEqualToString:@"vCellAlign"]) {
            NSNumber *vCellAlign = [self getNumberProperty:options property:property];
            switch ([vCellAlign intValue]) {
                case 1:
                    // top
                    break;
                case 2:
                    // middle
                    break;
                case 3:
                    // bottom
                    break;
                default:
                    // none
                    break;
            }
        } else if ([property isEqualToString:@"hStretchWeight"]) {
            NSNumber *weight = [self getNumberProperty:options property:property];
            if (weight != nil) {
                if ([weight floatValue] <= 0.0) {
                    [self.view setHStretchWeight:0.0];
                } else {
                    [self.view setHStretchWeight:[weight floatValue]];
                }
            }
        } else if ([property isEqualToString:@"vStretchWeight"]) {
            NSNumber *weight = [self getNumberProperty:options property:property];
            if (weight != nil) {
                if ([weight floatValue] <= 0.0) {
                    [self.view setVStretchWeight:0.0];
                } else {
                    [self.view setVStretchWeight:[weight floatValue]];
                }
            }
        } else if ([property isEqualToString:@"size"]) {
            NSArray *size = [self getArrayProperty:options property:property];
            DDLogVerbose(@"settings size: %@", size);
            if (size != nil && [size count] == 2) {
                
                //[self.view sizeToFit];
                
                //CGRect frame = self.view.frame;
                
                //width = (NSNumber *)[size objectAtIndex:2];
                //height = (NSNumber *)[size objectAtIndex:3];
                
                //if (width < 0) {
                    //width = frame.size.width;
                //}
                
                //if (height > 0) {
                    //height = frame.size.height;
                //}
                
                //frame.size.width = width;
                //frame.size.height = height;
                
                //self.view.frame = frame;
            }
        } else if([property isEqualToString:@"bounds"]) {
            NSArray *bounds = [self getArrayProperty:options property:property];
            DDLogVerbose(@"settings bounds: %@", bounds);
            if (bounds != nil && [bounds count] == 4) {
                /*
                [self.view sizeToFit];
                
                CGRect frame = self.view.frame;
                
                int x = [(NSNumber *)[bounds objectAtIndex:0] intValue];
                int y = [(NSNumber *)[bounds objectAtIndex:1] intValue];
                
                float width = [(NSNumber *)[bounds objectAtIndex:2] floatValue];
                float height = [(NSNumber *)[bounds objectAtIndex:3] floatValue];
                
                if (width < 0) {
                    width = frame.size.width;
                }
                
                if (height < 0) {
                    height = frame.size.height;
                }
                
                CGRect f = CGRectMake(x, y, width, height);
                
                self.view.frame = f;
                [self.view setNeedsLayout];
                 */
            }
        } else if([property isEqualToString:@"visibility"]) {
            Boolean visibility = [self getBooleanProperty:options property:property];
            [self setVisibility:visibility];
            [self.view setNeedsLayout];
        } else if([property isEqualToString:@"backgroundColor"]) {
            NSArray *rgba = [self getArrayProperty:options property:property];
            if (rgba != nil) {
                DDLogVerbose(@"settings backgroundColor: %@", [FdView mkColor:rgba]);
                self.view.backgroundColor = [FdView mkColor:rgba];
            }
        }
    }
}

- (void)setVisibility:(Boolean)visibility
{
    self.view.hidden = !visibility;
    
}

- (void)on:(NSDictionary *)options
{
    //for(id key in options) {
        //NSString *property = (NSString *)key;
    //}
}

- (Boolean)getBooleanProperty:(NSDictionary *)options property:(NSString *)property
{
    NSNumber *value = (NSNumber *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return [value integerValue] == 1;
}

- (NSNumber *)getNumberProperty:(NSDictionary *)options property:(NSString *)property
{
    NSNumber *value = (NSNumber *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

- (NSString *)getStringProperty:(NSDictionary *)options property:(NSString *)property
{
    NSString *value = (NSString *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

- (NSArray *)getArrayProperty:(NSDictionary *)options property:(NSString *)property
{
    NSArray *value = (NSArray *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

@end
