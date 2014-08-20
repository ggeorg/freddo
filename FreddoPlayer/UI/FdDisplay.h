//
//  FtvDisplay.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "FdViewGroup.h"

@interface FdDisplay : FdViewGroup

@property (nonatomic,readonly) UIViewController *controller;

+ (FdView *) viewByName:(NSString*)name;

- (id)init:name withController:(UIViewController *)_controller;

@end
