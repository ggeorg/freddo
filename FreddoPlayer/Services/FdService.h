//
//  FtvService2.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "FdViewController.h"

@interface FdService : NSObject

@property (nonatomic,readonly) NSString *name;
@property (nonatomic,readonly) NSString *nameInReply;

@property (nonatomic,readonly) FdViewController *controller;

@property (nonatomic,readonly) NSMutableDictionary *refCnt;

- (id) init:(NSString *)name withController:(FdViewController*)_controller;

- (void)setup;
- (Boolean)onEvent:(NSDictionary*)event;
- (void)get:(NSString *)property request:(NSDictionary*)request;
- (void)set:(NSDictionary *)options;

- (Boolean)start;
- (void)stop;

- (void)fireEvent:(NSString *)event;
- (void)fireBooleanEvent:(NSString *)event value:(Boolean)value;
- (void)fireNumberEvent:(NSString *)event value:(NSNumber *)value;
- (void)fireStringEvent:(NSString *)event value:(NSString *)value;
- (void)fireObjectEvent:(NSString *)event value:(NSDictionary *)value;
- (void)fireArrayEvent:(NSString *)event value:(NSArray *)value;

- (void)sendGenericResponse:(id)value request:(NSDictionary*)request;
- (void)sendBooleanResponse:(Boolean)value request:(NSDictionary*)request;
- (void)sendNumberResponse:(NSNumber *)value request:(NSDictionary*)request;
- (void)sendStringResponse:(NSString *)value request:(NSDictionary*)request;
- (void)sendObjectResponse:(NSDictionary *)value request:(NSDictionary*)request;
- (void)sendArrayResponse:(NSArray *)value request:(NSDictionary*)request;

- (void)sendErrorResponse:(NSString*)error request:(NSDictionary*)request; // TODO add more object types...

- (Boolean)getBooleanProperty:(NSDictionary *)options property:(NSString *)property;
- (NSNumber *)getNumberProperty:(NSDictionary *)options property:(NSString *)property;
- (NSString *)getStringProperty:(NSDictionary *)options property:(NSString *)property;
- (NSDictionary *)getObjectProperty:(NSDictionary *)options property:(NSString *)property;
- (NSArray *)getArrayProperty:(NSDictionary *)options property:(NSString *)property;

- (void)startService:(NSString *)service;
- (void)stopService:(NSString *)service;
- (void)forwardEvent:event topic:(NSString *)topic;

@end