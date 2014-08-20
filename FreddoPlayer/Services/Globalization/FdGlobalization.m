#import "FdGlobalization.h"

@implementation FdGlobalization

-(void)setup {
    [super setup];
    currentLocale = CFLocaleCopyCurrent();
}

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    NSArray* arguments = nil;
    NSDictionary *params = [event objectForKey:@"params"];
    if ([params isKindOfClass:[NSNull class]]) {
        params = nil;
    }
    if (params != nil) {
        arguments = [NSArray arrayWithObjects:params, nil];
    }
    
    if ([@"getPreferredLanguage" isEqualToString:action]) {
        [self getPreferredLanguage:event];
        return YES;
    } else if ([@"getLocaleName" isEqualToString:action]) {
        [self getLocaleName:event];
        return YES;
    } else if ([@"dateToString" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self dateToString:event withOptions:options];
        return YES;
    } else if ([@"stringToDate" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self stringToDate:event withOptions:options];
        return YES;
    } else if ([@"getDatePattern" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self getDatePattern:event withOptions:options];
        return YES;
    } else if ([@"getDateNames" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self getDateNames:event withOptions:options];
        return YES;
    } else if ([@"isDayLightSavingsTime" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self isDayLightSavingsTime:event withOptions:options];
        return YES;
    } else if ([@"getFirstDayOfWeek" isEqualToString:action]) {
        [self getFirstDayOfWeek:event];
        return YES;
    } else if ([@"numberToString" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self numberToString:event withOptions:options];
        return YES;
    } else if ([@"stringToNumber" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self stringToNumber:event withOptions:options];
        return YES;
    } else if ([@"getNumberPattern" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self getNumberPattern:event withOptions:options];
        return YES;
    } else if ([@"getCurrencyPattern" isEqualToString:action]) {
        NSDictionary *options = [arguments objectAtIndex:0];
        [self getCurrencyPattern:event withOptions:options];
        return YES;
    }
    return [super onEvent:event];
}

- (void)getPreferredLanguage:(NSDictionary*)request
{
    // Source: http://stackoverflow.com/questions/3910244/getting-current-device-language-in-ios
    // (should be OK)
    NSString* language = [[NSLocale preferredLanguages] objectAtIndex:0];

    if (language) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:language forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];

    } else {
        // TBD is this ever expected to happen?
        [super sendErrorResponse:@"Unknown error" request:request];
    }
}

- (void)getLocaleName:(NSDictionary*)request
{
    NSDictionary* dictionary = nil;
    NSLocale* locale = [NSLocale currentLocale];

    if (locale) {
        dictionary = [NSDictionary dictionaryWithObject:[locale localeIdentifier] forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
    } else {
        [super sendErrorResponse:@"Unknown error" request:request];
    }
}

- (void)dateToString:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFDateFormatterStyle style = kCFDateFormatterShortStyle;
    CFDateFormatterStyle dateStyle = kCFDateFormatterShortStyle;
    CFDateFormatterStyle timeStyle = kCFDateFormatterShortStyle;
    NSDate* date = nil;
    NSString* dateString = nil;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    id milliseconds = [options valueForKey:@"date"];

    if (milliseconds && [milliseconds isKindOfClass:[NSNumber class]]) {
        // get the number of seconds since 1970 and create the date object
        date = [NSDate dateWithTimeIntervalSince1970:[milliseconds doubleValue] / 1000];
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];
    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired format length
                if ([key isEqualToString:@"formatLength"]) {
                    if ([item isEqualToString:@"short"]) {
                        style = kCFDateFormatterShortStyle;
                    } else if ([item isEqualToString:@"medium"]) {
                        style = kCFDateFormatterMediumStyle;
                    } else if ([item isEqualToString:@"long"]) {
                        style = kCFDateFormatterLongStyle;
                    } else if ([item isEqualToString:@"full"]) {
                        style = kCFDateFormatterFullStyle;
                    }
                }
                // get the type of date and time to generate
                else if ([key isEqualToString:@"selector"]) {
                    if ([item isEqualToString:@"date"]) {
                        dateStyle = style;
                        timeStyle = kCFDateFormatterNoStyle;
                    } else if ([item isEqualToString:@"time"]) {
                        dateStyle = kCFDateFormatterNoStyle;
                        timeStyle = style;
                    } else if ([item isEqualToString:@"date and time"]) {
                        dateStyle = style;
                        timeStyle = style;
                    }
                }
            }
        }
    }

    // create the formatter using the user's current default locale and formats for dates and times
    CFDateFormatterRef formatter = CFDateFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            dateStyle,
            timeStyle);
    // if we have a valid date object then call the formatter
    if (date) {
        dateString = (__bridge_transfer NSString*)CFDateFormatterCreateStringWithDate(kCFAllocatorDefault,
                formatter,
                (__bridge CFDateRef)date);
    }

    // if the date was converted to a string successfully then return the result
    if (dateString) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:dateString forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        // DLog(@"GlobalizationCommand dateToString unable to format %@", [date description]);
        [super sendErrorResponse:@"Formatting error" request:request];
    }
    CFRelease(formatter);
}

