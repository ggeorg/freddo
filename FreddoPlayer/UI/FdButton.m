//
//  FtvButton.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdButton.h"

@interface FdButton()
- (void) onClick:(id)sender;
@end

@implementation FdButton : FdView

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[UIButton buttonWithType:UIButtonTypeRoundedRect] options:options];
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
        if ([property isEqualToString:@"text"]) {
            NSString *text = [self getStringProperty:options property:property];
            if (text != nil) {
                UIButton *btn = (UIButton *)self.view;
                [btn setTitle:text forState: UIControlStateNormal];
                //if (btn.bounds.size.width == LAYOUT_WRAP_CONTENT && btn.bounds.size.height) {
                    [btn sizeToFit];
                //}
            }
        }
    }
    
    [super set:options];
}

- (void)on:(NSDictionary *)options
{
    for(id key in options) {
        NSString *event = (NSString *)key;
        if ([event isEqualToString:@"click"]) {
            UIButton *btn = (UIButton *)self.view;
            [btn addTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
        } else if ([event isEqualToString:@"~click"]) {
            UIButton *btn = (UIButton *)self.view;
            [btn removeTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
        }
    }
}

- (void)onClick:(id)sender {
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:self.nameInReply forKey:@"service"];
    [event setValue:@"onclick" forKey:@"action"];
    [event setValue:[[NSDictionary alloc] init] forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

@end
