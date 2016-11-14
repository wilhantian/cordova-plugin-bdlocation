/*****************************************
 * Create by WilhanTian - BDLocation
 *****************************************/
#import <Cordova/CDV.h>

#import <BaiduMapAPI_Base/BMKBaseComponent.h>
#import <BaiduMapAPI_Location/BMKLocationComponent.h>

@interface BDLocation : CDVPlugin<BMKLocationServiceDelegate> {
    BMKLocationService* _locService;
    CDVInvokedUrlCommand* _watchCommand;
}

- (void)watch:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;

- (void)didUpdateUserHeading:(BMKUserLocation *)userLocation;
- (void)didUpdateBMKUserLocation:(BMKUserLocation *)userLocation;

@end