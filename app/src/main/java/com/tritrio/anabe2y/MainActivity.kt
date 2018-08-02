package com.tritrio.anabe2y

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEND == intent?.action && intent.type != null) {
            if ("text/plain" == intent.type) {
//                val frag = supportFragmentManager.findFragmentByTag("fragment_create_post")
//                if (frag != null) {
//                    supportFragmentManager.beginTransaction().remove(frag).commit()
//                }
                CreatePostFragment.newInstance(intent.getStringExtra(Intent.EXTRA_TEXT)).show(supportFragmentManager, "fragment_create_post")
            } else if (intent.type.startsWith("image/")) {
//                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }
    }

    private fun setup() {
        if (Intent.ACTION_SEND == intent?.action && intent.type != null) {
            if ("text/plain" == intent.type) {
                CreatePostFragment.newInstance(intent.getStringExtra(Intent.EXTRA_TEXT)).show(supportFragmentManager, "fragment_create_post")
            } else if (intent.type.startsWith("image/")) {
//                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.container_main, MainFragment.newInstance()).commit()

        retry_button.setOnClickListener(null)
        retry_button.visibility = View.GONE

        fab_main.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(FirebaseAuth.getInstance().currentUser == null) {
            fab_main.isEnabled = false
            retry_button.setOnClickListener {
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(listOf(
                                        AuthUI.IdpConfig.GoogleBuilder().build(),
                                        AuthUI.IdpConfig.FacebookBuilder().build(),
                                        AuthUI.IdpConfig.EmailBuilder().build()))
                                .setTheme(R.style.SignInTheme)
                                .setLogo(R.drawable.logo)
                                .build(),
                        0)
            }
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(listOf(
                                    AuthUI.IdpConfig.GoogleBuilder().build(),
                                    AuthUI.IdpConfig.FacebookBuilder().build(),
                                    AuthUI.IdpConfig.EmailBuilder().build()))
                            .setTheme(R.style.SignInTheme)
                            .setLogo(R.drawable.logo)
                            .build(),
                    0)
        } else {
            setup()
        }

        fab_main.setOnClickListener{
            CreatePostFragment.newInstance(null).show(supportFragmentManager, "fragment_create_post")
        }

        navigation_main.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.menu_account -> {true}
                R.id.menu_signout -> {
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener {
                                if(fab_main.isEnabled) {
                                    val frag = supportFragmentManager.findFragmentById(R.id.container_main)
                                    if(frag != null) {
                                        supportFragmentManager.beginTransaction().remove(frag).commit()
                                    }
                                    retry_button.visibility = View.VISIBLE
                                    fab_main.isEnabled = false
                                    fab_main.isEnabled = false
                                    retry_button.setOnClickListener {
                                        startActivityForResult(
                                                AuthUI.getInstance()
                                                        .createSignInIntentBuilder()
                                                        .setAvailableProviders(listOf(
                                                                AuthUI.IdpConfig.GoogleBuilder().build(),
                                                                AuthUI.IdpConfig.FacebookBuilder().build(),
                                                                AuthUI.IdpConfig.EmailBuilder().build()))
                                                        .setTheme(R.style.SignInTheme)
                                                        .setLogo(R.drawable.logo)
                                                        .build(),
                                                0)
                                    }
                                }
                                drawer_main.closeDrawer(Gravity.START)
                            }
                    true
                }
                else -> {false}
            }
        }

        bottom_app_bar.setNavigationOnClickListener {
            drawer_main.openDrawer(Gravity.START)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0) {
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK) {
                setup()
            } else {
                when {
                    response == null -> {
                        Toast.makeText(this, "You have to sign-in.", Toast.LENGTH_LONG).show()
                    }
                    response.error?.errorCode == ErrorCodes.NO_NETWORK -> {
                        Toast.makeText(this, "No internet connection, please try again later.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this, "Something happened, please try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
