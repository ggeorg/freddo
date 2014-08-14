//
//  FtvHTTPConnection.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdHTTPConnection.h"
#import "FdServerWS.h"

#import "HTTPServer.h"
#import "HTTPMessage.h"
#import "HTTPResponse.h"
#import "HTTPDynamicFileResponse.h"

#import "GCDAsyncSocket.h"

#import "DDLog.h"

#import "NSData+Base64.h"

// Log levels: off, error, warn, info, verbose
static const int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation FdHTTPConnection

static int wsCount = 0;

- (NSObject<HTTPResponse> *) httpResponseForMethod:(NSString *)method URI:(NSString *)path
{
    DDLogVerbose(@"%@[%p]: ================================= %@ %@", THIS_FILE, self, THIS_METHOD, path);
    
    if ([[path pathExtension] caseInsensitiveCompare:@"js"] == NSOrderedSame
        || [[path pathExtension] caseInsensitiveCompare:@"html"] == NSOrderedSame
        || [path hasSuffix:@"/"]) {
    
        // We need to replace "%%WEBSOCKET_URL%%" with whatever URL the server is running on.
        //
		// NOTE: We can accomplish this easily with the HTTPDynamicFileResponse class,
		// which takes a dictionary of replacement key-value pairs,
		// and performs replacements on the fly as it uploads the file.
        
        NSString *wsLocation;
		
		NSString *wsHost = [request headerField:@"Host"];
		if (wsHost == nil) {
			NSString *port = [NSString stringWithFormat:@"%hu", [asyncSocket localPort]];
			wsLocation = [NSString stringWithFormat:@"ws://localhost:%@/dtalksrv", port];
		} else {
			wsLocation = [NSString stringWithFormat:@"ws://%@/dtalksrv", wsHost];
		}
		
		NSDictionary *replacementDict = [NSDictionary dictionaryWithObject:wsLocation forKey:@"WEBSOCKET_URL"];
		
		return [[HTTPDynamicFileResponse alloc] initWithFilePath:[self filePathForURI:path]
                                                   forConnection:self
                                                       separator:@"%%"
                                           replacementDictionary:replacementDict];
    }
    
    return [super httpResponseForMethod:method URI:path];
}

- (WebSocket *)webSocketForURI:(NSString *)path
{
    DDLogVerbose(@"%@[%p]: %@%@", THIS_FILE, self, THIS_METHOD, path);
    
	if([path hasPrefix:@"/dtalksrv"]) {
        NSString *name = [NSString stringWithFormat:@"ws_%d", ++wsCount];
    
        DDLogInfo(@"%@: Creating FtvServerWS: '%@'", self.class, name);
        return [[FdServerWS alloc] initWithRequest:request socket:asyncSocket localServiceName:config.server.publishedName serviceName:name];
    }
    
    return [super webSocketForURI:path];
}

@end