- (void)stringToDate:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFDateFormatterStyle style = kCFDateFormatterShortStyle;
    CFDateFormatterStyle dateStyle = kCFDateFormatterShortStyle;
    CFDateFormatterStyle timeStyle = kCFDateFormatterShortStyle;
    NSString* dateString = nil;
    NSDateComponents* comps = nil;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    // get the string that is to be parsed for a date
    id ms = [options valueForKey:@"dateString"];

    if (ms && [ms isKindOfClass:[NSString class]]) {
        dateString = ms;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];
    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired format length
                if ([key isEqualToString:@"formatLength"]) {
                    if ([item isEqualToString:@"short"]) {
                        style = kCFDateFormatterShortStyle;
                    } else if ([item isEqualToString:@"medium"]) {
                        style = kCFDateFormatterMediumStyle;
                    } else if ([item isEqualToString:@"long"]) {
                        style = kCFDateFormatterLongStyle;
                    } else if ([item isEqualToString:@"full"]) {
                        style = kCFDateFormatterFullStyle;
                    }
                }
                // get the type of date and time to generate
                else if ([key isEqualToString:@"selector"]) {
                    if ([item isEqualToString:@"date"]) {
                        dateStyle = style;
                        timeStyle = kCFDateFormatterNoStyle;
                    } else if ([item isEqualToString:@"time"]) {
                        dateStyle = kCFDateFormatterNoStyle;
                        timeStyle = style;
                    } else if ([item isEqualToString:@"date and time"]) {
                        dateStyle = style;
                        timeStyle = style;
                    }
                }
            }
        }
    }

    // get the user's default settings for date and time formats
    CFDateFormatterRef formatter = CFDateFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            dateStyle,
            timeStyle);

    // set the parsing to be more lenient
    CFDateFormatterSetProperty(formatter, kCFDateFormatterIsLenient, kCFBooleanTrue);

    // parse tha date and time string
    CFDateRef date = CFDateFormatterCreateDateFromString(kCFAllocatorDefault,
            formatter,
            (__bridge CFStringRef)dateString,
            NULL);

    // if we were able to parse the date then get the date and time components
    if (date != NULL) {
        NSCalendar* calendar = [NSCalendar currentCalendar];

        unsigned unitFlags = NSYearCalendarUnit |
            NSMonthCalendarUnit |
            NSDayCalendarUnit |
            NSHourCalendarUnit |
            NSMinuteCalendarUnit |
            NSSecondCalendarUnit;

        comps = [calendar components:unitFlags fromDate:(__bridge NSDate*)date];
        CFRelease(date);
    }

    // put the various elements of the date and time into a dictionary
    if (comps != nil) {
        NSArray* keys = [NSArray arrayWithObjects:@"year", @"month", @"day", @"hour", @"minute", @"second", @"millisecond", nil];
        NSArray* values = [NSArray arrayWithObjects:[NSNumber numberWithInt:[comps year]],
            [NSNumber numberWithInt:[comps month] - 1],
            [NSNumber numberWithInt:[comps day]],
            [NSNumber numberWithInt:[comps hour]],
            [NSNumber numberWithInt:[comps minute]],
            [NSNumber numberWithInt:[comps second]],
            [NSNumber numberWithInt:0],                /* iOS does not provide milliseconds */
            nil];

        NSDictionary* dictionary = [NSDictionary dictionaryWithObjects:values forKeys:keys];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        // Dlog(@"GlobalizationCommand stringToDate unable to parse %@", dateString);
        [super sendErrorResponse:@"unable to parse" request:request];
    }
    CFRelease(formatter);
}

