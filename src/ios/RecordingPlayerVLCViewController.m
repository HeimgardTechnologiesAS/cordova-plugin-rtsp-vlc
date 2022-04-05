//
//  RecordingPlayerVLCViewController.m
//  Heimgard
//
//  Created by Mario Balug on 17.03.2022..
//

#import "RecordingPlayerVLCViewController.h"
#import <MobileVLCKit/MobileVLCKit.h>
#import "VideoPlayerVLC.h"
#include <math.h>

@interface RecordingPlayerVLCViewController ()
@property(strong, nonatomic) VLCMediaPlayer* mediaPlayer;
@property(strong, nonatomic) VLCMedia* media;
@property(strong, nonatomic) UIView* subView;
@property(strong, nonatomic) UIView* mediaView;

@property(strong, nonatomic) UIImageView* closeButtonView;
@property(strong, nonatomic) UIImageView* playButtonView;

@property(strong, nonatomic) UIActivityIndicatorView* indicatorView;

@property(strong, nonatomic) NSTimer* loadingIndicatorDismissTimer;
@property(strong, nonatomic) NSTimer* hideVideoControlsTimer;
@property(strong, nonatomic) NSTimer* seekbarTimer;
@property(strong, nonatomic) NSTimer* seekToZeroTimer;


@property(strong, nonatomic) NSDateFormatter* dateFormatter;
@property(strong, nonatomic) NSDate* startDate;

@property(strong, nonatomic) UISlider* seekbar;
@property(strong, nonatomic) UILabel* seekbarMax;
@property(strong, nonatomic) UILabel* seekbarCurrent;

@property NSInteger currentNumberOfDisplayedPictures;
@end

@implementation RecordingPlayerVLCViewController {
    IBOutlet NSLayoutConstraint* mediaViewWidthConstraint;
    IBOutlet NSLayoutConstraint* mediaViewHeightConstraint;
    IBOutlet NSLayoutConstraint* mediaViewCenterHorizontallyConstraint;
    IBOutlet NSLayoutConstraint* mediaViewCenterVertiacllyConstraint;
    IBOutlet NSLayoutConstraint* mediaViewTopConstraint;
    IBOutlet NSLayoutConstraint* mediaViewLeftLandConstraint;
    IBOutlet NSLayoutConstraint* mediaViewBottomConstraint;
    
    
    IBOutlet NSLayoutConstraint* closeButtonHeightConstraint;
    IBOutlet NSLayoutConstraint* closeButtonAspectConstraint;
    IBOutlet NSLayoutConstraint* closeButtonTopConstraint;
    IBOutlet NSLayoutConstraint* closeButtonLeftConstraint;
    
    IBOutlet NSLayoutConstraint* playBtnAspectConstraint;
    IBOutlet NSLayoutConstraint* playBtnHeightConstraint;
    IBOutlet NSLayoutConstraint* playBtnCenterHorizontallyConstraint;
    IBOutlet NSLayoutConstraint* playBtnCenterVertiacllyConstraint;
}

- (id)init {
    if (self = [super init]) {
        self.playOnStart = YES;
    }
    return self;
}

