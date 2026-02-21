import requests
import time

BASE_URL = "http://localhost:8080/api"

def debug_specialization():
    spec_name = f"Test-Spec-{int(time.time())}"
    print(f"Testing creation of specialization: {spec_name}")
    
    # We use the /api/clinics endpoint because it triggers the createSpecialization internally
    payload = {
        "name": f"Debug Clinic {int(time.time())}",
        "specializations": [spec_name]
    }
    
    try:
        r = requests.post(f"{BASE_URL}/clinics", json=payload)
        print(f"Status Code: {r.status_code}")
        print(f"Response Text: {r.text}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    debug_specialization()
