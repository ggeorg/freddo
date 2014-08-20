//
//  FtvService2.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdService.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdService()
- (void) handleEvent:(NSDictionary *)event;
@end

@implementation FdService

@synthesize name, nameInReply, controller, refCnt;

- (id) init:(NSString *)_name withController:(FdViewController*)_controller
{
    if (self = [super init]) {
        name = _name;
        nameInReply = [NSString stringWithFormat:@"$%@", _name];
        refCnt = [[NSMutableDictionary alloc] init];
        controller = _controller;
        [self setup];
    }
    return self;
}

- (void)setup
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [[NSNotificationCenter defaultCenter] addObserverForName:name
                                                      object:nil
                                                       queue:[NSOperationQueue mainQueue]
                                                  usingBlock:^(NSNotification *note) {
                                                      [self handleEvent:[note userInfo]];
                                                  }];
    
    //[self set:options];
}

- (Boolean)start
{
    return TRUE;
}

- (void)stop
{
    // do nothing
}

#pragma mark
#pragma mark Event handler
#pragma mark

- (void)handleEvent:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if (![self onEvent:event]) {
            // Log event was not handled
            NSLog(@"Unhandled event: %@", event);
        }
    }
}

#pragma mark
#pragma mark Actions
#pragma mark

- (Boolean)onEvent:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if ([action isEqualToString:@"get"]) {
            NSString *_id = (NSString *)[event objectForKey:@"id"];
            NSString *property = (NSString *)[event objectForKey:@"params"];
            if (_id != nil && property != nil) {
                [self get:property request:event];
                return YES;
            }
        } else if ([action isEqualToString:@"set"]) {
            NSDictionary *params = (NSDictionary *)[event objectForKey:@"params"];
            if (params != nil) {
                [self set:params];
                return YES;
            }
        }
    }
    
    return NO;
}

- (void)get:(NSString *)property request:(NSDictionary*)request;
{
    // empty
}

- (void)set:(NSDictionary *)options
{
    // empty
}

#pragma mark
#pragma mark Fire Event
#pragma mark

- (NSString *) getCallbackService:(NSString *)event
{
    return [NSString stringWithFormat:@"%@.%@", self.nameInReply, event];
}

- (void)fireEvent:(NSString *)eventName
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSString *replyService = [self getCallbackService:eventName];
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:replyService forKey:@"service"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireBooleanEvent:(NSString *)eventName value:(Boolean)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);

    [self fireNumberEvent:eventName value:[[NSNumber alloc] initWithBool:value]];
}

- (void)fireNumberEvent:(NSString *)eventName value:(NSNumber *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSString *replyService = [self getCallbackService:eventName];
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:replyService forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireStringEvent:(NSString *)eventName value:(NSString *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSString *replyService = [self getCallbackService:eventName];
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:replyService forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireObjectEvent:(NSString *)eventName value:(NSDictionary *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSString *replyService = [self getCallbackService:eventName];
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:replyService forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireArrayEvent:(NSString *)eventName value:(NSArray *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSString *replyService = [self getCallbackService:eventName];
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:replyService forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

#pragma mark
#pragma mark Sent Response
#pragma mark

+ (NSMutableDictionary*)newResponse:(NSDictionary*)request
{
    NSString *_id = (NSString*)[request objectForKey:@"id"];
    if (_id != nil) {
        NSMutableDictionary *response = [[NSMutableDictionary alloc] initWithCapacity:4];
        [response setValue:@"1.0" forKey:@"dtalk"];
        [response setValue:_id forKey:@"service"];
        NSString *from = (NSString*)[request objectForKeyedSubscript:@"from"];
        if (from != nil) {
            // if 'from' is available, set 'from' as 'to' (recipient)
            [response setValue:from forKeyPath:@"to"];
        }
        return response;
    } else {
        return nil;
    }
}

+ (void)sendResponse:(NSDictionary *)response
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, response);
    
    NSString *to = (NSString*)[response objectForKey:@"to"];
    if (to != nil) {
        // Send as outgoing message...
        [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.OutgoingMsg" object:nil userInfo:response];
    } else {
        // Send as incoming message...
        [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:response];
    }
}

- (void)sendBooleanResponse:(Boolean)value request:(NSDictionary*)request
{
    [self sendNumberResponse:[[NSNumber alloc] initWithBool:value] request:request];
}

- (void)sendNumberResponse:(NSNumber *)value request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, value);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:value forKey:@"result"];
    
    // Send message...
    [FdService sendResponse:event];
}

- (void)sendStringResponse:(NSString *)value request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, value);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:value forKey:@"result"];
    
    // Send message...
    [FdService sendResponse:event];
}

- (void)sendObjectResponse:(NSDictionary *)value request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, value);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:value forKey:@"result"];
    
    // Send message...
    [FdService sendResponse:event];
}

- (void)sendArrayResponse:(NSArray *)value request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, value);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:value forKey:@"result"];
    
    // Send message...
    [FdService sendResponse:event];
}

- (void)sendGenericResponse:(id)value request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, value);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:value forKey:@"result"];
    
    // Send message...
    [FdService sendResponse:event];
}

- (void)sendErrorResponse:(NSString*)error request:(NSDictionary*)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, error);
    
    NSDictionary *event = [FdService newResponse:request];
    [event setValue:error forKey:@"error"];
    
    // Send message...
    [FdService sendResponse:event];
}

#pragma mark
#pragma mark Getters
#pragma mark

- (Boolean)getBooleanProperty:(NSDictionary *)options property:(NSString *)property
{
    NSNumber *value = (NSNumber *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return [value integerValue] == 1;
}

- (NSNumber *)getNumberProperty:(NSDictionary *)options property:(NSString *)property
{
    NSNumber *value = (NSNumber *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

- (NSString *)getStringProperty:(NSDictionary *)options property:(NSString *)property
{
    NSString *value = (NSString *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

- (NSDictionary *)getObjectProperty:(NSDictionary *)options property:(NSString *)property
{
    NSDictionary *value = (NSDictionary *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

- (NSArray *)getArrayProperty:(NSDictionary *)options property:(NSString *)property
{
    NSArray *value = (NSArray *)[options objectForKey:property];
    [options setValue:nil forKey:property];
    return value;
}

#pragma mark
#pragma mark Utility function
#pragma mark

- (void)startService:(NSString *)service
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, service);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:4];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:@"dtalk.Services" forKey:@"service"];
    [event setValue:@"start" forKey:@"action"];
    [event setValue:service forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)stopService:(NSString *)service
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, service);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:4];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:@"dtalk.Services" forKey:@"service"];
    [event setValue:@"stop" forKey:@"action"];
    [event setValue:service forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)forwardEvent:event topic:(NSString *)topic
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, topic);
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:topic object:nil userInfo:event];
}

@end
