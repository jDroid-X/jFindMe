using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;

/// <summary>
/// ⚡ STEP 5 EXPANSION: Profile Screen Integration (Render Texture Bridge)
/// 
/// This script creates an isolated Sandbox Camera that renders a 3D Avatar exclusively 
/// for the Android Profile UI. It prevents the 3D World layer from bleeding into the 
/// 2D native Android XML view, avoiding massive depth-sorting or clipping bugs.
/// </summary>
public class AvatarProfileRenderer : MonoBehaviour
{
    [Header("Rendering Components")]
    public Camera profileCamera;
    public GameObject avatarContainer;

    [Header("Texture Constraints")]
    public int captureWidth = 512;
    public int captureHeight = 720; // Portrait Mobile Ratio

    private RenderTexture activeRenderTexture;

    void Awake()
    {
        // QA Logic: Automatically generate a clean render target
        activeRenderTexture = new RenderTexture(captureWidth, captureHeight, 24);
        activeRenderTexture.antiAliasing = 4;
        activeRenderTexture.format = RenderTextureFormat.ARGB32;

        // Strip UI/Map layers. We strictly render the Avatar Layer (e.g. Layer 8)
        profileCamera.cullingMask = 1 << LayerMask.NameToLayer("AvatarProfileLayer");
        profileCamera.clearFlags = CameraClearFlags.SolidColor;
        
        // Transparent background so Android handles the Hex color styling
        profileCamera.backgroundColor = new Color(0, 0, 0, 0); 
        profileCamera.targetTexture = activeRenderTexture;
    }

    /// <summary>
    /// JNI Call from Android (ProfileActivity.java): 
    /// "Hey Unity, capture the current frame of the 3D Avatar and send it back to me as a Image."
    /// </summary>
    public void RequestProfileSnapshot()
    {
        StartCoroutine(CaptureAndSendRoutine());
    }

    private IEnumerator CaptureAndSendRoutine()
    {
        // Wait for the end of the physics and animation frame
        yield return new WaitForEndOfFrame();

        // Standardize the active texture
        RenderTexture.active = profileCamera.targetTexture;

        // Render the camera view manually right now
        profileCamera.Render();

        // Read physical GPU pixels into a CPU Texture2D
        Texture2D texture2D = new Texture2D(captureWidth, captureHeight, TextureFormat.RGBA32, false);
        texture2D.ReadPixels(new Rect(0, 0, captureWidth, captureHeight), 0, 0);
        texture2D.Apply();

        // Free the GPU memory lock
        RenderTexture.active = null;

        // Compress to PNG Byte Array
        byte[] imageBytes = texture2D.EncodeToPNG();

        // Convert to Base64 String to fire across the narrow JNI string bridge
        string base64Payload = Convert.ToBase64String(imageBytes);

        // Memory Management: Destroy the CPU Texture immediately
        Destroy(texture2D);

        // Fire back to Native Android Listener (e.g., ProfileActivity)
        SendSnapshotToAndroid(base64Payload);
    }

    private void SendSnapshotToAndroid(string base64Image)
    {
        try
        {
            // Unity -> Android Broadcast via pre-compiled plugin
            // Maps exactly to the dynamic Profile UI we built earlier!
            using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                using (AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                {
                    currentActivity.Call("onUnityProfileSnapshotReceived", base64Image);
                }
            }
        }
        catch (Exception e)
        {
            Debug.LogError("Error delivering Native Android JNI call: " + e.Message);
        }
    }

    /// <summary>
    /// Swipes/Drag Events on Android's Profile Screen pipe directly here 
    /// to dynamically rotate the 3D model.
    /// </summary>
    public void RotateAvatarFromAndroidTouch(float rotationDeltaAmount)
    {
        avatarContainer.transform.Rotate(Vector3.up, -rotationDeltaAmount * 15f * Time.deltaTime, Space.World);
    }
}
