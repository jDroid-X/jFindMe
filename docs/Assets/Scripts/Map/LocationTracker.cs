using System;
using UnityEngine;

namespace jFindMe.Multiplayer.Map
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): Translate Map/GPS to Unity Physics.
    /// Handles the math converting real-world Earth Lat/Lng decimals (e.g. 18.52, 73.85) 
    /// into a flat Vector3(x,y,z) coordinate system for physical avatar rendering.
    /// </summary>
    public class LocationTracker : MonoBehaviour
    {
        [Header("Engine Hooks")]
        public GPSService gpsService;
        public float syncInterpolationSpeed = 10f; 

        // Current target spatial location calculated from GPS
        private Vector3 networkTargetPosition;
        private Quaternion networkTargetRotation;

        // Earth Origin Reference Point (0,0,0) used for Cartesian calculation offset
        private double rootOriginLat = 18.52;
        private double rootOriginLng = 73.85;

        private void OnEnable()
        {
            // Subscribe to the Android GPS Hook Events 
            if (gpsService != null)
                gpsService.OnLocationUpdated += HandleNewGpsCoordinate;
        }

        private void OnDisable()
        {
            if (gpsService != null)
                gpsService.OnLocationUpdated -= HandleNewGpsCoordinate;
        }

        /// <summary>
        /// Converts the decimal GPS into a flat Vector3 relative to the origin coordinate.
        /// </summary>
        private void HandleNewGpsCoordinate(double lat, double lng, float speed)
        {
            // 1. Core Map Translation Logic
            float zMeters = CalculateDistanceInMeters(rootOriginLat, rootOriginLng, lat, rootOriginLng);
            float xMeters = CalculateDistanceInMeters(rootOriginLat, rootOriginLng, rootOriginLat, lng);

            if (lat < rootOriginLat) zMeters = -zMeters;
            if (lng < rootOriginLng) xMeters = -xMeters;

            // 2. Lock the Target Cartesian Coordinates
            networkTargetPosition = new Vector3(xMeters, transform.position.y, zMeters);
        }

        private void Update()
        {
            // 3. Extrapolate position smoothly between Android network ticks
            // Blueprint Architecture Check: Prevents "No interpolation -> jerky movement"
            if (Vector3.Distance(transform.position, networkTargetPosition) > 0.1f)
            {
                transform.position = Vector3.Lerp(transform.position, networkTargetPosition, Time.deltaTime * syncInterpolationSpeed);
                
                // Automatically rotate avatar model toward the new walking vector
                Vector3 directionalVector = (networkTargetPosition - transform.position).normalized;
                if (directionalVector != Vector3.zero) 
                {
                    networkTargetRotation = Quaternion.LookRotation(directionalVector);
                    transform.rotation = Quaternion.Slerp(transform.rotation, networkTargetRotation, Time.deltaTime * syncInterpolationSpeed * 2f);
                }
            }
        }

        /* -------------------------------------------------------------------------- */
        /* MATHEMATICS UTILITY (Haversine Grid Approximation)                         */
        /* -------------------------------------------------------------------------- */
        private const double EARTH_RADIUS_METERS = 6378137.0;

        private float CalculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2)
        {
            var dLat = (lat2 - lat1) * Mathf.Deg2Rad;
            var dLon = (lon2 - lon1) * Mathf.Deg2Rad;
            var a = Mathf.Sin((float)dLat / 2f) * Mathf.Sin((float)dLat / 2f) +
                    Mathf.Cos((float)lat1 * Mathf.Deg2Rad) * Mathf.Cos((float)lat2 * Mathf.Deg2Rad) *
                    Mathf.Sin((float)dLon / 2f) * Mathf.Sin((float)dLon / 2f);
            var c = 2f * Mathf.Atan2(Mathf.Sqrt(a), Mathf.Sqrt(1f - a));
            
            return (float)(EARTH_RADIUS_METERS * c);
        }
    }
}
