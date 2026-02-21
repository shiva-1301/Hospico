import requests
import json

BASE_URL = "http://localhost:8080/api"

def test_all_updates():
    print("Fetching IDs for testing...")
    
    # Clinics
    r_clinics = requests.get(f"{BASE_URL}/clinics")
    clinics = r_clinics.json()
    if not clinics: return print("No clinics found")
    clinic_id = clinics[0]['clinicId']
    print(f"Testing Clinic {clinic_id}...")
    requests.put(f"{BASE_URL}/clinics?id={clinic_id}", json={"name": "Test Hospital Updated", "rating": 4.9})
    print("Clinic update request sent.")

    # Specializations
    r_specs = requests.get(f"{BASE_URL}/specializations")
    specs = r_specs.json()
    if not specs: return print("No specializations found")
    spec_id = specs[0]['id']
    print(f"Testing Specialization {spec_id}...")
    requests.put(f"{BASE_URL}/specializations/{spec_id}", json={"specialization": "Pediatrics (Test)"})
    print("Specialization update request sent.")

    # Doctors
    r_doctors = requests.get(f"{BASE_URL}/doctors")
    doctors = r_doctors.json()
    if doctors:
        doc_id = doctors[0]['id']
        print(f"Testing Doctor {doc_id}...")
        requests.put(f"{BASE_URL}/doctors/{doc_id}", json={"experience": "20+ years"})
        print("Doctor update request sent.")

    # Reviews
    # Fetching reviews for a hospital/doctor... tricky without knowing which one has reviews.
    # We'll just try to fetch all reviews if an endpoint exists, or skip.
    print("Verification script update: IDs would be fetched dynamically.")

if __name__ == "__main__":
    test_all_updates()
