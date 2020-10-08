//
//  VideoPlayerVLC.m
//
//
//  Created by Yanbing Peng on 10/02/16.
//  Edited by Yossi Neiman
//
//
#import "VideoPlayerVLC.h"

@implementation VideoPlayerVLC

static VideoPlayerVLC* instance = nil;
static CDVInvokedUrlCommand* commandGlobPlay = nil;
static CDVInvokedUrlCommand* commandGlobExternalData = nil;

+ (id)getInstance
{
    return instance;
}

- (void)play:(CDVInvokedUrlCommand*)command
{
    
    instance = self;
    commandGlobPlay = command;
    if (self.player != nil) {
        self.player = nil;
    }
    
    CDVPluginResult* pluginResult = nil;
    NSString* urlString = [command.arguments objectAtIndex:0];
    
    if (urlString != nil) {
        @try {
            if (self.player == nil) {
                self.player = [[VideoPlayerVLCViewController alloc] init];
            }
            
            self.player.urlString = urlString;
            
            [self.viewController addChildViewController:self.player];
            [self.webView.superview insertSubview:self.player.view
                                     aboveSubview:self.webView];
            
        } @catch (NSException* exception) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:exception.reason];
            [pluginResult setKeepCallback:@YES];
            [self.commandDelegate sendPluginResult:pluginResult
                                        callbackId:command.callbackId];
        }
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:@"url-invalid"];
        [self.commandDelegate sendPluginResult:pluginResult
                                    callbackId:command.callbackId];
    }
}

- (void)receiveExternalData:(CDVInvokedUrlCommand*)command
{
    
    NSString* externalCommandString = [command.arguments objectAtIndex:0];
    if (externalCommandString != nil) {
        if ([externalCommandString isEqualToString:@"set_external_callback"]) {
            commandGlobExternalData = command;
            [self sendExternalData:@"set_external_callback"];
            return;
        } else {
            NSError* err = nil;
            NSMutableDictionary* JSONObj = [NSJSONSerialization
                                            JSONObjectWithData:[externalCommandString
                                                                dataUsingEncoding:NSUTF8StringEncoding]
                                            options:0
                                            error:&err];
            if (err == nil) {
                NSString* type = [JSONObj valueForKey:@"type"];
                if (type != nil) {
                    if ([type isEqualToString:@"webview_show_ptz_buttons"]) {
                        if([JSONObj[@"value"] boolValue]) {
                            [self.player setJoystickButtonViewEnabled];
                        }
                        return;
                    } else if ([type isEqualToString:@"webview_show_recording_button"]) {
                        BOOL value = [JSONObj[@"value"] boolValue];
                        // TODO: call method to show/hide recording buton
                        return;
                    } else if ([type isEqualToString:@"webview_update_rec_status"]) {
                        BOOL value = [JSONObj[@"value"] boolValue];
                        [self.player recordingStatusReceived:value];
                        return;
                    } else if ([type isEqualToString:@"webview_elements_visibility"]) {
                        BOOL value = [JSONObj[@"value"] boolValue];
                        [self.player elementsVisibilityRequest:value];
                        return;
                    } else if ([type isEqualToString:@"webview_set_translations"]) {
                        NSMutableDictionary* valueObj = [NSJSONSerialization
                                                         JSONObjectWithData:[JSONObj[@"value"]
                                                                             dataUsingEncoding:NSUTF8StringEncoding]
                                                         options:0
                                                         error:&err];
                        if (err == nil) {
                            NSString* liveIndicator =
                            [valueObj valueForKey:@"live_indicator"];
                            NSString* recNotification =
                            [valueObj valueForKey:@"rec_notification"];
                            [self.player setTranslations:liveIndicator
                                         recNotification:recNotification];
                        }
                        return;
                    }
                }
            }
        }
    }
    NSMutableDictionary* errorResult = [[NSMutableDictionary alloc] init];
    [errorResult setValue:@"error" forKey:@"type"];
    [errorResult setValue:@"error_parsing_request" forKey:@"value"];
    [self sendExternalDataAsDictionary:errorResult];
}

- (void)stop:(CDVInvokedUrlCommand*)command
{
    if(self.player != nil) {
        [self.player stop];
    }
}

- (void)sendVlcState:(NSString*)event
{
    CDVPluginResult* pluginResult = nil;
    
    @try {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                         messageAsString:event];
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:exception.reason];
    }
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:commandGlobPlay.callbackId];
}

- (void)sendExternalData:(NSString*)data
{
    
    if (commandGlobExternalData == nil) {
        return;
    }
    
    CDVPluginResult* pluginResult = nil;
    
    @try {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                         messageAsString:data];
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:exception.reason];
    }
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:commandGlobExternalData.callbackId];
}

- (void)sendExternalDataAsDictionary:(NSMutableDictionary*)data
{
    
    if (commandGlobExternalData == nil) {
        return;
    }
    
    CDVPluginResult* pluginResult = nil;
    
    @try {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                     messageAsDictionary:data];
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:exception.reason];
    }
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:commandGlobExternalData.callbackId];
}

- (void)stopInner
{
    
    CDVPluginResult* pluginResult = nil;
    if (self.player != nil) {
        @try {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                             messageAsString:@"onDestroyVlc"];
        } @catch (NSException* exception) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:exception.reason];
        }
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:@"not-playing"];
    }
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:commandGlobPlay.callbackId];
}

@end
