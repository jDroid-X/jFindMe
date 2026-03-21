using System;
using System.Collections;
using System.IO;
using UnityEngine;
using UnityEngine.Networking;
// using Siccity.GLTFUtility; // Must be installed via package manager in Production

namespace jFindMe.Multiplayer.Avatar
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): Download and Cache Model.
    /// Resolves the "No avatar caching -> repeated downloads" fatal failure point.
    /// Manages physical storage I/O and Native 3D format GLB instantiation.
    /// </summary>
    public class AvatarLoader : MonoBehaviour
    {
        [Header("System Anchors")]
        // The empty GameObject acting as the 0,0,0 spawn base on the map
        public Transform rootMapAnchor;  
        // Fallback model if connection drops mid-download
        public GameObject missingModelPrefab; 

        private string safeStorageDirectory;

        private void Awake()
        {
            // Maps exactly to the production blueprint requirement
            safeStorageDirectory = Path.Combine(Application.persistentDataPath, "DownloadedAvatars");
            
            if (!Directory.Exists(safeStorageDirectory))
            {
                Directory.CreateDirectory(safeStorageDirectory);
            }
        }

        public void DownloadAndCacheModel(string downloadUrl, Action<GameObject> onModelInstantiated)
        {
            StartCoroutine(CachePipelineRoutine(downloadUrl, onModelInstantiated));
        }

        private IEnumerator CachePipelineRoutine(string firebaseUrl, Action<GameObject> onModelInstantiated)
        {
            // Generate a secure hashed filename ensuring Android storage compatibility
            string fileName = $"model_{Mathf.Abs(firebaseUrl.GetHashCode())}.glb";
            string fullDiskPath = Path.Combine(safeStorageDirectory, fileName);

            // ARCHITECTURE CHECK: "No avatar caching" resolution
            if (File.Exists(fullDiskPath))
            {
                Debug.Log($"[Avatar Loader] Cache hit detected. Loading instantly from hardware: {fileName}");
                ParseAndMount3DAsset(fullDiskPath, onModelInstantiated);
                yield break;
            }

            Debug.Log($"[Avatar Loader] Cache miss. Downloading raw 3D mesh bytes from Firebase...");

            using (UnityWebRequest webLoader = UnityWebRequest.Get(firebaseUrl))
            {
                // Memory Optimization: Write directly to internal drive instead of crashing app RAM
                webLoader.downloadHandler = new DownloadHandlerFile(fullDiskPath);
                
                yield return webLoader.SendWebRequest();

                if (webLoader.result != UnityWebRequest.Result.Success)
                {
                    Debug.LogError("[Avatar Loader] Network disconnect during GLB fetch. Replacing with Emergency Fallback Mesh.");
                    SpawnEmergencyFallback(onModelInstantiated);
                }
                else
                {
                    Debug.Log($"[Avatar Loader] Success! Stored gigabyte asset locally at: {fullDiskPath}");
                    ParseAndMount3DAsset(fullDiskPath, onModelInstantiated);
                }
            }
        }

        private void ParseAndMount3DAsset(string rawFilePath, Action<GameObject> onSuccess)
        {
            try
            {
                // NOTE: Production projects install Siccity/GLTFUtility to un-parse the physical files back into Unity Physics Meshes
                // GameObject importedAvatar = Importer.LoadFromFile(rawFilePath);

                // Mock execution representing the resolved prefab
                GameObject importedAvatar = new GameObject("Rigged_GLB_Avatar"); 

                // Reset positions explicitly to Android bounds
                importedAvatar.transform.SetParent(rootMapAnchor, false);
                importedAvatar.transform.localPosition = Vector3.zero;

                onSuccess?.Invoke(importedAvatar);
            }
            catch (Exception ex)
            {
                Debug.LogError($"[Avatar Loader] Corruption while loading GLB file: {ex.Message}");
                SpawnEmergencyFallback(onSuccess);
            }
        }

        private void SpawnEmergencyFallback(Action<GameObject> onSuccess)
        {
            if (missingModelPrefab != null)
            {
                GameObject fallback = Instantiate(missingModelPrefab, rootMapAnchor);
                onSuccess?.Invoke(fallback);
            }
        }
    }
}
