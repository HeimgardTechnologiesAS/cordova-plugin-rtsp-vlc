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
@property(strong, nonatomic) VLCMediaPlayer *mediaPlayer;
@property(strong, nonatomic) UIView *subView;
@property(strong, nonatomic) UIView *mediaView;

@property(strong, nonatomic) UIImageView *closeButtonView;
@property(strong, nonatomic) UIImageView *imgView;
@property(strong, nonatomic) UIImageView *joystickView;
@property(strong, nonatomic) UIImageView *jstkUpBgView;
@property(strong, nonatomic) UIImageView *jstkLeftBgView;
@property(strong, nonatomic) UIImageView *jstkDownBgView;
@property(strong, nonatomic) UIImageView *jstkRightBgView;

@property(strong, nonatomic) UILabel *liveTextLabel;
@property(strong, nonatomic) UILabel *recordingProgressLabel;

@property(strong, nonatomic) NSTimer *recProgressTimer;

@property(strong, nonatomic) NSDateFormatter *dateFormatter;
@property(strong, nonatomic) IBOutletCollection(NSLayoutConstraint) NSArray* portraitConstrints;
@property(strong, nonatomic) NSDate *startDate;
@property BOOL recActive;
@property BOOL recWaitsForResponse;
@end

@implementation VideoPlayerVLCViewController {
    IBOutlet NSLayoutConstraint *mediaViewWidthConstraint;
    IBOutlet NSLayoutConstraint *mediaViewHeightConstraint;
    IBOutlet NSLayoutConstraint *mediaViewCenterHorizontallyConstraint;
    IBOutlet NSLayoutConstraint *mediaViewCenterVertiacllyConstraint;
    IBOutlet NSLayoutConstraint *mediaViewTopConstraint;
    IBOutlet NSLayoutConstraint *mediaViewLeftLandConstraint;
    IBOutlet NSLayoutConstraint *mediaViewBottomConstraint;
    
    IBOutlet NSLayoutConstraint *jstckBgAspectConstraint;
    IBOutlet NSLayoutConstraint *jstckBgHeightLandscapeConstraint;
    IBOutlet NSLayoutConstraint *jstckBgCenterHorizonzallyConstarint;
    IBOutlet NSLayoutConstraint *jstckBgBottomConstraint;
    IBOutlet NSLayoutConstraint *jstckBgRightConstraint;
    IBOutlet NSLayoutConstraint *jstckBgTopConstraint;
    
    IBOutlet NSLayoutConstraint *recButtonHeightConstraint;
    IBOutlet NSLayoutConstraint *recButtonAspectConstraint;
    IBOutlet NSLayoutConstraint *recButtonHorizontallyPortraitConstraint;
    IBOutlet NSLayoutConstraint *recButtonTopPortraitConstraint;
    IBOutlet NSLayoutConstraint *recButtonTopLandscapeConstraint;
    IBOutlet NSLayoutConstraint *recButtonRightLandscapeConstraint;
    IBOutlet NSLayoutConstraint *recButtonHeightLandscapeConstraint;
    
    IBOutlet NSLayoutConstraint *closeButtonHeightConstraint;
    IBOutlet NSLayoutConstraint *closeButtonAspectConstraint;
    IBOutlet NSLayoutConstraint *closeButtonTopConstraint;
    IBOutlet NSLayoutConstraint *closeButtonLeftConstraint;
    
    IBOutlet NSLayoutConstraint *liveLableHeightConstraint;
    IBOutlet NSLayoutConstraint *liveLableTopConstraint;
    IBOutlet NSLayoutConstraint *liveLableLeftPortraitConstraint;
    IBOutlet NSLayoutConstraint *liveLableLeftLandConstraint;
    IBOutlet NSLayoutConstraint *liveLableCenterYLandConstraint;
    
    IBOutlet NSLayoutConstraint *recLableHeightConstraint;
    IBOutlet NSLayoutConstraint *recLableBottomPortraitConstraint;
    IBOutlet NSLayoutConstraint *recLableTopLandConstraint;
    IBOutlet NSLayoutConstraint *recLableCenterXLandConstraint;
    

    

}

