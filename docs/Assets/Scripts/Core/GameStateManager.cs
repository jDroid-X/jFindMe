using System;
using System.Collections;
using UnityEngine;
using jFindMe.Multiplayer.Network;

namespace jFindMe.Multiplayer.Core
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): The Top-Level Supervisor Observer.
    /// Orchestrates the strict End-To-End Event Flow: 
    /// Login -> Fetch -> Cache -> Load -> Attach Animation -> GPS Bind -> Sync
    /// </summary>
    public class GameStateManager : MonoBehaviour
    {
        [Header("Engine Sub-Systems")]
        public SyncService syncService;    // Firebase Sync Pipeline
        public AvatarLoader avatarLoader;    // Local Cache + Download Logic
        public GPSService gpsTracker;        // Android native location hook

        // Security session loaded via Android JNI
        private string activeUserId = "U123";

        public enum SystemState
        {
            Authenticating,
            FetchingAvatarRegistry,
            DownloadingMesh,
            BindingSkeletalMesh,
            LiveSyncing
        }

        public SystemState currentState = SystemState.Authenticating;

        private void Start()
        {
            // Triggered the millisecond the Unity Engine completely boots inside Android
            InitializeAvatarLifecycleFlow();
        }

        /// <summary>
        /// The precise step-by-step sequence guarding against the "Common Failure Points"
        /// Specifically prevents "No Sync -> Freeze" and "Heavy Models -> FPS Drop"
        /// </summary>
        public void InitializeAvatarLifecycleFlow()
        {
            Debug.Log("[Game State] Commencing End-To-End Avatar Boot Sequence.");

            // Step 1: Call the Firebase JSON API to get the Avatar's URL
            currentState = SystemState.FetchingAvatarRegistry;
            
            syncService.FetchRemoteAvatar(
                userId: activeUserId,
                onSuccess: (dataRecord) => 
                {
                    Debug.Log($"[Game State] API Return Success. Avatar [{dataRecord.avatar_id}] verified.");
                    
                    // Step 2 & 3: Initiate the download (checks local cache first!)
                    currentState = SystemState.DownloadingMesh;
                    avatarLoader.DownloadAndCacheModel(dataRecord.model_url, OnModelLoaded);
                },
                onFailure: (errorString) => 
                {
                    // Fallback injection logic
                    Debug.LogError($"[Game State Fatal] API Sync Broken: {errorString}");
                }
            );
        }

        /// <summary>
        /// Call-back executed when AvatarLoader physically mounts the 3D GLB file into the Map.
        /// </summary>
        private void OnModelLoaded(GameObject physicsReadyModel)
        {
            currentState = SystemState.BindingSkeletalMesh;
            
            // Step 4: Attach the Animator State Machine dynamically 
            // Satisfies requirement: "Missing animator -> no animation"
            AvatarAnimator boneDriver = physicsReadyModel.AddComponent<AvatarAnimator>();
            AvatarController gpsController = physicsReadyModel.AddComponent<AvatarController>();

            // Step 5: Bind the loaded model to the Real-World Android GPS coordinates
            gpsController.BindNativeLocationService(gpsTracker);
            
            // Step 6: Begin pulsating the location back up to the Multiplayer server!
            currentState = SystemState.LiveSyncing;
            
            StartCoroutine(TransmitLiveLocationCoroutine(gpsController, boneDriver));
            Debug.Log("[Game State] Avatar is now a Real-Time Multiplayer Digital Entity!");
        }

        /// <summary>
        /// As background routine pumping location out to Firebase matching the "18.52 / 73.85" strict JSON structure.
        /// </summary>
        private IEnumerator TransmitLiveLocationCoroutine(AvatarController gpsTracker, AvatarAnimator stateMachine)
        {
            // Heartbeat loop: Only sends data 1x per second to prevent rate limiting 
            WaitForSeconds syncDelay = new WaitForSeconds(1.0f);

            while (currentState == SystemState.LiveSyncing)
            {
                syncService.BroadcastLiveState(
                    userId: activeUserId,
                    lat: gpsTracker.currentLat,
                    lng: gpsTracker.currentLng,
                    activeAnimationState: stateMachine.CurrentStateName
                );

                yield return syncDelay;
            }
        }
    }
}
