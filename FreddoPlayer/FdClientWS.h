//
//  FtvWebSocketDelegate.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 21/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "SRWebSocket.h"

@interface FdClientWS : SRWebSocket<SRWebSocketDelegate>

@property(nonatomic, readonly) NSString *serviceName;

- (id)initWithURLRequest:(NSURLRequest *)request andServiceName:(NSString *)serviceName;

@end
