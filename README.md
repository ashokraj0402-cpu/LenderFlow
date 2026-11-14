# LendFlow — Monthly Interest & Loan Ledger (`மாதாந்திர பணம் மற்றும் வட்டி கணக்கு`)

LendFlow is an offline-first Android app built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room (SQLite)** for tracking monthly interest-bearing loans, borrower repayments, overdue follow-ups, **Month Till Date** collections, and transferable **Excel (`.xlsx`)** ledgers.

---

## Table of Contents

1. [How the App Works](#how-the-app-works)
   - [Core Data Flow & Architecture](#core-data-flow--architecture)
   - [Room Database Entities & DAO](#room-database-entities--dao)
   - [Loan & Month-Till-Date Calculations](#loan--month-till-date-calculations)
   - [Pre-Seeded Portfolio Data](#pre-seeded-portfolio-data)
   - [Transferable 3-Sheet Excel Workbook (`.xlsx`)](#transferable-3-sheet-excel-workbook-xlsx)
2. [How the UI Looks](#how-the-ui-looks)
   - [Color Design & Always-White Theme](#color-design--always-white-theme)
   - [Screen-by-Screen Visual Walkthrough](#screen-by-screen-visual-walkthrough)
   - [Home Dashboard Layout Map](#home-dashboard-layout-map)
3. [How to Install on Your Android Mobile](#how-to-install-on-your-android-mobile)
   - [Method 1: Download APK from AI Studio (No Computer Needed)](#method-1-download-apk-from-ai-studio-no-computer-needed)
   - [Method 2: Build & Install via Android Studio (USB / Wireless Debugging)](#method-2-build--install-via-android-studio-usb--wireless-debugging)
   - [Method 3: Build & Verify via Gradle CLI](#method-3-build--verify-via-gradle-cli)
4. [Project Structure](#project-structure)

---

## How the App Works

### Core Data Flow & Architecture

LendFlow follows **MVVM** with reactive Kotlin `StateFlow` / `Flow` streams and local SQLite persistence (no login or internet connection required to manage your ledger):

```text
Compose UI (LendingMainScreen & Modals)
   │  observes StateFlow<LedgerUiState>
   ▼
LendingViewModel
   │  dispatches actions & computes live Month-Till-Date KPIs
   ▼
LendingRepository ──► ExcelWorkbookExporter (auto-syncs LendFlow_Ledger.xlsx)
   │  delegates atomic writes & reactive queries
   ▼
LendingDao (Room @Dao) ──► AppDatabase (SQLite: lendflow_ledger.db)
```

Whenever a borrower is added, edited, or receives a payment/top-up:
1. `LendingDao` executes an atomic `@Transaction` update in SQLite so borrower balances and the transaction log can never fall out of sync.
2. Room emits updated `Flow` lists to `LendingViewModel`, immediately refreshing all KPI cards, Month Till Date totals, Follow-up badges, and Ledger rows.
3. `LendingRepository` automatically regenerates `LendFlow_Ledger.xlsx` in app storage (`files/exports/`) on `Dispatchers.IO`.

---

### Room Database Entities & DAO

`AppDatabase` (version `3`) manages two relational tables plus a 1-to-many relation model:

1. **`Borrower` (`borrowers` table)**
   - **Identity & Schedule**: `id`, `customerCode` (e.g., `CUST-1001`), `name`, `phoneNumber`, `disbursementDate`, `dueDayOfMonth` (`1..31`).
   - **Principal Ledger**: `initialPrincipal`, `extraPrincipalAdded` (top-ups), `principalRepaid`, `currentActivePrincipal`.
   - **Interest & Arrears**: `monthlyInterestRate` (default `2.0%`), `monthlyInterestDue`, `outstandingInterest` (unpaid arrears).
   - **Status & Audit**: `status` (`Active`, `Follow-up Needed`, `Closed`), `lastPaymentDate`, `lastInterestPaidMonth` (`yyyy-MM`), `followUpNotes`, `createdAtMillis`, `updatedAtMillis`.

2. **`Transaction` (`transactions` table)**
   - Linked to `Borrower.id` via a `ForeignKey` (`onDelete = CASCADE`) with indices on `customerId` and `transactionDate`.
   - Records `id`, `transactionCode` (e.g., `TXN-1001`), `customerId`, `customerCode`, `customerName`, `transactionType`, `amount`, `paymentMode` (`UPI`, `Cash`, `Bank Transfer`, `Cheque`), `transactionDate`, `notes`, and `createdAtMillis`.

3. **`BorrowerWithTransactions`**
   - Room `@Relation` pairing each `Borrower` with their full list of `Transaction` history entries.

4. **`LendingDao` Operations**
   - **History & Filter Flows**: `observeBorrowers()`, `observeBorrowersByStatus()`, `observeTransactions()`, `observeTransactionsForBorrower()`, `observeTransactionsByType()`, `observeTransactionsInMonth()`, `observeBorrowerWithHistory()`, `observeAllBorrowersWithHistory()`.
   - **Live SQL Aggregations**: `observeActivePrincipalTotal()`, `observeMonthlyInterestExpectedTotal()`, `observeTotalInterestCollected()`.
   - **Atomic `@Transaction` Loan Workflows**:
     - `createBorrowerWithInitialDisbursement()` inserts a new borrower and their initial disbursement entry together.
     - `applyLoanTransaction()` processes:
       - **Monthly Interest Payment**: Records payment date/month, deducts from `outstandingInterest`, and restores status from `Follow-up Needed` to `Active` once arrears reach `₹0`.
       - **Top-Up**: Adds principal to `extraPrincipalAdded` and recalculates `currentActivePrincipal` and `monthlyInterestDue`.
       - **Principal Repayment**: Validates amount `<= currentActivePrincipal`, reduces principal, recalculates `monthlyInterestDue`, and closes the loan if principal reaches `₹0`.
       - **Full Closure**: Zeroes active principal, monthly interest due, and outstanding interest, marking the account `Closed`.
     - `updateBorrowerTerms()` updates contact info, due day, interest rate, status, and notes while recalculating `monthlyInterestDue`.

---

### Loan & Month-Till-Date Calculations

`LoanCalculator` handles all financial math and date tracking:

- **Monthly Interest Formula**:
  $$\text{Monthly Interest Due} = \text{Current Active Principal} \times \frac{\text{Monthly Interest Rate (\%)}}{100}$$
- **Month Till Date Due Calculation** (shown prominently on the front page):
  - Looks at every **non-closed** borrower whose `dueDayOfMonth <= currentDayOfMonth` (for example, if today is the 12th, everyone with due day `1` through `12`).
  - **To Receive**: Sum of `monthlyInterestDue` (plus any unpaid prior `outstandingInterest`) expected by today's date.
  - **Recorded**: Interest payments recorded during the current month (`yyyy-MM`) for those due-till-date accounts.
  - **Not Paid**: Remaining unpaid balance among those due-till-date accounts, along with the exact list of borrowers who haven't paid yet.

---

### Pre-Seeded Portfolio Data

On first launch, the database is pre-populated with 9 Tamil borrower accounts (`₹19,00,000` Total Active Principal, `₹38,000` Monthly Interest at 2%):

| Code | Borrower | Principal | Monthly Interest | Due Day | Status |
| :--- | :--- | ---: | ---: | :---: | :--- |
| `CUST-1001` | மணிமாறன் | ₹1,00,000 | ₹2,000 | 15 | Active |
| `CUST-1002` | இளங்கோ | ₹1,00,000 | ₹2,000 | 20 | Active |
| `CUST-1003` | குமரன் | ₹2,00,000 | ₹4,000 | 1 | **Follow-up Needed (₹11,000 overdue)** |
| `CUST-1004` | பால்பண்ணை | ₹9,00,000 | ₹18,000 | 15 | Active |
| `CUST-1005` | லதா ராஜா | ₹1,00,000 | ₹2,000 | 20 | Active |
| `CUST-1006` | கிருஷ்ணன் (1) | ₹1,50,000 | ₹3,000 | 2 | Active |
| `CUST-1007` | கிருஷ்ணன் (2) | ₹50,000 | ₹1,000 | 20 | Active |
| `CUST-1008` | ராம்குமார் | ₹1,00,000 | ₹2,000 | 1 | Active |
| `CUST-1009` | பாக்கியம் | ₹2,00,000 | ₹4,000 | 20 | Active |

---

### Transferable 3-Sheet Excel Workbook (`.xlsx`)

`ExcelWorkbookExporter` generates a standard OpenXML `.xlsx` spreadsheet (`LendFlow_Ledger.xlsx`) with zero external bloat, compatible with Microsoft Excel, Google Sheets, and WPS Office:

1. **Sheet 1 — `Summary`**: Portfolio KPIs, Month Till Date breakdown (`To Receive`, `Recorded`, `Not Paid`), and month-by-month collection totals.
2. **Sheet 2 — `Borrowers`**: Full borrower directory with codes, phone numbers, due dates, active principal, 2% monthly interest due, outstanding arrears, status, and Tamil/English notes.
3. **Sheet 3 — `Transactions`**: Complete chronological ledger of every disbursement, interest payment, top-up, principal repayment, and closure.

From the green **Excel/Table** icon in the top bar (or the **Excel / Export** button on the Ledger tab), you can:
- **Share `.xlsx`** directly via WhatsApp, Email, Telegram, or Google Drive using Android `FileProvider`.
- **Save `.xlsx` to Device** in Downloads/Documents using the Android file picker (`CreateDocument`).
- **Share or Copy CSV** as a lightweight text summary.

---

## How the UI Looks

### Color Design & Always-White Theme

The UI uses a clean, sleek **pure-white ledger design** inspired by modern social feeds (clean cards, crisp typography, pill buttons, and calm color accents). Both **Light Mode** and **Dark Mode** intentionally render on a **pure white background** so numbers remain crisp and readable in every environment:

| Role | Color | Hex | Usage |
| :--- | :--- | :--- | :--- |
| **Canvas & Cards** | Pure White | `#FFFFFF` | Screen background, KPI cards, borrower cards, modals, navigation bars |
| **Card Sub-Trays** | Whisper Slate | `#F8FAFC` / `#F3F4F6` | Search field fill, metric sub-boxes, composer input pill |
| **Hairline Borders** | Neutral Gray | `#E5E7EB` | `1.dp` card outlines and section dividers |
| **Primary Text** | Charcoal Ink | `#111827` | Headings, borrower names, principal/interest figures |
| **Secondary Text** | Calm Slate | `#6B7280` | Captions, due days, payment modes, subtitles |
| **Interactive Accent** | Cobalt Blue | `#2563EB` | Active bottom nav tab, `Record Pay` pill, progress bar, FAB, filter chips |
| **On Track / Collected** | Emerald Green | `#15803D` | `Active` pill, `Interest Collected` KPI, `Recorded` box, Excel button |
| **Follow-Up Warning** | Warm Amber | `#B45309` | `Follow-up Needed` status badge, `Top-Up` action icon |
| **Overdue Alert** | Coral Red | `#DC2626` | `Overdue` badge, `₹11,000` overdue highlight, `Not Paid` till-date alert box |

---

### Screen-by-Screen Visual Walkthrough

- **Top App Bar**: Displays **LendFlow** on the left and three circular 38dp icon buttons on the right:
  - `+` (**Add Borrower**)
  - Magnifying glass (**Toggle Search Bar**)
  - Green table icon (**Excel Workbook & Monthly Summary Modal**)
- **Tab 1 — Home (Front-Page Dashboard)**:
  - **Dashboard Header** (`மாதாந்திர பணம் மற்றும் வட்டி கணக்கு`) + green active account count pill (`9 Active`).
  - **4 KPI Cards** (2×2 grid on phones, 4-column row on tablets):
    1. **Total Active Principal** (`₹19,00,000`)
    2. **Monthly Interest Due** (`₹38,000` · 2% rate)
    3. **Overdue Interest** (`₹11,000` highlighted with a red border when overdue > 0)
    4. **Interest Collected** (cumulative collected interest in emerald green)
  - **Month Till Date Due Card**:
    - Shows the current date range (e.g., `Nov 1 – Nov 14 · Due day ≤ 14`) and an `Unpaid` or `All Paid` pill badge.
    - **3-column breakdown**: `To Receive` | `Recorded` | `Not Paid`.
    - Blue linear progress bar showing percentage collected till date.
    - Highlighted rows for any unpaid due-till-date borrowers with a 1-tap blue **`Record Pay`** pill button.
  - **Quick Composer Card** (Facebook-style status composer):
    - Round `LF` badge + *"Record interest, top-up, or repayment…"* pill + 3 quick action buttons (`Pay Interest`, `New Loan`, `Top-Up`).
  - **Borrower Accounts Feed**:
    - Filterable by `All`, `Active`, `Follow-up Needed`, and `Closed`.
    - Each card shows the borrower's avatar initials, name, customer code, status/overdue pill badges, due day, Active Principal, Monthly Interest Due, quick **`Pay`** and **`Call`** buttons, and taps open the full **Borrower Detail** dialog.
- **Tab 2 — Borrowers**: Full searchable directory with quick **`+ New Loan`** header button and status filters.
- **Tab 3 — Follow-up**: Focused action list of borrowers with overdue/follow-up balances (with a red numeric badge on the bottom navigation icon showing how many accounts need attention).
- **Tab 4 — Ledger**: Chronological audit feed of all transactions with search, transaction-type filter chips, payment-mode chips (`UPI`, `Cash`, `Bank Transfer`, `Cheque`), and a quick **Excel / Export** button.
- **Responsive Tablet / Foldable Layout**: On screens `>= 600dp` wide, the bottom navigation bar automatically adapts into a left-hand **Navigation Rail**, and the 4 KPI cards expand into a single 4-column row.
- **Floating Action Button (`+` Payments)**: Circular blue FAB pinned at the bottom-right for instant payment or adjustment entry from any tab.

---

### Home Dashboard Layout Map

```text
┌────────────────────────────────────────────────────────┐
│ LendFlow                                 (+)  (🔍) (📊)│  <- White TopBar + Hairline Divider
├────────────────────────────────────────────────────────┤
│ Dashboard                                   [9 Active] │
│ மாதாந்திர பணம் மற்றும் வட்டி கணக்கு                       │
│                                                        │
│ ┌──────────────────────┐  ┌──────────────────────────┐ │
│ │ Total Active Princip.│  │ Monthly Interest Due     │ │
│ │ ₹19,00,000           │  │ ₹38,000                  │ │
│ └──────────────────────┘  └──────────────────────────┘ │
│ ┌──────────────────────┐  ┌──────────────────────────┐ │
│ │ Overdue Interest   ! │  │ Interest Collected       │ │
│ │ ₹11,000 (Red border) │  │ ₹5,000                   │ │
│ └──────────────────────┘  └──────────────────────────┘ │
│                                                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Month Till Date Due                 [₹4,000 Unpaid]│ │
│ │ [To Receive]      [Recorded]      [Not Paid]       │ │
│ │ ███████████████░░░░░░░░░  55% collected            │ │
│ │ • குமரன் · Due Day 1 · Unpaid          [Record Pay] │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │ (LF) ( Record interest, top-up, or repayment…    ) │ │
│ │   Pay Interest   |   New Loan   |   Top-Up         │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ Borrower Accounts (9)                                  │
│ [All]  [Active]  [Follow-up Needed]  [Closed]          │
│ ┌────────────────────────────────────────────────────┐ │
│ │ (கு) குமரன் · CUST-1003       [Follow-up] [Overdue]│ │
│ │ Principal: ₹2,00,000  |  Interest: ₹4,000/mo       │ │
│ │                                  [Call]  [Pay]     │ │
│ └────────────────────────────────────────────────────┘ │
│                                                  (💳)  │  <- Quick Payment FAB
├────────────────────────────────────────────────────────┤
│   🏠 Home   |   👥 Borrowers  |  🔔 Follow-up  | 🧾 Ledger │
└────────────────────────────────────────────────────────┘
```

---

## How to Install on Your Android Mobile

**Requirements**: Any Android phone running **Android 7.0 (API 24)** or higher.

### Method 1: Download APK from AI Studio (No Computer Needed)

1. In Google AI Studio, open the project **Settings / Menu** and choose **Generate APK** (or **Build APK**).
2. Wait for the APK download link to finish building, then download the `.apk` file onto your Android phone (or transfer it via WhatsApp / Google Drive / USB).
3. Tap the downloaded `.apk` file in your phone's **Files** app or browser downloads.
4. If Android shows a security prompt (*"For your security, your phone isn't allowed to install unknown apps from this source"*):
   - Tap **Settings** on the prompt.
   - Toggle **Allow from this source** ON.
   - Go back and tap **Install**.
5. Once installed, tap **Open** (or launch **LendFlow** from your home screen). Your 9 default Tamil borrower accounts load automatically with zero sign-in required.

> **Updating later**: Whenever you download a newer APK build, install it over the existing app (using the same project signing configuration) so your existing Room database entries remain intact.

---

### Method 2: Build & Install via Android Studio (USB / Wireless Debugging)

1. **Export Project**: Download the project ZIP from AI Studio (or clone it from GitHub) and extract it on your computer.
2. **Open in Android Studio**: Launch Android Studio → **File → Open** → select the extracted project folder and wait for Gradle sync to finish.
3. **Enable Developer Options on Your Phone**:
   - Open **Settings → About phone**.
   - Tap **Build number** 7 times until *"You are now a developer!"* appears.
   - Open **Settings → System → Developer options** and turn on **USB debugging** (or **Wireless debugging** on Android 11+).
4. **Connect & Run**:
   - Connect your phone via USB cable and tap **Allow USB debugging** on your phone.
   - Select your phone from the device dropdown in Android Studio and click the green **Run ▶** button.

---

### Method 3: Build & Verify via Gradle CLI

If you are building from a terminal with the Android SDK configured:

```bash
# Build the installable debug APK
gradle :app:assembleDebug

# Run all 12 unit & Robolectric database/Excel tests
gradle :app:testDebugUnitTest
```

The compiled debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

You can copy `app-debug.apk` onto your phone and install it directly, or install over ADB from your workstation:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

- `app/src/main/java/com/example/MainActivity.kt` — Edge-to-edge activity configured with light system bars.
- `app/src/main/java/com/example/data/model/LendingModels.kt` — Room entities (`Borrower`, `Transaction`), `BorrowerWithTransactions` relation, KPI models, and input models.
- `app/src/main/java/com/example/data/local/LendingDao.kt` — Room DAO with reactive history queries, SQL aggregations, and atomic `@Transaction` loan calculations.
- `app/src/main/java/com/example/data/local/AppDatabase.kt` — Room database (`version = 3`) and `DefaultBorrowersSeed` (9 Tamil borrower accounts).
- `app/src/main/java/com/example/data/repository/LendingRepository.kt` — Repository coordinating DAO operations, seeding, and `.xlsx` background sync.
- `app/src/main/java/com/example/domain/LoanCalculator.kt` — Interest math, overdue checks, Month Till Date computations, and INR/CSV formatting.
- `app/src/main/java/com/example/domain/ExcelWorkbookExporter.kt` — Pure-Kotlin OpenXML `.xlsx` generator (`Summary`, `Borrowers`, `Transactions`) + `FileProvider` URI helper.
- `app/src/main/java/com/example/ui/theme/` — Pure-white light/dark `Theme.kt`, `Color.kt`, and `Type.kt`.
- `app/src/main/java/com/example/ui/viewmodel/LendingViewModel.kt` — Unidirectional UI state (`LedgerUiState`), filters, and action handlers.
- `app/src/main/java/com/example/ui/screens/LendingMainScreen.kt` — Responsive 4-tab main screen (`Home`, `Borrowers`, `Follow-up`, `Ledger`).
- `app/src/main/java/com/example/ui/components/LendingComponents.kt` — KPI cards, `MonthTillDateCard`, `QuickComposerCard`, `BorrowerListCard`, and `TransactionRowCard`.
- `app/src/main/java/com/example/ui/modals/LendingModals.kt` — `QuickPaymentModal`, `AddBorrowerModal`, `BorrowerDetailDialog`, `EditCustomerModal`, and `ExportSummaryModal`.
- `app/src/test/java/com/example/` — Unit and Robolectric tests for `LendingDao`, `AppDatabase`, `LoanCalculator`, and `ExcelWorkbookExporter`.
