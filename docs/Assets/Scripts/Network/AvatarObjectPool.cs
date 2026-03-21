using System.Collections.Generic;
using UnityEngine;

namespace jFindMe.Multiplayer.Network
{
    /// <summary>
    /// CORE RESPONSIBILITY: (♻️ 9. OBJECT POOLING & 🧊 6. OPTIMIZATION)
    /// Prevents CPU-locking Instantiate/Destroy spikes when avatars enter/leave the GPS range.
    /// Radically stabilizes frame rates (60FPS) by recycling dormant 3D models natively.
    /// </summary>
    public class AvatarObjectPool : MonoBehaviour
    {
        public static AvatarObjectPool Instance { get; private set; }

        [Header("Pool Configuration")]
        public GameObject baseHumanoidPrefab;
        public int initialPoolSize = 10;
        
        // Memory Pool storing inactive recycled avatars waiting to be spawned
        private Queue<GameObject> dormantAvatarCache = new Queue<GameObject>();

        private void Awake()
        {
            if (Instance == null) { Instance = this; }
            else { Destroy(gameObject); }

            // Pre-warm the pool to avoid first-spawn stutter
            PreWarmPool();
        }

        private void PreWarmPool()
        {
            for (int i = 0; i < initialPoolSize; i++)
            {
                GameObject newAvatar = Instantiate(baseHumanoidPrefab, Vector3.zero, Quaternion.identity);
                newAvatar.SetActive(false);
                dormantAvatarCache.Enqueue(newAvatar);
            }
            Debug.Log($"[Object Pool] Pre-warmed {initialPoolSize} avatars in offline memory.");
        }

        /// <summary>
        /// 📦 8. SPAWN SYSTEM: Called by MultiplayerVisibilityManager when a user enters range.
        /// </summary>
        public GameObject SpawnAvatarFromPool(Vector3 spawnLocation, Quaternion spawnRotation)
        {
            if (dormantAvatarCache.Count > 0)
            {
                // Instant Recycle (0 CPU Cost)
                GameObject recycledAvatar = dormantAvatarCache.Dequeue();
                recycledAvatar.transform.position = spawnLocation;
                recycledAvatar.transform.rotation = spawnRotation;
                recycledAvatar.SetActive(true);
                return recycledAvatar;
            }

            // Fallback: If 15 people suddenly log in, strictly allocate a new one to the pool
            GameObject emergencyAvatar = Instantiate(baseHumanoidPrefab, spawnLocation, spawnRotation);
            return emergencyAvatar;
        }

        /// <summary>
        /// 📦 8. DESPAWN SYSTEM: Called when user logs out or leaves 300m range.
        /// </summary>
        public void DespawnAndRecycleAvatar(GameObject activeAvatar)
        {
            // Clear any modular meshes or colored shirts previously strapped to the skeleton
            // (Assumes connection to ModularAvatarAssembler)
            ResetSkeletalModifications(activeAvatar);

            activeAvatar.SetActive(false);
            dormantAvatarCache.Enqueue(activeAvatar);
        }

        private void ResetSkeletalModifications(GameObject avatarInstance)
        {
            // Wipe customizations so the next user who recycles this model doesn't spawn wearing the last guy's shirt!
            Transform spine = avatarInstance.transform.Find("SpineBone");
            if (spine != null) 
            {
                foreach(Transform child in spine) { Destroy(child.gameObject); }
            }
        }
        
        /// <summary>
        /// ⚡ 7. LOD (Level of Detail) SWASTIKA (LOD0 -> LOD1 -> LOD2)
        /// Swaps mesh details natively on the given entity to prevent poly-melt
        /// </summary>
        public void SetEntityLOD(GameObject avatar, int lodLevel)
        {
            // Implementation expects an LODGroup component on the root prefab
            LODGroup detailGroup = avatar.GetComponent<LODGroup>();
            if (detailGroup != null)
            {
                detailGroup.ForceLOD(lodLevel);
            }
        }
    }
}
