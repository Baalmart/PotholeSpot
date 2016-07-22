package dev.potholespot.android.viewer.map.overlay;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.maps.MapView;
import com.mapquest.android.maps.Overlay;


public interface GoogleOverlayProvider
{
   public com.google.android.maps.Overlay getGoogleOverlay();
   public org.osmdroid.views.overlay.Overlay getOSMOverlay();
   public Overlay getMapQuestOverlay();
   boolean onTap(LatLng tappedGeoPoint, MapView mapview);
}