- (void)viewDidLoad {
    NSLog(@"[RecordingPlayerVLCViewController viewDidLoad]");
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor blackColor];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(stop)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];
    
    self.dateFormatter = [[NSDateFormatter alloc] init];
    [self.dateFormatter setDateFormat:@"mm:ss"];
    [self.dateFormatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0.0]];
    
    self.subView = [[UIView alloc] init];
    self.subView.backgroundColor = [UIColor blackColor];
    self.subView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.subView];
    [self.subView setUserInteractionEnabled:YES];
    
    if (@available(iOS 11, *)) {
        UILayoutGuide* guide = self.view.safeAreaLayoutGuide;
        [self.subView.leadingAnchor constraintEqualToAnchor:guide.leadingAnchor].active = YES;
        [self.subView.trailingAnchor constraintEqualToAnchor:guide.trailingAnchor].active = YES;
        [self.subView.topAnchor constraintEqualToAnchor:guide.topAnchor].active = YES;
        [self.subView.bottomAnchor constraintEqualToAnchor:guide.bottomAnchor].active = YES;
    }
    [self.view layoutIfNeeded];
    
    
    self.indicatorView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    self.indicatorView.translatesAutoresizingMaskIntoConstraints = NO;
    
    self.mediaView = [[UIView alloc] init];
    self.mediaView.backgroundColor = [UIColor greenColor];
    self.mediaView.translatesAutoresizingMaskIntoConstraints = NO;
    self.mediaView.backgroundColor = [UIColor blackColor];
    
    self.seekbarMax = [[UILabel alloc] init];
    UIColor* bg1 = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.5];
    self.seekbarMax.backgroundColor = bg1;
    self.seekbarMax.layer.cornerRadius = 2;
    self.seekbarMax.layer.masksToBounds = YES;
    self.seekbarMax.textColor = [UIColor whiteColor];
    [self.seekbarMax setFont:[UIFont fontWithName:@"Helvetica-Bold" size:12.0f]];
    self.seekbarMax.translatesAutoresizingMaskIntoConstraints = NO;
    self.seekbarMax.text = @"  00:00  ";
    
    self.seekbarCurrent = [[UILabel alloc] init];
    self.seekbarCurrent.backgroundColor = bg1;
    self.seekbarCurrent.layer.cornerRadius = 2;
    self.seekbarCurrent.layer.masksToBounds = YES;
    self.seekbarCurrent.textColor = [UIColor whiteColor];
    [self.seekbarCurrent setFont:[UIFont fontWithName:@"Helvetica-Bold" size:12.0f]];
    self.seekbarCurrent.translatesAutoresizingMaskIntoConstraints = NO;
    self.seekbarCurrent.text = @"  00:00  ";
    
    self.seekbar = [[UISlider alloc] init];
    self.seekbar.translatesAutoresizingMaskIntoConstraints = NO;
    [self.seekbar setUserInteractionEnabled:YES];
    [self.seekbar setBackgroundColor:[UIColor clearColor]];
    self.seekbar.minimumValue = 0.0;
    [self.seekbar setMinimumTrackTintColor:[UIColor whiteColor]];
    [self.seekbar setMaximumTrackTintColor:[self colorFromHex:@"#9a9a9a"]];
    UIImage* img = [UIImage imageNamed:@"seek_thumb.png"];
    
    [self.seekbar setThumbImage:[self imageWithImage:img scaledToFillSize:CGSizeMake(17.0, 17.0)] forState:UIControlStateNormal];
    
    self.closeButtonView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"close-button-land.png"]];
    [self.closeButtonView setContentMode:UIViewContentModeScaleAspectFit];
    self.closeButtonView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.closeButtonView setUserInteractionEnabled:YES];
    
    self.playButtonView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"back_play.png"]];
    [self.playButtonView setContentMode:UIViewContentModeScaleAspectFit];
    self.playButtonView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.playButtonView setUserInteractionEnabled:YES];
    
    
    [self.subView addSubview:self.mediaView];
    [self.subView addSubview:self.closeButtonView];
    [self.subView addSubview:self.indicatorView];
    [self.subView addSubview:self.playButtonView];
    [self.subView addSubview:self.seekbar];
    [self.subView addSubview:self.seekbarCurrent];
    [self.subView addSubview:self.seekbarMax];

    
    [self.subView bringSubviewToFront:self.closeButtonView];
    [self.subView bringSubviewToFront:self.indicatorView];
    [self.subView bringSubviewToFront:self.playButtonView];
    [self.subView bringSubviewToFront:self.seekbarCurrent];
    [self.subView bringSubviewToFront:self.seekbarMax];

    
    // create view constraints and apply generic constraints which are equal to both, portrait and landscape mode
    [self createMediaViewConstraints];
    [self createCloseButtonConstraints];
    [self createLoadingIndicatorConstraints];
    [self createPlayBtnConstraints];
    [self createSeekbarConstraints];
    
    // view will be loaded in portrait mode, so apply portrait constraints
    [self applyMediaViewPortraitConstraints];
    
    self.mediaPlayer = [[VLCMediaPlayer alloc]
                        initWithOptions:@[ @"--network-caching=2000 --clock-jitter=2500 --clock-synchro=0 --file-caching=3000 --live-caching=3000 --avcodec-skip-frame=1 --no-skip-frames --no-drop-late-frames --no-avcodec-hurry-up --avcodec-hw=any --prefetch-buffer-size=1048576 --prefetch-read-size=1048576 --prefetch-seek-threshold=1024 --no-directx-overlay" ]];
    self.mediaPlayer.delegate = self;
    self.mediaPlayer.drawable = self.mediaView;
    
    [self initViewGestures];
    
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    [self hidePlayerControls:nil];
    
    [self addObserver:self forKeyPath:@"_mediaPlayer.position" options:0 context:nil];
    
    [[VideoPlayerVLC getInstance] sendVlcState:@"onViewCreated"];
}

