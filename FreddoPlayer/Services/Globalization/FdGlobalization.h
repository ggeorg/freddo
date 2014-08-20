#import <Foundation/Foundation.h>
#import "FdService.h"

#define CDV_FORMAT_SHORT 0
#define CDV_FORMAT_MEDIUM 1
#define CDV_FORMAT_LONG 2
#define CDV_FORMAT_FULL 3
#define CDV_SELECTOR_MONTHS 0
#define CDV_SELECTOR_DAYS 1

enum FdGlobalizationError {
    CDV_UNKNOWN_ERROR = 0,
    CDV_FORMATTING_ERROR = 1,
    CDV_PARSING_ERROR = 2,
    CDV_PATTERN_ERROR = 3,
};
typedef NSUInteger CDVGlobalizationError;

@interface FdGlobalization : FdService {
    CFLocaleRef currentLocale;
}

@end
