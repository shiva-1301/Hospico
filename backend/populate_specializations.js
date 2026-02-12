// MongoDB script to populate specializations collection
// Run this with: mongosh "mongodb+srv://chunkupraney:Chunku@hospiico.7pgebzk.mongodb.net/HospiiCo" --file populate_specializations.js

const specializations = [
  { id: 1, specialization: "Anesthesiology" },
  { id: 2, specialization: "Cardiology" },
  { id: 3, specialization: "Dermatology" },
  { id: 4, specialization: "Emergency Medicine" },
  { id: 5, specialization: "Endocrinology" },
  { id: 6, specialization: "Family Medicine" },
  { id: 7, specialization: "Gastroenterology" },
  { id: 8, specialization: "General Surgery" },
  { id: 9, specialization: "Geriatrics" },
  { id: 10, specialization: "Gynecology" },
  { id: 11, specialization: "Hematology" },
  { id: 12, specialization: "Infectious Disease" },
  { id: 13, specialization: "Internal Medicine" },
  { id: 14, specialization: "Nephrology" },
  { id: 15, specialization: "Neurology" },
  { id: 16, specialization: "Neurosurgery" },
  { id: 17, specialization: "Obstetrics" },
  { id: 18, specialization: "Oncology" },
  { id: 19, specialization: "Ophthalmology" },
  { id: 20, specialization: "Orthopedics" },
  { id: 21, specialization: "Otolaryngology (ENT)" },
  { id: 22, specialization: "Pathology" },
  { id: 23, specialization: "Pediatrics" },
  { id: 24, specialization: "Physical Medicine" },
  { id: 25, specialization: "Plastic Surgery" },
  { id: 26, specialization: "Psychiatry" },
  { id: 27, specialization: "Pulmonology" },
  { id: 28, specialization: "Radiology" },
  { id: 29, specialization: "Rheumatology" },
  { id: 30, specialization: "Urology" },
  { id: 31, specialization: "Dentist" },
  { id: 32, specialization: "General Physician" }
];

// Use the HospiiCo database
db = db.getSiblingDB('HospiiCo');

// Clear existing specializations
print("Clearing existing specializations...");
db.specializations.deleteMany({});

// Insert specializations
print("Inserting " + specializations.length + " specializations...");
for (let spec of specializations) {
  spec._id = spec.id;  // Set _id to match id
  spec.ROWID = spec.id; // Set ROWID for compatibility
  db.specializations.insertOne(spec);
}

print("Done! Inserted specializations:");
db.specializations.find().forEach(printjson);
