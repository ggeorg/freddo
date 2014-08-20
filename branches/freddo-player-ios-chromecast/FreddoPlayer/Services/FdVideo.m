//
//  FtvVideo.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 10/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <MediaPlayer/MediaPlayer.h>

#import "FdVideo.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

typedef enum {
    PLAYING, BUFFERING, IDLE
} FdVideoPlaybackState;

@interface FdVideo()

@property(nonatomic, strong) MPMoviePlayerController *moviePlayerController;
@property(nonatomic) NSString *source;

@property(nonatomic) NSTimer *stateTimer;

@property(nonatomic,readonly) NSMutableDictionary *mCastDevices;
@property(atomic) NSString *mCastProtocol;

@property(atomic) FdVideoPlaybackState mPlaybackState;

- (void)playbackDidFinishNotification:(NSNotification*)notification;
- (void)loadStateDidChangeNotification:(NSNotification*)notification;

- (void)handleCastDeviceDidComeOnline:(NSDictionary*)event;
- (void)handleCastDeviceDidGoOffline:(NSDictionary*)event;

- (void) onStateTimerCb;

@end

@implementation FdVideo {
    NSMutableDictionary *mItem;
}

@synthesize moviePlayerController, stateTimer, mCastDevices, mCastProtocol, mPlaybackState;

- (void)setup
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    [super setup];
    
    mCastDevices = [[NSMutableDictionary alloc] init];
    mCastProtocol = nil;
    mItem = nil;
    
    mPlaybackState = IDLE;
    
    // ...
    moviePlayerController = [[MPMoviePlayerController alloc] init];
    moviePlayerController.scalingMode = MPMovieScalingModeAspectFit;
    moviePlayerController.shouldAutoplay = YES;
    moviePlayerController.allowsAirPlay = NO;
    moviePlayerController.fullscreen = NO;
    moviePlayerController.controlStyle = MPMovieControlStyleNone;
    
    //moviePlayerController.view.opaque = YES;
    moviePlayerController.view.backgroundColor = [UIColor blackColor];

    moviePlayerController.view.frame = CGRectInset([self.controller.view bounds], 0, 0);
    moviePlayerController.view.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    
    [self.controller.view addSubview:moviePlayerController.view];
    [self.controller.view sendSubviewToBack:moviePlayerController.view];
    
    //
    // Notifications
    //
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(playbackDidFinishNotification:)
                                                 name:MPMoviePlayerPlaybackDidFinishNotification
                                               object:moviePlayerController];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(loadStateDidChangeNotification:)
                                                 name:MPMoviePlayerLoadStateDidChangeNotification
                                               object:moviePlayerController];
    
    //
    // statusTimer
    //
    
    stateTimer = [NSTimer scheduledTimerWithTimeInterval:1
                                                  target:self
                                                selector:@selector(onStateTimerCb)
                                                userInfo:nil
                                                 repeats:YES];
    
    //
    // Video casting events
    //
    
    [[NSNotificationCenter defaultCenter] addObserverForName:@"dtalk.event.CastDeviceDidComeOnline"
                                                      object:nil
                                                       queue:[NSOperationQueue mainQueue]
                                                  usingBlock:^(NSNotification *note) {
                                                      [self handleCastDeviceDidComeOnline:[note userInfo]];
                                                  }];
    
    [[NSNotificationCenter defaultCenter] addObserverForName:@"dtalk.event.CastDeviceDidGoOffline"
                                                      object:nil
                                                       queue:[NSOperationQueue mainQueue]
                                                  usingBlock:^(NSNotification *note) {
                                                      [self handleCastDeviceDidGoOffline:[note userInfo]];
                                                  }];
}

#pragma mark
#pragma mark Notifications
#pragma mark

/**
 * Posted when a movie has finished playing.
 *
 * Constants:
 *
 *  - MPMovieFinishReasonPlaybackEnded: The end of the movie was reached.
 *  - MPMovieFinishReasonPlaybackError: There was an error during playback.
 *  - MPMovieFinishReasonUserExited:    The user stopped playback.
 */