-(id)init{
    if (self = [super init]){
        self.playOnStart = YES;
    }
    return  self;
}

-(UIColor *)colorFromHex:(NSString *) hexColor{
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexColor];
    [scanner setScanLocation:1];
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0XFF0000) >> 16)/255.0 green:((rgbValue & 0XFF00) >> 8)/255.0 blue:(rgbValue & 0XFF)/255.0 alpha:1.0];
}

- (void)viewDidLoad {
    NSLog(@"[VideoPlayerVLCViewController viewDidLoad]");
    [super viewDidLoad];
    self.view.backgroundColor =  [UIColor blackColor];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(stop) name:UIApplicationWillResignActiveNotification object:nil];
    
    
    self.dateFormatter = [[NSDateFormatter alloc] init];
    [self.dateFormatter setDateFormat:@"mm:ss"];
    [self.dateFormatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0.0]];
    
    
    self.subView = [[UIView alloc] init];
    self.subView.backgroundColor = [UIColor blackColor];
    self.subView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.subView];
    
    
    if(@available(iOS 11, *)) {
        UILayoutGuide * guide = self.view.safeAreaLayoutGuide;
        [self.subView.leadingAnchor constraintEqualToAnchor:guide.leadingAnchor].active = YES;
        [self.subView.trailingAnchor constraintEqualToAnchor:guide.trailingAnchor].active = YES;
        [self.subView.topAnchor constraintEqualToAnchor:guide.topAnchor].active = YES;
        [self.subView.bottomAnchor constraintEqualToAnchor:guide.bottomAnchor].active = YES;
    }
    [self.view layoutIfNeeded];
    
    self.liveTextLabel = [[UILabel alloc] init];
    self.liveTextLabel.backgroundColor = [self colorFromHex:@"#ec1d1d"];
    self.liveTextLabel.layer.cornerRadius = 2;
    self.liveTextLabel.layer.masksToBounds = YES;
    self.liveTextLabel.textColor = [UIColor whiteColor];
    [self.liveTextLabel setFont:[UIFont fontWithName:@"Helvetica-Bold" size:12.0f]];
    [self.liveTextLabel setText:[NSString stringWithFormat:@"  %@  ", @"LIVE"]];
    self.liveTextLabel.translatesAutoresizingMaskIntoConstraints = NO;
    
    self.recordingProgressLabel = [[UILabel alloc] init];
    self.recordingProgressLabel.backgroundColor = [self colorFromHex:@"#ec1d1d"];
    self.recordingProgressLabel.layer.cornerRadius = 2;
    self.recordingProgressLabel.layer.masksToBounds = YES;
    self.recordingProgressLabel.textColor = [UIColor whiteColor];
    [self.recordingProgressLabel setFont:[UIFont fontWithName:@"Helvetica-Bold" size:16.0f]];
    self.recordingProgressLabel.translatesAutoresizingMaskIntoConstraints = NO;
    
    
    self.mediaView = [[UIView alloc] init];
    self.mediaView.backgroundColor = [UIColor greenColor];
    
    
    self.imgView =  [[UIImageView alloc] initWithFrame:CGRectMake(self.view.frame.size.width/2 - 25, self.view.frame.size.height / 2 + 170, 50, 50)];
    [self.imgView setImage:[UIImage imageNamed:@"recording-button-idle.png"]];
    self.imgView.contentMode = UIViewContentModeScaleAspectFit;
    [self.imgView setUserInteractionEnabled:YES];

    
    self.joystickView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.joystickView setImage:[UIImage imageNamed:@"joystick-ref-bg.png"]];
    [self.joystickView setContentMode:UIViewContentModeScaleAspectFit];

    
    self.jstkUpBgView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.jstkUpBgView setImage:[UIImage imageNamed:@"joystick-up-portrait.png"]];
    [self.jstkUpBgView setContentMode:UIViewContentModeScaleAspectFit];
    self.jstkUpBgView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.jstkUpBgView setUserInteractionEnabled:YES];

    
    
    self.jstkLeftBgView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.jstkLeftBgView setImage:[UIImage imageNamed:@"joystick-left-portrait.png"]];
    [self.jstkLeftBgView setContentMode:UIViewContentModeScaleAspectFit];
    self.jstkLeftBgView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.jstkLeftBgView setUserInteractionEnabled:YES];
    
    self.jstkDownBgView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.jstkDownBgView setImage:[UIImage imageNamed:@"joystick-down-portrait.png"]];
    [self.jstkDownBgView setContentMode:UIViewContentModeScaleAspectFit];
    self.jstkDownBgView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.jstkDownBgView setUserInteractionEnabled:YES];
    
    self.jstkRightBgView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.jstkRightBgView setImage:[UIImage imageNamed:@"joystick-right-portrait.png"]];
    [self.jstkRightBgView setContentMode:UIViewContentModeScaleAspectFit];
    self.jstkRightBgView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.jstkRightBgView setUserInteractionEnabled:YES];
    
    self.closeButtonView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, 20.0, 20.0)];
    [self.closeButtonView setImage:[UIImage imageNamed:@"close-button-land.png"]];
    [self.closeButtonView setContentMode:UIViewContentModeScaleAspectFit];
    self.closeButtonView.translatesAutoresizingMaskIntoConstraints = NO;

    
   
    
    
    self.mediaPlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--network-caching=2000 --clock-jitter=0 --clock-synchro=0"]];
    self.mediaPlayer.delegate = self;
   
    
    self.imgView.translatesAutoresizingMaskIntoConstraints = NO;
    
    self.joystickView.translatesAutoresizingMaskIntoConstraints = NO;
    
   
    
    
    
    
    
    self.mediaView.translatesAutoresizingMaskIntoConstraints = NO;
    self.mediaView.backgroundColor = [UIColor blackColor];
    
    
    [self.subView addSubview:self.mediaView];
    [self.subView addSubview:self.imgView];
    [self.subView addSubview:self.joystickView];
    [self.subView addSubview:self.closeButtonView];
    [self.subView addSubview:self.liveTextLabel];
    [self.subView addSubview:self.recordingProgressLabel];
    
    [self.subView addSubview:self.jstkUpBgView];
    [self.subView addSubview:self.jstkLeftBgView];
    [self.subView addSubview:self.jstkDownBgView];
    [self.subView addSubview:self.jstkRightBgView];
    
    
    [self.subView bringSubviewToFront:self.imgView];
    [self.subView bringSubviewToFront:self.closeButtonView];
    //[self.subView bringSubviewToFront:self.joystickView];
    [self.subView bringSubviewToFront:self.liveTextLabel];
    [self.subView bringSubviewToFront:self.recordingProgressLabel];
    [self.subView bringSubviewToFront:self.jstkUpBgView];
    [self.subView bringSubviewToFront:self.jstkLeftBgView];
    [self.subView bringSubviewToFront:self.jstkDownBgView];
    [self.subView bringSubviewToFront:self.jstkRightBgView];
    
    [self.closeButtonView setUserInteractionEnabled:YES];
    


    
    mediaViewWidthConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeWidth multiplier:1.0 constant:0];
    mediaViewHeightConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeWidth multiplier:(9.0/16.0) constant:0];
    mediaViewCenterHorizontallyConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    mediaViewCenterVertiacllyConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0];
    mediaViewTopConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeTop multiplier:1.0 constant:0];
    mediaViewBottomConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self.view attribute:NSLayoutAttributeBottom multiplier:1.0 constant:0];
    
    closeButtonHeightConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:nil attribute:NSLayoutAttributeNotAnAttribute multiplier:1.0 constant:25.0];
    closeButtonAspectConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.closeButtonView attribute:NSLayoutAttributeHeight multiplier: self.closeButtonView.image.size.width / self.closeButtonView.image.size.height constant:0];
    closeButtonTopConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeTop multiplier:1.0 constant:16.0];
    closeButtonLeftConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeLeft multiplier:1.0 constant:8.0];
    

    recButtonHeightConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeHeight multiplier:(1.0/12.0) constant:0];
    recButtonHeightLandscapeConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier:(1.0/2.5) constant:0];
    recButtonAspectConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.imgView attribute:NSLayoutAttributeHeight multiplier: self.imgView.image.size.width / self.imgView.image.size.height constant:0];
    recButtonHorizontallyPortraitConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0.0];
    recButtonTopPortraitConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeBottom multiplier:1.0 constant:8];
    recButtonTopLandscapeConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeTop multiplier:1.0 constant:40];
    recButtonRightLandscapeConstraint = [NSLayoutConstraint constraintWithItem:self.imgView attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeRight multiplier:1.0 constant:-8];
    
    
    liveLableHeightConstraint = [NSLayoutConstraint constraintWithItem:self.liveTextLabel attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:nil attribute:NSLayoutAttributeNotAnAttribute multiplier:1.0 constant:16.0];
    liveLableTopConstraint = [NSLayoutConstraint constraintWithItem:self.liveTextLabel attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeTop multiplier:1.0 constant:16.0];
    liveLableLeftPortraitConstraint = [NSLayoutConstraint constraintWithItem:self.liveTextLabel attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeLeft multiplier:1.0 constant:8.0];
    liveLableLeftLandConstraint = [NSLayoutConstraint constraintWithItem:self.liveTextLabel attribute:NSLayoutAttributeLeading relatedBy:NSLayoutRelationEqual toItem:self.closeButtonView attribute:NSLayoutAttributeTrailing multiplier:1.0 constant:12.0];
    liveLableCenterYLandConstraint = [NSLayoutConstraint constraintWithItem:self.liveTextLabel attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.closeButtonView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0];
    


    
    [self createJoystickRefBgConstraints];
        
    

    [self.subView addConstraint:mediaViewWidthConstraint];
    [self.subView addConstraint:mediaViewHeightConstraint];
    [self.subView addConstraint:mediaViewCenterHorizontallyConstraint];
    [self.subView addConstraint:mediaViewCenterVertiacllyConstraint];
    

    [self.subView addConstraint:closeButtonHeightConstraint];
    [self.subView addConstraint:closeButtonAspectConstraint];
    [self.subView addConstraint:closeButtonTopConstraint];
    [self.subView addConstraint:closeButtonLeftConstraint];
    
    [self.subView addConstraint:liveLableHeightConstraint];
    [self.subView addConstraint:liveLableTopConstraint];
    [self.subView addConstraint:liveLableLeftPortraitConstraint];
    
    
    [self initRecProgressGenericConstraints];
    [self applyRecProgressPortraitConstraints];
    
    
    
    

    [self applyRecButtonGenericConstarints];
    [self applyRecButtonPortraitConstraints];


    [self applyJoystickGenericConstarints];
    [self applyJoystickPortraitConstraints];


    [self createJoystickConstraints];
    
    self.mediaPlayer.drawable = self.mediaView;
    
    
    [self.closeButtonView addGestureRecognizer:[[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(stop)]];
    [self.mediaView addGestureRecognizer:[[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(screenTouchRequest)]];
    
    
    UILongPressGestureRecognizer *upBtnPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(upBtnLongPress:)];
    upBtnPress.minimumPressDuration = 0;
    [self.jstkUpBgView addGestureRecognizer:upBtnPress];
        
    UILongPressGestureRecognizer *leftBtnPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(leftBtnLongPress:)];
    leftBtnPress.minimumPressDuration = 0;
    [self.jstkLeftBgView addGestureRecognizer:leftBtnPress];
    
    UILongPressGestureRecognizer *downBtnPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(downBtnLongPress:)];
    downBtnPress.minimumPressDuration = 0;
    [self.jstkDownBgView addGestureRecognizer:downBtnPress];
    
    UILongPressGestureRecognizer *rightBtnPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(rightBtnLongPress:)];
    rightBtnPress.minimumPressDuration = 0;
    [self.jstkRightBgView addGestureRecognizer:rightBtnPress];
    
    [self.imgView addGestureRecognizer:[[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(recButtonPressed:)]];
    
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    self.recActive = NO;
    //[[NSNotificationCenter defaultCenter] addObserver: self selector:@selector(deviceOrientationDidChange:) name:UIDeviceOrientationDidChangeNotification object:nil];
}

