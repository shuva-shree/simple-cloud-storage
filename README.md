# ☁️ Simple Cloud Storage (SCS)

A full-stack cloud storage web application modeled after Amazon S3, built as a capstone project for the Backend Engineering Launchpad by Airtribe. This simplified clone enables users to store, manage, and retrieve files securely from their own personal cloud space.

---

## 📚 Project Overview

In today's digital landscape, understanding how cloud storage systems like Amazon S3 work is crucial. This project aims to replicate core features of such systems, focusing on file management, versioning, authentication, access control, and metadata — all with a clean and intuitive web interface.

---

## 🎯 Core Features

### ✅ Authentication
- User Registration & Login
- JWT-based Session Management
- Secure password handling

### 📂 File Management
- Upload & Download files
- File versioning and rollback
- Folder/Bucket-like hierarchy
- File deletion and renaming

### 🔍 Organization & Search
- View and search uploaded files
- Search by name or metadata

### 🔒 Access Control
- Private/Public/Shared file visibility
- Role-based file access (optional enhancement)

### 🏷️ Metadata Support
- Add tags or custom properties to files
- Improve searchability and categorization

---

## 🧩 Add-ons & Extensions (Planned/Optional)

- 🚫 **File Deduplication**: Prevent storage of duplicate files.
- 📊 **Usage Analytics**: APIs to show storage usage, access frequency, file types, etc.

---

## 🛠️ Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Security + JWT
- AWS SDK for S3
- JPA/Hibernate
- MySQL

### Frontend
- React.js
- Material UI + Tailwind CSS
- Axios for API Calls

---

## 🗃️ Folder Structure

simple-cloud-storage/
├── backend/
│ ├── src/
│ └── pom.xml
├── frontend/
│ ├── src/
│ └── package.json

## ⚙️ Setup Instructions

### 🔧 Backend

1. **Configure `application.yml`**
   Replace AWS & DB credentials:

```yaml
aws:
  s3:
    bucket-name: your-bucket-name
    region: your-region
    access-key: your-access-key
    secret-key: your-secret-key
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db
    username: your_username
    password: your_password
Run Backend

bash
Copy
Edit
cd backend
./mvnw spring-boot:run
Backend will be available at http://localhost:8080.

💻 Frontend
bash
Copy
Edit
cd frontend
npm install
npm start
Runs at http://localhost:3000.

📬 API Reference
Method	    Endpoint	                              Description	Auth               Required
POST	        /api/auth/register	                  Register a new user	              ❌
POST	        /api/auth/login	                       Login and get JWT	              ❌
POST	        /api/files/upload	                    Upload file	                      ✅
GET	          /api/files	                          Get all user files               	✅
GET	          /api/files/download/{id}	            Download file by ID	              ✅
PUT	          /api/files/{id}	                      Rename file                      	✅
DELETE	      /api/files/{id}	                      Delete file                      	✅
GET	          /api/files/versions/{id}	            Get file version history        	✅



