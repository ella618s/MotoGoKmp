#import <MapKit/MapKit.h>
#import <UIKit/UIKit.h>

@interface MapBridge : NSObject <MKMapViewDelegate>
@property (nonatomic, copy) void (^onMarkerSelected)(NSString *title);
@end

@implementation MapBridge

- (void)mapView:(MKMapView *)mapView didSelectAnnotationView:(MKAnnotationView *)view {
    if (view.annotation.title) {
        NSString *title = view.annotation.title;
        if (title && self.onMarkerSelected) {
            self.onMarkerSelected(title);
        }
    }
}

// 🎯 把原本讓 Kotlin 崩潰的 render 邏輯移回 Obj-C 處理
- (MKOverlayRenderer *)mapView:(MKMapView *)mapView rendererForOverlay:(id<MKOverlay>)overlay {
    if ([overlay isKindOfClass:[MKPolyline class]]) {
        MKPolylineRenderer *renderer = [[MKPolylineRenderer alloc] initWithPolyline:(MKPolyline *)overlay];
        renderer.strokeColor = [UIColor systemBlueColor];
        renderer.lineWidth = 6.0;
        return renderer;
    }
    return [[MKOverlayRenderer alloc] initWithOverlay:overlay];
}

@end