import requests
import sys

BASE_URL = "http://localhost:8080/api"

hospitals = [
    {
        "name": "M. J. Naidu Super Speciality Hospital",
        "address": "Near Pushpa Hotel, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Orthopedics", "General Medicine", "General Surgery", "Neurosurgery", "Physiotherapy"],
        "phone": "0866 243 5555",
        "timings": "Open 24 Hours",
        "rating": 4.4,
        "reviews": 2105,
        "latitude": 16.511327842442128,
        "longitude": 80.6380999639433
    },
    {
        "name": "Nagarjuna Hospitals Limited",
        "address": "Kanuru",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "Nuclear Medicine", "OBG", "Orthopedics", "Pediatrics", "Pathology", "Plastic surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy unit", "Dialysis unit"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 4.4,
        "reviews": 1783,
        "latitude": 16.48720574039239,
        "longitude": 80.68409616586813
    },
    {
        "name": "Peoples Clinic",
        "address": "Palaparthivari Street, Governorpet",
        "city": "Vijayawada",
        "specializations": ["General medicine", "General Surgery", "Anaesthesia", "OBG", "Orthopaedics", "Paediatrics", "Plastic Surgery"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 4.0,
        "reviews": 20,
        "latitude": 16.506239219586416,
        "longitude": 80.6481031181904
    },
    {
        "name": "Dr. Pinnamaneni Siddhartha Institute of Medical Sciences & Research Centre",
        "address": "Chinaoutpalli, Gannavaram Mandal",
        "city": "Vijayawada",
        "specializations": ["Anaesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopaedics", "Paediatrics", "Pathology", "Plastic Surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical oncoloy", "Urology", "Physiotherpay Unit", "Dialysis"],
        "phone": "08676 257311",
        "timings": "08:00 AM - 04:00 PM",
        "rating": None,
        "reviews": 0,
        "latitude": 16.554435863059652,
        "longitude": 80.82707554412512
    },
    {
        "name": "Help Hospitals",
        "address": "D.No.#27-29-23, Behind Victoria Musem, MG Road",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "CT Surgery", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopedics", "Pediatrics", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Urology", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": "0866 5515552",
        "timings": "Open 24 Hours",
        "rating": 5.0,
        "reviews": 0,
        "latitude": 16.508909109522097,
        "longitude": 80.62732582161281
    },
    {
        "name": "Liberty Hospitals",
        "address": "74-1-2, King Squere Buildings OPP; Auto Nagar Bus Terminal",
        "city": "Vijayawada",
        "specializations": ["General Medicine", "General Surgery", "Neuro Surgery", "CT Surgery", "Orthopaedics", "Cardiology", "Pulmonology", "Radiology", "Plastic Surgery", "Surgical Oncology", "Urology"],
        "phone": "0866 255 4888",
        "timings": "Open 24 Hours",
        "rating": 4.5,
        "reviews": 779,
        "latitude": 16.489950539458864,
        "longitude": 80.67154769955941
    },
    {
        "name": "Kamineni Hospital",
        "address": "100 Ft.Road, Poranki",
        "city": "Vijayawada",
        "specializations": ["General Medicine", "General Surgery", "Pulmonology", "Pediatrics", "OBG", "Orthopedics", "ENT", "Cardiology", "CT Surgery", "Nephrology", "Urology", "Neurosurgery", "Surgical Gastroenterology"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 4.6,
        "reviews": 4720,
        "latitude": 16.495862279479695,
        "longitude": 80.7030532536673
    },
    {
        "name": "Sateesh Gastro & Liver Centre",
        "address": "#29-14-37, 5 Route, Prakasam Road, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Gastroenterology"],
        "phone": "+91 9177353586",
        "timings": "Open 24 Hours",
        "rating": 3.4,
        "reviews": 574,
        "latitude": 16.511479499997087,
        "longitude": 80.63497368735072
    },
    {
        "name": "Arun Kidney Center",
        "address": "29-23-9, Tadepallivari Street, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Nephrology", "Urology"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 3.7,
        "reviews": 292,
        "latitude": 16.51346860411751,
        "longitude": 80.63527688981101
    },
    {
        "name": "S.V.R Neuro & Trauma Super Speciality Hospital",
        "address": "# 40-1/1-14, ABC, Labbipet",
        "city": "Vijayawada",
        "specializations": ["Neurosurgery"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 4.4,
        "reviews": 513,
        "latitude": 16.50410697857134,
        "longitude": 80.64248055089872
    },
    {
        "name": "Dr. Ramesh Cardiac & Multi Speciality Hospital Ltd.",
        "address": "Ring Road, Near ITI College, Vijyawada, Krishna Dist.",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "Cardiothoracic surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "Nuclear Medicine", "OBG", "Orthopedics", "Pathology", "Plastic surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy unit", "Dialysis unit", "Neurosurgery"],
        "phone": "0866 2484800",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "reviews": 36,
        "latitude": 16.50576418399193,
        "longitude": 80.65772401473744
    },
    {
        "name": "Sunrise Hospitals",
        "address": "D.No. 33-25-35, Opp: Union Bank of India,Suryarao pet",
        "city": "Vijayawada",
        "specializations": ["Orthopaedics", "Nephrology", "General Surgery", "Cardiology", "General Medicine", "ENT", "Anaesthesia", "OBG", "Radiology", "Pulmonology", "Neuro Surgery", "Urology", "Plastic Surgery", "CT Surgery", "Surgical Gastro Enterology", "Surgical Oncology"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 3.0,
        "reviews": 456,
        "latitude": 16.51281900677876,
        "longitude": 80.63922501841644
    },
    {
        "name": "Sagar Vascular and Diabetik Foot Care Centre",
        "address": "Dr.No. 29-10-32, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["General Surgery"],
        "phone": None,
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "reviews": 14,
        "latitude": 16.512541801501662,
        "longitude": 80.63380097614606
    },
    {
        "name": "Harini Hospitals",
        "address": "Prakasam Road, Pushpa Hotel Center, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Surgical Gastroenterology"],
        "phone": "0866 248 5544",
        "timings": "Open 24 Hours",
        "rating": 4.9,
        "reviews": 10161,
        "latitude": 16.509275755953613,
        "longitude": 80.63750550322567
    }
]

def seed_batch():
    print(f"🚀 Starting insertion of {len(hospitals)} hospitals...")
    
    for i, data in enumerate(hospitals, 1):
        name = data["name"]
        print(f"\n[{i}/{len(hospitals)}] 🏥 Processing: {name}")
        
        payload = {
            "name": name,
            "address": data["address"],
            "city": data["city"],
            "phone": data["phone"],
            "latitude": data["latitude"],
            "longitude": data["longitude"],
            "rating": data["rating"],
            "reviews": data["reviews"],
            "timings": data["timings"],
            "imageUrl": "/src/assets/images/default-hospital.jpg",
            "specializations": data["specializations"]
        }
        
        try:
            r = requests.post(f"{BASE_URL}/clinics", json=payload)
            if r.status_code == 200:
                print(f"  ✅ Success!")
            else:
                print(f"  ❌ Failed: {r.status_code} - {r.text}")
        except Exception as e:
            print(f"  ❌ Connection Error: {str(e)}")
            break

    print("\n🏁 Batch 2 processing complete.")

if __name__ == "__main__":
    seed_batch()
