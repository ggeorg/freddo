//
//  FdDeviceBrowser.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 19/11/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdPresence.h"
#import "FdServerBrowser.h"
#import "DDLog.h"

static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdPresence

- (void)setup {
    [super setup];
    FdServerBrowser *browser = [FdServerBrowser sharedInstance];
    browser.delegate = self;
}

- (void)get:(NSString *)property request:(NSDictionary *)request {
    if ([@"list" isEqualToString:property]) {
        [super sendArrayResponse:[self listDevices] request:request];
    }
}

- (NSDictionary *) getDeviceInfoByName:(NSString *)name {
    FdServerBrowser *browser = [FdServerBrowser sharedInstance];
    NSNetService *netService = [browser.servers objectForKey:name];
    if (netService.TXTRecordData == nil) {
        // not yet resolved
        return nil;
    }
    
    return [self getDeviceInfo:netService];
}

- (NSDictionary *) getDeviceInfo:(NSNetService *)netService {
    NSString *name = netService.name;
    NSInteger port = netService.port;
    NSString *hostname = netService.hostName;
    
    NSDictionary *txtRecordDict = [NSNetService dictionaryFromTXTRecordData:netService.TXTRecordData];
    // DDLogVerbose(@"txtRecord: '%@'", txtRecordDict);
    
    NSData *dtalkData = [txtRecordDict valueForKey:@"dtalk"];
    if (dtalkData == nil) {
        DDLogWarn(@"Not valid service");
        return nil;
    }
    NSString *dtalk = [[NSString alloc] initWithData:dtalkData encoding:NSUTF8StringEncoding];
    
    NSData *dtypeData = [txtRecordDict valueForKey:@"dtype"];
    NSString *dtype = (dtypeData != nil) ? [[NSString alloc] initWithData:dtypeData encoding:NSUTF8StringEncoding] : @"Unknown";
    
    
    NSMutableDictionary *device = [NSMutableDictionary dictionary];
    [device setObject:name forKey:@"name"];
    [device setObject:hostname forKey:@"server"];
    [device setObject:[NSNumber numberWithInteger:port] forKey:@"port"];
    [device setObject:dtalk forKey:@"dtalk"];
    [device setObject:dtype forKey:@"dtype"];

    return device;
}

- (NSArray *)listDevices {
    FdServerBrowser *browser = [FdServerBrowser sharedInstance];
    NSMutableArray *result = [NSMutableArray array];
    for (NSNetService *netService in [browser.servers allValues]) {
        NSDictionary *device = [self getDeviceInfo:netService];
        if (device == nil)
            continue;
        NSString *name = [device objectForKey:@"name"];
        if ([name isEqualToString:[browser getPublishedName]]) {
            // don't include the local device in the list.
            continue;
        }
        
        [result addObject:device];
    }
    return result;
}

#pragma mark -
#pragma mark FtvHTTPServerBrowserDelegate Implementation

- (void) updateServerList {
    FdServerBrowser *browser = [FdServerBrowser sharedInstance];
    for (NSNetService *netService in [browser.servers allValues]) {
        if (netService.TXTRecordData == nil) {
            // not yet resolved
            continue;
        }
        
        // Fire presence change....
    }
}


-(void)onDeviceAddedWithName:(NSString *)name {
    DDLogVerbose(@"%@[%p]: %@ (%@)", THIS_FILE, self, THIS_METHOD, name);
    
    NSDictionary *device = [self getDeviceInfoByName:name];
    if (device != nil) {
        [super fireObjectEvent:@"resolved" value:device];
    }
}

-(void)onDeviceRemovedWithName:(NSString *)name {
    DDLogVerbose(@"%@[%p]: %@ (%@)", THIS_FILE, self, THIS_METHOD, name);
    
    NSDictionary *device = [self getDeviceInfoByName:name];
    if (device != nil) {
        [super fireObjectEvent:@"removed" value:device];
    }
}

@end