- (void)getDatePattern:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFDateFormatterStyle style = kCFDateFormatterShortStyle;
    CFDateFormatterStyle dateStyle = kCFDateFormatterShortStyle;
    CFDateFormatterStyle timeStyle = kCFDateFormatterShortStyle;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:options];
        return;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];

    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired format length
                if ([key isEqualToString:@"formatLength"]) {
                    if ([item isEqualToString:@"short"]) {
                        style = kCFDateFormatterShortStyle;
                    } else if ([item isEqualToString:@"medium"]) {
                        style = kCFDateFormatterMediumStyle;
                    } else if ([item isEqualToString:@"long"]) {
                        style = kCFDateFormatterLongStyle;
                    } else if ([item isEqualToString:@"full"]) {
                        style = kCFDateFormatterFullStyle;
                    }
                }
                // get the type of date and time to generate
                else if ([key isEqualToString:@"selector"]) {
                    if ([item isEqualToString:@"date"]) {
                        dateStyle = style;
                        timeStyle = kCFDateFormatterNoStyle;
                    } else if ([item isEqualToString:@"time"]) {
                        dateStyle = kCFDateFormatterNoStyle;
                        timeStyle = style;
                    } else if ([item isEqualToString:@"date and time"]) {
                        dateStyle = style;
                        timeStyle = style;
                    }
                }
            }
        }
    }

    // get the user's default settings for date and time formats
    CFDateFormatterRef formatter = CFDateFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            dateStyle,
            timeStyle);

    // get the date pattern to apply when formatting and parsing
    CFStringRef datePattern = CFDateFormatterGetFormat(formatter);
    // get the user's current time zone information
    CFTimeZoneRef timezone = (CFTimeZoneRef)CFDateFormatterCopyProperty(formatter, kCFDateFormatterTimeZone);

    // put the pattern and time zone information into the dictionary
    if ((datePattern != nil) && (timezone != nil)) {
        NSArray* keys = [NSArray arrayWithObjects:@"pattern", @"timezone", @"utc_offset", @"dst_offset", nil];
        NSArray* values = [NSArray arrayWithObjects:((__bridge NSString*)datePattern),
            [((__bridge NSTimeZone*)timezone)abbreviation],
            [NSNumber numberWithLong:[((__bridge NSTimeZone*)timezone)secondsFromGMT]],
            [NSNumber numberWithDouble:[((__bridge NSTimeZone*)timezone)daylightSavingTimeOffset]],
            nil];

        NSDictionary* dictionary = [NSDictionary dictionaryWithObjects:values forKeys:keys];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        NSMutableDictionary* dictionary = [NSMutableDictionary dictionaryWithCapacity:2];
        [dictionary setValue:[NSNumber numberWithInt:CDV_PATTERN_ERROR] forKey:@"code"];
        [dictionary setValue:@"Pattern error" forKey:@"message"];
        [super sendErrorResponse:@"Pattern error" request:request];
    }

    if (timezone) {
        CFRelease(timezone);
    }
    CFRelease(formatter);
}

