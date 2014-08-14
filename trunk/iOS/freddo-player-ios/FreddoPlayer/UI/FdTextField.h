//
//  FtvTextField.h
//  FreddoTV Player
//
//  Created by George Georgopoulos on 16/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdView.h"

@interface FdTextField : FdView<UITextFieldDelegate>

- (id) init:(NSString *)name options:(NSDictionary*)options;

@end
