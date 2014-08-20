//
//  FdServerWS.h
//  Freddo Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "WebSocket.h"

@interface FdServerWS : WebSocket {
    NSString *localServiceName;
    NSString *serviceName;
}

@property(nonatomic,readonly) NSString *localServiceName;
@property(nonatomic,readonly) NSString *serviceName;

- (id)initWithRequest:(HTTPMessage *)request socket:(GCDAsyncSocket *)socket localServiceName:(NSString *)localServiceName serviceName:(NSString *)serviceName;

@end
