package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LendingDao {

    @Query("SELECT * FROM customers ORDER BY createdAtMillis DESC")
    fun observeCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAtMillis DESC")
    fun observeTransactions(): Flow<List<LoanTransaction>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): Customer?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LoanTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LoanTransaction>)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: String)

    @Transaction
    suspend fun insertCustomerWithDisbursement(
        customer: Customer,
        transaction: LoanTransaction,
    ) {
        insertCustomer(customer)
        insertTransaction(transaction)
    }

    @Transaction
    suspend fun updateCustomerAndInsertTransaction(
        customer: Customer,
        transaction: LoanTransaction,
    ) {
        updateCustomer(customer)
        insertTransaction(transaction)
    }

    @Transaction
    suspend fun seedDefaultData(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
    ) {
        insertCustomers(customers)
        insertTransactions(transactions)
    }
}
