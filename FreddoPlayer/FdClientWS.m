//
//  FtvWebSocketDelegate.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 21/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdClientWS.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdClientWS {
    NSMutableArray *bufferedEvents;
}

- (id)initWithURLRequest:(NSURLRequest *)request andServiceName:(NSString *)serviceName
{
    _serviceName = serviceName;
    return [super initWithURLRequest:request protocols:nil];
}

- (void)send:(id)data // TODO lock bufferedEvents
{
    if(self.readyState != SR_OPEN) {
        // buffer msg
        if (bufferedEvents == nil) {
            bufferedEvents = [NSMutableArray array];
        }
        [bufferedEvents addObject:data];
    } else {
        if (bufferedEvents != nil) {
            [bufferedEvents addObject:data];
        } else {
            [super send:data];
        }
    }
}

- (void)webSocketDidOpen:(SRWebSocket *)webSocket // TODO lock bufferedEvents
{
    // check for buffered messages...
    if (bufferedEvents != nil) {
        for (id event in bufferedEvents) {
            [super send:event];
        }
        [bufferedEvents removeAllObjects];
        bufferedEvents = nil;
    }


    // Send 'didOpen' event...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didOpen" object:self userInfo:nil];
}

- (void)webSocket:(SRWebSocket *)webSocket didFailWithError:(NSError *)error
{
    // TODO: retry?

    // Send 'didClose' event... we need to clean up
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didClose" object:self userInfo:nil];
}

- (void)webSocket:(SRWebSocket *)webSocket didCloseWithCode:(NSInteger)code reason:(NSString *)reason wasClean:(BOOL)wasClean
{
    // Send 'didClose' event... we need to clean up
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didClose" object:self userInfo:nil];
}

- (void)webSocket:(SRWebSocket *)webSocket didReceiveMessage:(id)message
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, message);
    
    NSError *e = nil;
    
    @try {
        // parse JSON
        NSData *msgData = [message dataUsingEncoding:NSUTF8StringEncoding];
        NSMutableDictionary *msgDict = [NSJSONSerialization JSONObjectWithData:msgData options:NSJSONReadingMutableContainers error:&e];
        
        // process message
        
        NSString *to = [msgDict objectForKey:@"to"];
        NSString *from = [msgDict objectForKey:@"from"];
        
        if (from == nil) { // anonymous message
            from = [self serviceName];
            [msgDict setObject:from forKey:@"from"];
        }
        
        // if this message has no 'service' create dtalk.InvalidMessage
        if ([msgDict objectForKey:@"service"] == nil) {
            NSMutableDictionary *_msgDict = [[NSMutableDictionary alloc] initWithCapacity:2];
            [_msgDict setObject:@"dtalk.InvalidMessage" forKey:@"service"];
            [_msgDict setObject:msgDict forKey:@"params"];
            msgDict = _msgDict;
        }
        
        // post as incoming message
        [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:msgDict];
    }
    @catch (NSException *e) {
        DDLogError(@"%@[%p]: Error in message parsing: %@", THIS_FILE, self.class, e);
        // TODO create dtalk.InvalidMessage event and send as incoming message
    }
}

@end
