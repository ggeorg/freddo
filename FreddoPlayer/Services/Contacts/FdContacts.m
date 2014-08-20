/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "FdContacts.h"
#import <UIKit/UIKit.h>
#import "NSArray+Comparisons.h"
#import "NSDictionary+Extensions.h"

@implementation FdContacts

// no longer used since code gets AddressBook for each operation.
// If address book changes during save or remove operation, may get error but not much we can do about it
// If address book changes during UI creation, display or edit, we don't control any saves so no need for callback

/*void addressBookChanged(ABAddressBookRef addressBook, CFDictionaryRef info, void* context)
{
    // note that this function is only called when another AddressBook instance modifies
    // the address book, not the current one. For example, through an OTA MobileMe sync
    Contacts* contacts = (Contacts*)context;
    [contacts addressBookDirty];
    }*/

// overridden to clean up Contact statics
- (void)onAppTerminate
{
    // NSLog(@"Contacts::onAppTerminate");
}

- (void)runInBackground:(void (^)())block
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), block);
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    if ([@"search" isEqualToString:action]) {
        
        NSString *commandId = [event objectForKey:@"id"];
        
        NSDictionary *params = [event objectForKey:@"params"];
        NSArray *fields = [[event objectForKey:@"params"] objectForKey:@"fields"];
        
        if ([params isKindOfClass:[NSNull class]]) {
            params = nil;
        }
        if ([fields isKindOfClass:[NSNull class]]) {
            fields = nil;
        }
        
        if (params == nil || fields == nil) {
            [self sendErrorResponse:@"Invalid parameters" request:event];
            return YES;
        }
        
        NSDictionary *options = [params objectForKey:@"options"];
        [self search:fields andOptions:options withRequest:event];

        return YES;
    }     
    return [super onEvent:event];
}

- (void)search:(NSArray*)fields andOptions:(NSDictionary *)findOptions withRequest:(NSDictionary *)request
{
    [self runInBackground:^{
        // from Apple:  Important You must ensure that an instance of ABAddressBookRef is used by only one thread.
        // which is why address book is created within the dispatch queue.
        // more details here: http: //blog.byadrian.net/2012/05/05/ios-addressbook-framework-and-gcd/
        CDVAddressBookHelper* abHelper = [[CDVAddressBookHelper alloc] init];
        FdContacts* __weak weakSelf = self;     // play it safe to avoid retain cycles
        // it gets uglier, block within block.....
        [abHelper createAddressBook: ^(ABAddressBookRef addrBook, CDVAddressBookAccessError* errCode) {
            if (addrBook == NULL) {
                // permission was denied or other error - return error
                NSDictionary* errDict = @{@"error":[NSNumber numberWithInt:errCode.errorCode]};
                [weakSelf sendObjectResponse:errDict request:request];
                return;
            }

            NSArray* foundRecords = nil;
            // get the findOptions values
            BOOL multiple = NO;         // default is false
            NSString* filter = nil;
            if (![findOptions isKindOfClass:[NSNull class]]) {
                id value = nil;
                filter = (NSString*)[findOptions objectForKey:@"filter"];
                value = [findOptions objectForKey:@"multiple"];
                if ([value isKindOfClass:[NSNumber class]]) {
                    // multiple is a boolean that will come through as an NSNumber
                    multiple = [(NSNumber*)value boolValue];
                    // NSLog(@"multiple is: %d", multiple);
                }
            }

            NSDictionary* returnFields = [[FdContact class] calcReturnFields:fields];

            NSMutableArray* matches = nil;
            if (!filter || [filter isEqualToString:@""]) {
                // get all records
                foundRecords = (__bridge_transfer NSArray*)ABAddressBookCopyArrayOfAllPeople(addrBook);
                if (foundRecords && ([foundRecords count] > 0)) {
                    // create Contacts and put into matches array
                    // doesn't make sense to ask for all records when multiple == NO but better check
                    int xferCount = multiple == YES ? [foundRecords count] : 1;
                    matches = [NSMutableArray arrayWithCapacity:xferCount];

                    for (int k = 0; k < xferCount; k++) {
                        FdContact* xferContact = [[FdContact alloc] initFromABRecord:(__bridge ABRecordRef)[foundRecords objectAtIndex:k]];
                        [matches addObject:xferContact];
                        xferContact = nil;
                    }
                }
            } else {
                foundRecords = (__bridge_transfer NSArray*)ABAddressBookCopyArrayOfAllPeople(addrBook);
                matches = [NSMutableArray arrayWithCapacity:1];
                BOOL bFound = NO;
                int testCount = [foundRecords count];

                for (int j = 0; j < testCount; j++) {
                    FdContact* testContact = [[FdContact alloc] initFromABRecord:(__bridge ABRecordRef)[foundRecords objectAtIndex:j]];
                    if (testContact) {
                        bFound = [testContact foundValue:filter inFields:returnFields];
                        if (bFound) {
                            [matches addObject:testContact];
                        }
                        testContact = nil;
                    }
                }
            }
            NSMutableArray* returnContacts = [NSMutableArray arrayWithCapacity:1];

            if ((matches != nil) && ([matches count] > 0)) {
                // convert to JS Contacts format and return in callback
                // - returnFields  determines what properties to return
                @autoreleasepool {
                    int count = multiple == YES ? [matches count] : 1;

                    for (int i = 0; i < count; i++) {
                        FdContact* newContact = [matches objectAtIndex:i];
                        NSDictionary* aContact = [newContact toDictionary:returnFields];
                        [returnContacts addObject:aContact];
                    }
                }
            }
            // return found contacts (array is empty if no contacts found)
//            [weakSelf sendMessageWithService:callbackId andResult:returnContacts];
            [weakSelf sendArrayResponse:returnContacts request:request];

            if (addrBook) {
                CFRelease(addrBook);
            }
        }];
    }];     // end of workQueue block

    return;
}

