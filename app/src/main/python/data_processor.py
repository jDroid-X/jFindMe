import math

def calculate_distance(lat1, lon1, lat2, lon2):
    """
    Haversine formula to calculate distance between two points on Earth.
    Main -> Data Engine (Py) -> AnalyticsEngine -> calculate_distance
    """
    R = 6371000  # Radius of Earth in meters
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = math.sin(delta_phi / 2) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * \
        math.sin(delta_lambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return R * c

def calculate_total_path_distance(path):
    """
    Calculates total distance of a path (List of Dicts).
    Task: Data Analytics -> Total Distance.
    """
    total = 0.0
    for i in range(len(path) - 1):
        p1 = path[i]
        p2 = path[i+1]
        total += calculate_distance(p1['latitude'], p1['longitude'], p2['latitude'], p2['longitude'])
    return total

def filter_spatial_bounds(user_locations, center_lat, center_lon, radius_meters):
    """
    Filters a list of user locations based on a circular radius.
    Level 5: BoundaryChecker logic.
    """
    filtered = []
    for user in user_locations:
        dist = calculate_distance(center_lat, center_lon, user['latitude'], user['longitude'])
        if dist <= radius_meters:
            filtered.append(user)
    return filtered

def predict_next_point(path, interval_seconds=3):
    """
    Simple linear path prediction for smooth interpolation.
    Level 5: PathPredictor logic.
    """
    if len(path) < 2:
        return None
    
    p1 = path[-2]
    p2 = path[-1]
    
    dt = p2['timestamp'] - p1['timestamp']
    if dt == 0: return p2
    
    vel_lat = (p2['latitude'] - p1['latitude']) / dt
    vel_lon = (p2['longitude'] - p1['longitude']) / dt
    
    predicted_lat = p2['latitude'] + vel_lat * interval_seconds
    predicted_lon = p2['longitude'] + vel_lon * interval_seconds
    
    return {'latitude': predicted_lat, 'longitude': predicted_lon}
