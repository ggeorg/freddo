//
//  FdAirPlay.m
//  Freddo-Player
//
//  Created by Alejandro Garin on 24/10/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdAirPlay.h"
#import <MediaPlayer/MediaPlayer.h>

@implementation FdAirPlay

- (id) init:(NSString *)name options:(NSDictionary*)options
{
    return [super init:name withView:[[MPVolumeView alloc] initWithFrame:CGRectZero] options:options];
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
    
    MPVolumeView *airplayPickerView = (MPVolumeView *)self.view;
    
    [airplayPickerView setShowsVolumeSlider:NO];
    [airplayPickerView sizeToFit];
}

@end
