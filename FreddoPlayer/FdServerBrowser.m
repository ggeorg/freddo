//
//  FtvHTTPServerBrowser.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdServerBrowser.h"

#import "FdServerWS.h"
#import "FdClientWS.h"

#import "NSData+Base64.h"

#import "HTTPServer.h"
#import "HTTPLogging.h"

#define SERVICE_TYPE @"_http._tcp."

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdServerBrowser()

- (void) handleWSDidOpenEvent:(NSObject *)ws;
- (void) handleWSDidCloseEvent:(NSObject *)ws;
- (void) handleIncomingMsgEvent:(NSDictionary *)event;
- (void) handleOutgoingMsgEvent:(NSDictionary *)event;

@property(nonatomic,readonly) NSDictionary *subscriptions;

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation FdServerBrowser

@synthesize delegate, servers, connections, subscriptions;

static FdServerBrowser *sharedFdServerBrowser = nil;

static HTTPServer *httpServer;

+ (id)sharedInstance {
    return sharedFdServerBrowser;
}

+ (id)sharedInstance:(HTTPServer *)_httpServer; {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        httpServer = _httpServer;
        sharedFdServerBrowser = [[self alloc] init];
    });
    return sharedFdServerBrowser;
}

- (id) init
{
    if (sharedFdServerBrowser != nil) {
        return nil;
    }
    
    if ((self = [super init])) {
        servers = [[NSMutableDictionary alloc] init];
        connections = [[NSMutableDictionary alloc] init];
        subscriptions = [[NSMutableDictionary alloc] init];
        
        // register to ws didOpen events
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.ws.didOpen"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          // Handle WS didOpen
                                                          [self handleWSDidOpenEvent:[note object]];
                                                      }];
        
        // register to ws didClose events
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.ws.didClose"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          // Handle WS didOpen
                                                          [self handleWSDidCloseEvent:[note object]];
                                                      }];
        

        // register to incoming messages
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.IncomingMsg"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          // Handle IncomingMsg
                                                          [self handleIncomingMsgEvent:[note userInfo]];
                                                      }];

        // register to outgoing messages
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.OutgoingMsg"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          // Handle OutgoingMsg
                                                          [self handleOutgoingMsgEvent:[note userInfo]];
                                                      }];
    }
    return self;
}

- (BOOL) start
{
    // Restarting?
    if (netServiceBrowser != nil) {
        [self stop];
    }
    
    netServiceBrowser = [[NSNetServiceBrowser alloc] init];
    if (!netServiceBrowser) {
        // The NSNetServiceBrowser couldn't be allocated and initialized.
        return NO;
    }
    
    [netServiceBrowser setDelegate:self];
    [netServiceBrowser searchForServicesOfType:SERVICE_TYPE inDomain:@"local."];
    
    return YES;
}

- (void) stop
{
    if (netServiceBrowser == nil) {
        return;
    }
    
    [netServiceBrowser stop];
    netServiceBrowser = nil;
    
    [servers removeAllObjects];
}

