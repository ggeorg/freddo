//
//  FtvSlider.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 17/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSlider.h"

@implementation FdSlider

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[UISlider alloc] init] options:options];
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
    
    //UILabel *label = (UILabel *)self.view;
    //[label sizeToFit];
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if ([property isEqualToString:@"value"]) {
            NSNumber *value = [self getNumberProperty:options property:property];
            if (value != nil) {
                UISlider *slider = (UISlider *)self.view;
                slider.value = [value floatValue];
            }
        }
    }
    
    [super set:options];
}

- (void)on:(NSDictionary *)options
{
    for(id key in options) {
        NSString *event = (NSString *)key;
        if ([event isEqualToString:@"change"]) {
            UISlider *slider = (UISlider *)self.view;
            [slider addTarget:self action:@selector(valueChanged:) forControlEvents:UIControlEventValueChanged];
        } else if ([event isEqualToString:@"~change"]) {
            UISlider *slider = (UISlider *)self.view;
            [slider removeTarget:self action:@selector(valueChanged:) forControlEvents:UIControlEventValueChanged];
        }
    }
}

- (void)valueChanged:(UISlider*)sender
{
    NSNumber *value = [[NSNumber alloc] initWithFloat:sender.value];
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:self.nameInReply forKey:@"service"];
    [event setValue:@"onchange" forKey:@"action"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

@end
