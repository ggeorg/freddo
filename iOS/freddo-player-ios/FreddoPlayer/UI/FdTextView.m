//
//  FtvTextView.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdTextView.h"

@implementation FdTextView

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[UILabel alloc] init] options:options];
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
//    UILabel *label = (UILabel *)self.view;
//    [label sizeToFit];
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if ([property isEqualToString:@"text"]) {
            NSString *text = [self getStringProperty:options property:property];
            if (text != nil) {
                UILabel *label = (UILabel *)self.view;
                label.text = text;
            }
        }
    }
    
    [super set:options];
}

@end
