//
//  FtvViewController.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdViewController.h"

#import "WeView.h"

#import "FdWebViewDelegate.h"
#import "FdServiceMgr.h"
#import "FdDisplay.h"

#import "DDLog.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@interface FdViewController ()

@property (nonatomic,retain) FdWebViewDelegate *appViewDelegate;
@property (nonatomic,retain) FdServiceMgr *serviceMgr;
@property (nonatomic,retain) FdDisplay *display;

@property (readwrite,assign) BOOL initialized;

- (void)handleSystemEvent:(NSDictionary*)event;

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation FdViewController {
    BOOL webViewLoaded;
    NSString *prefBaseURL;
    UIWebView *splashWebView;
}

@synthesize appView;

- (void)__init
{
    if((self != nil) && !self.initialized) {
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.System"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          [self handleSystemEvent:[note userInfo]];
                                                      }];
        
        // register to ws didClose events
        [[NSNotificationCenter defaultCenter] addObserverForName:@"freddotv.web.didLoad"
                                                          object:nil
                                                           queue:[NSOperationQueue mainQueue]
                                                      usingBlock:^(NSNotification *note) {
                                                          self.appView.hidden = NO;
                                                          [splashWebView removeFromSuperview];
                                                      }];
        self.initialized = YES;
        webViewLoaded = NO;
    }
}

- (id)initWithNibName:(NSString*)nibNameOrNil bundle:(NSBundle*)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    [self __init];
    return self;
}

