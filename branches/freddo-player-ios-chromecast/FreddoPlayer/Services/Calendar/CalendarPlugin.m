//
//  calendarPlugin.m
//  Author: Felix Montanez
//  Date: 01-17-2011
//  Notes:
//
// Contributors : Sean Bedford


#import "CalendarPlugin.h"
#import <EventKitUI/EventKitUI.h>
#import <EventKit/EventKit.h>
#import "NSMutableArray+QueueAdditions.h"

#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)

@implementation CalendarPlugin
@synthesize eventStore;

#pragma mark Initialisation functions

-(void)setup {
    [super setup];
    [self initEventStoreWithCalendarCapabilities];
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    NSString *_id = (NSString*)[event objectForKey:@"id"];
    
    NSMutableArray *arguments = [NSMutableArray arrayWithObject:_id];
    NSArray *params = [event objectForKey:@"params"];
    if ([params isKindOfClass:[NSNull class]]) {
        params = nil;
    } else {
        [arguments addObjectsFromArray:params];
    }
    
    if ([@"createEvent" isEqualToString:action]) {
        [self createEvent:arguments withDict:event];
        return YES;
    } else if ([@"modifyEvent" isEqualToString:action]) {
        [self modifyEvent:arguments withDict:event];
        return YES;
    } if ([@"findEvent" isEqualToString:action]) {
        [self findEvent:arguments withDict:event];
        return YES;
    } if ([@"deleteEvent" isEqualToString:action]) {
        [self deleteEvent:arguments withDict:event];
        return YES;
    }
    return [super onEvent:event];
}

- (void)initEventStoreWithCalendarCapabilities {
    
    // Check for EventStore that is useful
    if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"6.0")) {
        // Need to request calendar permissions
        EKEventStore *localEventStore = [[EKEventStore alloc] init];
        [localEventStore requestAccessToEntityType:EKEntityTypeEvent completion:^(BOOL granted, NSError *error){
            if (granted) {
                self.eventStore = localEventStore;
            }
            else {
                UIAlertView *av = [[UIAlertView alloc] initWithTitle:@"No Event Support" message:@"There will be no support to view your calendar" delegate:nil cancelButtonTitle:@"OK" otherButtonTitles: nil];
                [av show];
            }
        }];
        
    }
    else {
        self.eventStore = [[EKEventStore alloc] init];
    }
}

#pragma mark Helper Functions

-(NSArray*)findEKEventsWithTitle: (NSString *)title
                        location: (NSString *)location
                         message: (NSString *)message
                       startDate: (NSDate *)startDate
                         endDate: (NSDate *)endDate {
    
    // Build up a predicateString - this means we only query a parameter if we actually had a value in it
    NSMutableString *predicateString= [[NSMutableString alloc] initWithString:@""];
    if (title.length > 0) {
        [predicateString appendString:[NSString stringWithFormat:@"title == '%@'" , title]];
    }
    if (location.length > 0) {
        [predicateString appendString:[NSString stringWithFormat:@" AND location == '%@'" , location]];
    }
    if (message.length > 0) {
        [predicateString appendString:[NSString stringWithFormat:@" AND notes == '%@'" , message]];
    }
    
    NSPredicate *matches = [NSPredicate predicateWithFormat:predicateString];
    
    NSArray *datedEvents = [self.eventStore eventsMatchingPredicate:[eventStore predicateForEventsWithStartDate:startDate endDate:endDate calendars:nil]];
    
    
    NSArray *matchingEvents = [datedEvents filteredArrayUsingPredicate:matches];
    
    
    return matchingEvents;
}

#pragma mark Cordova functions

- (void)createEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options {
    // Import arguments
    NSString *callbackId = [arguments pop];
    
    NSString* title      = [arguments objectAtIndex:0];
    NSString* location   = [arguments objectAtIndex:1];
    NSString* message    = [arguments objectAtIndex:2];
    NSString *startDate  = [arguments objectAtIndex:3];
    NSString *endDate    = [arguments objectAtIndex:4];
    
    //creating the dateformatter object
    NSDateFormatter *df = [[NSDateFormatter alloc] init];
    [df setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSDate *myStartDate = [df dateFromString:startDate];
    NSDate *myEndDate = [df dateFromString:endDate];

    EKEvent *myEvent = [EKEvent eventWithEventStore: self.eventStore];
    myEvent.title = title;
    myEvent.location = location;
    myEvent.notes = message;
    myEvent.startDate = myStartDate;
    myEvent.endDate = myEndDate;
    myEvent.calendar = self.eventStore.defaultCalendarForNewEvents;
    
    
    EKAlarm *reminder = [EKAlarm alarmWithRelativeOffset:-2*60*60];
    
    [myEvent addAlarm:reminder];
    
    NSError *error = nil;
    [self.eventStore saveEvent:myEvent span:EKSpanThisEvent error:&error];
    
    BOOL saved = [self.eventStore saveEvent:myEvent span:EKSpanThisEvent
                                      error:&error];
    if (saved) {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                        message:@"Saved to Calendar" delegate:self
                                              cancelButtonTitle:@"Thank you!"
                                              otherButtonTitles:nil];
        [alert show];
    }
    // Check error code + return result
    NSString *_from = (NSString*)[options objectForKey:@"from"];
    if (error) {
        [super sendErrorResponse:error.userInfo.description request:options];
    }
    else {
        [self sendObjectResponse:[NSDictionary dictionary] request:options];
    }
}

