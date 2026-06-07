# ClassCast

Attendance voting and class management app for professors and students.

**Platform:** Android (Kotlin) | **Firebase:** Auth · Firestore · Realtime Database  
**Student:** Jung Won Jung | **ID:** 58183 | **Course:** Mobile App Development

---

## Overview

ClassCast solves a common classroom problem: professors have no visibility into how many students plan to attend until class starts. Students can now signal their attendance intent in two taps, and professors see live vote counts update in real-time.

**Professor flow:** Login → Manage classes → Open headcount dashboard → Watch Going / Maybe / Not Going update live  
**Student flow:** Login → Student Vote (menu) → Enter class code → Tap Going / Maybe / Not Going

---

## Features

### Core (Lab 2)
- **Firebase Authentication** — email/password login with session persistence, auto-redirect if already signed in
- **Firestore CRUD** — create, read, update, delete classes; real-time snapshot listener filtered by `ownerId`
- **Status toggle** — mark classes active / inactive (Extension 2)
- **Input validation** — email format, password strength, course name/code validation (Extension 1)
- **Empty state UI** — friendly empty view when no classes exist (Extension 3)
- **Delete confirmation** — AlertDialog before removing a class

### Advanced (Lab 3 — Option C: Realtime Database)
- **VoteActivity** — students search by class code, submit attendance vote (`going` / `maybe` / `not_going`)
- **HeadcountActivity** — professor's live dashboard; `addValueEventListener` on `votes/{classId}` fires on every student vote, counts update with zero polling
- **Optimistic write** — `AddClassActivity` calls `finish()` immediately after `db.collection("classes").add(...)`, Firestore offline persistence ensures instant UI update

### Testing
- **28 JUnit4 unit tests** covering: email validation, password validation, password match, course name, course code, vote values, class code search
- All tests run on the JVM — no Android framework required
- Run with: `./gradlew test`

---

## Architecture

```
app/
├── LoginActivity.kt          # Firebase Auth sign-in
├── RegisterActivity.kt       # Account creation + validation
├── MainActivity.kt           # Class list (Firestore snapshot listener)
├── AddClassActivity.kt       # Create class (optimistic write)
├── VoteActivity.kt           # Student attendance voting (Realtime DB write)
├── HeadcountActivity.kt      # Live headcount dashboard (Realtime DB listener)
├── adapter/
│   └── ClassAdapter.kt       # RecyclerView adapter with delete / toggle / headcount callbacks
├── model/
│   └── ClassItem.kt          # Data class: classId, ownerId, courseName, courseCode, description, status, createdAt
└── utils/
    └── ValidationUtils.kt    # Pure Kotlin object — all validation logic (JVM testable)
```

---

## Firebase Data Model

### Firestore — `classes` collection

```
classes/{docId}
  ├── ownerId:     String   (Firebase Auth UID)
  ├── courseName:  String
  ├── courseCode:  String
  ├── description: String
  ├── status:      String   ("active" | "inactive")
  └── createdAt:   Timestamp
```

### Realtime Database — `votes` node

```
votes/
  {classId}/
    {userId}: String   ("going" | "maybe" | "not_going")
```

---

## Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /classes/{classId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == resource.data.ownerId;
      allow create: if request.auth != null;
    }
  }
}
```

## Realtime Database Security Rules

```json
{
  "rules": {
    "votes": {
      "$classId": {
        ".read": "auth != null",
        "$userId": {
          ".write": "auth != null && auth.uid === $userId"
        }
      }
    }
  }
}
```

> **Important:** `.read` must be at the `$classId` level (not `$userId`) so `HeadcountActivity`'s `addValueEventListener` on the parent node is permitted.

---

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Firebase project with Auth, Firestore, and Realtime Database enabled

### Steps

1. **Clone the repo**
   ```bash
   git clone <repo-url>
   cd ClassCast
   ```

2. **Add `google-services.json`**  
   Download from Firebase Console → Project Settings → Your Apps → Android → Download config file.  
   Place at `app/google-services.json`.

3. **Set Realtime Database URL**  
   The explicit URL is already set in `VoteActivity.kt` and `HeadcountActivity.kt`:
   ```kotlin
   FirebaseDatabase.getInstance("https://classcast-7a42b-default-rtdb.firebaseio.com")
   ```
   Update this if your project uses a different URL.

4. **Sync and run**  
   Open in Android Studio → File → Sync Project with Gradle Files → Run.

5. **Apply Firebase rules**  
   Paste the Firestore and Realtime Database rules above into the Firebase Console Rules tabs and Publish.

---

## Running Tests

```bash
./gradlew test
```

Test file: `app/src/test/java/com/example/classcast/ValidationUtilsTest.kt`  
28 tests, 6 categories, 100% pass rate.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Material Design 3, RecyclerView, ViewBinding |
| Auth | Firebase Authentication (email/password) |
| Database | Cloud Firestore (classes), Firebase Realtime Database (votes) |
| Testing | JUnit4 (local JVM tests) |
| Build | Gradle with version catalog (`libs.versions.toml`) |

---

## Lab Deliverables

| Lab | Feature |
|-----|---------|
| Lab 2 | Firebase Auth, Firestore CRUD, 3+ extensions |
| Lab 3 | Realtime Database (Option C), 28 unit tests, usability testing |
| Final | Full presentation + documentation |

---

## Known Issues / Limitations

- HeadcountActivity requires the Realtime Database `.read` rule at `$classId` level (see rules above). If counts stay at 0, check the Firebase Console → Realtime Database → Rules.
- VoteActivity uses Firestore to find a class by `courseCode`. If a student types the code in lowercase but the professor created it uppercase, the app tries an uppercase fallback automatically.
