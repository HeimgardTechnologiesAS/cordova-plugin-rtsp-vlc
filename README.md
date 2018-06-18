# PhoneGap/Cordova RTSP player using VLC

### Supported Platform

iOS **7.0** up

Android

### Installation

```
cordova plugin add https://github.com/dride/cordova-plugin-rtsp-vlc
```

### Usage

```
window.PYB.vlcStreamPlayer.play(urlString).then(
	 done => {},
	 error => {}
);
```

For Instance:

```
window.PYB.vlcStreamPlayer.play("rtsp://192.168.42.1/live.mov").then(
	 done => {},
	 error => {}
);
```
