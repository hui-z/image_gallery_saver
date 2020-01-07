#import "ImageGallerySaverPlugin.h"

@implementation ImageGallerySaverPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
	FlutterMethodChannel* channel = [FlutterMethodChannel
									 methodChannelWithName:@"image_gallery_saver"
									 binaryMessenger:[registrar messenger]];
	ImageGallerySaverPlugin* instance = [[ImageGallerySaverPlugin alloc] init];
	[registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
	resultBack = result;
    if ([@"saveImageToGallery" isEqualToString:call.method]) {
        FlutterStandardTypedData *data = call.arguments;
        UIImage *image=[UIImage imageWithData:data.data];
		
		UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), (__bridge void *)self);
    } else if ([@"saveFileToGallery" isEqualToString:call.method]) {
		NSString *path = call.arguments;
		if([isImageFile:path]){
			UIImage *image=[UIImage imageNamed:path];
			UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), (__bridge void *)self);
		}else{
			if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(path)) {
                UISaveVideoAtPathToSavedPhotosAlbum(path, self, @selector(video:didFinishSavingWithError:contextInfo:), nil);
            }
		}
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (BOOL)isImageFile:(NSString*)filename {
	NSString *extensionName = filename.pathExtension;
	if ([extensionName.lowercaseString isEqualToString:@"jpg"]
		||[extensionName.lowercaseString isEqualToString:@"png"]
		||[extensionName.lowercaseString isEqualToString:@"JPEG"]
		||[extensionName.lowercaseString isEqualToString:@"JPG"]
		||[extensionName.lowercaseString isEqualToString:@"PNG"]) {
		return true;
	}
	return false;
}

- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo
{
    NSLog(@"image = %@, error = %@, contextInfo = %@", image, error, contextInfo);
    resultBack(@"fail");
}

- (void)video:(NSString *)videoPath didFinishSavingWithError:(NSError *)error
                                                 contextInfo:(void *)contextInfo {
    NSLog(@"保存视频完成");
    resultBack(@"fail");
}
@end
