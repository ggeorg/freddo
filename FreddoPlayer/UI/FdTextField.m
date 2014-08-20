//
//  FtvTextField.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdTextField.h"

@implementation FdTextField

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[UITextField alloc] init] options:options];
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
 
    UITextField *textField = (UITextField *)self.view;
    textField.borderStyle = UITextBorderStyleRoundedRect;
    textField.clearButtonMode = UITextFieldViewModeWhileEditing;
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if ([property isEqualToString:@"text"]) {
            NSString *text = [self getStringProperty:options property:property];
            if (text != nil) {
                UITextField *textField = (UITextField *)self.view;
                textField.text = text;
                
                    NSLog(@"--------------- created -----%@-", text);
            }
        } else if ([property isEqualToString:@"placeholder"]) {
            NSString *placeholder = [self getStringProperty:options property:property];
            if (placeholder != nil) {
                UITextField *textField = (UITextField *)self.view;
                textField.placeholder = placeholder;
            }
        } else if ([property isEqualToString:@"secure"]) {
            Boolean secure = [self getBooleanProperty:options property:property];
            UITextField *textField = (UITextField *)self.view;
            textField.secureTextEntry = secure;
        }
    }
    
    [super set:options];
}

- (void)on:(NSDictionary *)options
{
    for(id key in options) {
        NSString *event = (NSString *)key;
        if ([event isEqualToString:@"change"]) {
            UITextField *textField = (UITextField *)self.view;
            textField.delegate = self;
        } else if ([event isEqualToString:@"~change"]) {
            UITextField *textField = (UITextField *)self.view;
            textField.delegate = nil;
        }
    }
}

#pragma mark -
#pragma mark UITextFieldDelegate Implementation

- (BOOL)textFieldShouldReturn:(UITextField *)textField{
	NSString *text = textField.text;
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:self.nameInReply forKey:@"service"];
    [event setValue:@"onchange" forKey:@"action"];
    [event setValue:text forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
    
	[textField resignFirstResponder];
	return YES;
}

@end
