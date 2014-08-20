//
//  FdAppView.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 18/11/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdAppView.h"

@implementation FdAppView

- (NSString *)encodeURL:(NSString *)urlString
{
    CFStringRef newString = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault, (CFStringRef)urlString, NULL, CFSTR("!*'();:@&=+@,/?#[]"), kCFStringEncodingUTF8);
    return (NSString *)CFBridgingRelease(newString);
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if([@"url" isEqualToString:property]) {
            FdViewController *controller = (FdViewController *)self.controller;
            NSString *url = [self getStringProperty:options property:property];
            NSString *ws = [NSString stringWithFormat:@"ws://localhost:%@/dtalksrv", controller.listeningPort];
            url = [NSString stringWithFormat:@"%@?ws=%@", url, [self encodeURL:ws]];
            [controller loadServiceUrl:url];
        }
    }
    [super set:options];
}

@end
