# Social Network Android Application

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/firebase-ffca28?style=for-the-badge&logo=firebase&logoColor=black)

## 1. Overview
A high-performance, real-time Android social networking application designed with a focus on **System Performance** and **Modular Architecture**. The project leverages Firebase as the primary backend infrastructure to handle real-time data synchronization, user authentication, and distributed state management.

## 2. Core Features & Technical Highlights

### 2.1. Native Media Optimization
* **Algorithm:** Custom-built image compression logic executing on background threads via Coroutines.
* **Process:** Resizes images to a maximum of 1024px and applies JPEG compression at 80% quality before uploading to Cloudinary.
* **Benefit:** Dramatically reduces network latency and prevents Out-of-Memory (OOM) errors during heavy feed scrolling.

### 2.2. Real-time Messaging & Interactions
* **Dynamic Routing:** Utilizes deterministic Chat Room IDs (`uid1_uid2`) for peer-to-peer communication.
* **Message Requests:** Includes a protocol-based status system (`PENDING`, `ACCEPTED`, `REJECTED`) for social safety.
* **Sub-collection Strategy:** Embeds Likes and Comments directly within the Post document to achieve "One-pass Fetching" for the Newsfeed.

### 2.3. Social Connectivity
* **Friend-Based Visibility:** Implements a strict bidirectional friendship validation logic; posts are only visible to verified friends, optimizing database query overhead.

## 3. Architecture & Tech Stack

The application strictly adheres to the **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository Pattern** to ensure a clean separation of concerns.

* **Language:** Kotlin
* **Async & State Management:** Kotlin Coroutines & LiveData.
* **BaaS (Backend as a Service):** Firebase Authentication & Realtime Database.
* **Media Provider:** Cloudinary API.

### Project Structure
```text
com.example.socialnetwork/
├── adapter/       # RecyclerView adapters for Feed, Chat, and Search
├── auth/          # Authentication flows & session handling
├── chat/          # Real-time messaging & room observation logic
├── core/          # Interfaces (Contracts) & Data Models (User, Post, ChatRoom)
├── feed/          # Timeline loading & Post interaction modules
├── firebase/      # Firebase DB abstract implementations
├── media/         # Native compression & Cloudinary Service
├── mock/          # Dummy data for offline UI testing
├── profile/       # User profile & Friend management
├── Services/      # Third-party service integrations (Cloudinary)
├── ui/            # Activities, Fragments & Custom UI components
└── util/          # Helper classes for Image processing & Navigation
```
## 4. Database Schema Strategy
The NoSQL architecture prioritizes **Read Speed** over write redundancy:

* **Denormalization:** Caching `username` and `avatarUrl` inside the `Comment` object to eliminate relational joins during scrolling.
* **Inbox Caching:** Storing `lastMessage`, `lastMessageTime`, and localized `userNames` inside the `ChatRoom` object for instantaneous inbox rendering without secondary queries to the `Users` node.
* **Sub-collection Logic:** Embedding interactions (`Likes`, `Comments`) directly within each `Post` document to achieve "One-pass Fetching" for the Newsfeed.

## 5. Setup & Installation

### 5.1. Environment Configuration
* **Firebase:** Place your `google-services.json` in the root `app/` directory.
* **Cloudinary:** Initialize your API credentials (Cloud Name, API Key, API Secret) inside `Services/CloudinaryService.kt`.

### 5.2. Sync & Build
* Execute **Sync Project with Gradle Files** in Android Studio.
* Run **Build > Clean Project** followed by **Rebuild Project** to resolve all generated classes and dependencies.

### 5.3. Deployment
* Deploy on a physical Android device or an Emulator running API Level 24 (Android Nougat) or higher.

## 6. Technical Roadmap (v2.0)

* **Dependency Injection (DI):** Migrate manual `ViewModelProvider.Factory` instantiations to **Hilt** for automated dependency management and better testability.
* **Advanced Media Caching:** Integrate **Glide** or **Coil** to cache loaded images locally, further reducing bandwidth overhead after the initial native compression.
* **Read Receipts:** Implement `isSeen` status tracking within the `Message` data model for an enhanced real-time chat experience.

---
*Developed as an Academic Project demonstrating Advanced Android Engineering and System Architecture.*
