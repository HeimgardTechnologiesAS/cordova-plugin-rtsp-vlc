//
//  RecordingPlayerVLCViewController.h
//  Heimgard
//
//  Created by Mario Balug on 17.03.2022..
//

#import <MobileVLCKit/MobileVLCKit.h>
#import <UIKit/UIKit.h>

@interface RecordingPlayerVLCViewController
: UIViewController <VLCMediaPlayerDelegate>
@property(nonatomic) BOOL playOnStart;
@property(strong, nonatomic) NSString *urlString;

- (void)play;
- (void)stop;
@end
