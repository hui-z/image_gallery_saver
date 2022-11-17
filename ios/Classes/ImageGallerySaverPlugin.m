#import "ImageGallerySaverPlugin.h"

#if __has_include(<image_gallery_saver/image_gallery_saver-Swift.h>)
#import <image_gallery_saver/image_gallery_saver-Swift.h>
#else
#import "image_gallery_saver-Swift.h"
#endif

@implementation ImageGallerySaverPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    if (registrar) {
        [SwiftImageGallerySaverPlugin registerWithRegistrar:registrar];
    }
}
@end
