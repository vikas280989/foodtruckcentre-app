package au.com.foodtruckcentre.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.foodtruckcentre.app.data.Api
import au.com.foodtruckcentre.app.data.ApiResult
import au.com.foodtruckcentre.app.data.MyEnquiry
import au.com.foodtruckcentre.app.data.SessionStore
import au.com.foodtruckcentre.app.data.Validation
import kotlinx.coroutines.launch

/** Login, sign-up, password reset and the customer's account. Shared for the life of the app. */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val api = Api.get(app)
    val sessions = SessionStore.get(app)

    // Login
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")

    // Sign-up
    var regName by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regPhone by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regAccepted by mutableStateOf(false)

    // Forgot password
    var forgotEmail by mutableStateOf("")
    var forgotSent by mutableStateOf<String?>(null)

    val errors = mutableStateMapOf<String, String>()
    var busy by mutableStateOf(false)
        private set
    var banner by mutableStateOf<QuoteViewModel.Banner?>(null)

    var enquiries by mutableStateOf<Loadable<List<MyEnquiry>>>(Loadable.Loading)
        private set

    fun clearMessages() {
        errors.clear(); banner = null
    }

    fun login(onDone: () -> Unit) {
        if (busy) return
        clearMessages()
        Validation.email(loginEmail)?.let { errors["email"] = it }
        if (loginPassword.isEmpty()) errors["password"] = "Please enter your password."
        if (errors.isNotEmpty()) return
        busy = true
        viewModelScope.launch {
            when (val r = api.login(loginEmail.trim(), loginPassword)) {
                is ApiResult.Success -> { loginPassword = ""; onDone() }
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            busy = false
        }
    }

    fun register(onDone: () -> Unit) {
        if (busy) return
        clearMessages()
        Validation.name(regName)?.let { errors["name"] = it }
        Validation.email(regEmail)?.let { errors["email"] = it }
        Validation.phone(regPhone)?.let { errors["phone"] = it }
        Validation.password(regPassword)?.let { errors["password"] = it }
        if (!regAccepted) errors["accept"] = "Please accept the Terms and Privacy Policy."
        if (errors.isNotEmpty()) {
            banner = QuoteViewModel.Banner("Please check the highlighted fields.", false)
            return
        }
        busy = true
        viewModelScope.launch {
            when (val r = api.register(regName.trim(), regEmail.trim(), regPhone.trim(), regPassword)) {
                is ApiResult.Success -> {
                    regPassword = ""; regName = ""; regEmail = ""; regPhone = ""; regAccepted = false
                    onDone()
                }
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            busy = false
        }
    }

    fun forgot() {
        if (busy) return
        clearMessages()
        Validation.email(forgotEmail)?.let { errors["email"] = it; return }
        busy = true
        viewModelScope.launch {
            when (val r = api.forgotPassword(forgotEmail.trim())) {
                is ApiResult.Success -> forgotSent = r.value
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            busy = false
        }
    }

    fun logout() {
        viewModelScope.launch { api.logout() }
        enquiries = Loadable.Loading
    }

    fun loadEnquiries() {
        enquiries = Loadable.Loading
        viewModelScope.launch {
            enquiries = when (val r = api.myEnquiries()) {
                is ApiResult.Success -> Loadable.Ready(r.value)
                is ApiResult.Invalid -> Loadable.Error(r.message, false)
                is ApiResult.Failure -> Loadable.Error(r.message, r.offline)
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch { api.refreshProfile() }
    }

    fun updateProfile(name: String, phone: String, onDone: () -> Unit) {
        if (busy) return
        clearMessages()
        Validation.name(name)?.let { errors["name"] = it }
        Validation.phone(phone)?.let { errors["phone"] = it }
        if (errors.isNotEmpty()) return
        busy = true
        viewModelScope.launch {
            when (val r = api.updateProfile(name.trim(), phone.trim())) {
                is ApiResult.Success -> onDone()
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            busy = false
        }
    }

    fun deleteAccount(password: String, onDone: () -> Unit) {
        if (busy) return
        clearMessages()
        if (password.isEmpty()) { errors["password"] = "Please enter your password."; return }
        busy = true
        viewModelScope.launch {
            when (val r = api.deleteAccount(password)) {
                is ApiResult.Success -> { enquiries = Loadable.Loading; onDone() }
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            busy = false
        }
    }
}
