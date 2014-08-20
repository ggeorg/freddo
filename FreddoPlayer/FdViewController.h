//
//  FtvViewController.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 19/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

#include "FdScreenOrientationDelegate.h"

@interface FdViewController : UIViewController<FdScreenOrientationDelegate> {
    
}

@property (weak, nonatomic) IBOutlet UIWebView *appView;
@property (strong, nonatomic) NSNumber *listeningPort;

-(void)loadServiceUrl:(NSString *)url;

@end

