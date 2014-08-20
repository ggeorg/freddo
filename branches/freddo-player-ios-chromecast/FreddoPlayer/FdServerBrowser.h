//
//  FtvHTTPServerBrowser.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

@class HTTPServer;

@protocol FdServerBrowserDelegate
- (void)updateServerList;
- (void)onDeviceAddedWithName:(NSString *)name;
- (void)onDeviceRemovedWithName:(NSString *)name;
@end

@interface FdServerBrowser : NSObject <NSNetServiceBrowserDelegate, NSNetServiceDelegate> {
    id<FdServerBrowserDelegate> delegate;
    
    NSNetServiceBrowser *netServiceBrowser;
    NSMutableDictionary *servers;
    
    NSMutableDictionary *connections;
    NSMutableDictionary *subscriptions;
}

@property(nonatomic,strong) id<FdServerBrowserDelegate> delegate;
@property(nonatomic,readonly) NSDictionary *servers;
@property(nonatomic,readonly) NSDictionary *connections;

+ (id)sharedInstance;

+ (id)sharedInstance:(HTTPServer *)httpServer;

/**
 * Start browsing for Bonjour services.
 */
- (BOOL) start;

/**
 * Stop everything.
 */
- (void) stop;

-(NSString *)getPublishedName;

@end
