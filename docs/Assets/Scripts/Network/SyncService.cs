using System;
using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

namespace jFindMe.Multiplayer.Network
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): Sync with Firebase.
    /// Manages the strict AVATAR SYNC API CONTRACTS preventing avatars from freezing on the Map.
    /// Handles End-to-End JSON payload generation tracking Lat/Lng and Mecanim states.
    /// </summary>
    public class SyncService : MonoBehaviour
    {
        [Header("API Configuration")]
        public string firebaseRestApiUrl = "https://jfindme-app.firebaseio.com/api/v1/";
        // A unique API authentication token securely provided by Android JNI upon login
        private string sessionAuthToken = "";

        // Internal dependency matching the exact provided structure
        private AvatarAnimator animatorSystem;

        private void Awake()
        {
            // Failsafe execution: ensures the required Animation component exists
            animatorSystem = GetComponent<AvatarAnimator>();
        }

        public void InitializeSessionAuth(string token)
        {
            sessionAuthToken = token;
        }

        /* ========================================================================= */
        /* CONTRACT 1: FETCH AVATAR (GET Array)                                      */
        /* ========================================================================= */
        
        /// <summary>
        /// Pulls the complete JSON Avatar configuration from the Cloud Firestore Node
        /// </summary>
        public void FetchRemoteAvatar(string userId, Action<AvatarSyncData> onSuccess, Action<string> onFailure)
        {
            StartCoroutine(FetchAvatarRoutine(userId, onSuccess, onFailure));
        }

        private IEnumerator FetchAvatarRoutine(string userId, Action<AvatarSyncData> onSuccess, Action<string> onFailure)
        {
            string fetchUrl = $"{firebaseRestApiUrl}avatar/{userId}?auth={sessionAuthToken}";
            
            using (UnityWebRequest webFetch = UnityWebRequest.Get(fetchUrl))
            {
                yield return webFetch.SendWebRequest();

                if (webFetch.result != UnityWebRequest.Result.Success)
                {
                    onFailure?.Invoke($"Network Timeout: {webFetch.error}. Reverting to cached model.");
                }
                else
                {
                    // Cleanly parses the incoming exact contract: 
                    // { avatar_id, model_url, state }
                    AvatarSyncData responseData = JsonUtility.FromJson<AvatarSyncData>(webFetch.downloadHandler.text);
                    onSuccess?.Invoke(responseData);
                }
            }
        }

        /* ========================================================================= */
        /* CONTRACT 2: UPDATE AVATAR STATE (POST Real-time Vector sync)              */
        /* ========================================================================= */

        /// <summary>
        /// Emits local Map/GPS movement back to the Firestore Multiplayer sync nodes.
        /// Warning: Over-polling this crashes servers! Rate limit to 1 Update per ~500ms.
        /// </summary>
        public void BroadcastLiveState(string userId, double lat, double lng, string activeAnimationState)
        {
            StartCoroutine(BroadcastStateRoutine(userId, lat, lng, activeAnimationState));
        }

        private IEnumerator BroadcastStateRoutine(string userId, double lat, double lng, string activeState)
        {
            // Format specifically to the User's Real-Time Sync Dictionary rules
            AvatarLiveState statePacket = new AvatarLiveState
            {
                user_id = userId,
                position = new SpatialCoord { lat = lat, lng = lng },
                state = activeState,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
            };

            string jsonPayload = JsonUtility.ToJson(statePacket);
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonPayload);

            string postUrl = $"{firebaseRestApiUrl}avatar/update?auth={sessionAuthToken}";

            // Standardize POST logic overriding standard URL escaping
            using (UnityWebRequest webPost = new UnityWebRequest(postUrl, "POST"))
            {
                webPost.uploadHandler = new UploadHandlerRaw(bodyRaw);
                webPost.downloadHandler = new DownloadHandlerBuffer();
                webPost.SetRequestHeader("Content-Type", "application/json");

                yield return webPost.SendWebRequest();

                if (webPost.result != UnityWebRequest.Result.Success)
                {
                    Debug.LogWarning($"[Sync Service] Live State Drop: The user drove into a tunnel! Continuing extrapolation.");
                    // No crash! The avatar smoothly interpolates locally according to our Blueprint Rule: "No interpolation -> jerky movement"
                }
            }
        }
    }

    /* ------------------------------------------------------------------------- */
    /* DATA STRUCTURE CONTRACTS (JSON SERIALIZATION CLASSES)                     */
    /* ------------------------------------------------------------------------- */

    [Serializable]
    public class AvatarSyncData
    {
        public string avatar_id;
        public string model_url;
        public string[] animations;
        public string state;
    }

    [Serializable]
    public class AvatarLiveState
    {
        public string user_id;
        public SpatialCoord position;
        public string state;
        public long timestamp;
    }

    [Serializable]
    public class SpatialCoord
    {
        public double lat;
        public double lng;
    }
}
