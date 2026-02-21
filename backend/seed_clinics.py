import json
import requests
import sys

BASE_URL = "http://localhost:8080/api"

def seed_clinics(limit=3, single_index=None):
    try:
        # Using utf-8-sig to handle the UTF-8 BOM
        with open("clinics.json", "r", encoding="utf-8-sig") as f:
            data = json.load(f)
    except FileNotFoundError:
        print("❌ clinics.json not found.")
        return
    except json.JSONDecodeError as e:
        print(f"❌ JSON Decode Error: {e}")
        return

    clinics = data.get("value", [])
    if not clinics:
        print("❌ No clinics found in clinics.json")
        return

    # Select specific clinic if index provided, else use limit
    if single_index is not None:
        if 0 <= single_index < len(clinics):
            to_process = [clinics[single_index]]
            print(f"🚀 Inserting single hospital [Index {single_index}]: {to_process[0]['name']}")
        else:
            print(f"❌ Index {single_index} out of range (0 to {len(clinics)-1})")
            return
    else:
        to_process = clinics[:limit]
        print(f"🚀 Preparing to insert {len(to_process)} hospitals sequentially...")

    for i, clinic_data in enumerate(to_process, 1):
        name = clinic_data.get("name")
        print(f"\n[{i}/{len(to_process)}] 🏥 Current: {name}")
        
        # Interactive confirmation if not single index
        if len(to_process) > 1:
            choice = input(f"Proceed with '{name}'? (y/n): ").strip().lower()
            if choice != 'y':
                print("⏩ Skipping...")
                continue

        # Payload for ClinicRequestDTO
        payload = {
            "name": name,
            "address": clinic_data.get("address"),
            "city": clinic_data.get("city"),
            "phone": clinic_data.get("phone"),
            "latitude": clinic_data.get("latitude"),
            "longitude": clinic_data.get("longitude"),
            "rating": clinic_data.get("rating"),
            "timings": clinic_data.get("timings"),
            "imageUrl": "/src/assets/images/default-hospital.jpg",
            "specializations": clinic_data.get("specializations", [])
        }

        try:
            r = requests.post(f"{BASE_URL}/clinics", json=payload)
            if r.status_code == 200:
                created = r.json()
                print(f"  ✅ Success! Clinic ID: {created.get('clinicId')}")
            else:
                print(f"  ❌ Failed: {r.status_code} - {r.text}")
            
        except Exception as e:
            print(f"  ❌ Connection Error: {str(e)}")
            break

    print("\n🏁 Process complete.")

if __name__ == "__main__":
    idx = None
    if len(sys.argv) > 1:
        try:
            idx = int(sys.argv[1])
        except ValueError:
            pass
    
    # If a large number is passed, assume it's the limit, if small, could be zero-based index.
    # To keep it simple, if user passes '0', '1', '2' etc, it inserts just that one.
    if idx is not None and idx < 100:
        seed_clinics(single_index=idx)
    else:
        seed_clinics(limit=3)