- (void)getDateNames:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    int style = CDV_FORMAT_LONG;
    int selector = CDV_SELECTOR_MONTHS;
    CFStringRef dataStyle = kCFDateFormatterMonthSymbols;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];

    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired type of name
                if ([key isEqualToString:@"type"]) {
                    if ([item isEqualToString:@"narrow"]) {
                        style = CDV_FORMAT_SHORT;
                    } else if ([item isEqualToString:@"wide"]) {
                        style = CDV_FORMAT_LONG;
                    }
                }
                // determine if months or days are needed
                else if ([key isEqualToString:@"item"]) {
                    if ([item isEqualToString:@"months"]) {
                        selector = CDV_SELECTOR_MONTHS;
                    } else if ([item isEqualToString:@"days"]) {
                        selector = CDV_SELECTOR_DAYS;
                    }
                }
            }
        }
    }

    CFDateFormatterRef formatter = CFDateFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            kCFDateFormatterFullStyle,
            kCFDateFormatterFullStyle);

    if ((selector == CDV_SELECTOR_MONTHS) && (style == CDV_FORMAT_LONG)) {
        dataStyle = kCFDateFormatterMonthSymbols;
    } else if ((selector == CDV_SELECTOR_MONTHS) && (style == CDV_FORMAT_SHORT)) {
        dataStyle = kCFDateFormatterShortMonthSymbols;
    } else if ((selector == CDV_SELECTOR_DAYS) && (style == CDV_FORMAT_LONG)) {
        dataStyle = kCFDateFormatterWeekdaySymbols;
    } else if ((selector == CDV_SELECTOR_DAYS) && (style == CDV_FORMAT_SHORT)) {
        dataStyle = kCFDateFormatterShortWeekdaySymbols;
    }

    CFArrayRef names = (CFArrayRef)CFDateFormatterCopyProperty(formatter, dataStyle);

    if (names) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:((__bridge NSArray*)names) forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
        CFRelease(names);
    }
    // error
    else {
        NSMutableDictionary* dictionary = [NSMutableDictionary dictionaryWithCapacity:2];
        [dictionary setValue:[NSNumber numberWithInt:CDV_UNKNOWN_ERROR] forKey:@"code"];
        [dictionary setValue:@"Unknown error" forKey:@"message"];
        [super sendErrorResponse:@"Unknown error" request:request];
    }
    CFRelease(formatter);
}

- (void)isDayLightSavingsTime:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    NSDate* date = nil;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    id milliseconds = [options valueForKey:@"date"];

    if (milliseconds && [milliseconds isKindOfClass:[NSNumber class]]) {
        // get the number of seconds since 1970 and create the date object
        date = [NSDate dateWithTimeIntervalSince1970:[milliseconds doubleValue] / 1000];
    }

    if (date) {
        // get the current calendar for the user and check if the date is using DST
        NSCalendar* calendar = [NSCalendar currentCalendar];
        NSTimeZone* timezone = [calendar timeZone];
        NSNumber* dst = [NSNumber numberWithBool:[timezone isDaylightSavingTimeForDate:date]];

        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:dst forKey:@"dst"];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        [super sendErrorResponse:@"Unknown error" request:request];
    }
}

- (void)getFirstDayOfWeek:(NSDictionary*)request
{
    NSCalendar* calendar = [NSCalendar autoupdatingCurrentCalendar];

    NSNumber* day = [NSNumber numberWithInt:[calendar firstWeekday]];

    if (day) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:day forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        [super sendErrorResponse:@"Unknown error" request:request];
    }
}

