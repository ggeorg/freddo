//
//  FtvWebViewDelegate.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdWebViewDelegate.h"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation FdWebViewDelegate

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark UIWebViewDelegate Implementation
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) webViewDidStartLoad:(UIWebView*)theWebView
{
    // Show the Top Activity THROBER in the Battery Bar
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
}

- (void) webViewDidFinishLoad:(UIWebView*)theWebView
{
    // Hide the Top Activity THROBER in the Battery Bar
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    if (!theWebView.loading) {
        [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.web.didLoad" object:self userInfo:nil];
    }
}

- (void) webView:(UIWebView*)webView didFailLoadWithError:(NSError*)error {
    NSLog(@"%@\tERROR Failed to load webpage with error: %@", self.class, [error localizedDescription]);
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.web.didLoad" object:self userInfo:nil];
}

- (BOOL) webView:(UIWebView*)theWebView shouldStartLoadWithRequest:(NSURLRequest*)request
  navigationType:(UIWebViewNavigationType)navigationType
{
    NSLog(@"%@\tDEBUG: %@", self.class, request);
    //NSURL* url = [request URL];

    return YES;
}

@end