- (void)viewWillDisappear:(BOOL)animated {
    [self stop];
}

-(void) initViewGestures {
    [self.closeButtonView
     addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self
                                                                  action:@selector(stop)]];
    [self.mediaView addGestureRecognizer:[[UITapGestureRecognizer alloc]
                                          initWithTarget:self
                                          action:@selector(screenTappedRequest:)]];
    [self.subView addGestureRecognizer:[[UITapGestureRecognizer alloc]
                                        initWithTarget:self
                                        action:@selector(screenTappedRequest:)]];
    
    [self.playButtonView addGestureRecognizer:[[UITapGestureRecognizer alloc]
                                          initWithTarget:self
                                          action:@selector(playBtnTappedRequest:)]];
    [self.seekbar addTarget:self action:@selector(seekbarTapStart:) forControlEvents:UIControlEventTouchDown];
    [self.seekbar addTarget:self action:@selector(seekbarTapStop:) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel];
    [self.seekbar addTarget:self action:@selector(seekbarValueChanged:) forControlEvents:UIControlEventValueChanged];
}

- (void)seekbarTapStart:(UISlider*)sender {
    if(self.hideVideoControlsTimer) {
        [self.hideVideoControlsTimer invalidate];
    }
    [self removeObserver:self forKeyPath:@"_mediaPlayer.position"];
}

- (void)seekbarTapStop:(UISlider*)sender {
    [self.mediaPlayer setPosition:roundf(self.seekbar.value / self.seekbar.maximumValue * 100) / 100];
    [self addObserver:self forKeyPath:@"_mediaPlayer.position" options:0 context:nil];
    if(self.mediaPlayer.isPlaying) {
        [self delayedPlayerControlsHide];
    }
}


- (void)seekbarValueChanged:(UISlider*)sender {
    if(self.seekbar.value > 0 ){
        NSUInteger m = [[NSNumber numberWithFloat:(self.seekbar.value / 1000 / 60.0)] unsignedIntegerValue] % 60;
        NSUInteger s = [[NSNumber numberWithFloat:self.seekbar.value / 1000] unsignedIntegerValue] % 60;
        self.seekbarCurrent.text = [NSString stringWithFormat:@"  %02lu:%02lu  ", m, s];
    }
}

- (BOOL)prefersStatusBarHidden {
    return YES;
}

