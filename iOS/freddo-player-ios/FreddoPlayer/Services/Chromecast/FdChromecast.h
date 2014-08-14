//
//  FdChromecast.h
//  Freddo-Player
//
//  Created by George Georgopoulos on 3/31/14.
//  Copyright (c) 2014 ArkaSoft LLC. All rights reserved.
//

#import "FdService.h"

#import <GoogleCast/GoogleCast.h>

@interface FdChromecast : FdService<GCKDeviceScannerListener, GCKDeviceManagerDelegate, GCKMediaControlChannelDelegate>

@end
