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
                UISaveVideoAtPathToSavedPhotosAlbum(urlStr, self, @selector(video:didFinishSavingWithError:contextInfo:), nil);
            }
		}
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (BOOL)isImageFile:(NSString*)filename {
	NSString *extensionName = path.pathExtension;
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


#pragma mark --- 图片保存完成后，调用的回调方法:
- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo
{
    // Was there an error?
    if (error != NULL)
    {
        // Show error message…
        NSLog(@"保存照片过程中发生错误，错误信息:%@",error.localizedDescription);
    }
    else  // No errors
    {
        // Show message image successfully saved
        NSLog(@"Save image successful");
    }
	
	resultBack(@"fail");
}
@end
