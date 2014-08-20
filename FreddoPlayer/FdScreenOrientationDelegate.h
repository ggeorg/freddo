//
//  FtvScreenOrientationDelegate.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 23/6/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#ifndef FreddoTV_Player_FtvScreenOrientationDelegate_h
#define FreddoTV_Player_FtvScreenOrientationDelegate_h

#import <Foundation/Foundation.h>

@protocol FdScreenOrientationDelegate <NSObject>

- (NSUInteger)supportedInterfaceOrientations;
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation;
- (BOOL)shouldAutorotate;

@end

#endif
