#import <Foundation/Foundation.h>
#import <AddressBook/ABAddressBook.h>
#import <AddressBookUI/AddressBookUI.h>
#import "FdContact.h"
#import "FdService.h"

@interface FdContacts : FdService
{
    ABAddressBookRef addressBook;
}
@end

@interface CDVAddressBookAccessError : NSObject
{}
@property (assign) CDVContactError errorCode;
- (CDVAddressBookAccessError*)initWithCode:(CDVContactError)code;
@end

typedef void (^ CDVAddressBookWorkerBlock)(
    ABAddressBookRef         addressBook,
    CDVAddressBookAccessError* error
    );
@interface CDVAddressBookHelper : NSObject
{}

- (void)createAddressBook:(CDVAddressBookWorkerBlock)workerBlock;
@end
