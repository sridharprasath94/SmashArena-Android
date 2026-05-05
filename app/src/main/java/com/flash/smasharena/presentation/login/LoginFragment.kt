package com.flash.smasharena.presentation.login

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentLoginBinding
import com.flash.smasharena.util.toUserMessage
import dev.androidbroadcast.vbpd.viewBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()
    private val binding by viewBinding(FragmentLoginBinding::bind)

    @Inject lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val idToken = task.getResult(ApiException::class.java).idToken
                if (idToken != null) viewModel.signInWithGoogle(idToken)
                else showSnackbar(getString(R.string.error_google_sign_in))
            } catch (e: ApiException) {
                showSnackbar(getString(R.string.error_google_sign_in))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.isAlreadySignedIn) {
            navigateToHome()
            return
        }

        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            viewModel.signIn(
                email = binding.etEmail.text?.toString().orEmpty().trim(),
                password = binding.etPassword.text?.toString().orEmpty(),
            )
        }

        binding.btnCreateAccount.setOnClickListener {
            viewModel.createAccount(
                email = binding.etEmail.text?.toString().orEmpty().trim(),
                password = binding.etPassword.text?.toString().orEmpty(),
            )
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.btnSignIn.isEnabled = !state.isLoading
                    binding.btnCreateAccount.isEnabled = !state.isLoading
                    binding.btnGoogleSignIn.isEnabled = !state.isLoading

                    binding.tilEmail.error = state.emailError
                    binding.tilPassword.error = state.passwordError

                    state.generalError?.let { error ->
                        viewModel.onGeneralErrorShown()
                        showSnackbar(error.toUserMessage(requireContext()))
                    }

                    if (state.navigateToHome) {
                        viewModel.onNavigatedToHome()
                        navigateToHome()
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(
            R.id.action_loginFragment_to_homeFragment,
        )
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }
}