-(NSString *)getPublishedName {
    if (httpServer != nil) {
        return [httpServer publishedName];
    } else {
        return @"";
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
#pragma mark Event handlers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) handleWSDidOpenEvent:(id)ws {
    // 'ws' can be either FdServerWS or FdClientWS
    DDLogVerbose(@"%s: %@", __FUNCTION__, [ws serviceName]);
    if ([ws serviceName] != nil) {
        [connections setObject:ws forKey:[ws serviceName]];
    }
}

- (void) handleWSDidCloseEvent:(id)ws {
    // 'ws' can be either FdServerWS or FdClientWS
    DDLogVerbose(@"%s: %@", __FUNCTION__, [ws serviceName]);
    if ([ws serviceName] != nil) {
        [connections removeObjectForKey:[ws serviceName]];
    }
}

- (void) handleIncomingMsgEvent:(NSDictionary*)event {
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, event);
    
    NSString *service = (NSString *)[event objectForKey:@"service"];
    if ([@"dtalk.Dispatcher" isEqualToString:service]) {
        NSString *from = [event objectForKey:@"from"];
        
        // Handle subscribe/unsubscribe of external applications, e.g. web pages
        // or external monitoring tools.
        if (from != nil) {
            NSString *action = [event objectForKey:@"action"];
            if ([@"subscribe" isEqualToString:action]) {
                NSString *topic = [event objectForKey:@"params"];
                if (topic != nil) {
                    DDLogInfo(@"subscribe: %@", topic);
                    
                    id wsConn = [connections objectForKey:from];
                    if (wsConn == nil) {
                        DDLogWarn(@"WebSocket not found for %@", from);
                        return;
                    }
                    
                    NSMutableArray *array = [subscriptions objectForKey:topic];
                    
                    if (array == nil) {
                        array = [[NSMutableArray alloc] initWithCapacity:10];
                        [subscriptions setObject:array forKey:topic];
                    }
                    
                    if ([array containsObject:from]) {
                        [array removeObject:from];
                    }
                    [array addObject:from];
                }
            } else if ([@"unsubscribe" isEqualToString:action]) {
                NSString *topic = [event objectForKey:@"params"];
                if (topic != nil) {
                    DDLogInfo(@"unsubscribe: %@", topic);
                    
                    NSMutableArray *array = [subscriptions objectForKey:topic];
                    
                    if (array != nil) {
                        [array removeObject:from];
                        
                        if (array.count == 0) {
                            [subscriptions removeObjectForKey:topic];
                        }
                    }
                    
                }
            } else {
                // Ignore invalid action
            }
        } else {
            // Ignore from == nil
        }
    } else if (service != nil) {
        // Dispatch message event...
        
        @try {
            DDLogVerbose(@"%@[%p]: Dispatch message: %@", THIS_FILE, self, service);
            [[NSNotificationCenter defaultCenter] postNotificationName:service object:nil userInfo:event];
        }
        @catch (NSException *e) {
            DDLogError(@"%@[%p]: Error dispatching message: %@", THIS_FILE, self.class, e);
        }
        
        // dispatch message to subscribers...
        
        NSMutableArray *array = [subscriptions objectForKey:service];
        DDLogVerbose(@"%@[%p]: Dispatch message to subscribers: %@ %@", THIS_FILE, self, array, event);
        
        DDLogVerbose(@"%@[%p]: --------------------------------------------------------- ", THIS_FILE, self);
        
        if (array != nil) {
            
            // Clone the event and send it...
            NSMutableDictionary *_event = [[NSMutableDictionary alloc] initWithDictionary:event copyItems:YES];
            [_event removeObjectForKey:@"to"];
            
            NSString *from = (NSString *) [event objectForKey:@"from"];
            if (from == nil) {
                [_event setObject:httpServer.publishedName forKey:@"from"];
                event = _event;
            }
            
            @try {
                // Generate a JSON string form event dictionary
                NSError* error;
                NSData* data = [NSJSONSerialization dataWithJSONObject:event
                                                               options:NSJSONWritingPrettyPrinted error:&error];
                
                // Items to remove if the connection returned by the FdServerWS is nil
                NSMutableArray *toRemove = [[NSMutableArray alloc] initWithCapacity:10];
                
                for (NSString *serviceName in array) {
                    // Avoid cyclic broadcast messages...
                    if ([serviceName isEqualToString:from]) {
                        continue;
                    }
                    
                    @try {
                        id wsConn = [connections objectForKey:serviceName];
                        if (wsConn == nil) {
                            [toRemove addObject:serviceName];
                            continue;
                        }
                        
                        DDLogVerbose(@"Sending message to %@", serviceName);
                        if ([wsConn isKindOfClass:[FdServerWS class]]) {
                            [wsConn sendMessage:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]];
                        } else if ([wsConn isKindOfClass:[FdClientWS class]]) {
                            [wsConn send:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]];
                        }
                    }
                    @catch (NSException *e) {
                        DDLogError(@"%@[%p]: Error dispatching message: %@, to: %@", THIS_FILE, self.class, e, serviceName);
                    }
                }
                
                // Clean up connections.
                for (NSString *serviceName in toRemove) {
                    [array removeObject:serviceName];
                }
                
            }
            @catch (NSException *e) {
                DDLogError(@"Error: %@", e);
            }
        }
        
    }
}

