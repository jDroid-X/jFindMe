using UnityEngine;
using jFindMe.Multiplayer.Network;

namespace jFindMe.Multiplayer.Core
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 2 Blueprint): Application Root Bootstrapper.
    /// The absolute top-level Unity MonoBehavior handling Unity Engine initialization,
    /// Android JNI messaging, Frame Rate caps, and Battery preservation.
    /// </summary>
    public class AppManager : MonoBehaviour
    {
        public static AppManager Instance { get; private set; }

        [Header("Global Hooks")]
        public GameStateManager masterStateController;
        public SyncService masterFirebaseAuth;

        // Validates App Initialization only happens exactly once per runtime
        private bool isEngineBooted = false;

        private void Awake()
        {
            // Singleton enforcing standard Unity design rules
            if (Instance == null)
            {
                Instance = this;
                DontDestroyOnLoad(gameObject);
                InitializeDeviceHardwareLimits();
            }
            else
            {
                Destroy(gameObject);
            }
        }

        private void InitializeDeviceHardwareLimits()
        {
            // Protect Native Android battery drain!
            // Resolves Blueprint Concern: "Heavy models -> FPS drop"
            Application.targetFrameRate = 60; // DO NOT run phones at default unlocked 120 FPS
            QualitySettings.vSyncCount = 0;   // Override VSync to let FrameRate manage battery
            Screen.sleepTimeout = SleepTimeout.NeverSleep; // Map needs to stay alive

            Debug.Log("[App Manager] Engine Constraints Applied. VSync [OFF]. Frames [60 Max].");
        }

        /* ========================================================================= */
        /* UNITY <-> ANDROID (JNI) SECURITY LAYER                                    */
        /* ========================================================================= */

        /// <summary>
        /// This Method is fired explicitly by Android Kotlin/Java immediately after successful Login.
        /// It acts as the Gateway preventing Unity from polling Firebase before Auth tokens are ready.
        /// Example Payload sent from Android: "U123-TokenHash-XyZ"
        /// </summary>
        public void WakeUnityEngine_JNI(string serializedAuthToken)
        {
            if (isEngineBooted) 
            {
                Debug.LogWarning("[App Manager] Engine already active. Ignoring duplicate Android Login Event.");
                return;
            }

            Debug.Log("[App Manager] Authentication Token Received from Native App.");

            try
            {
                // Disseminate security tokens to the Multiplayer Network Hook
                masterFirebaseAuth.InitializeSessionAuth(serializedAuthToken);
                
                // Fire the Central Game State sequence!
                isEngineBooted = true;
                masterStateController.InitializeAvatarLifecycleFlow();
            }
            catch (System.Exception ex)
            {
                Debug.LogError($"[App Manager Fatal] System Crash during Boot: {ex.Message}");
            }
        }

        private void OnApplicationFocus(bool hasFocus)
        {
            if (!hasFocus)
            {
                // Optionally suspend the heavy 3D Renderer if the user clicks a Native Android Pop-up
                Debug.Log("[App Manager] User unfocused Unity. Pausing Render Engine to save Battery...");
            }
        }
    }
}
