# Geofence App

This project is a Geofence App for Android that allows users to define geographical areas on a map and monitor whether the device's current location is inside those areas. The app tracks this information and notifies the user when entering or exiting a defined area.

## Features

1. **Define Geofences:** Users can mark circular regions on the map by long-pressing an area. Multiple geofences can be added or removed.
2. **Location Tracking:** The app tracks the device's location and checks whether it enters or exits any defined geofence.
3. **Data Persistence:** Geofence areas and location data are stored in a database using a ContentProvider.
4. **Service Management:** Background services track device location changes and check for geofence crossings.
5. **Results Display:** Users can view the geofences and points of entry/exit on the map for the most recent session.

## App Components

### Main Activity

- Provides options to:
  - Define geofences on a map.
  - Stop tracking and logging.
  - View points where the device entered/exited geofences.

### Map Activity

- Displays the current location on a Google Map.
- Allows the user to:
  - Long-press on the map to define a circular geofence (100m radius).
  - Remove a geofence by long-pressing on it.
  - Press "Cancel" to return to the main screen without saving.
  - Press "Start" to:
    - Save geofence locations to the database.
    - Start the location tracking service.

### Content Provider

Handles database access:

- Stores geofence centers for each session.
- Retrieves geofence data for the current session.
- Records entry/exit points into/out of geofences.
- Retrieves data from the last session.

### Service

- Tracks device location changes every 5 seconds (if moved more than 50 meters).
- Compares the current location with defined geofences.
- Logs entry/exit points when crossing geofences.

### Broadcast Receiver

- Stops the tracking service when there is no GPS signal.
- Restarts the service when GPS signal is available.

### Results Map Activity

- Displays a map showing:
  - Defined geofences from the last session.
  - Entry and exit points for geofences.
  - The current device location.
- Includes buttons to pause/restart the service or return to the main screen.
