//
//  FdChromecast.m
//  Freddo-Player
//
//  Created by George Georgopoulos on 3/31/14.
//  Copyright (c) 2014 ArkaSoft LLC. All rights reserved.
//

#import "FdChromecast.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdChromecast () {
    
}

@property GCKMediaControlChannel *mediaControlChannel;
@property GCKApplicationMetadata *applicationMetadata;
@property GCKDevice *selectedDevice;
@property(nonatomic, strong) GCKDeviceScanner *deviceScanner;
@property(nonatomic, strong) GCKDeviceManager *deviceManager;
@property(nonatomic, strong) NSString *contentId;

@end

@implementation FdChromecast

@synthesize mediaControlChannel, selectedDevice, deviceScanner, deviceManager, contentId;

static NSString * kReceiverAppID;

- (void)setup
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [super setup];
    
    // app id here that you get by registering with the Google Cast SDK Developer Console
    // https://cast.google.com/publish
    kReceiverAppID = @"EA9B09E7";
}

- (Boolean) start
{
    // Start Video service
    [self startService:@"dtalk.service.Video"];
    
    // Initialize device scanner
    if (self.deviceScanner == nil) {
        self.deviceScanner = [[GCKDeviceScanner alloc] init];
        [self.deviceScanner addListener:self];
    }
    if (!self.deviceScanner.scanning) {
        [self.deviceScanner startScan];
    }
    
    return TRUE;
}

- (void) stop
{
    if (self.deviceScanner != nil && self.deviceScanner.scanning) {
        [self.deviceScanner stopScan];
    }
    
    // Stop Video service
    [self stopService:@"dtalk.service.Video"];
}

- (Boolean)isConnected
{
    return deviceManager.isConnected;
}

- (void)deviceDisconnected {
    mediaControlChannel = nil;
    deviceManager = nil;
    selectedDevice = nil;
}

- (void)setDevice:(NSString *)deviceId
{
    if (selectedDevice != nil && [selectedDevice.deviceID isEqualToString:deviceId]) {
        return;
    }
    
    GCKDevice *device = nil;
    if (deviceId != nil) {
        for (GCKDevice *d in self.deviceScanner.devices) {
            if ([deviceId isEqualToString:d.deviceID]) {
                device = d;
                break;
            }
        }
    }
    
    if (selectedDevice != nil) {
        // TODO disconnect
    }
    
    selectedDevice = device;
    
    if (selectedDevice != nil) {
        NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
        deviceManager = [[GCKDeviceManager alloc] initWithDevice:selectedDevice
                                               clientPackageName:[info objectForKey:@"CFBundleIdentifier"]];
        deviceManager.delegate = self;
        [deviceManager connect];
    }
}

#pragma mark
#pragma mark Fire Custom Events
#pragma mark

- (void)fireCustomEvent:(NSString *)eventName
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:eventName forKey:@"service"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireCustomNumberEvent:(NSString *)eventName value:(NSNumber *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:eventName forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

- (void)fireCustomObjectEvent:(NSString *)eventName value:(NSDictionary *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, eventName);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:eventName forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];
}

#pragma mark
#pragma mark Action Events
#pragma mark

- (void)doPause:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [self.mediaControlChannel pause];
}

- (void)doSeekTo:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSNumber *sec = (NSNumber *)[event objectForKey:@"params"];
    if (sec != nil) {
        [self.mediaControlChannel seekToTimeInterval:sec.doubleValue];
    }
}

- (void)doPlay:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [self.mediaControlChannel play];
}

- (void)doStop:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [self.mediaControlChannel stop];
}

- (void)doCast:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *deviceId = (NSString *)[event objectForKey:@"params"];
    [self setDevice:deviceId];
}

- (void)doFireStatusEvent
{
    if (mediaControlChannel) {
        GCKMediaStatus *mediaStatus = mediaControlChannel.mediaStatus;
        if (!(mediaStatus.playerState == GCKMediaPlayerStateUnknown || mediaStatus.playerState == GCKMediaPlayerStateIdle)) {
            [self fireCustomObjectEvent:@"$dtalk.service.Video.onstatus" value:[self getInfo]];
        }
    }
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    if ([@"pause" isEqualToString:action]) {
        [self doPause:event];
        return YES;
    } else if ([@"seekTo" isEqualToString:action]) {
        [self doSeekTo:event];
        return YES;
    } else if([@"play" isEqualToString:action]) {
        [self doPlay:event];
        return YES;
    } else if ([@"stop" isEqualToString:action]) {
        [self doStop:event];
        return YES;
    } else if ([@"cast" isEqualToString:action]) {
        [self doCast:event];
        return YES;
    } else if ([@"fireStatusEvent" isEqualToString:action]) {
        [self doFireStatusEvent];
        return YES;
    }
    
    return [super onEvent:event];
}

