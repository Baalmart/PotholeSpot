package dev.potholespot.android.viewer.map.overlay;
//import com.google.android.gms.maps.;

public interface OverlayProvider
{
  //public Overlay getGoogleOverlay();

   public org.osmdroid.views.overlay.Overlay getOSMOverlay();

   public com.mapquest.android.maps.Overlay getMapQuestOverlay();
}
