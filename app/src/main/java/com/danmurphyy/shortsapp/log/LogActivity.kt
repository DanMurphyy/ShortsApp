package com.danmurphyy.shortsapp.log

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.danmurphyy.shortsapp.activities.MainActivity
import com.danmurphyy.shortsapp.utils.Constants
import com.danmurphyy.shortsapp.utils.UiUtils
import com.danmurphyy.shortsapp.databinding.ActivityLogBinding
import com.danmurphyy.shortsapp.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.submitBtn.setOnClickListener {
            signup()
        }

        binding.submitBtnIn.setOnClickListener {
            signin()
        }

        binding.goToLoginBtn.setOnClickListener {
            binding.loSignup.visibility = View.GONE
            binding.loSignIn.visibility = View.VISIBLE
        }

        binding.goToSignupBtn.setOnClickListener {
            binding.loSignIn.visibility = View.GONE
            binding.loSignup.visibility = View.VISIBLE
        }
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitBtn.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.submitBtn.visibility = View.VISIBLE
        }
    }

    private fun signin() {
        val email = binding.emailInputIn.text.toString()
        val password = binding.passwordInputIn.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputIn.error = "Email not valid"
            return
        }
        if (password.length < 6) {
            binding.passwordInputIn.error = "Minimum 6 characters"
            return
        }

        loginWithFirebase(email, password)
    }

    private fun loginWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                UiUtils.showToast(applicationContext, "Login successfully")
                setInProgress(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                UiUtils.showToast(
                    applicationContext,
                    it.localizedMessage ?: "Something went wrong while auth create"
                )
                setInProgress(false)
            }
    }

    private fun signup() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Email not valid"
            return
        }
        if (password.length < 6) {
            binding.passwordInput.error = "Minimum 6 characters"
            return
        }
        if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Password is not matched"
            return
        }
        signupWithFirebase(email, password)
    }

    private fun signupWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                it.user?.let { user ->
                    val userModel = UserModel(user.uid, email, email.substringBefore("@"))
                    Firebase.firestore.collection(Constants.Users)
                        .document(user.uid)
                        .set(userModel)
                        .addOnSuccessListener {
                            UiUtils.showToast(applicationContext, "Account created successfully")
                            setInProgress(false)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            UiUtils.showToast(
                                applicationContext,
                                (e.localizedMessage ?: "Something went wrong while account create")
                            )
                            setInProgress(false)
                        }
                }
            }
            .addOnFailureListener {
                UiUtils.showToast(
                    applicationContext,
                    it.localizedMessage ?: "Something went wrong while auth create"
                )
                setInProgress(false)
            }
    }
}