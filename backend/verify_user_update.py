import requests
import time

BASE_URL = "http://localhost:8080/api"

def test_user_update():
    print("Step 1: Signing up a new user...")
    timestamp = int(time.time())
    email = f"testuser_{timestamp}@example.com"
    signup_payload = {
        "name": "Original Name",
        "email": email,
        "phone": "0000000000",
        "password": "password123"
    }
    
    signup_response = requests.post(f"{BASE_URL}/auth/signup", json=signup_payload)
    if signup_response.status_code != 200:
        print(f"Signup failed: {signup_response.text}")
        return
    
    signup_data = signup_response.json()
    token = signup_data.get('token')
    print(f"Signup successful. Token obtained: {token[:10]}...")

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    print("\nStep 2: Updating profile...")
    update_payload = {
        "name": "Updated Name",
        "phone": "1231231234",
        "age": 25,
        "gender": "Female"
    }
    
    update_response = requests.put(f"{BASE_URL}/users/profile", json=update_payload, headers=headers)
    print(f"Update Status Code: {update_response.status_code}")
    print(f"Update Response: {update_response.text}")

    if update_response.status_code == 200:
        print("\nStep 3: Verifying change via /api/users/profile...")
        verify_response = requests.get(f"{BASE_URL}/users/profile", headers=headers)
        print(f"Verification Status: {verify_response.status_code}")
        print(f"Verification Response: {verify_response.text}")
        
        user_data = verify_response.json()
        if user_data.get('name') == "Updated Name" and user_data.get('age') == 25:
            print("\n✅ User profile update working correctly!")
        else:
            print("\n❌ User profile update verification failed.")
    else:
        print("\n❌ User profile update failed.")

if __name__ == "__main__":
    test_user_update()
