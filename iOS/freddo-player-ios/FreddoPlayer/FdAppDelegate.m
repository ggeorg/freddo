//
//  FtvAppDelegate.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdAppDelegate.h"

#import "FdHTTPConnection.h"

#import "FdViewController.h"
#import "FdServerBrowser.h"

#import "HTTPServer.h"

#import "DDLog.h"
#import "DDTTYLogger.h"
#import "TestFlight.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdAppDelegate

@synthesize window;
@synthesize viewController;

- (void)sendDeviceReady:(int)listeningPort
{
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:4];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:@"freddotv.System" forKey:@"service"];
    [event setValue:@"deviceReady" forKey:@"action"];
    [event setValue:[[NSNumber alloc] initWithInt:listeningPort] forKey:@"params"];
    
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.System" object:nil userInfo:event];
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [TestFlight takeOff:@"70d034e6-fa26-41fa-888b-292a8086bbaa"];
    // ---------------------------------------------------------------------------------------------------
    // Configure our logging framework.
	// To keep things simple and fast, we're just going to log to the Xcode console.
	[DDLog addLogger:[DDTTYLogger sharedInstance]];
    
    // Create server
    httpServer = [[HTTPServer alloc] init];
    //[serverBrowser setDelegate:self];
    
    // Create HTTP server browser
    serverBrowser = [FdServerBrowser sharedInstance:httpServer];
    
    // Tell server to use our custom FtvHTTPConnection class.
	[httpServer setConnectionClass:[FdHTTPConnection class]];
    
    // Tell the server to broadcast its presents via Bonjour.
    // This allows browsers such as Safari to automatically discover our service.
    [httpServer setType:@"_http._tcp."];
    //[httpServer setName:@"Freddo Player"];
    
   	// Normally there's no need to run our server on any specific port.
	// Technologies like Bonjour allow clients to dynamically discover the server's port at runtime.
	// However, for easy testing you may want force a certain port so you can just hit the refresh button.
	//[httpServer setPort:57035];
    
    // Serve files from our embedded www folder
	NSString *webPath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"www"];
	DDLogInfo(@"Setting document root: %@", webPath);
    
    [httpServer setDocumentRoot:webPath];
    // ---------------------------------------------------------------------------------------------------
    
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    // Override point for customization after application launch.
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        self.viewController = [[FdViewController alloc] initWithNibName:@"FdViewController_iPhone" bundle:nil];
    } else {
        self.viewController = [[FdViewController alloc] initWithNibName:@"FdViewController_iPad" bundle:nil];
    }
    self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];
    
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    
    // ---------------------------------------------------------------------------------------------------
    // There is no public(allowed in AppStore) method for iOS to run continiously in the background for our purposes (serving HTTP).
    // So, we stop the server when the app is paused (if a users exits from the app or locks a device) and
    // restart the server when the app is resumed (based on this document: http://developer.apple.com/library/ios/#technotes/tn2277/_index.html )
    
    [serverBrowser setDelegate:nil];
    [serverBrowser stop];
    
    [httpServer stop];
    // ----------------------------------------------------------------------------------------------------
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    BOOL isRenderer = [defaults boolForKey:@"pref_dtype_renderer"];
    
    // FreddoTV related TXT record dictionary
    NSMutableDictionary *txtRecordDict = [NSMutableDictionary dictionaryWithCapacity:8];
    [txtRecordDict setObject:@"1" forKey:@"dtalk"];
    if (isRenderer) {
        [txtRecordDict setObject:@"Renderer/1" forKey:@"dtype"];
    } else {
        [txtRecordDict setObject:@"Controller/1" forKey:@"dtype"];
    }
    [httpServer setTXTRecordDictionary:txtRecordDict];
    
    // ---------------------------------------------------------------------------------------------------
    // Start the server (add check for problems)
    NSError *error;
    if ([httpServer start:&error]) {
        [self sendDeviceReady:[httpServer listeningPort]];
        
        // Start the HTTP server browser
        [serverBrowser start];
    } else {
        DDLogError(@"Error starting HTTP Server: %@", error);
    }
    // ---------------------------------------------------------------------------------------------------
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

@end
