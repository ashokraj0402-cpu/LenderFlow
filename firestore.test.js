const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 5000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_lender_123";
const BOB_UID = "bob_lender_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCustomerPayload(uid) {
  return {
    userId: uid,
    customerCode: "CUST-1001",
    name: "Rajesh Sharma",
    phoneNumber: "+91 9876543210",
    disbursementDate: "2026-10-05",
    dueDayOfMonth: 5,
    initialPrincipal: 100000,
    extraPrincipalAdded: 0,
    principalRepaid: 0,
    currentActivePrincipal: 100000,
    monthlyInterestRate: 2.0,
    monthlyInterestDue: 2000,
    status: "Active",
    lastPaymentDate: "",
    lastInterestPaidMonth: "",
    followUpNotes: "On-time borrower",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validTransactionPayload(uid, customerId, type = "Monthly Interest Payment", amount = 2000) {
  return {
    userId: uid,
    transactionCode: "TXN-1001",
    customerId,
    customerCode: "CUST-1001",
    customerName: "Rajesh Sharma",
    transactionType: type,
    amount,
    paymentMode: "UPI",
    transactionDate: "2026-11-05",
    notes: "November interest received",
    createdAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller: cannot read or list customers or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Owner: can create valid customer and transaction atomically", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const customerRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  const txnRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("txn_1");

  const batch = aliceDb.batch();
  batch.set(customerRef, validCustomerPayload(ALICE_UID));
  batch.set(txnRef, validTransactionPayload(ALICE_UID, "cust_1", "Initial Disbursement", 100000));
  await assertSucceeds(batch.commit());
});

test("Cross-user isolation: Bob cannot read or write Alice's customers or transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  await assertSucceeds(custRef.set(validCustomerPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_2").set(validCustomerPayload(BOB_UID))
  );
});

test("Shadow update & identity spoofing: rejects ghost fields and mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ghostPayload = { ...validCustomerPayload(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_ghost").set(ghostPayload)
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_spoof").set(validCustomerPayload(BOB_UID))
  );
});

test("Orphan transaction guard: rejects transaction when referenced customer does not exist", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_orphan")
      .set(validTransactionPayload(ALICE_UID, "nonexistent_cust"))
  );
});

test("Audit trail immutability: rejects updating or deleting transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  const txnRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("txn_1");
  await assertSucceeds(custRef.set(validCustomerPayload(ALICE_UID)));
  await assertSucceeds(txnRef.set(validTransactionPayload(ALICE_UID, "cust_1")));

  await assertFails(txnRef.update({ amount: 9999 }));
  await assertFails(txnRef.delete());
});

test("Ledger update & closed account lock: allows Top-Up update when Active, blocks financial update when Closed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  await assertSucceeds(custRef.set(validCustomerPayload(ALICE_UID)));

  await assertSucceeds(
    custRef.update({
      extraPrincipalAdded: 25000,
      currentActivePrincipal: 125000,
      monthlyInterestDue: 2500,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    custRef.update({
      principalRepaid: 125000,
      currentActivePrincipal: 0,
      monthlyInterestDue: 0,
      status: "Closed",
      lastPaymentDate: "2026-11-14",
      lastInterestPaidMonth: "2026-11",
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    custRef.update({
      extraPrincipalAdded: 50000,
      currentActivePrincipal: 50000,
      monthlyInterestDue: 1000,
      updatedAt: serverTimestamp(),
    })
  );
});