-(void)deleteEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options {
    // Import arguments
    
    NSString *callbackId = [arguments pop];
    
    NSString* title      = [arguments objectAtIndex:0];
    NSString* location   = [arguments objectAtIndex:1];
    NSString* message    = [arguments objectAtIndex:2];
    NSString *startDate  = [arguments objectAtIndex:3];
    NSString *endDate    = [arguments objectAtIndex:4];
    bool delAll = [arguments objectAtIndex:5];
    
    NSDateFormatter *df = [[NSDateFormatter alloc] init];
    [df setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSDate *myStartDate = [df dateFromString:startDate];
    NSDate *myEndDate = [df dateFromString:endDate];
    
    NSArray *matchingEvents = [self findEKEventsWithTitle:title location:location message:message startDate:myStartDate endDate:myEndDate];
    

    if (delAll || matchingEvents.count == 1) {
        // Definitive single match - delete it!      
        NSError *error = NULL;
        bool hadErrors = false;
        if (delAll) {
            for (EKEvent * event in matchingEvents) {
                [self.eventStore removeEvent:event span:EKSpanThisEvent error:&error];
                // Check for error codes and return result
                if (error) {
                    hadErrors = true;
                }
            }
        }
        else {
            [self.eventStore removeEvent:[matchingEvents lastObject] span:EKSpanThisEvent error:&error];
        }
        // Check for error codes and return result
        if (error || hadErrors) {
            NSString *messageString;
            if (hadErrors) {
                messageString = @"Error deleting events";
            }
            else {
                messageString = error.userInfo.description;
            }
            [super sendErrorResponse:messageString request:options];
        }
        else {
            [self sendObjectResponse:[NSDictionary dictionary] request:options];
        }
    }
}

-(void)findEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options {
    // Import arguments
    
    NSString* title      = [arguments objectAtIndex:0];
    NSString* location   = [arguments objectAtIndex:1];
    NSString* message    = [arguments objectAtIndex:2];
    NSString *startDate  = [arguments objectAtIndex:3];
    NSString *endDate    = [arguments objectAtIndex:4];
    
    NSDateFormatter *df = [[NSDateFormatter alloc] init];
    [df setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSDate *myStartDate = [df dateFromString:startDate];
    NSDate *myEndDate = [df dateFromString:endDate];
    
    NSArray *matchingEvents = [self findEKEventsWithTitle:title location:location message:message startDate:myStartDate endDate:myEndDate];
    
    NSMutableArray *finalResults = [[NSMutableArray alloc] initWithCapacity:matchingEvents.count];
    
    
    // Stringify the results - Cordova can't deal with Obj-C objects
    for (EKEvent * event in matchingEvents) {
        NSMutableDictionary *entry = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
        event.title, @"title",
        event.location, @"location",
        event.notes, @"message",
        [df stringFromDate:event.startDate], @"startDate",
        [df stringFromDate:event.endDate], @"endDate", nil];
        [finalResults addObject:entry];
    }
    
    if (finalResults.count > 0) {
        // Return the results we got
        [self sendArrayResponse:finalResults request:options];
    }
    else {
        [self sendArrayResponse:[NSArray array] request:options];
    }
    
}

 
-(void)modifyEvent:(NSMutableArray*)arguments withDict:(NSDictionary*)options {
    // Import arguments
    
    NSString* title      = [arguments objectAtIndex:0];
    NSString* location   = [arguments objectAtIndex:1];
    NSString* message    = [arguments objectAtIndex:2];
    NSString *startDate  = [arguments objectAtIndex:3];
    NSString *endDate    = [arguments objectAtIndex:4];
    
    NSString* ntitle      = [arguments objectAtIndex:5];
    NSString* nlocation   = [arguments objectAtIndex:6];
    NSString* nmessage    = [arguments objectAtIndex:7];
    NSString *nstartDate  = [arguments objectAtIndex:8];
    NSString *nendDate    = [arguments objectAtIndex:9];
    
    // Make NSDates from our strings
    NSDateFormatter *df = [[NSDateFormatter alloc] init];
    [df setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSDate *myStartDate = [df dateFromString:startDate];
    NSDate *myEndDate = [df dateFromString:endDate];
    
    // Find matches
    NSArray *matchingEvents = [self findEKEventsWithTitle:title location:location message:message startDate:myStartDate endDate:myEndDate];
    
    if (matchingEvents.count == 1) {
        // Presume we have to have an exact match to modify it!
        // Need to load this event from an EKEventStore so we can edit it
        EKEvent *theEvent = [self.eventStore eventWithIdentifier:((EKEvent*)[matchingEvents lastObject]).eventIdentifier];
        if (ntitle) {
            theEvent.title = ntitle;
        }
        if (nlocation) {
            theEvent.location = nlocation;
        }
        if (nmessage) {
            theEvent.notes = nmessage;
        }
        if (nstartDate) {
            NSDate *newMyStartDate = [df dateFromString:nstartDate];
            theEvent.startDate = newMyStartDate;
        }
        if (nendDate) {
            NSDate *newMyEndDate = [df dateFromString:nendDate];
            theEvent.endDate = newMyEndDate;
        }
        
        // Now save the new details back to the store
        NSError *error = nil;
        [self.eventStore saveEvent:theEvent span:EKSpanThisEvent error:&error];
        
        // Check error code + return result
        if (error) {
            [super sendErrorResponse:error.userInfo.description request:options];
        }
        else {
            [self sendObjectResponse:[NSDictionary dictionary] request:options];
        }
    }
    else {
        // Otherwise return a no result error
        [self sendObjectResponse:[NSDictionary dictionary] request:options];
    }
}

@end