- (void)numberToString:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFNumberFormatterStyle style = kCFNumberFormatterDecimalStyle;
    NSNumber* number = nil;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    id value = [options valueForKey:@"number"];

    if (value && [value isKindOfClass:[NSNumber class]]) {
        number = (NSNumber*)value;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];
    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired style of formatting
                if ([key isEqualToString:@"type"]) {
                    if ([item isEqualToString:@"percent"]) {
                        style = kCFNumberFormatterPercentStyle;
                    } else if ([item isEqualToString:@"currency"]) {
                        style = kCFNumberFormatterCurrencyStyle;
                    } else if ([item isEqualToString:@"decimal"]) {
                        style = kCFNumberFormatterDecimalStyle;
                    }
                }
            }
        }
    }

    CFNumberFormatterRef formatter = CFNumberFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            style);

    // get the localized string based upon the locale and user preferences
    NSString* numberString = (__bridge_transfer NSString*)CFNumberFormatterCreateStringWithNumber(kCFAllocatorDefault,
            formatter,
            (__bridge CFNumberRef)number);

    if (numberString) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:numberString forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        // DLog(@"GlobalizationCommand numberToString unable to format %@", [number stringValue]);
        [super sendErrorResponse:@"Unable to format" request:request];
    }
    CFRelease(formatter);
}

- (void)stringToNumber:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFNumberFormatterStyle style = kCFNumberFormatterDecimalStyle;
    NSString* numberString = nil;
    double doubleValue;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    id value = [options valueForKey:@"numberString"];

    if (value && [value isKindOfClass:[NSString class]]) {
        numberString = (NSString*)value;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];
    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired style of formatting
                if ([key isEqualToString:@"type"]) {
                    if ([item isEqualToString:@"percent"]) {
                        style = kCFNumberFormatterPercentStyle;
                    } else if ([item isEqualToString:@"currency"]) {
                        style = kCFNumberFormatterCurrencyStyle;
                    } else if ([item isEqualToString:@"decimal"]) {
                        style = kCFNumberFormatterDecimalStyle;
                    }
                }
            }
        }
    }

    CFNumberFormatterRef formatter = CFNumberFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            style);

    // we need to make this lenient so as to avoid problems with parsing currencies that have non-breaking space characters
    if (style == kCFNumberFormatterCurrencyStyle) {
        CFNumberFormatterSetProperty(formatter, kCFNumberFormatterIsLenient, kCFBooleanTrue);
    }

    // parse againist the largest type to avoid data loss
    Boolean rc = CFNumberFormatterGetValueFromString(formatter,
            (__bridge CFStringRef)numberString,
            NULL,
            kCFNumberDoubleType,
            &doubleValue);

    if (rc) {
        NSDictionary* dictionary = [NSDictionary dictionaryWithObject:[NSNumber numberWithDouble:doubleValue] forKey:@"value"];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        // DLog(@"GlobalizationCommand stringToNumber unable to parse %@", numberString);
        [super sendErrorResponse:@"Unable to parse" request:request];
    }

    CFRelease(formatter);
}

- (void)getNumberPattern:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    CFNumberFormatterStyle style = kCFNumberFormatterDecimalStyle;
    CFStringRef symbolType = NULL;
    NSString* symbol = @"";

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    // see if any options have been specified
    id items = [options valueForKey:@"options"];

    if (items && [items isKindOfClass:[NSMutableDictionary class]]) {
        NSEnumerator* enumerator = [items keyEnumerator];
        id key;

        // iterate through all the options
        while ((key = [enumerator nextObject])) {
            id item = [items valueForKey:key];

            // make sure that only string values are present
            if ([item isKindOfClass:[NSString class]]) {
                // get the desired style of formatting
                if ([key isEqualToString:@"type"]) {
                    if ([item isEqualToString:@"percent"]) {
                        style = kCFNumberFormatterPercentStyle;
                    } else if ([item isEqualToString:@"currency"]) {
                        style = kCFNumberFormatterCurrencyStyle;
                    } else if ([item isEqualToString:@"decimal"]) {
                        style = kCFNumberFormatterDecimalStyle;
                    }
                }
            }
        }
    }

    CFNumberFormatterRef formatter = CFNumberFormatterCreate(kCFAllocatorDefault,
            currentLocale,
            style);

    NSString* numberPattern = (__bridge NSString*)CFNumberFormatterGetFormat(formatter);

    if (style == kCFNumberFormatterCurrencyStyle) {
        symbolType = kCFNumberFormatterCurrencySymbol;
    } else if (style == kCFNumberFormatterPercentStyle) {
        symbolType = kCFNumberFormatterPercentSymbol;
    }

    if (symbolType) {
        symbol = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, symbolType);
    }

    NSString* decimal = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterDecimalSeparator);
    NSString* grouping = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterGroupingSeparator);
    NSString* posSign = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterPlusSign);
    NSString* negSign = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterMinusSign);
    NSNumber* fracDigits = (__bridge_transfer NSNumber*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterMinFractionDigits);
    NSNumber* roundingDigits = (__bridge_transfer NSNumber*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterRoundingIncrement);

    // put the pattern information into the dictionary
    if (numberPattern != nil) {
        NSArray* keys = [NSArray arrayWithObjects:@"pattern", @"symbol", @"fraction", @"rounding",
            @"positive", @"negative", @"decimal", @"grouping", nil];
        NSArray* values = [NSArray arrayWithObjects:numberPattern, symbol, fracDigits, roundingDigits,
            posSign, negSign, decimal, grouping, nil];
        NSDictionary* dictionary = [NSDictionary dictionaryWithObjects:values forKeys:keys];
        [super sendObjectResponse:dictionary request:request];
    }
    // error
    else {
        [super sendErrorResponse:@"Pattern error" request:request];
    }

    CFRelease(formatter);
}