-(BOOL)prefersStatusBarHidden{
    return YES;
}

-(void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator {
    [self.subView removeConstraint:mediaViewTopConstraint];
    [self.subView removeConstraints:@[mediaViewWidthConstraint,liveLableTopConstraint, liveLableLeftPortraitConstraint, liveLableLeftLandConstraint, liveLableCenterYLandConstraint]];
    [self.view removeConstraint:mediaViewBottomConstraint];
    UILayoutGuide * guide = self.view.safeAreaLayoutGuide;
    UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
    if(UIDeviceOrientationIsPortrait(orientation)) {
        [self.subView.bottomAnchor constraintEqualToAnchor:guide.bottomAnchor].active = NO;
        [self applyJoystickPortraitImages];
        [self applyRecProgressPortraitConstraints];
        [self.subView addConstraints:@[mediaViewWidthConstraint, liveLableTopConstraint, liveLableLeftPortraitConstraint]];
        [self applyRecButtonPortraitConstraints];
        [self applyJoystickPortraitConstraints];
        
    } else if(UIDeviceOrientationIsLandscape(orientation)){
        
        [self.subView.bottomAnchor constraintEqualToAnchor:guide.bottomAnchor].active = NO;
        [self applyJoystickLandscapeImages];
        [self applyRecButtonLandscapeConstraints];
        [self applyRecProgressLandConstraints];
        [self applyJoystickLandscapeConstraints];
        [self.subView addConstraint:mediaViewTopConstraint];
        [self.view addConstraint:mediaViewBottomConstraint];
        [self.subView addConstraint:liveLableLeftLandConstraint];
        [self.subView addConstraint:liveLableCenterYLandConstraint];
        
        
    }
}

-(void) applyJoystickPortraitImages{
    [self.jstkUpBgView setImage:[UIImage imageNamed:@"joystick-up-portrait.png"]];
    [self.jstkLeftBgView setImage:[UIImage imageNamed:@"joystick-left-portrait.png"]];
    [self.jstkDownBgView setImage:[UIImage imageNamed:@"joystick-down-portrait.png"]];
    [self.jstkRightBgView setImage:[UIImage imageNamed:@"joystick-right-portrait.png"]];
}

-(void) applyJoystickLandscapeImages{
    [self.jstkUpBgView setImage:[UIImage imageNamed:@"joystick-up-land.png"]];
    [self.jstkLeftBgView setImage:[UIImage imageNamed:@"joystick-left-land.png"]];
    [self.jstkDownBgView setImage:[UIImage imageNamed:@"joystick-down-land.png"]];
    [self.jstkRightBgView setImage:[UIImage imageNamed:@"joystick-right-land.png"]];
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

-(void)recButtonPressed:(UITapGestureRecognizer *) gesture {
    if(self.recWaitsForResponse) {
        return;
    }
    if(self.recActive) {
        [self recordingRequest:NO];
        [self stopTimer];
        [self.imgView setImage:[UIImage imageNamed:@"recording-button-idle.png"]];
        self.recActive = NO;
        [self screenTouchRequest];
    } else{
        self.recWaitsForResponse = YES;
        [self recordingRequest:YES];
        self.imgView.alpha = 0.4;
    }
}

-(void) recordingRequest: (BOOL) value {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_recording_request" forKey:@"type"];
    [request setValue:@(value) forKey:@"value"];
    NSError *err;
    NSData * jsonData = [NSJSONSerialization dataWithJSONObject:request options:0 error:&err];
    [[VideoPlayerVLC getInstance] sendExternalData:[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]];
}

-(void) recordingStatusReceived: (BOOL) value {
    self.recWaitsForResponse = NO;
    self.imgView.alpha = 1;
    if(value) {
        [self startTimer];
        [self.imgView setImage:[UIImage imageNamed:@"recording-button-active.png"]];
        self.recActive = YES;
    } else {
        self.recActive = NO;
        [self.imgView setImage:[UIImage imageNamed:@"recording-button-idle.png"]];
    }
}

-(void) cameraMoveRequest: (NSString *) value {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_camera_move_request" forKey:@"type"];
    [request setValue:value forKey:@"value"];
    NSError *err;
    NSData * jsonData = [NSJSONSerialization dataWithJSONObject:request options:0 error:&err];
    [[VideoPlayerVLC getInstance] sendExternalData:[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]];
}

-(void) screenTouchRequest {
    NSMutableDictionary* request = [[NSMutableDictionary alloc] init];
    [request setValue:@"player_screen_touch_event" forKey:@"type"];
    [request setValue:@"value" forKey:@"value"];
    [[VideoPlayerVLC getInstance] sendExternalDataAsDictionary:request];
}

-(void) initRecProgressGenericConstraints {
    recLableHeightConstraint = [NSLayoutConstraint constraintWithItem:self.recordingProgressLabel attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:nil attribute:NSLayoutAttributeNotAnAttribute multiplier:1.0 constant:18.0];
    recLableBottomPortraitConstraint = [NSLayoutConstraint constraintWithItem:self.recordingProgressLabel attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeTop multiplier:1.0 constant:-16.0];
    recLableTopLandConstraint = [NSLayoutConstraint constraintWithItem:self.recordingProgressLabel attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeTop multiplier:1.0 constant:16.0];
    recLableCenterXLandConstraint = [NSLayoutConstraint constraintWithItem:self.recordingProgressLabel attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    
    [self.subView addConstraint:recLableCenterXLandConstraint];
    [self.subView addConstraint:recLableHeightConstraint];

}

-(void) applyRecProgressPortraitConstraints {
    [self.subView removeConstraint:recLableTopLandConstraint];
    [self.subView addConstraint:recLableBottomPortraitConstraint];
}

-(void) applyRecProgressLandConstraints {
    [self.subView removeConstraint:recLableBottomPortraitConstraint];
    [self.subView addConstraint:recLableTopLandConstraint];
}


-(void) applyRecButtonGenericConstarints {
    [self.subView addConstraint:recButtonAspectConstraint];
}

-(void) applyRecButtonPortraitConstraints {
    [self.subView removeConstraints:@[recButtonHeightLandscapeConstraint, recButtonTopLandscapeConstraint]];
    [self.subView removeConstraint:recButtonRightLandscapeConstraint];
    [self.subView addConstraints:@[recButtonHeightConstraint, recButtonHorizontallyPortraitConstraint, recButtonTopPortraitConstraint]];
}

-(void) applyRecButtonLandscapeConstraints {
    [self.subView removeConstraints:@[recButtonHeightConstraint, recButtonHorizontallyPortraitConstraint, recButtonTopPortraitConstraint]];
    [self.subView addConstraints:@[recButtonHeightLandscapeConstraint, recButtonTopLandscapeConstraint]];
    [self.subView addConstraint:recButtonRightLandscapeConstraint];
}



-(void) createJoystickRefBgConstraints {
    jstckBgHeightLandscapeConstraint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeHeight multiplier:0.4 constant:0];
    jstckBgAspectConstraint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier: self.joystickView.image.size.width / self.joystickView.image.size.height constant:0];
    jstckBgCenterHorizonzallyConstarint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0.0];
    jstckBgTopConstraint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.imgView attribute:NSLayoutAttributeBottom multiplier:1.0 constant:20];
    jstckBgBottomConstraint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self.subView attribute:NSLayoutAttributeBottom multiplier:1.0 constant:-24];
    jstckBgRightConstraint = [NSLayoutConstraint constraintWithItem:self.joystickView attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self.mediaView attribute:NSLayoutAttributeRight multiplier:1.0 constant:-8];
}

