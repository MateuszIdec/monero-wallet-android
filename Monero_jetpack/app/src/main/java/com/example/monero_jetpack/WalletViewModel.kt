package com.example.monero_jetpack

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

data class Transaction(val amount: Double, val fee: Double, val type: String)


class WalletViewModel : ViewModel() {
    private val _accountName = MutableStateFlow("Loading...")
    val accountName: StateFlow<String> = _accountName

    private val _accountBalance = MutableStateFlow("0.00")
    val accountBalance: StateFlow<String> = _accountBalance

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions


    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val authToken = "your_auth_token_here" // Replace with your actual token

    init {
        fetchWalletData()
    }

    @SuppressLint("DefaultLocale")
    fun fetchWalletData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Move network call to IO dispatcher
                val response = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://wallet.goodnodedemo.ovh/wallet/XMR/balance")
                        .addHeader("Authorization", "orzechySaZdrowe")
                        .build()

                    client.newCall(request).execute()
                }

                val response2 = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://wallet.goodnodedemo.ovh/wallet/XMR/transactions")
                        .addHeader("Authorization", "orzechySaZdrowe")
                        .build()

                    client.newCall(request).execute()
                }
                if (!response.isSuccessful) {
                    _error.value = "Error: ${response.code}"
                    return@launch
                }

                val responseData = response.body?.string()
                val responseData2 = response2.body?.string()

                // Parse your response here and update state
                // Assuming the response is JSON like: {"confirmed": 0.0002, "unconfirmed": 0}

                val gson = Gson()
                val jsonResponse = gson.fromJson(responseData, JsonObject::class.java)
                val jsonResponse2 = gson.fromJson(responseData2, JsonObject::class.java)

                val confirmedBalance = jsonResponse.get("confirmed").asDouble

                // Parsing transactions
                val inTransactions = jsonResponse2.getAsJsonArray("in").map {
                    val transactionObj = it.asJsonObject
                    Transaction(
                        amount = transactionObj.get("amount").asDouble,
                        fee = transactionObj.get("fee").asDouble,
                        type = transactionObj.get("type").asString
                    )
                }

                val outTransactions = jsonResponse2.getAsJsonArray("out").map {
                    val transactionObj = it.asJsonObject
                    Transaction(
                        amount = transactionObj.get("amount").asDouble,
                        fee = transactionObj.get("fee").asDouble,
                        type = transactionObj.get("type").asString
                    )
                }

                // Combine 'in' and 'out' transactions
                val allTransactions = inTransactions + outTransactions

                // Update account name and balance on the main thread
                withContext(Dispatchers.Main) {
                    // Update account name and balance
                    _accountName.value = "My XMR Wallet"
                    _accountBalance.value = "$confirmedBalance XMR"
                    _transactions.value = allTransactions  // Update the transactions list

                }

            } catch (e: IOException) {
                _error.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
