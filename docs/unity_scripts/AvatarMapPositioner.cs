using System.Collections;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// ⚡ STEP 4 EXPANSION: Map Integration (Spatial Binding)
/// 
/// Translates raw decimal GPS coordinates (Latitude/Longitude) streamed from the Android Firebase Tracker
/// into a flat-earth Cartesian Vector3 space for Unity rendering.
/// Features heavy Slerp interpolation to prevent models from rubber-banding during network lag.
/// </summary>
public class AvatarMapPositioner : MonoBehaviour
{
    [Header("Engine Hooks")]
    private AvatarAnimationController animController;
    private Rigidbody rb;

    [Header("Network Interpolation (Core Problem Solver)")]
    private Vector3 networkTargetPosition;
    private Quaternion networkTargetRotation;
    
    // The delay between Firebase ticks, usually 500ms - 1s on Mobile Data
    private float syncInterpolationSpeed = 10f; 
    
    // Unity's World Center (Maps coordinates relative to user's 0,0,0)
    public double originLat = 0.0;
    public double originLng = 0.0;

    void Awake()
    {
        animController = GetComponent<AvatarAnimationController>();
        rb = GetComponent<Rigidbody>();
        
        // Lock physics rotation to prevent avatars from falling over
        rb.constraints = RigidbodyConstraints.FreezeRotationX | RigidbodyConstraints.FreezeRotationZ;

        // Initialize targets to spawn point
        networkTargetPosition = transform.position;
        networkTargetRotation = transform.rotation;
    }

    /// <summary>
    /// Decodes the JSON broadcasted over the JNI Bridge from UnityFirebaseBridge.kt.
    /// Maps Lat/Lng into a relative floating grid point.
    /// </summary>
    public void ReceiveNetworkState(double lat, double lng, float headingDegrees, float speed)
    {
        // 1. Calculate relative distance from Origin in Meters
        float zPos = MetricConversion.CalculateDistanceInMeters(originLat, originLng, lat, originLng);
        float xPos = MetricConversion.CalculateDistanceInMeters(originLat, originLng, originLat, lng);

        // Determine quadrants
        if (lat < originLat) zPos = -zPos;
        if (lng < originLng) xPos = -xPos;

        // Set the interpolation target
        networkTargetPosition = new Vector3(xPos, transform.position.y, zPos);
        networkTargetRotation = Quaternion.Euler(0f, headingDegrees, 0f);

        // Tell the Animation Engine how fast to move the legs
        animController.UpdateSpatialVelocity(speed);
    }

    void Update()
    {
        // 2. Extrapolate smoothly between positions frame by frame (No network stuttering)
        // Lerp closes the distance between current and networkTarget.
        transform.position = Vector3.Lerp(transform.position, networkTargetPosition, Time.deltaTime * syncInterpolationSpeed);
        
        // Slerp smoothly rotates the character to face its new direction.
        transform.rotation = Quaternion.Slerp(transform.rotation, networkTargetRotation, Time.deltaTime * syncInterpolationSpeed);
    }
}

/// <summary>
/// Utility Class for Flat-Earth Haversine approximations.
/// </summary>
public static class MetricConversion
{
    private const double EARTH_RADIUS_METERS = 6378137.0;

    public static float CalculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2)
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
