# Pretty Petty: Your Style, Your Color, Your Chaos

“Your life is a mess. At least make it designer. 🎀”

Pretty Petty is a fashion personality quiz and a scrapbook-style planner. Take the quiz, meet one of five original fictional fashion villains, choose a palette independently, and keep your notes in a private MongoDB-backed board.

## Requirements

- Java 17 or newer
- Apache Maven 3.9 or newer
- Node.js 20 or newer and npm
- MongoDB Community Server installed and running locally

No Docker, MongoDB Atlas, paid services, or external image services are required.

## Run locally

Start MongoDB Community Server first. On Windows, if it is installed as a service:

```powershell
Start-Service MongoDB
```

If your service uses a different name, start it from Windows Services or use your installation's normal `mongod` command. The backend connects to `mongodb://localhost:27017/pretty_petty` by default.

In a first terminal, start the Spring Boot API:

```powershell
cd backend
mvn spring-boot:run
```

Configure a different MongoDB connection by setting `MONGODB_URI` before starting Spring:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/pretty_petty"
```

In a second terminal, start the React/Vite website:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Open the Vite URL printed in the terminal (normally `http://localhost:5173`). Vite proxies `/api` requests to the Spring Boot API on port 8080. Build the frontend with `npm.cmd run build` from `frontend`; run backend tests with `mvn test` from `backend`.

## Try the application

1. Take the five-question quiz and reveal a villain result.
2. Choose a palette independently from the suggested villain palette.
3. Select **Enter My Chaos** to save your profile and enter the planner without creating an account.
4. Add, edit, complete, drag, and delete notes. Deadlines are optional and displayed in your local timezone.
5. Use **MAKE IT WORSE** to rearrange the board, then change its palette or copy its URL.
6. Refresh `/board/{boardId}` while signed in to restore the saved board and notes.

The planner is private to the browser session that owns the board. A URL alone does not grant access to another session. A localhost link works only on the same computer; access from another computer requires hosting or network configuration.

The frontend uses an anonymous session and does not show login or signup forms. The backend uses a session cookie and CSRF token for planner requests. Planner notes, positions, reactions, boards, and profile palette/villain selection are stored in MongoDB, not `localStorage`.

## Quiz scoring

Each submitted answer awards one point to one villain. Ties are resolved by this fixed backend priority: Pink Manipulator, Corporate Heartbreaker, Romantic Menace, Lavender Chaos Queen, then Innocent Menace. The priority is listed in `backend/src/main/resources/quiz-data.json`.

## Fashion artwork and reactions

Outfit illustrations and the cat sticker are original bundled SVGs. Editorial moodboards arrange labeled fashion-item illustrations and captions locally; they do not use product photos, third-party image URLs, or invented brand logos.

Reaction captions are maintained in `backend/src/main/resources/reaction-data.json`. The backend selects a language uniformly from English, Moroccan Darija, Arabic, French, Spanish, Urdu, Russian, and Kyrgyz, then selects a caption for that language. Each new note saves its own reaction and text direction. User-written notes are never translated or modified.
