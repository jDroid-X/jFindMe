using System.Collections;
using UnityEngine;
using jFindMe.Multiplayer.Map; // Access to GPS coordinates

namespace jFindMe.Multiplayer.Avatar
{
    /// <summary>
    /// CORE RESPONSIBILITY (PHASE 3: Blueprint Option 2): Avatar Behavior AI
    /// Drives actual auto-movement logic for both Multiplayer Avatars and Autonomous NPCs.
    /// Handles exact WANDER, PATROL, CHASE, and STATIC logic maps securely linked to the Animator.
    /// </summary>
    [RequireComponent(typeof(AvatarAnimator))]
    public class AvatarController : MonoBehaviour
    {
        [Header("AI State Config")]
        public string currentState = "STATIC";
        private AvatarAnimator animatorSystem;

        [Header("Global GPS Hooks")]
        public double currentLat;
        public double currentLng;
        private GPSService nativeGpsHook;

        // Navigation parameters
        private Vector3 startPosition;
        private Vector3 patrolTarget;
        private float wanderRadius = 15f; 
        private float movementSpeed = 3f;

        private void Awake()
        {
            animatorSystem = GetComponent<AvatarAnimator>();
            startPosition = transform.position;
        }

        /* ========================================================================= */
        /* MAP + UNITY INTEGRATION (Exact Method Execution)                          */
        /* ========================================================================= */
        
        /// <summary>
        /// Explicitly binds the Android hardware GPS service natively overriding default Unity Physics
        /// </summary>
        public void BindNativeLocationService(GPSService androidTracker)
        {
            nativeGpsHook = androidTracker;
            // Native Event Hook: Constantly updates its internal vector variables whenever Android sends a JNI packet
            nativeGpsHook.OnLocationUpdated += SyncTruePosition;
            Debug.Log("[Avatar Behavior AI] Native Android Location Hook Attached!");
        }

        private void SyncTruePosition(double lat, double lng, float m_speed)
        {
            currentLat = lat;
            currentLng = lng;
            
            // Overrides manual AI if the entity is a human multiplayer instance
            if (currentState == "MULTIPLAYER_SYNC")
            {
                animatorSystem.CurrentStateName = m_speed > 2.0f ? "RUN" : "WALK";
            }
        }

        private void OnDestroy()
        {
            if (nativeGpsHook != null)
                nativeGpsHook.OnLocationUpdated -= SyncTruePosition;
        }

        /* ========================================================================= */
        /* AUTONOMOUS MOVEMENT AI LOOP                                               */
        /* ========================================================================= */

        /// <summary>
        /// Fired dynamically by the GameStateManager depending on what the Firebase Schema demands.
        /// </summary>
        public void InitializeAutonomousBehavior(string movementType)
        {
            currentState = movementType;
            StopAllCoroutines();

            switch (movementType)
            {
                case "WANDER":
                    StartCoroutine(ExecWanderLogic());
                    break;
                case "PATROL":
                    StartCoroutine(ExecPatrolLogic());
                    break;
                case "CHASE":
                    StartCoroutine(ExecChaseLogic());
                    break;
                case "STATIC":
                default:
                    animatorSystem.CurrentStateName = "IDLE";
                    break;
            }
        }

        private IEnumerator ExecWanderLogic()
        {
            while (currentState == "WANDER")
            {
                // 1. Calculate a random localized waypoint
                Vector2 randomCircle = Random.insideUnitCircle * wanderRadius;
                Vector3 waypoint = startPosition + new Vector3(randomCircle.x, 0, randomCircle.y);
                
                // 2. Turn towards target
                transform.LookAt(waypoint);
                animatorSystem.CurrentStateName = "WALK";

                // 3. Physically march toward the coordinate
                while (Vector3.Distance(transform.position, waypoint) > 0.5f)
                {
                    transform.position = Vector3.MoveTowards(transform.position, waypoint, movementSpeed * Time.deltaTime);
                    yield return null; // Yield loop allowing Frame-Rate to stay locked at 60 FPS
                }

                // 4. Stand still and rest before generating a new path
                animatorSystem.CurrentStateName = "IDLE";
                yield return new WaitForSeconds(Random.Range(2f, 5f));
            }
        }

        private IEnumerator ExecPatrolLogic()
        {
            // Simple Line-Of-Sight A->B march logic simulating standard Guards
            patrolTarget = startPosition + (transform.forward * 20f);
            bool movingForward = true;

            while (currentState == "PATROL")
            {
                Vector3 currentDest = movingForward ? patrolTarget : startPosition;
                transform.LookAt(currentDest);
                animatorSystem.CurrentStateName = "WALK";

                while (Vector3.Distance(transform.position, currentDest) > 0.5f)
                {
                    transform.position = Vector3.MoveTowards(transform.position, currentDest, (movementSpeed * 1.5f) * Time.deltaTime);
                    yield return null;
                }

                // Reverse path
                movingForward = !movingForward;
                animatorSystem.CurrentStateName = "IDLE";
                yield return new WaitForSeconds(1.5f);
            }
        }

        private IEnumerator ExecChaseLogic()
        {
            // WARNING: Heavy CPU requirement calculating distance to User's GPS hardware continuously
            while (currentState == "CHASE")
            {
                // Extrapolates distance to main camera (The Device Operator)
                Vector3 playerAnchor = Camera.main.transform.position;
                playerAnchor.y = transform.position.y; // Lock Y axis to prevent floating

                if (Vector3.Distance(transform.position, playerAnchor) < 30f) // Detection Radius
                {
                    transform.LookAt(playerAnchor);
                    animatorSystem.CurrentStateName = "RUN";
                    
                    // Closes distance aggressively
                    transform.position = Vector3.MoveTowards(transform.position, playerAnchor, (movementSpeed * 2.5f) * Time.deltaTime);
                } 
                else
                {
                    animatorSystem.CurrentStateName = "IDLE";
                }

                yield return null;
            }
        }
    }
}