@end

@implementation CDVAddressBookAccessError

@synthesize errorCode;

- (CDVAddressBookAccessError*)initWithCode:(CDVContactError)code
{
    self = [super init];
    if (self) {
        self.errorCode = code;
    }
    return self;
}

@end

@implementation CDVAddressBookHelper

/**
 * NOTE: workerBlock is responsible for releasing the addressBook that is passed to it
 */
- (void)createAddressBook:(CDVAddressBookWorkerBlock)workerBlock
{
    // TODO: this probably should be reworked - seems like the workerBlock can just create and release its own AddressBook,
    // and also this important warning from (http://developer.apple.com/library/ios/#documentation/ContactData/Conceptual/AddressBookProgrammingGuideforiPhone/Chapters/BasicObjects.html):
    // "Important: Instances of ABAddressBookRef cannot be used by multiple threads. Each thread must make its own instance."
    ABAddressBookRef addressBook;

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 60000
        if (&ABAddressBookCreateWithOptions != NULL) {
            CFErrorRef error = nil;
            // CFIndex status = ABAddressBookGetAuthorizationStatus();
            addressBook = ABAddressBookCreateWithOptions(NULL, &error);
            // NSLog(@"addressBook access: %lu", status);
            ABAddressBookRequestAccessWithCompletion(addressBook, ^(bool granted, CFErrorRef error) {
                    // callback can occur in background, address book must be accessed on thread it was created on
                    dispatch_sync(dispatch_get_main_queue(), ^{
                        if (error) {
                            workerBlock(NULL, [[CDVAddressBookAccessError alloc] initWithCode:UNKNOWN_ERROR]);
                        } else if (!granted) {
                            workerBlock(NULL, [[CDVAddressBookAccessError alloc] initWithCode:PERMISSION_DENIED_ERROR]);
                        } else {
                            // access granted
                            workerBlock(addressBook, [[CDVAddressBookAccessError alloc] initWithCode:UNKNOWN_ERROR]);
                        }
                    });
                });
        } else
#endif
    {
        // iOS 4 or 5 no checks needed
        addressBook = ABAddressBookCreate();
        workerBlock(addressBook, NULL);
    }
}

@end