#pragma mark
#pragma mark Get Events
#pragma mark

- (NSString *)getSrc
{
    return contentId;
}

- (NSNumber *)canPause
{
    return [[NSNumber alloc] initWithBool:([mediaControlChannel.mediaStatus isMediaCommandSupported:kGCKMediaCommandPause])];
}

- (NSNumber *) isPaused
{
    return [[NSNumber alloc] initWithBool:(mediaControlChannel.mediaStatus.playerState == GCKMediaPlayerStatePaused)];
}

- (NSNumber *) getCurrentPosition
{
    return [[NSNumber alloc] initWithDouble:mediaControlChannel.mediaStatus.streamPosition];
}

- (NSNumber *) getDuration
{
    return [[NSNumber alloc] initWithDouble:mediaControlChannel.mediaStatus.mediaInformation.streamDuration];
}

- (NSDictionary *)getInfo
{
    NSDictionary *info = [[NSMutableDictionary alloc] initWithCapacity:5];
    
    [info setValue:[self getSrc] forKey:@"src"];
    [info setValue:[self canPause] forKey:@"canPause"];
    [info setValue:[self isPaused] forKey:@"paused"];
    [info setValue:[self getCurrentPosition] forKey:@"position"];
    [info setValue:[self getDuration] forKey:@"duration"];
    
    return info;
}

- (void)get:(NSString *)property request:(NSDictionary *)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, property);
    
    if ([@"info" isEqualToString:property]) {
        [self sendObjectResponse:[self getInfo] request:request];
    }
}

#pragma mark
#pragma mark Set Events
#pragma mark

- (void)setSrc:(NSString *)src
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, src);
    
    if (!self.deviceManager || !self.deviceManager.isConnected) {
        DDLogError(@"Not connected");
        return;
    }
    
    contentId = src;
    
    // Define Media Metadata
    GCKMediaMetadata *metadata = [[GCKMediaMetadata alloc] init];
    
    [metadata setString:@"Big Buck Bunny (2008)" forKey:kGCKMetadataKeyTitle];
    
    [metadata setString:@"Big Buck Bunny tells the story of a giant rabbit with a heart bigger than "
     "himself. When one sunny day three rodents rudely harass him, something "
     "snaps... and the rabbit ain't no bunny anymore! In the typical cartoon "
     "tradition he prepares the nasty rodents a comical revenge."
                 forKey:kGCKMetadataKeySubtitle];
    
    [metadata addImage:[[GCKImage alloc] initWithURL:[[NSURL alloc] initWithString:@"http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg"]
                                               width:480
                                              height:360]];
    
    GCKMediaInformation *mi = [[GCKMediaInformation alloc] initWithContentID:contentId
                                                                  streamType:GCKMediaStreamTypeNone
                                                                 contentType:@"video/mp4"
                                                                    metadata:metadata
                                                              streamDuration:0
                                                                  customData:nil];
    
    [self.mediaControlChannel loadMedia:mi autoplay:FALSE playPosition:0];
}

- (void)set:(NSDictionary *)options
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, options);
    
    for(id key in options) {
        NSString *property = (NSString *)key;
        if([@"src" isEqualToString:property]) {
            NSString *src = [self getStringProperty:options property:property];
            if (src != nil) {
                [self setSrc:src];
            }
        }
    }
    
    [super set:options];
}

#pragma mark -
#pragma mark GCKDeviceScannerListener
#pragma mark -

- (void)publish:(NSString *)topic value:(NSDictionary *)value
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, topic);
    
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"dtalk"];
    [event setValue:topic forKey:@"service"];
    [event setValue:value forKey:@"params"];
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:topic object:nil userInfo:event];
}

- (void)deviceDidComeOnline:(GCKDevice *)device
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSDictionary *deviceInfo = [[NSMutableDictionary alloc] initWithCapacity:3];
    [deviceInfo setValue:device.deviceID forKeyPath:@"id"];
    [deviceInfo setValue:device.friendlyName forKeyPath:@"name"];
    [deviceInfo setValue:self.name forKeyPath:@"protocol"];
    
    [self publish:@"dtalk.event.CastDeviceDidComeOnline" value:deviceInfo];
 }

- (void)deviceDidGoOffline:(GCKDevice *)device
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSDictionary *deviceInfo = [[NSMutableDictionary alloc] initWithCapacity:3];
    [deviceInfo setValue:device.deviceID forKeyPath:@"id"];
    [deviceInfo setValue:device.friendlyName forKeyPath:@"name"];
    [deviceInfo setValue:self.name forKeyPath:@"protocol"];
    
    [self publish:@"dtalk.event.CastDeviceDidGoOffline" value:deviceInfo];
}

