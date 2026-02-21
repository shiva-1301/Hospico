import requests

BASE_URL = "http://localhost:8080/api"
TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsImlkIjoyNjU2NjAwMDAwMDA4MDE0NiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJzdWIiOiJhZG1pbkBnbWFpbC5jb20iLCJpYXQiOjE3NzE2NjQ5MTMsImV4cCI6MTc3MjI2OTcxM30.znEfHcbIYwkIEz3o22MCzp8v9L1gCNhv4X9bBjjlc9o"

def check_schema():
    headers = {"Authorization": f"Bearer {TOKEN}"}
    # We use /me because it calls findByEmail and returns user data
    # But we want the RAW response if possible.
    # Actually, I'll just try to update only name/phone which we know exist.
    
    print("Attempting to update name and phone only...")
    update_payload = {
        "name": "Admin Updated",
        "phone": "9876543210"
    }
    r = requests.put(f"{BASE_URL}/users/profile", json=update_payload, headers=headers)
    print(f"Update status: {r.status_code}")
    print(f"Update response: {r.text}")

if __name__ == "__main__":
    check_schema()
