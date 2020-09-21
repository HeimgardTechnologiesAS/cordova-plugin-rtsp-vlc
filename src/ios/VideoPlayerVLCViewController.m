//
//  VideoPlayerVLCViewController.m
//  MobileVLCKiteTest
//
//  Created by Yanbing Peng on 9/02/16.
//  Copyright © 2016 Yanbing Peng. All rights reserved.
//

#import "VideoPlayerVLCViewController.h"
#import <MobileVLCKit/MobileVLCKit.h>
#import "VideoPlayerVLC.h"


@interface VideoPlayerVLCViewController ()
@property(strong, nonatomic) UIButton *closeButton;
@property(strong, nonatomic) VLCMediaPlayer *mediaPlayer;
@property(strong, nonatomic) UIView *mediaView;
@property(strong, nonatomic) UIImageView *imgView;
@end

@implementation VideoPlayerVLCViewController

-(id)init{
    if (self = [super init]){
        self.playOnStart = YES;
    }
    return  self;
}

- (void)viewDidLoad {
    NSLog(@"[VideoPlayerVLCViewController viewDidLoad]");
    [super viewDidLoad];
    self.view.backgroundColor =  [UIColor blackColor];
    
    
    self.mediaView = [[UIView alloc] init];
    self.closeButton = [[UIButton alloc] init];
    self.imgView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 50, 50)];
    self.imgView.image = [UIImage imageNamed:@"rec.png"];
    self.mediaPlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--network-caching=150 --clock-jitter=0 --clock-synchro=0"]];
    self.mediaPlayer.delegate = self;
   
    self.closeButton.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.closeButton setTitle:@"×" forState:UIControlStateNormal];
    [self.closeButton setFont:[UIFont systemFontOfSize:20]];
    
    
    self.mediaView.translatesAutoresizingMaskIntoConstraints = NO;
    self.mediaView.backgroundColor = [UIColor blackColor];
    
    [self.view addSubview:self.imgView];
    [self.view addSubview:self.closeButton];
    [self.view addSubview:self.mediaView];
    
    
    NSLayoutConstraint *closeButtonTopConstraint = [NSLayoutConstraint constraintWithItem:self.closeButton attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.topLayoutGuide attribute:NSLayoutAttributeBottom multiplier:1.0 constant:0.0];
    NSLayoutConstraint *closeButtonLeftConstraint = [NSLayoutConstraint constraintWithItem:self.closeButton attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self.view attribute:NSLayoutAttributeLeftMargin multiplier:1.0 constant:0.0];
    NSLayoutConstraint *closeButtonHeightConstraint = [NSLayoutConstraint constraintWithItem:self.closeButton attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:nil attribute:NSLayoutAttributeNotAnAttribute multiplier:1.0 constant:30];

    
    
    NSLayoutConstraint *mediaViewWidthConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.view attribute:NSLayoutAttributeWidth multiplier:1.0 constant:0];
    NSLayoutConstraint *mediaViewCenterHorizontallyConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.view attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    NSLayoutConstraint *mediaViewTopConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.closeButton attribute:NSLayoutAttributeBottom multiplier:1.0 constant:8.0];
    NSLayoutConstraint *mediaViewBottomConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self.view attribute:NSLayoutAttributeBottom multiplier:1.0 constant:0];
    
    
    [self.view addConstraint:closeButtonTopConstraint];
    [self.view addConstraint:closeButtonLeftConstraint];
    [self.view addConstraint:closeButtonHeightConstraint];
    [self.view addConstraint:mediaViewWidthConstraint];
    [self.view addConstraint:mediaViewCenterHorizontallyConstraint];
    [self.view addConstraint:mediaViewTopConstraint];
    [self.view addConstraint:mediaViewBottomConstraint];
    
    self.mediaPlayer.drawable = self.mediaView;

    [self.closeButton addTarget:self action:@selector(stop) forControlEvents:UIControlEventTouchUpInside];
    [self.mediaView addGestureRecognizer:[[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(screenTouchRequest)]];

}

- (void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    if (self.playOnStart) {
        [self play];
    }
}

- (void)play{
    
    if (self.mediaPlayer != nil) {
        if (!self.mediaPlayer.isPlaying) {
            NSURL *mediaUrl = [[NSURL alloc] initWithString:self.urlString];
            if(mediaUrl != nil){
                [self.mediaPlayer setMedia:[[VLCMedia alloc] initWithURL:mediaUrl]];
            }
            else{
                return;
            }
            [self.mediaPlayer play];
        }
    }
}

- (void)stop{
    if (self.mediaPlayer != nil) {
        if (self.mediaPlayer.isPlaying) {
            [self.mediaPlayer stop];
        }
    }
    [[VideoPlayerVLC getInstance] stopInner];
    
    // dismiss view from stack
    [self.view removeFromSuperview];


}

- (void)mediaPlayerStateChanged:(NSNotification *)aNotification {
    VLCMediaPlayerState vlcState = self.mediaPlayer.state;

    switch (vlcState) {
        case VLCMediaPlayerStateStopped:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onStopVlc"];
            [self stop];
            break;
        case VLCMediaPlayerStateOpening:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onBuffering"];
            break;
        case VLCMediaPlayerStateBuffering:
            break;
        case VLCMediaPlayerStateEnded:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onVideoEnd"];
            [self stop];
            break;
        case VLCMediaPlayerStateError:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onError"];
            break;
        case VLCMediaPlayerStatePlaying:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onPlayVlc"];
            break;
        case VLCMediaPlayerStatePaused:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onPauseVlc"];
            break;
        default:
            [[VideoPlayerVLC getInstance] sendVlcState:@"default"];
            break;
    };
}

-(void) recordingRequest: (BOOL) value {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_recording_request" forKey:@"type"];
    [request setValue:@(value) forKey:@"value"];
    [[VideoPlayerVLC getInstance] sendExternalDataAsDictionary:request];
}

-(void) cameraMoveRequest: (NSString *) value {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_camera_move_request" forKey:@"type"];
    [request setValue:value forKey:@"value"];
    [[VideoPlayerVLC getInstance] sendExternalDataAsDictionary:request];
}

-(void) screenTouchRequest {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_screen_touch_event" forKey:@"type"];
    [request setValue:@"value" forKey:@"value"];
    [[VideoPlayerVLC getInstance] sendExternalDataAsDictionary:request];
}

@end
