//
//  FtvAppDelegate.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

#include "FdServerBrowser.h"

@class FdViewController;

@class HTTPServer;

@interface FdAppDelegate : UIResponder <UIApplicationDelegate> {
    HTTPServer *httpServer;
    FdServerBrowser *serverBrowser;
    
    UIWindow *window;
    FdViewController *viewController;
}

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) FdViewController *viewController;

@end
