import React, { useState } from 'react';
import { StyleSheet, View, Alert, Text, Platform, Dimensions, TouchableOpacity, PermissionsAndroid } from 'react-native';
import { LeafletView, MapShape, MapShapeType } from 'react-native-leaflet-view';
import Geolocation from '@react-native-community/geolocation';

const { height, width } = Dimensions.get('window');
const DEFAULT_LOCATION = { lat: 13.0827, lng: 80.2707 };
const DEFAULT_ZOOM = 15;

export default function App() {
  const [mapCenter, setMapCenter] = useState(DEFAULT_LOCATION);
  const [zoomLevel, setZoomLevel] = useState(DEFAULT_ZOOM);

  const requestLocationPermission = async () => {
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
      );
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    }
    return true;
  };

  const goToMyLocation = async () => {
    const hasPermission = await requestLocationPermission();
    
    if (hasPermission) {
      Geolocation.getCurrentPosition( // watchPosition -- live location tracking
        (position) => {
          const { latitude, longitude } = position.coords;
          setMapCenter({ lat: latitude, lng: longitude });
        },
        (error) => Alert.alert("Error", error.message),
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 10000 }
      );
    }
  };

  const mapMarkers = [
    {
      id: 'marker-1',
      position: mapCenter,
      icon: '🦝', 
      title: 'Racoon HQ',
      size: [30, 30],
    },
  ];

  const mapShapes:MapShape[] = [
    {
      shapeType: MapShapeType.POLYGON,
      id: 'poly-1',
      positions: [
        { lat: mapCenter.lat + 0.01, lng: mapCenter.lng },
        { lat: mapCenter.lat, lng: mapCenter.lng + 0.01 },
        { lat: mapCenter.lat - 0.01, lng: mapCenter.lng },
      ],
      color: 'red',
    },
    {
      shapeType: MapShapeType.POLYGON,
      id: 'poly-2',
      positions: [
        { lat: mapCenter.lat, lng: mapCenter.lng - 0.02},
        { lat: mapCenter.lat - 0.01, lng: mapCenter.lng + 0.01},
        { lat: mapCenter.lat - 0.02, lng: mapCenter.lng + 0.015},
        { lat: mapCenter.lat - 0.03, lng: mapCenter.lng - 0.01},
      ],
      color: 'blue',
    },
  ];

  const handleZoomIn = () => {
    if (zoomLevel < 18) setZoomLevel(prev => prev + 1);
  };

  const handleZoomOut = () => {
    if (zoomLevel > 1) setZoomLevel(prev => prev - 1);
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>🦝</Text>
      </View>
      <View style={styles.mapWrapper}>
        <LeafletView
          mapCenterPosition={mapCenter}
          zoom={zoomLevel}
          doDebug={true}
          mapMarkers={mapMarkers}
          mapShapes={mapShapes}
          onMessageReceived={(message) => {
            if (message.msg === 'MAP_READY') {
              console.log("Map reported READY. Forcing center...");
              setMapCenter(DEFAULT_LOCATION); // Small nudge to trigger the update bridge
              setZoomLevel(DEFAULT_ZOOM)
            }
          }}
          onError={() => {Alert.alert("Map", "Could not load map. Please try again...")}}
          zoomControl={false}
          androidHardwareAccelerationDisabled={false}
        />
      </View>
      <View style={styles.zoomControls}>
        <TouchableOpacity style={styles.glassButton} onPressOut={handleZoomIn} disabled={zoomLevel === 18}>
          <Text style={styles.buttonText}>+</Text>
        </TouchableOpacity>
        
        <View style={styles.divider} />
        
        <TouchableOpacity style={styles.glassButton} onPressOut={handleZoomOut} disabled={zoomLevel === 1}>
          <Text style={styles.buttonText}>−</Text>
        </TouchableOpacity>
      </View>
      <TouchableOpacity style={styles.glassCircle} onPress={goToMyLocation}>
          <Text style={{ fontSize: 24 }}></Text>
        </TouchableOpacity>
    </View>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    position: 'absolute',
    top: 20,
    left: 10,
    right: 10,
    height: 70,
    zIndex: 10,
    
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    borderRadius: 20,
    paddingHorizontal: 20,
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.5)',
    
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 8,
      },
      android: {
        elevation: 5,
      },
    }),
  },
  title: {
    fontSize: 40,
  },
  mapWrapper: {
    ...StyleSheet.absoluteFill,
    height: height,
    width: width,
  },
  zoomControls: {
    position: 'absolute',
    bottom: 40,
    right: 20,
    width: 50,
    backgroundColor: 'rgba(255, 255, 255, 0.85)',
    borderRadius: 15,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.5)',
    overflow: 'hidden',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 5,
  },
  glassButton: {
    height: 50,
    width: 50,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonText: {
    fontSize: 24,
    fontWeight: '600',
    color: '#333',
  },
  divider: {
    height: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.1)',
    marginHorizontal: 10,
  },
  glassCircle: {
    position: 'absolute',
    bottom: 40,
    right: 80,
    width: 40,
    height: 40,
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 27.5,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.6)',
    elevation: 6,
  },
});