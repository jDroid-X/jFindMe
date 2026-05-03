// Firebase Web SDK (v10.x Modular)
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import { getDatabase, connectDatabaseEmulator } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getFunctions, connectFunctionsEmulator } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-functions.js";

const firebaseConfig = {
  apiKey: "AIzaSyB2zKc74mL366T23pNMq6ykPJt3VSIHDiM",
  authDomain: "jfindme-3b15c.firebaseapp.com",
  databaseURL: "https://jfindme-3b15c-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "jfindme-3b15c",
  storageBucket: "jfindme-3b15c.firebasestorage.app",
  messagingSenderId: "251276830691",
  appId: "1:251276830691:web:45179684234699906ca2d6"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const database = getDatabase(app);
const auth = getAuth(app);
const functions = getFunctions(app);

// 🚀 CLOUD_FUNCTIONS_LOCAL_HANDSHAKE
// If running in development/local mode, attach to emulators
if (location.hostname === "localhost" || location.hostname === "127.0.0.1") {
    // connectDatabaseEmulator(database, "localhost", 9000);
    connectFunctionsEmulator(functions, "localhost", 5001);
    console.log("[SYSTEM] Tactical Handshake: Connected to Local Cloud Functions.");
}

export { database, auth, functions, firebaseConfig };
