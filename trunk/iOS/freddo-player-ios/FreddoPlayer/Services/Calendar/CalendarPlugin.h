//
//  calendarPlugin.h
//  Author: Felix Montanez
//  Date: 01-17-2011
//  Notes:
//

#import <Foundation/Foundation.h>
#import <EventKitUI/EventKitUI.h>
#import <EventKit/EventKit.h>
#import "FdService.h"

@interface CalendarPlugin : FdService

@property (nonatomic, retain) EKEventStore* eventStore;

- (void)initEventStoreWithCalendarCapabilities;

-(NSArray*)findEKEventsWithTitle: (NSString *)title
                        location: (NSString *)location
                         message: (NSString *)message
                       startDate: (NSDate *)startDate
                         endDate: (NSDate *)endDate;

// Calendar Instance methods

- (void)createEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options;
- (void)modifyEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options;
- (void)findEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options;
- (void)deleteEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options;

@end
