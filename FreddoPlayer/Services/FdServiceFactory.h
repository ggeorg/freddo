//
//  FdServiceFactory.h
//  Freddo Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "FdService.h"

@interface FdServiceFactory : FdService

- (FdService *)createService:(NSString *)name withController:(FdViewController *)controller;

@end