-(void) applyJoystickGenericConstarints {
    [self.subView addConstraint:jstckBgAspectConstraint];
    [self.subView addConstraint:jstckBgBottomConstraint];
}

-(void) applyJoystickPortraitConstraints {
    [self.subView removeConstraints:@[jstckBgHeightLandscapeConstraint]];
    [self.subView removeConstraints:@[jstckBgRightConstraint]];
    [self.subView addConstraints:@[jstckBgCenterHorizonzallyConstarint, jstckBgTopConstraint]];
}

-(void) applyJoystickLandscapeConstraints {
    [self.subView removeConstraints:@[jstckBgCenterHorizonzallyConstarint, jstckBgTopConstraint]];
    [self.subView addConstraints:@[jstckBgHeightLandscapeConstraint]];
    [self.subView addConstraint:jstckBgRightConstraint];
}




-(void) createJoystickConstraints {
    NSLayoutConstraint *jstkHeight = [NSLayoutConstraint constraintWithItem:self.jstkUpBgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier:self.jstkUpBgView.image.size.height / self.joystickView.image.size.height constant:0];
    NSLayoutConstraint *jstkAspectRatio = [NSLayoutConstraint constraintWithItem:self.jstkUpBgView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.jstkUpBgView attribute:NSLayoutAttributeHeight multiplier: self.jstkUpBgView.image.size.width / self.jstkUpBgView.image.size.height constant:0];
    NSLayoutConstraint *jstkUpCenter = [NSLayoutConstraint constraintWithItem:self.jstkUpBgView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0.0];
    NSLayoutConstraint *jstkUpTop = [NSLayoutConstraint constraintWithItem:self.jstkUpBgView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeTop multiplier:1.0 constant:0.0];

    
    NSLayoutConstraint *jstkLeftHeight = [NSLayoutConstraint constraintWithItem:self.jstkLeftBgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationLessThanOrEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier:self.jstkLeftBgView.image.size.height / self.joystickView.image.size.height constant:0];
    NSLayoutConstraint *jstkLeftAspectRatio = [NSLayoutConstraint constraintWithItem:self.jstkLeftBgView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.jstkLeftBgView attribute:NSLayoutAttributeHeight multiplier: self.jstkLeftBgView.image.size.width / self.jstkLeftBgView.image.size.height constant:0];
    NSLayoutConstraint *jstkLeftVerticalCenter = [NSLayoutConstraint constraintWithItem:self.jstkLeftBgView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0.0];
    NSLayoutConstraint *jstkLeftLeft = [NSLayoutConstraint constraintWithItem:self.jstkLeftBgView attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeLeft multiplier:1.0 constant:0];
    
    
    NSLayoutConstraint *jstkDownHeight = [NSLayoutConstraint constraintWithItem:self.jstkDownBgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier:self.jstkDownBgView.image.size.height / self.joystickView.image.size.height constant:0];
    NSLayoutConstraint *jstkDownAspectRatio = [NSLayoutConstraint constraintWithItem:self.jstkDownBgView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.jstkDownBgView attribute:NSLayoutAttributeHeight multiplier: self.jstkDownBgView.image.size.width / self.jstkDownBgView.image.size.height constant:0];
    NSLayoutConstraint *jstkDownBottomCenter = [NSLayoutConstraint constraintWithItem:self.jstkDownBgView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    NSLayoutConstraint *jstkDownBottom = [NSLayoutConstraint constraintWithItem:self.jstkDownBgView attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeBottom multiplier:1.0 constant:0];
    
    NSLayoutConstraint *jstkRightHeight = [NSLayoutConstraint constraintWithItem:self.jstkRightBgView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeHeight multiplier:self.jstkRightBgView.image.size.height / self.joystickView.image.size.height constant:0];
    NSLayoutConstraint *jstkRightAspectRatio = [NSLayoutConstraint constraintWithItem:self.jstkRightBgView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self.jstkRightBgView attribute:NSLayoutAttributeHeight multiplier: self.jstkRightBgView.image.size.width / self.jstkRightBgView.image.size.height constant:0];
    NSLayoutConstraint *jstkRightVerticalCenter = [NSLayoutConstraint constraintWithItem:self.jstkRightBgView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0.0];
    NSLayoutConstraint *jstkRightRight = [NSLayoutConstraint constraintWithItem:self.jstkRightBgView attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self.joystickView attribute:NSLayoutAttributeRight multiplier:1.0 constant:0];
    
    [self.subView addConstraint:jstkHeight];
    [self.subView addConstraint:jstkAspectRatio];
    [self.subView addConstraint:jstkUpCenter];
    [self.subView addConstraint:jstkUpTop];
    
    [self.subView addConstraint:jstkLeftHeight];
    [self.subView addConstraint:jstkLeftAspectRatio];
    [self.subView addConstraint:jstkLeftVerticalCenter];
    [self.subView addConstraint:jstkLeftLeft];

    
    [self.subView addConstraint:jstkDownHeight];
    [self.subView addConstraint:jstkDownAspectRatio];
    [self.subView addConstraint:jstkDownBottomCenter];
    [self.subView addConstraint:jstkDownBottom];

    [self.subView addConstraint:jstkRightHeight];
    [self.subView addConstraint:jstkRightAspectRatio];
    [self.subView addConstraint:jstkRightVerticalCenter];
    [self.subView addConstraint:jstkRightRight];
}

-(void) startTimer {
    if(self.recProgressTimer) {
        [self stopTimer];
    }
    
    self.recordingProgressLabel.text = @"  00:00  ";
    self.recordingProgressLabel.alpha  = 1;
    self.startDate = [NSDate date];
    self.recProgressTimer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(updateTimer:) userInfo:nil repeats:YES];
}