- (id)init
{
    self = [super init];
    [self __init];
    return self;
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.appViewDelegate = [[FdWebViewDelegate alloc] init];
    self.appView.delegate = self.appViewDelegate;
    
    self.serviceMgr = [[FdServiceMgr alloc] init:@"dtalk.Services" withController:self];
    
    self.display = [[FdDisplay alloc] init:@"freddotv.ui.Display" withController:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidLayoutSubviews
{
    [super viewDidLayoutSubviews];
    
    if (!self.display.view.hidden) {
        DDLogVerbose(@"%@[%p]: %@", THIS_FILE, self, THIS_METHOD);
    }
}

-(void)loadServiceUrl:(NSString *)url
{
    NSURL *appURL = [NSURL URLWithString:url];
    NSURLRequest *appReq = [NSURLRequest requestWithURL:appURL cachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData timeoutInterval:20.0];
    [self.appView loadRequest:appReq];
}

// Encode URL parameter:
// http://stackoverflow.com/questions/12652396/ios-how-to-do-proper-url-encoding
- (NSString *)encodeURL:(NSString *)urlString
{
    CFStringRef newString = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault, (CFStringRef)urlString, NULL, CFSTR("!*'();:@&=+@,/?#[]"), kCFStringEncodingUTF8);
    return (NSString *)CFBridgingRelease(newString);
}

- (void)loadUrl:(NSNumber*)listetingPort
{
    NSUserDefaults *defautls = [NSUserDefaults standardUserDefaults];
    [defautls synchronize];
    
    NSString *prefBaseURLCheck = [defautls objectForKey:@"pref_url"];
    prefBaseURL = prefBaseURLCheck;
    
    if (prefBaseURLCheck == nil) {
        prefBaseURLCheck = [self getSettingFromBundle:@"pref_url"];
    }

    DDLogVerbose(@"%@:%@ - prefURL: %@", THIS_FILE, THIS_METHOD, prefBaseURLCheck);
    
    if (prefBaseURLCheck == nil || [prefBaseURLCheck isEqualToString:@""]) {
        // We don't need to use a url parameter since FdHTTPConnection will be used here.
        prefBaseURLCheck = [NSString stringWithFormat:@"http://localhost:%@/index.html", listetingPort];
    } else {
        //prefBaseURLCheck = [NSString stringWithFormat:@"%@?dtalksrv=%@", prefBaseURLCheck, listetingPort];
        NSString *ws = [NSString stringWithFormat:@"ws://localhost:%@/dtalksrv", listetingPort];
        prefBaseURLCheck = [NSString stringWithFormat:@"%@?ws=%@", prefBaseURLCheck, [self encodeURL:ws]];
    }
    
    NSURL *appURL = [NSURL URLWithString:prefBaseURLCheck];
    NSURLRequest *appReq = [NSURLRequest requestWithURL:appURL cachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData timeoutInterval:20.0];
    [self.appView loadRequest:appReq];
    
    NSString *basePath = [prefBaseURLCheck stringByDeletingLastPathComponent];
    basePath = [NSString stringWithFormat:@"%@/splash.png", basePath];
    
    //basePath = @"http://192.168.61.7/~alejandro/splash.png";
    
    NSURL *baseUrl = [NSURL URLWithString:basePath];
    NSData *splashIconData = [NSData dataWithContentsOfURL:baseUrl];
    
    if (splashIconData) {
        splashWebView = [[UIWebView alloc] initWithFrame:CGRectMake(0, 0, 1024, 768)];
        //        CGRect frame = splashWebView.frame;
        self.appView.hidden = YES;
        [self.view addSubview:splashWebView];
        [self.view bringSubviewToFront:splashWebView];
        //[splashWebView loadRequest:[NSURLRequest requestWithURL:baseUrl]];
        NSString *tinyHtmlBody = @"<!doctype html><html><head><meta http-equiv='content-type' content='text/html; charset=UTF-8'><meta name='viewport'	content='width=device-width,initial-scale=1,maximum-scale=1,minimum-scale=1,user-scalable=no' /><meta name='apple-mobile-web-app-capable' content='yes' /></head><body><center><img src='_SOURCE_IMAGE_'/></center></body></html>";
        tinyHtmlBody = [tinyHtmlBody stringByReplacingOccurrencesOfString:@"_SOURCE_IMAGE_" withString:basePath];
        [splashWebView loadHTMLString:tinyHtmlBody baseURL:nil];
    }
}

- (NSString*)getSettingFromBundle:(NSString*)settingsName
{
	NSString *pathStr = [[NSBundle mainBundle] bundlePath];
	NSString *settingsBundlePath = [pathStr stringByAppendingPathComponent:@"Settings.bundle"];
	NSString *finalPath = [settingsBundlePath stringByAppendingPathComponent:@"Root.plist"];
	
	NSDictionary *settingsDict = [NSDictionary dictionaryWithContentsOfFile:finalPath];
	NSArray *prefSpecifierArray = [settingsDict objectForKey:@"PreferenceSpecifiers"];
	NSDictionary *prefItem;
	for (prefItem in prefSpecifierArray)
	{
		if ([[prefItem objectForKey:@"Key"] isEqualToString:settingsName])
			return [prefItem objectForKey:@"DefaultValue"];
	}
	return nil;
}

#pragma mark -
#pragma mark - System Events
     
- (void)handleSystemEvent:(NSDictionary*)event
{
    NSLog(@"--------------------");
    
    NSString *action = (NSString*)[event objectForKey:@"action"];
    if (action != nil) {
        if([action isEqualToString:@"deviceReady"]) {
            self.listeningPort = (NSNumber*)[event objectForKey:@"params"];
            if (self.listeningPort != nil) {

                NSUserDefaults *defautls = [NSUserDefaults standardUserDefaults];
                [defautls synchronize];
                NSString *prefBaseURLCheck = [defautls objectForKey:@"pref_url"];
                //
                // if it's the first time we load the webpage or we changed the pref url in settings
                // load the url.
                if (!webViewLoaded || ![prefBaseURLCheck isEqualToString:prefBaseURL]) {
                    DDLogVerbose(@"%@:%@ - Loading web app for the first time", THIS_FILE, THIS_METHOD);
                    webViewLoaded = YES;
                    [self loadUrl:self.listeningPort];
                } else {
                    NSString *dtalkCheck = [self.appView stringByEvaluatingJavaScriptFromString:@"typeof DTalk._publish"];
                    if ([dtalkCheck isEqualToString:@"function"]) {
                        NSString *topic = @"freddo.websocket.reconnect";
                        NSString *json = [NSString stringWithFormat:@"{\"port\" : %@}", self.listeningPort];
                        NSString *event = [NSString stringWithFormat:@"try {DTalk._publish('%@', '%@');}catch(e){alert(e)}", topic, json];
                        [self.appView stringByEvaluatingJavaScriptFromString:event];
                        DDLogVerbose(@"%@:%@ - Sending reconnect event to webapp: %@", THIS_FILE, THIS_METHOD, event);
                    }
                }
            }
        }
    }
}

@end
