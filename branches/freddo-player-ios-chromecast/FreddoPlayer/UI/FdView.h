//
//  FtvView.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "WeViewEnums.h"

@interface FdView : NSObject

@property (nonatomic,readonly) NSString *name;
@property (nonatomic,readonly) NSString *nameInReply;
@property (nonatomic,readonly) UIView *view;

+ (UIColor *)mkColor:(NSArray *)rgba;

- (id) init:(NSString *)name withView:(UIView *)view options:(NSDictionary*)options;

- (void)setup:(NSDictionary*)options;
- (Boolean)onEvent:(NSDictionary*)event;
- (void)set:(NSDictionary *)options;
- (void)on:(NSDictionary *)options;

- (Boolean)getBooleanProperty:(NSDictionary *)options property:(NSString *)property;
- (NSNumber *)getNumberProperty:(NSDictionary *)options property:(NSString *)property;
- (NSString *)getStringProperty:(NSDictionary *)options property:(NSString *)property;
- (NSArray *)getArrayProperty:(NSDictionary *)options property:(NSString *)property;

- (void)setVisibility:(Boolean)visibility;

@end
