//
//  FtvTextFieldFactory.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdTextFieldFactory.h"

#import "FdTextField.h"

@implementation FdTextFieldFactory

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options
{
    return [[FdTextField alloc] init:name options:options];
}

@end
