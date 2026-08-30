package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Google Play Billing for the LockDraw Lifetime VIP Pass (one-time INAPP purchase).
 * Product ID: "lockdraw_vip_pass"
 */
class PlayBillingManager(
    private val context: Context,
    private val onPremiumStatusChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _billingState = MutableStateFlow<BillingStatus>(BillingStatus.Connecting)
    val billingState: StateFlow<BillingStatus> = _billingState.asStateFlow()

    private val _vipProductDetails = MutableStateFlow<ProductDetails?>(null)
    val vipProductDetails: StateFlow<ProductDetails?> = _vipProductDetails.asStateFlow()

    private val _formattedPrice = MutableStateFlow("4,99 €")
    val formattedPrice: StateFlow<String> = _formattedPrice.asStateFlow()

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryExistingPurchases()
            return
        }

        _billingState.value = BillingStatus.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup successful")
                    _billingState.value = BillingStatus.Ready
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Google Play Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _billingState.value = BillingStatus.Unavailable(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Google Play Billing disconnected. Retrying...")
                _billingState.value = BillingStatus.Disconnected
            }
        })
    }

    /**
     * Query Google Play Store for the VIP Pass INAPP product details.
     */
    fun queryProductDetails() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(VIP_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val vipDetails = productDetailsList.firstOrNull { it.productId == VIP_PRODUCT_ID }
                if (vipDetails != null) {
                    _vipProductDetails.value = vipDetails
                    val price = vipDetails.oneTimePurchaseOfferDetails?.formattedPrice
                    if (!price.isNullOrBlank()) {
                        _formattedPrice.value = price
                    }
                    Log.d(TAG, "VIP Product found: ${vipDetails.title}, price: ${_formattedPrice.value}")
                }
            } else {
                Log.d(TAG, "Product details query returned code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Check if the user has already purchased the VIP pass on their Google Play account.
     */
    fun queryExistingPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasVip = false
                for (purchase in purchasesList) {
                    if (purchase.products.contains(VIP_PRODUCT_ID)) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            hasVip = true
                            if (!purchase.isAcknowledged) {
                                acknowledgePurchase(purchase)
                            }
                        }
                    }
                }
                if (hasVip) {
                    Log.d(TAG, "Existing VIP Pass verified on Google Play account!")
                    onPremiumStatusChanged(true)
                }
            } else {
                Log.w(TAG, "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launch Google Play Billing Flow to buy the VIP Pass.
     */
    fun launchBillingFlow(activity: Activity): Boolean {
        if (!billingClient.isReady) {
            startConnection()
            return false
        }

        val details = _vipProductDetails.value
        val flowParams = if (details != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
        } else {
            // If Google Play hasn't returned details yet (e.g. app not yet published in console or testing in sandbox),
            // return false
            Log.w(TAG, "Product details not loaded yet.")
            return false
        }

        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Callback from Google Play after user completes purchase or cancels.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled Play Store purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.i(TAG, "Item already owned on Google Play. Unlocking VIP.")
                onPremiumStatusChanged(true)
            }
            else -> {
                Log.e(TAG, "Purchase failed: code=${billingResult.responseCode}, message=${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(VIP_PRODUCT_ID)) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                } else {
                    onPremiumStatusChanged(true)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        scope.launch {
            billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase successfully acknowledged with Google Play!")
                    onPremiumStatusChanged(true)
                } else {
                    Log.e(TAG, "Acknowledge purchase failed: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * Trigger manual restore of purchases (e.g., when user clicks "Ripristina Acquisti").
     */
    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            startConnection()
            onComplete(false)
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var found = false
                for (purchase in purchasesList) {
                    if (purchase.products.contains(VIP_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        found = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        } else {
                            onPremiumStatusChanged(true)
                        }
                    }
                }
                onComplete(found)
            } else {
                onComplete(false)
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    sealed class BillingStatus {
        data object Connecting : BillingStatus()
        data object Ready : BillingStatus()
        data object Disconnected : BillingStatus()
        data class Unavailable(val message: String) : BillingStatus()
    }

    companion object {
        const val VIP_PRODUCT_ID = "lockdraw_vip_pass"
        private const val TAG = "PlayBillingManager"
    }
}
