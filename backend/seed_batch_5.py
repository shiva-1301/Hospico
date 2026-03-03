import requests
import json

hospitals = [
    {
        "name": "M/s American Cancer Care Private Limited",
        "address": "D.No. 29-13/41/1, Near Pushpa Hotel Bus Stop, Kaleswara Rao Road, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Oncology"],
        "phone": "Not Available",
        "timings": "Not Available",
        "rating": 0.0,
        "latitude": 16.509933081273278,
        "longitude": 80.63778499152109,
        "imageUrl": "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Royal Hospitals",
        "address": "D.No. 33-25-45, Pushpa Hotel Road, Kasturibaipet",
        "city": "Vijayawada",
        "specializations": ["Orthopedics"],
        "phone": "0866 244 1778",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "latitude": 16.510267634913564,
        "longitude": 80.6384420208238,
        "imageUrl": "https://images.unsplash.com/photo-1586773860418-d37222d8fce2?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Sagar ENT- Head and Neck Superspeciality Center",
        "address": "D.No.29-28/1-16, Kovelamudivari Street, Suryaraoept",
        "city": "Vijayawada",
        "specializations": ["ENT", "Anaesthesiology"],
        "phone": "0866 244 0806",
        "timings": "10:00 AM - 05:00 PM",
        "rating": 4.5,
        "latitude": 16.512465062657125,
        "longitude": 80.63790346269191,
        "imageUrl": "https://images.unsplash.com/photo-1512678080530-7760d81faba6?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Crest Hospital",
        "address": "Road No.1, D.No. 54-1-7/2H, Vijayalakshmi Colony",
        "city": "Vijayawada",
        "specializations": ["General Surgery"],
        "phone": "Not Available",
        "timings": "Open 24 Hours",
        "rating": 5.0,
        "latitude": 16.510331984302276,
        "longitude": 80.6728375032364,
        "imageUrl": "https://images.unsplash.com/photo-1538108197017-c1a986eb933d?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Nori Medicare Private Limited",
        "address": "D.No.24-2-16/A, Nageshwara Rao Panthulu Road, Gandhi Nagar",
        "city": "Vijayawada",
        "specializations": ["Pediatrics"],
        "phone": "Not Available",
        "timings": "Not Available",
        "rating": 0.0,
        "latitude": 16.517484,
        "longitude": 80.630338,
        "imageUrl": "https://images.unsplash.com/photo-1628177142898-93e36e4e3a30?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Meenakshi Eye Hospital",
        "address": "D.No. 29-19-81, Dornakal Road, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Ophthalmology"],
        "phone": "0866 243 4343",
        "timings": "Open 24 Hours",
        "rating": 4.3,
        "latitude": 16.510505617620186,
        "longitude": 80.63490775319097,
        "imageUrl": "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Unity Dental Clinic",
        "address": "Dr No. 29-25-9, Sri Guru Nilayam Vemuri Vari Street, Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Dental Surgery"],
        "phone": "Not Available",
        "timings": "10:00 AM - 09:00 PM",
        "rating": 5.0,
        "latitude": 16.512622320755266,
        "longitude": 80.63615979040084,
        "imageUrl": "https://images.unsplash.com/photo-1586773860418-d37222d8fce2?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Ayaan Eye Hospital",
        "address": "D.No.12-2-148/A, Opp: ICICI Bank, B R P Road, Islmapeta",
        "city": "Vijayawada",
        "specializations": ["Ophthalmology"],
        "phone": "081213 10131",
        "timings": "Open 24 Hours",
        "rating": 4.8,
        "latitude": 16.520081660950577,
        "longitude": 80.6154323015029,
        "imageUrl": "https://images.unsplash.com/photo-1512678080530-7760d81faba6?auto=format&fit=crop&q=80&w=800"
    },
    {
        "name": "M/s Palagani Hospital",
        "address": "D.No. 29-25-22, Dornakal Road, Behind Endocare Hospital,Suryaraopet",
        "city": "Vijayawada",
        "specializations": ["Ophthalmology"],
        "phone": "Not Available",
        "timings": "Not Available",
        "rating": 0.0,
        "latitude": 16.51365357239475,
        "longitude": 80.63651848587978,
        "imageUrl": "https://images.unsplash.com/photo-1538108197017-c1a986eb933d?auto=format&fit=crop&q=80&w=800"
    }
]

url = "http://localhost:8080/api/clinics"
headers = {"Content-Type": "application/json"}

for hospital in hospitals:
    response = requests.post(url, data=json.dumps(hospital), headers=headers)
    if response.status_code == 200 or response.status_code == 201:
        print(f"Successfully seeded: {hospital['name']}")
    else:
        print(f"Failed to seed: {hospital['name']} - Status: {response.status_code} - Response: {response.text}")
