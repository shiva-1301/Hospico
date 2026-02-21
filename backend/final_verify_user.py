import requests

BASE_URL = "http://localhost:8080/api"
TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsImlkIjoyNjU2NjAwMDAwMDA4MDE0NiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJzdWIiOiJhZG1pbkBnbWFpbC5jb20iLCJpYXQiOjE3NzE2NjQ5MTMsImV4cCI6MTc3MjI2OTcxM30.znEfHcbIYwkIEz3o22MCzp8v9L1gCNhv4X9bBjjlc9o"

def final_check():
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Content-Type": "application/json"
    }
    
    print("Testing full profile update (Name, Phone, Age, Gender)...")
    update_payload = {
        "name": "Admin Fully Updated",
        "phone": "9876543210",
        "age": 35,
        "gender": "Male"
    }
    
    r = requests.put(f"{BASE_URL}/users/profile", json=update_payload, headers=headers)
    print(f"Update status: {r.status_code}")
    print(f"Update response: {r.text}")
    
    if r.status_code == 200:
        print("\n✅ Verification SUCCESSFUL! All fields updated in Zoho.")
    else:
        print("\n❌ Verification FAILED. Check if columns 'age' and 'gender' were created correctly in Zoho Catalyst.")

if __name__ == "__main__":
    final_check()