- (void) playbackDidFinishNotification:(NSNotification*)notification {
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    // Obtain the reason why the movie playback finished
    NSNumber *finishReason = [[notification userInfo] objectForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey];
    
    if ([finishReason intValue] == MPMovieFinishReasonPlaybackError) {
        DDLogVerbose(@">>> onError");
        
        NSDictionary *error = [[NSMutableDictionary alloc] initWithCapacity:3];
        [error setValue:@"Can't play video" forKey:@"message"];
        // TODO error code
        
        [self fireNumberEvent:@"oncompletion" value:[[NSNumber alloc] initWithInt:2]];
        [self fireObjectEvent:@"onerror" value:error];
        
    } else if ([finishReason intValue] == MPMovieFinishReasonUserExited) {
        DDLogVerbose(@">>> onStopped");
        
        [self fireNumberEvent:@"oncompletion" value:[[NSNumber alloc] initWithInt:-1]];
        
    } else if ([finishReason intValue] == MPMovieFinishReasonPlaybackEnded) {
        DDLogVerbose(@">>> onCompletion %d", moviePlayerController.playbackState);
        
        if (moviePlayerController.playbackState == MPMoviePlaybackStatePaused) {
            [self fireNumberEvent:@"oncompletion" value:[[NSNumber alloc] initWithInt:0]];
        } else {
            [self fireNumberEvent:@"oncompletion" value:[[NSNumber alloc] initWithInt:1]];
        }
    }
}

/**
 * Posted when the ready for display state changes.
 *
 */
- (void) loadStateDidChangeNotification:(NSNotification*)notification
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (moviePlayerController.loadState & MPMovieLoadStatePlayable) {
        [self fireEvent:@"onprepared"];
    }
}

- (void)handleCastDeviceDidComeOnline:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSDictionary *device = [event objectForKey:@"params"];
    if (device != nil) {
        [mCastDevices setValue:device forKeyPath:[device objectForKey:@"id"]];
    }
}

- (void)handleCastDeviceDidGoOffline:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSDictionary *device = [event objectForKey:@"params"];
    if (device != nil) {
        [mCastDevices removeObjectForKey:[device objectForKey:@"id"]];
    }
}

#pragma mark
#pragma mark Action Events
#pragma mark

/**
 * Pauses playback of the current item.
 *
 * If playback is not currently underway, this method has no effect. To resume playback of the 
 * current item from the pause point, call the play method.
 */
- (void)doPause:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (mCastProtocol != nil) {
        [self forwardEvent:event topic:mCastProtocol];
    } else {
        [moviePlayerController pause];
    }
}

/**
 * Moves the playhead to the new location.
 *
 * For video-on-demand or progressively downloaded content, this value is measured in seconds
 * from the beginning of the current item. For content streamed live from a server, this value
 * represents the time from the beginning of the playlist when it was first loaded.
 */
- (void)doSeekTo:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (mCastProtocol != nil) {
        [self forwardEvent:event topic:mCastProtocol];
    } else {
        NSNumber *sec = (NSNumber *)[event objectForKey:@"params"];
        if (sec != nil) {
            [moviePlayerController setCurrentPlaybackTime:(NSTimeInterval)[sec doubleValue]];
        }
    }
}

/**
 * Initiates playback of the current item.
 *
 * If playback was previously paused, this method resumes playback where it left; otherwise, this
 * method plays the first available item, from the beginning.
 */
- (void)doPlay:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (mCastProtocol != nil) {
        [self forwardEvent:event topic:mCastProtocol];
    } else {
        [moviePlayerController play];
    }
}

/**
 * Ends playback of the current item.
 *
 * This method stops playback of the current item and resets the playhead to the start of the item.
 * Calling the play method again initiates playback from the beginning of the item.
 */
- (void)doStop:(NSDictionary *)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (mCastProtocol != nil) {
        [self forwardEvent:event topic:mCastProtocol];
    } else {
        [moviePlayerController stop];
    }
    
    mPlaybackState = IDLE;
    
    if (mCastProtocol != nil) {
        [self fireNumberEvent:@"oncompletion" value:[[NSNumber alloc] initWithInt:-1]];
    }
}

/**
 * Start video casting.
 *
 */
- (void)doCast:(NSDictionary*)event
{
    DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    NSString *deviceId = (NSString *)[event objectForKey:@"params"];
    
    if (deviceId != nil) {
        NSDictionary *castDevice = [mCastDevices objectForKey:deviceId];
        if (castDevice != nil) {
            NSString *protocol = [castDevice objectForKeyedSubscript:@"protocol"];
            if (protocol != nil) {
                //mCastCurrentPlayback = false;
                if (mCastProtocol != nil) {
                    // TODO check previous protocol...
                } else if ([moviePlayerController playbackState] != MPMoviePlaybackStateStopped
                           && [moviePlayerController playbackState] != MPMoviePlaybackStateInterrupted) {
                    //mCastCurrentPlayback = true;
                }
                mCastProtocol = protocol;
                [self forwardEvent:event topic:mCastProtocol];
            } else {
                [self sendErrorResponse:@"Cast protocol not defined" request:event];
            }
        } else {
            [self sendErrorResponse:@"Cast device not found" request:event];
        }
    } else if(mCastProtocol != nil) {
        NSString * protocol = mCastProtocol;
        mCastProtocol = nil;
        [self forwardEvent:event topic:protocol];
    } else {
        [self sendErrorResponse:@"Invalid request" request:event];
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
    }
    
    return [super onEvent:event];
}

