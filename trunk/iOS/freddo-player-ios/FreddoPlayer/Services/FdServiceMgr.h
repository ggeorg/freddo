//
//  FtvServiceMgr.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "FdService.h"

@interface FdServiceMgr : FdService

+ (FdService *) serviceByName:(NSString*)name;

@end