- (void) handleOutgoingMsgEvent:(NSDictionary*)event {
    if (httpServer == nil || httpServer.publishedName == nil)
        return;
    
    NSString *to = (NSString *) [event objectForKey:@"to"];
    if (to == nil) {
        return;
    }
    
    // Clone the event and send it...
    NSMutableDictionary *_event = [[NSMutableDictionary alloc] initWithDictionary:event copyItems:YES];
    NSString *from = (NSString *) [event objectForKey:@"from"];
    if (from == nil) {
        [_event setObject:httpServer.publishedName forKey:@"from"];
        event = _event;
    }

    id wsConn = [connections objectForKey:to];
    if (wsConn != nil) {
        NSError* error;
        NSData* data = [NSJSONSerialization dataWithJSONObject:event options:NSJSONWritingPrettyPrinted error:&error];

        // Send the message and return
        DDLogVerbose(@"%@:%@ - %@", THIS_FILE, THIS_METHOD, @"Found an established WS");
        
        if ([wsConn isKindOfClass:[FdServerWS class]]) {
            DDLogVerbose(@"%@:%@ - %@", THIS_FILE, THIS_METHOD, event);
            [wsConn sendMessage:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]];
        } else if ([wsConn isKindOfClass:[FdClientWS class]]) {
            [wsConn send:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]];
        }
        
        return;
    }
    
    // Create a new client connection...
    NSNetService *netService = [self.servers objectForKey:to];
    if (netService != nil) {
        NSString *url = [NSString stringWithFormat:@"ws://%@:%d/dtalksrv", netService.hostName, netService.port];
        
        FdClientWS *clientWS = [[FdClientWS alloc] initWithURLRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:url]] andServiceName:to];
        if (clientWS != nil) {
            clientWS.delegate = clientWS;
            [clientWS open];
            
            NSError* error;
            NSData* data = [NSJSONSerialization dataWithJSONObject:event options:NSJSONWritingPrettyPrinted error:&error];
            [clientWS send:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]];
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
#pragma mark NSNetServiceBrowser Delegate Method Implementations
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)netServiceBrowser:(NSNetServiceBrowser *)netServiceBrowser didFindService:(NSNetService *)netService moreComing:(BOOL)moreServicesComing
{
    if([netService.name hasPrefix:@"@"]) {
        return;
    }
    
    // Make sure that we don't have such service already (why would this happen? not sure)
    if ([servers valueForKey:netService.name] == nil ) {
        // Set the delegate
        [netService setDelegate:self];
        
        // Add it to our list
        [servers setValue:netService forKey:netService.name];
        
        // Starts a resolve process
        [netService resolveWithTimeout:60.0];
    }
    
    // If more entries are coming, no need to update UI just yet
    if ( moreServicesComing ) {
        return;
    }
}

- (void)netServiceBrowser:(NSNetServiceBrowser *)netServiceBrowser didRemoveService:(NSNetService *)netService moreComing:(BOOL)moreServicesComing
{
    DDLogInfo(@"Service was removed: %@", netService.name);
    
    if([netService.name hasPrefix:@"@"]) {
        return;
    }
    
    // Remove the delegate
    [netService setDelegate:nil];
    [netService stopMonitoring];
    
    // Let our delegate know
    if (delegate) {
        [delegate onDeviceRemovedWithName:netService.name];
    }
    
    // Remove from list
    [servers removeObjectForKey:netService.name];

    // Let our delegate know
    if (delegate) {
        [delegate updateServerList];
    }
    
    // If more entries are coming, no need to update UI just yet
    if ( moreServicesComing ) {
        return;
    }
}

- (void)netService:(NSNetService *)sender didNotResolve:(NSDictionary *)errorDict
{
    DDLogInfo(@"The address for '%@' did not resolve: %@.", sender.name, errorDict);
    
    // Remove the delegate
    [sender setDelegate:nil];
    [sender stopMonitoring];
    
    // Remove from list
    [servers removeObjectForKey:sender.name];
    
    // Let our delegate know
    if (delegate) {
        [delegate updateServerList];
    }
}

- (void)netServiceDidResolveAddress:(NSNetService *)sender {
    DDLogVerbose(@"%@[%p]: %@ (%@)", THIS_FILE, self, THIS_METHOD, sender);
    
    NSDictionary *txtRecordDict = [NSNetService dictionaryFromTXTRecordData:sender.TXTRecordData];
    //DDLogVerbose(@"txtRecord: '%@'", txtRecordDict);
    
    NSData *dtalkData = [txtRecordDict valueForKey:@"dtalk"];
    if (dtalkData == nil) {
        DDLogWarn(@"Not valid service");
        return;
    }
    NSString *dtalk = [[NSString alloc] initWithData:dtalkData encoding:NSUTF8StringEncoding];
    
    NSData *dtypeData = [txtRecordDict valueForKey:@"dtype"];
    NSString *dtype = (dtypeData != nil) ? [[NSString alloc] initWithData:dtypeData encoding:NSUTF8StringEncoding] : @"Unknown";
    
    DDLogInfo(@"dtalk=%@, dtype=%@", dtalk, dtype);
    
    // Monitor service
    [sender startMonitoring];
    
    DDLogVerbose(@"delegate: %@", delegate);
    
    // Let our delegate know
    if (delegate) {
        [delegate updateServerList];
    }
    
    if (delegate) {
        [delegate onDeviceAddedWithName:sender.name];
    }
    
    //[self performSelector:@selector(sendHowdy:) withObject:sender.name afterDelay:10];
}

- (void) sendHowdy:(NSString *)to {
    // Send Howdy!
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:@"Howdy!" forKey:@"service"];
    [event setValue:to forKey:@"to"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.OutgoingMsg" object:nil userInfo:event];
}

- (void) netService:(NSNetService *)sender didUpdateTXTRecordData:(NSData *)data
{
    DDLogInfo(@"TXT record for '%@' has been changed.", sender.name);
    
    // Let our delegate know
    if (delegate) {
        [delegate updateServerList];
    }
}

- (void) netServiceWillResolve:(NSNetService *)sender {
    DDLogInfo(@"The service '%@' is ready to resolve.", sender.name);
}

@end