-(void) stopTimer{
    self.recordingProgressLabel.alpha  = 0;
    [self.recProgressTimer invalidate];
    self.recProgressTimer = nil;
}

-(void) updateTimer:(NSTimer *)timer {
    NSDate *currentDate = [NSDate date];
    NSTimeInterval timeInterval = [currentDate timeIntervalSinceDate:self.startDate];
    NSDate *timerDate = [NSDate dateWithTimeIntervalSince1970:timeInterval];
    NSString *timeString = [self.dateFormatter stringFromDate:timerDate];
    self.recordingProgressLabel.text = [NSString stringWithFormat:@"  %@  ", timeString];
}

-(void) upBtnLongPress:(UILongPressGestureRecognizer *)gesture {
    if(gesture.state == UIGestureRecognizerStateBegan) {
        [self cameraMoveRequest:@"1"];
        self.jstkUpBgView.alpha = 0.5;
    }
    else if(gesture.state == UIGestureRecognizerStateEnded) {
        [self cameraMoveRequest:@"0"];
        self.jstkUpBgView.alpha = 1;
    }
}

-(void) leftBtnLongPress:(UILongPressGestureRecognizer *)gesture {
    if(gesture.state == UIGestureRecognizerStateBegan) {
        [self cameraMoveRequest:@"3"];
        self.jstkLeftBgView.alpha = 0.5;
    }
    else if(gesture.state == UIGestureRecognizerStateEnded) {
        [self cameraMoveRequest:@"0"];
        self.jstkLeftBgView.alpha = 1;
    }
}

-(void) downBtnLongPress:(UILongPressGestureRecognizer *)gesture {
    if(gesture.state == UIGestureRecognizerStateBegan) {
        [self cameraMoveRequest:@"2"];
        self.jstkDownBgView.alpha = 0.5;
    }
    else if(gesture.state == UIGestureRecognizerStateEnded) {
        [self cameraMoveRequest:@"0"];
        self.jstkDownBgView.alpha = 1;
    }
}

-(void) rightBtnLongPress:(UILongPressGestureRecognizer *)gesture {
    if(gesture.state == UIGestureRecognizerStateBegan) {
        [self cameraMoveRequest:@"4"];
        self.jstkRightBgView.alpha = 0.5;
    }
    else if(gesture.state == UIGestureRecognizerStateEnded) {
        [self cameraMoveRequest:@"0"];
        self.jstkRightBgView.alpha = 1;
    }
}


@end
