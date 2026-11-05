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

# GAP-006: Python Bridge Functions

BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

def encode_geohash(lat, lon, precision=9):
    lat_interval = [-90.0, 90.0]
    lon_interval = [-180.0, 180.0]
    geohash = []
    bits = [16, 8, 4, 2, 1]
    bit = 0
    ch = 0
    even = True
    while len(geohash) < precision:
        if even:
            mid = (lon_interval[0] + lon_interval[1]) / 2
            if lon > mid:
                ch |= bits[bit]
                lon_interval[0] = mid
            else:
                lon_interval[1] = mid
        else:
            mid = (lat_interval[0] + lat_interval[1]) / 2
            if lat > mid:
                ch |= bits[bit]
                lat_interval[0] = mid
            else:
                lat_interval[1] = mid
        even = not even
        if bit < 4:
            bit += 1
        else:
            geohash.append(BASE32[ch])
            bit = 0
            ch = 0
    return "".join(geohash)

def decode_geohash(hashcode):
    lat_interval = [-90.0, 90.0]
    lon_interval = [-180.0, 180.0]
    even = True
    for c in hashcode:
        cd = BASE32.find(c)
        for mask in [16, 8, 4, 2, 1]:
            if even:
                mid = (lon_interval[0] + lon_interval[1]) / 2
                if cd & mask:
                    lon_interval[0] = mid
                else:
                    lon_interval[1] = mid
            else:
                mid = (lat_interval[0] + lat_interval[1]) / 2
                if cd & mask:
                    lat_interval[0] = mid
                else:
                    lat_interval[1] = mid
            even = not even
    lat = (lat_interval[0] + lat_interval[1]) / 2
    lon = (lon_interval[0] + lon_interval[1]) / 2
    return {'latitude': lat, 'longitude': lon}

def filter_gps_jitter(path, threshold_meters=5.0):
    if len(path) < 3:
        return path
    filtered = [path[0]]
    for i in range(1, len(path)-1):
        prev_p = filtered[-1]
        curr_p = path[i]
        dist = calculate_distance(prev_p['latitude'], prev_p['longitude'], curr_p['latitude'], curr_p['longitude'])
        if dist > threshold_meters:
            filtered.append(curr_p)
    filtered.append(path[-1])
    return filtered

def sanitize_history_batch(batch):
    if not batch: return []
    unique = {p['timestamp']: p for p in batch}
    sorted_batch = sorted(unique.values(), key=lambda k: k['timestamp'])
    return filter_gps_jitter(sorted_batch)
