#import <Foundation/Foundation.h>
#import "FdReachability.h"
#import "FdService.h"

@interface FdConnection : FdService {
    NSString* type;
    NSString* _callbackId;

    FdReachability* internetReach;
}

@property (copy) NSString* connectionType;
@property (strong) FdReachability* internetReach;

@end
