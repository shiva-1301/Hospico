import requests

BASE_URL = "http://localhost:8080/api"

def check_clinic_id(clinic_id):
    print(f"🔍 Checking clinic ID: {clinic_id}")
    try:
        r = requests.get(f"{BASE_URL}/clinics/id?id={clinic_id}")
        print(f"  Status Code: {r.status_code}")
        if r.status_code == 200:
            print(f"  ✅ Clinic Found: {r.json().get('name')}")
        else:
            print(f"  ❌ Clinic Not Found: {r.text}")
    except Exception as e:
        print(f"  ❌ Error: {e}")

def check_reviews(clinic_id):
    print(f"\n🔍 Checking reviews for clinic ID: {clinic_id}")
    try:
        r = requests.get(f"{BASE_URL}/reviews/hospital/{clinic_id}")
        print(f"  Status Code: {r.status_code}")
        if r.status_code == 200:
            print(f"  ✅ Reviews Found: {len(r.json())} reviews")
        else:
            print(f"  ❌ Reviews Fetch Failed: {r.text}")
    except Exception as e:
        print(f"  ❌ Error: {e}")

if __name__ == "__main__":
    # Sateesh Gastro & Liver Centre - confirmed ID
    target_id = "26566000000080553"
    check_clinic_id(target_id)
    check_reviews(target_id)
    
    # Latha - confirmed ID
    latha_id = "26566000000080343"
    print("\n--- Checking Slice 3 ID ---")
    check_clinic_id(latha_id)
    check_reviews(latha_id)
