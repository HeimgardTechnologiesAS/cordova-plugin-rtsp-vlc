//
//  VideoPlayerVLCViewController.h
//  MobileVLCKiteTest
//
//  Created by Yanbing Peng on 9/02/16.
//  Copyright © 2016 Yanbing Peng. All rights reserved.
//

#import <MobileVLCKit/MobileVLCKit.h>
#import <UIKit/UIKit.h>

@interface VideoPlayerVLCViewController
: UIViewController <VLCMediaPlayerDelegate>
@property(nonatomic) BOOL playOnStart;
@property(strong, nonatomic) NSString *urlString;

- (void)play;
- (void)stop;
- (UIColor *)colorFromHex:(NSString *)hexColor;
- (void)recordingRequest:(BOOL)value;
- (void)cameraMoveRequest:(NSString *)value;
- (void)screenTouchRequest;
- (void)recordingStatusReceived:(BOOL)value;
- (void)elementsVisibilityRequest:(BOOL)value;
- (void)setTranslations:(NSString *)liveIndicator recNotification:(NSString *)recNotification;
- (void) setJoystickButtonViewEnabled;

@end
