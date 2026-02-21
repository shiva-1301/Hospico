import requests
import sys

BASE_URL = "http://localhost:8080/api"

hospitals = [
    {
        "name": "Sentini Hospitals (P) Ltd.",
        "address": "54-15-5 B & C, Besides Vinayak Theatre, Ring Road",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopedics", "Pediatrics", "Pathology", "Plastic Surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": "0866 247 9444",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "reviews": 2494,
        "latitude": 16.50981627119791,
        "longitude": 80.6649629240617
    },
    {
        "name": "Andhra Hospitals Pvt., Ltd., CVR Complex",
        "address": "Prakasam Road",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "Cardiothoracic surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "Nuclear Medicine", "OBG", "Orthopedics", "Pathology", "Plastic surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy unit", "Dialysis unit"],
        "phone": "0866 257 4757",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "reviews": 3000,
        "latitude": 16.51117741820386,
        "longitude": 80.6294306813449
    },
    {
        "name": "Vijaya Super Speciality Hospital",
        "address": "29-26-29A, Near Swathi Press, Boyapativari Street, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Orthopedics", "Neurosurgery", "Radiology", "Nephrology", "General Surgery", "Surgical Gastroenterology", "Cardiology", "Urology"],
        "phone": "0866 243 0000",
        "timings": "Open 24 Hours",
        "rating": 4.4,
        "reviews": 2407,
        "latitude": 16.513666647753862,
        "longitude": 80.6372788759688
    },
    {
        "name": "Metro Super Speciality Hospital",
        "address": "29-14-32, Nakkal Road Cross Junction, Prakasam Road, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopedics", "Plastic Surgery", "Radiology", "Urology", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": "0866 244 2324",
        "timings": "Open 24 Hours",
        "rating": 4.6,
        "reviews": 0,
        "latitude": 16.51076472842161,
        "longitude": 80.63237626376701
    },
    {
        "name": "Praveen Hospitals",
        "address": "#32-09-18, Moghalrajapuram, Madhugarden Centre",
        "city": "Vijayawada",
        "specializations": ["Cardiology"],
        "phone": "0866 247 2222",
        "timings": "Open 24 Hours",
        "rating": 4.2,
        "reviews": 98,
        "latitude": 16.50784560781151,
        "longitude": 80.64276169367497
    },
    {
        "name": "Usha Cardiac Centre Ltd.",
        "address": "39-2-11, Pitchaiah Street, M.G.Road, Labbipet",
        "city": "Vijayawada",
        "specializations": ["Cardiology", "CT Surgery", "General Medicine", "Pulmonology"],
        "phone": "0866 247 4747",
        "timings": "Open 24 Hours",
        "rating": 4.2,
        "reviews": 98,
        "latitude": 16.501093342991144,
        "longitude": 80.6423871041949
    },
    {
        "name": "M/s HCG Curie City Cancer Centre",
        "address": "#44-1-1/3, Padavalarevu, Gunadala",
        "city": "Vijayawada",
        "specializations": ["Oncology"],
        "phone": "0866 669 9999",
        "timings": "Open 24 Hours",
        "rating": 4.7,
        "reviews": 2912,
        "latitude": 16.523952321750247,
        "longitude": 80.65505452140079
    },
    {
        "name": "Krishna Gastro & Liver Centre and Safe Multi Speciality Hospital",
        "address": "29-14-52, Pushpa Hotel Centre, Prakasam Road, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Surgical Gastroenterology"],
        "phone": "0866 243 5999",
        "timings": "Open 24 Hours",
        "rating": 4.9,
        "reviews": 2924,
        "latitude": 16.50977216618124,
        "longitude": 80.63762968422164
    },
    {
        "name": "M/s Sri Anu Hospitals",
        "address": "D.No.29-28/1-13, Kovelamudivari Street, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "Cardiology", "Cardiothoracic Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "Nuclear Medicine", "OBG", "Orthopedics", "Pediatrics", "Pathology", "Plastic Surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy unit", "Dialysis unit"],
        "phone": "0866 243 8585",
        "timings": "Open 24 Hours",
        "rating": 4.0,
        "reviews": 78,
        "latitude": 16.51169707971456,
        "longitude": 80.63747165970142
    },
    {
        "name": "Global Multi Speciality Hospital",
        "address": "Beside Police Control Room, 27-39-1, MG Road",
        "city": "Vijayawada",
        "specializations": ["General Medicine", "General Surgery", "Anaethesiology", "ENT", "Nephrology", "Neurosurgery", "OBG", "Orthopaedics", "Paediatrics", "Pathology", "Plastic Surgery", "Pulmonology", "Radiology", "Urology", "Physiotherpay Unit", "Dialysis Unit"],
        "phone": "0866 257 1613",
        "timings": "Open 24 Hours",
        "rating": 5.0,
        "reviews": 98,
        "latitude": 16.511294761964525,
        "longitude": 80.61834194568262
    },
    {
        "name": "V.G.R Diabetes Specialities Hospital",
        "address": "# 40-5-19/18/B, A.S. Rama Rao Road, Moghalrajpuram",
        "city": "Vijayawada",
        "specializations": ["General Medicine"],
        "phone": "0866 247 2472",
        "timings": "09:00 AM - 08:00 PM",
        "rating": 4.8,
        "reviews": 3659,
        "latitude": 16.5058678140856,
        "longitude": 80.64667236235803
    },
    {
        "name": "Trust Hospital",
        "address": "Kalanagar, Near Benz circle",
        "city": "Vijayawada",
        "specializations": ["Orthopaedics"],
        "phone": "0866 247 4777",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "reviews": 733,
        "latitude": 16.500867281323913,
        "longitude": 80.65401681894083
    },
    {
        "name": "Capital Hospital",
        "address": "#15-177/1, Machilipatnam Road, Poranki",
        "city": "Vijayawada",
        "specializations": ["Cardiology", "Cardiothoracic Surgery", "ENT", "General Medicine", "General Surgery", "Surgical Gastroenterology", "Nephrology", "Urology", "Neurosurgery", "OBG", "Orthopedics", "Pediatrics", "Pathology", "Pulmonology", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": "0866 258 3666",
        "timings": "Open 24 Hours",
        "rating": 4.6,
        "reviews": 5675,
        "latitude": 16.47794016153474,
        "longitude": 80.70182918898541
    },
    {
        "name": "Martha Health Care Services Pvt Ltd",
        "address": "Tiruvuru",
        "city": "Vijayawada",
        "specializations": ["General Medicine", "General Surgery", "OBG", "Ortopaedics", "Anaesthesiology", "ENT", "Paediatrics", "Nephrology", "Physiotherapy"],
        "phone": None,
        "timings": None,
        "rating": 4.3,
        "reviews": 3,
        "latitude": 17.11342626013233,
        "longitude": 80.61466090677706
    },
    {
        "name": "Svara Super Speciality Hospitals",
        "address": "D.No. 23-5/1-1b, BRTS Roas, Rajavari Street, Satyanarayanapuram",
        "city": "Vijayawada",
        "specializations": ["Anesthesiology", "General Medicine", "General Surgery", "ENT", "Nephrology", "Neurosurgery", "Radiology", "Urology", "Surgical Gastroenterology", "Orthopedics", "Pulmonology", "Cardiology", "OBG", "Plastic Surgery", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": "0866 243 8585",
        "timings": "Open 24 Hours",
        "rating": 3.8,
        "reviews": 359,
        "latitude": 16.521080224369022,
        "longitude": 80.6356614840933
    },
    {
        "name": "Top Star Hospitals",
        "address": "#10-35, Bandar Road, Kanuru",
        "city": "Vijayawada",
        "specializations": ["Anaesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopaedics", "Paediatrics", "Pathology", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy Unit"],
        "phone": "099124 84646",
        "timings": "10:00 AM - 05:00 PM",
        "rating": 4.2,
        "reviews": 139,
        "latitude": 16.484906733429664,
        "longitude": 80.68310174583621
    },
    {
        "name": "Sri Bhavani Hospitals",
        "address": "# 75-7-29, Nagarjuna Street, Bhavanipuram",
        "city": "Vijayawada",
        "specializations": ["Anaesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopaedics", "Paediatrics", "Pathology", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy Unit"],
        "phone": "0866 243 8585",
        "timings": "Open 24 Hours",
        "rating": 3.8,
        "reviews": 145,
        "latitude": 16.52419660582987,
        "longitude": 80.59414990908081
    },
    {
        "name": "Mohan Gandhi American Kidney Institute",
        "address": "#54-15-14/2A, Near Novatel Hotel, NH5 Service Road, Bharathi Nagar",
        "city": "Vijayawada",
        "specializations": ["Anaesthesiology", "Cardiology", "CT Surgery", "ENT", "Emergency Medicine", "General Medicine", "General Surgery", "Nephrology", "Neurosurgery", "OBG", "Orthopaedics", "Paediatrics", "Pathology", "Plastic Surgery", "Pulmonology", "Radiology", "Surgical Gastroenterology", "Surgical Oncology", "Urology", "Physiotherapy Unit", "Dialysis Unit"],
        "phone": None,
        "timings": None,
        "rating": 4.7,
        "reviews": 88,
        "latitude": 16.508044519098945,
        "longitude": 80.6643746269751
    }
]

def seed_batch():
    print(f"🚀 Starting insertion of {len(hospitals)} hospitals (Batch 3)...")
    
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

    print("\n🏁 Batch 3 processing complete.")

if __name__ == "__main__":
    seed_batch()