#pragma mark -
#pragma mark GCKDeviceManagerDelegate
#pragma mark -

- (void)deviceManagerDidConnect:(GCKDeviceManager *)deviceManager
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [self.deviceManager launchApplication:kReceiverAppID];
}

- (void)deviceManager:(GCKDeviceManager *)deviceManager didConnectToCastApplication:(GCKApplicationMetadata *)applicationMetadata
            sessionID:(NSString *)sessionID
  launchedApplication:(BOOL)launchedApplication
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    mediaControlChannel = [[GCKMediaControlChannel alloc] init];
    mediaControlChannel.delegate = self;
    [self.deviceManager addChannel:self.mediaControlChannel];
    [mediaControlChannel requestStatus];
}

- (void)deviceManager:(GCKDeviceManager *)deviceManager didFailToConnectToApplicationWithError:(NSError *)error
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, error);
    
    //[self showError:error];
    [self deviceDisconnected];
}

- (void)deviceManager:(GCKDeviceManager *)deviceManager didFailToConnectWithError:(GCKError *)error
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, error);
    
    //[self showError:error];
    [self deviceDisconnected];
}

- (void)deviceManager:(GCKDeviceManager *)deviceManager didDisconnectWithError:(GCKError *)error
{
    DDLogVerbose(@"%@[%p]: %@:%@", THIS_FILE, self, THIS_METHOD, error);
    
    if (error != nil) {
        //[self showError:error];
    }
    
    [self deviceDisconnected];
}

- (void)deviceManager:(GCKDeviceManager *)deviceManager didReceiveStatusForApplication:(GCKApplicationMetadata *)applicationMetadata
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    self.applicationMetadata = applicationMetadata;
}

#pragma mark -
#pragma mark GCKMediaControlChannelDelegate
#pragma mark -

/**
 * Called when updated player status information is received.
 */
- (void)mediaControlChannelDidUpdateStatus:(GCKMediaControlChannel *)mediaControlChannel
{
    // DDLogVerbose(@"%@[%p]: %@:%d", THIS_FILE, self, THIS_METHOD, [self.mediaControlChannel requestStatus]);
    
    GCKMediaStatus *mediaStatus = mediaControlChannel.mediaStatus;
    
    switch(mediaStatus.playerState) {
            /** Constant indicating unknown player state. */
        case GCKMediaPlayerStateUnknown:
            break;
            
            /** Constant indicating that the media player is idle. */
        case GCKMediaPlayerStateIdle:
            switch(mediaStatus.idleReason) {
                    /** Constant indicating that the player currently has no idle reason. */
                case GCKMediaPlayerIdleReasonNone:
                    break;
                    
                    /** Constant indicating that the player is idle because playback has finished. */
                case GCKMediaPlayerIdleReasonFinished:
                    DDLogCVerbose(@"Fire oncompletion...");
                    [self fireCustomNumberEvent:@"$dtalk.service.Video.oncompletion" value:[[NSNumber alloc] initWithInt:0]];
                    break;
                    
                    /**
                     * Constant indicating that the player is idle because playback has been cancelled in
                     * response to a STOP command.
                     */
                case GCKMediaPlayerIdleReasonCancelled:
                    break;
                    
                    /**
                     * Constant indicating that the player is idle because playback has been interrupted by
                     * a LOAD command.
                     */
                case GCKMediaPlayerIdleReasonInterrupted:
                    break;
                    
                    /** Constant indicating that the player is idle because a playback error has occurred. */
                case GCKMediaPlayerIdleReasonError:
                    DDLogCVerbose(@"Fire error...");

                    NSDictionary *error = [[NSMutableDictionary alloc] initWithCapacity:3];
                    [error setValue:@"Can't play video" forKey:@"message"];
                    // TODO error code

                    [self fireCustomNumberEvent:@"$dtalk.service.Video.oncompletion" value:[[NSNumber alloc] initWithInt:2]];
                    [self fireCustomObjectEvent:@"$dtalk.service.Video.onerror" value:error];
                    break;
            }
            break;
            
            /** Constant indicating that the media player is playing. */
        case GCKMediaPlayerStatePlaying:
            DDLogCVerbose(@"Fire onprepared...");
            [self fireCustomEvent:@"$dtalk.service.Video.onprepared"];
            break;
            
            /** Constant indicating that the media player is paused. */
        case GCKMediaPlayerStatePaused:
            break;
            
            /** Constant indicating that the media player is buffering. */
        case GCKMediaPlayerStateBuffering:
            break;
    }
}

@end
