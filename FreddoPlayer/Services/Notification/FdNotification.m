#import "FdNotification.h"

@implementation FdNotification

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    if ([@"vibrate" isEqualToString:action]) {
        [self vibrate:event];
        return YES;
    } else if ([@"beep" isEqualToString:action]) {
        NSArray *params = [event objectForKey:@"params"];
        NSNumber *count = nil;
        if ([params isKindOfClass:[NSNull class]] || params == nil) {
            count = [NSNumber numberWithInt:1];
        } else {
            count = [params objectAtIndex:0];
        }
        [self beep:count withRequest:event];
        return YES;
    }
    return [super onEvent:event];
}

- (void)vibrate:(NSDictionary*)request
{
    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
    [self sendObjectResponse:[NSDictionary dictionary] request:request];
}

static void playBeep(int count) {
    SystemSoundID completeSound;
    NSURL* audioPath = [[NSBundle mainBundle] URLForResource:@"FdNotification.bundle/beep" withExtension:@"wav"];
    #if __has_feature(objc_arc)
        AudioServicesCreateSystemSoundID((__bridge CFURLRef)audioPath, &completeSound);
    #else
        AudioServicesCreateSystemSoundID((CFURLRef)audioPath, &completeSound);
    #endif
    AudioServicesAddSystemSoundCompletion(completeSound, NULL, NULL, soundCompletionCallback, (void*)(count-1));
    AudioServicesPlaySystemSound(completeSound);
}

static void soundCompletionCallback(SystemSoundID  ssid, void* data) {
    int count = (int)data;
    AudioServicesRemoveSystemSoundCompletion (ssid);
    AudioServicesDisposeSystemSoundID(ssid);
    if (count > 0) {
        playBeep(count);
    }
}

- (void)beep:(NSNumber*)count withRequest:(NSDictionary *)request
{
    playBeep([count intValue]);
    [self sendObjectResponse:[NSDictionary dictionary] request:request];
}


@end