-(void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context{
    if([keyPath isEqualToString:@"_mediaPlayer.position"]){
        if(_mediaPlayer) {
            self.seekbar.value = self.media.length.value.floatValue * self.mediaPlayer.position;
            if(self.seekbar.value > 0 ){
                NSUInteger m = [[NSNumber numberWithFloat:(self.seekbar.value / 1000 / 60.0)] unsignedIntegerValue] % 60;
                NSUInteger s = [[NSNumber numberWithFloat:self.seekbar.value / 1000] unsignedIntegerValue] % 60;
                self.seekbarCurrent.text = [NSString stringWithFormat:@"  %02lu:%02lu  ", m, s];
            }
        }
    }else{
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

- (void)viewWillTransitionToSize:(CGSize)size
       withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator {
    
    if (UIDeviceOrientationIsPortrait([[UIDevice currentDevice] orientation])) {
        [self.subView.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = NO;
        [self applyMediaViewPortraitConstraints];
        
    } else if (UIDeviceOrientationIsLandscape([[UIDevice currentDevice] orientation])) {
        [self.subView.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = NO;
        [self applyMediaViewLandscapeConstraints];
    }
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    if (self.playOnStart) {
        [self play];
    }
}

- (void)play {
    if (self.mediaPlayer != nil) {
        if (!self.mediaPlayer.isPlaying) {
            NSURL* mediaUrl = [[NSURL alloc] initWithString:self.urlString];
            if (mediaUrl != nil) {
                self.media = [[VLCMedia alloc] initWithURL:mediaUrl];
                [self.mediaPlayer setMedia: self.media];
            } else {
                return;
            }
            [self.mediaPlayer play];
            
        }
    }
}

- (void)stop {
    if (self.mediaPlayer != nil) {
        if (self.mediaPlayer.isPlaying) {
            [self.mediaPlayer stop];
        }
    }
    [[VideoPlayerVLC getInstance] stopInner];
    
    // dismiss view from stack
    [self.view removeFromSuperview];
}

- (void)mediaPlayerStateChanged:(NSNotification*)aNotification {
    VLCMediaPlayerState vlcState = self.mediaPlayer.state;
    VLCMediaState mediaState = self.mediaPlayer.media.state;
    switch (mediaState) {
        case VLCMediaStateNothingSpecial:
            [[VideoPlayerVLC getInstance] sendVlcState:@"VLCMediaStateNothingSpecial"];
            break;
        case VLCMediaStateBuffering:
            [[VideoPlayerVLC getInstance] sendVlcState:@"VLCMediaStateBuffering"];
            break;
        case VLCMediaStatePlaying:
            [[VideoPlayerVLC getInstance] sendVlcState:@"VLCMediaStatePlaying"];
            
            if(self.mediaPlayer.isPlaying) {
                self.seekbar.maximumValue = self.media.length.value.floatValue;
                if(self.seekbar.maximumValue > 0 ){
                    NSUInteger m = [[NSNumber numberWithFloat:(self.seekbar.maximumValue / 1000 / 60.0)] unsignedIntegerValue] % 60;
                    NSUInteger s = [[NSNumber numberWithFloat:self.seekbar.maximumValue / 1000] unsignedIntegerValue] % 60;
                    self.seekbarMax.text = [NSString stringWithFormat:@"  %02lu:%02lu  ", m, s];
                }
                [self hidePlayerControls:nil];
                [self.playButtonView setImage:[UIImage imageNamed:@"back_pause.png"]];
            } else {
                if(self.hideVideoControlsTimer) {
                    [self.hideVideoControlsTimer invalidate];
                }
                [self.playButtonView setImage:[UIImage imageNamed:@"back_play.png"]];
            }
            break;
        case VLCMediaStateError:
            [[VideoPlayerVLC getInstance] sendVlcState:@"VLCMediaStateError"];
            break;
    }
    
    switch (vlcState) {
        case VLCMediaPlayerStateStopped:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onStopVlc"];
            [self stop];
            break;
        case VLCMediaPlayerStateOpening:
            [self resetPlayer];
            [[VideoPlayerVLC getInstance] sendVlcState:@"onBuffering"];
            break;
        case VLCMediaPlayerStateBuffering:
            [self delayedDismissLoadingAnimation];
            [self resetPlayer];
            [[VideoPlayerVLC getInstance] sendVlcState:@"onBuffering"];
            break;
        case VLCMediaPlayerStateEnded:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onVideoEnd"];
            [self stop];
            break;
        case VLCMediaPlayerStateError:
            [[VideoPlayerVLC getInstance] sendVlcState:@"onError"];
            break;
        case VLCMediaPlayerStatePlaying:
            [self delayedDismissLoadingAnimation];
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


- (void)resetPlayer {
    if(self.seekToZeroTimer) {
        [self.seekToZeroTimer invalidate];
    }
    if(!self.mediaPlayer.isPlaying) {
        return;
    }
    self.currentNumberOfDisplayedPictures = [self.media numberOfDisplayedPictures];
    self.seekToZeroTimer = [NSTimer scheduledTimerWithTimeInterval:3 repeats:NO block:^(NSTimer *timer) {
        if([self.media numberOfDisplayedPictures]==self.currentNumberOfDisplayedPictures){
            [[VideoPlayerVLC getInstance] sendVlcState:@"OnStuckPictures"];
            [self.mediaPlayer setPosition:MAX(0, roundf(self.seekbar.value / self.seekbar.maximumValue * 100) / 100 - 0.02)];
        }
    }];
}

- (void)screenTappedRequest:(UITapGestureRecognizer*)gesture {
    if([self.media numberOfDisplayedPictures] == 0) {
        return;
    }
    [self delayedPlayerControlsHide];
}

- (void)playBtnTappedRequest:(UITapGestureRecognizer*)gesture {
    if (!self.mediaPlayer.isPlaying) {
        [self.mediaPlayer play];
    } else {
        [self.mediaPlayer pause];
    }
}

- (void)pinchMediaView:(UIPinchGestureRecognizer*)gesture {
    CGAffineTransform transform = CGAffineTransformMakeScale(gesture.scale, gesture.scale);
    self.mediaView.transform = transform;
}

- (void)panMediaView:(UIPanGestureRecognizer*)gesture {
    self.mediaView.center = [gesture locationInView:self.mediaView.superview];
}

- (void) delayedDismissLoadingAnimation {
    if(self.loadingIndicatorDismissTimer) {
        [self.loadingIndicatorDismissTimer invalidate];
    }
    [self.indicatorView startAnimating];
    self.loadingIndicatorDismissTimer = [NSTimer scheduledTimerWithTimeInterval:0.6
                                                                         target:self
                                                                       selector:@selector(dismissLoadingIndicator:)
                                                                       userInfo:nil
                                                                        repeats:NO];
}

- (void)dismissLoadingIndicator:(NSTimer*)timer {
    if([self.media numberOfDisplayedPictures] != 0) {
        [self.indicatorView stopAnimating];
    } else {
        [self delayedDismissLoadingAnimation];
    }
}

- (void)delayedPlayerControlsHide {
    if(self.hideVideoControlsTimer) {
        [self.hideVideoControlsTimer invalidate];
    }
    self.playButtonView.alpha = 1;
    self.seekbarCurrent.alpha = 1;
    self.seekbar.alpha = 1;
    self.seekbarMax.alpha = 1;
    if (!self.mediaPlayer.isPlaying) {
        return;
    }
    self.hideVideoControlsTimer = [NSTimer scheduledTimerWithTimeInterval:3
                                                                         target:self
                                                                       selector:@selector(hidePlayerControls:)
                                                                       userInfo:nil
                                                                        repeats:NO];
}

- (void)hidePlayerControls:(NSTimer*)timer {
    self.playButtonView.alpha = 0;
    self.seekbarCurrent.alpha = 0;
    self.seekbar.alpha = 0;
    self.seekbarMax.alpha = 0;
}

-(void) applyMediaViewPortraitConstraints {
    [self.subView
     removeConstraint: mediaViewTopConstraint];
    [self.view
     removeConstraint: mediaViewBottomConstraint ];
    [self.subView addConstraint:
     mediaViewWidthConstraint
     ];
}

-(void) applyMediaViewLandscapeConstraints {
    [self.subView
     removeConstraint: mediaViewWidthConstraint];
    [self.subView addConstraint:
     mediaViewTopConstraint];
    [self.view addConstraint: mediaViewBottomConstraint];
}

-(void)createMediaViewConstraints {
    mediaViewWidthConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView
                                                            attribute:NSLayoutAttributeWidth
                                                            relatedBy:NSLayoutRelationEqual
                                                               toItem:self.subView
                                                            attribute:NSLayoutAttributeWidth
                                                           multiplier:1.0
                                                             constant:0];
    mediaViewHeightConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView
                                                             attribute:NSLayoutAttributeHeight
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.mediaView
                                                             attribute:NSLayoutAttributeWidth
                                                            multiplier:(9.0 / 16.0)
                                                              constant:0];
    mediaViewCenterHorizontallyConstraint =
    [NSLayoutConstraint constraintWithItem:self.mediaView
                                 attribute:NSLayoutAttributeCenterX
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.subView
                                 attribute:NSLayoutAttributeCenterX
                                multiplier:1.0
                                  constant:0];
    mediaViewCenterVertiacllyConstraint =
    [NSLayoutConstraint constraintWithItem:self.mediaView
                                 attribute:NSLayoutAttributeCenterY
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.subView
                                 attribute:NSLayoutAttributeCenterY
                                multiplier:1.0
                                  constant:0];
    mediaViewTopConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView
                                                          attribute:NSLayoutAttributeTop
                                                          relatedBy:NSLayoutRelationEqual
                                                             toItem:self.subView
                                                          attribute:NSLayoutAttributeTop
                                                         multiplier:1.0
                                                           constant:0];
    mediaViewBottomConstraint = [NSLayoutConstraint constraintWithItem:self.mediaView
                                                             attribute:NSLayoutAttributeBottom
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.view
                                                             attribute:NSLayoutAttributeBottom
                                                            multiplier:1.0
                                                              constant:0];
    
    [self.subView addConstraint:mediaViewHeightConstraint];
    [self.subView addConstraint:mediaViewCenterHorizontallyConstraint];
    [self.subView addConstraint:mediaViewCenterVertiacllyConstraint];
}

-(void)createPlayBtnConstraints {
    playBtnHeightConstraint = [NSLayoutConstraint constraintWithItem:self.playButtonView
                                                    attribute:NSLayoutAttributeHeight
                                                    relatedBy:NSLayoutRelationEqual
                                                    toItem:nil
                                                    attribute:NSLayoutAttributeNotAnAttribute
                                                    multiplier:1
                                                    constant:52];
    playBtnAspectConstraint =
    [NSLayoutConstraint constraintWithItem:self.playButtonView
                                 attribute:NSLayoutAttributeWidth
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.playButtonView
                                 attribute:NSLayoutAttributeHeight
                                multiplier:self.playButtonView.image.size.width /
     self.playButtonView.image.size.height
                                  constant:0];
    playBtnCenterHorizontallyConstraint =
    [NSLayoutConstraint constraintWithItem:self.playButtonView
                                 attribute:NSLayoutAttributeCenterX
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.subView
                                 attribute:NSLayoutAttributeCenterX
                                multiplier:1.0
                                  constant:0];
    playBtnCenterVertiacllyConstraint =
    [NSLayoutConstraint constraintWithItem:self.playButtonView
                                 attribute:NSLayoutAttributeCenterY
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.subView
                                 attribute:NSLayoutAttributeCenterY
                                multiplier:1.0
                                  constant:0];
    
    [self.subView addConstraint:playBtnHeightConstraint];
    [self.subView addConstraint:playBtnAspectConstraint];
    [self.subView addConstraint:playBtnCenterHorizontallyConstraint];
    [self.subView addConstraint:playBtnCenterVertiacllyConstraint];
}

-(void) createCloseButtonConstraints {
    closeButtonHeightConstraint =
    [NSLayoutConstraint constraintWithItem:self.closeButtonView
                                 attribute:NSLayoutAttributeHeight
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:nil
                                 attribute:NSLayoutAttributeNotAnAttribute
                                multiplier:1.0
                                  constant:40.0];
    closeButtonAspectConstraint =
    [NSLayoutConstraint constraintWithItem:self.closeButtonView
                                 attribute:NSLayoutAttributeWidth
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.closeButtonView
                                 attribute:NSLayoutAttributeHeight
                                multiplier:self.closeButtonView.image.size.width /
     self.closeButtonView.image.size.height
                                  constant:0];
    closeButtonTopConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView
                                                            attribute:NSLayoutAttributeTop
                                                            relatedBy:NSLayoutRelationEqual
                                                               toItem:self.subView
                                                            attribute:NSLayoutAttributeTop
                                                           multiplier:1.0
                                                             constant:16.0];
    closeButtonLeftConstraint = [NSLayoutConstraint constraintWithItem:self.closeButtonView
                                                             attribute:NSLayoutAttributeLeft
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.mediaView
                                                             attribute:NSLayoutAttributeLeft
                                                            multiplier:1.0
                                                              constant:8.0];
    [self.subView addConstraint:closeButtonHeightConstraint];
    [self.subView addConstraint:closeButtonAspectConstraint];
    [self.subView addConstraint:closeButtonTopConstraint];
    [self.subView addConstraint:closeButtonLeftConstraint];
}

-(void)createLoadingIndicatorConstraints{
    NSLayoutConstraint *indicatorHorizontallyConstraint =
    [NSLayoutConstraint constraintWithItem:self.indicatorView
                                 attribute:NSLayoutAttributeCenterX
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeCenterX
                                multiplier:1.0
                                  constant:0];
    NSLayoutConstraint *indicatorverticallyConstraint =
    [NSLayoutConstraint constraintWithItem:self.indicatorView
                                 attribute:NSLayoutAttributeCenterY
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeCenterY
                                multiplier:1.0
                                  constant:0];
    [self.subView addConstraint:indicatorHorizontallyConstraint];
    [self.subView addConstraint:indicatorverticallyConstraint];
}

-(void)createSeekbarConstraints{
    NSLayoutConstraint *labelStartBottom =
    [NSLayoutConstraint constraintWithItem:self.seekbarCurrent
                                 attribute:NSLayoutAttributeBottom
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeBottom
                                multiplier:1.0
                                  constant:-24];
    
    NSLayoutConstraint *labelStartLeft =
    [NSLayoutConstraint constraintWithItem:self.seekbarCurrent
                                 attribute:NSLayoutAttributeLeft
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeLeft
                                multiplier:1.0
                                  constant:8];
    
    NSLayoutConstraint *labelEndBottom =
    [NSLayoutConstraint constraintWithItem:self.seekbarMax
                                 attribute:NSLayoutAttributeBottom
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeBottom
                                multiplier:1.0
                                  constant:-24];
    
    NSLayoutConstraint *labelEndRight =
    [NSLayoutConstraint constraintWithItem:self.seekbarMax
                                 attribute:NSLayoutAttributeTrailing
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeTrailing
                                multiplier:1.0
                                  constant:-8];

    NSLayoutConstraint *bottomConstraint =
    [NSLayoutConstraint constraintWithItem:self.seekbar
                                 attribute:NSLayoutAttributeBottom
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.mediaView
                                 attribute:NSLayoutAttributeBottom
                                multiplier:1.0
                                  constant:-24];
    NSLayoutConstraint *leftConstraint =
    [NSLayoutConstraint constraintWithItem:self.seekbar
                                 attribute:NSLayoutAttributeLeft
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.seekbarCurrent
                                 attribute:NSLayoutAttributeRight
                                multiplier:1.0
                                  constant:8];
    
    NSLayoutConstraint *rightConstraint =
    [NSLayoutConstraint constraintWithItem:self.seekbar
                                 attribute:NSLayoutAttributeTrailing
                                 relatedBy:NSLayoutRelationEqual
                                    toItem:self.seekbarMax
                                 attribute:NSLayoutAttributeLeading
                                multiplier:1.0
                                  constant:-8];

    [self.subView addConstraint:labelStartBottom];
    [self.subView addConstraint:labelStartLeft];
    [self.subView addConstraint:labelEndBottom];
    [self.subView addConstraint:labelEndRight];
    [self.subView addConstraint:bottomConstraint];
    [self.subView addConstraint:leftConstraint];
    [self.subView addConstraint:rightConstraint];
}


- (UIColor*)colorFromHex:(NSString*)hexColor {
    unsigned rgbValue = 0;
    NSScanner* scanner = [NSScanner scannerWithString:hexColor];
    [scanner setScanLocation:1];
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0XFF0000) >> 16) / 255.0
                           green:((rgbValue & 0XFF00) >> 8) / 255.0
                            blue:(rgbValue & 0XFF) / 255.0
                           alpha:1.0];
}

- (UIImage *)imageWithImage:(UIImage *)image scaledToFillSize:(CGSize)size
{
    CGFloat scale = MAX(size.width/image.size.width, size.height/image.size.height);
    CGFloat width = image.size.width * scale;
    CGFloat height = image.size.height * scale;
    CGRect imageRect = CGRectMake((size.width - width)/2.0f,
                                  (size.height - height)/2.0f,
                                  width,
                                  height);

    UIGraphicsBeginImageContextWithOptions(size, NO, 0);
    [image drawInRect:imageRect];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

@end
