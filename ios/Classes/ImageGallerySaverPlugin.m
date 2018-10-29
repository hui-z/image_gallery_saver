#import "ImageGallerySaverPlugin.h"
#import <image_gallery_saver/image_gallery_saver-Swift.h>

@implementation ImageGallerySaverPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftImageGallerySaverPlugin registerWithRegistrar:registrar];
}
@end
