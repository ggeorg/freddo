//
//  FtvViewFactory.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdView.h"

@interface FdViewFactory : NSObject

- (FdView *)createView:(NSString *)name withOptions:(NSDictionary *)options;

@end
