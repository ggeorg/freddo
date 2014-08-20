//
//  FtvWebSocket.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdServerWS.h"

#import "FdAppDelegate.h"

#import "HTTPServer.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdServerWS {
    NSString *_name;
}

@synthesize localServiceName, serviceName;

- (id) initWithRequest:(HTTPMessage *)aRequest socket:(GCDAsyncSocket *)socket localServiceName:(NSString *)localName serviceName:(NSString *)name
{
    if (self = [super initWithRequest:aRequest socket:socket]) {
        localServiceName = localName;
        _name = name;
    }
    
    return self;
}

- (void) didOpen
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [super didOpen];
    
    // Notify server browser about this connection
    //[[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didOpen" object:self userInfo:nil];
}

- (void) didReceiveMessage:(NSString *)msg
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, msg);
    
    NSError *e = nil;
    
    @try {
        // parse JSON
        NSData *msgData = [msg dataUsingEncoding:NSUTF8StringEncoding];
        NSMutableDictionary *msgDict = [NSJSONSerialization JSONObjectWithData:msgData options:NSJSONReadingMutableContainers error:&e];
        
        // process message
        
        NSString *to = [msgDict objectForKey:@"to"];
        NSString *from = [msgDict objectForKey:@"from"];
        
        // if this message has no 'service' create dtalk.InvalidMessage
        if ([msgDict objectForKey:@"service"] == nil) {
            NSMutableDictionary *_msgDict = [[NSMutableDictionary alloc] initWithCapacity:2];
            [_msgDict setObject:@"dtalk.InvalidMessage" forKey:@"service"];
            [_msgDict setObject:msgDict forKey:@"params"];
            msgDict = _msgDict;
        }
        
        DDLogVerbose(@"to: %@ (local: %@)", serviceName, localServiceName);
        
        if (to != nil && ![to isEqual:localServiceName]) {
            
            //
            // outgoing message
            //
            
            DDLogVerbose(@"%@[%p]: Sending outgoing message: %@ [%@", THIS_FILE, self.class, to, msgDict);
            [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.OutgoingMsg" object:nil userInfo:msgDict];
            
        } else {
            
            //
            // incoming message
            //
            
            if (serviceName == nil) {
                serviceName = from != nil ? from : _name;
                
                // Notify server browser about this connection
                [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didOpen" object:self userInfo:nil];
            }
            
            if (from == nil) { // anonymous message
                NSString *service = [msgDict objectForKey:@"service"];
                if (![service hasPrefix:@"$"]) {
                    from = serviceName;
                    [msgDict setObject:from forKey:@"from"];
                }
            }
            
            DDLogVerbose(@"%@[%p]: Post incoming message: %@", THIS_FILE, self.class, msgDict);
            [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:msgDict];
        }
        
    }
    @catch (NSException *e) {
        DDLogError(@"%@[%p]: Error in message parsing: %@", THIS_FILE, self.class, e);
        
        // TODO create dtalk.InvalidMessage event and send as incoming message
    }
}

- (void) didClose
{
	DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
	
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.ws.didClose" object:self userInfo:nil];
    
	[super didClose];
}

@end
