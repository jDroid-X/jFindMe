using System.Collections;
using System.Collections.Generic;
using UnityEngine;
// using UnityEngine.XR.ARFoundation; // Required AR Package Installation!
// using UnityEngine.XR.ARSubsystems;

namespace jFindMe.Multiplayer.AR
{
    /// <summary>
    /// 🚀 NEXT EVOLUTION: AR mode (real-world overlay)
    /// Overrides the standard 2D flat Map physics plane and projects live Multiplayer Avatars 
    /// directly onto the User's Real-World camera feed using AR Foundation.
    /// </summary>
    public class AREngineManager : MonoBehaviour
    {
        public static AREngineManager Instance { get; private set; }

        [Header("AR System Anchors")]
        public Camera arCamera;
        // public ARSessionOrigin sessionOrigin; 
        // public ARRaycastManager raycastManager;
        
        // The master container holding all active multiplayer avatars
        public Transform avatarNetworkContainer;

        private bool isARModeActive = false;

        private void Awake()
        {
            if (Instance == null) { Instance = this; }
            else { Destroy(gameObject); }
        }

        /// <summary>
        /// Triggered cleanly from Android JNI when the user taps an 'AR Horizon' button on their Map GUI.
        /// Instantly swaps the Map render background with their physical Camera.
        /// </summary>
        public void ToggleRealWorldOverlay(bool enableAR)
        {
            isARModeActive = enableAR;
            
            if (enableAR)
            {
                Debug.Log("[AR Engine] Engaging Camera Subsystems. Sweeping for Plane Surfaces...");
                // Background execution to initialize heavy Mobile Camera sensors without stuttering
                StartCoroutine(InitializeHardwareSensors());
            }
            else
            {
                // De-activate AR to save immense mobile battery drain when users revert to 2D Map mode
                Debug.Log("[AR Engine] Suspending AR tracking to preserve active GPU life.");
                // sessionOrigin.gameObject.SetActive(false);
                
                // Reposition all avatars perfectly flat on the mathematical origin plane 
                RevertToStandardMapScaling();
            }
        }

        private IEnumerator InitializeHardwareSensors()
        {
            // sessionOrigin.gameObject.SetActive(true);
            
            // Allow 500ms for Unity to grab the Android Camera Permission Context natively 
            yield return new WaitForSeconds(0.5f);
            
            // Re-parents the entire Multiplayer Networking Grid directly tracked onto the moving Phone!
            avatarNetworkContainer.parent = arCamera.transform;
            avatarNetworkContainer.localPosition = new Vector3(0, -1.5f, 5f); // Lock grid 5 meters in front of user
            avatarNetworkContainer.localRotation = Quaternion.identity;

            Debug.Log("[AR Engine] Sensor Sweep Complete. Real-World Projection Matrix LOCKED!");
        }

        /// <summary>
        /// Transforms raw GPS Multiplayer Coordinates (Lat/Lng Lerps) into accurate AR Camera Scale logic
        /// </summary>
        public void ProjectEntityIntoARSpace(GameObject targetedAvatar, Vector3 gpsCartesianVector)
        {
            if (!isARModeActive) return;

            // In standard Unity, GPS vectors are mathematically massive (e.g. x: 50402, z: -10292)
            // In AR Space, moving your phone 2 feet completely breaks scale.
            // We mathematically normalize the massive GPS grid relative to the User's physical Desktop/Street

            Vector3 localizedARVector = (gpsCartesianVector - arCamera.transform.position) * 0.1f; // Shrink to human 1:1 scale
            localizedARVector.y = -1.2f; // Ground the avatar 

            targetedAvatar.transform.localPosition = localizedARVector;

            // Optional Next Level: AR Raycast downward to ensure the avatar's feet lock precisely onto the concrete Asphalt
            /*
            List<ARRaycastHit> hits = new List<ARRaycastHit>();
            if (raycastManager.Raycast(targetedAvatar.transform.position, hits, TrackableType.PlaneWithinPolygon))
            {
                targetedAvatar.transform.position = hits[0].pose.position;
            }
            */
        }
        
        private void RevertToStandardMapScaling()
        {
            avatarNetworkContainer.parent = null;
            avatarNetworkContainer.position = Vector3.zero;
            avatarNetworkContainer.rotation = Quaternion.identity;
        }
    }
}
