using UnityEngine;
using System;

namespace jFindMe.Multiplayer.Map
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): Real-world movement.
    /// Intercepts hyper-accurate location payloads from Android's FusedLocationProviderClient
    /// via the C++ JNI bridge, rather than relying on Unity's slower Input.location module.
    /// </summary>
    public class GPSService : MonoBehaviour
    {
        [Header("Live State Tracking")]
        public double currentLatitude = 0.0;
        public double currentLongitude = 0.0;
        public float currentSpeedMetersPerSec = 0f;
        
        [Header("Events")]
        public Action<double, double, float> OnLocationUpdated;

        /// <summary>
        /// This Method is strictly reserved for JNI calls from Android (MapActivity.java).
        /// Payload arrives as a delimited string to save bridge overhead: "lat|lng|speed"
        /// E.g. UpdateNativeLocationPayload("18.52|73.85|4.2")
        /// </summary>
        public void UpdateNativeLocationPayload(string jniPayload)
        {
            try
            {
                string[] data = jniPayload.Split('|');
                if (data.Length < 3) return;

                // Safely parse the raw strings into double precision math
                double parsedLat = double.Parse(data[0]);
                double parsedLng = double.Parse(data[1]);
                float parsedSpeed = float.Parse(data[2]);

                // Update strict state
                currentLatitude = parsedLat;
                currentLongitude = parsedLng;
                currentSpeedMetersPerSec = parsedSpeed;

                // Fire the event hook to anyone listening (e.g. LocationTracker.cs / GameStateManager.cs)
                OnLocationUpdated?.Invoke(currentLatitude, currentLongitude, currentSpeedMetersPerSec);
                
                Debug.Log($"[GPS Service] Native Android Vector Sync: {parsedLat}, {parsedLng}");
            }
            catch (Exception ex)
            {
                Debug.LogError($"[GPS Service Fatal] Failed parsing Native JNI payload: {ex.Message}");
            }
        }
    }
}
