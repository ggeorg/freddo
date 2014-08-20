#import "FdConnection.h"
#import "FdReachability.h"

@interface FdConnection (PrivateMethods)
- (void)updateOnlineStatus;
- (void)sendPluginResult;
@end

@implementation FdConnection

@synthesize connectionType, internetReach;

- (void)get:(NSString *)property request:(NSDictionary*)request;
{
    if ([@"connection" isEqualToString:property]) {
        NSString *returnInfo = self.connectionType;
        [self sendStringResponse:returnInfo request:request];
    }
}

- (NSString*)w3cConnectionTypeFor:(FdReachability*)reachability
{
    NetworkStatus networkStatus = [reachability currentReachabilityStatus];

    switch (networkStatus) {
        case NotReachable:
            return @"none";

        case ReachableViaWWAN:
            return @"cellular";

        case ReachableViaWiFi:
            return @"wifi";

        default:
            return @"unknown";
    }
}

- (BOOL)isCellularConnection:(NSString*)theConnectionType
{
    return [theConnectionType isEqualToString:@"2g"] ||
           [theConnectionType isEqualToString:@"3g"] ||
           [theConnectionType isEqualToString:@"4g"] ||
           [theConnectionType isEqualToString:@"cellular"];
}

- (void)updateReachability:(FdReachability*)reachability
{
    if (reachability) {
        // check whether the connection type has changed
        NSString* newConnectionType = [self w3cConnectionTypeFor:reachability];
        if ([newConnectionType isEqualToString:self.connectionType]) { // the same as before, remove dupes
            return;
        } else {
            self.connectionType = [self w3cConnectionTypeFor:reachability];
        }
    }
    [self sendPluginResult];
}

- (void)updateConnectionType:(NSNotification*)note
{
    FdReachability* curReach = [note object];

    if ((curReach != nil) && [curReach isKindOfClass:[FdReachability class]]) {
        [self updateReachability:curReach];
    }
}

- (void)onPause
{
    [self.internetReach stopNotifier];
}

- (void)onResume
{
    [self.internetReach startNotifier];
    [self updateReachability:self.internetReach];
}

-(void) setup {
    [super setup];
    self.connectionType = @"none";
    self.internetReach = [FdReachability reachabilityForInternetConnection];
    self.connectionType = [self w3cConnectionTypeFor:self.internetReach];
    [self.internetReach startNotifier];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateConnectionType:)
                                                 name:kReachabilityChangedNotification object:nil];
    if (&UIApplicationDidEnterBackgroundNotification && &UIApplicationWillEnterForegroundNotification) {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause) name:UIApplicationDidEnterBackgroundNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume) name:UIApplicationWillEnterForegroundNotification object:nil];
    }
}

@end
