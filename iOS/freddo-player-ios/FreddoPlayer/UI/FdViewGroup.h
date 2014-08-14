//
//  FtvViewGroup.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 15/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "FdView.h"

@interface FdViewGroup : FdView {

}

- (void)addView:(NSString *)name options:(NSDictionary *)options;

- (void)removeView:(NSString *)name;

@end
