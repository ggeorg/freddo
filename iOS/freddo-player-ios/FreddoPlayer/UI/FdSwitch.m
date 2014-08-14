//
//  FtvSwitch.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 20/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdSwitch.h"

@interface FdSwitch()
- (void)valueChanged:(UISwitch*)sender;
@end

@implementation FdSwitch

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[UISwitch alloc] initWithFrame:CGRectZero] options:options];
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
    
    //UISwitch *s = (UISwitch *)self.view;
//    textField.borderStyle = UITextBorderStyleRoundedRect;
//    textField.clearButtonMode = UITextFieldViewModeWhileEditing;
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if ([property isEqualToString:@"on"]) {
            Boolean on = [self getBooleanProperty:options property:property];
            UISwitch *onoff = (UISwitch *)self.view;
            onoff.on = on;
        }
    }
    
    [super set:options];
}

- (void)on:(NSDictionary *)options
{
    for(id key in options) {
        NSString *event = (NSString *)key;
        if ([event isEqualToString:@"change"]) {
            UISwitch *onoff = (UISwitch *)self.view;
            [onoff addTarget:self action:@selector(valueChanged:) forControlEvents:UIControlEventValueChanged];
        } else if ([event isEqualToString:@"~change"]) {
            UISwitch *onoff = (UISwitch *)self.view;
            [onoff removeTarget:self action:@selector(valueChanged:) forControlEvents:UIControlEventValueChanged];
        }
    }
}

//- (IBAction)valueChanged:(id)sender {
- (void)valueChanged:(UISwitch*)sender
{
    NSNumber *value = [[NSNumber alloc] initWithBool:sender.on];
        
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:self.nameInReply forKey:@"service"];
    [event setValue:@"onchange" forKey:@"action"];
    [event setValue:value forKey:@"params"];
        
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

@end
