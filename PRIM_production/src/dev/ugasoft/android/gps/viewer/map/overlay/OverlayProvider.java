package dev.ugasoft.android.gps.viewer.map.overlay;

import com.mapquest.android.maps.Overlay;


public interface OverlayProvider
{
   public com.google.android.maps.Overlay getGoogleOverlay();
   public org.osmdroid.views.overlay.Overlay getOSMOverlay();
   public Overlay getMapQuestOverlay();
}
