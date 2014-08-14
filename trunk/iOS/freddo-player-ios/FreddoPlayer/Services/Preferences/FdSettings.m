#import "FdSettings.h"

@implementation FdSettings

- (Boolean)onEvent:(NSDictionary*)event
{
    NSString *action = (NSString*)[event objectForKey:@"action"];
    
    if ([@"getPreference" isEqualToString:action]) {
        NSString *property = [event objectForKey:@"params"];
        [self get:property request:event];
        return YES;
    } else if ([@"setPreference" isEqualToString:action]) {
        [self set:[event objectForKey:@"params"]];
        [super sendBooleanResponse:YES request:event];
        return YES;
    }
    
    return [super onEvent:event];
}

- (void)get:(NSString *)property request:(NSDictionary *)request
{
    id returnVar = [[NSUserDefaults standardUserDefaults] objectForKey:property];
    if (returnVar == nil) {
        returnVar = [self getSettingFromBundle:property];
        if (returnVar == nil) {
            [super sendErrorResponse:@"Key not found" request:request];
        } else {
            [super sendGenericResponse:returnVar request:request];
        }
    } else {
        [super sendGenericResponse:returnVar request:request];
    }
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        id settingsValue = [options objectForKey:key];
        if ([settingsValue isKindOfClass:[NSNull class]]) {
            settingsValue = nil;
        }
        [[NSUserDefaults standardUserDefaults] setValue:settingsValue forKey:key];
        [[NSUserDefaults standardUserDefaults] synchronize];
        
    }
    [super set:options];
}

/*
 Parsing the Root.plist for the key, because there is a bug/feature in Settings.bundle
 So if the user haven't entered the Settings for the app, the default values aren't accessible through NSUserDefaults.
 */
- (NSString*)getSettingFromBundle:(NSString*)settingsName
{
	NSString *pathStr = [[NSBundle mainBundle] bundlePath];
	NSString *settingsBundlePath = [pathStr stringByAppendingPathComponent:@"Settings.bundle"];
	NSString *finalPath = [settingsBundlePath stringByAppendingPathComponent:@"Root.plist"];
	
	NSDictionary *settingsDict = [NSDictionary dictionaryWithContentsOfFile:finalPath];
	NSArray *prefSpecifierArray = [settingsDict objectForKey:@"PreferenceSpecifiers"];
	NSDictionary *prefItem;
	for (prefItem in prefSpecifierArray)
	{
		if ([[prefItem objectForKey:@"Key"] isEqualToString:settingsName])
			return [prefItem objectForKey:@"DefaultValue"];
	}
	return nil;
}
@end
