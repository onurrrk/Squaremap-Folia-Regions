# 🗺️ Squaremap-Folia-Regions

A high-performance **Squaremap** extension specifically designed for **Folia** servers to visualize threaded regions in real-time.

![map](https://raw.githubusercontent.com/onurrrk/Pictures/refs/heads/main/Squaremap-Folia-Regions.jpg)

## 🚀 Key Features

*   **Threaded Region Mapping:** Visually see how Folia splits your world into different threads (regions) on the map.
*   **Live Metrics (Popup):** Click on any region to see real-time data:
    *   **TPS & MSPT:** Individual performance monitoring for each thread.
    *   **Region Statistics:** Current chunk, player, and entity counts per region.
*   **Zero Performance Impact:** Built using optimized reflections and asynchronous schedulers to ensure your server TPS stays stable.
*   **Dynamic Shape Engine:** Uses Convex Hull algorithms to draw the exact boundaries of active regions as they grow or merge.

## 🛠️ Installation

1.  Make sure you have **Squaremap** installed on your **Folia** server.
2.  Download the latest `.jar` file and place it in your `plugins` folder.
3.  Restart your server.
4.  Open your web map and enable the **"Regions"** layer from the sidebar.

## 📄 Requirements

*   **Server Engine:** Folia (1.20.1 - ∞)
*   **Dependency:** Squaremap
*   **Java Version:** Java 21+
