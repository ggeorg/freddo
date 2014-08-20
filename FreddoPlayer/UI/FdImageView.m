//
//  FtvImageView.m
//  FreddoTV Player
//
//  Created by George Georgopoulos on 18/7/13.
//  Copyright (c) 2013 ArkaSoft LLC. All rights reserved.
//

#import "FdImageView.h"

@interface FdImageView()

@property (nonatomic,readonly) NSOperationQueue *operationQueue;

- (void)loadImage:(NSString *)url;
- (void)displayImage:(UIImage *)image;

@end

@implementation FdImageView

@synthesize operationQueue;

- (id)init:(NSString *)name options:(NSDictionary*)options
{
    if (self = [super init:name withView:[[UIImageView alloc] init] options:options]) {
        operationQueue = [[NSOperationQueue alloc] init];
    }
    return self;
}

- (void)setup:(NSDictionary *)options
{
    [super setup:options];
    
    //    UILabel *label = (UILabel *)self.view;
    //    [label sizeToFit];
}

- (void)set:(NSDictionary *)options
{
    for(id key in options) {
        NSString *property = (NSString *)key;
        if ([property isEqualToString:@"src"]) {
            NSString *src = [self getStringProperty:options property:property];
            if (src != nil) {
                NSInvocationOperation *operation = [[NSInvocationOperation alloc]
                                                    initWithTarget:self
                                                    selector:@selector(loadImage:)
                                                    object:src];
                
                [operationQueue addOperation:operation];
            }
        }
    }
    
    [super set:options];
}

- (void)loadImage:(NSString *)url
{
    NSDictionary *event = [[NSMutableDictionary alloc] initWithCapacity:3];
    [event setValue:@"1.0" forKey:@"ftv"];
    [event setValue:self.nameInReply forKey:@"service"];
    [event setValue:@"onload" forKey:@"action"];
    
    NSData *imageData = [[NSData alloc] initWithContentsOfURL:[NSURL URLWithString:url]];
    if (imageData != nil) {
        UIImage *image = [[UIImage alloc] initWithData:imageData];
        [self performSelectorOnMainThread:@selector(displayImage:) withObject:image waitUntilDone:NO];
        [event setValue:[[NSNumber alloc] initWithBool:TRUE] forKey:@"params"];
    } else {
        [event setValue:[[NSNumber alloc] initWithBool:FALSE] forKey:@"params"];
    }
    
    // Send as incoming message...
    [[NSNotificationCenter defaultCenter] postNotificationName:@"freddotv.IncomingMsg" object:nil userInfo:event];

}

- (void)displayImage:(UIImage *)image {
    UIImageView *imageView = (UIImageView *)self.view;
    [imageView setImage:image];
}

@end
