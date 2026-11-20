# Property Broker — Free Admin Website Hosting Guide (100% Free)

This `admin-web/` folder is a **zero-build, static web app** directly connected to your live **Property Broker** Android app's Firebase Auth and Cloud Firestore database. You can host it **100% FREE forever** using any of the 3 methods below.

---

## Option 1: Firebase Hosting (Recommended — Already Pre-Configured!)

`firebase.json` in this project is already configured to serve the `admin-web/` folder on Firebase's **Free Spark Plan** (10 GB/month free transfer + automatic SSL HTTPS).

1. Install Firebase CLI (if not installed):
   ```bash
   npm install -g firebase-tools
   ```
2. Log in with your admin Google account (`md.hashim248@gmail.com`):
   ```bash
   firebase login
   ```
3. Deploy the admin website from the project root folder:
   ```bash
   firebase deploy --only hosting --project gen-lang-client-0124044841
   ```
4. Your free live admin URL will immediately be:
   - `https://gen-lang-client-0124044841.web.app`
   - `https://gen-lang-client-0124044841.firebaseapp.com`

> Because these domains belong to your Firebase project, **Google Sign-In works immediately with zero extra setup!**

---

## Option 2: Netlify Drop (Fastest — No Coding or CLI Required)

1. Go to [https://app.netlify.com/drop](https://app.netlify.com/drop) and create a free account.
2. Drag and drop the **`admin-web`** folder directly into the browser window.
3. Netlify will give you a free HTTPS URL (e.g., `https://property-broker-admin.netlify.app`).
4. **One-Time Auth Step**: Open Firebase Console → **Authentication** → **Settings** → **Authorized domains** → Click **Add domain** → Paste your Netlify hostname (e.g., `property-broker-admin.netlify.app`).

---

## Option 3: GitHub Pages (100% Free — Auto-Deploy Ready!)

This project already includes the GitHub Pages workflow (`.github/workflows/deploy-github-pages.yml`), `admin-web/.nojekyll`, and `admin-web/404.html`.

### Method A: Push Full Project to GitHub (Recommended — Auto-Deploys `admin-web/`)
1. Push this project to a **GitHub repository** (using AI Studio's **Push to GitHub** option or `git push`).
2. Open your repository on GitHub → click **Settings** → **Pages** (left sidebar).
3. Under **Build and deployment → Source**, select **GitHub Actions**.
4. Open the **Actions** tab → select **Deploy Property Broker Admin Website to GitHub Pages** → click **Run workflow** (future pushes to `admin-web/` deploy automatically!).
5. Your live website URL will be:
   `https://<your-github-username>.github.io/<your-repo-name>/`

### Method B: Upload Only the `admin-web/` Folder to a New GitHub Repo
1. Create a new **Public** GitHub repository (e.g., `property-broker-admin`).
2. Upload all files from inside `admin-web/` (`index.html`, `styles.css`, `app.js`, `admin-service.js`, `firebase-config.js`, `map-manager.js`, `analytics-manager.js`, `404.html`, `.nojekyll`) to the repo root.
3. Go to **Settings → Pages → Source: Deploy from a branch → Branch: `main` / `(root)` → Save**.
4. Wait ~1 minute and open `https://<your-github-username>.github.io/<your-repo-name>/`.

### One-Time Firebase Auth Step (Required for Google Sign-In on GitHub Pages)
1. Open [Firebase Console](https://console.firebase.google.com/project/gen-lang-client-0124044841/authentication/settings).
2. Go to **Authentication → Settings → Authorized domains → Add domain**.
3. Add **`<your-github-username>.github.io`** and click **Add**.
4. Refresh your live GitHub Pages URL and sign in with **`md.hashim248@gmail.com`**.

## Option 4: Cloudflare Pages (100% Free)
1. Connect your GitHub repo on Cloudflare Pages.
2. Leave **Build Command** blank and set **Build output directory** to `admin-web`.
3. Add your `*.pages.dev` domain under Firebase Console → **Authentication → Settings → Authorized domains**.

---

## Admin Login Security
- Only the verified Google account **`md.hashim248@gmail.com`** can read admin lists, view owner contact vaults, grant/revoke 7-Day Premium Passes, moderate chats, or edit `/app_config/settings`.
- All other users are strictly blocked at the **Firestore Security Rules** layer (`firestore.rules`).