- (void)getCurrencyPattern:(NSDictionary*)request withOptions:(NSDictionary *)options
{
    NSString* currencyCode = nil;
    NSString* numberPattern = nil;
    NSString* decimal = nil;
    NSString* grouping = nil;
    int32_t defaultFractionDigits;
    double roundingIncrement;
    Boolean rc;

    if (!options) {
        [super sendErrorResponse:@"no options given" request:request];
        return;
    }

    id value = [options valueForKey:@"currencyCode"];

    if (value && [value isKindOfClass:[NSString class]]) {
        currencyCode = (NSString*)value;
    }

    // first see if there is base currency info available and fill in the currency_info structure
    rc = CFNumberFormatterGetDecimalInfoForCurrencyCode((__bridge CFStringRef)currencyCode, &defaultFractionDigits, &roundingIncrement);

    // now set the currency code in the formatter
    if (rc) {
        CFNumberFormatterRef formatter = CFNumberFormatterCreate(kCFAllocatorDefault,
                currentLocale,
                kCFNumberFormatterCurrencyStyle);

        CFNumberFormatterSetProperty(formatter, kCFNumberFormatterCurrencyCode, (__bridge CFStringRef)currencyCode);
        CFNumberFormatterSetProperty(formatter, kCFNumberFormatterInternationalCurrencySymbol, (__bridge CFStringRef)currencyCode);

        numberPattern = (__bridge NSString*)CFNumberFormatterGetFormat(formatter);
        decimal = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterCurrencyDecimalSeparator);
        grouping = (__bridge_transfer NSString*)CFNumberFormatterCopyProperty(formatter, kCFNumberFormatterCurrencyGroupingSeparator);

        NSArray* keys = [NSArray arrayWithObjects:@"pattern", @"code", @"fraction", @"rounding",
            @"decimal", @"grouping", nil];
        NSArray* values = [NSArray arrayWithObjects:numberPattern, currencyCode, [NSNumber numberWithInt:defaultFractionDigits],
            [NSNumber numberWithDouble:roundingIncrement], decimal, grouping, nil];
        NSDictionary* dictionary = [NSDictionary dictionaryWithObjects:values forKeys:keys];
        [super sendObjectResponse:dictionary request:request];
        CFRelease(formatter);
    }
    // error
    else {
        // DLog(@"GlobalizationCommand getCurrencyPattern unable to get pattern for %@", currencyCode);
        [super sendErrorResponse:@"Unable to get pattern" request:request];
    }
}

- (void)dealloc
{
    if (currentLocale) {
        CFRelease(currentLocale);
        currentLocale = nil;
    }
}

@end