#pragma mark
#pragma mark Get Events
#pragma mark

- (NSString *)getSrc
{
    return [[moviePlayerController contentURL] absoluteString];
}

- (NSNumber *) isPaused
{
    return [[NSNumber alloc] initWithBool:(moviePlayerController.playbackState == MPMoviePlaybackStatePaused)];
}

- (NSNumber *) getCurrentPosition
{
    return [[NSNumber alloc] initWithDouble:moviePlayerController.currentPlaybackTime];
}

/**
 * The duration of the movie, measured in seconds (read-only).
 * If the duration of the movie is not known, the value in this property is 0.0.
 */
- (NSNumber *) getDuration
{
    return [[NSNumber alloc] initWithDouble:moviePlayerController.duration];
}

- (NSDictionary *)getInfo
{
    NSDictionary *info = [[NSMutableDictionary alloc] initWithCapacity:5];
    
    [info setValue:[self getSrc] forKey:@"src"];
    //[info setValue:<#(id)#> forKey:@"canPause"];
    [info setValue:[self isPaused] forKey:@"paused"];
    [info setValue:[self getCurrentPosition] forKey:@"position"];
    [info setValue:[self getDuration] forKey:@"duration"];
    
    return info;
}

- (NSArray *)getCastDevices
{
    NSMutableArray *result = [NSMutableArray array];
    for (NSDictionary *device in [mCastDevices allValues]) {
        [result addObject:device];
    }
    return result;
}

-(void)getItem:(NSDictionary *)request
{
    if (mItem == nil) {
        mItem = [NSMutableDictionary dictionary];
    }
    [mItem removeObjectForKey:@"src"];
    if ([self getSrc] != nil) {
        [mItem setObject:[self getSrc] forKey:@"src"];
    }
    [self sendObjectResponse:mItem request:request];
}

- (void)get:(NSString *)property request:(NSDictionary *)request
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, property);
    
    if ([@"src" isEqualToString:property]) {
        [self sendStringResponse:[self getSrc] request:request];
    } else if ([@"info" isEqualToString:property]) {
        if (mCastProtocol != nil) {
            [self forwardEvent:request topic:mCastProtocol];
        } else {
            [self sendObjectResponse:[self getInfo] request:request];
        }
    } else if ([@"castDevices" isEqualToString:property]) {
        [self sendArrayResponse:[self getCastDevices] request:request];
    } else if ([@"item" isEqualToString:property]) {
        [self getItem:request];
    }
}

#pragma mark
#pragma mark Set Events
#pragma mark

- (void)setSrc:(NSString *)src
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, src);
    
    mPlaybackState = BUFFERING;
    
    self.source = src;
    mItem = nil;
    
    if (mCastProtocol != nil) {
        NSDictionary *params = [[NSMutableDictionary alloc] initWithCapacity:1];
        [params setValue:src forKey:@"src"];

        NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:4];
        [event setValue:@"1.0" forKey:@"dtalk"];
        [event setValue:mCastProtocol forKey:@"service"];
        [event setValue:@"set" forKey:@"action"];
        [event setValue:params forKey:@"params"];
        
        [self forwardEvent:event topic:mCastProtocol];
    } else {
        [moviePlayerController setContentURL:[NSURL URLWithString:src]];
    }
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
        } else if([@"item" isEqualToString:property]) {
            NSDictionary *item = [options objectForKey:@"item"];
            if (item != nil) {
                NSString *src = [item objectForKey:@"src"];
                if (src != nil) {
                    [self setSrc:src];
                }
                // NOTE: setSrc will clear this.item; we set self.mItem
                // after setSrc() call...
                mItem = [NSMutableDictionary dictionaryWithDictionary:item];
            }
        }
    }
    
    [super set:options];
}

#pragma mark
#pragma mark Player State Monitor
#pragma mark

- (void) onStateTimerCb
{
    //DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    
    if (mCastProtocol != nil) {
        if (mPlaybackState != IDLE) {
            NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
            [event setValue:@"1.0" forKey:@"dtalk"];
            [event setValue:mCastProtocol forKey:@"service"];
            [event setValue:@"fireStatusEvent" forKey:@"action"];
            [self forwardEvent:event topic:mCastProtocol];
        }
    } else {
        if (moviePlayerController.playbackState == MPMoviePlaybackStatePlaying
            || moviePlayerController.playbackState == MPMoviePlaybackStatePaused) {
            
            // TODO broadcast player state
            DDLogVerbose(@"Broadcast player state");
            
            [self fireObjectEvent:@"onstatus" value:[self getInfo]];
        }
    }
}

@end
