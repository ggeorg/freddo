//
//  FtvProgressView.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdProgressView.h"

@implementation FdProgressView

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[UIProgressView alloc] init] options:options];
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
        if ([property isEqualToString:@"progress"]) {
            NSNumber *progress = [self getNumberProperty:options property:property];
            if (progress != nil) {
                UIProgressView *progressView = (UIProgressView *)self.view;
                progressView.progress = [progress floatValue];
            }
        }
    }
    
    [super set:options];
}

@end
