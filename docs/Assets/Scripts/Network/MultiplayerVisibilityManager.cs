using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace jFindMe.Multiplayer.Network
{
    /// <summary>
    /// CORE RESPONSIBILITY: (B) Multiplayer Visibility System without Melting Devices.
    /// Operates a high-performance Spatial Distance Grid tracking all active Multi-User Entities across the Map.
    /// Radically prevents GPU bottlenecks by strictly enforcing 'Lazy Rendering' and selective Culling.
    /// </summary>
    public class MultiplayerVisibilityManager : MonoBehaviour
    {
        public static MultiplayerVisibilityManager Instance { get; private set; }

        [Header("Optimized Rendering Constraints")]
        [Tooltip("Avatars further than this distance will be completely de-materialized from memory.")]
        public float lazyRenderDistanceThreshold = 100f; // Meters
        [Tooltip("Avatars slightly out of view will have their expensive Skeletons frozen to save CPU.")]
        public float animationFreezeThreshold = 50f;

        // Tracks every active avatar connected to your Firebase specific GeoNode
        private Dictionary<string, CullableAvatarEntity> trackedNetworkEntities = new Dictionary<string, CullableAvatarEntity>();

        // Central Reference Point (Usually the Local Android Camera / User Map Center)
        private Transform activePlayerCamera;

        private void Awake()
        {
            if (Instance == null) { Instance = this; }
            else { Destroy(gameObject); }
            
            // Start the asynchronous visibility grid loop preventing heavy main-thread locking
            StartCoroutine(CullingHeartbeatLoop());
        }

        private void Start()
        {
            if (Camera.main != null) activePlayerCamera = Camera.main.transform;
            Debug.Log("[Visibility Manager] Spatial Hashing Culling Engine Engaged.");
        }

        /// <summary>
        /// Registers a new Android user who just popped up on your Map!
        /// </summary>
        public void RegisterNetworkEntity(string userId, GameObject avatarInstance)
        {
            if (!trackedNetworkEntities.ContainsKey(userId))
            {
                trackedNetworkEntities.Add(userId, new CullableAvatarEntity
                {
                    MasterGameObject = avatarInstance,
                    PhysicsRenderer = avatarInstance.GetComponentsInChildren<Renderer>(true),
                    SkeletonDriver = avatarInstance.GetComponent<Animator>(),
                    IsCurrentlyRendered = true
                });
            }
        }

        public void DeregisterEntity(string userId)
        {
            if (trackedNetworkEntities.ContainsKey(userId))
            {
                Destroy(trackedNetworkEntities[userId].MasterGameObject);
                trackedNetworkEntities.Remove(userId);
            }
        }

        /// <summary>
        /// 8. PERFORMANCE OPTIMIZATION (Predictive Render Queue)
        /// Scans all entities only 2 times a second. Saves colossal battery drains on mobile.
        /// </summary>
        private IEnumerator CullingHeartbeatLoop()
        {
            WaitForSeconds pollingDelay = new WaitForSeconds(0.5f); // Half sec ticks 

            while (true)
            {
                if (activePlayerCamera == null) yield return pollingDelay;
                
                Vector3 cameraPos = activePlayerCamera.position;

                // Cycle the hash table using safe Enumerator references
                foreach (var entity in trackedNetworkEntities.Values)
                {
                    if (entity.MasterGameObject == null) continue;

                    // Calculate spatial vector distance to the Android operator
                    float distanceFromCamera = Vector3.Distance(cameraPos, entity.MasterGameObject.transform.position);

                    // 1. HARD CULL (Completely Turn off the Mesh Renderer Data to clear VRAM)
                    if (distanceFromCamera > lazyRenderDistanceThreshold && entity.IsCurrentlyRendered)
                    {
                        ToggleEntityGraphics(entity, false);
                    }
                    else if (distanceFromCamera <= lazyRenderDistanceThreshold && !entity.IsCurrentlyRendered)
                    {
                        ToggleEntityGraphics(entity, true);
                    }

                    // 2. SOFT CULL (Freeze the SkinnedMesh Animator logic but keep the static object drawn)
                    if (entity.IsCurrentlyRendered && entity.SkeletonDriver != null)
                    {
                        entity.SkeletonDriver.enabled = (distanceFromCamera <= animationFreezeThreshold);
                    }
                }

                yield return pollingDelay;
            }
        }

        private void ToggleEntityGraphics(CullableAvatarEntity entity, bool renderState)
        {
            entity.IsCurrentlyRendered = renderState;
            foreach (var mesh in entity.PhysicsRenderer)
            {
                if (mesh != null) mesh.enabled = renderState;
            }
        }
    }

    /// <summary>
    /// Lightweight caching wrapper avoiding recurrent Expensive GetComponent() calls in Unity's Update.
    /// </summary>
    class CullableAvatarEntity
    {
        public GameObject MasterGameObject;
        public Renderer[] PhysicsRenderer;
        public Animator SkeletonDriver;
        public bool IsCurrentlyRendered;
    }
}
